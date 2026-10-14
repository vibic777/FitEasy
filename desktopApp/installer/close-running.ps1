# Runs before installation changes files. Only the selected installed executable is considered.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Windows.Forms
$locations = @(foreach ($hive in @('HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*', 'HKLM:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*')) {
    Get-ItemProperty -Path $hive -ErrorAction SilentlyContinue | Where-Object {
        $_.DisplayName -eq 'FitEasy' -and $_.Publisher -eq 'Vibic777' -and $_.InstallLocation
    } | ForEach-Object { [IO.Path]::GetFullPath((Join-Path $_.InstallLocation 'FitEasy.exe')) }
})
function Running-FitEasy {
    @(Get-Process -Name FitEasy -ErrorAction SilentlyContinue | Where-Object {
        try { [IO.Path]::GetFullPath($_.Path) -in $locations } catch { $false }
    })
}
if ((Running-FitEasy).Count -eq 0) { exit 0 }
$owner = New-Object System.Windows.Forms.Form -Property @{ TopMost = $true }
try {
    while ((Running-FitEasy).Count -gt 0) {
        $answer = [System.Windows.Forms.MessageBox]::Show($owner,
            "FitEasy запущен. Закройте его перед установкой.`n`nДа — запросить штатное закрытие автоматически.`nНет — закрыть вручную, затем нажать Повторить.`nОтмена — остановить установку.",
            'FitEasy', 'YesNoCancel', 'Question')
        if ($answer -eq 'Cancel') { exit 1 }
        if ($answer -eq 'Yes') {
            try {
                $lines = @(Get-Content -LiteralPath (Join-Path $env:LOCALAPPDATA 'FitEasy\instance.port') -TotalCount 2)
                if ($lines.Count -eq 2) {
                    $client = New-Object System.Net.Sockets.TcpClient
                    try {
                        $client.Connect('127.0.0.1', [int]$lines[0]); $client.ReceiveTimeout = 3000
                        $stream = $client.GetStream()
                        $bytes = [Text.Encoding]::ASCII.GetBytes("quit`n$($lines[1])`n")
                        $stream.Write($bytes, 0, $bytes.Length)
                        $reader = New-Object IO.StreamReader($stream)
                        if ($reader.ReadLine() -eq 'ok') {
                            foreach ($process in (Running-FitEasy)) { [void]$process.WaitForExit(15000) }
                        }
                    } finally { $client.Dispose() }
                }
            } catch { }
        }
        if ((Running-FitEasy).Count -gt 0) {
            $retry = [System.Windows.Forms.MessageBox]::Show($owner,
                "FitEasy ещё работает. Сохраните изменения и выберите «Завершить FitEasy» в трее, затем нажмите Повторить.",
                'FitEasy', 'RetryCancel', 'Information')
            if ($retry -eq 'Cancel') { exit 1 }
        }
    }
} finally { $owner.Dispose() }
exit 0
