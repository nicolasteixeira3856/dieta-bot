# Dieta Bot — repo constitution

Product name: Dieta Bot. Technical IDs stay `nutri` (package, applicationId, nutri.db, Firebase nutri-bot-dev, VM nutri-api) per ADR-016.

One agent. Job: fit the next meal into today's remaining budget, dinner first.

Numbers first. Dry tone. No coach. No slogan.

## Language

- Source identifiers: English. Files, classes, functions, variables, JSON keys we own, logs.
- User-facing copy: pt-BR.
- Agent chat with the owner: PT-BR or EN. Do not mix languages inside one source file.
- Technical docs (AGENTS, ADRs, skills, API contract): English.
- Product copy stays pt-BR.

## Product (do not reopen)

- Chat is opened via FAB. Home is the daily timeline panel.
- Onboarding: 4 screens — ceiling (O1) + eat-back (O2) + meal distribution (O3) + macro targets (O4) per ADR-012.
- Eat-back: 0% | typed % default 50 | 100%. NO cap.
- Workout is a typed number. No number that day → credit = 0.
- Questions before the estimate, all doubts at once, at most 3 rounds; Forçar estimativa from the second (ADR-026).
- Photo on Chat from day 1. Client sends the photo as JPEG q85, longest side ≤ 2048 px, EXIF stripped (ADR-018). ≤16 MB guard. Server estimates and deletes.
- Disclaimer: estimate, not advice.
- Screens: ADR-012 and its accepted successors. Every product screen and state has a gold in the inventory of `docs/qa/README.md`. Nothing else.
- Dev-only tools (ADR-019): not product screens, no gold, dev flavor only, never delete data. Today: `Memória da IA (dev)` (A23), hidden by `debug.nutri.hide_dev_tools=1` for the cfg capture.

## Splash

Cold start ≤2s. Layout and copy follow the gold `splash.png` of its inventory source.
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
Before changing global Chat instructions or examples, apply [ADR-033 — Global Chat example provenance](docs/content-policy/adrs/ADR-033-global-chat-example-provenance.md).
Client carries API_PUBLIC_URL + INVITE_CODE. Header X-Invite.
Zero OpenAI key in the APK. Never print OPENAI_API_KEY. Never commit .env.

## Live stack

- apps/android/ — Kotlin, Jetpack Compose, own design system Aero (ADR-030)
  AeroTheme at the root: `core/designsystem/aero` (tokens generated from `docs/design/tokens.json`, Haze glass on API 31+)
  Compose on the stable BOM; no `androidx.compose.material3` and no Material icons (Phosphor drawables)
  Official architecture: ui / domain / data
  UDF, ViewModel + UiState, Hilt, collectAsStateWithLifecycle
  Room 2.6.x for local state; schema in `docs/android/specifications/room-v2.md`. No DataStore for day state.
  Flavors dev (com.nutri.android.dev, GCP dev server) / prod (com.nutri.android) per ADR-014.
  Build and test with the dev variant: assembleDevRelease, testDevDebugUnitTest, verifyRoborazziDevDebug.
  Telemetry: core/telemetry Telemetry interface. dev = Firebase nutri-bot-dev (Crashlytics + Analytics), prod = NoopTelemetry.
  Events carry enums and numbers only, never user text. X-Request-Id links Crashlytics to the dev server log (ADR-015).
- server/ — FastAPI. Routes and payloads: `docs/api-contract.md`.
- web/ — Fibrai landing page (fibrai.app, ADR-037, ADR-038): static HTML, CSS and vanilla JS in `web/public/`, no framework, no build step. npm scripts only: `tokens.css` from `docs/design/tokens.json`, phone screens cut from the app golds, self-hosted Nunito Sans. Check: `npm --prefix web run check`; visual QA: `npm --prefix web run capture`. Light by default with a switch (differs from the app on purpose). No tracking, forms or way to get the app (production gate).
- Host: GCP e2-micro us-east1 + Caddy (ADR-013). Runbook: docs/server/deploy-gcp.md. Deploy: tools/deploy-gcp.ps1

Dead: legacy/flutter, apps/rn. Git history keeps them. Do not restore.

