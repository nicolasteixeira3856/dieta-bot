<#
.SYNOPSIS
  Builds the signed dev release APK, bumps 0.0.N and ships it through Firebase App Distribution (A16).

.DESCRIPTION
  1. Refuses a dirty git tree, a missing key.properties (repo root) or google-services.json (app/src/dev).
  2. Reads VERSION_PATCH from apps/android/version.properties. If tag dev-v0.0.N already exists,
     writes N+1 (never reuses or lowers a number); otherwise ships N as recorded (first run: 0.0.1).
  3. testDevDebugUnitTest, then assembleDevRelease.
  4. Checks the APK: signer CN=Nutri (A9) with apksigner, versionName 0.0.N-dev / versionCode N with aapt2.
  5. firebase appdistribution:distribute to the "owner" group of nutri-bot-dev.
  6. Commit "chore(release): 0.0.N-dev" + tag dev-v0.0.N, push both.
  Any failure before step 6 restores version.properties, so the number is not burned.

.EXAMPLE
  ./tools/distribute-dev.ps1           # build, distribute, commit, tag, push
  ./tools/distribute-dev.ps1 -DryRun   # build and check only; tree left clean
#>
param(
    [switch]$DryRun,
    [string]$FirebaseProject = "nutri-bot-dev",
    [string]$FirebaseApp = "1:823717355877:android:d01b29a0b20b0674bd818c",
    [string]$Group = "owner"
)

# Continue, not Stop: Windows PowerShell 5.1 turns native stderr (Gradle, npx) into terminating
# errors. Every native call checks $LASTEXITCODE instead.
$ErrorActionPreference = "Continue"
$root = Split-Path -Parent $PSScriptRoot
$android = Join-Path $root "apps/android"
$versionFile = Join-Path $android "version.properties"
$consoleUrl = "https://console.firebase.google.com/project/$FirebaseProject/appdistribution/app/android:com.nutri.android.dev/releases"

function Fail([string]$msg) { Write-Host "x $msg" -ForegroundColor Red; exit 1 }

function Read-Patch {
    $line = Get-Content $versionFile | Where-Object { $_ -match '^\s*VERSION_PATCH\s*=' } | Select-Object -First 1
    if (-not $line) { Fail "VERSION_PATCH missing in $versionFile" }
    $value = ($line -split '=', 2)[1].Trim()
    $n = 0
    if (-not [int]::TryParse($value, [ref]$n) -or $n -lt 1) { Fail "VERSION_PATCH must be a positive integer, got '$value'" }
    return $n
}

function Restore-Version {
    git -C $root checkout -- "apps/android/version.properties" 2>$null | Out-Null
}

