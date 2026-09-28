#!/usr/bin/env bash
# Chat QA on a running emulator against tools/fake-chat-server.mjs (no OpenAI):
# onboarding -> chat0 -> chatL -> (dark: timeout + retry) -> chatE -> chatT -> chatP -> Gravar -> chatG
# -> Home ring. Captures land in docs/qa/android/current/<theme>/.
#
# Prereqs: node tools/fake-chat-server.mjs running on the host (port 8765);
#   APK built with -PAPI_PUBLIC_URL=http://10.0.2.2:8765 and installed;
#   AVD at gold geometry (wm size 780x1688, wm density 320); python3; curl.
# Usage: tools/capture-chat.sh dark|light
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
shot() { sleep "${2:-0.8}"; "$ADB" exec-out screencap -p > "$OUT/$1.png"; echo "  captured $THEME/$1"; }
mode() { curl -s -X POST -d "{\"hang\": $1}" "$FAKE/__mode" >/dev/null; }
calls() { curl -s "$FAKE/__calls" | "$PY" -c "import json,sys; print(json.load(sys.stdin)['calls'])"; }
last_memory() { curl -s "$FAKE/__calls" | "$PY" -c "import json,sys; print(json.load(sys.stdin)['memory'])"; }
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
"$ADB" shell am start -W -n $PKG/.MainActivity >/dev/null
sleep 3

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
  "$ADB" shell am start -W -n $PKG/.MainActivity >/dev/null; sleep 3
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

# A8/A8b: Gravar wrote one memory line to memory.bin (AES-GCM, key in the Keystore, atomic move).
"$ADB" exec-out run-as $PKG cat files/memory.bin > "$TMP/memory.raw" 2>/dev/null
size=$(wc -c < "$TMP/memory.raw")
if [ "$size" -gt 15 ] && [ "$(head -c 2 "$TMP/memory.raw")" = "NM" ]; then echo "  ✓ memory.bin written ($size bytes, NM header)"; else echo "  ✗ memory.bin missing ($size bytes)"; FAIL=1; fi
if grep -aq "380 kcal\|pães\|Caf" "$TMP/memory.raw"; then echo "  ✗ memory.bin is plaintext"; FAIL=1; else echo "  ✓ memory.bin raw shows no line (not plaintext)"; fi
if "$ADB" exec-out run-as $PKG ls files/ | grep -q "memory.txt"; then echo "  ✗ A8 memory.txt still there"; FAIL=1; else echo "  ✓ no A8 memory.txt"; fi
# Crash mid-write: a half-written memory.bin.new must not touch the memory (checked on the next POST).
"$ADB" shell am force-stop $PKG
"$ADB" shell run-as $PKG sh -c "'echo lixo-de-crash > files/memory.bin.new'"

# Gold captures: the flow above is the functional check. The gold message has accents adb cannot
# type, so the exact gold conversation is seeded and chatE / chatT / chatP are captured again.
"$ADB" shell am force-stop $PKG
rm -f "$TMP"/nutri.db*
for f in nutri.db nutri.db-wal nutri.db-shm; do "$ADB" exec-out run-as $PKG cat databases/$f > "$TMP/$f" 2>/dev/null; done
"$PY" - "$TMP/nutri.db" <<'EOF'
import sqlite3, sys, time
c = sqlite3.connect(sys.argv[1])
today = c.execute("select firstDay from profile").fetchone()[0]
first = c.execute("select id from meal_slot order by minutesFromMidnight").fetchone()[0]
for table in ("chat_message", "meal_log", "slot_skip"):
    c.execute(f"delete from {table}")
now = int(time.time() * 1000)
c.execute("insert into chat_message(date,role,text,createdAtEpochMs) values(?,?,?,?)",
          (today, "user", "2 pães franceses com 2 ovos mexidos no café da manhã", now - 2000))
c.execute("insert into chat_message(date,role,text,createdAtEpochMs,estimateKcal,estimateP,estimateC,estimateG,"
          "estimateConfidence,estimateSlotId,estimateItems) values(?,?,?,?,?,?,?,?,?,?,?)",
          (today, "assistant", "Identifiquei 2 pães franceses e 2 ovos mexidos. A estimativa total é de:", now - 1000,
           380, 22, 36, 16, "high", first, "2 pães franceses" + chr(10) + "2 ovos mexidos"))
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
tap 'resource-id="home-fab"' 1.5
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
mem=$(last_memory)
case "$mem" in *"380 kcal"*) echo "  ✓ POST memory carries the Gravar line: $mem";; *) echo "  ✗ POST memory: '$mem'"; FAIL=1;; esac
digests=$(db "select count(*), max(text) from day_digest")
case "$digests" in *"(1, 'Resumo QA"*) echo "  ✓ day_digest stored: $digests";; *) echo "  ✗ day_digest: $digests"; FAIL=1;; esac
inchat=$(db "select count(*) from chat_message where text like 'Resumo QA%'")
if [ "$inchat" = "[(0,)]" ]; then echo "  ✓ digest not in chat_message"; else echo "  ✗ digest in chat_message"; FAIL=1; fi

rm -rf "$TMP"
exit $FAIL
