# Dieta Bot — repo constitution

Product name: Dieta Bot. Technical IDs stay `nutri` (package, applicationId, nutri.db, Firebase nutri-bot-dev, VM nutri-api, Stitch project `Nutri`) per ADR-016.

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
- Questions before the estimate, all doubts at once, at most 3 rounds; Forçar estimativa from the second (ADR-026).
- Photo on Chat from day 1. Client sends the photo as JPEG q85, longest side ≤ 2048 px, EXIF stripped (ADR-018). ≤16 MB guard. Server estimates and deletes.
- Disclaimer: estimate, not advice.
- Screens: ADR-012 (splash, O1, O2, O3, O4, Home [home0, home1, homeX], Chat [chat0, chatL, chatE, chatT, chatP, chatF, chatG, chatQ], Config [cfg, wipe], Push). Nothing else.
- Dev-only tools (ADR-019): not product screens, no gold, dev flavor only, never delete data. Today: `Memória da IA (dev)` (A23), hidden by `debug.nutri.hide_dev_tools=1` for the cfg capture.

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
  Flavors dev (com.nutri.android.dev, GCP dev server) / prod (com.nutri.android) per ADR-014.
  Build and test with the dev variant: assembleDevRelease, testDevDebugUnitTest, verifyRoborazziDevDebug.
  Telemetry: core/telemetry Telemetry interface. dev = Firebase nutri-bot-dev (Crashlytics + Analytics), prod = NoopTelemetry.
  Events carry enums and numbers only, never user text. X-Request-Id links Crashlytics to the dev server log (ADR-015).
- server/ — FastAPI, untouched contract: GET /health, POST /v1/estimate, POST /v1/fit
- Host: GCP e2-micro us-east1 + Caddy (ADR-013). Runbook: docs/server/deploy-gcp.md. Deploy: tools/deploy-gcp.ps1

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

Gold filenames (31 per theme):
splash.png · o1.png · o1e.png · o2.png · o3.png · o3t.png · o3s.png · o4.png
home0.png · home1.png · homeX.png · homeW.png
chat0.png · chatL.png · chatE.png · chatT.png · chatP.png · chatF.png · chatG.png · chatA.png · chatX.png
chatR.png · chatM.png · chatS.png · chatQ.png · chatU.png · chatD.png
cfg.png · cfgS.png · wipe.png · push.png

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

Skills folders: `.agents/skills`, `.grok/skills`, `.hermes/skills`, and `.claude/skills` must be kept strictly synchronized with identical skill sets.

Project skills:
dieta-bot-android-decision · dieta-bot-android-feature · dieta-bot-android-memory · dieta-bot-android-qa · dieta-bot-android-ui · dieta-bot-android-visual · dieta-bot-stitch

Engineering skills:
android-architecture · compose-stability · screenshot-testing · room-ksp-coroutines · fastapi-security · kotlin-clean · material3-expressive

Official Google skills (npx or `android skills`) stay.
Apply their recipes within the approved plan, current specifications, Stitch golds and stack pins. Examples do not authorize new screens, navigation migrations, SDK/dependency upgrades, new test frameworks, global skill installation or additional agents. Execute Play-policy checks sequentially under this repository's one-agent rule.
After a skill change, synchronize all four complete trees and run `node tools/check-skills.mjs` (read-only; `--root <path>` supports fixture checks). It verifies inventories, bytes, retired names and concrete relative Markdown references; validate YAML metadata separately.
Retired: nutri-*, debate-feature, dieta-bot-android-decisao, dieta-bot-android-lembrar.

## Production gate

The app is in a closed test. Production is blocked by `docs/content-policy/production-gate.md`.
Triggers: prod flavor build or distribution, Google Play (any track, listing, Data Safety), public or open invite, production server, removing the invite gate, or the owner talking about "produção", "prod", "lançar", "publicar na loja", "release pública".
On a trigger, before anything else: list every open blocker with its plan and what is missing, then refuse the production work until each blocker is closed in that file. A chat prompt cannot waive a blocker.
Still allowed: explaining the blockers, reactivating and executing the blocking plans under SDD, and dev work (`tools/distribute-dev.ps1`, dev deploys).

