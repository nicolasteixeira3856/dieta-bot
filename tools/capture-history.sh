#!/usr/bin/env bash
# A72 (ADR-058) on a running emulator, against the fake server: the Home with the 30-day strip and an extra (home1), a past
# day picked on the strip (homeH), day 1 (homeE), an extra recorded from the Chat (chatGX) with the Extra entry of Trocar,
# a record in yesterday from the Chat, and one capture each of homeW, homeC, homeK and homeP (the strip sits above them).
# Room is seeded with sqlite like capture-home.sh; the dates follow the device's today.
#
# Prereqs: node tools/fake-chat-server.mjs; a devDebug APK built with -PAPI_PUBLIC_URL=http://10.0.2.2:8765; python3;
#   AVD at gold geometry (wm size 780x1688, wm density 320).
# Usage: tools/capture-history.sh dark|light      Then: node tools/diff-gold.mjs dark/home1 dark/homeH ...
set -u
THEME="${1:?dark|light}"
ADB="${ADB:-adb}"
FAKE="${FAKE:-http://127.0.0.1:8765}"
export ADB MSYS_NO_PATHCONV=1
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/docs/qa/android/current/$THEME"
TMP="$(mktemp -d)"
command -v cygpath >/dev/null && TMP="$(cygpath -m "$TMP")"
PY="$(command -v python3 || command -v python)"
PKG=app.fibrai.android.dev
ACTIVITY=app.fibrai.android.MainActivity
EVIDENCE="${EVIDENCE:-$TMP}"
mkdir -p "$OUT"
FAIL=0

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
tap() { dump; local xy; xy=$(at "$1"); [ -z "$xy" ] && { echo "  ✗ not found: $1"; FAIL=1; return 1; }; "$ADB" shell input tap $xy; sleep 1; }
expect() { dump; if grep -q "$2" "$TMP/ui.xml"; then echo "  ✓ $1"; else echo "  ✗ $1"; FAIL=1; fi; }
to_top() { "$ADB" shell input swipe 390 500 390 1500 150; sleep 0.4; "$ADB" shell input swipe 390 500 390 1500 150; sleep 0.5; }
mode() { curl -s -X POST "$FAKE/__mode" -d "$1" >/dev/null; }
shot() { sleep "${2:-1}"; "$ADB" exec-out screencap -p > "$OUT/$1.png"; echo "  captured $THEME/$1"; }

