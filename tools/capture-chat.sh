#!/usr/bin/env bash
# Chat QA on a running emulator against tools/fake-chat-server.mjs (no OpenAI):
# onboarding -> chat0 -> chatL -> (dark: timeout + retry) -> chatE (Registrar) -> Registrar -> receipt -> chatT
# -> Home ring -> compact -> chatX (A25: 2100 characters block send and photo; back to 2000 sends)
# -> A29: routine over 3 past days -> chatS (Quase igual, Registrar) -> plan + Registrar assim -> chatR -> chatM.
# -> A30: questions before the estimate, Forçar estimativa, 3-round cap -> chatQ.
# -> A34: automatic record, receipt actions (Excluir, Trocar refeição, Desfazer, Editar), Substituir inline,
#    Registrar, skip by text -> chatG, chatU, chatD.
# Captures land in docs/qa/android/current/<theme>/. SCENES=v2 runs only onboarding + the A29 scenes;
# SCENES=a30 only onboarding + the A30 scenes; SCENES=a32 only onboarding + the A32 long-history scenes
# (opens at the bottom, pages of 20 up to 60 days, no jump on a reply while scrolled up, camera closes the keyboard).
#
# Prereqs: node tools/fake-chat-server.mjs running on the host (port 8765);
#   devDebug APK built with -PAPI_PUBLIC_URL=http://10.0.2.2:8765 and installed;
#   AVD at gold geometry (wm size 780x1688, wm density 320); python3; curl.
# SCENES=a34 only onboarding + the A34 scenes.
# SCENES=a54 only onboarding + the A54 scene: an `auto` addition into the empty dinner is recorded, the next DAY says eaten.
# SCENES=a59 only onboarding + the A59 scenes: a meal and a skip in one message (two receipts), a skip over a record
#   (Excluir e pular, Desfazer, Manter registro, expiry on the next send) and the chatSK, chatSD golds.
# SCENES=a50|a55|a57|a58 (or a60 for the four): A60 parts A-D: chatRB (Pode passar, Ajustar, recreation), the tone in
#   Config (cfg, cfgT) and on the wire, the day and week closures through the dev-only broadcast (homeC, homeK, offline,
#   collapse), formatted replies (chatR, chatRK, chatE, plain history and records), the reservation (chatRL, homeP).
# SCENES=a61 only onboarding + A61: a plan's actions under it scroll with the thread while the composer stays; long press
#   selects (1), a tap on the reply adds it (2), a tap on a receipt changes nothing (chatCP), Copiar ends the selection and
#   Colar in the composer gives both messages as plain text (PASTE=1, see the scene); the app's copy confirmation
#   (chatCC) on Android 12 and earlier only (an API 30 AVD at gold geometry); back and ✕ end the selection.
# SCENES=a64 only onboarding + A64: recent_days on the wire, the day balance on the newest receipt, the projection of an
#   `ask` estimate, `Anotado:` for an explicit permanent fact, `Tali está pensando…` after 4 s, and chatF seeded (with the
#   balance line in the receipt) and chatL.
# SCENES=a65 only onboarding + A65: a workout reported in the Chat writes day.workoutKcal with its receipt (Treino registrado),
#   Home shows it, Desfazer restores the previous number, and add sums (Treino somado). No gold: the receipt is Chat/Receipt.
# SCENES=a66 only onboarding + A66: typed actions (the whole day: two records and a skip with Desfazer of the batch; a log and
#   a plan; a clarification on one of two). No new gold: receipts and bubbles of chatSK, chatR, chatQ.
# SKIP_ONBOARDING=1 skips tools/capture-onboarding.sh when the app is already onboarded (a rerun of one scene).
# Usage: [SCENES=v2|a30|a32|a34|a54|a59|a50|a55|a57|a58|a60|a61|a64|a65|a66] tools/capture-chat.sh dark|light
set -u
THEME="${1:?dark|light}"
ADB="${ADB:-adb}"
FAKE="${FAKE:-http://127.0.0.1:8765}"
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

# SKIP_ONBOARDING=1: the app is already onboarded on this emulator (a scene rerun); only the theme and the slot name below.
if [ "${SKIP_ONBOARDING:-0}" = 1 ]; then "$ADB" shell am force-stop $PKG; else bash "$ROOT/tools/capture-onboarding.sh" "$THEME" | tail -1; fi
# Fresh package (A10 .dev): answer the A7 notification prompt up front. capture-push.sh tests the prompt itself.
"$ADB" shell pm grant $PKG android.permission.POST_NOTIFICATIONS >/dev/null 2>&1

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
shot() { sleep "${2:-0.8}"; "$ADB" exec-out screencap -p > "$OUT/$1.png"; echo "  captured $THEME/$1"; }
mode() { curl -s -X POST -d "{\"hang\": $1}" "$FAKE/__mode" >/dev/null; }
calls() { curl -s "$FAKE/__calls" | "$PY" -c "import json,sys; print(json.load(sys.stdin)['calls'])"; }
files_has() { "$ADB" exec-out run-as $PKG ls -1 files/ | tr -d '\r' | grep -qx "$1"; }
fact_keys() { curl -s "$FAKE/__calls" | "$PY" -c "import json,sys; print(' '.join(sorted(f['key'] for f in json.load(sys.stdin)['facts'])))"; }
compacts() { curl -s "$FAKE/__calls" | "$PY" -c "import json,sys; print(json.load(sys.stdin)['compacts'])"; }
db() { # db <sql> -> rows, after a force-stop so the WAL is in the pulled files
  "$ADB" shell am force-stop $PKG
  rm -f "$TMP"/fibrai.db*
  for f in fibrai.db fibrai.db-wal fibrai.db-shm; do "$ADB" exec-out run-as $PKG cat databases/$f > "$TMP/$f" 2>/dev/null; done
  "$PY" -c "import sqlite3,sys; c=sqlite3.connect(sys.argv[1]); print(c.execute(sys.argv[2]).fetchall())" "$TMP/fibrai.db" "$1"
}

# Gold names the first slot "Café da manhã"; adb cannot type accents.
"$ADB" shell am force-stop $PKG
rm -f "$TMP"/fibrai.db*
for f in fibrai.db fibrai.db-wal fibrai.db-shm; do "$ADB" exec-out run-as $PKG cat databases/$f > "$TMP/$f" 2>/dev/null; done
"$PY" - "$TMP/fibrai.db" <<'EOF'
import sqlite3, sys
c = sqlite3.connect(sys.argv[1])
first = c.execute("select id from meal_slot order by minutesFromMidnight").fetchone()[0]
c.execute("update meal_slot set name='Café da manhã' where id=?", (first,))
c.commit(); c.execute("pragma wal_checkpoint(TRUNCATE)"); c.execute("pragma journal_mode=DELETE"); c.close()
EOF
"$ADB" push "$TMP/fibrai.db" /data/local/tmp/fibrai.db >/dev/null
"$ADB" shell chmod 644 /data/local/tmp/fibrai.db
"$ADB" shell run-as $PKG sh -c "'rm -f databases/fibrai.db-wal databases/fibrai.db-shm; cp /data/local/tmp/fibrai.db databases/fibrai.db'"
"$ADB" shell am start -W -n $PKG/$ACTIVITY >/dev/null
sleep 3

if [ "${SCENES:-all}" = all ]; then
mode false
tap 'resource-id="home-fab"' 1.5 && expect "FAB opens Chat" 'resource-id="chat"'
shot chat0

# chatL: the server holds the request.
mode true
tap 'resource-id="chat-suggestion-0"'
tap 'resource-id="chat-send"' 1.5
expect "loading bubble while waiting" 'resource-id="chat-loading"'
shot chatL 0.2
before=$(calls)
if [ "$THEME" = dark ]; then
  echo "  … waiting for the client timeout (60 s)"
  sleep 66
  expect "timeout shows the retry bubble" 'resource-id="chat-failed"'
  mode false
  tap 'resource-id="chat-failed"' 3
else
  # Kill mid-request: nothing was stored, the day stays empty.
  "$ADB" shell am force-stop $PKG
  mode false
  "$ADB" shell am start -W -n $PKG/$ACTIVITY >/dev/null; sleep 3
  tap 'resource-id="home-fab"' 1.5
  expect "killed request left no messages" 'resource-id="chat-greeting"'
  tap 'resource-id="chat-suggestion-0"'
  tap 'resource-id="chat-send"' 3
fi
# A34: the fake sends no `record` here (a server before S14): the estimate offers Registrar only.
expect "estimate with Registrar" 'resource-id="chat-register"'
dump; if grep -q 'resource-id="chat-actions"\|resource-id="chat-skip"' "$TMP/ui.xml"; then echo "  ✗ Gravar | Trocar | Pular still drawn"; FAIL=1; else echo "  ✓ no Gravar | Trocar | Pular"; fi
shot chatE

sent=$(calls)
tap 'resource-id="chat-register"' 1.5
expect "Registrar shows the receipt with its actions" 'resource-id="chat-receipt-delete"'
after=$(calls)
if [ "$after" = "$sent" ]; then echo "  ✓ Registrar made no second POST ($after calls)"; else echo "  ✗ Registrar posted again ($sent -> $after)"; FAIL=1; fi

"$ADB" shell input keyevent 4; sleep 1
expect "Home ring shows the recorded kcal" 'text="380" resource-id="home-consumed"'
rows=$(db "select m.name, l.kcal, l.carbs, l.fat, l.source from meal_log l join meal_slot m on m.id = l.slotId")
echo "  meal_log: $rows"
case "$rows" in *"Café da manhã', 380, 36, 16, 'user'"*) echo "  ✓ logged into the suggested slot";; *) echo "  ✗ unexpected meal_log"; FAIL=1;; esac

# A28: a record applies only the memory_updates the AI proposed. The plain fake estimate has none, so
# no memory.bin. Existence comes from ls: exec-out merges cat's stderr into the pulled bytes.
# The sealed format and the crash mid-write are checked in the A29 routine block.
if files_has memory.bin; then echo "  ✗ memory.bin written by a plain record"; FAIL=1; else echo "  ✓ no memory.bin after a plain record (no memory_updates)"; fi
if files_has memory.txt; then echo "  ✗ A8 memory.txt still there"; FAIL=1; else echo "  ✓ no A8 memory.txt"; fi

# Gold captures: the flow above is the functional check. The gold message has accents adb cannot
# type, so the exact gold conversation is seeded and chatE / chatT are captured again (A34: no chatP in the Chat).
# Since ST7/A30 chatE has no question bubble either: the questions come before the estimate (chatQ).
seed_gold() { # seed_gold <question or empty> [noslot]: an `ask` estimate (A34); noslot = Registrar opens Trocar
"$ADB" shell am force-stop $PKG
rm -f "$TMP"/fibrai.db*
for f in fibrai.db fibrai.db-wal fibrai.db-shm; do "$ADB" exec-out run-as $PKG cat databases/$f > "$TMP/$f" 2>/dev/null; done
"$PY" - "$TMP/fibrai.db" "$1" "${2:-}" <<'EOF'
import sqlite3, sys, time
c = sqlite3.connect(sys.argv[1])
question = sys.argv[2] or None
today = c.execute("select firstDay from profile").fetchone()[0]
first = c.execute("select id from meal_slot order by minutesFromMidnight").fetchone()[0]
if sys.argv[3] == "noslot":
    first = None
for table in ("chat_message", "meal_log", "slot_skip"):
    c.execute(f"delete from {table}")
now = int(time.time() * 1000)
c.execute("insert into chat_message(date,role,text,createdAtEpochMs) values(?,?,?,?)",
          (today, "user", "2 pães franceses com 2 ovos mexidos no café da manhã", now - 2000))
c.execute("insert into chat_message(date,role,text,createdAtEpochMs,estimateKcal,estimateP,estimateC,estimateG,"
          "estimateConfidence,estimateSlotId,estimateItems,estimateQuestion,intent,recordMode) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
          (today, "assistant", "Identifiquei 2 pães franceses e 2 ovos mexidos. A estimativa total é de:", now - 1000,
           380, 22, 36, 16, "medium" if question else "high", first, "2 pães franceses" + chr(10) + "2 ovos mexidos", question, "log", "ask"))
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
tap 'resource-id="home-fab"' 1.5
}
seed_gold ""
dump; if grep -q 'resource-id="chat-question"' "$TMP/ui.xml"; then echo "  ✗ question bubble under the estimate"; FAIL=1; else echo "  ✓ estimate without a question bubble"; fi
expect "gold: Registrar" 'resource-id="chat-register"'
shot chatE
# chatT: an estimate without a slot of today; Registrar opens Trocar with nothing picked.
seed_gold "" noslot
tap 'resource-id="chat-register"' 1
expect "Registrar without a slot opens Trocar" 'resource-id="chat-sheet"'
dump; last=$(grep -o 'resource-id="chat-sheet-slot-[0-9]*"' "$TMP/ui.xml" | tail -1)
tap "$last" 0.6
shot chatT
tap 'resource-id="chat-sheet-cancel"'
seed_gold ""

# A5b compact: the seeded thread is 2 raw. 5 sends make 12; the 6th asks compact=true first.
say() { tap 'resource-id="chat-input"' 0.4; "$ADB" shell input text "$1"; tap 'resource-id="chat-send"' 2.5; }
c0=$(compacts)
for i in 1 2 3 4 5; do say "ovo%s$i"; done
c5=$(compacts)
if [ "$c5" = "$c0" ]; then echo "  ✓ no compact under 12 raw"; else echo "  ✗ compact before 12 raw ($c0 -> $c5)"; FAIL=1; fi
before=$(calls)
say "jantar%sleve"
after=$(calls); c6=$(compacts)
if [ "$c6" = "$((c5 + 1))" ] && [ "$after" = "$((before + 2))" ]; then echo "  ✓ 12 raw: compact + turn in one send"; else echo "  ✗ compact send ($before -> $after calls, $c5 -> $c6 compacts)"; FAIL=1; fi
expect "turn answered after compact" 'resource-id="chat-register"'
dump; if grep -q "Resumo QA" "$TMP/ui.xml"; then echo "  ✗ digest drawn as a bubble"; FAIL=1; else echo "  ✓ digest never drawn"; fi
sent_mem=$(curl -s "$FAKE/__calls" | "$PY" -c "import json,sys; d=json.load(sys.stdin); print(repr(d['memory']), d['facts'])")
if [ "$sent_mem" = "'' []" ]; then echo "  ✓ POST carries memory \"\" and facts [] (A28: nothing invented by a plain record)"; else echo "  ✗ POST memory/facts: $sent_mem"; FAIL=1; fi
digests=$(db "select count(*), max(text) from day_digest")
case "$digests" in *"(1, 'Resumo QA"*) echo "  ✓ day_digest stored: $digests";; *) echo "  ✗ day_digest: $digests"; FAIL=1;; esac
inchat=$(db "select count(*) from chat_message where text like 'Resumo QA%'")
if [ "$inchat" = "[(0,)]" ]; then echo "  ✓ digest not in chat_message"; else echo "  ✗ digest in chat_message"; FAIL=1; fi

