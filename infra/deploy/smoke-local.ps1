$ErrorActionPreference = "Stop"
$base = "http://127.0.0.1"
$frontend = Invoke-WebRequest -Uri "${base}:3000/login" -UseBasicParsing
$core = Invoke-RestMethod -Uri "${base}:8080/actuator/health"
$verification = Invoke-RestMethod -Uri "${base}:8081/internal/health"
$actor = Invoke-RestMethod -Uri "${base}:8080/api/auth/me" -Headers @{ "X-Demo-Role" = "LECTURER" }
$classes = Invoke-RestMethod -Uri "${base}:8080/api/catalog/classes" -Headers @{ "X-Demo-Role" = "LECTURER" }

if ($frontend.StatusCode -ne 200 -or $core.status -ne "UP" -or $actor.role -ne "LECTURER" -or $classes.Count -lt 1) {
  throw "Local staging smoke check failed. Inspect .local/logs."
}
if ($verification.status -ne "UP" -and $verification.Status -ne "UP") { throw "Verification health check failed." }

Write-Output "PASS portal HTTP 200"
Write-Output "PASS core health $($core.status)"
Write-Output "PASS verification health"
Write-Output "PASS local lecturer identity and scoped course catalog ($($classes.Count) classes)"
