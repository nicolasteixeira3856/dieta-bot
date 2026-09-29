#!/usr/bin/env bash
# Photo QA (A6, A18/ADR-018) on a running emulator against tools/fake-chat-server.mjs:
# gallery JPEG 2000 px (re-encoded, not enlarged) -> camera (runtime permission + TakePicture,
# longest side <= 2048) -> 50 MP JPEG turned by EXIF (posted at 1536x2048, < 2 MB) -> the old
# "> 16 MB" file now passes -> WebP (JPEG, same branch as HEIC) -> chatF capture with the gold
# conversation seeded. Captures land in docs/qa/android/current/<theme>/.
#
# Prereqs: node tools/fake-chat-server.mjs running (port 8765); devDebug APK built with
#   -PAPI_PUBLIC_URL=http://10.0.2.2:8765 and installed; AVD at gold geometry (wm size 780x1688,
#   wm density 320) with the stock camera app; python3 with Pillow.
# Usage: tools/capture-photo.sh dark|light      Then: node tools/diff-gold.mjs dark/chatF ...
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
# The prompt may already be on screen from the onboarding relaunch: restart so Home opens clean.
"$ADB" shell am force-stop $PKG; "$ADB" shell am start -W -n $PKG/$ACTIVITY >/dev/null; sleep 3

dump() { "$ADB" shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; "$ADB" exec-out cat /sdcard/ui.xml > "$TMP/ui.xml"; }
center() { # center <attribute regex> -> "x y" of the first matching node (current dump)
  local b; b=$(grep -oE "$1[^>]*" "$TMP/ui.xml" | grep -o 'bounds="[^"]*"' | head -1 | tr -dc '0-9,[]')
  [ -z "$b" ] && return 1
  set -- $(echo "$b" | tr '[],' '   ')
  echo "$(( ($1 + $3) / 2 )) $(( ($2 + $4) / 2 ))"
}
tap() { dump; local xy; xy=$(center "$1") || { echo "  ✗ not found: $1"; FAIL=1; return 1; }; "$ADB" shell input tap $xy; sleep "${2:-1.5}"; }
expect() { dump; if grep -qE "$2" "$TMP/ui.xml"; then echo "  ✓ $1"; else echo "  ✗ $1"; FAIL=1; fi; }
shot() { sleep "${2:-0.8}"; "$ADB" exec-out screencap -p > "$OUT/$1.png"; echo "  captured $THEME/$1"; }
fake() { curl -s "$FAKE/__calls" | "$PY" -c "import json,sys; d=json.load(sys.stdin); print($1)"; }
# posted -> GOT="<new POST?> <jpeg?> <width> <height> <under 2 MB?>". A photo only counts if it made a new POST.
posted() {
  set -- $(fake "d['image']['photos'], d['image']['jpeg'], d['image']['width'], d['image']['height'], d['image']['bytes'] < 2 * 1024 * 1024" | tr -d ',')
  local new=False; [ "$1" -gt "$PHOTOS" ] && new=True
  PHOTOS=$1; GOT="$new $2 $3 $4 $5"
}
photos_on_device() { "$ADB" exec-out run-as $PKG ls files/photos/ 2>/dev/null | tr -d '\r' | grep -c '\.jpg$'; }

# Test images: the plate from the chatF gold at 2000 px, the same padded past 16 MB, a WebP, and a
# 50 MP (8160x6120) JPEG with EXIF orientation 6 (rotate 90) and GPS.
PYROOT="$ROOT"
command -v cygpath >/dev/null && PYROOT="$(cygpath -m "$ROOT")"
"$PY" - "$PYROOT" "$TMP" <<'EOF'
import sys
from PIL import Image
root, tmp = sys.argv[1], sys.argv[2]
im = Image.open(root + "/docs/qa/stitch/dark/chatF.png").convert("RGB").crop((238, 392, 722, 662)).resize((2000, 1116), Image.LANCZOS)
im.save(tmp + "/prato.jpg", quality=92)
im.save(tmp + "/prato.webp", quality=90)
data = open(tmp + "/prato.jpg", "rb").read()
open(tmp + "/gigante.jpg", "wb").write(data + b"\0" * (17 * 1024 * 1024 - len(data)))
exif = Image.Exif()
exif[0x0112] = 6  # Orientation: rotate 90 CW
exif[0x010F] = "QA Phone"  # Make
im.resize((8160, 6120), Image.BILINEAR).save(tmp + "/50mp.jpg", quality=95, exif=exif)
EOF
gallery() { # gallery <file>: newest photo in the picker, then pick it
  "$ADB" shell rm -f /sdcard/Pictures/nutri-qa-*
  "$ADB" push "$TMP/$1" "/sdcard/Pictures/nutri-qa-$1" >/dev/null
  "$ADB" shell touch "/sdcard/Pictures/nutri-qa-$1"
  "$ADB" shell content call --uri content://media --method scan_volume --arg external_primary >/dev/null 2>&1
  tap 'resource-id="chat-photo"' && tap 'resource-id="chat-photo-gallery"' 2.5
  tap 'content-desc="Photo taken on' "${2:-4}"
}
"$ADB" shell pm revoke $PKG android.permission.CAMERA >/dev/null 2>&1

"$ADB" shell am start -W -n $PKG/$ACTIVITY >/dev/null; sleep 2
tap 'resource-id="home-fab"' && expect "FAB opens Chat" 'resource-id="chat"'

