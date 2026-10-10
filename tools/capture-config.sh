#!/usr/bin/env bash
# Config QA on a running emulator: onboarding -> Home gear -> cfg (capture) -> slot rename (no wipe)
# -> workout 400 / empty -> new ceiling -> wipe dialog (capture) -> Cancelar keeps the ceiling ->
# Confirmar wipes today's meal_log only -> back to Home. Captures land in docs/qa/android/current/<theme>/.
#
# Prereqs: devDebug APK installed; AVD at gold geometry (wm size 780x1688, wm density 320); python3.
# Usage (multiple devices): ANDROID_SERIAL=emulator-5554 tools/capture-config.sh dark|light      Then: node tools/diff-gold.mjs dark/cfg dark/wipe ...
set -uo pipefail
THEME="${1:?dark|light}"
ADB="${ADB:-adb}"
export ADB MSYS_NO_PATHCONV=1 PYTHONIOENCODING=utf-8
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/docs/qa/android/current/$THEME"
TMP="$(mktemp -d)"
command -v cygpath >/dev/null && TMP="$(cygpath -m "$TMP")"
PY="$(command -v python3 || command -v python)"
PKG=app.fibrai.android.dev
# The Kotlin package did not change with the dev flavor (A10): name the activity in full.
ACTIVITY=app.fibrai.android.MainActivity
mkdir -p "$OUT"
FAIL=0
# shellcheck source=tools/input-checks.sh
. "$ROOT/tools/input-checks.sh"

# ADR-019: hide any dev-only tool row so cfg matches the gold (none since A69 removed "Memória da IA (dev)"). Cleared on exit.
"$ADB" shell setprop debug.fibrai.hide_dev_tools 1
trap '"$ADB" shell setprop debug.fibrai.hide_dev_tools 0' EXIT

QUICK=1 bash "$ROOT/tools/capture-onboarding.sh" "$THEME" | tail -1 || exit 1
# Fresh package (A10 .dev): answer the A7 notification prompt up front. capture-push.sh tests the prompt itself.
"$ADB" shell pm grant $PKG android.permission.POST_NOTIFICATIONS >/dev/null 2>&1
# The prompt may already be on screen from the onboarding relaunch: restart so Home opens clean.
"$ADB" shell am force-stop $PKG; "$ADB" shell am start -W -n $PKG/$ACTIVITY >/dev/null; sleep 3

