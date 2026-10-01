#!/usr/bin/env bash
# Chat QA on a running emulator against tools/fake-chat-server.mjs (no OpenAI):
# onboarding -> chat0 -> chatL -> (dark: timeout + retry) -> chatE -> chatT -> chatP -> Gravar -> chatG
# -> Home ring -> compact -> chatX (A25: 2100 characters block send and photo; back to 2000 sends)
# -> A29: routine over 3 past days -> chatS (Quase igual, Registrar) -> plan + Registrar assim -> chatR -> chatM.
# -> A30: questions before the estimate, Forçar estimativa, 3-round cap -> chatQ.
# Captures land in docs/qa/android/current/<theme>/. SCENES=v2 runs only onboarding + the A29 scenes;
# SCENES=a30 only onboarding + the A30 scenes; SCENES=a32 only onboarding + the A32 long-history scenes
# (opens at the bottom, pages of 20 up to 60 days, no jump on a reply while scrolled up, camera closes the keyboard).
#
# Prereqs: node tools/fake-chat-server.mjs running on the host (port 8765);
#   devDebug APK built with -PAPI_PUBLIC_URL=http://10.0.2.2:8765 and installed;
#   AVD at gold geometry (wm size 780x1688, wm density 320); python3; curl.
# Usage: [SCENES=v2|a30|a32] tools/capture-chat.sh dark|light
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
PKG=com.nutri.android.dev
# The Kotlin package did not change with the dev flavor (A10): name the activity in full.
ACTIVITY=com.nutri.android.MainActivity
mkdir -p "$OUT"
FAIL=0

bash "$ROOT/tools/capture-onboarding.sh" "$THEME" | tail -1
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
  rm -f "$TMP"/nutri.db*
  for f in nutri.db nutri.db-wal nutri.db-shm; do "$ADB" exec-out run-as $PKG cat databases/$f > "$TMP/$f" 2>/dev/null; done
  "$PY" -c "import sqlite3,sys; c=sqlite3.connect(sys.argv[1]); print(c.execute(sys.argv[2]).fetchall())" "$TMP/nutri.db" "$1"
}

# Gold names the first slot "Café da manhã"; adb cannot type accents.
"$ADB" shell am force-stop $PKG
rm -f "$TMP"/nutri.db*
for f in nutri.db nutri.db-wal nutri.db-shm; do "$ADB" exec-out run-as $PKG cat databases/$f > "$TMP/$f" 2>/dev/null; done
"$PY" - "$TMP/nutri.db" <<'EOF'
import sqlite3, sys
c = sqlite3.connect(sys.argv[1])
first = c.execute("select id from meal_slot order by minutesFromMidnight").fetchone()[0]
c.execute("update meal_slot set name='Café da manhã' where id=?", (first,))
c.commit(); c.execute("pragma wal_checkpoint(TRUNCATE)"); c.execute("pragma journal_mode=DELETE"); c.close()
EOF
"$ADB" push "$TMP/nutri.db" /data/local/tmp/nutri.db >/dev/null
"$ADB" shell chmod 644 /data/local/tmp/nutri.db
"$ADB" shell run-as $PKG sh -c "'rm -f databases/nutri.db-wal databases/nutri.db-shm; cp /data/local/tmp/nutri.db databases/nutri.db'"
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
expect "estimate with actions" 'resource-id="chat-actions"'
shot chatE

tap 'resource-id="chat-swap"' && expect "Trocar opens the slot sheet" 'resource-id="chat-sheet"'
dump; last=$(grep -o 'resource-id="chat-sheet-slot-[0-9]*"' "$TMP/ui.xml" | tail -1)
tap "$last" 0.6
shot chatT
tap 'resource-id="chat-sheet-cancel"'

tap 'resource-id="chat-skip"' && expect "Pular asks for confirmation" 'resource-id="chat-skip-dialog"'
shot chatP
tap 'resource-id="chat-skip-cancel"'

