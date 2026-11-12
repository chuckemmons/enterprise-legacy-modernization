param(
    [Parameter(Mandatory = $true)][uri]$BaseUrl,
    [string]$OutputPath = 'target/observability-checks.json'
)

$ErrorActionPreference = 'Stop'
if ($BaseUrl.UserInfo -or $BaseUrl.Query -or $BaseUrl.Fragment) {
    throw 'Use an application base URL without credentials, query or fragment.'
}
$results = @('live', 'ready') | ForEach-Object {
    $checkName = $_
    $timer = [Diagnostics.Stopwatch]::StartNew()
    $healthy = $false
    $requestId = $null
    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri ($BaseUrl.AbsoluteUri.TrimEnd('/') + '/health/' + $checkName) -TimeoutSec 10
        $parsed = $response.Content | ConvertFrom-Json
        $requestId = [string]$response.Headers['X-Request-ID']
        $parsedId = [guid]::Empty
        $healthy = $response.StatusCode -eq 200 -and $parsed.status -eq 'up' -and $parsed.implementation -eq 'legacy' -and
            [string]$response.Headers['X-Implementation'] -eq 'legacy' -and [guid]::TryParse($requestId, [ref]$parsedId)
        if (-not $healthy) { $requestId = $null }
    } catch {
        # Exception text and URLs may include secrets; retain only the failed check result.
        $failedResponse = $_.Exception.Response
        if ($failedResponse) {
            $failedId = [guid]::Empty
            if ([guid]::TryParse([string]$failedResponse.Headers['X-Request-ID'], [ref]$failedId)) {
                $requestId = $failedId.ToString()
            }
        }
    } finally {
        $timer.Stop()
    }
    [ordered]@{
        timestamp = [DateTime]::UtcNow.ToString('o')
        implementation = 'legacy'
        check = $checkName
        requestId = $requestId
        healthy = $healthy
        durationSeconds = $timer.Elapsed.TotalSeconds
    }
}
$directory = Split-Path -Parent $OutputPath
if ($directory) { New-Item -ItemType Directory -Force -Path $directory | Out-Null }
$results | ConvertTo-Json | Set-Content -LiteralPath $OutputPath -Encoding UTF8
if (@($results | Where-Object { -not $_.healthy }).Count -gt 0) {
    throw 'Functional probes failed; inspect the safe result artifact.'
}
Write-Output "Functional probes passed. Results: $OutputPath"
