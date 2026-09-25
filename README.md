# Nutri

App Android para encaixar a próxima refeição no saldo do dia, sobretudo a janta.

Android app that fits the next meal into today's remaining budget, dinner first.

Log in natural language + photo. Returns kcal, P, and whether it fits. Not a food-search diary. Not a dietitian. Does not compute TDEE. Estimate, not advice.

## Stack

| Layer | What |
|---|---|
| Client | `apps/android/` — Kotlin, Jetpack Compose, Material 3 Expressive |
| API | `server/` — FastAPI, `gpt-6-luna`, `reasoning.effort=none` |
| Infra | Home tower + Cloudflare Tunnel. No VPS. No router port |

Client architecture: `ui` / `domain` / `data`. UDF. ViewModel + `StateFlow`. Hilt. Formulas in pure domain.

## What the app does

1. O1 — ceiling (same every day / weekday-weekend / 7 days)
2. O2 — eat-back (0% / typed % / 100%, no cap)
3. T0 — Home: remaining, next window, text+photo composer, CTA “o que cabe agora”
4. T1 — log sheet
5. T2 — short card
6. T3 — fit sheet (`POST /v1/fit`)

Home on day 1 has no chip. A chip appears on the 2nd stable log of the window.

## Folders

```
apps/android/     client
server/           API
docs/             contract, ADRs, tokens, QA
wires/            nutri-wires.html
.grok/skills/     agent skills
```

Flutter and React Native left the tree. They live in git history.

## Run (Windows)

Emulator open (`adb devices` shows a device).

```powershell
# API (on the tower)
cd server
docker compose up -d

# App
cd apps\android
.\gradlew.bat test
.\gradlew.bat :app:installDebug
```

The client uses `API_PUBLIC_URL` + `INVITE_CODE`. Header `X-Invite`. Never `OPENAI_API_KEY` in the APK. `.env` does not go in git.

## Tokens

`docs/tokens.md`. Background `#0b0d10`, accent `#e8b86d`, CTA `#f3f5f7` / `#111`. No default Material purple. No dynamic color.

## Agent

Constitution: `AGENTS.md`.
1 `/goal` = 1 folder.
UI DONE = emulator screenshot vs wire.
Decision = ADR in `docs/decisions/`.
