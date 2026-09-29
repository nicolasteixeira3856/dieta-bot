#!/usr/bin/env bash
# Home QA on a running emulator: onboarding, Home interactions, then captures of home0/home1/homeX/homeW
# in docs/qa/android/current/<theme>/. Logs are seeded straight into Room (sqlite) because meal
# logging only exists once the Chat lands (A5).
#
# Prereqs: devDebug APK installed; AVD at gold geometry (wm size 780x1688, wm density 320); python3.
# Usage: tools/capture-home.sh dark|light      Then: node tools/diff-gold.mjs dark/home1 ...
set -u
THEME="${1:?dark|light}"
ADB="${ADB:-adb}"
export ADB MSYS_NO_PATHCONV=1
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/docs/qa/android/current/$THEME"
TMP="$(mktemp -d)"
command -v cygpath >/dev/null && TMP="$(cygpath -m "$TMP")"
PY="$(command -v python3 || command -v python)"
PKG=com.nutri.android.dev
# The Kotlin package did not change with the dev flavor (A10): name the activity in full.
ACTIVITY=com.nutri.android.MainActivity
mkdir -p "$OUT"
FAIL=0

bash "$ROOT/tools/capture-onboarding.sh" "$THEME" | tail -1
# Fresh package (A10 .dev): answer the A7 notification prompt up front. capture-push.sh tests the prompt itself.
"$ADB" shell pm grant $PKG android.permission.POST_NOTIFICATIONS >/dev/null 2>&1
# The prompt may already be on screen from the onboarding relaunch: restart so Home opens clean.
"$ADB" shell am force-stop $PKG; "$ADB" shell am start -W -n $PKG/$ACTIVITY >/dev/null; sleep 3

