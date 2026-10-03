$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "../..")).Path
$processFile = Join-Path $repoRoot ".local/stage-processes.json"
if (!(Test-Path -LiteralPath $processFile)) { Write-Output "Local staging is not running."; exit 0 }

$tracked = Get-Content -Raw -LiteralPath $processFile | ConvertFrom-Json
$items = @($tracked)
[array]::Reverse($items)
foreach ($item in $items) {
  $process = Get-Process -Id $item.pid -ErrorAction SilentlyContinue
  if ($process) {
    Stop-Process -Id $item.pid -Force
    Write-Output "Stopped $($item.service) (PID $($item.pid))."
  }
}
Remove-Item -LiteralPath $processFile -Force
