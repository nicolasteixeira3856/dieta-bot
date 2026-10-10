#!/usr/bin/env bash
# A61 part C (ADR-048 decision 3): the system scrolling screenshot on the long product screens, on a running emulator
# (Android 12+, AOSP SystemUI). For Home (full timeline and a closure card), Chat (a thread longer than the screen) and
# Config: the system screenshot (KEYCODE_SYSRQ), "Capture more" / "Capturar mais" in the screenshot UI, the long
# screenshot editor opens, Save, and the saved image is taller than the screen. The long screenshots land in $SCROLL_OUT
# (default: a temp folder, printed at the end); they are evidence, never committed.
#
# Prereqs: devDebug APK installed; AVD at gold geometry (wm size 780x1688, wm density 320); python3.
# Usage: [SCROLL_OUT=<dir>] tools/capture-scroll.sh dark|light
set -u
THEME="${1:?dark|light}"
ADB="${ADB:-adb}"
export ADB MSYS_NO_PATHCONV=1 PYTHONIOENCODING=utf-8
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TMP="$(mktemp -d)"
command -v cygpath >/dev/null && TMP="$(cygpath -m "$TMP")"
OUT="${SCROLL_OUT:-$TMP/long}"
mkdir -p "$OUT"
PY="$(command -v python3 || command -v python)"
PKG=app.fibrai.android.dev
ACTIVITY=app.fibrai.android.MainActivity
FAIL=0

sdk=$("$ADB" shell getprop ro.build.version.sdk | tr -d '\r')
[ "$sdk" -ge 31 ] || { echo "✗ API $sdk: the scrolling screenshot needs Android 12 (API 31) or later"; exit 1; }

QUICK=1 bash "$ROOT/tools/capture-onboarding.sh" "$THEME" | tail -1
"$ADB" shell pm grant $PKG android.permission.POST_NOTIFICATIONS >/dev/null 2>&1