dump() { "$ADB" shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; "$ADB" exec-out cat /sdcard/ui.xml > "$TMP/ui.xml"; }
at() { # at <attribute regex> -> "x y" of the first matching node
  "$PY" - "$1" "$TMP/ui.xml" <<'EOF'
import re, sys
xml = open(sys.argv[2], encoding="utf-8").read()
m = re.search(sys.argv[1] + r'[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml)
if m:
    x1, y1, x2, y2 = map(int, m.groups())
    print((x1 + x2) // 2, (y1 + y2) // 2)
EOF
}
tap() { dump; local xy; xy=$(at "$1"); [ -z "$xy" ] && { echo "  ✗ not found: $1"; FAIL=1; return 1; }; "$ADB" shell input tap $xy; sleep 1; }
expect() { dump; if grep -q "$2" "$TMP/ui.xml"; then echo "  ✓ $1"; else echo "  ✗ $1"; FAIL=1; fi; }
to_top() { "$ADB" shell input swipe 390 500 390 1500 150; sleep 0.4; "$ADB" shell input swipe 390 500 390 1500 150; sleep 0.5; }

# Interactions on the empty day (after onboarding).
tap 'resource-id="home-slot-[0-9]+"' && expect "tap empty slot asks to skip" 'text="Pular [^"]*?"'
tap 'text="Pular"' && expect "skip redraws as Refeição pulada" 'Refeição pulada'
to_top; tap 'resource-id="home-fab"' && expect "FAB opens Chat" 'resource-id="chat"'
"$ADB" shell input keyevent 4; sleep 0.8; expect "back returns Home" 'resource-id="home"'
to_top; tap 'resource-id="home-config"' && expect "gear opens Config" 'resource-id="cfg"'
"$ADB" shell input keyevent 4; sleep 0.8; expect "back returns Home" 'resource-id="home"'
if grep -q 'android.widget.EditText' "$TMP/ui.xml"; then echo "  ✗ Home has a text field"; FAIL=1; else echo "  ✓ zero text fields on Home"; fi

# A22: Treino de hoje from the Home in 3 taps (row, field, Salvar). The sheet focuses the field itself.
expect "workout row reads Informar" 'text="Informar"'
tap 'resource-id="home-workout"' && expect "row opens the workout sheet" 'resource-id="home-workout-sheet"'
"$ADB" shell input text 350; sleep 0.5
"$ADB" shell input keyevent 4; sleep 0.5 # hide the keyboard
tap 'resource-id="home-workout-save"' && expect "saved workout shows on the row" 'text="350 kcal · +[0-9]* na meta"'
to_top; tap 'resource-id="home-config"' && expect "Config shows the Home workout" 'text="350 kcal"'
"$ADB" shell input keyevent 4; sleep 0.8
tap 'resource-id="home-workout"' && expect "sheet opens with the stored value" 'text="350"'
for _ in 1 2 3 4; do "$ADB" shell input keyevent 67; done; sleep 0.3
"$ADB" shell input keyevent 4; sleep 0.5
tap 'resource-id="home-workout-save"' && expect "empty Salvar clears the workout" 'text="Informar"'

seed() { # seed home0|home1|homeX -> capture
  "$ADB" shell am force-stop $PKG
  rm -f "$TMP"/nutri.db*
  for f in nutri.db nutri.db-wal nutri.db-shm; do "$ADB" exec-out run-as $PKG cat databases/$f > "$TMP/$f" 2>/dev/null; done
  "$PY" - "$TMP/nutri.db" "$1" <<'EOF'
import sqlite3, sys
db, state = sys.argv[1], sys.argv[2]
c = sqlite3.connect(db)
today = c.execute("select firstDay from profile").fetchone()[0]
ids = [r[0] for r in c.execute("select id from meal_slot order by minutesFromMidnight")]
c.execute("update meal_slot set name='Café da manhã' where id=?", (ids[0],))  # adb cannot type accents
c.execute("delete from meal_log")
c.execute("delete from slot_skip")
# A22 golds: home1 "350 kcal · +175 na meta", homeX "200 kcal · +100 na meta", pill still at 2000.
workout = {"home1": (1825, 350), "homeW": (1825, 350), "homeX": (1900, 200)}.get(state)
c.execute("update profile set ceilingMode='same', kcalSame=?, eat=?, pct=50", (workout[0] if workout else 2000, "partial" if workout else "zero"))
c.execute("insert or ignore into day(date, workoutKcal, removedWindows, askedWindows) values(?, null, '[]', '[]')", (today,))
c.execute("update day set workoutKcal=? where date=?", (workout[1] if workout else None, today))
logs = {
    "home1": [(0, "2 pães franceses, 2 ovos mexidos e café com leite", 520, 28, 52, 22, "user"),
              (1, "Prato feito: frango grelhado, arroz, feijão e salada", 780, 48, 82, 18, "photo")],
    "homeW": [(0, "2 pães franceses, 2 ovos mexidos e café com leite", 520, 28, 52, 22, "user"),
              (1, "Prato feito: frango grelhado, arroz, feijão e salada", 780, 48, 82, 18, "photo")],
    "homeX": [(0, "2 pães franceses, 2 ovos", 380, 22, 48, 12, "user"),
              (1, "PF de frango grelhado, arroz e feijão", 780, 64, 88, 16, "user"),
              (3, "Pizza brotinho e refrigerante", 1120, 82, 104, 54, "user")],
}.get(state, [])
for i, text, kcal, p, carbs, fat, src in logs:
    c.execute("insert into meal_log(date,window,text,kcal,p,stable,slotId,carbs,fat,source) values(?,?,?,?,?,1,?,?,?,?)",
              (today, "", text, kcal, p, ids[i], carbs, fat, src))
if state != "home0":
    c.execute("insert into slot_skip(date,slotId) values(?,?)", (today, ids[2]))
c.commit()
c.execute("pragma wal_checkpoint(TRUNCATE)")
c.execute("pragma journal_mode=DELETE")
c.close()
EOF
  "$ADB" push "$TMP/nutri.db" /data/local/tmp/nutri.db >/dev/null
  "$ADB" shell chmod 644 /data/local/tmp/nutri.db
  "$ADB" shell run-as $PKG sh -c "'rm -f databases/nutri.db-wal databases/nutri.db-shm; cp /data/local/tmp/nutri.db databases/nutri.db'"
  "$ADB" shell am start -W -n $PKG/$ACTIVITY >/dev/null
  sleep 3
  if [ "$1" = homeW ]; then # sheet open over home1, keyboard hidden, field still focused
    tap 'resource-id="home-workout"'; "$ADB" shell input keyevent 4; sleep 0.8
  fi
  "$ADB" exec-out screencap -p > "$OUT/$1.png"
  echo "  captured $THEME/$1"
}
seed home0
seed home1
seed homeX
seed homeW
rm -rf "$TMP"
exit $FAIL