# A25 / ADR-022: chatX. Empty day, 2100 characters typed: red border, "Texto muito longo", send and
# camera do nothing (0 POST). Back to 2000: sends, the fake gets 2000 code points. adb cannot type
# accents: the gold message goes without them.
"$ADB" shell am force-stop $PKG
rm -f "$TMP"/fibrai.db*
for f in fibrai.db fibrai.db-wal fibrai.db-shm; do "$ADB" exec-out run-as $PKG cat databases/$f > "$TMP/$f" 2>/dev/null; done
"$PY" - "$TMP/fibrai.db" <<'EOF'
import sqlite3, sys
c = sqlite3.connect(sys.argv[1])
for table in ("chat_message", "day_digest", "meal_log", "slot_skip"):
    c.execute(f"delete from {table}")
c.commit(); c.execute("pragma wal_checkpoint(TRUNCATE)"); c.execute("pragma journal_mode=DELETE"); c.close()
EOF
"$ADB" push "$TMP/fibrai.db" /data/local/tmp/fibrai.db >/dev/null
"$ADB" shell chmod 644 /data/local/tmp/fibrai.db
"$ADB" shell run-as $PKG sh -c "'rm -f databases/fibrai.db-wal databases/fibrai.db-shm; cp /data/local/tmp/fibrai.db databases/fibrai.db'"
"$ADB" shell am start -W -n $PKG/$ACTIVITY >/dev/null
sleep 3
tap 'resource-id="home-fab"' 1.5
composer_len() { # trimmed code points in the composer, from the last dump
  "$PY" -c "
import re, sys, html
xml = open(sys.argv[1], encoding='utf-8').read()
m = re.search(r'text=\"([^\"]*)\"[^>]*resource-id=\"chat-input\"', xml)
print(len(html.unescape(m.group(1)).strip()) if m else -1)" "$TMP/ui.xml"
}
tap 'resource-id="chat-input"' 0.4
# 2100 characters, typed in chunks: one long `input text` overflows the shell argument.
"$PY" - <<'EOF'
import os, subprocess
t = ("Hoje no almoco comi arroz branco, feijao carioca, duas coxas de frango assadas sem pele, salada de alface "
     "com tomate e cebola, uma colher de farofa, meio bife acebolado e de sobremesa um pedaco de pudim de leite. "
     "No lanche da tarde tomei um cafe com leite e comi um pao de queijo grande e uma banana. ")
s = (t * 10)[:2100]
# No space at the ends of 2100 nor of the first 2000: the limit counts the trimmed text.
s = s[:1999] + "x" + s[2000:2099] + "x"
adb = os.environ.get("ADB", "adb")
for i in range(0, len(s), 200):
    subprocess.run([adb, "shell", "input", "text", s[i:i + 200].replace(" ", "%s")], check=True)
EOF
# `input text` sometimes drops a character: top up to exactly 2100 at the end.
dump; missing=$((2100 - $(composer_len)))
[ "$missing" -gt 0 ] && "$ADB" shell input text "$(printf 'x%.0s' $(seq $missing))"
dump; echo "  composer holds $(composer_len) characters"
"$ADB" shell input keyevent 4; sleep 0.8  # hide the keyboard: the gold has none
# A36: the field scrolled to the cursor at the end; the gold shows the first 5 lines. Drag the text down to
# its top: a drag scrolls without moving the cursor, and no key event (keys flip Gboard to its physical
# keyboard mode). Each swipe first finds the field, so a changed screen gets no more input.
scrolled=1
for _ in $(seq 24); do
  dump
  box=$("$PY" -c "
import re, sys
m = re.search(r'resource-id=\"chat-input\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', open(sys.argv[1], encoding='utf-8').read())
print(' '.join(m.groups()) if m else '')" "$TMP/ui.xml")
  [ -z "$box" ] && { echo "  ✗ chat-input gone while scrolling to the top"; FAIL=1; scrolled=0; break; }
  read -r x0 y0 x1 y1 <<< "$box"
  "$ADB" shell input swipe $(((x0 + x1) / 2)) $((y0 + 10)) $(((x0 + x1) / 2)) $((y1 - 10)) 150
done
[ "$scrolled" = 1 ] && echo "  composer dragged down 24 times"
expect "over 2000: Texto muito longo" 'resource-id="chat-too-long"'
shot chatX
before=$(calls)
tap 'resource-id="chat-send"' 1
tap 'resource-id="chat-photo"' 1
dump
if grep -q 'resource-id="chat-photo-camera"' "$TMP/ui.xml"; then echo "  ✗ camera opened the photo sheet"; FAIL=1; "$ADB" shell input keyevent 4; else echo "  ✓ camera did nothing"; fi
after=$(calls)
if [ "$after" = "$before" ]; then echo "  ✓ over 2000: 0 POST"; else echo "  ✗ over 2000 posted ($before -> $after)"; FAIL=1; fi
# The field keeps focus with the cursor at the end: no tap (a tap would move the cursor).
"$ADB" shell input keyevent $(printf '67 %.0s' $(seq 100)); sleep 0.8
dump
if grep -q 'resource-id="chat-too-long"' "$TMP/ui.xml"; then echo "  ✗ still too long at 2000"; FAIL=1; else echo "  ✓ 2000: error gone"; fi
echo "  composer holds $(composer_len) characters"
tap 'resource-id="chat-send"' 3
expect "2000 characters answered" 'resource-id="chat-register"'
len=$(curl -s "$FAKE/__calls" | "$PY" -c "import json,sys; print(json.load(sys.stdin)['textLen'])")
if [ "$len" = 2000 ]; then echo "  ✓ fake got 2000 characters"; else echo "  ✗ fake got $len characters"; FAIL=1; fi
fi

# ------------------------------------------------------------------ helpers (A29, A30)
# The device clock moves (cmd alarm set-time, no root): 3 breakfasts on 3 past days make a strong
# dynamic routine through the real flow (fake memory_updates -> Registrar -> memory.bin, Keystore key).
# Then today at 08:10 (America/Sao_Paulo) the routine card shows (chatS); at 20:15 the plan (chatR)
# and the memory chips (chatM) are captured on seeded threads, like seed_gold.
fake_mode() { curl -s -X POST -d "$1" "$FAKE/__mode" >/dev/null; }
set_clock() { # set_clock <days from today> <HH:MM in Sao Paulo>
  local ms
  ms=$("$PY" -c "
import sys, datetime, zoneinfo
sp = zoneinfo.ZoneInfo('America/Sao_Paulo')
d = datetime.datetime.now(sp).date() + datetime.timedelta(days=int(sys.argv[1]))
h, m = map(int, sys.argv[2].split(':'))
print(int(datetime.datetime(d.year, d.month, d.day, h, m, tzinfo=sp).timestamp() * 1000))" "$1" "$2")
  "$ADB" shell settings put global auto_time 0
  "$ADB" shell cmd alarm set-time "$ms" >/dev/null
}
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
open_chat() { "$ADB" shell am force-stop $PKG; "$ADB" shell am start -W -n $PKG/$ACTIVITY >/dev/null; sleep 3; tap 'resource-id="home-fab"' 1.5; }
CLEAN='for t in ("chat_message", "day_digest", "meal_log", "slot_skip"): c.execute(f"delete from {t}")'
clarify_sent() { curl -s "$FAKE/__calls" | "$PY" -c "import json,sys; c=json.load(sys.stdin)['clarify']; print(c['rounds'], c['force'])"; }
say() { tap 'resource-id="chat-input"' 0.4; "$ADB" shell input text "$1"; tap 'resource-id="chat-send"' 3; }
has() { dump; grep -q "resource-id=\"$1\"" "$TMP/ui.xml"; }

if [ "${SCENES:-all}" = all ] || [ "${SCENES:-all}" = a30 ]; then
# ------------------------------------------------------------------ A30: questions before the estimate (ST7)
echo "  A30: question only, Forçar estimativa from the second"
sql "$CLEAN"
fake_mode '{"clarify": true}'
open_chat
say "jantei%smacarrao%scom%sfrango"
expect "round 1: question bubble" 'resource-id="chat-question"'
if has chat-bot-[0-9]*; then echo "  ✗ reply bubble on a question"; FAIL=1; else echo "  ✓ no reply bubble, no estimate"; fi
if has chat-register || has chat-force-estimate; then echo "  ✗ actions or Forçar on round 1"; FAIL=1; else echo "  ✓ round 1: no actions, no Forçar"; fi
[ "$(clarify_sent)" = "0 False" ] && echo "  ✓ clarify_rounds 0 sent" || { echo "  ✗ sent $(clarify_sent)"; FAIL=1; }
say "creme%sde%sleite"
expect "round 2: Forçar estimativa" 'resource-id="chat-force-estimate"'
[ "$(clarify_sent)" = "1 False" ] && echo "  ✓ clarify_rounds 1 sent" || { echo "  ✗ sent $(clarify_sent)"; FAIL=1; }
before=$(calls)
tap 'resource-id="chat-force-estimate"' 3
expect "Forçar: estimate with Registrar" 'resource-id="chat-register"'
# A32: the thread follows the newest reply; with the keyboard open the tall estimate fills the view.
"$ADB" shell dumpsys input_method | grep -q 'mInputShown=true' && { "$ADB" shell input keyevent 4; sleep 0.8; }
expect "Forçar: user message Pode estimar assim." 'Pode estimar assim\.'
[ "$(clarify_sent)" = "2 True" ] && echo "  ✓ force_estimate sent at round 2" || { echo "  ✗ sent $(clarify_sent)"; FAIL=1; }
[ "$(calls)" = "$((before + 1))" ] && echo "  ✓ one POST" || { echo "  ✗ calls $before -> $(calls)"; FAIL=1; }
if has chat-force-estimate; then echo "  ✗ Forçar still shown"; FAIL=1; else echo "  ✓ Forçar gone"; fi

echo "  A30: 3-round cap"
sql "$CLEAN"
open_chat
say "jantei%smacarrao"
say "creme%sde%sleite"
say "grelhado"
expect "round 3: still a question" 'resource-id="chat-force-estimate"'
say "sem%squeijo"
expect "4th turn: the estimate" 'resource-id="chat-register"'
[ "$(clarify_sent)" = "3 False" ] && echo "  ✓ clarify_rounds 3 sent" || { echo "  ✗ sent $(clarify_sent)"; FAIL=1; }
tap 'resource-id="chat-register"' 2
logged=$(db "select text from meal_log")
case "$logged" in *[Jj]"antei macarrao"*) echo "  ✓ recorded the meal text, not an answer: $logged";; *) echo "  ✗ recorded: $logged"; FAIL=1;; esac
fake_mode '{}'

# Gold thread (accents adb cannot type): the dinner, two question-only turns, Forçar showing.
sql "$CLEAN"'
now = int(time.time() * 1000)
q1 = "O molho branco levou creme de leite ou requeijão? E o macarrão, foi 1 prato raso ou fundo?"
q2 = "O frango foi grelhado ou empanado?"
rows = [("user", "Jantei macarrão com frango ao molho branco", None), ("assistant", "Entendi: macarrão com frango ao molho branco." + chr(10) + q1, q1),
        ("user", "Creme de leite, prato fundo", None), ("assistant", "Entendi: macarrão com frango ao molho branco." + chr(10) + q2, q2)]
for i, (role, text, q) in enumerate(rows):
    c.execute("insert into chat_message(date,role,text,createdAtEpochMs,estimateQuestion,intent) values(?,?,?,?,?,?)",
              (today, role, text, now - (4 - i) * 1000, q, "log" if q else None))
'
open_chat
expect "gold: two question bubbles" 'chat-question.*chat-question'
expect "gold: Forçar estimativa" 'resource-id="chat-force-estimate"'
shot chatQ

# chatE gold after the questions: the estimate alone, no question bubble.
sql "$CLEAN"'
first = c.execute("select id from meal_slot order by minutesFromMidnight").fetchone()[0]
now = int(time.time() * 1000)
c.execute("insert into chat_message(date,role,text,createdAtEpochMs) values(?,?,?,?)", (today, "user", "2 pães franceses com 2 ovos mexidos no café da manhã", now - 2000))
c.execute("insert into chat_message(date,role,text,createdAtEpochMs,estimateKcal,estimateP,estimateC,estimateG,estimateConfidence,estimateSlotId,estimateItems,intent,recordMode) values(?,?,?,?,?,?,?,?,?,?,?,?,?)",
          (today, "assistant", "Identifiquei 2 pães franceses e 2 ovos mexidos. A estimativa total é de:", now - 1000, 380, 22, 36, 16, "high", first, "2 pães franceses" + chr(10) + "2 ovos mexidos", "log", "ask"))
'
open_chat
expect "gold: estimate with Registrar" 'resource-id="chat-register"'
if has chat-question; then echo "  ✗ question bubble under the estimate"; FAIL=1; else echo "  ✓ no question bubble"; fi
[ "${SCENES:-all}" = a30 ] && shot chatE
fi

