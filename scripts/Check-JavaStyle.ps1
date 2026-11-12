param(
    [string]$JavaHome = $env:JAVA_HOME,
    [switch]$Format
)

$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$javaExecutable = if ($JavaHome) { Join-Path $JavaHome 'bin/java.exe' } else { 'java' }
$toolDirectory = Join-Path $repositoryRoot 'target/style'
New-Item -ItemType Directory -Force -Path $toolDirectory | Out-Null

# Style tools use a separate modern JDK; the application still builds and runs on Java 8.
$versionOutput = & $javaExecutable -version 2>&1
$versionMatch = [regex]::Match(($versionOutput -join "`n"), 'version "(\d+)')
if (-not $versionMatch.Success -or [int]$versionMatch.Groups[1].Value -lt 21) {
    throw 'Java style checks require JDK 21 or newer. Supply its location with -JavaHome.'
}

function Get-StyleTool([string]$FileName, [string]$DownloadUrl) {
    $destination = Join-Path $toolDirectory $FileName
    if (-not (Test-Path -LiteralPath $destination)) {
        Invoke-WebRequest -Uri $DownloadUrl -OutFile $destination
    }
    return $destination
}

$formatter = Get-StyleTool 'google-java-format-2.0.0-all-deps.jar' `
    'https://repo.maven.apache.org/maven2/com/google/googlejavaformat/google-java-format/2.0.0/google-java-format-2.0.0-all-deps.jar'
$checkstyle = Get-StyleTool 'checkstyle-10.26.1-all.jar' `
    'https://github.com/checkstyle/checkstyle/releases/download/checkstyle-10.26.1/checkstyle-10.26.1-all.jar'

$sourceDirectories = @(
    'legacy-enterprise-case-management/src/main/java',
    'legacy-enterprise-case-management/src/test/java',
    'functional-tests/src',
    'routing-gateway/src'
) | ForEach-Object { Join-Path $repositoryRoot $_ }
$sourceFiles = @($sourceDirectories | ForEach-Object {
    Get-ChildItem -LiteralPath $_ -Recurse -File -Filter '*.java'
} | Sort-Object FullName)
$argumentFile = Join-Path $toolDirectory 'java-files.txt'
$encoding = New-Object System.Text.UTF8Encoding($false)
[IO.File]::WriteAllLines($argumentFile, @($sourceFiles | ForEach-Object {
    $_.FullName.Replace('\', '/')
}), $encoding)

if ($Format) {
    & $javaExecutable -jar $formatter --replace "@$argumentFile"
} else {
    & $javaExecutable -jar $formatter --dry-run --set-exit-if-changed "@$argumentFile"
}
if ($LASTEXITCODE -ne 0) {
    throw 'Google Java formatting failed. Use -Format to apply formatting changes.'
}

$reportPath = Join-Path $toolDirectory 'checkstyle.xml'
& $javaExecutable -jar $checkstyle -c /google_checks.xml -f xml -o $reportPath @sourceDirectories
if ($LASTEXITCODE -ne 0) {
    throw "Checkstyle could not complete. See $reportPath."
}
[xml]$report = Get-Content -LiteralPath $reportPath
$findings = @($report.SelectNodes('/checkstyle/file/error'))
if ($findings.Count -gt 0) {
    foreach ($finding in $findings) {
        Write-Output ('{0}:{1}: {2}' -f $finding.ParentNode.name, $finding.line, $finding.message)
    }
    throw "Google Checkstyle reported $($findings.Count) finding(s)."
}
Write-Output "Google formatting and Checkstyle passed for $($sourceFiles.Count) Java files."
