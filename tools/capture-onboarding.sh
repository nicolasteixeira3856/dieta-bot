#!/usr/bin/env bash
# Drive splash + the conversational onboarding (A71, ADR-057) on a running emulator and capture
# docs/qa/android/current/<theme>/: splash, ob0 (welcome), ob2 (targets prefilled), ob1 (tone question),
# ob1e (an answer not understood), ob3 (summary), ob4 (building), ob6 (forced HTTP 500) and ob5 (retry).
# Checks on the way: a kill of the process at step 9 resumes at step 9; Tentar de novo after the 500 reaches ob5;
# a relaunch after the onboarding opens the Home.
#
# Prereqs: the fake server (node tools/fake-chat-server.mjs) and a devDebug APK built against it
#   (./gradlew :app:assembleDevDebug -PAPI_PUBLIC_URL=http://10.0.2.2:8765); python3; AVD at gold geometry:
#   adb shell wm size 780x1688 && adb shell wm density 320
# QUICK=1 (the other capture scripts): no captures, the profile they expect (male 27/180/116, 2000 kcal with the
# 30/40/30 split, the four default meals, 0 % eat-back, Seco, no facts).
# Usage (multiple devices): ANDROID_SERIAL=emulator-5554 tools/capture-onboarding.sh dark|light
# Then:  node tools/diff-gold.mjs
set -u
THEME="${1:?dark|light}"
QUICK="${QUICK:-0}"
ADB="${ADB:-adb}"
FAKE="${FAKE:-http://127.0.0.1:8765}"
export MSYS_NO_PATHCONV=1
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/docs/qa/android/current/$THEME"
TMP="$(mktemp -d)"
command -v cygpath >/dev/null && TMP="$(cygpath -m "$TMP")" # Git Bash: node needs a Windows path
PKG=app.fibrai.android.dev
# The Kotlin package did not change with the dev flavor (A10): name the activity in full.
ACTIVITY=app.fibrai.android.MainActivity
mkdir -p "$OUT"
FAIL=0

center() { # center <resource-id> -> "x y"
  "$ADB" shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  "$ADB" exec-out cat /sdcard/ui.xml > "$TMP/ui.xml"
  node -e '
    const xml = require("fs").readFileSync(process.argv[2], "utf8");
    const tag = process.argv[1].replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
    const m = xml.match(new RegExp(`resource-id="${tag}"[^>]*bounds="\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]"`));
    if (!m) { console.error("missing " + process.argv[1]); process.exit(1); }
    const [x1, y1, x2, y2] = m.slice(1).map(Number);
    console.log(((x1 + x2) >> 1) + " " + ((y1 + y2) >> 1));
  ' "$1" "$TMP/ui.xml"
}
tap() {
  local xy attempt
  # A cold start can still be on Splash when am start reports its first frame.
  for attempt in 1 2 3 4 5 6 7 8; do
    if xy=$(center "$1" 2>/dev/null); then
      "$ADB" shell input tap $xy; sleep 0.5; return
    fi
    sleep 0.5
  done
  echo "  missing $1" >&2; exit 1
}
has() { "$ADB" shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; "$ADB" exec-out cat /sdcard/ui.xml | grep -q "$1"; }
hide_kb() { if "$ADB" shell dumpsys input_method | grep -q "mInputShown=true"; then "$ADB" shell input keyevent 4; sleep 0.6; fi; }
# say <text>: types into the composer (spaces as %s; adb cannot type accents) and sends.
say() {
  tap composer-field
  "$ADB" shell input keyevent KEYCODE_MOVE_END
  for _ in $(seq 1 60); do "$ADB" shell input keyevent 67; done
  "$ADB" shell input text "$(printf '%s' "$1" | sed 's/ /%s/g')"
  sleep 0.3
  tap composer-send
  hide_kb
}
reply() { tap "ob-reply-$1"; sleep 0.3; }
mode() { curl -s -X POST "$FAKE/__mode" -d "$1" >/dev/null; }
shot() { [ "$QUICK" = 1 ] && return; sleep "${2:-0.8}"; "$ADB" exec-out screencap -p > "$OUT/$1.png"; echo "  captured $THEME/$1"; }
check() { if has "$2"; then echo "  ✓ $1"; else echo "  ✗ $1"; FAIL=1; fi; }