# ------------------------------------------------------------------ A32: opens at the bottom, pages of 20, keyboard off
if [ "${SCENES:-all}" = all ] || [ "${SCENES:-all}" = a32 ]; then
echo "  A32: long history (66 rows over 6 days + 3 rows 61 days back)"
sql "$CLEAN"'
now = int(time.time() * 1000)
day = 86400000
d0 = datetime.date.fromisoformat(today)
for k in range(6):
    date = (d0 - datetime.timedelta(days=5 - k)).isoformat()
    for i in range(11):
        c.execute("insert into chat_message(date,role,text,createdAtEpochMs) values(?,?,?,?)",
                  (date, "user", "msg%02d" % (k * 11 + i), now - (5 - k) * day - (11 - i) * 60000))
old = (d0 - datetime.timedelta(days=61)).isoformat()
for i in range(3):
    c.execute("insert into chat_message(date,role,text,createdAtEpochMs) values(?,?,?,?)", (old, "user", "velha%d" % i, now - 61 * day + i * 1000))
'
fake_mode '{}'
"$ADB" shell am force-stop $PKG; "$ADB" shell am start -W -n $PKG/$ACTIVITY >/dev/null; sleep 3
# First frame already at the bottom: a 3 s recording from the FAB tap (0.6 s in). screenrecord writes a
# frame only when the screen changes; from 1.5 s (tap + 0.9 s, the navigation fade is over) every frame
# must equal the last one in the thread area. The old scroll from the top still moved there (diff ~12).
dump; fab=$(at 'resource-id="home-fab"')
"$ADB" shell screenrecord --time-limit 3 /sdcard/a32-open.mp4 & rec=$!
sleep 0.6; "$ADB" shell input tap $fab; wait $rec
"$ADB" pull /sdcard/a32-open.mp4 "$TMP/a32-open.mp4" >/dev/null 2>&1
ffmpeg -loglevel error -i "$TMP/a32-open.mp4" -vf fps=20 "$TMP/open%03d.png"
moved=$("$PY" - "$TMP" <<'PYEOF'
import glob, sys
import numpy as np
from PIL import Image
frames = sorted(glob.glob(sys.argv[1] + "/open*.png"))
def thread(f):
    a = np.asarray(Image.open(f).convert("L"), dtype=float)
    return a[int(a.shape[0] * 0.10):int(a.shape[0] * 0.80)]
last = thread(frames[-1])
print(round(max([float(np.abs(thread(f) - last).mean()) for f in frames[30:]] or [0.0]), 1))
PYEOF
)
frames=$(ls "$TMP"/open*.png | wc -l)
"$PY" -c "import sys; sys.exit(0 if float(sys.argv[1]) < 3 else 1)" "$moved"   && echo "  ✓ open: settled with the navigation fade, no scroll after it ($frames frames at 20 fps, diff $moved)"   || { echo "  ✗ open: the thread still moved after the fade (diff $moved)"; FAIL=1; }
expect "open: newest message on screen" 'text="msg65"'
seen_indicator=0
for i in $(seq 1 40); do
  "$ADB" shell input swipe 390 500 390 1400 120
  dump
  grep -q 'resource-id="chat-loading-older"' "$TMP/ui.xml" && seen_indicator=1
  grep -q 'text="msg00"' "$TMP/ui.xml" && break
done
expect "scroll up: reaches the oldest row of the 60 days" 'text="msg00"'
[ "$seen_indicator" = 1 ] && echo "  ✓ indicator seen while a page loaded" || echo "  · indicator not caught (Room answered before the dump)"
for i in 1 2 3; do "$ADB" shell input swipe 390 500 390 1400 120; done
dump; if grep -q 'text="velha' "$TMP/ui.xml"; then echo "  ✗ a row older than 60 days drawn"; FAIL=1; else echo "  ✓ stops at 60 days"; fi

echo "  A32: a reply while scrolled up does not jump; a send at the bottom follows"
open_chat
fake_mode '{"delay": 8000}'
say "dois%sovos"
for i in 1 2 3; do "$ADB" shell input swipe 390 500 390 1400 120; done
sleep 0.5; dump; top=$("$PY" -c "import re,sys; t=re.findall(r'text=\"(msg\d\d)\"', open(sys.argv[1], encoding='utf-8').read()); print(t[0] if t else '')" "$TMP/ui.xml")
sleep 8
dump
if grep -q 'Identifiquei' "$TMP/ui.xml"; then echo "  ✗ the reply pulled the thread down"; FAIL=1; else echo "  ✓ scrolled up: the reply landed off screen"; fi
[ -n "$top" ] && grep -q "text=\"$top\"" "$TMP/ui.xml" && echo "  ✓ $top still on screen" || { echo "  ✗ position moved (was $top)"; FAIL=1; }
fake_mode '{}'
open_chat
expect "reopen: at the bottom with the reply" 'Identifiquei'
say "pao%sde%squeijo"
# Keyboard down: the tall reply plus its question fit the thread again. A34: only the newest open
# answer keeps "Deseja registrar…" (the earlier one expired), so it marks the new reply on screen.
"$ADB" shell dumpsys input_method | grep -q 'mInputShown=true' && { "$ADB" shell input keyevent 4; sleep 0.8; }
expect "send at the bottom: followed to the new reply" 'Deseja registrar essa refei'

echo "  A32: camera closes the keyboard"
tap 'resource-id="chat-input"' 1
"$ADB" shell dumpsys input_method | grep -q 'mInputShown=true' && echo "  ✓ keyboard open" || { echo "  ✗ keyboard did not open"; FAIL=1; }
tap 'resource-id="chat-photo"' 1
"$ADB" shell dumpsys input_method | grep -q 'mInputShown=true' && { echo "  ✗ keyboard still open with the sheet"; FAIL=1; } || echo "  ✓ keyboard closed"
expect "photo sheet open" 'resource-id="chat-photo-sheet"'
"$ADB" exec-out screencap -p > "$TMP/a32-photo-sheet.png"
tap 'resource-id="chat-photo-cancel"' 1
"$ADB" shell dumpsys input_method | grep -q 'mInputShown=true' && { echo "  ✗ keyboard reopened on close"; FAIL=1; } || echo "  ✓ sheet closed, keyboard stays closed"
fi

# ------------------------------------------------------------------ A29: chatS, chatR, chatM (ST6)
if [ "${SCENES:-all}" = all ] || [ "${SCENES:-all}" = v2 ]; then
echo "  A29 routine: 3 breakfasts on 3 past days"
# Emulator clock and host clock can disagree by a day at midnight: both use Sao Paulo dates.
sql "$CLEAN"
fake_mode '{"routine": true}'
for back in 3 2 1; do
  set_clock "-$back" 08:10
  open_chat
  dump
  if grep -q 'resource-id="chat-routine"' "$TMP/ui.xml"; then echo "  ✗ card before 3 days (day -$back)"; FAIL=1; else echo "  ✓ no card on day -$back"; fi
  tap 'resource-id="chat-input"' 0.4; "$ADB" shell input text "cafe%sde%ssempre"; tap 'resource-id="chat-send"' 3
  expect "day -$back: estimate with Registrar" 'resource-id="chat-register"'
  [ "$back" = 3 ] && expect "day -3: Memória atualizada on the answer (leite)" 'resource-id="chat-memory-updated"'
  [ "$back" = 1 ] && expect "day -1: origin chips (permanente + dinâmica)" 'chat-memory-permanent.*chat-memory-dynamic'
  # A8b intent on the A28 file: the stale memory.bin.new left below never reaches the memory.
  if [ "$back" = 2 ]; then
    keys=$(fact_keys)
    if [ "$keys" = "cafe leite" ]; then echo "  ✓ stale memory.bin.new ignored, memory intact (facts: $keys)"; else echo "  ✗ facts after crash mid-write: '$keys'"; FAIL=1; fi
  fi
  tap 'resource-id="chat-register"' 2
  expect "day -$back: receipt + Memória atualizada (routine applied)" 'resource-id="chat-receipt-[0-9]+"'
  if [ "$back" = 3 ]; then
    # A28 memory.bin: "NM" + version 1 + 12-byte IV + AES-GCM ciphertext + 16-byte tag over {"v":2,...}.
    if files_has memory.bin; then
      "$ADB" exec-out run-as $PKG cat files/memory.bin > "$TMP/memory.raw"
      sealed=$("$PY" -c "
import sys
b = open(sys.argv[1], 'rb').read()
plain = any(w in b for w in (b'cafe', b'leite', b'semidesnatado', b'routine', b'\"v\":2'))
print(len(b), b[:3].hex(), 'plain' if plain else 'sealed')" "$TMP/memory.raw")
      read -r msize mhead mplain <<< "$sealed"
      if [ "$msize" -gt 31 ] && [ "$mhead" = 4e4d01 ]; then echo "  ✓ memory.bin sealed ($msize bytes, NM v1 header)"; else echo "  ✗ memory.bin format: $sealed"; FAIL=1; fi
      if [ "$mplain" = sealed ]; then echo "  ✓ memory.bin raw shows no fact (not plaintext)"; else echo "  ✗ memory.bin is plaintext"; FAIL=1; fi
    else
      echo "  ✗ memory.bin missing after the routine record"; FAIL=1
    fi
    # Crash mid-write: a half-written memory.bin.new must not touch the memory (checked on day -2).
    "$ADB" shell am force-stop $PKG
    "$ADB" shell run-as $PKG sh -c "'echo lixo-de-crash > files/memory.bin.new'"
  fi
  if [ "$back" = 2 ]; then
    if files_has memory.bin.new; then echo "  ✗ memory.bin.new left after the next write"; FAIL=1; else echo "  ✓ next write replaced memory.bin.new atomically"; fi
  fi
done
facts=$(curl -s "$FAKE/__calls" | "$PY" -c "import json,sys; print([(f['id'], f['days_seen']) for f in json.load(sys.stdin)['facts']])")
echo "  facts sent on day -1: $facts"

echo "  A29 chatS: today 08:10, café empty"
set_clock 0 08:10
fake_mode '{}'
open_chat
expect "routine card for Café da manhã" 'resource-id="chat-routine"'
expect "card says O de sempre no Café da manhã?" 'O de sempre no Caf'
expect "dynamic memory chip" 'resource-id="chat-memory-dynamic"'
shot chatS
before=$(calls)
tap 'resource-id="chat-routine-edit"' 1.2
dump
case "$(grep -o 'text="[^"]*"[^>]*resource-id="chat-input"' "$TMP/ui.xml")" in *"2 ovos mexidos"*) echo "  ✓ Quase igual filled the composer";; *) echo "  ✗ composer not filled"; FAIL=1;; esac
if "$ADB" shell dumpsys input_method | grep -q "mInputShown=true"; then echo "  ✓ keyboard open"; else echo "  ✗ keyboard closed"; FAIL=1; fi
"$ADB" shell input keyevent 4; sleep 0.6
tap 'resource-id="chat-routine-record"' 2
expect "Registrar: receipt with Memória atualizada" 'resource-id="chat-memory-updated"'
after=$(calls)
if [ "$after" = "$before" ]; then echo "  ✓ Registrar made no POST"; else echo "  ✗ Registrar posted ($before -> $after)"; FAIL=1; fi
dump; if grep -q 'resource-id="chat-routine"' "$TMP/ui.xml"; then echo "  ✗ card still there after Registrar"; FAIL=1; else echo "  ✓ card gone after Registrar"; fi
logged=$(db "select m.name, l.kcal, l.p, l.carbs, l.fat, l.source from meal_log l join meal_slot m on m.id = l.slotId order by l.id desc limit 1")
case "$logged" in *"Café da manhã', 440, 25, 38, 22, 'routine'"*) echo "  ✓ routine logged: $logged";; *) echo "  ✗ routine log: $logged"; FAIL=1;; esac

echo "  A29 chatR: plan -> Registrar assim"
set_clock 0 20:15
sql "$CLEAN"
fake_mode '{"plan": true}'
open_chat
tap 'resource-id="chat-input"' 0.4; "$ADB" shell input text "vou%sfazer%spizza"; tap 'resource-id="chat-send"' 3
expect "plan: projected day panel" 'resource-id="chat-plan-panel"'
expect "plan: Registrar assim" 'resource-id="chat-record-plan"'
dump; if grep -q 'resource-id="chat-register"' "$TMP/ui.xml"; then echo "  ✗ Registrar on a plan"; FAIL=1; else echo "  ✓ only Registrar assim"; fi
tap 'resource-id="chat-record-plan"' 2
expect "Registrar assim: receipt" 'resource-id="chat-receipt-[0-9]+"'
logged=$(db "select m.name, l.kcal, l.source from meal_log l join meal_slot m on m.id = l.slotId")
case "$logged" in *"Jantar', 420, 'user'"*) echo "  ✓ plan logged in Jantar: $logged";; *) echo "  ✗ plan log: $logged"; FAIL=1;; esac
fake_mode '{}'

# Gold thread: 1.640 kcal eaten (86P 152C 46G), ceiling 2.200, targets 167/223/74 (ST6 chatR).
sql "$CLEAN"'
slots = [r[0] for r in c.execute("select id from meal_slot order by minutesFromMidnight")]
c.execute("update profile set ceilingMode=?, kcalSame=2200, eat=?, proteinTargetG=167, carbTargetG=223, fatTargetG=74", ("same", "zero"))
c.execute("update day set workoutKcal=null")
for slot, kcal, p, carbs, fat in [(slots[0], 440, 25, 38, 22), (slots[1], 820, 45, 76, 14), (slots[2], 380, 16, 38, 10)]:
    c.execute("insert into meal_log(date,window,text,kcal,p,stable,slotId,carbs,fat,source) values(?,?,?,?,?,1,?,?,?,?)", (today, "", "x", kcal, p, slot, carbs, fat, "user"))
now = int(time.time() * 1000)
reply = chr(10).join(["Para caber nas 560 kcal que sobram hoje:", "• 1 pão sírio (60 g)", "• 2 colheres de sopa de molho de tomate (30 g)",
    "• 100 g de frango desfiado", "• 30 g de milho", "• 30 g de muçarela", "Monte e leve ao forno a 200 °C por 8 a 10 min.", "Total: ~420 kcal · 40P · 38C · 12G"])