dump() { "$ADB" shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; "$ADB" exec-out cat /sdcard/ui.xml > "$TMP/ui.xml"; }
at() {
  "$PY" - "$1" "$TMP/ui.xml" <<'EOF'
import re, sys
xml = open(sys.argv[2], encoding="utf-8").read()
m = re.search(sys.argv[1] + r'[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml)
if m:
    x1, y1, x2, y2 = map(int, m.groups())
    print((x1 + x2) // 2, (y1 + y2) // 2)
EOF
}
tap() { dump; local xy; xy=$(at "$1"); [ -z "$xy" ] && { echo "  ✗ not found: $1"; FAIL=1; return 1; }; "$ADB" shell input tap $xy; sleep "${2:-1}"; }
expect() { dump; if grep -qE "$2" "$TMP/ui.xml"; then echo "  ✓ $1"; else echo "  ✗ $1"; FAIL=1; fi; }
hide_kb() { if "$ADB" shell dumpsys input_method | grep -q "mInputShown=true"; then "$ADB" shell input keyevent 4; sleep 0.6; fi; }
type_into() { tap "resource-id=\"$1\"" 0.5; "$ADB" shell input keyevent 123; for _ in 1 2 3 4 5 6; do "$ADB" shell input keyevent 67; done; [ -n "$2" ] && "$ADB" shell input text "$2"; sleep 0.3; hide_kb; }
shot() { sleep "${2:-0.8}"; "$ADB" exec-out screencap -p > "$OUT/$1.png"; echo "  captured $THEME/$1"; }
pull_db() {
  rm -f "$TMP"/fibrai.db*
  for f in fibrai.db fibrai.db-wal fibrai.db-shm; do "$ADB" exec-out run-as $PKG cat databases/$f > "$TMP/$f" 2>/dev/null; done
}
sql() { pull_db; "$PY" -c "import sqlite3,sys; c=sqlite3.connect(sys.argv[1]); print(c.execute(sys.argv[2]).fetchone()[0])" "$TMP/fibrai.db" "$1"; }
check() { local got; got=$(sql "$2"); if [ "$got" = "$3" ]; then echo "  ✓ $1"; else echo "  ✗ $1 (got $got, want $3)"; FAIL=1; fi; }

# Seed the gold names (adb cannot type accents), one log of today and one chat message.
"$ADB" shell am force-stop $PKG
pull_db
"$PY" - "$TMP/fibrai.db" <<'EOF'
import sqlite3, sys, time
c = sqlite3.connect(sys.argv[1])
today = c.execute("select firstDay from profile").fetchone()[0]
ids = [r[0] for r in c.execute("select id from meal_slot order by minutesFromMidnight")]
c.execute("update meal_slot set name='Café da manhã' where id=?", (ids[0],))
c.execute("update meal_slot set name='Lanche da tarde' where id=?", (ids[2],))
c.execute("delete from meal_log")
c.execute("insert into meal_log(date,window,text,kcal,p,stable,slotId,carbs,fat,source) values(?,?,?,?,?,1,?,?,?,?)",
          (today, "", "2 ovos", 380, 22, ids[0], 4, 16, "user"))
c.execute("insert into chat_message(date,role,text,createdAtEpochMs) values(?,?,?,?)", (today, "user", "2 ovos", int(time.time() * 1000)))
c.commit()
c.execute("pragma wal_checkpoint(TRUNCATE)")
c.execute("pragma journal_mode=DELETE")
c.close()
EOF
"$ADB" push "$TMP/fibrai.db" /data/local/tmp/fibrai.db >/dev/null
"$ADB" shell chmod 644 /data/local/tmp/fibrai.db
"$ADB" shell run-as $PKG sh -c "'rm -f databases/fibrai.db-wal databases/fibrai.db-shm; cp /data/local/tmp/fibrai.db databases/fibrai.db'"
"$ADB" shell am start -W -n $PKG/$ACTIVITY >/dev/null
sleep 3

tap 'resource-id="home-config"' && expect "gear opens Config" 'resource-id="cfg"'
expect "ceiling row 2000 kcal" 'text="2000 kcal"'
expect "no workout: credit 0" 'Crédito atual: 0 kcal'
dump; if grep -q 'resource-id="cfg-dev-memory"' "$TMP/ui.xml"; then echo "  ✗ the A23 dev row is back"; FAIL=1; else echo "  ✓ no dev tool row (A69)"; fi
"$ADB" shell input swipe 390 1400 390 300 300; sleep 0.8
expect "Da Tali: Receitas and O que a Tali sabe" 'resource-id="cfg-recipes".*resource-id="cfg-memory"|resource-id="cfg-memory".*resource-id="cfg-recipes"'
"$ADB" shell input swipe 390 300 390 1400 300; sleep 0.8
shot cfg

# A46: in Config the focused field rises above the keyboard and an edit starts at the end of the value.
# Meal editor (the O3 screen): the last meal name. Back leaves the editor without saving.
tap 'resource-id="cfg-slot-0"'
"$ADB" shell input swipe 390 1300 390 500 300; sleep 0.6
"$ADB" shell input swipe 390 1300 390 500 300; sleep 0.6
before=$(text_of cfg-name-3 | tr -d '\r')
tap 'resource-id="cfg-name-3"'
above_ime cfg-name-3 "Config meal editor: last name above the keyboard"
"$ADB" shell input text x; sleep 0.4
ends_at_end cfg-name-3 "$before" x "Config meal name: typing goes to the end"
hide_kb
"$ADB" shell input keyevent 4; sleep 1
expect "back leaves the meal editor" 'resource-id="cfg-ceiling"'
# Macros sheet: the adjust button puts the cursor after the stored grams.
tap 'resource-id="cfg-macros"'
before=$(text_of cfg-carb-field | tr -d '\r')
tap_desc "Ajustar Carboidrato"
above_ime cfg-carb-field "Config Carboidrato above the keyboard"
"$ADB" shell input text 5; sleep 0.4
ends_at_end cfg-carb-field "$before" 5 "Config Carboidrato: adjust puts the cursor at the end"
hide_kb
tap 'resource-id="cfg-cancel"'
# Ceiling sheet, one goal per day: Dom and the whole sheet (title to Salvar) above the keyboard.
tap 'resource-id="cfg-ceiling"'
tap 'resource-id="cfg-mode-seven"'
"$ADB" shell input swipe 390 1250 390 750 300; sleep 0.6
"$ADB" shell input swipe 390 1250 390 750 300; sleep 0.6
tap 'resource-id="cfg-day-6"'
above_ime cfg-day-6 "Config ceiling Dom above the keyboard"
above_ime cfg-sheet "Config ceiling sheet fits above the keyboard"
hide_kb
tap 'resource-id="cfg-cancel"'
expect "cancel keeps 2000 kcal before the rename" 'text="2000 kcal"'

# Slot rename: relabels, no wipe.
tap 'resource-id="cfg-slot-0"' && tap 'resource-id="cfg-chip-0-1"' && tap 'resource-id="cfg-save"'
expect "slot renamed to Desjejum" 'text="Desjejum"'
check "rename kept today's log" "select count(*) from meal_log" 1
check "rename did not wipe" "select count(*) from chat_message where role='wiped'" 0

# Workout of today: typed number, empty = null.
tap 'resource-id="cfg-workout"' && type_into cfg-workout-field 400 && tap 'resource-id="cfg-save"'
expect "workout 400 kcal shown" 'text="400 kcal"'
expect "policy 0%: credit stays 0" 'Crédito atual: 0 kcal'
check "workout stored" "select workoutKcal from day" 400
tap 'resource-id="cfg-workout"' && type_into cfg-workout-field "" && tap 'resource-id="cfg-save"'
expect "empty workout: Nenhum informado" 'text="Nenhum informado"'
check "workout null" "select ifnull(workoutKcal,'null') from day" null

# New ceiling: dialog first. Cancelar keeps 2000; Confirmar stores 1800 and wipes today.
tap 'resource-id="cfg-ceiling"' && type_into cfg-same 1800 && tap 'resource-id="cfg-save"'
expect "wipe dialog up" 'resource-id="cfg-wipe"'
shot wipe
tap 'resource-id="cfg-wipe-cancel"'
expect "cancel keeps 2000 kcal" 'text="2000 kcal"'
check "cancel stored nothing" "select kcalSame from profile" 2000
check "cancel kept the log" "select count(*) from meal_log" 1
tap 'resource-id="cfg-ceiling"' && type_into cfg-same 1800 && tap 'resource-id="cfg-save"'
tap 'resource-id="cfg-wipe-confirm"'
expect "confirm shows 1800 kcal" 'text="1800 kcal"'
check "ceiling stored" "select kcalSame from profile" 1800
check "today's logs wiped" "select count(*) from meal_log" 0
check "chat kept" "select count(*) from chat_message where role='user'" 1
check "wipe marker written" "select count(*) from chat_message where role='wiped'" 1
check "profile kept" "select onboardingDone from profile" 1

# A24 grouped Config fixture keeps the log/chat history; verify the group editor does too.
"$ADB" shell am force-stop $PKG
pull_db
"$PY" - "$TMP/fibrai.db" <<'EOF'
import sqlite3, sys
c = sqlite3.connect(sys.argv[1])
c.execute("update profile set slotMode='split',kcalSame=2000")
c.execute("update meal_slot set days=31")
first = c.execute("select id from meal_slot order by sortOrder limit 1").fetchone()[0]
c.execute("update meal_slot set name='Café da manhã' where id=?", (first,))
c.executemany("insert into meal_slot(name,minutesFromMidnight,sortOrder,days) values(?,?,?,96)",
              [("Café da manhã",570,4),("Almoço",810,5),("Jantar",1230,6)])
c.commit()
c.execute("pragma wal_checkpoint(TRUNCATE)")
c.execute("pragma journal_mode=DELETE")
c.close()
EOF
"$ADB" push "$TMP/fibrai.db" /data/local/tmp/fibrai.db >/dev/null
"$ADB" shell run-as $PKG sh -c "'rm -f databases/fibrai.db-wal databases/fibrai.db-shm; cp /data/local/tmp/fibrai.db databases/fibrai.db'"
"$ADB" shell am start -W -n $PKG/$ACTIVITY >/dev/null
sleep 2
tap 'resource-id="home-config"'
shot cfgS
tap 'resource-id="cfg-group-1"'
expect "weekend full-screen editor" 'text="Sáb e Dom"'
tap 'resource-id="cfg-save"'
check "group save preserves chat" "select count(*) from chat_message where role='user'" 1

# A53 (ADR-040): Resetar app. Cancelar keeps everything; Apagar tudo erases Room, memory, photos and
# reminders, keeps the installation id, opens O1 with no back stack.
"$ADB" shell run-as $PKG sh -c "'echo sealed > files/memory.bin; mkdir -p files/photos; echo jpeg > files/photos/p.jpg'"
id_before=$("$ADB" exec-out run-as $PKG cat no_backup/installation_id 2>/dev/null)
alarms() { "$ADB" shell dumpsys alarm | grep -c "Alarm{.*$PKG}"; }
echo "  · alarms of the package before reset: $(alarms)"
"$ADB" shell input swipe 390 1300 390 300 300; sleep 0.6
"$ADB" shell input swipe 390 1300 390 300 300; sleep 0.6
tap 'resource-id="cfg-reset"' && expect "reset dialog up" 'resource-id="cfg-reset-dialog"'
shot cfgR
tap 'resource-id="cfg-reset-cancel"'
expect "cancel stays on Config" 'resource-id="cfg-reset"'
check "cancel kept the profile" "select onboardingDone from profile" 1
tap 'resource-id="cfg-reset"' && tap 'resource-id="cfg-reset-confirm"' 2
expect "reset opens the onboarding" 'resource-id="ob0'
for t in profile day meal_log meal_slot slot_skip chat_message day_digest; do check "reset emptied $t" "select count(*) from $t" 0; done
files=$("$ADB" shell run-as $PKG sh -c "'ls files/memory.bin files/photos 2>/dev/null | wc -l'" | tr -d '
 ')
if [ "$files" = "0" ]; then echo "  ✓ memory and photos deleted"; else echo "  ✗ memory or photos left ($files)"; FAIL=1; fi
id_after=$("$ADB" exec-out run-as $PKG cat no_backup/installation_id 2>/dev/null)
if [ -n "$id_before" ] && [ "$id_before" = "$id_after" ]; then echo "  ✓ installation id kept"; else echo "  ✗ installation id changed or missing"; FAIL=1; fi
left=$(alarms); if [ "$left" -le 1 ]; then echo "  ✓ no slot reminder left ($left alarm: the daily resync at most)"; else echo "  ✗ $left alarms left"; FAIL=1; fi
"$ADB" shell input keyevent 4; sleep 1
if "$ADB" shell dumpsys activity activities | grep -m1 "topResumedActivity" | grep -q "$PKG"; then echo "  ✗ back from O1 stayed in the app"; FAIL=1; else echo "  ✓ back from O1 leaves the app"; fi
"$ADB" shell am start -W -n $PKG/$ACTIVITY >/dev/null; sleep 3
expect "relaunch after reset opens the onboarding" 'resource-id="ob0'
rm -rf "$TMP"
exit $FAIL