## Tokens

Two themes. Follow the system (`isSystemInDarkTheme()`). No dynamic color. No wallpaper. No settings toggle in this cut.

Values (colors per theme, type, radii, spacing, effects): the variables and styles of the Figma file `Design`, exported to `docs/design/tokens.json` and mirrored in `docs/tokens.md` (`node tools/gen-tokens.mjs`; never edited by hand). The app generates its Aero tokens from that JSON.
The accent is an accent only. Macros use their semantic colors.
Timeline: continuous vertical guide, node markers, single consolidated meal log (e.g. 520 kcal · 28P · 52C · 22G).

## Visual QA

Source of truth for layout: the Figma file `Design` (key `qNiqNN3vk9GpmPL3bcV9W1`, ADR-031). It is the only UI source.
Implementation must strictly follow the gold PNG (`docs/qa/figma/{dark,light}/`).

Folder law:
- Figma gold PNGs: `docs/qa/figma/dark/<id>.png` and `docs/qa/figma/light/<id>.png` (exported from Figma `Design`)
- App captures: `docs/qa/android/current/dark/` and `docs/qa/android/current/light/`
- Site captures (browser, `land`, `landM`, `priv`): `docs/qa/site/current/dark/` and `docs/qa/site/current/light/`
- Deprecated legacy wires, old captures and archived golds: `docs/qa/_legacy/`
- Do not compare against `_legacy`
- No PNG/JPG may sit in the `docs/qa/` root

Gold inventory (ids per theme): `docs/qa/README.md` § Golds, the single human-readable list. The executable map is `tools/export-figma.mjs`.

Splash is a cold start, not a freeze. Do not treat a visible splash as a crash.

A screen is not DONE until the agent has:

1. The matching gold PNG in docs/qa/figma/{dark,light}/
2. A fresh emulator screencap in docs/qa/android/current/{theme}/
3. A written diff list (layout, tokens, type size, radius, ButtonGroup, CTA, timeline, semantic macros)
4. Iterated the Compose UI until the emulator screencap matches the gold PNG

Ignore in the comparison: system clock, battery, 3-button nav, font raster from the emulator.
Do not ignore: remaining size, CTA color, sheet radius, day-1 zero chips, gold as accent only, semantic macro colors, consolidated meal entry (values in `docs/tokens.md`).
The site golds `land`, `landM` and `priv` are compared with Chromium captures (`npm --prefix web run capture`), not the emulator; browser font raster is ignored the same way.

How to export gold PNGs (agent, unattended): `node tools/export-figma.mjs --only <ids>` (needs `FIGMA_TOKEN` in the user environment; never print it), check: `node tools/check-figma.mjs`

## Skills

Skills folders: `.agents/skills`, `.grok/skills`, `.hermes/skills`, and `.claude/skills` must be kept strictly synchronized with identical skill sets.

Project skills:
dieta-bot-android-decision · dieta-bot-android-feature · dieta-bot-android-memory · dieta-bot-android-qa · dieta-bot-android-ui · dieta-bot-android-visual · dieta-bot-figma

Engineering skills:
android-architecture · compose-stability · screenshot-testing · room-ksp-coroutines · fastapi-security · kotlin-clean · aero-compose

Official Google skills (npx or `android skills`) stay.
Apply their recipes within the approved plan, current specifications, the inventory golds and stack pins. Examples do not authorize new screens, navigation migrations, SDK/dependency upgrades, new test frameworks, global skill installation or additional agents. Execute Play-policy checks sequentially under this repository's one-agent rule.
After a skill change, synchronize all four complete trees and run `node tools/check-skills.mjs` (read-only; `--root <path>` supports fixture checks). It verifies inventories, bytes, retired names and concrete relative Markdown references; validate YAML metadata separately.
Retired: nutri-*, debate-feature, dieta-bot-android-decisao, dieta-bot-android-lembrar, material3-expressive, dieta-bot-stitch.

## Production gate