c.execute("insert into chat_message(date,role,text,createdAtEpochMs) values(?,?,?,?)", (today, "user", "Vou fazer uma pizza de pão sírio na janta. Quantas gramas de cada item?", now - 2000))
c.execute("insert into chat_message(date,role,text,createdAtEpochMs,estimateKcal,estimateP,estimateC,estimateG,estimateConfidence,estimateSlotId,intent) values(?,?,?,?,?,?,?,?,?,?,?)",
          (today, "assistant", reply, now - 1000, 420, 40, 38, 12, "high", slots[3], "plan"))
'
open_chat
expect "gold plan: Dia 1.640 -> 2.060 de 2.200" '1\.640.*2\.060.*2\.200'
shot chatR

echo "  A29 chatM: memory chips"
sql "$CLEAN"'
first = c.execute("select id from meal_slot order by minutesFromMidnight").fetchone()[0]
now = int(time.time() * 1000)
c.execute("insert into chat_message(date,role,text,createdAtEpochMs) values(?,?,?,?)", (today, "user", "Café da manhã igual ao de sempre, mas hoje com pão integral", now - 2000))
c.execute("insert into chat_message(date,role,text,createdAtEpochMs,estimateKcal,estimateP,estimateC,estimateG,estimateConfidence,estimateSlotId,intent,memoryUsedKinds,memoryUpdated,recordMode) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
          (today, "assistant", "Usei o seu café de sempre, com pão integral no lugar do francês e leite semidesnatado, como você costuma usar.", now - 1000, 430, 26, 36, 20, "high", first, "log", "permanent,dynamic", 1, "ask"))
'
open_chat
expect "chips in order" 'chat-memory-updated.*chat-memory-permanent.*chat-memory-dynamic'
shot chatM
fi

# ------------------------------------------------------------------ A34: autonomous record (ST9)
if [ "${SCENES:-all}" = all ] || [ "${SCENES:-all}" = a34 ]; then
# The reversed thread lists its newest items first in the dump; the keyboard can cover a new receipt.
kb_off() { "$ADB" shell dumpsys input_method | grep -q 'mInputShown=true' && { "$ADB" shell input keyevent 4; sleep 0.8; }; }
auto_sent() { curl -s "$FAKE/__calls" | "$PY" -c "import json,sys; print(json.load(sys.stdin)['autoRecord'])"; }
set_clock 0 20:15
echo "  A34: a clear meal is recorded by itself"
sql "$CLEAN"
fake_mode '{"record": "auto"}'
open_chat
say "comi%s2%spaes%se%s2%sovos"; kb_off
expect "auto: receipt with Excluir · Trocar refeição · Editar" 'chat-receipt-delete.*chat-receipt-move.*chat-receipt-edit'
if has chat-register; then echo "  ✗ Registrar on an automatic record"; FAIL=1; else echo "  ✓ no Registrar, no tap"; fi
[ "$(auto_sent)" = "True" ] && echo "  ✓ auto_record true on the wire" || { echo "  ✗ auto_record sent: $(auto_sent)"; FAIL=1; }
rows=$(db "select m.name, l.kcal from meal_log l join meal_slot m on m.id = l.slotId")
case "$rows" in "[('Café da manhã', 380)]") echo "  ✓ recorded in the suggested slot: $rows";; *) echo "  ✗ meal_log: $rows"; FAIL=1;; esac
state=$(db "select recordMode, recordState from chat_message where role = 'assistant'")
[ "$state" = "[('auto', 'recorded')]" ] && echo "  ✓ answer stored auto / recorded" || { echo "  ✗ answer state: $state"; FAIL=1; }

echo "  A34: Excluir"
open_chat
tap 'resource-id="chat-receipt-delete"' 1.5
expect "Excluir marks the receipt" 'text="Excluído"'
if has chat-receipt-delete; then echo "  ✗ actions left after Excluir"; FAIL=1; else echo "  ✓ no actions after Excluir"; fi
[ "$(db "select count(*) from meal_log")" = "[(0,)]" ] && echo "  ✓ record deleted, no confirmation" || { echo "  ✗ meal_log after Excluir"; FAIL=1; }

echo "  A34: Trocar refeição to an empty slot, then Desfazer"
open_chat
say "comi%spao%scom%sovo"; kb_off
tap 'resource-id="chat-receipt-move"' 1
expect "Trocar refeição opens the sheet" 'resource-id="chat-sheet"'
expect "the record's slot is marked (atual)" '\(atual\)'
dump; last=$(grep -o 'resource-id="chat-sheet-slot-[0-9]*"' "$TMP/ui.xml" | tail -1)
tap "$last" 0.6
tap 'resource-id="chat-sheet-confirm"' 1.5
expect "moved receipt" 'Movido para'
rows=$(db "select m.name from meal_log l join meal_slot m on m.id = l.slotId")
case "$rows" in "[('Jantar',)]"|"[('Ceia',)]") echo "  ✓ record moved: $rows";; *) echo "  ✗ after move: $rows"; FAIL=1;; esac
open_chat
tap 'resource-id="chat-receipt-undo"' 1.5
expect "Desfazer of a move: Restaurado" 'Restaurado em'
rows=$(db "select m.name from meal_log l join meal_slot m on m.id = l.slotId")
[ "$rows" = "[('Café da manhã',)]" ] && echo "  ✓ back in the café" || { echo "  ✗ after undo: $rows"; FAIL=1; }

echo "  A34: the slot already has a record -> Substituir inside the conversation, then Desfazer"
fake_mode '{"record": "auto", "kcal": 620, "reply": "Juntei ao café."}'
open_chat
say "tambem%scomi%sum%spudim"; kb_off
expect "inline confirmation below the answer" 'resource-id="chat-replace-card"'
[ "$(db "select kcal from meal_log")" = "[(380,)]" ] && echo "  ✓ nothing changed before Substituir" || { echo "  ✗ meal_log changed before Substituir"; FAIL=1; }
open_chat
tap 'resource-id="chat-replace-confirm"' 1.5
expect "replacement receipt" 'Atualizado em'
[ "$(db "select kcal from meal_log")" = "[(620,)]" ] && echo "  ✓ one record, 620 kcal" || { echo "  ✗ after Substituir: $(db "select kcal from meal_log")"; FAIL=1; }
open_chat
tap 'resource-id="chat-receipt-undo"' 1.5
expect "Desfazer: Desfeito" 'text="Desfeito"'
expect "Desfazer: Restaurado" 'Restaurado em'
[ "$(db "select kcal from meal_log")" = "[(380,)]" ] && echo "  ✓ previous record restored" || { echo "  ✗ after Desfazer: $(db "select kcal from meal_log")"; FAIL=1; }

echo "  A34: Editar puts the text back in the composer"
open_chat
tap 'resource-id="chat-receipt-edit"' 1.5
expect "Removido para editar" 'Removido para editar'
dump
case "$(grep -o 'text="[^"]*"[^>]*resource-id="chat-input"' "$TMP/ui.xml")" in *"comi pao com ovo"*) echo "  ✓ composer holds the record text";; *) echo "  ✗ composer not filled"; FAIL=1;; esac
if "$ADB" shell dumpsys input_method | grep -q "mInputShown=true"; then echo "  ✓ keyboard open"; else echo "  ✗ keyboard closed"; FAIL=1; fi
"$ADB" shell input keyevent 4; sleep 0.6
[ "$(db "select count(*) from meal_log")" = "[(0,)]" ] && echo "  ✓ record removed for editing" || { echo "  ✗ meal_log after Editar"; FAIL=1; }

echo "  A34: unsure meal -> Registrar; expires on the next send"
sql "$CLEAN"
fake_mode '{"record": "ask"}'
open_chat
say "pudim%sde%sleite%scom%scalda"; kb_off
expect "ask: one Registrar" 'resource-id="chat-register"'
[ "$(db "select count(*) from meal_log")" = "[(0,)]" ] && echo "  ✓ nothing recorded without a tap" || { echo "  ✗ recorded without a tap"; FAIL=1; }
open_chat
say "e%sum%scafe"; kb_off
open_chat
expect "the first one is Não registrado" 'resource-id="chat-not-recorded"'
tap 'resource-id="chat-register"' 1.5
expect "Registrar records the latest" 'resource-id="chat-receipt-delete"'

echo "  A34: skip by text"
sql "$CLEAN"
fake_mode '{"skip": "Jan"}'
open_chat
say "hoje%snao%svou%sjantar"; kb_off
expect "skip receipt" 'Pulado'
expect "skip: Desfazer only" 'resource-id="chat-receipt-undo"'
[ "$(db "select count(*) from slot_skip")" = "[(1,)]" ] && echo "  ✓ slot skipped" || { echo "  ✗ slot_skip: $(db "select count(*) from slot_skip")"; FAIL=1; }

echo "  A34: another day is never recorded"
sql "$CLEAN"
fake_mode '{"record": "none", "reply": "So registro as refeicoes de hoje."}'
open_chat
say "ontem%sjantei%spizza"; kb_off
if has chat-register || has chat-receipt-delete; then echo "  ✗ action on another day"; FAIL=1; else echo "  ✓ no record, no Registrar"; fi
[ "$(db "select count(*) from meal_log")" = "[(0,)]" ] && echo "  ✓ meal_log empty" || { echo "  ✗ recorded another day"; FAIL=1; }
fake_mode '{}'

# Gold threads (accents adb cannot type), stored as the app writes them: undo data matches the slot.
A34_SEED='
import json
slots = {n: i for i, n in c.execute("select id, name from meal_slot")}
cafe = next(i for n, i in slots.items() if n.startswith("Caf"))
jantar = next(i for n, i in slots.items() if n.startswith("Jan"))
now = int(time.time() * 1000)
def rec(text, kcal, p, cc, g, source="user"):
    return {"text": text, "kcal": kcal, "p": p, "c": cc, "g": g, "source": source, "window": "", "stable": True}
def state(*records):
    return {"records": list(records), "skipped": False}
def log(slot, r):
    c.execute("insert into meal_log(date,window,text,kcal,p,stable,slotId,carbs,fat,source) values(?,?,?,?,?,1,?,?,?,?)",
              (today, "", r["text"], r["kcal"], r["p"], slot, r["c"], r["g"], r["source"]))
def msg(role, text, at, **kw):
    cols = ["date", "role", "text", "createdAtEpochMs"] + list(kw)
    c.execute(f"insert into chat_message({chr(44).join(cols)}) values({chr(44).join(chr(63) * len(cols))})", [today, role, text, now - at] + list(kw.values()))
'
echo "  A34 gold: chatG"
sql "$CLEAN$A34_SEED"'
eggs = rec("2 pães franceses com 2 ovos mexidos no café da manhã", 380, 22, 36, 16)
log(cafe, eggs)
msg("user", eggs["text"], 3000)
msg("assistant", "Identifiquei 2 pães franceses e 2 ovos mexidos.", 2000, estimateKcal=380, estimateP=22, estimateC=36, estimateG=16,
    estimateConfidence="high", estimateSlotId=cafe, estimateItems="2 pães franceses" + chr(10) + "2 ovos mexidos", intent="log", recordMode="auto", recordState="recorded")
msg("logged", "Café da manhã", 1000, estimateKcal=380, estimateSlotId=cafe, recordSource="user",
    undoData=json.dumps({"slots": [{"date": today, "slotId": cafe, "before": state(), "after": state(eggs)}]}))
'
open_chat
expect "gold chatG: three actions" 'chat-receipt-delete.*chat-receipt-move.*chat-receipt-edit'
shot chatG

echo "  A34 gold: chatU"
sql "$CLEAN$A34_SEED"'
dinner = rec("arroz, feijão e frango grelhado", 380, 30, 40, 9)
log(jantar, dinner)
msg("user", "Também comi um pudim de leite no jantar", 2000)
msg("assistant", "Juntei o pudim ao jantar. A estimativa total é de:", 1000, estimateKcal=620, estimateP=30, estimateC=82, estimateG=19,
    estimateConfidence="high", estimateSlotId=jantar, intent="log", recordMode="auto", recordState="pending_replace",
    undoData=json.dumps({"date": today, "slotId": jantar, "before": state(dinner), "after": state(dinner)}))
'
open_chat
expect "gold chatU: Substituir Jantar?" 'text="Substituir Jantar\?"'
expect "gold chatU: copy" 'text="Jantar tem 380 kcal. Fica com 620 kcal."'
shot chatU

echo "  A34 gold: chatD"
sql "$CLEAN$A34_SEED"'
dinner = rec("arroz, feijão e frango grelhado", 380, 30, 40, 9)
pudding = rec("arroz, feijão, frango grelhado e pudim de leite", 620, 30, 82, 19)
log(jantar, dinner)
msg("user", "Também comi um pudim de leite no jantar", 4000)
msg("assistant", "Juntei o pudim ao jantar.", 3000, estimateKcal=620, estimateP=30, estimateC=82, estimateG=19,
    estimateConfidence="high", estimateSlotId=jantar, intent="log", recordMode="auto", recordState="recorded")
msg("replaced", "Jantar", 2000, estimateKcal=620, estimateSlotId=jantar, recordSource="user", receiptState="undone",
    undoData=json.dumps({"slots": [{"date": today, "slotId": jantar, "before": state(dinner), "after": state(pudding)}]}))
msg("restored", "Jantar", 1000, estimateKcal=380, estimateSlotId=jantar, recordSource="user",
    undoData=json.dumps({"slots": [{"date": today, "slotId": jantar, "before": state(pudding), "after": state(dinner)}]}))
'
open_chat
expect "gold chatD: Desfeito" 'text="Desfeito"'
expect "gold chatD: Restaurado with actions" 'chat-receipt-delete'
expect "gold chatD: Restaurado em Jantar" 'Restaurado em Jantar'
shot chatD
fi

if [ "${SCENES:-all}" = all ] || [ "${SCENES:-all}" = a59 ]; then
kb_off() { "$ADB" shell dumpsys input_method | grep -q 'mInputShown=true' && { "$ADB" shell input keyevent 4; sleep 0.8; }; }
skips_sent() { curl -s "$FAKE/__calls" | "$PY" -c "import json,sys; print(json.load(sys.stdin)['skipSlots'])"; }
# Slots by name, the seeding helpers of A34 and a lunch record (the skip over a record).
A59_SEED='
import json
slots = {n: i for i, n in c.execute("select id, name from meal_slot")}
cafe = next(i for n, i in slots.items() if n.startswith("Caf"))
almoco = next(i for n, i in slots.items() if n.startswith("Alm"))
now = int(time.time() * 1000)
def rec(text, kcal, p, cc, g, source="user"):
    return {"text": text, "kcal": kcal, "p": p, "c": cc, "g": g, "source": source, "window": "", "stable": True}
def state(*records, skipped=False):
    return {"records": list(records), "skipped": skipped}
def log(slot, r):
    c.execute("insert into meal_log(date,window,text,kcal,p,stable,slotId,carbs,fat,source) values(?,?,?,?,?,1,?,?,?,?)",
              (today, "", r["text"], r["kcal"], r["p"], slot, r["c"], r["g"], r["source"]))
def msg(role, text, at, **kw):
    cols = ["date", "role", "text", "createdAtEpochMs"] + list(kw)
    c.execute(f"insert into chat_message({chr(44).join(cols)}) values({chr(44).join(chr(63) * len(cols))})", [today, role, text, now - at] + list(kw.values()))
    return c.execute("select max(id) from chat_message").fetchone()[0]
lunch = rec("arroz, feijão e frango grelhado", 640, 42, 70, 18)
'
LUNCH='log(almoco, lunch)'
set_clock 0 12:40

echo "  A59: a meal and a skip in one message -> two receipts"
sql "$CLEAN"
fake_mode '{"record": "auto", "skips": ["Alm"], "reply": "Almoco de hoje fora. Identifiquei 2 paes e 2 ovos."}'
open_chat
say "pulei%so%salmoco,%sno%scafe%scomi%s2%spaes%se%s2%sovos"; kb_off
[ "$(skips_sent)" = "True" ] && echo "  ✓ skip_slots true on the wire" || { echo "  ✗ skip_slots sent: $(skips_sent)"; FAIL=1; }
expect "two receipts: Registrado em, then Pulado" 'Pulado.*Registrado em'
rows=$(db "select m.name, l.kcal from meal_log l join meal_slot m on m.id = l.slotId")
case "$rows" in "[('Café da manhã', 380)]") echo "  ✓ breakfast recorded: $rows";; *) echo "  ✗ meal_log: $rows"; FAIL=1;; esac
rows=$(db "select m.name from slot_skip s join meal_slot m on m.id = s.slotId")
case "$rows" in "[('Almoço',)]"|"[('Almoco',)]") echo "  ✓ lunch skipped: $rows";; *) echo "  ✗ slot_skip: $rows"; FAIL=1;; esac
rows=$(db "select count(*) from chat_message where role = 'skipped'")
[ "$rows" = "[(1,)]" ] && echo "  ✓ one skip receipt" || { echo "  ✗ skip receipts: $rows"; FAIL=1; }

