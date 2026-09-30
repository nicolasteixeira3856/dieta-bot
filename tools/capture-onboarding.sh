#!/usr/bin/env bash
# Drive splash + O1..O4 on a running emulator and capture docs/qa/android/current/<theme>/.
# Reaches the state shown in the Stitch gold through the real UI (testTag = resource-id), then
# finishes onboarding, kills the app and checks that a relaunch skips onboarding.
#
# Prereqs: devDebug APK installed; python3; AVD at gold geometry:
#   adb shell wm size 780x1688 && adb shell wm density 320
# Usage: tools/capture-onboarding.sh dark|light
# Then:  node tools/diff-gold.mjs
set -u
THEME="${1:?dark|light}"
ADB="${ADB:-adb}"
export MSYS_NO_PATHCONV=1
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/docs/qa/android/current/$THEME"
TMP="$(mktemp -d)"
command -v cygpath >/dev/null && TMP="$(cygpath -m "$TMP")" # Git Bash: node needs a Windows path
PKG=com.nutri.android.dev
# The Kotlin package did not change with the dev flavor (A10): name the activity in full.
ACTIVITY=com.nutri.android.MainActivity
mkdir -p "$OUT"

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
  for attempt in 1 2 3 4 5 6; do
    if xy=$(center "$1" 2>/dev/null); then
      "$ADB" shell input tap $xy; sleep 0.4; return
    fi
    sleep 0.5
  done
  echo "  missing $1" >&2; exit 1
}
hide_kb() { if "$ADB" shell dumpsys input_method | grep -q "mInputShown=true"; then "$ADB" shell input keyevent 4; sleep 0.6; fi; }
type_into() { tap "$1"; for _ in 1 2 3 4 5 6; do "$ADB" shell input keyevent 67; done; "$ADB" shell input text "$2"; sleep 0.3; hide_kb; }
to_top() { "$ADB" shell input swipe 390 500 390 1500 150; sleep 0.5; }
scroll_down() { "$ADB" shell input swipe 390 1300 390 500 300; sleep 0.6; }
shot() { sleep "${2:-0.8}"; "$ADB" exec-out screencap -p > "$OUT/$1.png"; echo "  captured $THEME/$1"; }

"$ADB" shell cmd uimode night "$([ "$THEME" = dark ] && echo yes || echo no)" >/dev/null
"$ADB" shell pm clear $PKG >/dev/null
# This journey tests onboarding/time editing; capture-push.sh owns the permission prompt.
"$ADB" shell pm grant $PKG android.permission.POST_NOTIFICATIONS >/dev/null 2>&1

# Splash in capture mode (it stays up), then a normal cold start into O1.
"$ADB" shell am start -W -n $PKG/$ACTIVITY -e nutri_tela splash >/dev/null
shot splash 2.5
"$ADB" shell am force-stop $PKG
"$ADB" shell am start -W -f 0x10008000 -n $PKG/$ACTIVITY >/dev/null
sleep 2.5

tap o1-sex-male
type_into o1-age 27
type_into o1-height 180
type_into o1-weight 116
scroll_down
type_into o1-ceiling 2000
to_top
shot o1
tap o1-continue

shot o2
tap o2-continue

# adb cannot type accents: names come from the suggestion chips.
sleep 0.6
tap o3-chip-0-0
tap o3-chip-1-0
scroll_down
tap o3-chip-2-0
tap o3-chip-3-0
to_top
shot o3
tap o3-time-0
tap time-wheel-cancel
tap o3-continue

shot o4
tap o4-finish
sleep 1.5

# Kill + relaunch: must land on Home, not onboarding.
"$ADB" shell am force-stop $PKG
"$ADB" shell am start -W -n $PKG/$ACTIVITY >/dev/null
sleep 3
"$ADB" shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
"$ADB" exec-out cat /sdcard/ui.xml > "$TMP/relaunch.xml"
if grep -q 'resource-id="o1-' "$TMP/relaunch.xml"; then
  echo "  ✗ relaunch opened onboarding"
  exit 1
fi
# ST3 uses a typed meal name rather than the short suggestion "Café".
# Seed that Unicode name in the persisted test profile, as in capture-config.sh,
# then reopen O3 and tap the real time field. No capture-only app behavior.
"$ADB" shell am force-stop $PKG
for f in nutri.db nutri.db-wal nutri.db-shm; do "$ADB" exec-out run-as $PKG cat databases/$f > "$TMP/$f" 2>/dev/null; done
PY="$(command -v python3 || command -v python)"
"$PY" - "$TMP/nutri.db" <<'EOF'
import sqlite3, sys
c = sqlite3.connect(sys.argv[1])
c.execute("update meal_slot set name='Café da manhã' where id=(select id from meal_slot order by minutesFromMidnight limit 1)")
c.commit()
c.execute("pragma wal_checkpoint(TRUNCATE)")
c.execute("pragma journal_mode=DELETE")
c.close()
EOF
"$ADB" push "$TMP/nutri.db" /data/local/tmp/nutri.db >/dev/null
"$ADB" shell chmod 644 /data/local/tmp/nutri.db
"$ADB" shell run-as $PKG sh -c "'rm -f databases/nutri.db-wal databases/nutri.db-shm; cp /data/local/tmp/nutri.db databases/nutri.db'"
"$ADB" shell am start -W -f 0x10008000 -n $PKG/$ACTIVITY -e nutri_tela o3 >/dev/null
sleep 2
tap o3-time-0
shot o3t
tap time-wheel-cancel
"$ADB" shell am force-stop $PKG
"$ADB" shell am start -W -n $PKG/$ACTIVITY >/dev/null
sleep 2
echo "  ✓ relaunch skipped onboarding; o3t captured from the real time field"
rm -rf "$TMP"