sent=$(calls)
tap 'resource-id="chat-record"' 1.5
expect "Gravar shows the receipt" 'resource-id="chat-receipt-[0-9]+"'
shot chatG
after=$(calls)
if [ "$after" = "$sent" ]; then echo "  ✓ Gravar made no second POST ($after calls)"; else echo "  ✗ Gravar posted again ($sent -> $after)"; FAIL=1; fi

"$ADB" shell input keyevent 4; sleep 1
expect "Home ring shows the recorded kcal" 'text="380" resource-id="home-consumed"'
rows=$(db "select m.name, l.kcal, l.carbs, l.fat, l.source from meal_log l join meal_slot m on m.id = l.slotId")
echo "  meal_log: $rows"
case "$rows" in *"Café da manhã', 380, 36, 16, 'user'"*) echo "  ✓ logged into the suggested slot";; *) echo "  ✗ unexpected meal_log"; FAIL=1;; esac

# A28: Gravar applies only the memory_updates the AI proposed. The plain fake estimate has none, so
# no memory.bin. Existence comes from ls: exec-out merges cat's stderr into the pulled bytes.
# The sealed format and the crash mid-write are checked in the A29 routine block.
if files_has memory.bin; then echo "  ✗ memory.bin written by a plain Gravar"; FAIL=1; else echo "  ✓ no memory.bin after a plain Gravar (no memory_updates)"; fi
if files_has memory.txt; then echo "  ✗ A8 memory.txt still there"; FAIL=1; else echo "  ✓ no A8 memory.txt"; fi

# Gold captures: the flow above is the functional check. The gold message has accents adb cannot
# type, so the exact gold conversation is seeded and chatE / chatT / chatP are captured again.
# Since ST7/A30 chatE has no question bubble either: the questions come before the estimate (chatQ).
seed_gold() { # seed_gold <question or empty>
"$ADB" shell am force-stop $PKG
rm -f "$TMP"/nutri.db*
for f in nutri.db nutri.db-wal nutri.db-shm; do "$ADB" exec-out run-as $PKG cat databases/$f > "$TMP/$f" 2>/dev/null; done
"$PY" - "$TMP/nutri.db" "$1" <<'EOF'
import sqlite3, sys, time
c = sqlite3.connect(sys.argv[1])
question = sys.argv[2] or None
today = c.execute("select firstDay from profile").fetchone()[0]
first = c.execute("select id from meal_slot order by minutesFromMidnight").fetchone()[0]
for table in ("chat_message", "meal_log", "slot_skip"):
    c.execute(f"delete from {table}")
now = int(time.time() * 1000)
c.execute("insert into chat_message(date,role,text,createdAtEpochMs) values(?,?,?,?)",
          (today, "user", "2 pães franceses com 2 ovos mexidos no café da manhã", now - 2000))
c.execute("insert into chat_message(date,role,text,createdAtEpochMs,estimateKcal,estimateP,estimateC,estimateG,"
          "estimateConfidence,estimateSlotId,estimateItems,estimateQuestion) values(?,?,?,?,?,?,?,?,?,?,?,?)",
          (today, "assistant", "Identifiquei 2 pães franceses e 2 ovos mexidos. A estimativa total é de:", now - 1000,
           380, 22, 36, 16, "medium" if question else "high", first, "2 pães franceses" + chr(10) + "2 ovos mexidos", question))
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
tap 'resource-id="home-fab"' 1.5
}
seed_gold ""
dump; if grep -q 'resource-id="chat-question"' "$TMP/ui.xml"; then echo "  ✗ question bubble under the estimate"; FAIL=1; else echo "  ✓ estimate without a question bubble"; fi
shot chatE
tap 'resource-id="chat-swap"'
dump; last=$(grep -o 'resource-id="chat-sheet-slot-[0-9]*"' "$TMP/ui.xml" | tail -1)
tap "$last" 0.6
shot chatT
tap 'resource-id="chat-sheet-cancel"'
tap 'resource-id="chat-skip"'
shot chatP
tap 'resource-id="chat-skip-cancel"'

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
expect "turn answered after compact" 'resource-id="chat-actions"'
dump; if grep -q "Resumo QA" "$TMP/ui.xml"; then echo "  ✗ digest drawn as a bubble"; FAIL=1; else echo "  ✓ digest never drawn"; fi
sent_mem=$(curl -s "$FAKE/__calls" | "$PY" -c "import json,sys; d=json.load(sys.stdin); print(repr(d['memory']), d['facts'])")
if [ "$sent_mem" = "'' []" ]; then echo "  ✓ POST carries memory \"\" and facts [] (A28: nothing invented by a plain Gravar)"; else echo "  ✗ POST memory/facts: $sent_mem"; FAIL=1; fi
digests=$(db "select count(*), max(text) from day_digest")
case "$digests" in *"(1, 'Resumo QA"*) echo "  ✓ day_digest stored: $digests";; *) echo "  ✗ day_digest: $digests"; FAIL=1;; esac
inchat=$(db "select count(*) from chat_message where text like 'Resumo QA%'")
if [ "$inchat" = "[(0,)]" ]; then echo "  ✓ digest not in chat_message"; else echo "  ✗ digest in chat_message"; FAIL=1; fi

