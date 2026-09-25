# 008 — Visual QA folder law and gold PNG export

Date: 2026-09-25

## Decision

Visual QA uses this folder layout:

- Gold PNGs: `docs/qa/wire/<id>.png`
- App captures: `docs/qa/android/current/dark/` and `docs/qa/android/current/light/`
- Old shots: `docs/qa/_legacy/`
- No PNG/JPG in the `docs/qa/` root
- Do not compare against `_legacy`

Gold filenames (13):

splash.png · o1.png · o2.png · t0.png · t0d2.png · t0fds.png · t1.png · t1load.png · t2.png · t2q.png · t3quero.png · t3tenho.png · t3ideia.png

Splash is a cold start, not a freeze. A visible splash is not a crash. Do not remove it.

Layout source: `wires/nutri-wires-expressive.html`.

## Command that worked (Windows, this machine)

From the repo root:

```
npm install --prefix tools
npx --yes playwright install chromium
node tools/export-wires.mjs
```

`node tools/export-wires.mjs` writes the 13 gold names straight into `docs/qa/wire/`. Ran twice; both runs succeeded. Inventory: `node tools/check-wires.mjs`.

Playwright lives in `tools/package.json`. Chromium landed at `%LOCALAPPDATA%\ms-playwright\chromium-1243`.

Tracked emulator/RN shots that used to sit in the `docs/qa/` root were restored from git and moved into `docs/qa/_legacy/` (not deleted).

## Rejected

- Numbered gold names (`00-splash.png` … `12-t3-no-idea.png`) as the gating set. Old numbered leftover `00-splash.png` moved to `docs/qa/_legacy/`.
- Treating `_legacy/` as a comparison oracle.
- Inventing gold PNGs. Export is Playwright screenshot of `#phone`.
- HTML Downloads fallback. Playwright ran here.
- Editing `apps/android/` UI or `server/`.