echo "  A59: skipping a meal with a record asks first (chatSD), Excluir e pular, then Desfazer"
sql "$CLEAN$A59_SEED$LUNCH"
fake_mode '{"skip": "Alm", "skips": ["Alm"], "reply": "Almoco de hoje fora."}'
open_chat
say "acabei%snao%salmocando"; kb_off
expect "delete proposal below the answer" 'resource-id="chat-skip-delete-card"'
[ "$(db "select kcal from meal_log")" = "[(640,)]" ] && echo "  ✓ nothing changed before the tap" || { echo "  ✗ meal_log changed before the tap"; FAIL=1; }
open_chat
tap 'resource-id="chat-skip-delete-confirm"' 1.5
expect "skip receipt after Excluir e pular" 'Pulado'
[ "$(db "select count(*) from meal_log")" = "[(0,)]" ] && echo "  ✓ record removed" || { echo "  ✗ meal_log after Excluir e pular"; FAIL=1; }
[ "$(db "select count(*) from slot_skip")" = "[(1,)]" ] && echo "  ✓ lunch skipped" || { echo "  ✗ slot_skip after Excluir e pular"; FAIL=1; }
open_chat
tap 'resource-id="chat-receipt-undo"' 1.5
expect "Desfazer brings the lunch back" 'Restaurado em'
[ "$(db "select kcal from meal_log")" = "[(640,)]" ] && echo "  ✓ record restored" || { echo "  ✗ meal_log after Desfazer"; FAIL=1; }
[ "$(db "select count(*) from slot_skip")" = "[(0,)]" ] && echo "  ✓ skip removed" || { echo "  ✗ slot_skip after Desfazer"; FAIL=1; }

echo "  A59: Manter registro"
sql "$CLEAN$A59_SEED$LUNCH"
open_chat
say "nao%salmocei"; kb_off
open_chat
tap 'resource-id="chat-skip-delete-keep"' 1.5
expect "card marked Registro mantido" 'text="Registro mantido"'
[ "$(db "select kcal from meal_log")" = "[(640,)]" ] && echo "  ✓ record kept" || { echo "  ✗ meal_log after Manter registro"; FAIL=1; }

echo "  A59: the proposal expires on the next send"
sql "$CLEAN$A59_SEED$LUNCH"
open_chat
say "nao%salmocei"; kb_off
fake_mode '{"record": "none", "reply": "Ok."}'
say "obrigado"; kb_off
open_chat
if has chat-skip-delete-card; then echo "  ✗ proposal alive after a send"; FAIL=1; else echo "  ✓ proposal gone after a send"; fi
expect "expired proposal: Não registrado" 'resource-id="chat-not-recorded"'
fake_mode '{}'

echo "  A59 gold: chatSK"
set_clock 0 07:42
sql "$CLEAN$A59_SEED"'
c.execute("insert into meal_slot(name,minutesFromMidnight,sortOrder,days) values(?,?,?,?)", ("Pré-treino", 360, -1, 127))
pre = c.execute("select id from meal_slot where name = ?", ("Pré-treino",)).fetchone()[0]
eggs = rec("2 ovos mexidos e 1 pão francês", 320, 17, 29, 16)
log(cafe, eggs)
c.execute("insert into slot_skip(date, slotId) values(?, ?)", (today, pre))
msg("user", "Pulei o pré-treino. No café comi 2 ovos mexidos e 1 pão francês.", 60000)
answer = msg("assistant", "Pré-treino de hoje fora. Identifiquei 2 ovos mexidos e 1 pão francês. A estimativa total é de:", 0,
    estimateKcal=320, estimateP=17, estimateC=29, estimateG=16, estimateConfidence="high", estimateSlotId=cafe,
    estimateItems="2 ovos mexidos" + chr(10) + "1 pão francês", intent="log", recordMode="auto", recordState="recorded",
    skipOutcomes=json.dumps({"date": today, "with": "log", "slots": [{"slotId": pre, "state": state(), "outcome": "skipped"}]}))
msg("logged", "Café da manhã", -1000, estimateKcal=320, estimateSlotId=cafe, recordSource="user",
    undoData=json.dumps({"slots": [{"date": today, "slotId": cafe, "before": state(), "after": state(eggs)}]}))
msg("skipped", "Pré-treino", -2000, estimateSlotId=pre,
    undoData=json.dumps({"slots": [{"date": today, "slotId": pre, "before": state(), "after": state(skipped=True)}]}))
'
open_chat
expect "gold chatSK: log receipt actions" 'chat-receipt-delete.*chat-receipt-move.*chat-receipt-edit'
expect "gold chatSK: Pulado Pré-treino with Desfazer" 'chat-receipt-undo'
shot chatSK

echo "  A59 gold: chatSD"
set_clock 0 14:05
sql "$CLEAN$A59_SEED$LUNCH"'
c.execute("delete from meal_slot where name = ?", ("Pré-treino",))
msg("user", "Acabei não almoçando hoje.", 60000)
msg("assistant", "Almoço de hoje fora.", 0, intent="skip", recordMode="auto",
    skipOutcomes=json.dumps({"date": today, "with": "skip", "slots": [{"slotId": almoco, "state": state(lunch), "outcome": "pending_delete"}]}))
'
open_chat
expect "gold chatSD: Pular Almoço?" 'text="Pular Almoço\?"'
expect "gold chatSD: copy" 'text="Almoço tem 640 kcal registrados. O registro sai e o Almoço fica pulado."'
expect "gold chatSD: Excluir e pular over Manter registro" 'chat-skip-delete-confirm.*chat-skip-delete-keep'
shot chatSD
sql "$CLEAN"
fi

if [ "${SCENES:-all}" = all ] || [ "${SCENES:-all}" = a54 ]; then
kb_off() { "$ADB" shell dumpsys input_method | grep -q 'mInputShown=true' && { "$ADB" shell input keyevent 4; sleep 0.8; }; }
dinner_sent() { curl -s "$FAKE/__calls" | "$PY" -c "import json,sys; d=json.load(sys.stdin)['day']; print(' '.join(f\"{s['status']}:{s.get('kcal')}\" for s in d if s['id'] == sys.argv[1]))" "$1"; }
set_clock 0 16:53
echo "  A54: an auto addition into the empty dinner is recorded"
sql "$CLEAN"
fake_mode '{"a54": true}'
open_chat
say "vamos%scom%s2%someletes"; kb_off
expect "a54: receipt in the Chat" 'resource-id="chat-receipt-delete"'
if has chat-not-recorded; then echo "  ✗ Não registrado on a recordable addition"; FAIL=1; else echo "  ✓ no Não registrado"; fi
rows=$(db "select m.name, l.kcal, l.p, l.carbs, l.fat from meal_log l join meal_slot m on m.id = l.slotId")
case "$rows" in "[('Jantar', 263, 20, 1, 24)]") echo "  ✓ recorded in the dinner: $rows";; *) echo "  ✗ meal_log: $rows"; FAIL=1;; esac
state=$(db "select recordMode, recordState from chat_message where role = 'assistant'")
[ "$state" = "[('auto', 'recorded')]" ] && echo "  ✓ answer stored auto / recorded" || { echo "  ✗ answer state: $state"; FAIL=1; }
jantar=$(db "select id from meal_slot where name like 'Jan%'" | tr -dc '0-9')
"$ADB" shell am force-stop $PKG; "$ADB" shell am start -W -n $PKG/$ACTIVITY >/dev/null; sleep 3
expect "a54: Home counts the dinner (263 kcal consumidas)" 'text="263"'
fake_mode '{"record": "none", "reply": "Ok."}'
open_chat
say "e%sagora"; kb_off
sent=$(dinner_sent "$jantar")
[ "$sent" = "eaten:263" ] && echo "  ✓ next request carries the dinner as eaten" || { echo "  ✗ next DAY dinner: $sent"; FAIL=1; }
fake_mode '{}'
fi
# ------------------------------------------------------------------ A60: budget choice, tone and closures, formatting, planned
# SCENES=a50 (chatRB), a55 (Config tone, cfgT, closures homeC/homeK), a57 (chatR, chatRK, chatE), a58 (chatRL, homeP); a60 = all four.
a60_on() { [ "${SCENES:-all}" = all ] || [ "${SCENES:-all}" = a60 ] || [ "${SCENES:-all}" = "$1" ]; }
if a60_on a50 || a60_on a55 || a60_on a57 || a60_on a58; then
kb_off() { "$ADB" shell dumpsys input_method | grep -q 'mInputShown=true' && { "$ADB" shell input keyevent 4; sleep 0.8; }; }
called() { curl -s "$FAKE/__calls" | "$PY" -c "import json,sys; d=json.load(sys.stdin); print(d.get(sys.argv[1]))" "$1"; }
# The gold day: 1.640 kcal eaten (86P 152C 46G), ceiling 2.200, targets 167/223/74; slots by name.
A60_DAY='
import json
slots = {n: i for i, n in c.execute("select id, name from meal_slot")}
order = [r[0] for r in c.execute("select id from meal_slot order by minutesFromMidnight")]
jantar = next(i for n, i in slots.items() if n.startswith("Jan"))
c.execute("update profile set ceilingMode=?, kcalSame=2200, eat=?, proteinTargetG=167, carbTargetG=223, fatTargetG=74, tone=?", ("same", "zero", "seco"))
c.execute("update day set workoutKcal=null")
for slot, kcal, p, carbs, fat in [(order[0], 440, 25, 38, 22), (order[1], 820, 45, 76, 14), (order[2], 380, 16, 38, 10)]:
    c.execute("insert into meal_log(date,window,text,kcal,p,stable,slotId,carbs,fat,source) values(?,?,?,?,?,1,?,?,?,?)", (today, "", "x", kcal, p, slot, carbs, fat, "user"))
now = int(time.time() * 1000)
def msg(role, text, at, **kw):
    cols = ["date", "role", "text", "createdAtEpochMs"] + list(kw)
    c.execute(f"insert into chat_message({chr(44).join(cols)}) values({chr(44).join(chr(63) * len(cols))})", [today, role, text, now - at] + list(kw.values()))
    return c.execute("select max(id) from chat_message").fetchone()[0]
NL = chr(10)
OPTIONS = NL.join(["Duas opções para o jantar:",
    "- **Pizza de pão sírio**: 1 pão sírio (60 g), 30 g de molho de tomate, 100 g de frango desfiado, 30 g de milho e 30 g de muçarela · **420 kcal**",
    "- **Omelete de forno**: 3 ovos, 50 g de ricota e 1 fatia de pão integral (25 g) · **360 kcal**",
    "Primeira opção: ~**420 kcal** · 40P · 38C · 12G"])
'
set_clock 0 20:15