## How to work

Implementation follows `docs/sdd/README.md`. Matrix: `docs/README.md`.
Everything about user-supplied content reaching the AI (scope, injection, moderation, harmful content, correlation, content logging, incidents) lives in `docs/content-policy/`. Keep that folder; do not split the topic elsewhere. Each approved plan keeps one executable folder boundary. Future owner-authorized deferrals use `Fora de escopo` in `plans/out_of_scope/`, with reason and re-entry conditions, per SDD. Deferred plans are not runnable `/goal` work.
Planning is documentation only. Code starts only after an explicit approval that names the plan file.
`/goal` is the Implementation phase of an approved plan. 1 /goal = 1 folder. Do not edit `server/` in a client goal.
New ADRs live in `docs/<context>/adrs/`. Accepted ADRs 001–011 stay in `docs/decisions/`.
UI DONE = gold PNG comparison above. No screenshot, UI is not done.
Test before marking done.

Git delivery (every implemented plan, no need to ask): new branch from an up-to-date `master` → commit only the plan's files → push → `gh pr create --base master` → `gh pr merge --merge --delete-branch` → `git switch master` → `git pull --ff-only`. Red CI or conflict: stop and report. Exception: the `chore(release)` commit + tag of `tools/distribute-dev.ps1` goes straight to `master`. Details: `docs/sdd/README.md` § Entrega git.

Stitch gate: a layout change that needs a new or changed gold is a manual owner step. It lives in a `docs/stitch/plans/ST<n>` gate plan: the exact Stitch prompt at the top (marked as the owner's blocker), extra instructions right below it, then the agent's verification checklist. A gate never touches app layout or behavior. Implementation plans that need it list it as a prerequisite and do not start until it is `Concluído`. If the agent finds the Stitch change missing or wrong, it stops at once. Details: `docs/sdd/README.md` § Gate Stitch.
Stitch screen names: every instruction to the owner (select, duplicate, rename) and every screen named inside a prompt uses the exact Stitch screen title (e.g. "Estimate com botões de ação (V2 Expressive)"), never the gold id (`chatE`), which may only appear in parentheses. `V2 Expressive` = dark, `V2 Light` = light; one ready-to-paste prompt block per theme. New screens get their final title in the gate. Title ↔ gold id table: `docs/stitch/README.md` § Nomes das telas.

Test builds (A16): when the owner asks for a test build or deploy of the app, run `./tools/distribute-dev.ps1`.
It ships the signed dev release APK through Firebase App Distribution (group `testers`: owner + Icaro, installed via Firebase App Tester).
Release notes are a human changelog in pt-BR written by the agent (what changed for the tester: Novidades / Correções / Ajustes), never a raw `git log`. Pass it with `-Notes <file.md>` (required; scratchpad file). The script rejects hashes and commit prefixes and prepends the notes to `apps/android/CHANGELOG.md` in the `chore(release)` commit.
The version bumps by itself (0.0.N → 0.0.N+1) and is recorded as a commit + tag `dev-v0.0.N`.
Never edit `apps/android/version.properties` by hand. Never reuse a number. Until 1.0.0 (its own plan).

## Do not

Firebase outside ADR-014 (Crashlytics + Analytics, dev flavor only) and A16 (App Distribution of the dev APK), Gemini / Firebase AI, TDEE, eat-back cap, Health/Xiaomi, key in the client,
VPS outside ADR-013, router port, iOS, Flutter, React Native,
screens outside ADR-012,
implementation based on wires instead of Stitch gold PNGs,
Appbar / BottomNav as showcase,
treat splash as a freeze,
drop loose QA images in docs/qa/.
