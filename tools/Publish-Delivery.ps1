param([Parameter(Mandatory=$true)][string]$SourceRevision,[Parameter(Mandatory=$true)][int]$Tests)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$identity=ConvertFrom-StringData ([IO.File]::ReadAllText((Join-Path $root 'version.properties')))
$version=$identity.version
$destination=Join-Path $root "dist/$($identity.date)-$version"
$files=@(
    @{Source='app/build/outputs/apk/debug/app-debug.apk';Name="FitEasy-$version-debug.apk"},
    @{Source='app/build/outputs/apk/release/app-release.apk';Name="FitEasy-$version-release-test.apk"},
    @{Source="desktopApp/build/managed-installer/exe/FitEasy-$version.exe";Name="FitEasy-$version.exe"},
    @{Source="desktopApp/build/managed-installer/msi/FitEasy-$version.msi";Name="FitEasy-$version.msi"},
    @{Source="desktopApp/build/portable/FitEasy-$version-portable.zip";Name="FitEasy-$version-portable.zip"}
)
foreach($variant in @('debug','release')){
    $metadata=Get-Content (Join-Path $root "app/build/outputs/apk/$variant/output-metadata.json") -Raw | ConvertFrom-Json
    foreach($item in $metadata.elements){if($item.versionName -ne $version -or $item.versionCode -ne [int]$identity.build){throw "APK identity mismatch: $variant"}}
}
foreach($file in $files){if(!(Test-Path -LiteralPath (Join-Path $root $file.Source))){throw "Missing artifact: $($file.Source)"}}
# The packaged runtime must be JetBrains Runtime: with any other Java Windows draws its own title strip over the app (D129).
$modules=[Text.Encoding]::GetEncoding(28591).GetString([IO.File]::ReadAllBytes((Join-Path $root 'desktopApp/build/compose/binaries/main/app/FitEasy/runtime/lib/modules')))
if(!$modules.Contains('CustomTitleBar')){throw 'The Windows packages were not built with JetBrains Runtime'}
$null=New-Item -ItemType Directory -Path $destination -Force
$artifacts=foreach($file in $files){
    $target=Join-Path $destination $file.Name
    Copy-Item -LiteralPath (Join-Path $root $file.Source) -Destination $target -Force
    @{file=$file.Name;bytes=(Get-Item -LiteralPath $target).Length;sha256=(Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash.ToLowerInvariant()}
}
$manifest=[ordered]@{source=$SourceRevision;version=$version;build=[int]$identity.build;date=$identity.date;channel='prerelease';contributor='Vibic777';releaseSigning='Android Debug - test signing';installerLanguages=@('en','ru','be','fr','de','es','it','zh','ja','ko');installerLanguagePolicy='Windows user UI language; unsupported language -> en';physicalDatabaseSchema=2;lanProtocol=7;transferVersion=5;backupWriter=5;backupReader=@(1,2,3,4,5);tests=$Tests;artifacts=@($artifacts)}
[IO.File]::WriteAllText((Join-Path $destination 'manifest.json'),($manifest | ConvertTo-Json -Depth 8),(New-Object Text.UTF8Encoding($false)))
Write-Output $destination
