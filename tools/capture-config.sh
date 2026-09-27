#!/usr/bin/env bash
# Config QA on a running emulator: onboarding -> Home gear -> cfg (capture) -> slot rename (no wipe)
# -> workout 400 / empty -> new ceiling -> wipe dialog (capture) -> Cancelar keeps the ceiling ->
# Confirmar wipes today's meal_log only -> back to Home. Captures land in docs/qa/android/current/<theme>/.
#
# Prereqs: debug APK installed; AVD at gold geometry (wm size 780x1688, wm density 320); python3.
# Usage: tools/capture-config.sh dark|light      Then: node tools/diff-gold.mjs dark/cfg dark/wipe ...
set -u
THEME="${1:?dark|light}"
ADB="${ADB:-adb}"
export ADB MSYS_NO_PATHCONV=1 PYTHONIOENCODING=utf-8
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/docs/qa/android/current/$THEME"
TMP="$(mktemp -d)"
command -v cygpath >/dev/null && TMP="$(cygpath -m "$TMP")"
PY="$(command -v python3 || command -v python)"
PKG=com.nutri.android
mkdir -p "$OUT"
FAIL=0

bash "$ROOT/tools/capture-onboarding.sh" "$THEME" | tail -1

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
type_into() { tap "resource-id=\"$1\"" 0.5; for _ in 1 2 3 4 5 6; do "$ADB" shell input keyevent 67; done; [ -n "$2" ] && "$ADB" shell input text "$2"; sleep 0.3; hide_kb; }
shot() { sleep "${2:-0.8}"; "$ADB" exec-out screencap -p > "$OUT/$1.png"; echo "  captured $THEME/$1"; }
pull_db() {
  rm -f "$TMP"/nutri.db*
  for f in nutri.db nutri.db-wal nutri.db-shm; do "$ADB" exec-out run-as $PKG cat databases/$f > "$TMP/$f" 2>/dev/null; done
}
sql() { pull_db; "$PY" -c "import sqlite3,sys; c=sqlite3.connect(sys.argv[1]); print(c.execute(sys.argv[2]).fetchone()[0])" "$TMP/nutri.db" "$1"; }
check() { local got; got=$(sql "$2"); if [ "$got" = "$3" ]; then echo "  ✓ $1"; else echo "  ✗ $1 (got $got, want $3)"; FAIL=1; fi; }

# Seed the gold names (adb cannot type accents), one log of today and one chat message.
"$ADB" shell am force-stop $PKG
pull_db
"$PY" - "$TMP/nutri.db" <<'EOF'
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
"$ADB" push "$TMP/nutri.db" /data/local/tmp/nutri.db >/dev/null
"$ADB" shell chmod 644 /data/local/tmp/nutri.db
"$ADB" shell run-as $PKG sh -c "'rm -f databases/nutri.db-wal databases/nutri.db-shm; cp /data/local/tmp/nutri.db databases/nutri.db'"
"$ADB" shell am start -W -n $PKG/.MainActivity >/dev/null
sleep 3

tap 'resource-id="home-config"' && expect "gear opens Config" 'resource-id="cfg"'
expect "ceiling row 2000 kcal" 'text="2000 kcal"'
expect "no workout: credit 0" 'Crédito atual: 0 kcal'
shot cfg

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

"$ADB" shell input keyevent 4; sleep 0.8
expect "back returns Home" 'resource-id="home"'
rm -rf "$TMP"
exit $FAIL
