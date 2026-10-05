# A46: the focused field stays above the keyboard; an edit starts at the end of the value.
# Needs $ADB, $TMP and $PY. Each check prints ✓/✗ and sets FAIL=1 on ✗.
export PYTHONIOENCODING=utf-8
ime_dump() {
  "$ADB" shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  "$ADB" exec-out cat /sdcard/ui.xml > "$TMP/ime.xml"
}
# above_ime <resource-id> <label>: the field is inside the window, between the status bar and the keyboard top.
above_ime() {
  sleep 1.2
  local top=0
  if "$ADB" shell dumpsys input_method | grep -q "mInputShown=true"; then
    top=$("$ADB" shell dumpsys window InputMethod | tr -d '\r' | grep -o 'touchable region=SkRegion((0,[0-9]*' | head -1 | sed 's/.*,//')
  fi
  ime_dump
  "$PY" - "$TMP/ime.xml" "$1" "${top:-0}" "$2" <<'PYEOF' || FAIL=1
import re, sys
xml = open(sys.argv[1], encoding="utf-8").read()
rid, top, label = sys.argv[2], int(sys.argv[3]), sys.argv[4]
m = re.search(r'resource-id="' + re.escape(rid) + r'"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml)
if not m or top <= 0:
    print(f"  ✗ {label} ({'no field' if not m else 'field ok'}, keyboard top {top or 'closed'})")
    sys.exit(1)
y1, y2 = int(m.group(2)), int(m.group(4))
ok = y1 >= 48 and y2 <= top
print(f"  {'✓' if ok else '✗'} {label} (field {y1}-{y2}, keyboard top {top})")
sys.exit(0 if ok else 1)
PYEOF
}
# text_of <resource-id>: the field's text as uiautomator reports it.
text_of() {
  ime_dump
  "$PY" - "$TMP/ime.xml" "$1" <<'PYEOF'
import re, sys
xml = open(sys.argv[1], encoding="utf-8").read()
node = re.search(r'<node [^>]*resource-id="' + re.escape(sys.argv[2]) + r'"[^>]*>', xml)
t = re.search(r' text="([^"]*)"', node.group(0)) if node else None
print(t.group(1) if t else "")
PYEOF
}
# ends_at_end <resource-id> <before> <typed> <label>: what was typed landed after the existing value.
ends_at_end() {
  local now; now=$(text_of "$1" | tr -d '\r')
  if [ "$now" = "$2$3" ]; then echo "  ✓ $4 ('$2' + '$3' = '$now')"; else echo "  ✗ $4 (had '$2', typed '$3', got '$now')"; FAIL=1; fi
}
# tap_desc <content-desc>: tap a node by its content description.
tap_desc() {
  ime_dump
  local xy
  xy=$("$PY" - "$TMP/ime.xml" "$1" <<'PYEOF'
import re, sys
xml = open(sys.argv[1], encoding="utf-8").read()
m = re.search(r'content-desc="' + re.escape(sys.argv[2]) + r'"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml)
if m:
    x1, y1, x2, y2 = map(int, m.groups())
    print((x1 + x2) // 2, (y1 + y2) // 2)
PYEOF
)
  xy=$(printf '%s' "$xy" | tr -d '\r')
  [ -z "$xy" ] && { echo "  ✗ not found: $1"; FAIL=1; return 1; }
  "$ADB" shell input tap $xy; sleep 1
}
