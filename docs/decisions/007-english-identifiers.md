# 007 — English source identifiers

Date: 2026-09-25

## Decision

Source identifiers in `apps/android/` and `server/` are English. User-facing copy stays pt-BR. Nothing was in production, so JSON keys the repo owns were renamed on both client and server in the same change set.

Intent extra `nutri_tela` kept as the QA capture key so existing `adb` seeds still work.

DataStore day JSON uses the new English property names. `ignoreUnknownKeys` is on; an old local day is dropped, not migrated.

## Glossary

| before | after |
|---|---|
| teto | ceiling |
| teto_base | baseCeiling |
| teto_efetivo | effectiveCeiling |
| credito | credit |
| orçamento_janela | windowBudget |
| comido | eaten |
| reserva_próximas | reservedUpcoming |
| eat-back | eatBack |
| janela | window |
| janta (id) | dinner |
| fds | weekend |
| útil | weekday |
| saldo | remaining |
| confiança | confidence |
| encaixe | fit |
| treino | workout |

Window ids: cafe→breakfast, lanche_manha→morningSnack, almoco→lunch, lanche_tarde→afternoonSnack, janta→dinner, ceia→eveningSnack. On-screen titles stay Café / Janta / ….

Confidence values: alto→high, medio→medium, baixa→low.

Ceiling modes: mesmo→same, util→weekdayWeekend, sete→seven.

Eat-back: parcial→partial, cem→full.

Fit modes: quero→want, tenho→have, ideia→idea.

## JSON keys renamed (before → after)

janela → window
confianca → confidence
pergunta → question
itens → items
nome → name
itens_disponiveis → available_items
orcamento → budget
prato → dish
cabe → fits
opcoes → options
porcoes → portions
quantidade → quantity
alimento → food
modificador → modifier

Kept: kcal, p, c, g, text, mode, local_time, image_b64, model, ok.

HTTP paths unchanged: `/health`, `/v1/estimate`, `/v1/fit`. Header `X-Invite` unchanged.

## File renames (git mv)

Janela.kt → Window.kt
FotoEscala.kt → PhotoScale.kt
DiaStore.kt → DayStore.kt
FotoCompressor.kt → PhotoCompressor.kt
RedeModule.kt → NetworkModule.kt
DiaViewModel.kt → DayViewModel.kt
RegrasExtraTest.kt → ExtraRulesTest.kt
RedeTest.kt → NetworkTest.kt
dieta-bot-android-decisao → dieta-bot-android-decision
dieta-bot-android-lembrar → dieta-bot-android-memory

Untracked then named: DiaUi.kt → DayUi.kt, CapturaTest.kt → CaptureTest.kt.

## Tests that passed

- `apps/android`: `.\gradlew.bat test` and `.\gradlew.bat :app:compileDebugKotlin`
- `server/`: `.venv\Scripts\python.exe -m unittest tests.test_api`

## Refused

Translating on-screen pt-BR copy. Changing formula math. Bumping libraries. Restoring Flutter/RN. Editing `wires/nutri-wires-expressive.html`.