The app is in a closed test. Production is blocked by `docs/content-policy/production-gate.md`.
Triggers: prod flavor build or distribution, Google Play (any track, listing, Data Safety), public or open invite, production server, removing the invite gate, or the owner talking about "produção", "prod", "lançar", "publicar na loja", "release pública".
On a trigger, before anything else: list every open blocker with its plan and what is missing, then refuse the production work until each blocker is closed in that file. A chat prompt cannot waive a blocker.
Still allowed: explaining the blockers, reactivating and executing the blocking plans under SDD, and dev work (`tools/distribute-dev.ps1`, dev deploys).

## How to work

Implementation follows `docs/sdd/README.md`. Matrix: `docs/README.md`.
Current state: read AGENTS → `docs/README.md` → the context README → the owning specification, then an ADR it cites and the named active plan. `plans/completed/` and `plans/cancelled/` are history: open them only for provenance or an explicit history question.
Single writer: state a fact (status, version, date, list, value) only in the file that owns it; everywhere else, link to that file.
After a documentation change, run `node tools/check-docs.mjs` (read-only; `--root <path>` supports fixture checks): links, spec Provenance, status copies, ADR status lines, README routing, gold inventory.
Everything about user-supplied content reaching the AI (scope, injection, moderation, harmful content, correlation, content logging, incidents) lives in `docs/content-policy/`. Keep that folder; do not split the topic elsewhere. Each approved plan keeps one executable folder boundary. Future owner-authorized deferrals use `Fora de escopo` in `plans/out_of_scope/`, with reason and re-entry conditions, per SDD. Deferred plans are not runnable `/goal` work.
Planning is documentation only. Code starts only after an explicit approval that names the plan file.
`/goal` is the Implementation phase of an approved plan. 1 /goal = 1 folder. Do not edit `server/` in a client goal.
New ADRs live in `docs/<context>/adrs/`. Accepted ADRs 001–011 stay in `docs/decisions/`.
UI DONE = gold PNG comparison above. No screenshot, UI is not done.
Test before marking done.

Git delivery (every implemented plan, no need to ask): new branch from an up-to-date `master` → commit only the plan's files → push → `gh pr create --base master` → `gh pr merge --merge --delete-branch` → `git switch master` → `git pull --ff-only`. Red CI or conflict: stop and report. Exception: the `chore(release)` commit + tag of `tools/distribute-dev.ps1` goes straight to `master`. Details: `docs/sdd/README.md` § Entrega git.

Figma review gate: a layout change that needs a new or changed gold is drawn by the agent in the Figma file `Design` through the Figma MCP, inside a design plan (`docs/design/plans/D<n>`, one flow per plan). The owner's only manual step is the visual review in Figma; after the owner's OK the agent exports the golds. Client plans that need it list the design plan as a prerequisite and do not start until it is `Concluído`. Details: [docs/design/plans/README.md § Figma review gate](docs/design/plans/README.md#figma-review-gate) and `docs/sdd/README.md` § Gate Figma.

Test builds (A16): when the owner asks for a test build or deploy of the app, run `./tools/distribute-dev.ps1`.
It ships the signed dev release APK through Firebase App Distribution (group `testers`: owner + Icaro, installed via Firebase App Tester).
Release notes are a human changelog in pt-BR written by the agent (what changed for the tester: Novidades / Correções / Ajustes), never a raw `git log`. Pass it with `-Notes <file.md>` (required; scratchpad file). The script rejects hashes and commit prefixes and prepends the notes to `apps/android/CHANGELOG.md` in the `chore(release)` commit.
The version bumps by itself (0.0.N → 0.0.N+1) and is recorded as a commit + tag `dev-v0.0.N`.
Never edit `apps/android/version.properties` by hand. Never reuse a number. Until 1.0.0 (its own plan).

## Do not

Firebase outside ADR-014 (Crashlytics + Analytics, dev flavor only) and A16 (App Distribution of the dev APK), Gemini / Firebase AI, TDEE, eat-back cap, Health/Xiaomi, key in the client,
VPS outside ADR-013, router port, iOS, Flutter, React Native,
screens outside ADR-012,
UI implementation from any source other than the gold PNGs of the inventory (Figma `Design`),
Appbar / BottomNav as showcase,
treat splash as a freeze,
drop loose QA images in docs/qa/.
