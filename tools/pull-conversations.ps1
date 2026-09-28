<#
.SYNOPSIS
  Reads the dev conversation log (ADR-015) from the nutri-api VM.

.EXAMPLE
  ./tools/pull-conversations.ps1                 # last 20 lines
  ./tools/pull-conversations.ps1 -Tail 50
  ./tools/pull-conversations.ps1 -RequestId s6-smoke
  ./tools/pull-conversations.ps1 -Download       # copies conversations.jsonl* to logs/
#>
param(
    [int]$Tail = 20,
    [string]$RequestId,
    [switch]$Download,
    [string]$Project = (gcloud config get-value project 2>$null),
    [string]$Zone = "us-east1-b",
    [string]$Vm = "nutri-api"
)

# Continue, not Stop: Windows PowerShell 5.1 turns native stderr into terminating errors.
$ErrorActionPreference = "Continue"
$root = Split-Path -Parent $PSScriptRoot
if (-not $Project) { throw "No project. Pass -Project or run: gcloud config set project <id>" }
$common = @("--zone", $Zone, "--project", $Project, "--tunnel-through-iap", "--quiet")
$log = "/opt/nutri/logs/conversations.jsonl"

if ($Download) {
    $dest = Join-Path $root "logs"
    New-Item -ItemType Directory -Force $dest | Out-Null
    gcloud compute ssh $Vm @common --command "sudo tar -czf /tmp/nutri-logs.tgz -C /opt/nutri/logs . && sudo chmod 644 /tmp/nutri-logs.tgz"
    if ($LASTEXITCODE -ne 0) { throw "packing logs failed" }
    $tgz = Join-Path $dest "nutri-logs.tgz"
    gcloud compute scp "${Vm}:/tmp/nutri-logs.tgz" $tgz @common
    if ($LASTEXITCODE -ne 0) { throw "download failed" }
    gcloud compute ssh $Vm @common --command "sudo rm -f /tmp/nutri-logs.tgz" | Out-Null
    tar -xzf $tgz -C $dest
    Remove-Item $tgz -Force
    Get-ChildItem $dest -Filter "conversations.jsonl*" | Select-Object Name, Length
    exit 0
}

if ($RequestId) {
    if ($RequestId -notmatch '^[A-Za-z0-9-]{1,64}$') { throw "invalid request id" }
    # No double quotes in the remote command: PS 5.1 drops them when calling native programs.
    # Coarse grep on the VM, exact request_id match here.
    $cmd = "sudo sh -c 'cat $log.5 $log.4 $log.3 $log.2 $log.1 $log 2>/dev/null' | grep -F -- $RequestId || true"
    gcloud compute ssh $Vm @common --command $cmd | ForEach-Object {
        try { if (($_ | ConvertFrom-Json).request_id -eq $RequestId) { $_ } } catch {}
    }
    exit 0
}
gcloud compute ssh $Vm @common --command "sudo tail -n $Tail $log"