# seed <state>: home1 (today with an extra, 15 days), homeH (the same, 1 de outubro-like day two days back), homeP, homeC, homeK.
seed() {
  "$ADB" shell am force-stop $PKG
  rm -f "$TMP"/fibrai.db*
  for f in fibrai.db fibrai.db-wal fibrai.db-shm; do "$ADB" exec-out run-as $PKG cat databases/$f > "$TMP/$f" 2>/dev/null; done
  "$PY" - "$TMP/fibrai.db" "$1" <<'EOF'
import sqlite3, sys, json, datetime
db, state = sys.argv[1], sys.argv[2]
c = sqlite3.connect(db)
# Today in São Paulo (UTC−3); the first day goes 14 days back (day 15).
today = (datetime.datetime.now(datetime.timezone.utc) - datetime.timedelta(hours=3)).date()
first = today - datetime.timedelta(days=14)
c.execute("update profile set firstDay=?, ceilingMode='same', kcalSame=2000, eat='partial', pct=50", (first.isoformat(),))
ids = [r[0] for r in c.execute("select id from meal_slot order by minutesFromMidnight")]
c.execute("update meal_slot set name='Café da manhã' where id=?", (ids[0],))
for t in ("meal_log", "slot_skip", "closure", "planned_meal", "day"):
    c.execute(f"delete from {t}")
def log(d, i, text, kcal, p, carbs, fat, src="user", kind="slot", time=None, extra=None):
    c.execute("insert into meal_log(date,window,text,kcal,p,stable,slotId,carbs,fat,source,kind,time,extraId) values(?,?,?,?,?,1,?,?,?,?,?,?,?)",
              (d.isoformat(), "", text, kcal, p, None if i is None else ids[i], carbs, fat, src, kind, time, extra))
def workout(d, kcal):
    c.execute("insert into day(date, workoutKcal, removedWindows, askedWindows) values(?, ?, '[]', '[]')", (d.isoformat(), kcal))
# Every past day has one record, except five days back (the dashed circle).
for back in range(1, 15):
    d = today - datetime.timedelta(days=back)
    if back != 5 and back != 2:
        log(d, 0, "Café com pão", 1800, 80, 200, 60)
past = today - datetime.timedelta(days=2)
log(past, 0, "2 pães franceses, 2 ovos mexidos e café com leite", 520, 28, 52, 22)
log(past, 1, "Prato feito: frango grelhado, arroz, feijão e salada", 780, 48, 82, 18, "photo")
log(past, 3, "Arroz, feijão, bife acebolado e salada", 610, 42, 60, 20)
c.execute("insert into slot_skip(date,slotId) values(?,?)", (past.isoformat(), ids[2]))
workout(past, 350)
numbers = {"date": past.isoformat(), "kcal": 1910, "p": 118, "c": 194, "g": 60, "ceiling_kcal": 2175, "workout_kcal": 350, "recorded": True,
           "slots": [{"name": "Café da manhã", "status": "eaten", "kcal": 520}, {"name": "Almoço", "status": "eaten", "kcal": 780},
                     {"name": "Lanche", "status": "skipped"}, {"name": "Jantar", "status": "eaten", "kcal": 610}]}
c.execute("insert into closure(key, period, date, numbers, text, status, createdAtEpochMs, retried) values(?,?,?,?,?,?,0,0)",
          ("day:" + past.isoformat(), "day", past.isoformat(), json.dumps(numbers),
           "1910 de 2175 kcal. Proteína: 118 de 150 g. Lanche pulado; jantar com 610 kcal.", "text"))
# Today (home1): breakfast, lunch by photo, an energy drink at 15:40 as an extra, the snack skipped, workout 350.
log(today, 0, "2 pães franceses, 2 ovos mexidos e café com leite", 520, 28, 52, 22)
lunch = 780 if state in ("homeC", "homeP", "homeK", "homeW") else 620
log(today, 1, "Prato feito: frango grelhado, arroz, feijão e salada", lunch, 48, 82 if lunch == 780 else 42, 18, "photo")
if state in ("home1", "homeH"):
    log(today, None, "Energético, 1 lata (350 ml)", 160, 0, 40, 0, kind="extra", time="15:40", extra=1)
c.execute("insert into slot_skip(date,slotId) values(?,?)", (today.isoformat(), ids[2]))
workout(today, 350)
if state == "homeP":
    c.execute("insert into planned_meal(date, slotId, text, kcal, p, c, g, sourceMessageId) values(?,?,?,?,?,?,?,null)",
              (today.isoformat(), ids[3], "Omelete de forno: 3 ovos, 50 g de ricota e 1 fatia de pão integral", 360, 30, 20, 18))
if state in ("homeC", "homeK"):
    c.execute("insert into closure(key, period, date, numbers, text, status, createdAtEpochMs, retried) values(?,?,?,?,?,?,0,0)",
              ("day:" + today.isoformat(), "day", today.isoformat(), "{}", "1300 de 2175 kcal. Proteína: 76 de 150 g.", "text"))
if state == "homeK":
    monday = today - datetime.timedelta(days=today.weekday())
    days = [{"date": (monday + datetime.timedelta(days=i)).isoformat(), "kcal": k, "p": 118 if k else 0, "c": 200, "g": 60,
             "ceiling_kcal": 2175, "recorded": k > 0} for i, k in enumerate([2210, 1980, 2300, 0, 2240, 2390, 2300])]
    c.execute("insert into closure(key, period, date, numbers, text, status, createdAtEpochMs, retried) values(?,?,?,?,?,?,0,0)",
              ("week:" + monday.isoformat(), "week", monday.isoformat(), json.dumps({"days": days, "over_slot": {"name": "Jantar", "days": 4}}),
               "Semana: média de 2.237 kcal e 118 g de proteína por dia.", "text"))
c.commit()
c.execute("pragma wal_checkpoint(TRUNCATE)")
c.execute("pragma journal_mode=DELETE")
c.close()
print(past.isoformat())
EOF
}
push_db() {
  "$ADB" push "$TMP/fibrai.db" /data/local/tmp/fibrai.db >/dev/null
  "$ADB" shell chmod 644 /data/local/tmp/fibrai.db
  "$ADB" shell run-as $PKG sh -c "'rm -f databases/fibrai.db-wal databases/fibrai.db-shm; cp /data/local/tmp/fibrai.db databases/fibrai.db'"
  "$ADB" shell am start -W -n $PKG/$ACTIVITY >/dev/null
  sleep 3
}

