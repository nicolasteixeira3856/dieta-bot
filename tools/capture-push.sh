#!/usr/bin/env bash
# Push QA (A7) on a running emulator: onboarding -> POST_NOTIFICATIONS -> slots seeded relative
# to now (SP) -> AlarmManager checks (inexact without SCHEDULE_EXACT_ALARM, exact with it) -> real
# alarms fire: empty slot notifies ("{nome}. Ainda não registrou."), logged slot does not, skipped
# slot has no alarm -> lock-screen capture in docs/qa/android/current/<theme>/push.png ->
# Pular (skip stored, notification gone) -> Registrar (opens the Chat, notification gone).
#
# Prereqs: devDebug APK installed; AVD at gold geometry (wm size 780x1688, wm density 320); python3.
# Turns on the swipe lock screen (locksettings) for the capture. Takes ~6 minutes (real alarms).
# Usage: tools/capture-push.sh dark|light
set -u
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

# No keyguard while driving the UI (it is turned on only for the lock-screen capture).
"$ADB" shell locksettings set-disabled true >/dev/null
"$ADB" shell input keyevent 224; sleep 1; "$ADB" shell wm dismiss-keyguard >/dev/null 2>&1; sleep 1
QUICK=1 bash "$ROOT/tools/capture-onboarding.sh" "$THEME" | tail -1

dump() { "$ADB" shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; "$ADB" exec-out cat /sdcard/ui.xml > "$TMP/ui.xml"; }
center() {
  local b; b=$(grep -oE "$1[^>]*" "$TMP/ui.xml" | grep -o 'bounds="[^"]*"' | head -1 | tr -dc '0-9,[]')
  [ -z "$b" ] && return 1
  set -- $(echo "$b" | tr '[],' '   ')
  echo "$(( ($1 + $3) / 2 )) $(( ($2 + $4) / 2 ))"
}
tap() { dump; local xy; xy=$(center "$1") || { echo "  ✗ not found: $1"; FAIL=1; return 1; }; "$ADB" shell input tap $xy; sleep "${2:-1.5}"; }
expect() { dump; if grep -qE "$2" "$TMP/ui.xml"; then echo "  ✓ $1"; else echo "  ✗ $1"; FAIL=1; fi; }
check() { if [ "$2" = "$3" ]; then echo "  ✓ $1"; else echo "  ✗ $1 (got '$2', want '$3')"; FAIL=1; fi; }
pull_db() { rm -f "$TMP"/fibrai.db*; for f in fibrai.db fibrai.db-wal fibrai.db-shm; do "$ADB" exec-out run-as $PKG cat databases/$f > "$TMP/$f" 2>/dev/null; done; }
sql() { pull_db; "$PY" -c "import sqlite3,sys; c=sqlite3.connect(sys.argv[1]); print(c.execute(sys.argv[2]).fetchone()[0])" "$TMP/fibrai.db" "$1"; }
# Active slot alarms only: "tag=..." lines of live entries (history snapshots read "type=... tag=...").
alarm_dump() { "$ADB" shell dumpsys alarm | tr -d '\r' > "$TMP/alarm.txt"; }
alarms() { alarm_dump; grep -cE '^ +tag=\*walarm\*:app\.fibrai\.android\.push\.SLOT$' "$TMP/alarm.txt"; }
exact_alarms() { grep -A1 -E '^ +tag=\*walarm\*:app\.fibrai\.android\.push\.SLOT$' "$TMP/alarm.txt" | grep -c 'window=0 exactAllowReason=permission'; }
notified() { "$ADB" shell dumpsys notification --noredact | tr -d '\r' | grep -c "android.title=String (${1}. Ainda não registrou.)"; }
# Shade: expand the app notification group, then the notification titled $1, then tap its action $2.
notification_action() {
  "$ADB" shell cmd statusbar expand-notifications; sleep 2
  local attempt xy
  for attempt in 1 2 3; do
    dump
    xy=$("$PY" - "$TMP/ui.xml" "$1" "$2" <<'EOF'
import re, sys
xml = open(sys.argv[1], encoding="utf-8").read()
nodes = re.findall(r"<node [^>]*>", xml)
def box(n):
    return tuple(map(int, re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n).groups()))
def mid(b):
    return (b[0] + b[2]) // 2, (b[1] + b[3]) // 2
title = next((i for i, n in enumerate(nodes) if f"{sys.argv[2]}. Ainda" in n), None)
if title is None:
    sys.exit(0)
tb = box(nodes[title])
# Action already visible below the title?
for n in nodes[title + 1:]:
    if f'text="{sys.argv[3]}"' in n:
        print("tap", *mid(box(n))); sys.exit(0)
    if "Ainda não registrou" in n:
        break
# Otherwise the expand button on the title's row (a group header counts too).
cy = (tb[1] + tb[3]) // 2
best = None
for n in nodes:
    if 'content-desc="Expand"' in n:
        b = box(n)
        if b[1] - 60 <= cy <= b[3] + 60 and (best is None or abs(mid(b)[1] - cy) < abs(mid(best)[1] - cy)):
            best = b
if best:
    print("expand", *mid(best))
EOF
)
    xy=$(printf '%s' "$xy" | tr -d '\r') # Windows python ends lines with CR; adb rejects "488\r"
    case "$xy" in
      tap*) "$ADB" shell input tap ${xy#tap }; sleep 3; "$ADB" shell cmd statusbar collapse; return 0 ;;
      expand*) "$ADB" shell input tap ${xy#expand }; sleep 1.5 ;;
      *) break ;;
    esac
  done
  "$ADB" shell cmd statusbar collapse
  return 1
}

