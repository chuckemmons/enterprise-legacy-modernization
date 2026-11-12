param(
    [ValidateSet('Start', 'Stop')][string]$Action = 'Start',
    [string]$Nginx = $env:ROUTING_NGINX,
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$Config,
    [string]$Prefix
)

$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$moduleDirectory = Join-Path $repositoryRoot 'routing-gateway'
if (-not $Config) { $Config = Join-Path $moduleDirectory 'routes-intellij.json' }
if (-not $Prefix) { $Prefix = Join-Path $moduleDirectory 'target/intellij-runtime' }
$Prefix = [IO.Path]::GetFullPath($Prefix)
if (-not $Nginx) {
    $installedNginx = Get-Command nginx.exe -ErrorAction SilentlyContinue
    $cachedNginx = Join-Path $moduleDirectory 'target/nginx-1.30.6/nginx.exe'
    if ($installedNginx) { $Nginx = $installedNginx.Source }
    elseif (Test-Path -LiteralPath $cachedNginx) { $Nginx = $cachedNginx }
    else { throw 'Set ROUTING_NGINX to the full nginx.exe path, or pass -Nginx.' }
}
$Nginx = (Resolve-Path -LiteralPath $Nginx).Path
$pidFile = Join-Path $Prefix 'nginx.pid'
$running = $false
if (Test-Path -LiteralPath $pidFile) {
    $processText = [IO.File]::ReadAllText($pidFile).Trim()
    $gatewayProcessId = 0
    if (-not [int]::TryParse($processText, [ref]$gatewayProcessId) -or $gatewayProcessId -le 0) {
        throw "Invalid gateway PID file: $pidFile. Inspect it before removing it."
    }
    $gatewayProcess = Get-CimInstance Win32_Process -Filter "ProcessId = $gatewayProcessId"
    if ($gatewayProcess) {
        $expectedPrefix = $Prefix.Replace('\', '/') + '/'
        if (-not [string]::Equals($gatewayProcess.ExecutablePath, $Nginx, [StringComparison]::OrdinalIgnoreCase) -or
            -not $gatewayProcess.CommandLine.Replace('\', '/').Contains($expectedPrefix)) {
            throw 'Gateway PID belongs to another process or runtime. No action taken.'
        }
        $running = $true
    } else {
        # Remove only the verified, stale PID file; never stop a process by an unverified PID.
        [IO.File]::Delete($pidFile)
    }
}
$nginxPrefix = $Prefix.Replace('\', '/') + '/'
if ($Action -eq 'Stop') {
    if ($running) {
        & $Nginx -p $nginxPrefix -c nginx.conf -s quit
        if ($LASTEXITCODE -ne 0) { throw 'Gateway stop failed.' }
        $stopDeadline = [DateTime]::UtcNow.AddSeconds(10)
        while ((Test-Path -LiteralPath $pidFile) -and [DateTime]::UtcNow -lt $stopDeadline) {
            Start-Sleep -Milliseconds 100
        }
        if (Test-Path -LiteralPath $pidFile) { throw 'Gateway has not finished stopping.' }
        Write-Output 'Routing Gateway stopped.'
    } else { Write-Output 'Routing Gateway is already stopped.' }
    return
}

$javaExecutable = if ($JavaHome) { Join-Path $JavaHome 'bin/java.exe' } else { 'java.exe' }
$jar = Join-Path $moduleDirectory 'target/routing-gateway.jar'
if (-not (Test-Path -LiteralPath $jar)) { throw 'Build routing-gateway with Maven package first.' }
if ($running) {
    & $javaExecutable -jar $jar apply --config $Config --prefix $Prefix --nginx $Nginx
    if ($LASTEXITCODE -ne 0) { throw 'Gateway route reload failed.' }
    Write-Output 'Routing Gateway is already running; configuration applied.'
    return
}

& $javaExecutable -jar $jar render --config $Config --prefix $Prefix
if ($LASTEXITCODE -ne 0) { throw 'Gateway render failed.' }
& $Nginx -p $nginxPrefix -c nginx.conf -t
if ($LASTEXITCODE -ne 0) { throw 'Gateway configuration validation failed.' }
# Reserve-check before launch to avoid starting a second gateway on an occupied port.
$routes = Get-Content -LiteralPath $Config -Raw | ConvertFrom-Json
$endpoint = $routes.listen.Split(':')
$portCheck = New-Object Net.Sockets.TcpListener([Net.IPAddress]::Parse($endpoint[0]), [int]$endpoint[1])
try { $portCheck.Start() } finally { $portCheck.Stop() }
Start-Process -FilePath $Nginx -ArgumentList @('-p', ('"' + $nginxPrefix + '"'), '-c', 'nginx.conf') -WindowStyle Hidden
$deadline = [DateTime]::UtcNow.AddSeconds(10)
do {
    if (Test-Path -LiteralPath $pidFile) {
        Write-Output "Routing Gateway started at http://$($routes.listen)$($routes.context)/"
        return
    }
    Start-Sleep -Milliseconds 100
} while ([DateTime]::UtcNow -lt $deadline)
throw 'Gateway did not create its PID file. Inspect the runtime and NGINX configuration.'
