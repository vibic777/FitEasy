param([string]$BaseMsi,[string]$OutputMsi,[string]$OutputExe,[string]$WixDir,[string]$JdkHome,[string]$SourceDir,[string]$WorkDir,[string]$Version='0.0.0',[int]$BuildNumber=0)
$ErrorActionPreference='Stop'
$utf8=New-Object Text.UTF8Encoding($false)
New-Item -ItemType Directory -Force -Path $WorkDir | Out-Null
if($Version -notmatch '^\d+\.\d+\.\d+$' -or $BuildNumber -lt 0 -or $BuildNumber -gt 65535){throw 'Invalid installer version/build'}
$versionSource=Join-Path $WorkDir 'AssemblyInfo.cs'
[IO.File]::WriteAllText($versionSource,"using System.Reflection;[assembly: AssemblyVersion(`"$Version.$BuildNumber`")][assembly: AssemblyFileVersion(`"$Version.$BuildNumber`")][assembly: AssemblyInformationalVersion(`"$Version+$BuildNumber`")][assembly: AssemblyProduct(`"FitEasy`")][assembly: AssemblyCompany(`"Vibic777`")]",$utf8)
$null=[Reflection.Assembly]::LoadFrom((Join-Path $WixDir 'Microsoft.Deployment.WindowsInstaller.dll'))
$assembly=[Reflection.Assembly]::LoadFrom((Join-Path $WixDir 'WixUIExtension.dll'))
$stream=$assembly.GetManifestResourceStream('Microsoft.Tools.WindowsInstallerXml.Extensions.Data.ui.wixlib')
$memory=New-Object IO.MemoryStream
try {$stream.CopyTo($memory);$content=[Text.Encoding]::UTF8.GetString($memory.ToArray());[xml]$xml=$content.Substring($content.IndexOf('<?xml'))}finally {$memory.Dispose();$stream.Dispose()}
$cultures=@{en='en-us';ru='ru-ru';fr='fr-fr';de='de-de';es='es-es';it='it-it';zh='zh-cn';ja='ja-jp';ko='ko-kr'}
$native=@{}
foreach($lang in $cultures.Keys){
    $native[$lang]=@{}
    $node=$xml.SelectNodes("//*[local-name()='WixLocalization']") | Where-Object {$_.Culture -eq $cultures[$lang]} | Select-Object -First 1
    if(!$node){throw "Missing WiX culture $lang"}
    foreach($item in $node.ChildNodes){if($item.Id){$native[$lang][$item.Id]=$item.InnerText}}
}
$native.be=@{}
$be=Get-Content -LiteralPath (Join-Path $SourceDir 'be.json') -Raw -Encoding UTF8 | ConvertFrom-Json
foreach($p in $be.PSObject.Properties){$native.be[$p.Name]=$p.Value}
foreach($lang in $native.Keys){
    foreach($prefix in @('FatalError','UserExit')) {
        $native[$lang][$prefix+'Combined']=if($lang -eq 'be'){$native.be[$prefix+'Description']}else {$native[$lang][$prefix+'Description1']+' '+$native[$lang][$prefix+'Description2']}
    }
    $native[$lang]['OutOfRbCombined']=$native[$lang]['OutOfRbDiskDlgText']+' '+$native[$lang]['OutOfRbDiskDlgText2']
}
$own=Get-Content -LiteralPath (Join-Path $SourceDir 'own.json') -Raw -Encoding UTF8 | ConvertFrom-Json
function Normalize([string]$text){[regex]::Replace(($text -replace '^\{\\[^}]+\}',''),'\s+',' ').Trim()}
foreach($id in @($native.en.Keys)) {
    if(!$native.be.ContainsKey($id)) {
        foreach($alias in @($native.be.Keys)) {
            if($native.en.ContainsKey($alias) -and (Normalize $native.en[$alias]) -eq (Normalize $native.en[$id])) {$native.be[$id]=$native.be[$alias];break}
        }
    }
}
$english=@{}
foreach($id in $native.en.Keys){$english[(Normalize $native.en[$id])]=$id}
$languages=@('en','ru','be','fr','de','es','it','zh','ja','ko')
$ownIndex=@{}
for($i=0;$i -lt $own.keys.Count;$i++){$ownIndex[(Normalize $own.keys[$i])]=$i}
foreach($lang in $languages){if($own.$lang.Count -ne $own.keys.Count){throw "Own strings incomplete: $lang"}}
$translations=@{}
foreach($lang in $languages){$translations[$lang]=@{};for($i=0;$i -lt $own.keys.Count;$i++){if($own.keys[$i] -like 'MSG_*'){$translations[$lang][$own.keys[$i]]=$own.$lang[$i]}}}
$failures=Get-Content -LiteralPath (Join-Path $SourceDir 'failures.json') -Raw -Encoding UTF8 | ConvertFrom-Json
 $removeData=Get-Content -LiteralPath (Join-Path $SourceDir 'remove-data.json') -Raw -Encoding UTF8 | ConvertFrom-Json
 foreach($lang in $languages){if(!$removeData.$lang){throw "Missing removal text: $lang"};$translations[$lang]['MSG_DELETE_DATA']=$removeData.$lang}