if a60_on a50; then
echo "  A50: a plan over its window asks Pode passar | Ajustar para caber (chatRB)"
sql "$CLEAN$A60_DAY"
fake_mode '{"plan_budget": "over"}'
open_chat
say "receita%sde%smacarrao%scom%satum"; kb_off
[ "$(called planBudget)" = "True" ] && echo "  ✓ plan_budget true on the wire" || { echo "  ✗ plan_budget sent: $(called planBudget)"; FAIL=1; }
expect "lines below the plan" 'Passa 310 kcal do que sobra.*|Reservei 250 kcal para fatia de bolo'
expect "Pode passar and Ajustar para caber" 'chat-budget-over-ok'
if has chat-record-plan; then echo "  ✗ Registrar assim while choosing"; FAIL=1; else echo "  ✓ no Registrar assim while choosing"; fi
tap 'resource-id="chat-budget-over-ok"' 1.5
expect "Pode passar: Registrar assim returns" 'resource-id="chat-record-plan"'
if has chat-budget-fit; then echo "  ✗ pills after Pode passar"; FAIL=1; else echo "  ✓ pills gone"; fi
local_choice=$(db "select planBudget from chat_message where role = 'assistant' order by id desc limit 1")
case "$local_choice" in *'"local":"over_ok"'*) echo "  ✓ over_ok stored on the plan";; *) echo "  ✗ stored budget: $local_choice"; FAIL=1;; esac
open_chat
expect "recreation keeps Registrar assim" 'resource-id="chat-record-plan"'
echo "  A50: Ajustar para caber sends fit_kcal; still over shows the choice again; a fitting plan is chatR"
fake_mode '{"plan_budget": "still"}'
say "outra%sreceita"; kb_off
tap 'resource-id="chat-budget-fit"' 3; kb_off
[ "$(called fit)" = "310" ] && echo "  ✓ fit_kcal 310 on the wire" || { echo "  ✗ fit_kcal: $(called fit)"; FAIL=1; }
expect "still over: the choice again" 'chat-budget-fit'
fake_mode '{"plan_budget": "over"}'
tap 'resource-id="chat-budget-fit"' 3; kb_off
expect "adjusted plan fits: Registrar assim" 'resource-id="chat-record-plan"'
fake_mode '{"plan_budget": "zero"}'
say "mais%suma"; kb_off
if has chat-budget-fit; then echo "  ✗ pills with nothing to adjust to"; FAIL=1; else echo "  ✓ limit 0: no pills (chatR)"; fi
# Gold thread (D12).
sql "$CLEAN$A60_DAY"'
RECIPE = NL.join(["Macarrão com atum ao sugo:", "- 80 g de macarrão cru", "- 1 lata de atum em água (120 g)", "- 150 g de molho de tomate",
    "- 20 g de queijo ralado (opcional)", "1. Cozinhe o macarrão por 9 min.", "2. Aqueça o molho com o atum por 5 min.",
    "3. Misture e finalize com o queijo.", "Total: ~**620 kcal** · 42P · 70C · 18G"])
msg("user", "Me passa uma receita de macarrão com atum pro jantar? Mais tarde ainda como uma fatia de bolo.", 2000)
msg("assistant", RECIPE, 1000, estimateKcal=620, estimateP=42, estimateC=70, estimateG=18, estimateConfidence="medium", estimateSlotId=jantar,
    intent="plan", recordMode="none",
    planBudget=json.dumps({"limitKcal": 310, "overKcal": 310, "reserved": [{"label": "fatia de bolo", "kcal": 250}]}))
'
open_chat
expect "gold: Dia 1.640 -> 2.260 de 2.200" '1\.640.*2\.260.*2\.200'
shot chatRB
fake_mode '{}'
fi

if a60_on a57; then
echo "  A57: formatted replies render as blocks; records and history stay plain"
sql "$CLEAN$A60_DAY"
fake_mode '{"format": "plan"}'
open_chat
say "o%sque%sjanto"; kb_off
expect "plan: bullets" 'resource-id="chat-reply-bullet"'
dump; if grep -q '\*\*' "$TMP/ui.xml"; then echo "  ✗ markers on screen"; FAIL=1; else echo "  ✓ no ** on screen"; fi
fake_mode '{"format": "recipe"}'
say "receita%sde%sfrango"; kb_off
expect "recipe: table and steps" 'resource-id="chat-reply-table"'
expect "recipe: steps" 'resource-id="chat-reply-step"'
history=$(curl -s "$FAKE/__calls" | "$PY" -c "import json,sys; print(sum('**' in m['text'] for m in json.load(sys.stdin)['messages']))")
[ "$history" = 0 ] && echo "  ✓ the history of the next turn carries no markers" || { echo "  ✗ markers in the history: $history"; FAIL=1; }
sql "$CLEAN"
fake_mode '{"format": "log", "record": "auto"}'
open_chat
say "comi%s2%spaes%se%s2%sovos"; kb_off
expect "log: receipt" 'resource-id="chat-receipt-delete"'
rows=$(db "select text from meal_log where text like '%**%'")
[ "$rows" = "[]" ] && echo "  ✓ no markers in meal_log" || { echo "  ✗ markers recorded: $rows"; FAIL=1; }
fake_mode '{"format": "malformed"}'
open_chat
say "arroz%se%sfeijao"; kb_off
expect "malformed markup stays literal" '\*\*Arroz com feij'
# Gold threads (D17).
sql "$CLEAN$A60_DAY"'
msg("user", "Não sei o que jantar. Me dá umas ideias?", 2000)
msg("assistant", OPTIONS, 1000, estimateKcal=420, estimateP=40, estimateC=38, estimateG=12, estimateConfidence="high", estimateSlotId=jantar,
    estimateMealText="Pizza de pão sírio", intent="plan", recordMode="none")
'
open_chat
expect "gold chatR: Reservar para o Jantar" 'resource-id="chat-reserve"'
shot chatR
sql "$CLEAN$A60_DAY"'
RK = NL.join(["Frango com brócolis e arroz", "| Item | Gramas |", "| --- | --- |", "| Peito de frango | 120 g |", "| Arroz cozido | 120 g |",
    "| Brócolis | 100 g |", "| Azeite | 5 g |", "| Alho | 5 g |", "| Queijo ralado (opcional) | 15 g |",
    "1. Corte o frango em cubos e grelhe por 8 min.", "2. Refogue o alho no azeite e junte o brócolis por 3 min.",
    "3. Misture o arroz e o frango e finalize com o queijo.", "Total: ~**520 kcal** · 46P · 41C · 17G"])
msg("user", "Me passa uma receita de frango com brócolis pro jantar?", 2000)
msg("assistant", RK, 1000, estimateKcal=520, estimateP=46, estimateC=41, estimateG=17, estimateConfidence="high", intent="plan", recordMode="none")
'
open_chat
expect "gold chatRK: the table" 'resource-id="chat-reply-table"'
shot chatRK
sql "$CLEAN"'
first = c.execute("select id from meal_slot order by minutesFromMidnight").fetchone()[0]
now = int(time.time() * 1000)
c.execute("insert into chat_message(date,role,text,createdAtEpochMs) values(?,?,?,?)", (today, "user", "2 pães franceses com 2 ovos mexidos no café da manhã", now - 2000))
c.execute("insert into chat_message(date,role,text,createdAtEpochMs,estimateKcal,estimateP,estimateC,estimateG,estimateConfidence,estimateSlotId,estimateItems,intent,recordMode) values(?,?,?,?,?,?,?,?,?,?,?,?,?)",
          (today, "assistant", "Identifiquei 2 pães franceses (**100 g**) e 2 ovos mexidos (**100 g**). A estimativa total é de:", now - 1000,
           380, 22, 36, 16, "high", first, "2 pães franceses" + chr(10) + "2 ovos mexidos", "log", "ask"))
'
open_chat
expect "gold chatE: Registrar" 'resource-id="chat-register"'
shot chatE
fake_mode '{}'
fi

if a60_on a58; then
echo "  A58: reserve the plan, the planned day, a record against it, Desfazer"
sql "$CLEAN$A60_DAY"'
msg("user", "Não sei o que jantar. Me dá umas ideias?", 2000)
msg("assistant", OPTIONS, 1000, estimateKcal=420, estimateP=40, estimateC=38, estimateG=12, estimateConfidence="high", estimateSlotId=jantar,
    estimateMealText="Pizza de pão sírio", intent="plan", recordMode="none")
'
open_chat
tap 'resource-id="chat-reserve"' 1.5
expect "Reservado para o Jantar (chatRL)" 'resource-id="chat-reserved"'
if has chat-reserve; then echo "  ✗ Reservar still drawn"; FAIL=1; else echo "  ✓ Reservar gone, Registrar assim kept"; fi
shot chatRL
rows=$(db "select text, kcal from planned_meal")
case "$rows" in "[('Pizza de pão sírio', 420)]") echo "  ✓ planned_meal: $rows";; *) echo "  ✗ planned_meal: $rows"; FAIL=1;; esac
open_chat
fake_mode '{"record": "none", "reply": "Ok."}'
say "ok"; kb_off
jantar=$(db "select id from meal_slot where name like 'Jan%'" | tr -dc '0-9')
sent=$(dinner_sent "$jantar" 2>/dev/null || curl -s "$FAKE/__calls" | "$PY" -c "import json,sys; d=json.load(sys.stdin)['day']; print(' '.join(f\"{s['status']}:{s.get('kcal')}\" for s in d if s['id'] == sys.argv[1]))" "$jantar")
[ "$sent" = "planned:420" ] && echo "  ✓ DAY carries the dinner as planned" || { echo "  ✗ DAY dinner: $sent"; FAIL=1; }
fake_mode '{"planned": true}'
open_chat
say "jantei%sfrango%se%sarroz"; kb_off
expect "receipt: Plano 420 · Registrado 610 (+190 kcal)" 'Plano: 420 · Registrado: 610 \(\+190 kcal\)'
rows=$(db "select count(*) from planned_meal")
[ "$rows" = "[(0,)]" ] && echo "  ✓ the record replaced the reservation" || { echo "  ✗ planned_meal after record: $rows"; FAIL=1; }
open_chat
tap 'resource-id="chat-receipt-undo"' 2
rows=$(db "select text, kcal from planned_meal")
case "$rows" in "[('Pizza de pão sírio', 420)]") echo "  ✓ Desfazer restored the reservation";; *) echo "  ✗ after Desfazer: $rows"; FAIL=1;; esac
# homeP gold: home1 with the dinner reserved.
sql "$CLEAN"'
slots = {n: i for i, n in c.execute("select id, name from meal_slot")}
order = [r[0] for r in c.execute("select id from meal_slot order by minutesFromMidnight")]
c.execute("update profile set kcalSame=2000, eat=?, pct=50, proteinTargetG=150, carbTargetG=200, fatTargetG=67", ("partial",))
c.execute("insert or replace into day(date, workoutKcal, removedWindows, askedWindows) values(?,?,?,?)", (today, 350, "[]", "[]"))
c.execute("insert into meal_log(date,window,text,kcal,p,stable,slotId,carbs,fat,source) values(?,?,?,?,?,1,?,?,?,?)", (today, "", "2 pães franceses, 2 ovos mexidos e café com leite", 520, 28, order[0], 52, 22, "user"))
c.execute("insert into meal_log(date,window,text,kcal,p,stable,slotId,carbs,fat,source) values(?,?,?,?,?,1,?,?,?,?)", (today, "", "Prato feito: frango grelhado, arroz, feijão e salada", 780, 48, order[1], 82, 18, "photo"))
c.execute("insert into slot_skip(date, slotId) values(?,?)", (today, order[2]))
c.execute("delete from planned_meal")
c.execute("insert into planned_meal(date,slotId,text,kcal,p,c,g) values(?,?,?,?,?,?,?)", (today, order[3], "Omelete de forno: 3 ovos, 50 g de ricota e 1 fatia de pão integral", 360, 30, 20, 18))
'
"$ADB" shell am start -W -n $PKG/$ACTIVITY >/dev/null; sleep 3
"$ADB" shell input swipe 390 1400 390 300 300; sleep 1
expect "homeP: planejado · 360 kcal" 'planejado · 360 kcal'
"$ADB" shell input swipe 390 300 390 1400 300; sleep 1
shot homeP
fake_mode '{}'
fi

