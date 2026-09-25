# Nutri — repo constitution

One agent. Job: fit the next meal into today's remaining budget, dinner first.

Numbers first. Dry tone. No coach. No slogan.

## Language

- Source identifiers: English. Files, classes, functions, variables, JSON keys we own, logs.
- User-facing copy: pt-BR.
- Agent chat with the owner: PT-BR or EN. Do not mix languages inside one source file.
- Technical docs (AGENTS, ADRs, skills, API contract, SETUP): English.
- Product wires stay pt-BR.

## Product (do not reopen)

- Chat is a sheet, not the home.
- Home day 1: zero chips.
- Chip on the 2nd stable log of the same window, asked once, removable.
- Onboarding: 2 screens — ceiling + eat-back.
- Eat-back: 0% | typed % default 50 | 100%. NO cap.
- Workout is a typed number. No number that day → credit = 0.
- One question if confidence is not high.
- Photo on T1 from day 1. Client: ≤1280 JPEG 70. Server estimates and deletes.
- Weekend: lunch + dinner + close only.
- Disclaimer: estimate, not advice.
- Screens: splash, O1, O2, T0, T1, T2, T3. Nothing else.

## Splash

Cold start ≤2s. Wordmark + gold bar. Copy: “estimativa, não consulta”.
Splash is not a freeze. Do not remove it. Do not treat a visible splash as a crash.

## Formulas

Timezone: America/Sao_Paulo.

- effectiveCeiling = baseCeiling + credit
- credit = 0 if policy is 0 OR workout kcal for the day is missing
- credit = workoutKcal * pct/100 if partial
- credit = workoutKcal if 100%
- windowBudget = max(0, effectiveCeiling − eaten − reservedUpcoming)

## LLM

LLM only on the server: gpt-6-luna, reasoning.effort=none.
Client carries API_PUBLIC_URL + INVITE_CODE. Header X-Invite.
Zero OpenAI key in the APK. Never print OPENAI_API_KEY. Never commit .env.

## Live stack

- apps/android/ — Kotlin, Jetpack Compose, Material 3 Expressive
  MaterialExpressiveTheme + MotionScheme.expressive()
  material3 1.5.0-alpha29 (or newer 1.5 alpha) on top of the stable BOM
  Official architecture: ui / domain / data
  UDF, ViewModel + UiState, Hilt, collectAsStateWithLifecycle
- server/ — FastAPI, untouched contract: GET /health, POST /v1/estimate, POST /v1/fit

Dead: legacy/flutter, apps/rn. Git history keeps them. Do not restore.

## Tokens

Two themes. Follow the system (`isSystemInDarkTheme()`). No dynamic color. No wallpaper. No settings toggle in this cut.

Dark:
bg #0b0d10 · panel #12151a · phone #0e1114 · surf #171b20 · surf2 #1e242b · line #2a3139
text #f3f5f7 · muted #8b939c · dim #5c6570 · gold #e8b86d · good #7dda9a · bad #e07a6a
CTA #f3f5f7 on #111

Light:
bg #f4f3f0 · panel #eceae6 · phone #f7f6f3 · surf #ffffff · surf2 #e8e6e2 · line #d5d2cc
text #14161a · muted #5c636b · dim #8b939c · gold #b8873d · good #1f8a4c · bad #c14d40
CTA #111111 on #f3f5f7

Remaining 34pt w590. Field 28pt. Sheet radius 22 top. Card/chip 14. Bar 6px gold.

## Visual QA

Source of truth for layout: `wires/nutri-wires-expressive.html`.

Folder law:
- Gold PNGs: `docs/qa/wire/<id>.png`
- App captures: `docs/qa/android/current/dark/` and `docs/qa/android/current/light/`
- Old shots: `docs/qa/_legacy/`
- Do not compare against `_legacy`
- No PNG/JPG may sit in the `docs/qa/` root

Gold filenames (13):
splash.png · o1.png · o2.png · t0.png · t0d2.png · t0fds.png
t1.png · t1load.png · t2.png · t2q.png
t3quero.png · t3tenho.png · t3ideia.png

Splash is a cold start, not a freeze. Do not treat a visible splash as a crash.

A screen is not DONE until the agent has:

1. The matching gold PNG in docs/qa/wire/
2. A fresh emulator screencap in docs/qa/android/current/{theme}/
3. A written diff list (layout, tokens, type size, radius, ButtonGroup, CTA)
4. Iterated the Compose UI until that list is empty

Ignore in the comparison: system clock, battery, 3-button nav, font raster from the emulator.
Do not ignore: remaining 34pt, CTA color, sheet radius 22, day-1 zero chips, gold as accent only.

How to export gold PNGs (agent, unattended):
- Preferred: `npm install --prefix tools` then `npx --yes playwright install chromium` then `node tools/export-wires.mjs`
- Fallback: open `wires/nutri-wires-expressive.html`, click Export PNGs, move `Downloads\wire-*.png` into `docs/qa/wire/` and strip the `wire-` prefix. If the exporter already wrote the 13 gold names, move them as-is.
- Inventory: `node tools/check-wires.mjs`

## Skills

dieta-bot-android-decision
dieta-bot-android-feature
dieta-bot-android-memory
dieta-bot-android-qa
dieta-bot-android-ui
dieta-bot-android-visual
Official Google skills from npx stay.
Retired: nutri-*, dieta-bot-android-decisao, dieta-bot-android-lembrar.

## How to work

1 /goal = 1 folder. Do not edit server/ in a client goal (except the english-rename goal already done).
UI DONE = gold PNG comparison above. No screenshot, UI is not done.
Every product or tech decision becomes an ADR in docs/decisions/.
Test before marking done.

## Do not

Firebase, Gemini, TDEE, eat-back cap, Health/Xiaomi, key in the client,
VPS, router port, iOS, Flutter, React Native,
screens outside splash O1 O2 T0 T1 T2 T3,
Appbar / FAB / BottomNav as showcase,
treat splash as a freeze,
drop loose QA images in docs/qa/.