# First Home after onboarding asks POST_NOTIFICATIONS (Android 13+). capture-onboarding.sh grants it up front (A21):
# take it back so this journey sees the prompt.
"$ADB" shell am force-stop $PKG
"$ADB" shell pm revoke $PKG android.permission.POST_NOTIFICATIONS >/dev/null 2>&1
"$ADB" shell pm clear-permission-flags $PKG android.permission.POST_NOTIFICATIONS user-set user-fixed >/dev/null 2>&1
"$ADB" shell appops set $PKG SCHEDULE_EXACT_ALARM default >/dev/null 2>&1
"$ADB" shell am start -W -n $PKG/$ACTIVITY >/dev/null; sleep 4
expect "Home asks for notification permission" 'permission_allow_button'
tap 'resource-id="com.android.permissioncontroller:id/permission_allow_button"' 2
check "POST_NOTIFICATIONS granted" "$("$ADB" shell dumpsys package $PKG | tr -d '\r' | grep -c 'POST_NOTIFICATIONS: granted=true')" "1"

# Slots relative to now (SP = UTC-3, no DST): A +2 min (empty), B +3 min (logged), C +4 min
# (empty), D +10 min (skipped). Names without accents: adb reads them back from dumpsys.
"$ADB" shell am force-stop $PKG
pull_db
"$PY" - "$TMP/fibrai.db" <<'EOF'
import sqlite3, sys, datetime
c = sqlite3.connect(sys.argv[1])
now = datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=-3)))
m = now.hour * 60 + now.minute
today = now.date().isoformat()
if m + 10 >= 1440:
    sys.exit("too close to midnight SP: run again after 00:10")
c.execute("update profile set firstDay=?", (today,))
c.execute("delete from meal_slot"); c.execute("delete from meal_log"); c.execute("delete from slot_skip")
for i, (name, off) in enumerate([("Lanche A", 2), ("Lanche B", 3), ("Lanche C", 4), ("Ceia D", 10)]):
    c.execute("insert into meal_slot(id,name,minutesFromMidnight,sortOrder) values(?,?,?,?)", (i + 1, name, m + off, i))