if a60_on a55; then
echo "  A55: the tone in Config (cfg, cfgT), on the wire, and the closures (homeC, homeK)"
sql "$CLEAN"'
c.execute("update profile set kcalSame=2000, eat=?, pct=0, proteinTargetG=150, carbTargetG=200, fatTargetG=67, tone=?", ("zero", "seco"))
c.execute("delete from closure")
'
"$ADB" shell am start -W -n $PKG/$ACTIVITY >/dev/null; sleep 3
tap 'resource-id="home-config"' 1.5
expect "cfg: Tom da Tali · Seco" 'Tom da Tali'
shot cfg
tap 'resource-id="cfg-tone"' 1.2
expect "cfgT: the two options" 'resource-id="cfg-tone-duro"'
shot cfgT
tap 'resource-id="cfg-tone-duro"' 0.6
tap 'resource-id="cfg-save"' 1.2
expect "row reads Duro" 'text="Duro"'
rows=$(db "select tone from profile")
[ "$rows" = "[('duro',)]" ] && echo "  ✓ tone stored" || { echo "  ✗ tone: $rows"; FAIL=1; }
fake_mode '{"record": "none", "reply": "Ok."}'
open_chat
say "oi"; kb_off
[ "$(called tone)" = "duro" ] && echo "  ✓ profile.tone duro on the wire" || { echo "  ✗ tone sent: $(called tone)"; FAIL=1; }
# Closures on Sunday 22:05: the clock jump makes the 22:00 alarm due, so the app closes the day and the week at its
# next start (the alarm path); the dev-only broadcast runs one more time and changes nothing (produced once).
days=$("$PY" -c "import datetime, zoneinfo; d = datetime.datetime.now(zoneinfo.ZoneInfo('America/Sao_Paulo')).date(); print((6 - d.weekday()) % 7)")
set_clock "$days" 22:05
closure() { "$ADB" shell am broadcast -a app.fibrai.android.dev.RUN_CLOSURE --es period "$1" -n $PKG/app.fibrai.android.core.closure.RunClosureReceiver >/dev/null; sleep 3; }
start_app() { "$ADB" shell am force-stop $PKG; "$ADB" shell am start -W -n $PKG/$ACTIVITY >/dev/null; sleep "${1:-5}"; }
notified() { "$ADB" shell dumpsys notification --noredact | grep -q "$1"; }
# The seeded day is the emulator's (Sunday), not the host's.
HOME_C="
import datetime
today = (datetime.date.fromisoformat(today) + datetime.timedelta(days=$days)).isoformat()
"'
order = [r[0] for r in c.execute("select id from meal_slot order by minutesFromMidnight")]
c.execute("update profile set kcalSame=2000, eat=?, pct=50, proteinTargetG=150, carbTargetG=200, fatTargetG=67, tone=?", ("partial", "seco"))
for t in ("closure", "meal_log", "slot_skip", "planned_meal"): c.execute(f"delete from {t}")
c.execute("insert or replace into day(date, workoutKcal, removedWindows, askedWindows) values(?,?,?,?)", (today, 350, "[]", "[]"))
c.execute("insert into meal_log(date,window,text,kcal,p,stable,slotId,carbs,fat,source) values(?,?,?,?,?,1,?,?,?,?)", (today, "", "2 pães franceses, 2 ovos mexidos e café com leite", 520, 28, order[0], 52, 22, "user"))
c.execute("insert into meal_log(date,window,text,kcal,p,stable,slotId,carbs,fat,source) values(?,?,?,?,?,1,?,?,?,?)", (today, "", "Prato feito: frango grelhado, arroz, feijão e salada", 780, 48, order[1], 82, 18, "photo"))
c.execute("insert into slot_skip(date, slotId) values(?,?)", (today, order[2]))
'
sql "$CLEAN$HOME_C"
fake_mode '{}'
start_app 8
notified "Fechamento do dia" && echo "  ✓ notification Fechamento do dia" || { echo "  ✗ no day notification"; FAIL=1; }
notified "Fechamento da semana" && echo "  ✓ notification Fechamento da semana" || { echo "  ✗ no week notification"; FAIL=1; }
rows=$(db "select period, status from closure order by period")
[ "$rows" = "[('day', 'text'), ('week', 'text')]" ] && echo "  ✓ the alarm closed the day and the week, with their texts" || { echo "  ✗ closure rows: $rows"; FAIL=1; }
sent=$(called close | "$PY" -c "import ast,sys; d=ast.literal_eval(sys.stdin.read()); print(d['period'], d['tone'], sorted(d['numbers']))")
echo "  last /v1/close: $sent"
start_app
closure day
rows=$(db "select count(*) from closure")
[ "$rows" = "[(2,)]" ] && echo "  ✓ the broadcast produced nothing new (once per day and week)" || { echo "  ✗ closures: $rows"; FAIL=1; }
# homeC: the day card alone (the week row out of the way), then homeK.
sql 'c.execute("delete from closure where period = ?", ("week",))'
start_app
"$ADB" shell input swipe 390 1300 390 700 300; sleep 1
expect "homeC: the day card" 'resource-id="home-closure-day"'
"$ADB" shell input swipe 390 700 390 1300 300; sleep 1
shot homeC
closure week
start_app
"$ADB" shell input swipe 390 1300 390 700 300; sleep 1
expect "homeK: the week card above the day card" 'home-closure-week.*home-closure-day'
"$ADB" shell input swipe 390 700 390 1300 300; sleep 1
shot homeK
echo "  A55: without network, the numbers alone; one retry at the next start"
sql "$HOME_C"
fake_mode '{"close_fail": true}'
start_app 8
"$ADB" shell input swipe 390 1300 390 700 300; sleep 1
expect "offline card: Sem o texto: sem rede." 'Sem o texto: sem rede.'
rows=$(db "select status, retried from closure where period = 'day'")
[ "$rows" = "[('offline', 0)]" ] && echo "  ✓ offline stored" || { echo "  ✗ offline: $rows"; FAIL=1; }
fake_mode '{}'
start_app 8
rows=$(db "select status, retried from closure where period = 'day'")
[ "$rows" = "[('text', 1)]" ] && echo "  ✓ the retry at start fetched the text" || { echo "  ✗ after retry: $rows"; FAIL=1; }
echo "  A55: the next day the card collapses after the first record"
set_clock "$((days + 1))" 08:30
sql "
import datetime
today = (datetime.date.fromisoformat(today) + datetime.timedelta(days=$((days + 1)))).isoformat()
"'
first = c.execute("select id from meal_slot order by minutesFromMidnight").fetchone()[0]
c.execute("insert into meal_log(date,window,text,kcal,p,stable,slotId,carbs,fat,source) values(?,?,?,?,?,1,?,?,?,?)", (today, "", "café", 300, 15, first, 30, 10, "user"))
'
start_app
"$ADB" shell input swipe 390 1300 390 700 300; sleep 1
expect "collapsed: Ontem: 1300 de 2175 kcal" 'Ontem: 1300 de 2175 kcal'
"$ADB" shell am force-stop $PKG
fi
fi

# ------------------------------------------------------------------ A61: actions in the thread, copying messages (ADR-048)
if [ "${SCENES:-all}" = all ] || [ "${SCENES:-all}" = a61 ]; then
kb_off() { "$ADB" shell dumpsys input_method | grep -q 'mInputShown=true' && { "$ADB" shell input keyevent 4; sleep 0.8; }; }
box() { # box <tag> -> "x1 y1 x2 y2" of the first node with that resource-id, empty when not drawn
  dump; "$PY" - "$1" "$TMP/ui.xml" <<'EOF'
import re, sys
m = re.search(r'resource-id="' + re.escape(sys.argv[1]) + r'"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', open(sys.argv[2], encoding="utf-8").read())
print(" ".join(m.groups()) if m else "")
EOF
}
hold() { # hold <tag>: long press on its centre
  local b; b=$(box "$1"); [ -z "$b" ] && { echo "  ✗ not found: $1"; FAIL=1; return 1; }
  set -- $b; "$ADB" shell input swipe $(( ($1 + $3) / 2 )) $(( ($2 + $4) / 2 )) $(( ($1 + $3) / 2 )) $(( ($2 + $4) / 2 )) 900; sleep 1
}
count_is() { # count_is <n>: the selection bar shows n
  dump; "$PY" - "$1" "$TMP/ui.xml" <<'EOF'
import re, sys
m = re.search(r'<node[^>]*text="([^"]*)"[^>]*resource-id="chat-selection-count"', open(sys.argv[2], encoding="utf-8").read())
sys.exit(0 if m and m.group(1) == sys.argv[1] else 1)
EOF
}
A61_SEED='
import json
slots = {n: i for i, n in c.execute("select id, name from meal_slot")}
cafe = next(i for n, i in slots.items() if n.startswith("Caf"))
jantar = next(i for n, i in slots.items() if n.startswith("Jan"))
now = int(time.time() * 1000)
def msg(role, text, at, **kw):
    cols = ["date", "role", "text", "createdAtEpochMs"] + list(kw)
    c.execute(f"insert into chat_message({chr(44).join(cols)}) values({chr(44).join(chr(63) * len(cols))})", [today, role, text, now - at] + list(kw.values()))
    return c.execute("select max(id) from chat_message").fetchone()[0]
NL = chr(10)
'
set_clock 0 20:15

echo "  A61 part A: a plan's actions sit under it and scroll with the thread; the composer stays"
sql "$CLEAN$A61_SEED"'
for i in range(4):
    msg("user", f"Pergunta {i + 1} sobre o dia", 9000 - i * 2000)
    msg("assistant", "Resposta curta. " * 12, 8000 - i * 2000)
msg("user", "Não sei o que jantar. Me dá umas ideias?", 1500)
msg("assistant", NL.join(["Duas opções para o jantar:", "- **Pizza de pão sírio** · **420 kcal**", "- **Omelete de forno** · **360 kcal**",
    "Primeira opção: ~**420 kcal** · 40P · 38C · 12G"]), 1000, estimateKcal=420, estimateP=40, estimateC=38, estimateG=12,
    estimateConfidence="high", estimateSlotId=jantar, estimateMealText="Pizza de pão sírio", intent="plan", recordMode="none")
'
open_chat
plan0=$(box chat-record-plan); input0=$(box chat-input)
[ -n "$plan0" ] && echo "  ✓ Registrar assim drawn: $plan0" || { echo "  ✗ no Registrar assim"; FAIL=1; }
set -- $plan0 $input0
[ "${4:-0}" -lt "${6:-0}" ] && echo "  ✓ the stack ends above the composer" || { echo "  ✗ stack over the composer ($plan0 / $input0)"; FAIL=1; }
"$ADB" shell input swipe 390 500 390 900 400; sleep 1.2
plan1=$(box chat-record-plan); input1=$(box chat-input)
p0=$(echo "$plan0" | cut -d' ' -f2); p1=$(echo "$plan1" | cut -d' ' -f2)
if [ -z "$plan1" ] || [ "${p1:-0}" -gt "${p0:-0}" ]; then echo "  ✓ the stack moved with the thread (${p0} -> ${p1:-off screen})"; else echo "  ✗ the stack did not move ($plan0 -> $plan1)"; FAIL=1; fi
[ "$input1" = "$input0" ] && echo "  ✓ the composer stayed ($input0)" || { echo "  ✗ the composer moved ($input0 -> $input1)"; FAIL=1; }
"$ADB" shell input swipe 390 900 390 300 300; sleep 1
tap 'resource-id="chat-record-plan"' 1.5
expect "Registrar assim still records from the thread" 'resource-id="chat-receipt-delete"|resource-id="chat-sheet"'

echo "  A61 part B: long press selects, taps toggle, a receipt is never selected, Copiar"
COPY_SEED='
eggs = "2 pães franceses com 2 ovos mexidos no café da manhã"
c.execute("insert into meal_log(date,window,text,kcal,p,stable,slotId,carbs,fat,source) values(?,?,?,?,?,1,?,?,?,?)", (today, "", eggs, 380, 22, cafe, 36, 16, "user"))
u = msg("user", eggs, 3000)
a = msg("assistant", "Identifiquei 2 pães franceses e 2 ovos mexidos. A estimativa total é de:", 2000, estimateKcal=380, estimateP=22, estimateC=36,
    estimateG=16, estimateConfidence="high", estimateSlotId=cafe, estimateItems="2 pães franceses" + NL + "2 ovos mexidos", intent="log",
    recordMode="auto", recordState="recorded")
r = {"text": eggs, "kcal": 380, "p": 22, "c": 36, "g": 16, "source": "user", "window": "", "stable": True}
msg("logged", "Café da manhã", 1000, estimateKcal=380, estimateSlotId=cafe, recordSource="user",
    undoData=json.dumps({"slots": [{"date": today, "slotId": cafe, "before": {"records": [], "skipped": False}, "after": {"records": [r], "skipped": False}}]}))