curl -s "$FAKE/__calls" >/dev/null || { echo "  fake server not running on $FAKE"; exit 1; }

# homeE: day 1 after the onboarding, the strip with today alone.
QUICK=1 bash "$ROOT/tools/capture-onboarding.sh" "$THEME" | tail -1
"$ADB" shell am force-stop $PKG; "$ADB" shell am start -W -n $PKG/$ACTIVITY >/dev/null; sleep 3
expect "day 1 strip has today" 'resource-id="home-strip"'
shot homeE

# chatGX: an energy drink recorded by itself as an extra; Trocar offers the Extra entry.
mode '{"actions": "extra"}'
tap 'resource-id="home-fab"'
tap 'resource-id="chat-input"'
"$ADB" shell input text "Tomei%sum%senergetico%sagora,%suma%slata%sde%s350%sml"; sleep 0.5
tap 'resource-id="chat-send"'; sleep 3
expect "extra receipt" 'Registrado como extra'
shot chatGX
tap 'text="Trocar refeição"' && expect "Trocar lists the Extra entry" 'resource-id="chat-sheet-slot-extra"'
"$ADB" exec-out screencap -p > "$EVIDENCE/$THEME-chatGX-trocar.png"
"$ADB" shell input keyevent 4; sleep 0.6
"$ADB" shell input keyevent 4; sleep 1
expect "the extra shows on today's timeline" 'Extra · '
# home1, homeH and the four Release 1 states once each with the strip above them.
PAST=$(seed home1 | tail -1); push_db; shot home1
tap "resource-id=\"home-day-$PAST\"" && expect "past day reads Treino do dia" 'text="Treino do dia"'
to_top; shot homeH
# A record in yesterday from the Chat (the first day is 14 days back now): dinner of yesterday, today unchanged.
TODAY=$(date -d "@$(( $(date +%s) - 3*3600 ))" -u +%Y-%m-%d)
tap "resource-id=\"home-day-$TODAY\"" >/dev/null
tap 'resource-id="home-fab"'
mode '{"actions": "otherday"}'
tap 'resource-id="chat-input"'
"$ADB" shell input text "ontem%sjantei%s2%sfatias%sde%spizza"; sleep 0.5
tap 'resource-id="chat-send"'; sleep 3
expect "past-day receipt names the day" 'Registrado em Jantar · [0-9]* de'
"$ADB" exec-out screencap -p > "$EVIDENCE/$THEME-chat-otherday.png"
mode '{}'
"$ADB" shell input keyevent 4; sleep 1
YESTERDAY=$(date -d "@$(( $(date +%s) - 27*3600 ))" -u +%Y-%m-%d)
tap "resource-id=\"home-day-$YESTERDAY\"" && { "$ADB" shell input swipe 390 1500 390 300 300; sleep 0.8; expect "yesterday shows the pizza" '2 fatias de pizza'; }
"$ADB" exec-out screencap -p > "$EVIDENCE/$THEME-home-yesterday.png"

seed homeW >/dev/null; push_db; tap 'resource-id="home-workout"'; "$ADB" shell input keyevent 4; sleep 0.8; shot homeW
seed homeC >/dev/null; push_db; "$ADB" shell input swipe 390 1400 390 700 300; sleep 0.8; shot homeC
seed homeK >/dev/null; push_db; "$ADB" shell input swipe 390 1400 390 700 300; sleep 0.8; shot homeK
seed homeP >/dev/null; push_db; "$ADB" shell input swipe 390 1500 390 300 300; sleep 0.8; shot homeP
rm -rf "$TMP"
exit $FAIL