c.execute("insert into meal_log(date,window,text,kcal,p,stable,slotId,carbs,fat,source) values(?,?,?,?,?,1,?,?,?,?)", (today, "", "iogurte", 150, 8, 2, 20, 4, "user"))
c.execute("insert into slot_skip(date,slotId) values(?,?)", (today, 4))
c.commit(); c.execute("pragma wal_checkpoint(TRUNCATE)"); c.execute("pragma journal_mode=DELETE"); c.close()
EOF
[ $? -eq 0 ] || { echo "  ✗ seed failed"; exit 1; }
"$ADB" push "$TMP/fibrai.db" /data/local/tmp/fibrai.db >/dev/null
"$ADB" shell chmod 644 /data/local/tmp/fibrai.db
"$ADB" shell run-as $PKG sh -c "'rm -f databases/fibrai.db-wal databases/fibrai.db-shm; cp /data/local/tmp/fibrai.db databases/fibrai.db'"

# Without SCHEDULE_EXACT_ALARM (Android 14+ default): alarms still set, inexact (window > 0).
"$ADB" shell appops set $PKG SCHEDULE_EXACT_ALARM deny >/dev/null 2>&1
"$ADB" shell am start -W -n $PKG/$ACTIVITY >/dev/null; sleep 4
check "exact denied: A and C scheduled (B logged, D skipped)" "$(alarms)" "2"
check "exact denied: both inexact" "$(exact_alarms)" "0"
# With it: exact. The permission change resyncs through the receiver; the app start does too.
"$ADB" shell appops set $PKG SCHEDULE_EXACT_ALARM allow >/dev/null 2>&1
"$ADB" shell am force-stop $PKG; "$ADB" shell am start -W -n $PKG/$ACTIVITY >/dev/null; sleep 4
check "exact allowed: A and C scheduled" "$(alarms)" "2"
check "exact allowed: both exact (window 0)" "$(exact_alarms)" "2"
"$ADB" shell input keyevent 3 # home: the app in background, alarms must still fire

echo "  … waiting for the real alarms (~4 min)"
for _ in $(seq 1 60); do [ "$(notified 'Lanche A')" -ge 1 ] && break; sleep 5; done
check "A (empty) notified with the spec copy" "$(notified 'Lanche A')" "1"
sleep 70
check "B (logged) never notified" "$(notified 'Lanche B')" "0"
for _ in $(seq 1 30); do [ "$(notified 'Lanche C')" -ge 1 ] && break; sleep 5; done
check "C (empty) notified" "$(notified 'Lanche C')" "1"
check "D (skipped) never notified" "$(notified 'Ceia D')" "0"

# Lock-screen capture (the gold push is a lock screen): swipe keyguard, screen off and on.
"$ADB" shell locksettings set-disabled false >/dev/null
"$ADB" shell input keyevent 26; sleep 1.5; "$ADB" shell input keyevent 26; sleep 2.5
"$ADB" exec-out screencap -p > "$OUT/push.png"; echo "  captured $THEME/push"
"$ADB" shell locksettings set-disabled true >/dev/null
"$ADB" shell wm dismiss-keyguard >/dev/null 2>&1; sleep 1.5

# Pular on A: skip stored, notification gone, no log.
notification_action "Lanche A" "Pular" || { echo "  ✗ Pular action not found"; FAIL=1; }
check "Pular stored the skip of A" "$(sql "select count(*) from slot_skip where slotId=1")" "1"
check "Pular added no log" "$(sql "select count(*) from meal_log where slotId=1")" "0"
check "Pular removed A's notification" "$(notified 'Lanche A')" "0"

# Registrar on C: the app opens on the Chat and the reminder goes away.
notification_action "Lanche C" "Registrar" || { echo "  ✗ Registrar action not found"; FAIL=1; }
sleep 5
expect "Registrar opens the Chat" 'resource-id="chat"'
check "Registrar removed C's notification" "$(notified 'Lanche C')" "0"

rm -rf "$TMP"
exit $FAIL
