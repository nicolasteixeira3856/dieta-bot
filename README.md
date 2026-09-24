# Nutri

App Android para encaixar a próxima refeição no saldo do dia, sobretudo a janta.

Registra em linguagem natural + foto. Devolve kcal, P e se cabe. Não é diário de busca. Não é nutricionista. Não calcula TDEE. Estimativa, não consulta.

## Stack

| Camada | O quê |
|---|---|
| Client | `apps/android/` — Kotlin, Jetpack Compose, Material 3 Expressive |
| API | `server/` — FastAPI, `gpt-6-luna`, `reasoning.effort=none` |
| Infra | Torre em casa + Cloudflare Tunnel. Sem VPS. Sem porta no roteador |

Arquitetura do client: `ui` / `domain` / `data`. UDF. ViewModel + `StateFlow`. Hilt. Fórmulas em domain puro.

## O que o app faz

1. O1 — teto (mesmo todos os dias / útil-fds / 7 dias)
2. O2 — eat-back (0% / % digitável / 100%, sem cap)
3. T0 — Home: saldo, próxima janela, composer texto+foto, CTA “o que cabe agora”
4. T1 — sheet registra
5. T2 — card curto
6. T3 — sheet encaixa no orçamento (`POST /v1/fit`)

Home no dia 1 não tem chip. Chip nasce no 2º log estável da janela.

## Pastas

```
apps/android/     client
server/           API
docs/             contrato, ADRs, tokens, QA
wires/            nutri-wires.html
.grok/skills/     skills do agente
```

Flutter e React Native saíram do tree. Estão no histórico git.

## Rodar (Windows)

Emulador aberto (`adb devices` mostra device).

```powershell
# API (na torre)
cd server
docker compose up -d

# App
cd apps\android
.\gradlew.bat test
.\gradlew.bat :app:installDebug
```

Client usa `API_PUBLIC_URL` + `INVITE_CODE`. Header `X-Invite`. Nunca `OPENAI_API_KEY` no APK. `.env` não entra no git.

## Tokens

`docs/tokens.md`. Fundo `#0b0d10`, acento `#e8b86d`, CTA `#f3f5f7` / `#111`. Sem roxo Material default. Sem dynamic color.

## Agente

Constituição: `AGENTS.md`.
1 `/goal` = 1 pasta.
UI DONE = screenshot no emulador vs wire.
Decisão = ADR em `docs/decisions/`.
