# Nutri — constituição do repo

Um agente. Job: encaixar a próxima refeição no saldo do dia, sobretudo a janta.

Número + tom seco. Sem coach. Sem slogan.

## Produto (congelado)

- Chat é sheet, não home.
- Home dia 1 zero chips.
- Chip no 2º log estável da mesma janela, pergunta 1x, removível.
- Onboarding 2 telas: teto + eat-back.
- Eat-back: 0% | % digitável default 50 | 100%. SEM cap.
- Treino digitado. Sem número no dia, crédito = 0.
- 1 pergunta se confiança ≠ alto.
- Foto T1 desde o dia 1. ≤1280 JPEG 70 no client. Sobe, estima, apaga no server.
- Fds: cobra só almoço + janta + fechamento.
- Disclaimer: estimativa, não consulta.
- Telas: O1 O2 T0 T1 T2 T3. T3 é o job (POST /v1/fit). Settings extra e push 16:30 fora até ADR novo.

## Fórmulas

Timezone: America/Sao_Paulo.

- teto_efetivo = teto_base + credito
- credito = 0 se política 0 OU se treino do dia não informado
- credito = treino_kcal * pct/100 se parcial; = treino_kcal se 100%
- orçamento_janela = max(0, teto_efetivo − comido − reserva_próximas)

teto_base vem do perfil (mesmo todos os dias | útil/fds | 7 dias).

## LLM e rede

LLM só no server: gpt-6-luna, reasoning.effort=none.
Client leva só API_PUBLIC_URL + INVITE_CODE. Header `X-Invite`.
Zero chave OpenAI no APK.
Server lê `.env`. Nunca printar OPENAI_API_KEY. Nunca commitar `.env`.

## Stack viva

- `apps/android/` — Kotlin, Jetpack Compose, Material 3 Expressive.
  Theme: `MaterialExpressiveTheme` + `MotionScheme.expressive()`.
  material3: 1.5.0-alpha29 ou alpha 1.5 mais nova. BOM Compose estável + override avulso. Sem `compose-bom-alpha` no projeto inteiro. Sem dynamic color / wallpaper.
- Arquitetura oficial Android:
  https://developer.android.com/topic/architecture
  https://developer.android.com/topic/architecture/recommendations
  Camadas: `ui` / `domain` / `data`.
  UDF. ViewModel de tela + `uiState: StateFlow`. `collectAsStateWithLifecycle`.
  Repository mesmo com uma fonte. Hilt. Sem `AndroidViewModel`. Sem Activity como source of truth.
  Domain puro para as fórmulas (sem Android).
- `server/` FastAPI intacto. Torre + Cloudflare Tunnel. Sem VPS agora. Sem porta no roteador.

## Stack morta

Não reabrir. Não restaurar do git neste projeto.

- Flutter (era `legacy/flutter/`)
- React Native (era `apps/rn/`)

Histórico git guarda o código.

## Tokens

`docs/tokens.md` é lei.

bg #0b0d10 · panel #12151a · phone #0e1114 · surf #171b20 · surf2 #1e242b · line #2a3139 · text #f3f5f7 · muted #8b939c · dim #5c6570 · gold #e8b86d · good #7dda9a · bad #e07a6a

Saldo 34pt w590. Campo 28pt. CTA #f3f5f7 texto #111. Sheet radius 22 top. Card/chip 14. Barra 6px gold.
Sem roxo Material default. Sem Appbar / FAB / BottomNav de showcase.

## Como trabalhar

- 1 `/goal` = 1 pasta. Não editar `server/` num goal de client.
- UI DONE = `adb exec-out screencap` comparado ao wire. Sem screenshot, não é DONE.
- Toda decisão técnica ou de negócio vira ADR em `docs/decisions/`.
- Testar antes de marcar done. `.\gradlew.bat test` e `.\gradlew.bat :app:compileDebugKotlin`.
- Feature nova: debate A/B/C antes de spec/código.
- Cada ideia: o que o user faz em <15s + o que o sistema devolve + VALIDADO vs HIPÓTESE.
- Não inventar tela fora desta constituição.
- Não usar computer-use em painel de cloud/roteador.
- Windows: PowerShell, `gradlew.bat`, `adb`. Sem bashism.

## Skills

Repo: `.grok/skills/` (ou a pasta que o `npx skills add` já criou).
User: `%USERPROFILE%\.grok\skills\`.

Oficiais Google (já instaladas via npx): não recriar. Sem wear / tv / xr / Play.

Locais do produto (prefixo `dieta-bot-android-`):
- dieta-bot-android-decisao
- dieta-bot-android-feature
- dieta-bot-android-lembrar
- dieta-bot-android-qa
- dieta-bot-android-ui
- dieta-bot-android-visual

Não usar mais os nomes `nutri-*`.

## NÃO FAZER

Flutter, React Native, Firebase, Gemini, TDEE, cap de eat-back, Health/Xiaomi, chave no client, VPS, porta no roteador, iOS, tela fora de O1 O2 T0 T1 T2 T3, dynamic color.