# 1. Gallery JPEG at 2000 px: re-encoded JPEG q85, not enlarged.
PHOTOS=$(fake "d['image']['photos']")
gallery prato.jpg
posted; got=$GOT
if [ "$got" = "True True 2000 1116 True" ]; then echo "  ✓ gallery JPEG posted at 2000x1116, < 2 MB"; else echo "  ✗ gallery JPEG: fake got '$got'"; FAIL=1; fi
expect "photo bubble in the thread" 'resource-id="chat-photo-[0-9]+"'

# 2. Camera: runtime permission, TakePicture into filesDir/photos through the FileProvider.
before=$(fake "d['image']['photos']")
tap 'resource-id="chat-photo"' && tap 'resource-id="chat-photo-camera"' 2
expect "runtime camera permission asked" 'permission_allow_foreground_only_button'
tap 'resource-id="com.android.permissioncontroller:id/permission_allow_foreground_only_button"' 3
# The emulator camera (webcam backend) can take a while to open: wait for its buttons.
wait_for() { for _ in $(seq 1 20); do dump; grep -q "$1" "$TMP/ui.xml" && return 0; sleep 1; done; return 1; }
wait_for 'com.android.camera2:id/shutter_button'
tap 'resource-id="com.android.camera2:id/shutter_button"' 3
wait_for 'com.android.camera2:id/done_button'
tap 'resource-id="com.android.camera2:id/done_button"' 5
after=$(fake "d['image']['photos'], d['image']['jpeg'], max(d['image']['width'], d['image']['height']) <= 2048")
PHOTOS=$(fake "d['image']['photos']")
if [ "$after" = "$((before + 1)) True True" ]; then echo "  ✓ camera photo posted as JPEG, longest side <= 2048"; else echo "  ✗ camera: $before -> '$after'"; FAIL=1; fi

# 3. 50 MP with EXIF rotation: upright, longest side 2048, < 2 MB, no EXIF kept on the device.
gallery 50mp.jpg 8
posted; got=$GOT
if [ "$got" = "True True 1536 2048 True" ]; then echo "  ✓ 50 MP posted upright at 1536x2048, < 2 MB"; else echo "  ✗ 50 MP: fake got '$got'"; FAIL=1; fi
dump; grep -q 'text="Foto grande demais."' "$TMP/ui.xml" && { echo "  ✗ 50 MP showed 'Foto grande demais.'"; FAIL=1; }
newest=$("$ADB" exec-out run-as $PKG ls -t files/photos/ | head -1 | tr -d '\r')
"$ADB" exec-out run-as $PKG cat "files/photos/$newest" > "$TMP/stored.jpg"
exif=$("$PY" -c "from PIL import Image; print(len(Image.open('$TMP/stored.jpg').getexif()))")
if [ "$exif" = "0" ]; then echo "  ✓ stored photo has no EXIF"; else echo "  ✗ stored photo keeps $exif EXIF tags"; FAIL=1; fi

# 4. The file that was "> 16 MB" (valid JPEG + padding) now passes.
gallery gigante.jpg 4
posted; got=$GOT
if [ "$got" = "True True 2000 1116 True" ]; then echo "  ✓ former > 16 MB file posted, < 2 MB"; else echo "  ✗ former > 16 MB: fake got '$got'"; FAIL=1; fi

# 5. WebP (same branch as HEIC): JPEG before the POST, not enlarged.
gallery prato.webp
posted; got=$GOT
if [ "$got" = "True True 2000 1116 True" ]; then echo "  ✓ WebP became JPEG 2000x1116"; else echo "  ✗ WebP: fake got '$got'"; FAIL=1; fi
"$ADB" shell rm -f /sdcard/Pictures/nutri-qa-*

# chatF: the gold conversation (accents adb cannot type), with the plate photo in app storage.
"$ADB" shell am force-stop $PKG
rm -f "$TMP"/nutri.db*
for f in nutri.db nutri.db-wal nutri.db-shm; do "$ADB" exec-out run-as $PKG cat databases/$f > "$TMP/$f" 2>/dev/null; done
"$ADB" push "$TMP/prato.jpg" /data/local/tmp/chatf.jpg >/dev/null
"$ADB" shell chmod 644 /data/local/tmp/chatf.jpg
"$ADB" shell run-as $PKG sh -c "'mkdir -p files/photos; cp /data/local/tmp/chatf.jpg files/photos/chatf.jpg'"
"$PY" - "$TMP/nutri.db" <<'EOF'
import sqlite3, sys, time
c = sqlite3.connect(sys.argv[1])
today = c.execute("select firstDay from profile").fetchone()[0]
slots = [r[0] for r in c.execute("select id from meal_slot order by minutesFromMidnight")]
almoco = slots[1]
c.execute("update meal_slot set name='Almoço' where id=?", (almoco,))
for table in ("chat_message", "meal_log", "slot_skip", "day_digest"):
    c.execute(f"delete from {table}")
now = int(time.time() * 1000)
c.execute("insert into chat_message(date,role,text,createdAtEpochMs,photoPath) values(?,?,?,?,?)",
          (today, "user", "Almoço de hoje", now - 2000, "/data/user/0/com.nutri.android.dev/files/photos/chatf.jpg"))
c.execute("insert into chat_message(date,role,text,createdAtEpochMs,estimateKcal,estimateP,estimateC,estimateG,"
          "estimateConfidence,estimateSlotId,estimateItems) values(?,?,?,?,?,?,?,?,?,?,?)",
          (today, "assistant", "Identifiquei um Prato Feito com filé de frango grelhado, arroz, feijão e salada verde.",
           now - 1000, 780, 48, 82, 18, "high", almoco, "Prato Feito"))
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
tap 'resource-id="home-fab"' 2
shot chatF

rm -rf "$TMP"
exit $FAIL
