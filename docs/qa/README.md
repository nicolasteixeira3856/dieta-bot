# docs/qa

## Folders

- `wire/` gold PNGs exported from `wires/nutri-wires-expressive.html`
- `android/current/dark/` emulator captures, dark theme
- `android/current/light/` emulator captures, light theme
- `_legacy/` old shots. Do not compare against this folder.

No PNG/JPG in the `docs/qa/` root.

## Filenames (13 gold)

splash.png
o1.png
o2.png
t0.png
t0d2.png
t0fds.png
t1.png
t1load.png
t2.png
t2q.png
t3quero.png
t3tenho.png
t3ideia.png

Same basename in `wire/` and `android/current/{theme}/`.

Splash is a cold start, not a freeze.

## Export

Preferred:

```
npm install --prefix tools
npx --yes playwright install chromium
node tools/export-wires.mjs
```

Fallback: open `wires/nutri-wires-expressive.html`, click Export PNGs, move `Downloads\wire-*.png` into `docs/qa/wire/` and strip the `wire-` prefix.

Inventory: `node tools/check-wires.mjs`

## Loop

1. Export gold into `wire/`
2. `adb exec-out screencap` of the same state into `android/current/{theme}/`
3. List diffs against the gold (layout, tokens, type, radius, CTA)
4. Fix Compose
5. Recapture
6. Repeat until the list is empty