# A25 / ADR-022: chatX. Empty day, 2100 characters typed: red border, "Texto muito longo", send and
# camera do nothing (0 POST). Back to 2000: sends, the fake gets 2000 code points. adb cannot type
# accents: the gold message goes without them.
"$ADB" shell am force-stop $PKG
rm -f "$TMP"/nutri.db*
for f in nutri.db nutri.db-wal nutri.db-shm; do "$ADB" exec-out run-as $PKG cat databases/$f > "$TMP/$f" 2>/dev/null; done
"$PY" - "$TMP/nutri.db" <<'EOF'
import sqlite3, sys
c = sqlite3.connect(sys.argv[1])
for table in ("chat_message", "day_digest", "meal_log", "slot_skip"):
    c.execute(f"delete from {table}")
c.commit(); c.execute("pragma wal_checkpoint(TRUNCATE)"); c.execute("pragma journal_mode=DELETE"); c.close()
EOF
"$ADB" push "$TMP/nutri.db" /data/local/tmp/nutri.db >/dev/null
"$ADB" shell chmod 644 /data/local/tmp/nutri.db
"$ADB" shell run-as $PKG sh -c "'rm -f databases/nutri.db-wal databases/nutri.db-shm; cp /data/local/tmp/nutri.db databases/nutri.db'"
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
expect "2000 characters answered" 'resource-id="chat-actions"'
len=$(curl -s "$FAKE/__calls" | "$PY" -c "import json,sys; print(json.load(sys.stdin)['textLen'])")
if [ "$len" = 2000 ]; then echo "  ✓ fake got 2000 characters"; else echo "  ✗ fake got $len characters"; FAIL=1; fi
fi

# ------------------------------------------------------------------ helpers (A29, A30)
# The device clock moves (cmd alarm set-time, no root): 3 breakfasts on 3 past days make a strong
# dynamic routine through the real flow (fake memory_updates -> Gravar -> memory.bin, Keystore key).
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
  rm -f "$TMP"/nutri.db*
  for f in nutri.db nutri.db-wal nutri.db-shm; do "$ADB" exec-out run-as $PKG cat databases/$f > "$TMP/$f" 2>/dev/null; done
  "$PY" - "$TMP/nutri.db" "$1" <<'EOF'
