<#
.SYNOPSIS
  Deploys server/ + infra/gcp/ to the nutri-api VM (ADR-013) and smoke-tests /health.

.EXAMPLE
  ./tools/deploy-gcp.ps1          # code only
  ./tools/deploy-gcp.ps1 -Env     # code + repo-root .env (never read or printed here)
#>
param(
    [switch]$Env,
    [string]$Project = (gcloud config get-value project 2>$null),
    [string]$Zone = "us-east1-b",
    [string]$Region = "us-east1",
    [string]$Vm = "nutri-api",
    [string]$Address = "nutri-api-ip"
)

# Continue, not Stop: Windows PowerShell 5.1 turns native stderr (docker progress) into
# terminating errors. Every native call checks $LASTEXITCODE instead.
$ErrorActionPreference = "Continue"
$root = Split-Path -Parent $PSScriptRoot
if (-not $Project) { throw "No project. Pass -Project or run: gcloud config set project <id>" }

$ip = (gcloud compute addresses describe $Address --region $Region --project $Project --format "value(address)").Trim()
$publicHost = ($ip -replace '\.', '-') + ".sslip.io"
Write-Host "project=$Project vm=$Vm host=$publicHost"

$common = @("--zone", $Zone, "--project", $Project, "--tunnel-through-iap", "--quiet")

function Invoke-Remote([string]$cmd) {
    gcloud compute ssh $Vm @common --command $cmd
    if ($LASTEXITCODE -ne 0) { throw "remote command failed: $cmd" }
}

# 1. Package server/ (no tests, venv, caches) + infra/gcp/.
$pkg = Join-Path $env:TEMP "nutri-deploy.tgz"
if (Test-Path $pkg) { Remove-Item $pkg -Force }
Push-Location $root
try {
    tar -czf $pkg --exclude=tests --exclude=.venv --exclude=__pycache__ --exclude=.pytest_cache server infra/gcp
    if ($LASTEXITCODE -ne 0) { throw "tar failed" }
} finally { Pop-Location }

# 2. Upload.
gcloud compute scp $pkg "${Vm}:/tmp/nutri-deploy.tgz" @common
if ($LASTEXITCODE -ne 0) { throw "scp of package failed" }
Remove-Item $pkg -Force

# 3. Optional .env: copied as-is, CRLF stripped on the VM, mode 600.
if ($Env) {
    $envFile = Join-Path $root ".env"
    if (-not (Test-Path $envFile)) { throw ".env not found at repo root" }
    gcloud compute scp $envFile "${Vm}:/tmp/nutri.env" @common
    if ($LASTEXITCODE -ne 0) { throw "scp of .env failed" }
    Invoke-Remote "sudo mkdir -p /opt/nutri && sudo install -m 600 -o root -g root /tmp/nutri.env /opt/nutri/.env && sudo sed -i 's/\r$//' /opt/nutri/.env && rm -f /tmp/nutri.env"
}

# 4. Replace code and (re)start the stack.
Invoke-Remote ("sudo mkdir -p /opt/nutri && sudo rm -rf /opt/nutri/server /opt/nutri/infra && " +
    "sudo tar -xzf /tmp/nutri-deploy.tgz -C /opt/nutri && rm -f /tmp/nutri-deploy.tgz && " +
    "test -f /opt/nutri/.env && cd /opt/nutri && " +
    # Compose interpolation file: lets plain `docker compose ps|logs` work on the VM.
    "echo PUBLIC_HOST=$publicHost | sudo tee infra/gcp/.env >/dev/null && " +
    "sudo docker compose -f infra/gcp/compose.yml up -d --build --remove-orphans")

# 5. Smoke: /health over HTTPS (first run waits for the certificate).
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$url = "https://$publicHost/health"
for ($i = 1; $i -le 24; $i++) {
    try {
        $r = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 10
        if ($r.StatusCode -eq 200) { Write-Host "OK $url -> 200 $($r.Content)"; exit 0 }
    } catch { Start-Sleep -Seconds 5 }
}
throw "health check failed: $url"
