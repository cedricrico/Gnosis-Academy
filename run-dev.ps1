param(
    [int]$Port = 8080,
    [string]$Profile = "mysql"
)

$listenLines = netstat -ano | Select-String "LISTENING" | Select-String (":" + $Port)
foreach ($line in $listenLines) {
    $parts = ($line.ToString() -split "\s+") | Where-Object { $_ -ne "" }
    $pid = [int]$parts[-1]
    if ($pid -gt 0) {
        try {
            Stop-Process -Id $pid -Force -ErrorAction Stop
            Write-Host "Stopped process on port $Port (PID $pid)"
        } catch {
            Write-Host "Could not stop PID $pid on port $Port: $($_.Exception.Message)"
        }
    }
}

& .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=$Profile"
