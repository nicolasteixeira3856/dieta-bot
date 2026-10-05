#!/usr/bin/env bash
# A18 QA on a running emulator against tools/fake-chat-server.mjs (no OpenAI):
# Enter inserts a line break (no send) -> "4 esfihas" recorded in the Ceia -> "também tomei suco"
# re-estimated for the Ceia -> Registrar asks inside the conversation (A34, chatU) -> Outra refeição
# opens Trocar -> Substituir leaves one Ceia log with the new numbers -> Home shows the new total.
# No capture: the inline confirmation is gated by chatU (tools/capture-chat.sh SCENES=a34).
#
# Prereqs: node tools/fake-chat-server.mjs running on the host (port 8765);
#   devDebug APK built with -PAPI_PUBLIC_URL=http://10.0.2.2:8765 and installed;
#   AVD at gold geometry (wm size 780x1688, wm density 320); python3; curl.
# Usage: tools/capture-replace.sh dark|light
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
ACTIVITY=app.fibrai.android.MainActivity
mkdir -p "$OUT"
FAIL=0

bash "$ROOT/tools/capture-onboarding.sh" "$THEME" | tail -1
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
mode() { curl -s -X POST -d "$1" "$FAKE/__mode" >/dev/null; }
calls() { curl -s "$FAKE/__calls" | "$PY" -c "import json,sys; print(json.load(sys.stdin)['calls'])"; }
hide_ime() { "$ADB" shell dumpsys input_method | grep -q "mInputShown=true" && { "$ADB" shell input keyevent 4; sleep 0.8; }; return 0; }
say() { tap 'resource-id="chat-input"' 0.4; "$ADB" shell input text "$1"; tap 'resource-id="chat-send"' 2.5; }
db() { # db <sql> -> rows, after a force-stop so the WAL is in the pulled files
  "$ADB" shell am force-stop $PKG
  rm -f "$TMP"/fibrai.db*
  for f in fibrai.db fibrai.db-wal fibrai.db-shm; do "$ADB" exec-out run-as $PKG cat databases/$f > "$TMP/$f" 2>/dev/null; done
  "$PY" -c "import sqlite3,sys; c=sqlite3.connect(sys.argv[1]); print(c.execute(sys.argv[2]).fetchall())" "$TMP/fibrai.db" "$1"
}

# The last slot is the Ceia of the tester report.
"$ADB" shell am force-stop $PKG
rm -f "$TMP"/fibrai.db*
for f in fibrai.db fibrai.db-wal fibrai.db-shm; do "$ADB" exec-out run-as $PKG cat databases/$f > "$TMP/$f" 2>/dev/null; done
"$PY" - "$TMP/fibrai.db" <<'EOF'
import sqlite3, sys
c = sqlite3.connect(sys.argv[1])
last = c.execute("select id from meal_slot order by minutesFromMidnight desc").fetchone()[0]
c.execute("update meal_slot set name='Ceia' where id=?", (last,))
c.commit(); c.execute("pragma wal_checkpoint(TRUNCATE)"); c.execute("pragma journal_mode=DELETE"); c.close()
EOF
"$ADB" push "$TMP/fibrai.db" /data/local/tmp/fibrai.db >/dev/null
"$ADB" shell chmod 644 /data/local/tmp/fibrai.db
"$ADB" shell run-as $PKG sh -c "'rm -f databases/fibrai.db-wal databases/fibrai.db-shm; cp /data/local/tmp/fibrai.db databases/fibrai.db'"
"$ADB" shell am start -W -n $PKG/$ACTIVITY >/dev/null
sleep 3
tap 'resource-id="home-fab"' 1.5 && expect "FAB opens Chat" 'resource-id="chat"'

# Enter = line break, never a send.
before=$(calls)
tap 'resource-id="chat-input"' 0.6
"$ADB" shell input text "linha1"; "$ADB" shell input keyevent 66; "$ADB" shell input text "linha2"; sleep 0.8
dump
# Sentences capitalization turns the first letter upper case.
if grep -qE 'text="Linha1(&#10;|\n)linha2"' "$TMP/ui.xml"; then echo "  ✓ Enter inserts a line break"; else echo "  ✗ Enter: $(grep -oE 'text="linha1[^"]*"' "$TMP/ui.xml" | head -1)"; FAIL=1; fi
[ "$(calls)" = "$before" ] && echo "  ✓ Enter did not send" || { echo "  ✗ Enter sent a request"; FAIL=1; }
# Clear the composer (12 characters and the line break).
"$ADB" shell input keyevent KEYCODE_MOVE_END; for i in $(seq 1 14); do "$ADB" shell input keyevent 67; done

# 1. First meal into the empty Ceia: recorded at once.
mode '{"slot": "Ceia", "kcal": 880}'
say "4%sesfihas"
expect "estimate offers Registrar" 'resource-id="chat-register"'
tap 'resource-id="chat-register"' 1.5
expect "empty slot records without asking" 'resource-id="chat-receipt-[0-9]+"'
dump; grep -q 'resource-id="chat-replace-card"' "$TMP/ui.xml" && { echo "  ✗ confirmation on an empty slot"; FAIL=1; }

# 2. The whole meal again, for the taken Ceia.
mode '{"slot": "Ceia", "kcal": 1220}'
say "tambem%stomei%s2%scopos%sde%ssuco"
hide_ime
tap 'resource-id="chat-register"' 1.2
expect "taken slot asks inside the conversation" 'resource-id="chat-replace-card"'
expect "copy: old and new kcal" 'text="Ceia tem 880 kcal. Fica com 1220 kcal."'

# 3. Outra refeição: Trocar opens, Room untouched; the question stays.
tap 'resource-id="chat-replace-elsewhere"' 1
expect "Outra refeição opens Trocar" 'resource-id="chat-sheet"'
tap 'resource-id="chat-sheet-cancel"' 1
expect "question still there" 'resource-id="chat-replace-card"'

# 4. Substituir: one Ceia log with the new numbers.
tap 'resource-id="chat-replace-confirm"' 1.5
expect "receipt says Atualizado" 'Atualizado em'
# Back closes the keyboard first, then the Chat.
"$ADB" shell input keyevent 4; sleep 1
dump; grep -q 'resource-id="chat"' "$TMP/ui.xml" && { "$ADB" shell input keyevent 4; sleep 1; }
expect "Home total is the new meal only" 'text="1220" resource-id="home-consumed"'
rows=$(db "select m.name, l.kcal, l.text from meal_log l join meal_slot m on m.id = l.slotId")
echo "  meal_log: $rows"
case "$rows" in "[('Ceia', 1220, '"[Tt]"ambem tomei 2 copos de suco')]") echo "  ✓ one Ceia log, new numbers";; *) echo "  ✗ unexpected meal_log"; FAIL=1;; esac

rm -rf "$TMP"
exit $FAIL
