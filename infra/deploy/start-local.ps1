param([switch]$NoBuild)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "../..")).Path
$localRoot = Join-Path $repoRoot ".local"
$logRoot = Join-Path $localRoot "logs"
$dataRoot = Join-Path $localRoot "data"
$storageRoot = Join-Path $localRoot "storage"
$processFile = Join-Path $localRoot "stage-processes.json"

foreach ($directory in @($localRoot, $logRoot, $dataRoot, $storageRoot)) {
  New-Item -ItemType Directory -Path $directory -Force | Out-Null
}

$verificationPackages = Join-Path $localRoot "verification-packages"
$basePython = $env:VERIFICATION_BASE_PYTHON
if (!$basePython) {
  $pythonCommand = Get-Command python.exe, python3.exe -ErrorAction SilentlyContinue |
    Where-Object { $_.Source -and $_.Source -notmatch "\\WindowsApps\\" } |
    Select-Object -First 1
  if ($pythonCommand) { $basePython = $pythonCommand.Source }
}
if (!$basePython -and $env:USERPROFILE) {
  $bundledPython = Join-Path $env:USERPROFILE ".cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe"
  if (Test-Path -LiteralPath $bundledPython) { $basePython = $bundledPython }
}
if (!$basePython -or !(Test-Path -LiteralPath $basePython)) {
  throw "Python 3.10+ is required for the pyHanko verification engine. Set VERIFICATION_BASE_PYTHON to its executable."
}
$env:VERIFICATION_PYTHON = $basePython
$env:PYTHONPATH = if ($env:PYTHONPATH) { "$verificationPackages;$env:PYTHONPATH" } else { $verificationPackages }
& $basePython -c "import pyhanko.pdf_utils.reader; import pyhanko.sign.validation" 2>$null
if ($LASTEXITCODE -ne 0) {
  $pipCache = Join-Path $repoRoot ".tools/pip-cache"
  New-Item -ItemType Directory -Path $pipCache -Force | Out-Null
  New-Item -ItemType Directory -Path $verificationPackages -Force | Out-Null
  & $basePython -m pip install --disable-pip-version-check --no-input --cache-dir $pipCache --target $verificationPackages -r (Join-Path $repoRoot "backend/verification/requirements.txt")
  if ($LASTEXITCODE -ne 0) { throw "Could not install the pyHanko verification dependencies." }
}

if (Test-Path -LiteralPath $processFile) {
  $tracked = Get-Content -Raw -LiteralPath $processFile | ConvertFrom-Json
  $running = @($tracked | Where-Object { Get-Process -Id $_.pid -ErrorAction SilentlyContinue })
  if ($running.Count -gt 0) { throw "Local staging is already running. Run infra/deploy/stop-local.ps1 first." }
}

$ports = @(3000, 8080, 8081)
$listeners = foreach ($port in $ports) { Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue }
if ($listeners) {
  $occupied = ($listeners | Select-Object -ExpandProperty LocalPort -Unique) -join ", "
  throw "Ports already in use: $occupied. Stop those services or choose another machine before starting staging."
}

$javaHome = Join-Path $repoRoot ".tools/jdk21/jdk-21.0.12.1+1"
$java = Join-Path $javaHome "bin/java.exe"
$maven = Join-Path $repoRoot ".tools/maven/apache-maven-3.9.16/bin/mvn.cmd"
if (!(Test-Path -LiteralPath $java) -or !(Test-Path -LiteralPath $maven)) {
  throw "The workspace JDK 21 and Maven 3.9.16 runtimes are required. See the local staging setup notes."
}

if (!$NoBuild) {
  $env:JAVA_HOME = $javaHome
  $env:Path = "$javaHome\bin;$env:Path"
  & $maven -B "-Dmaven.repo.local=$(Join-Path $repoRoot '.tools/m2')" -f (Join-Path $repoRoot "backend/pom.xml") verify
  if ($LASTEXITCODE -ne 0) { throw "Backend verification failed; staging was not started." }
  Push-Location (Join-Path $repoRoot "frontend")
  try {
    $env:NEXT_PUBLIC_LOCAL_DEMO = "true"
    $env:NEXT_PUBLIC_API_BASE = ""
    $env:API_PROXY_TARGET = "http://127.0.0.1:8080"
    npm.cmd run build
    if ($LASTEXITCODE -ne 0) { throw "Frontend production build failed; staging was not started." }
  } finally { Pop-Location }
}

