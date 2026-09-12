param([switch]$NoBrowser, [int]$Port = 8766)
$ErrorActionPreference = 'Stop'
$editorDirectory = $PSScriptRoot
$configFile = Join-Path $editorDirectory 'config.json'
$projectDirectory = if (Test-Path -LiteralPath $configFile) {
    (Get-Content -Raw -LiteralPath $configFile | ConvertFrom-Json).project
} else { [IO.Path]::GetFullPath((Join-Path $editorDirectory '..\..')) }
$catalogFile = Join-Path $projectDirectory 'translations\catalog.json'
if (-not (Test-Path -LiteralPath $catalogFile)) { throw "Translation catalog not found: $catalogFile" }
$editorUrl = "http://127.0.0.1:$Port"
function Read-Editor {
    try { Invoke-RestMethod -Uri "$editorUrl/api/catalog" -TimeoutSec 1 } catch { $null }
}
$existingEditor = Read-Editor
if ($existingEditor -and ([IO.Path]::GetFullPath($existingEditor.project) -ne [IO.Path]::GetFullPath($projectDirectory))) {
    throw "Port $Port is already serving a different project. Run Start.ps1 -Port 8767."
}
if (-not $existingEditor) {
    $pythonProgram = $null
    $pythonPrefix = @()
    foreach ($candidate in @('C:\dev\exc\python\p311\python.exe', 'python.exe', 'py.exe')) {
        $resolvedProgram = Get-Command $candidate -ErrorAction SilentlyContinue
        if ($resolvedProgram) {
            $pythonProgram = $resolvedProgram.Source
            if ($candidate -eq 'py.exe') { $pythonPrefix = @('-3') }
            break
        }
    }
    if (-not $pythonProgram) { throw 'Python 3.10 or newer is required. No additional packages are needed.' }
    $logDirectory = Join-Path $editorDirectory 'logs'
    New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null
    $arguments = $pythonPrefix + @('-X', 'utf8', '-u', ('"' + (Join-Path $editorDirectory 'server.py') + '"'), '--project', ('"' + $projectDirectory + '"'), '--port', "$Port")
    $serverProcess = Start-Process -FilePath $pythonProgram -ArgumentList $arguments -WorkingDirectory $editorDirectory -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $logDirectory "server-$Port.log") -RedirectStandardError (Join-Path $logDirectory "server-$Port.error.log")
    for ($attempt = 0; $attempt -lt 25; $attempt++) {
        Start-Sleep -Milliseconds 200
        $existingEditor = Read-Editor
        if ($existingEditor) { break }
        if ($serverProcess.HasExited) { break }
    }
    if (-not $existingEditor) { throw "The editor could not start. See $logDirectory\server-$Port.error.log" }
}
Write-Host "Translation editor: $editorUrl"
Write-Host "Project: $projectDirectory"
if (-not $NoBrowser) { Start-Process $editorUrl }