dump() { "$ADB" shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; "$ADB" exec-out cat /sdcard/ui.xml > "$TMP/ui.xml"; }
at() { # at <attribute regex> -> centre "x y" of the first node matching it
  "$PY" - "$1" "$TMP/ui.xml" <<'EOF'
import re, sys
m = re.search(sys.argv[1] + r'[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', open(sys.argv[2], encoding="utf-8").read())
if m:
    x1, y1, x2, y2 = map(int, m.groups())
    print((x1 + x2) // 2, (y1 + y2) // 2)
EOF
}
tap() { dump; local xy; xy=$(at "$1"); [ -z "$xy" ] && { echo "  ✗ not found: $1"; FAIL=1; return 1; }; "$ADB" shell input tap $xy; sleep "${2:-1}"; }
resumed() { "$ADB" shell dumpsys activity activities | grep -E "topResumedActivity|ResumedActivity" | head -1; }

sql() { # sql <python body using c (sqlite3 connection) and today (iso)>
  "$ADB" shell am force-stop $PKG
  rm -f "$TMP"/fibrai.db*
  for f in fibrai.db fibrai.db-wal fibrai.db-shm; do "$ADB" exec-out run-as $PKG cat databases/$f > "$TMP/$f" 2>/dev/null; done
  "$PY" - "$TMP/fibrai.db" "$1" <<'EOF'
import sqlite3, sys, time, datetime, zoneinfo
c = sqlite3.connect(sys.argv[1])
today = datetime.datetime.now(zoneinfo.ZoneInfo("America/Sao_Paulo")).date().isoformat()
exec(sys.argv[2])
c.commit(); c.execute("pragma wal_checkpoint(TRUNCATE)"); c.execute("pragma journal_mode=DELETE"); c.close()
EOF
  "$ADB" push "$TMP/fibrai.db" /data/local/tmp/fibrai.db >/dev/null
  "$ADB" shell chmod 644 /data/local/tmp/fibrai.db
  "$ADB" shell run-as $PKG sh -c "'rm -f databases/fibrai.db-wal databases/fibrai.db-shm; cp /data/local/tmp/fibrai.db databases/fibrai.db'"
}

# longshot <screen>: system screenshot -> Capture more -> the long screenshot editor -> Save -> pull the image.
longshot() {
  local name="$1" xy before after file h
  before=$("$ADB" shell ls -1 /sdcard/Pictures/Screenshots 2>/dev/null | tr -d '\r' | sort | tail -1)
  "$ADB" shell input keyevent KEYCODE_SYSRQ; sleep 2
  dump; xy=$(at 'content-desc="(?:Capture more|Capturar mais)"')
  if [ -z "$xy" ]; then echo "  ✗ $name: no Capture more in the screenshot UI"; FAIL=1; "$ADB" shell input keyevent 4; sleep 1; return 1; fi
  echo "  ✓ $name: Capture more offered"
  "$ADB" shell input tap $xy
  for _ in 1 2 3 4 5 6 7 8 9 10; do sleep 1; resumed | grep -q LongScreenshotActivity && break; done
  if ! resumed | grep -q LongScreenshotActivity; then echo "  ✗ $name: the long screenshot editor did not open"; FAIL=1; return 1; fi
  echo "  ✓ $name: the long screenshot editor opened"
  sleep 2
  tap 'text="(?:Save|Salvar)"' 3 || return 1
  after=$("$ADB" shell ls -1 /sdcard/Pictures/Screenshots 2>/dev/null | tr -d '\r' | sort | tail -1)
  if [ -z "$after" ] || [ "$after" = "$before" ]; then echo "  ✗ $name: nothing saved"; FAIL=1; return 1; fi
  file="$OUT/$THEME-$name.png"
  "$ADB" exec-out cat "/sdcard/Pictures/Screenshots/$after" > "$file"
  h=$("$PY" -c "import struct,sys; d=open(sys.argv[1],'rb').read(24); print(struct.unpack('>I', d[20:24])[0])" "$file" 2>/dev/null || echo 0)
  screen=$("$ADB" shell wm size | tail -1 | sed 's/.*x//' | tr -d '\r')
  if [ "${h:-0}" -gt "${screen:-0}" ]; then echo "  ✓ $name: saved ${h}px tall (screen ${screen}px)"; else echo "  · $name: saved, height not read from the pulled file (${h:-0}px)"; fi
  resumed | grep -q LongScreenshotActivity && { "$ADB" shell input keyevent 4; sleep 1; }
}

echo "  Home: a full timeline and a closure card"
sql '
slots = [r[0] for r in c.execute("select id from meal_slot order by minutesFromMidnight")]
for t in ("meal_log", "chat_message", "closure"): c.execute(f"delete from {t}")
for i, s in enumerate(slots):
    c.execute("insert into meal_log(date,window,text,kcal,p,stable,slotId,carbs,fat,source) values(?,?,?,?,?,1,?,?,?,?)",
              (today, "", f"refeição {i + 1} com arroz, feijão e salada", 400 + 50 * i, 25, s, 40, 12, "user"))
c.execute("insert into closure(key,period,date,numbers,text,status,createdAtEpochMs,retried) values(?,?,?,?,?,?,?,0)",
          (f"day:{today}", "day", today, "{}", "1700 de 2000 kcal. Proteína: 100 de 150 g.", "text", int(time.time() * 1000)))
'
"$ADB" shell am start -W -n $PKG/$ACTIVITY >/dev/null; sleep 3
dump; grep -q 'resource-id="home"' "$TMP/ui.xml" || { echo "  ✗ Home not shown"; FAIL=1; }
longshot home

echo "  Chat: a thread longer than the screen"
sql '
for t in ("chat_message",): c.execute(f"delete from {t}")
now = int(time.time() * 1000)
for i in range(10):
    c.execute("insert into chat_message(date,role,text,createdAtEpochMs) values(?,?,?,?)", (today, "user", f"Pergunta {i + 1}: o que eu como agora?", now - 20000 + i * 2000))
    c.execute("insert into chat_message(date,role,text,createdAtEpochMs) values(?,?,?,?)", (today, "assistant", "Uma resposta longa o bastante para ocupar algumas linhas da conversa. " * 2, now - 19000 + i * 2000))
'
"$ADB" shell am force-stop $PKG; "$ADB" shell am start -W -n $PKG/$ACTIVITY >/dev/null; sleep 3
tap 'resource-id="home-fab"' 2
longshot chat

echo "  Config"
"$ADB" shell am force-stop $PKG; "$ADB" shell am start -W -n $PKG/$ACTIVITY >/dev/null; sleep 3
tap 'resource-id="home-config"' 2
longshot cfg

"$ADB" shell am force-stop $PKG
echo "  long screenshots: $OUT"
exit $FAIL