import sqlite3, sys, time, datetime, zoneinfo
c = sqlite3.connect(sys.argv[1])
today = datetime.datetime.now(zoneinfo.ZoneInfo("America/Sao_Paulo")).date().isoformat()
exec(sys.argv[2])
c.commit(); c.execute("pragma wal_checkpoint(TRUNCATE)"); c.execute("pragma journal_mode=DELETE"); c.close()
EOF
  "$ADB" push "$TMP/nutri.db" /data/local/tmp/nutri.db >/dev/null
  "$ADB" shell chmod 644 /data/local/tmp/nutri.db
  "$ADB" shell run-as $PKG sh -c "'rm -f databases/nutri.db-wal databases/nutri.db-shm; cp /data/local/tmp/nutri.db databases/nutri.db'"
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
if has chat-actions || has chat-force-estimate; then echo "  ✗ actions or Forçar on round 1"; FAIL=1; else echo "  ✓ round 1: no actions, no Forçar"; fi
[ "$(clarify_sent)" = "0 False" ] && echo "  ✓ clarify_rounds 0 sent" || { echo "  ✗ sent $(clarify_sent)"; FAIL=1; }
say "creme%sde%sleite"
expect "round 2: Forçar estimativa" 'resource-id="chat-force-estimate"'
[ "$(clarify_sent)" = "1 False" ] && echo "  ✓ clarify_rounds 1 sent" || { echo "  ✗ sent $(clarify_sent)"; FAIL=1; }
before=$(calls)
tap 'resource-id="chat-force-estimate"' 3
expect "Forçar: estimate with actions" 'resource-id="chat-actions"'
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
expect "4th turn: the estimate" 'resource-id="chat-actions"'
[ "$(clarify_sent)" = "3 False" ] && echo "  ✓ clarify_rounds 3 sent" || { echo "  ✗ sent $(clarify_sent)"; FAIL=1; }
tap 'resource-id="chat-record"' 2
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
c.execute("insert into chat_message(date,role,text,createdAtEpochMs,estimateKcal,estimateP,estimateC,estimateG,estimateConfidence,estimateSlotId,estimateItems,intent) values(?,?,?,?,?,?,?,?,?,?,?,?)",
          (today, "assistant", "Identifiquei 2 pães franceses e 2 ovos mexidos. A estimativa total é de:", now - 1000, 380, 22, 36, 16, "high", first, "2 pães franceses" + chr(10) + "2 ovos mexidos", "log"))
'
open_chat
expect "gold: estimate with actions" 'resource-id="chat-actions"'
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
# Keyboard down: the tall reply plus its question fit the thread again.
"$ADB" shell dumpsys input_method | grep -q 'mInputShown=true' && { "$ADB" shell input keyevent 4; sleep 0.8; }
expect "send at the bottom: followed to the new reply" 'text="pao de queijo"'

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
  expect "day -$back: estimate with Gravar" 'resource-id="chat-record"'
  [ "$back" = 3 ] && expect "day -3: Memória atualizada on the answer (leite)" 'resource-id="chat-memory-updated"'
  [ "$back" = 1 ] && expect "day -1: origin chips (permanente + dinâmica)" 'chat-memory-permanent.*chat-memory-dynamic'
  # A8b intent on the A28 file: the stale memory.bin.new left below never reaches the memory.
  if [ "$back" = 2 ]; then
    keys=$(fact_keys)
    if [ "$keys" = "cafe leite" ]; then echo "  ✓ stale memory.bin.new ignored, memory intact (facts: $keys)"; else echo "  ✗ facts after crash mid-write: '$keys'"; FAIL=1; fi
  fi
  tap 'resource-id="chat-record"' 2
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
      echo "  ✗ memory.bin missing after the routine Gravar"; FAIL=1
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
dump; if grep -q 'resource-id="chat-record"\|resource-id="chat-swap"' "$TMP/ui.xml"; then echo "  ✗ Gravar/Trocar on a plan"; FAIL=1; else echo "  ✓ no Gravar/Trocar/Pular"; fi
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
c.execute("insert into chat_message(date,role,text,createdAtEpochMs,estimateKcal,estimateP,estimateC,estimateG,estimateConfidence,estimateSlotId,intent,memoryUsedKinds,memoryUpdated) values(?,?,?,?,?,?,?,?,?,?,?,?,?)",
          (today, "assistant", "Usei o seu café de sempre, com pão integral no lugar do francês e leite semidesnatado, como você costuma usar.", now - 1000, 430, 26, 36, 20, "high", first, "log", "permanent,dynamic", 1))
'
open_chat
expect "chips in order" 'chat-memory-updated.*chat-memory-permanent.*chat-memory-dynamic'
shot chatM
fi
"$ADB" shell settings put global auto_time 1

rm -rf "$TMP"
exit $FAIL