$verificationJar = Join-Path $repoRoot "backend/verification/target/verification-1.0.0-SNAPSHOT.jar"
$coreJar = Join-Path $repoRoot "backend/core/target/core-1.0.0-SNAPSHOT.jar"
$frontendRuntime = Join-Path $repoRoot "frontend/.next/standalone/frontend"
$frontendServer = Join-Path $frontendRuntime "server.js"
foreach ($artifact in @($verificationJar, $coreJar, $frontendServer)) {
  if (!(Test-Path -LiteralPath $artifact)) { throw "Missing deployment artifact: $artifact" }
}
$frontendStatic = Join-Path $repoRoot "frontend/.next/static"
$runtimeStatic = Join-Path $frontendRuntime ".next/static"
if (!(Test-Path -LiteralPath $frontendStatic)) { throw "Missing frontend static assets: $frontendStatic" }
New-Item -ItemType Directory -Path $runtimeStatic -Force | Out-Null
Copy-Item -Path (Join-Path $frontendStatic "*") -Destination $runtimeStatic -Recurse -Force

$env:VERIFICATION_SERVICE_TOKEN = "local-stage-only"
$env:PORT = "3000"
$env:HOSTNAME = "127.0.0.1"
$verification = Start-Process -FilePath $java -ArgumentList @("-jar", $verificationJar) -WorkingDirectory $repoRoot -PassThru -WindowStyle Hidden -RedirectStandardOutput (Join-Path $logRoot "verification.out.log") -RedirectStandardError (Join-Path $logRoot "verification.err.log")
$core = Start-Process -FilePath $java -ArgumentList @("-jar", $coreJar, "--spring.profiles.active=local") -WorkingDirectory $repoRoot -PassThru -WindowStyle Hidden -RedirectStandardOutput (Join-Path $logRoot "core.out.log") -RedirectStandardError (Join-Path $logRoot "core.err.log")
$frontend = Start-Process -FilePath (Get-Command node.exe).Source -ArgumentList @($frontendServer) -WorkingDirectory $frontendRuntime -PassThru -WindowStyle Hidden -RedirectStandardOutput (Join-Path $logRoot "frontend.out.log") -RedirectStandardError (Join-Path $logRoot "frontend.err.log")

$tracked = @(
  [pscustomobject]@{ service = "verification"; pid = $verification.Id },
  [pscustomobject]@{ service = "core"; pid = $core.Id },
  [pscustomobject]@{ service = "frontend"; pid = $frontend.Id }
)
$tracked | ConvertTo-Json | Set-Content -LiteralPath $processFile -Encoding utf8

function Wait-HttpReady([string]$Uri, [string]$Service, [int]$ProcessId) {
  for ($attempt = 0; $attempt -lt 60; $attempt++) {
    if (!(Get-Process -Id $ProcessId -ErrorAction SilentlyContinue)) { throw "$Service exited during startup. Check .local/logs." }
    try {
      $response = Invoke-WebRequest -Uri $Uri -UseBasicParsing -TimeoutSec 2
      if ($response.StatusCode -eq 200) { return }
    } catch { }
    Start-Sleep -Milliseconds 1000
  }
  throw "$Service did not become healthy within 60 seconds. Check .local/logs."
}

try {
  Wait-HttpReady "http://127.0.0.1:8081/internal/health" "Verification" $verification.Id
  Wait-HttpReady "http://127.0.0.1:8080/actuator/health" "Core" $core.Id
  Wait-HttpReady "http://127.0.0.1:3000/login" "Portal" $frontend.Id
} catch {
  & (Join-Path $PSScriptRoot "stop-local.ps1")
  throw
}

Write-Output "Local staging is ready: http://127.0.0.1:3000"
Write-Output "Core health: http://127.0.0.1:8080/actuator/health"
Write-Output "Verification is loopback only: http://127.0.0.1:8081/internal/health"
Write-Output "Logs: $logRoot"