curl -s "$FAKE/__calls" >/dev/null || { echo "  fake server not running on $FAKE"; exit 1; }
mode '{}'
"$ADB" shell cmd uimode night "$([ "$THEME" = dark ] && echo yes || echo no)" >/dev/null
"$ADB" shell pm clear $PKG >/dev/null
# The notification step asks POST_NOTIFICATIONS; granted here, capture-push.sh owns the prompt.
"$ADB" shell pm grant $PKG android.permission.POST_NOTIFICATIONS >/dev/null 2>&1

if [ "$QUICK" != 1 ]; then
  # Splash in capture mode (it stays up).
  "$ADB" shell am start -W -n $PKG/$ACTIVITY -e fibrai_tela splash >/dev/null
  shot splash 2.5
  "$ADB" shell am force-stop $PKG
fi
"$ADB" shell am start -W -f 0x10008000 -n $PKG/$ACTIVITY >/dev/null
sleep 2.5

shot ob0 1.5
tap ob0-start
if [ "$QUICK" = 1 ]; then
  reply 1; say 27; say 180; say 116; say 2000
  reply 2; reply 0; reply 0
  reply 0; reply 0; reply 0; say "variado"; reply 0; reply 3; reply 0; reply 0
  sleep 1
  "$ADB" shell input swipe 390 1400 390 300 200; sleep 0.4
  "$ADB" shell input swipe 390 1400 390 300 200; sleep 0.6
  tap ob3-confirm
  sleep 2.5
  tap ob5-home
  sleep 1.5
  echo "  ✓ onboarded (quick)"
  rm -rf "$TMP"
  exit 0
fi

# The D27 sample person: female, 32 years, 165 cm, 66 kg.
reply 0
say 32
say 165
say 66
# Step 5: the TMB/IMC proposal with the targets prefilled in the composer (gold ob2).
shot ob2
tap composer-send
hide_kb
reply 2            # 4 meals
reply 0            # Usar o padrão
reply 1            # 50 %
# Step 9: the tone question, Seco preselected (gold ob1).
shot ob1
say "pode ser um meio termo"
shot ob1e
check "não entendi keeps step 9" '9 de 16'

# Kill at step 9: the relaunch resumes the chat at the same step.
"$ADB" shell am force-stop $PKG
"$ADB" shell am start -W -n $PKG/$ACTIVITY >/dev/null
sleep 3
check "kill at step 9 resumes at step 9" '9 de 16'

reply 0            # Seco
reply 1            # Sem lactose
reply 1            # Medidas caseiras
say "Cafe: pao com ovo. Almoco: arroz, feijao e frango"
say "figado"
say "Air fryer e micro-ondas"
say "60 kg ate 30/04/2027"
reply 0            # Sim
sleep 1
shot ob3

# A forced HTTP 500 after 3 s: building (ob4), then the error (ob6); Tentar de novo reaches ob5.
mode '{"profile_fail": 500, "profile_delay": 3000}'
"$ADB" shell input swipe 390 1400 390 300 200; sleep 0.4
"$ADB" shell input swipe 390 1400 390 300 200; sleep 0.6
tap ob3-confirm
shot ob4 0.8
sleep 3.5
shot ob6
check "HTTP 500 shows ob6" 'ob6-retry'
mode '{"profile_delay": 1000}'
tap ob6-retry
sleep 2.5
shot ob5
check "Tentar de novo reaches ob5" 'ob5-home'
mode '{}'
tap ob5-home
sleep 1.5

# Kill + relaunch: must land on Home, not the onboarding.
"$ADB" shell am force-stop $PKG
"$ADB" shell am start -W -n $PKG/$ACTIVITY >/dev/null
sleep 3
if has 'resource-id="ob0'; then echo "  ✗ relaunch opened the onboarding"; FAIL=1; else echo "  ✓ relaunch opened the Home"; fi
rm -rf "$TMP"
exit $FAIL
