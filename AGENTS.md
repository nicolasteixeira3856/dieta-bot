# Nutri — repo constitution

One agent. Job: fit the next meal into today's remaining budget, dinner first.

Numbers first. Dry tone. No coach. No slogan.

## Language

- Source identifiers: English. Files, classes, functions, variables, JSON keys we own, logs.
- User-facing copy: pt-BR.
- Agent chat with the owner: PT-BR or EN. Do not mix languages inside one source file.
- Technical docs (AGENTS, ADRs, skills, API contract, SETUP): English.
- Product copy stays pt-BR.

## Product (do not reopen)

- Chat is opened via FAB. Home is the daily timeline panel.
- Onboarding: 4 screens — ceiling (O1) + eat-back (O2) + meal distribution (O3) + macro targets (O4) per ADR-012.
- Eat-back: 0% | typed % default 50 | 100%. NO cap.
- Workout is a typed number. No number that day → credit = 0.
- One question if confidence is not high.
- Photo on Chat from day 1. Client: ≤1280 JPEG 70. Server estimates and deletes.
- Disclaimer: estimate, not advice.
- Screens: ADR-012 (splash, O1, O2, O3, O4, Home [home0, home1, homeX], Chat [chat0, chatL, chatE, chatT, chatP, chatF, chatG], Config [cfg, wipe], Push). Nothing else.

## Splash

Cold start ≤2s. Layout and copy follow the Stitch gold `splash.png`.
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
  Room 2.6.x for profile + day + meal_log. No DataStore for day state.
- server/ — FastAPI, untouched contract: GET /health, POST /v1/estimate, POST /v1/fit

Dead: legacy/flutter, apps/rn. Git history keeps them. Do not restore.

## Tokens

Two themes. Follow the system (`isSystemInDarkTheme()`). No dynamic color. No wallpaper. No settings toggle in this cut.

Dark:
bg #0b0d10 · panel #12151a · phone #0e1114 · surf #171b20 · surf2 #1e242b · line #2a3139
text #f3f5f7 · muted #8b939c · dim #5c6570 · gold #e8b86d · good #7dda9a · bad #e07a6a
protein #4ec994 · carbs #e58e42 · fat #e8b86d
CTA #f3f5f7 on #111

Light:
bg #f4f3f0 · panel #eceae6 · phone #f7f6f3 · surf #ffffff · surf2 #e8e6e2 · line #d5d2cc
text #14161a · muted #5c636b · dim #8b939c · gold #b8873d · good #1f8a4c · bad #c14d40
protein #1b7a4b · carbs #c2651e · fat #b8873d
CTA #111111 on #f3f5f7

Remaining 34pt w590. Field 28pt. Sheet radius 22 top. Card/chip 14. Bar 6px gold.
Timeline: continuous vertical guide, node markers, single consolidated meal log (e.g. 520 kcal · 28P · 52C · 22G).

## Visual QA

Source of truth for layout: Google Stitch project `Nutri` (ID: `6282733070135794645`).
Implementation must strictly follow the Stitch gold PNGs (`docs/qa/stitch/{dark,light}/`). Layout creation can use wireframes as initial reference (prefer Stitch via MCP, fallback to wireframes).

Folder law:
- Gold PNGs: `docs/qa/stitch/dark/<id>.png` and `docs/qa/stitch/light/<id>.png` (exported from Stitch `Nutri`)
- App captures: `docs/qa/android/current/dark/` and `docs/qa/android/current/light/`
- Deprecated legacy wires & old captures: `docs/qa/_legacy/`
- Do not compare against `_legacy`
- No PNG/JPG may sit in the `docs/qa/` root

Gold filenames (18 per theme):
splash.png · o1.png · o2.png · o3.png · o4.png
home0.png · home1.png · homeX.png
chat0.png · chatL.png · chatE.png · chatT.png · chatP.png · chatF.png · chatG.png
cfg.png · wipe.png · push.png

Splash is a cold start, not a freeze. Do not treat a visible splash as a crash.

A screen is not DONE until the agent has:

1. The matching gold PNG in docs/qa/stitch/{dark,light}/
2. A fresh emulator screencap in docs/qa/android/current/{theme}/
3. A written diff list (layout, tokens, type size, radius, ButtonGroup, CTA, timeline, semantic macros)
4. Iterated the Compose UI until the emulator screencap matches the Stitch gold PNG

Ignore in the comparison: system clock, battery, 3-button nav, font raster from the emulator.
Do not ignore: remaining 34pt, CTA color, sheet radius 22, day-1 zero chips, gold as accent only, semantic macro colors, consolidated meal entry.

How to export gold PNGs (agent, unattended):
- `node tools/export-stitch.mjs`
- Inventory: `node tools/check-stitch.mjs`

## Skills

Skills folders: `.agents/skills`, `.grok/skills`, and `.hermes/skills` must be kept strictly synchronized with identical skill sets.

Project skills:
dieta-bot-android-decision · dieta-bot-android-feature · dieta-bot-android-memory · dieta-bot-android-qa · dieta-bot-android-ui · dieta-bot-android-visual

Engineering skills:
android-architecture · compose-stability · screenshot-testing · room-ksp-coroutines · fastapi-security · kotlin-clean · material3-expressive

Official Google skills from npx stay.
Retired: nutri-*, debate-feature, dieta-bot-android-decisao, dieta-bot-android-lembrar.

## How to work

Implementation follows `docs/sdd/README.md`. Matrix: `docs/README.md`.
Planning is documentation only. Code starts only after an explicit approval that names the plan file.
`/goal` is the Implementation phase of an approved plan. 1 /goal = 1 folder. Do not edit `server/` in a client goal.
New ADRs live in `docs/<context>/adrs/`. Accepted ADRs 001–011 stay in `docs/decisions/`.
UI DONE = gold PNG comparison above. No screenshot, UI is not done.
Test before marking done.

## Do not

Firebase, Gemini, TDEE, eat-back cap, Health/Xiaomi, key in the client,
VPS, router port, iOS, Flutter, React Native,
screens outside ADR-012,
implementation based on wires instead of Stitch gold PNGs,
Appbar / BottomNav as showcase,
treat splash as a freeze,
drop loose QA images in docs/qa/.
