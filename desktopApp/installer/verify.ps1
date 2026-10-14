param([string]$Msi,[string]$WixDir,[string]$WorkDir,[string]$Exe='')
$ErrorActionPreference='Stop'
$Msi=[IO.Path]::GetFullPath($Msi)
$null=[Reflection.Assembly]::LoadFrom((Join-Path $WixDir 'Microsoft.Deployment.WindowsInstaller.dll'))
$null=[Reflection.Assembly]::LoadFrom((Join-Path $WorkDir 'FitEasy.Localizer.dll'))
$null=[Microsoft.Deployment.WindowsInstaller.Installer]::SetInternalUI([Microsoft.Deployment.WindowsInstaller.InstallUIOptions]::Silent)
[Microsoft.Deployment.WindowsInstaller.Installer]::EnableLog([Microsoft.Deployment.WindowsInstaller.InstallLogModes]::Verbose,(Join-Path $WorkDir 'verify-msi.log'))
$translations=Get-Content (Join-Path $WorkDir 'languages.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$engineMsi=[IO.Path]::GetFullPath((Join-Path $WorkDir 'verify-engine.msi'))
Copy-Item -LiteralPath $Msi -Destination $engineMsi -Force
# Isolate engine checks from a user-installed FitEasy version. No installation is run.
$testDb=New-Object Microsoft.Deployment.WindowsInstaller.Database($engineMsi,[Microsoft.Deployment.WindowsInstaller.DatabaseOpenMode]::Transact)
try {$testDb.Execute('UPDATE `Property` SET `Value` = ''{0}'' WHERE `Property` = ''ProductCode''',@('{'+[Guid]::NewGuid().ToString().ToUpperInvariant()+'}'));$testDb.Commit()}finally {$testDb.Dispose()}
$cases=@('en-US','ru-RU','be-BY','fr-CA','de-AT','es-MX','it-CH','zh-CN','ja-JP','ko-KR','pt-BR','xx-YY','')
$expected=@('en','ru','be','fr','de','es','it','zh','ja','ko','en','en',[LanguagePolicy]::SystemLanguage())
for($i=0;$i -lt $cases.Count;$i++){
    # OpenPackage alone performs no installation. Invoke only our property-setting action.
    $session=[Microsoft.Deployment.WindowsInstaller.Installer]::OpenPackage($engineMsi,$false)
    try {
        $session['FITEASYLANGUAGE']=$cases[$i]
        $session.DoAction('FitEasySelectLanguage')
        $lang=$session['FITEASYLANGUAGE']
        if($lang -ne $expected[$i]){throw "Language mismatch for $($cases[$i]): $lang"}
        foreach($property in $translations.$lang.PSObject.Properties){
            if($property.Name -like 'MSG_*'){continue}
            $value=$property.Value.Replace('[ProductName]',$session['ProductName'])
            if($session[$property.Name] -ne $value){throw "Unicode property mismatch: $lang / $($property.Name)"}
        }
        if($session['WIXUI_EXITDIALOGOPTIONALCHECKBOX'] -ne '1'){throw 'Launch checkbox default is not checked'}
        if(!$translations.$lang.MSG_DELETE_DATA){throw 'Missing localized uninstall choice'}
        $condition='REMOVE="ALL" AND NOT UPGRADINGPRODUCTCODE AND FITEASYDELETEDATA="1"'
        foreach($scenario in @(@('ALL','','1',$true),@('ALL','','0',$false),@('ALL','','',$false),@('ALL','upgrade','1',$false),@('','','1',$false))){
            $session['REMOVE']=$scenario[0];$session['UPGRADINGPRODUCTCODE']=$scenario[1];$session['FITEASYDELETEDATA']=$scenario[2]
            if([bool]$session.EvaluateCondition($condition) -ne $scenario[3]){throw 'Uninstall data condition mismatch'}
        }
        $session['REMOVE']='';$session['UPGRADINGPRODUCTCODE']='';$session['FITEASYDELETEDATA']=''
        $session['INSTALLDIR']='C:\FitEasy-Language-Probe'
        $view=$session.Database.OpenView('SELECT `Text` FROM `Control`');$view.Execute()
        while($record=$view.Fetch()) {
            try {$text=$record.GetString(1);if($text -like '*FELOC*') {
                $formatted=$session.Format($text)
                if($formatted -match '\[FELOC|\[INSTALLDIR\]|\[ProductName\]'){throw "Unexpanded dialog text: $lang"}
            }}finally {$record.Dispose()}
        };$view.Dispose()
        $view=$session.Database.OpenView('SELECT `Condition` FROM `ControlEvent` WHERE `Argument` = ''FitEasyLaunch''');$view.Execute();$record=$view.Fetch()
        try {
            if(!$record -or $record.GetString(1) -notmatch 'UILevel >= 5'){throw 'Missing silent launch guard'}
            $condition=$record.GetString(1);$session['Installed']=''
            foreach($level in @(2,3,4)){$session['UILevel']=[string]$level;if($session.EvaluateCondition($condition)){throw 'Launch enabled for silent/passive UI'}}
            $session['UILevel']='5';if(!$session.EvaluateCondition($condition)){throw 'Launch disabled for full UI'}
            $session['WIXUI_EXITDIALOGOPTIONALCHECKBOX']='';if($session.EvaluateCondition($condition)){throw 'Unchecked launch checkbox ignored'}
        }finally {if($record){$record.Dispose()};$view.Dispose()}
    }finally {$session.Dispose()}
    Write-Output "PASS language case $($cases[$i])"
}
$db=New-Object Microsoft.Deployment.WindowsInstaller.Database($Msi,[Microsoft.Deployment.WindowsInstaller.DatabaseOpenMode]::ReadOnly)
try {
    $view=$db.OpenView('SELECT `Action`,`Sequence` FROM `InstallUISequence`');$view.Execute();$seq=@{}
    while($record=$view.Fetch()){$seq[$record.GetString(1)]=$record.GetInteger(2);$record.Dispose()};$view.Dispose()
    if($seq.FitEasySelectLanguage -ge $seq.CostInitialize -or $seq.FitEasyCloseLocalized -ge $seq.ExecuteAction){throw 'Incorrect action ordering'}
    $view=$db.OpenView('SELECT `Action`,`Condition`,`Sequence` FROM `InstallExecuteSequence`');$view.Execute();$execute=@{};$conditions=@{}
    while($record=$view.Fetch()){$execute[$record.GetString(1)]=$record.GetInteger(3);$conditions[$record.GetString(1)]=$record.GetString(2);$record.Dispose()};$view.Dispose()
    if($execute.FitEasyAskDeleteData -le $execute.InstallValidate -or $execute.FitEasyAskDeleteData -ge $execute.InstallInitialize -or $execute.FitEasyDeleteData -le $execute.InstallFinalize){throw 'Unsafe uninstall cleanup ordering'}
    if($conditions.FitEasyDeleteData -ne 'REMOVE="ALL" AND NOT UPGRADINGPRODUCTCODE AND FITEASYDELETEDATA="1"'){throw 'Uninstall cleanup condition changed'}
    if(![Localizer]::IsDataTarget('C:\Probe\FitEasy','C:\Probe') -or [Localizer]::IsDataTarget('C:\Probe','C:\Probe') -or [Localizer]::IsDataTarget('C:\Else\FitEasy','C:\Probe') -or [Localizer]::IsDataTarget('C:\Probe\FitEasy\..','C:\Probe')){throw 'Uninstall target scope mismatch'}
    Write-Output 'PASS uninstall translations, first-choice guard, upgrade/repair exclusions, post-finalize cleanup and target scope. No user data was deleted.'
    $view=$db.OpenView('SELECT `Value` FROM `Property` WHERE `Property` = ''REINSTALLMODE''');$view.Execute();$record=$view.Fetch()
    try {if(!$record -or $record.GetString(1) -ne 'amus'){throw 'REINSTALLMODE is not amus'}}finally {if($record){$record.Dispose()};$view.Dispose()}
    Write-Output 'PASS REINSTALLMODE=amus: an update writes every file anew.'
}finally {$db.Dispose()}
if($Exe){
    $Exe=[IO.Path]::GetFullPath($Exe)
    $bootstrap=[Reflection.Assembly]::LoadFrom($Exe)
    $stream=$bootstrap.GetManifestResourceStream('FitEasy.msi')
    try {$hash=[Security.Cryptography.SHA256]::Create();$embedded=[Convert]::ToBase64String($hash.ComputeHash($stream));$fileStream=[IO.File]::OpenRead($Msi);try {$actual=[Convert]::ToBase64String($hash.ComputeHash($fileStream))}finally {$fileStream.Dispose()};if($actual -ne $embedded){throw 'EXE contains a different MSI'}}finally {$stream.Dispose();$hash.Dispose()}
    $process=Start-Process -FilePath $Exe -ArgumentList '/QUIET','/invalid' -WindowStyle Hidden -Wait -PassThru
    if($process.ExitCode -ne 1603){throw 'Silent invalid-argument exit code differs'}
    Write-Output 'PASS EXE embeds exact final MSI; quiet argument validation returns 1603 without opening UI or installation.'
}
Write-Output "PASS: ten languages, regional variants, two unsupported fallbacks and real Windows UI language ($([LanguagePolicy]::SystemLanguage())); Unicode MSI properties, checkbox and action ordering. No install/execute sequence was run."