foreach($lang in $languages){if(!$failures.$lang){throw "Missing failure message: $lang"};$translations[$lang]['MSG_FAILURE']=$failures.$lang}
$script:counter=0
function Add-Translation([string]$text,[string]$property){
    $normalized=Normalize $text
    if($ownIndex.ContainsKey($normalized)){$index=$ownIndex[$normalized];foreach($lang in $languages){$translations[$lang][$property]=Normalize $own.$lang[$index]};return}
    if(!$english.ContainsKey($normalized)){throw "Unmapped installer string: $text"}
    $id=$english[$normalized]
    foreach($lang in $languages){
        if(!$native[$lang].ContainsKey($id)){throw "Missing $lang translation: $id"}
        $translations[$lang][$property]=Normalize $native[$lang][$id]
    }
}
Copy-Item -LiteralPath $BaseMsi -Destination $OutputMsi -Force
$db=New-Object Microsoft.Deployment.WindowsInstaller.Database($OutputMsi,[Microsoft.Deployment.WindowsInstaller.DatabaseOpenMode]::Transact)
function Bound-Query([string]$sql,[object[]]$values){
    $view=$db.OpenView($sql);$params=New-Object Microsoft.Deployment.WindowsInstaller.Record($values.Count)
    try {for($i=0;$i -lt $values.Count;$i++){if($values[$i] -is [int]){$params.SetInteger($i+1,$values[$i])}else {$params.SetString($i+1,[string]$values[$i])}};$view.Execute($params);return ,$view}finally {$params.Dispose()}
}
function Bound-Change([string]$sql,[object[]]$values){$view=Bound-Query $sql $values;$view.Dispose()}
try {
    foreach($spec in @(@('Control','Dialog_','Control','Text'),@('Dialog','Dialog','Title'))){
        $table=$spec[0];$columns=$spec[1..($spec.Count-1)];$last=$columns.Count
        $query='SELECT '+(($columns | ForEach-Object {'`'+$_+'`'}) -join ',')+' FROM `'+$table+'`'
        $view=$db.OpenView($query);$view.Execute()
        try {while($record=$view.Fetch()){
            try {
                $text=$record.GetString($last)
                # Values/graphics/embedded RTF are not labels; preserve MSI bindings.
                if(!$text -or $text -match '^(WixUI_|\[|\{\\rtf|\{\d+\})'){
                    if($text -ne '[ProductName] Setup' -and $text -notlike '[[]ProductName[]]*Setup Wizard*' -and $text -notlike '[[]ProductName[]]*setup was interrupted*'){continue}
                }
                $script:counter++;$property='FELOC'+$script:counter.ToString('D4')
                Add-Translation $text $property
                $style=[regex]::Match($text,'^\{\\[^}]+\}').Value
                if($text.Contains('[INSTALLDIR]')) {
                    foreach($lang in $languages) {
                        $parts=$translations[$lang][$property] -split '\[INSTALLDIR\]',2
                        if($parts.Count -ne 2){throw "Missing directory placeholder: $lang"}
                        $translations[$lang][$property+'A']=$parts[0];$translations[$lang][$property+'B']=$parts[1]
                        $translations[$lang].Remove($property)
                    }
                    $record.SetString($last,$style+'['+$property+'A][INSTALLDIR]['+$property+'B]')
                }else {$record.SetString($last,$style+'['+$property+']')}
                $view.Modify([Microsoft.Deployment.WindowsInstaller.ViewModifyMode]::Update,$record)
            }finally {$record.Dispose()}
        }}finally {$view.Dispose()}
    }
    Add-Translation 'Launch FitEasy' 'WIXUI_EXITDIALOGOPTIONALCHECKBOXTEXT'
    $view=$db.OpenView('SELECT `Property`,`Value` FROM `Property`');$view.Execute()
    try {while($record=$view.Fetch()) {
        try {$name=$record.GetString(1);$text=$record.GetString(2);$normalized=Normalize $text
            if($english.ContainsKey($normalized) -or $ownIndex.ContainsKey($normalized)){Add-Translation $text $name}
        }finally {$record.Dispose()}
    }}finally {$view.Dispose()}
    foreach($p in $translations.en.Keys){if($p -like 'MSG_*'){continue};$view=Bound-Query 'SELECT `Property`, `Value` FROM `Property` WHERE `Property` = ?' @($p);$record=$view.Fetch();if($record){$record.SetString(2,$translations.en[$p]);$view.Modify([Microsoft.Deployment.WindowsInstaller.ViewModifyMode]::Update,$record);$record.Dispose()}else {Bound-Change 'INSERT INTO `Property` (`Property`, `Value`) VALUES (?, ?)' @($p,$translations.en[$p])};$view.Dispose()}
    [IO.File]::WriteAllText((Join-Path $WorkDir 'languages.json'),($translations|ConvertTo-Json -Depth 8),$utf8)
    $compiler=Join-Path $env:WINDIR 'Microsoft.NET\Framework64\v4.0.30319\csc.exe'
    $managed=Join-Path $WorkDir 'FitEasy.Localizer.dll'
    & $compiler /nologo /target:library /platform:x64 /codepage:65001 "/out:$managed" "/reference:$(Join-Path $WixDir 'Microsoft.Deployment.WindowsInstaller.dll')" /reference:System.Web.Extensions.dll /reference:System.Windows.Forms.dll "/resource:$(Join-Path $WorkDir 'languages.json'),languages.json" (Join-Path $SourceDir 'Localizer.cs') (Join-Path $SourceDir 'LanguagePolicy.cs') $versionSource
    if($LASTEXITCODE -ne 0){throw 'Localizer compilation failed'}
    $packed=Join-Path $WorkDir 'FitEasy.Localizer.CA.dll'
    & (Join-Path $WixDir 'sdk\MakeSfxCA.exe') $packed (Join-Path $WixDir 'sdk\x64\sfxca.dll') $managed (Join-Path $SourceDir 'CustomAction.config') (Join-Path $WixDir 'Microsoft.Deployment.WindowsInstaller.dll')
    if($LASTEXITCODE -ne 0){throw 'Custom action packing failed'}
    $view=$db.OpenView('INSERT INTO `Binary` (`Name`, `Data`) VALUES (?, ?)');$record=New-Object Microsoft.Deployment.WindowsInstaller.Record(2);$record.SetString(1,'FitEasyLocalizer');$record.SetStream(2,$packed);$view.Execute($record);$record.Dispose();$view.Dispose()
    Bound-Change 'INSERT INTO `CustomAction` (`Action`,`Type`,`Source`,`Target`) VALUES (?, ?, ?, ?)' @('FitEasySelectLanguage',1,'FitEasyLocalizer','SelectLanguage')
    Bound-Change 'INSERT INTO `CustomAction` (`Action`,`Type`,`Source`,`Target`) VALUES (?, ?, ?, ?)' @('FitEasyCloseLocalized',1,'FitEasyLocalizer','CloseRunning')
    Bound-Change 'INSERT INTO `CustomAction` (`Action`,`Type`,`Source`,`Target`) VALUES (?, ?, ?, ?)' @('FitEasyAskDeleteData',1,'FitEasyLocalizer','AskDeleteData')
    Bound-Change 'INSERT INTO `CustomAction` (`Action`,`Type`,`Source`,`Target`) VALUES (?, ?, ?, ?)' @('FitEasySetDeleteData',51,'FITEASYDELETEPATH','[LocalAppDataFolder]FitEasy')
    Bound-Change 'INSERT INTO `CustomAction` (`Action`,`Type`,`Source`,`Target`) VALUES (?, ?, ?, ?)' @('FitEasyDeleteData',1,'FitEasyLocalizer','DeleteData')
    Bound-Change 'INSERT INTO `InstallExecuteSequence` (`Action`,`Condition`,`Sequence`) VALUES (?, ?, ?)' @('FitEasyAskDeleteData','REMOVE="ALL" AND NOT UPGRADINGPRODUCTCODE',1450)
    foreach($item in @(@('FitEasySetDeleteData',6501),@('FitEasyDeleteData',6700))){Bound-Change 'INSERT INTO `InstallExecuteSequence` (`Action`,`Condition`,`Sequence`) VALUES (?, ?, ?)' @($item[0],'REMOVE="ALL" AND NOT UPGRADINGPRODUCTCODE AND FITEASYDELETEDATA="1"',[int]$item[1])}
    Bound-Change 'INSERT INTO `InstallUISequence` (`Action`,`Condition`,`Sequence`) VALUES (?, ?, ?)' @('FitEasySelectLanguage','',20)
    Bound-Change 'INSERT INTO `InstallUISequence` (`Action`,`Condition`,`Sequence`) VALUES (?, ?, ?)' @('FitEasyCloseLocalized','NOT REMOVE="ALL"',1299)
    Bound-Change 'DELETE FROM `InstallUISequence` WHERE `Action` = ?' @('FitEasyPowerShell')
    Bound-Change 'DELETE FROM `InstallUISequence` WHERE `Action` = ?' @('FitEasyCloseRunning')
    Bound-Change 'DELETE FROM `CustomAction` WHERE `Action` = ?' @('FitEasyPowerShell')
    Bound-Change 'DELETE FROM `CustomAction` WHERE `Action` = ?' @('FitEasyCloseRunning')
    Bound-Change 'INSERT INTO `Property` (`Property`,`Value`) VALUES (?, ?)' @('FITEASYBUILD',[string]$BuildNumber)
    Bound-Change 'INSERT INTO `Property` (`Property`,`Value`) VALUES (?, ?)' @('FITEASYCONTRIBUTOR','Vibic777')
    # Every file is written anew (D122): an update must not rely on unchanged libraries the old version's removal may still delete after a restart.
    Bound-Change 'DELETE FROM `Property` WHERE `Property` = ?' @('REINSTALLMODE')
    Bound-Change 'INSERT INTO `Property` (`Property`,`Value`) VALUES (?, ?)' @('REINSTALLMODE','amus')
    $db.Commit()
}finally {$db.Dispose()}
& $compiler /nologo /target:winexe /platform:x64 /codepage:65001 "/out:$OutputExe" /reference:System.Windows.Forms.dll /reference:System.Web.Extensions.dll "/win32icon:$(Join-Path $SourceDir '..\src\jvmMain\resources\icons\fiteasy.ico')" "/resource:$(Join-Path $WorkDir 'languages.json'),languages.json" "/resource:$OutputMsi,FitEasy.msi" (Join-Path $SourceDir 'Bootstrapper.cs') (Join-Path $SourceDir 'LanguagePolicy.cs') $versionSource
if($LASTEXITCODE -ne 0){throw 'Bootstrapper compilation failed'}
Write-Output "Localized $counter labels, ten languages; EXE embeds the final MSI."