function Find-BuildTool([string]$name) {
    $sdk = $env:ANDROID_HOME
    if (-not $sdk) { $sdk = $env:ANDROID_SDK_ROOT }
    if (-not $sdk) {
        $local = Join-Path $android "local.properties"
        if (Test-Path $local) {
            $line = Get-Content $local | Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -First 1
            if ($line) { $sdk = ($line -replace '^sdk\.dir=', '') -replace '\\:', ':' -replace '\\\\', '\' }
        }
    }
    if (-not $sdk) { Fail "Android SDK not found (ANDROID_HOME or sdk.dir in local.properties)" }
    $tool = Get-ChildItem (Join-Path $sdk "build-tools") -Directory |
        Sort-Object { [version]($_.Name -replace '[^0-9.].*$', '') } -Descending |
        ForEach-Object { Get-ChildItem $_.FullName -Filter "$name*" -File } |
        Where-Object { $_.BaseName -eq $name } | Select-Object -First 1
    if (-not $tool) { Fail "$name not found in $sdk/build-tools" }
    return $tool.FullName
}

# 1. Preconditions.
$dirty = git -C $root status --porcelain
if ($LASTEXITCODE -ne 0) { Fail "git status failed" }
if ($dirty) { Fail "git tree is dirty: the distributed APK must match a commit. Commit or stash first.`n$($dirty -join "`n")" }
if (-not (Test-Path (Join-Path $root "key.properties"))) { Fail "key.properties missing at the repo root (A9)" }
if (-not (Test-Path (Join-Path $android "app/src/dev/google-services.json"))) { Fail "app/src/dev/google-services.json missing (A11)" }
if (-not (Test-Path $versionFile)) { Fail "$versionFile missing" }

# 2. Version: bump only past a number that was already shipped (tagged).
$n = Read-Patch
git -C $root fetch --tags --quiet 2>$null | Out-Null
$shipped = git -C $root tag --list "dev-v0.0.$n"
if ($shipped) {
    $n = $n + 1
    if (git -C $root tag --list "dev-v0.0.$n") { Fail "tag dev-v0.0.$n already exists: refusing to reuse a number" }
    (Get-Content $versionFile) -replace '^\s*VERSION_PATCH\s*=.*$', "VERSION_PATCH=$n" | Set-Content $versionFile -Encoding ascii
}
$version = "0.0.$n-dev"
$tag = "dev-v0.0.$n"
Write-Host "version $version (versionCode $n)$(if ($DryRun) { ' [dry run]' })"

try {
    # 3. Tests, then the signed release APK.
    & (Join-Path $android "gradlew.bat") -p $android testDevDebugUnitTest --console=plain
    if ($LASTEXITCODE -ne 0) { throw "testDevDebugUnitTest failed: nothing built, version not bumped" }
    & (Join-Path $android "gradlew.bat") -p $android assembleDevRelease --console=plain
    if ($LASTEXITCODE -ne 0) { throw "assembleDevRelease failed" }
    $apk = Get-ChildItem (Join-Path $android "app/build/outputs/apk/dev/release") -Filter "*.apk" |
        Where-Object { $_.Name -notmatch 'unsigned' } | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $apk) { throw "no signed APK in app/build/outputs/apk/dev/release (key.properties loaded?)" }

    # 4. Signer and version inside the APK.
    $certs = & (Find-BuildTool "apksigner") verify --print-certs $apk.FullName 2>&1 | Out-String
    if ($LASTEXITCODE -ne 0) { throw "apksigner verify failed:`n$certs" }
    if ($certs -notmatch 'Signer #1 certificate DN: CN=Nutri') { throw "APK is not signed with the A9 key (CN=Nutri):`n$certs" }
    $badging = & (Find-BuildTool "aapt2") dump badging $apk.FullName 2>&1 | Select-Object -First 1 | Out-String
    if ($badging -notmatch "versionCode='$n'" -or $badging -notmatch "versionName='$([regex]::Escape($version))'") {
        throw "APK version mismatch, expected $version / $($n):`n$badging"
    }
    Write-Host "ok $($apk.Name): CN=Nutri, versionName $version, versionCode $n"

    if ($DryRun) {
        Write-Host "dry run: not distributed, not committed, not pushed"
        return
    }

    # 5. Distribute. Release notes = commits since the previous dev tag.
    $prev = git -C $root describe --tags --abbrev=0 --match "dev-v0.0.*" 2>$null
    $log = if ($prev) { git -C $root log --oneline "$prev..HEAD" } else { git -C $root log --oneline -20 }
    $notes = Join-Path $env:TEMP "dieta-bot-release-notes.txt"
    [IO.File]::WriteAllText($notes, ((@("Dieta Bot $version") + $log) -join "`n"), (New-Object Text.UTF8Encoding $false))
    npx -y firebase-tools@latest appdistribution:distribute $apk.FullName --app $FirebaseApp --groups $Group --release-notes-file $notes --project $FirebaseProject
    if ($LASTEXITCODE -ne 0) { throw "firebase appdistribution:distribute failed" }
    Remove-Item $notes -Force -ErrorAction SilentlyContinue
} catch {
    Restore-Version
    Fail $_.Exception.Message
} finally {
    if ($DryRun) { Restore-Version }
}
if ($DryRun) { exit 0 }

# 6. Record the shipped number: commit (empty on the first run, 0.0.1 is already recorded) + tag + push.
git -C $root add "apps/android/version.properties"
git -C $root commit --allow-empty -m "chore(release): $version" | Out-Null
if ($LASTEXITCODE -ne 0) { Fail "distributed $version but the release commit failed: commit and tag $tag by hand" }
git -C $root tag -a $tag -m "Dieta Bot $version (Firebase App Distribution)"
if ($LASTEXITCODE -ne 0) { Fail "distributed $version but tagging failed: tag $tag by hand" }
git -C $root push origin HEAD
if ($LASTEXITCODE -ne 0) { Fail "push of the release commit failed: run git push origin HEAD" }
git -C $root push origin $tag
if ($LASTEXITCODE -ne 0) { Fail "push of $tag failed: run git push origin $tag" }

Write-Host "distributed $version (versionCode $n) to group '$Group'"
Write-Host $consoleUrl