'
sql "$CLEAN$A61_SEED$COPY_SEED"
open_chat
ids=$(db "select id, role from chat_message order by id")
uid=$(echo "$ids" | "$PY" -c "import ast,sys; print([i for i, r in ast.literal_eval(sys.stdin.read()) if r == 'user'][0])")
aid=$(echo "$ids" | "$PY" -c "import ast,sys; print([i for i, r in ast.literal_eval(sys.stdin.read()) if r == 'assistant'][0])")
rid=$(echo "$ids" | "$PY" -c "import ast,sys; print([i for i, r in ast.literal_eval(sys.stdin.read()) if r == 'logged'][0])")
open_chat
hold "chat-row-u-$uid"
count_is 1 && echo "  ✓ long press on the user bubble: selection bar with 1" || { echo "  ✗ selection bar after the long press"; FAIL=1; }
tap "resource-id=\"chat-row-a-$aid\"" 1
count_is 2 && echo "  ✓ a tap on the Tali reply: 2" || { echo "  ✗ count after the tap on the reply"; FAIL=1; }
tap "resource-id=\"chat-receipt-$rid\"" 1
count_is 2 && echo "  ✓ a tap on the receipt changes nothing" || { echo "  ✗ the receipt changed the selection"; FAIL=1; }
shot chatCP
tap 'resource-id="chat-copy"' 1.5
if has chat-selection; then echo "  ✗ the selection is still open after Copiar"; FAIL=1; else echo "  ✓ Copiar ends the selection"; fi
# Colar in the composer: the copied text, plain. PASTE=1 only: the paste key comes from a keyboard source (the field's
# text toolbar cannot be read by uiautomator, a dpad paste does not reach the field), and Gboard then stays in its
# physical-keyboard mode until the AVD restarts without saving its snapshot, which breaks the keyboard checks of the
# other scripts (input-checks.sh).
if [ "${PASTE:-0}" = 1 ]; then
tap 'resource-id="chat-input"' 1.5
"$ADB" shell input keyevent KEYCODE_PASTE; sleep 1.5
dump
"$PY" - "$TMP/ui.xml" <<'EOF' && echo "  ✓ pasted: both messages in order, a blank line apart, no markers" || { echo "  ✗ pasted text"; FAIL=1; }
import html, re, sys
xml = open(sys.argv[1], encoding="utf-8").read()
m = re.search(r'<node[^>]*text="([^"]*)"[^>]*resource-id="chat-input"', xml)
text = html.unescape(m.group(1)) if m else ""
want = "2 pães franceses com 2 ovos mexidos no café da manhã\n\nIdentifiquei 2 pães franceses e 2 ovos mexidos. A estimativa total é de:"
print("   ", repr(text[:160]))
sys.exit(0 if text == want and "**" not in text else 1)
EOF
else echo "  · Colar check skipped (PASTE=1 runs it)"; fi
kb_off
echo "  A61 part B: the copy confirmation (the app's own on Android 12 and earlier, the system's on 13+)"
open_chat
hold "chat-row-u-$uid"
tap 'resource-id="chat-copy"' 0.3
if [ "$("$ADB" shell getprop ro.build.version.sdk | tr -d '
')" -le 32 ]; then
  expect "Mensagem copiada above the composer" 'text="Mensagem copiada"'
  shot chatCC 0.2
  sleep 2.5
  if has chat-copied; then echo "  ✗ the confirmation stayed"; FAIL=1; else echo "  ✓ the confirmation goes away by itself"; fi
else
  if has chat-copied; then echo "  ✗ the app's confirmation on Android 13+"; FAIL=1; else echo "  ✓ Android 13+: no confirmation of the app's own (the system overlay shows it)"; fi
fi
echo "  A61 part B: back and ✕ end the selection"
open_chat
hold "chat-row-u-$uid"
has chat-selection && "$ADB" shell input keyevent 4 && sleep 1
if has chat-selection; then echo "  ✗ back kept the selection"; FAIL=1; elif has chat; then echo "  ✓ back ends the selection and stays in the Chat"; else echo "  ✗ back left the Chat"; FAIL=1; fi
hold "chat-row-a-$aid"
tap 'resource-id="chat-selection-close"' 1
if has chat-selection; then echo "  ✗ ✕ kept the selection"; FAIL=1; else echo "  ✓ ✕ ends the selection"; fi
"$ADB" shell am force-stop $PKG
fi

if [ "${SCENES:-all}" = all ] || [ "${SCENES:-all}" = a64 ]; then
kb_off() { "$ADB" shell dumpsys input_method | grep -q 'mInputShown=true' && { "$ADB" shell input keyevent 4; sleep 0.8; }; }
recent_sent() { curl -s "$FAKE/__calls" | "$PY" -c "import json,sys; d=json.load(sys.stdin)['recentDays'] or []; print(len(d), d[0]['recorded'] if d else None, d[0]['kcal'] if d else None)"; }
set_clock 0 20:15
echo "  A64: recent_days on the wire (yesterday's dinner), the day balance on the receipt"
sql "$CLEAN"'
import datetime
yesterday = (datetime.date.fromisoformat(today) - datetime.timedelta(days=1)).isoformat()
c.execute("update profile set firstDay = ?", ((datetime.date.fromisoformat(today) - datetime.timedelta(days=3)).isoformat(),))
jantar = c.execute("select id from meal_slot where name like ?", ("Jan%",)).fetchone()[0]
c.execute("insert into meal_log(date,window,text,kcal,p,stable,slotId,carbs,fat,source) values(?,?,?,?,?,1,?,?,?,?)",
          (yesterday, "", "frango e arroz", 900, 50, jantar, 80, 20, "user"))
'
fake_mode '{"record": "auto"}'
open_chat
say "comi%s2%spaes%se%s2%sovos"; kb_off
got=$(recent_sent)
[ "$got" = "3 True 900" ] && echo "  ✓ recent_days: 3 days, yesterday recorded with 900 kcal" || { echo "  ✗ recent_days sent: $got"; FAIL=1; }
expect "balance line on the receipt" 'resource-id="chat-receipt-balance"'
expect "balance: 380 de ... kcal" 'text="380 de [0-9.]+ kcal · faltam [0-9]+ g de proteína"'

echo "  A64: an ask estimate projects the day; Registrar moves it to the receipt"
fake_mode '{"record": "ask", "slot": "Jan"}'
say "frango%scom%sarroz"; kb_off
expect "projection under the estimate" 'text="Projeção: 760 de [0-9.]+ kcal'
tap 'resource-id="chat-register"' 1.5
expect "after Registrar: the balance on the new receipt" 'text="760 de [0-9.]+ kcal'
if has chat-projection; then echo "  ✗ projection left after Registrar"; FAIL=1; else echo "  ✓ no projection after Registrar"; fi

echo "  A64: an explicit permanent fact is noted as stored"
sql "$CLEAN"
fake_mode '{"routine": true}'
open_chat
say "cafe%sde%ssempre"; kb_off
expect "Anotado line" 'text="Anotado: Usa leite semidesnatado"'

echo "  A64: after 4 s without an answer, Tali está pensando…"
sql "$CLEAN"
mode true
open_chat
tap 'resource-id="chat-suggestion-0"'
tap 'resource-id="chat-send"' 0.5
expect "loading copy first" 'resource-id="chat-loading-text"'
sleep 4.5
expect "waiting copy after 4 s" 'resource-id="chat-thinking"'
mode false
"$ADB" shell am force-stop $PKG

echo "  A64 gold: chatL"
sql "$CLEAN"
mode true
open_chat
tap 'resource-id="chat-suggestion-0"'
# The shot comes before any dump: a dump takes seconds and the copy changes at 4 s.
tap 'resource-id="chat-send"' 1
shot chatL 0.1
expect "loading bubble, still the first copy" 'resource-id="chat-loading-text"'
mode false
"$ADB" shell am force-stop $PKG

echo "  A64 gold: chatF (seeded as capture-photo.sh does, the receipt with its balance line)"
"$PY" - "$TMP/prato.jpg" "$ROOT" <<'PYEOF'
import sys
from PIL import Image
root = sys.argv[2]
Image.open(root + "/docs/qa/_legacy/stitch/dark/chatF.png").convert("RGB").crop((238, 392, 722, 662)).resize((2000, 1116), Image.LANCZOS).save(sys.argv[1], "JPEG", quality=85)
PYEOF
"$ADB" push "$TMP/prato.jpg" /data/local/tmp/chatf.jpg >/dev/null
"$ADB" shell chmod 644 /data/local/tmp/chatf.jpg
"$ADB" shell run-as $PKG sh -c "'mkdir -p files/photos; cp /data/local/tmp/chatf.jpg files/photos/chatf.jpg'"
set_clock 0 12:41
sql "$CLEAN"'
import json
slots = [r[0] for r in c.execute("select id from meal_slot order by minutesFromMidnight")]
almoco = slots[1]
c.execute("update meal_slot set name=?, minutesFromMidnight=750 where id=?", ("Almoço", almoco))
now = int(time.time() * 1000)
c.execute("insert into chat_message(date,role,text,createdAtEpochMs,photoPath) values(?,?,?,?,?)",
          (today, "user", "Almoço de hoje", now - 2000, "/data/user/0/app.fibrai.android.dev/files/photos/chatf.jpg"))
text = "Identifiquei um Prato Feito com filé de frango grelhado, arroz, feijão e salada verde."
c.execute("insert into chat_message(date,role,text,createdAtEpochMs,estimateKcal,estimateP,estimateC,estimateG,"
          "estimateConfidence,estimateSlotId,estimateItems,intent,recordMode,recordState) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
          (today, "assistant", text, now - 1000, 680, 48, 82, 18, "high", almoco, "Prato Feito", "log", "auto", "recorded"))
rec = {"text": text, "kcal": 680, "p": 48, "c": 82, "g": 18, "source": "photo", "window": "", "stable": True}
c.execute("insert into meal_log(date,window,text,kcal,p,stable,slotId,carbs,fat,source) values(?,?,?,?,?,1,?,?,?,?)",
          (today, "", text, 680, 48, almoco, 82, 18, "photo"))
undo = {"slots": [{"date": today, "slotId": almoco, "before": {"records": [], "skipped": False}, "after": {"records": [rec], "skipped": False}}]}
c.execute("insert into chat_message(date,role,text,createdAtEpochMs,estimateKcal,estimateSlotId,undoData,recordSource) values(?,?,?,?,?,?,?,?)",
          (today, "logged", "Almoço", now - 500, 680, almoco, json.dumps(undo), "photo"))
'
open_chat
expect "chatF: receipt with Excluir and Trocar refeição" 'chat-receipt-delete.*chat-receipt-move'
expect "chatF: the balance line" 'text="680 de [0-9.]+ kcal · faltam [0-9]+ g de proteína"'
shot chatF
sql "$CLEAN"
fi

if [ "${SCENES:-all}" = all ] || [ "${SCENES:-all}" = a65 ]; then
kb_off() { "$ADB" shell dumpsys input_method | grep -q 'mInputShown=true' && { "$ADB" shell input keyevent 4; sleep 0.8; }; }
workout_sent() { curl -s "$FAKE/__calls" | "$PY" -c "import json,sys; print(json.load(sys.stdin)['workout'])"; }
day_workout() { db "select workoutKcal from day where date = (select max(date) from day)"; }
set_clock 0 19:30
echo "  A65: \"treino de hoje 450 kcal\" writes the day's number with a receipt"
sql "$CLEAN"'
c.execute("update day set workoutKcal = null")
'
fake_mode '{"workout": {"kcal": 450, "mode": "replace"}}'
open_chat
say "treino%sde%shoje%s450%skcal"; kb_off
[ "$(workout_sent)" = "True" ] && echo "  ✓ workout true on the wire" || { echo "  ✗ workout sent: $(workout_sent)"; FAIL=1; }
expect "workout receipt" 'text="Treino registrado"'
expect "receipt chip 450 kcal" 'text="450 kcal"'
expect "Desfazer on the receipt" 'resource-id="chat-receipt-undo"'
"$ADB" exec-out screencap -p > "$TMP/a65-receipt.png"; cp "$TMP/a65-receipt.png" "${A65_EVIDENCE:-$TMP}/a65-receipt-$THEME.png" 2>/dev/null
[ "$(day_workout)" = "[(450,)]" ] && echo "  ✓ day.workoutKcal 450" || { echo "  ✗ day workout: $(day_workout)"; FAIL=1; }
"$ADB" shell am start -W -n $PKG/$ACTIVITY >/dev/null; sleep 3
expect "Home: the workout line shows the Chat value" 'resource-id="home-workout-value"[^>]*|text="450 kcal'
dump; grep -o 'text="450 kcal[^"]*"' "$TMP/ui.xml" | head -1 | sed 's/^/  · Home: /'
echo "  A65: Desfazer restores the previous number"
open_chat
tap 'resource-id="chat-receipt-undo"' 1.5
expect "receipt marked Desfeito" 'text="Desfeito"'
[ "$(day_workout)" = "[(None,)]" ] && echo "  ✓ day.workoutKcal back to none" || { echo "  ✗ day workout after Desfazer: $(day_workout)"; FAIL=1; }
echo "  A65: replace 300 then add 200 -> 500"
fake_mode '{"workout": {"kcal": 300, "mode": "replace"}}'
open_chat
say "treinei%s300%skcal"; kb_off
fake_mode '{"workout": {"kcal": 200, "mode": "add"}}'
say "mais%s200%skcal%sde%streino"; kb_off
expect "Treino somado receipt" 'text="Treino somado"'
expect "chip +200 kcal" 'text="\+200 kcal"'
[ "$(day_workout)" = "[(500,)]" ] && echo "  ✓ day.workoutKcal 500" || { echo "  ✗ day workout after add: $(day_workout)"; FAIL=1; }
fake_mode '{}'
sql "$CLEAN"'
c.execute("update day set workoutKcal = null")
'
fi

if [ "${SCENES:-all}" = all ] || [ "${SCENES:-all}" = a66 ]; then
kb_off() { "$ADB" shell dumpsys input_method | grep -q 'mInputShown=true' && { "$ADB" shell input keyevent 4; sleep 0.8; }; }
actions_sent() { curl -s "$FAKE/__calls" | "$PY" -c "import json,sys; print(json.load(sys.stdin)['actions'])"; }
set_clock 0 20:15
echo "  A66: the whole day in one message -> two records and a skip, three receipts, Desfazer of the batch"
sql "$CLEAN"
fake_mode '{"actions": "day"}'
open_chat
say "cafe%s2%spaes%se%s2%sovos,%salmoco%sarroz%sfeijao%sfrango,%spulei%so%slanche"; kb_off
[ "$(actions_sent)" = "True" ] && echo "  ✓ actions true on the wire" || { echo "  ✗ actions sent: $(actions_sent)"; FAIL=1; }
rows=$(db "select m.name, l.kcal from meal_log l join meal_slot m on m.id = l.slotId order by m.minutesFromMidnight")
case "$rows" in "[('Café da manhã', 380), ('Almoço', 640)]") echo "  ✓ two records: $rows";; *) echo "  ✗ meal_log: $rows"; FAIL=1;; esac
[ "$(db "select count(*) from slot_skip")" = "[(1,)]" ] && echo "  ✓ lanche skipped" || { echo "  ✗ slot_skip"; FAIL=1; }
[ "$(db "select count(*) from chat_message where role in ('logged','skipped')")" = "[(3,)]" ] && echo "  ✓ three receipts" || { echo "  ✗ receipts"; FAIL=1; }
[ "$(db "select count(*) from chat_message where role = 'assistant' and actions is not null")" = "[(1,)]" ] && echo "  ✓ actions stored on the first row" || { echo "  ✗ actions column"; FAIL=1; }
open_chat
"$ADB" exec-out screencap -p > "${A66_EVIDENCE:-$TMP}/a66-day-$THEME.png"
tap 'resource-id="chat-receipt-undo"' 1.5
[ "$(db "select count(*) from meal_log")" = "[(0,)]" ] && echo "  ✓ Desfazer reverted both records" || { echo "  ✗ meal_log after Desfazer"; FAIL=1; }
[ "$(db "select count(*) from slot_skip")" = "[(0,)]" ] && echo "  ✓ Desfazer reverted the skip" || { echo "  ✗ slot_skip after Desfazer"; FAIL=1; }
echo "  A66: jantei X, me sugere o lanche -> the dinner recorded, the plan with Registrar assim"
sql "$CLEAN"
fake_mode '{"actions": "plan"}'
open_chat
say "jantei%sfrango%se%sarroz,%sme%ssugere%so%slanche"; kb_off
[ "$(db "select kcal from meal_log")" = "[(610,)]" ] && echo "  ✓ dinner recorded" || { echo "  ✗ meal_log: $(db "select kcal from meal_log")"; FAIL=1; }
open_chat
expect "the plan keeps Registrar assim" 'resource-id="chat-record-plan"'
"$ADB" exec-out screencap -p > "${A66_EVIDENCE:-$TMP}/a66-plan-$THEME.png"
echo "  A66: a clarification on one of two -> one recorded, the question shows"
sql "$CLEAN"
fake_mode '{"actions": "held"}'
open_chat
say "cafe%s2%spaes%se%s2%sovos%se%salmoco%sarroz%se%sfrango"; kb_off
[ "$(db "select kcal from meal_log")" = "[(380,)]" ] && echo "  ✓ breakfast recorded alone" || { echo "  ✗ meal_log: $(db "select kcal from meal_log")"; FAIL=1; }
open_chat
expect "the held question shows" 'text="Quanto de arroz no almoço\?"'
"$ADB" exec-out screencap -p > "${A66_EVIDENCE:-$TMP}/a66-held-$THEME.png"
fake_mode '{}'
sql "$CLEAN"
fi

"$ADB" shell settings put global auto_time 1

rm -rf "$TMP"
exit $FAIL
