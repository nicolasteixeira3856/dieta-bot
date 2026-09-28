# Plano — A12 Remover o legado T1/T2/T3

- Estado: Concluído
- Data: 28/09/2026
- Contexto proprietário: `android`
- Código afetado: `apps/android/` (remoção de código morto e dos testes dele), docs. Nenhuma tela viva muda.
- Pré-requisitos: Nenhum. [A5](a5-chat.md) concluído.

## Gate de autorização

Este plano é exclusivamente documental. A implementação só começa após aprovação explícita que identifique este arquivo:

> Aprovo o plano `docs/android/plans/a12-remover-legado-t123.md`. Implemente o plano aprovado.

Se a implementação revelar decisão não coberta, pare, atualize os artefatos e peça nova aprovação.

## Objetivo

Apagar o fluxo antigo de registro (sheet T1, rota cheia T2, sugestões T3 e o Home com composer), substituído pelo Chat no A5. O [ADR-012](../../../produto/adrs/ADR-012-chat-home-perfil.md) já mandava: "T2 e T3 saem quando A5 estiver DONE". Isso não tinha sido feito.

## Diagnóstico (28/09/2026)

- Nenhum botão leva ao T2. `RouteT2` só abre com o extra de captura `nutri_tela=t2|t2q`, e a lista de telas do `AGENTS.md` não tem T2.
- Os arquivos abaixo só são usados entre si e pelos próprios testes. O Home vivo é `HomePanel*`, e o Chat usa só `/v1/chat`.

## Escopo de implementação

### 1. Apagar (código morto)

| Arquivo | Era |
|---|---|
| `feature/t2/T2Screen.kt`, `T2UiState.kt`, `T2ViewModel.kt` | T2, confirmação da estimativa |
| `ui/DayViewModel.kt`, `ui/DayUi.kt` (`Stage`, `captureState`) | estado do fluxo T0–T3 |
| `feature/home/HomeViewModel.kt`, `HomeScreen.kt`, `HomeSheets.kt`, `HomeUiState.kt` | Home antigo com composer, T1/T3 |
| `core/network/EstimateGate.kt`, `core/network/PhotoCompressor.kt` | chamadas a `/v1/estimate` e `/v1/fit`; foto do fluxo antigo (o A6 usa `PhotoStore`) |
| `domain/FitGuard.kt`, `domain/ChipRule.kt`, `domain/PhotoScale.kt`, `domain/Window.kt` | regras do fluxo antigo |

### 2. Ajustar (código vivo)

- `MainActivity.kt`: sai `RouteT2`, o `composable<RouteT2>`, os imports e os ramos `"t2", "t2q"` do extra de captura e de `screenName`.
- `core/network/NetworkModule.kt`: sai o provider `gate()`.
- `core/network/ApiModels.kt`: saem `estimate()`/`fit()` do `NutriApi` e os tipos usados só por eles (`EstimateIn/Out`, `FitIn/Out`, `BudgetIn`, `AssumptionIn`, `Dish`, `Portion`…). Ficam os tipos que o Chat e a memória usam (`ItemOut`, `Assumption`…). O compilador decide o que fica.
- `server/` não muda: `/v1/estimate` e `/v1/fit` continuam no contrato. Removê-los do server, se for o caso, é outro plano (S*).

### 3. Testes

- Apagar os testes que cobrem só código removido (`ui/DayActionsTest`, `feature/home/HomeViewModelTest`, `ui/CaptureTest` se for só T0–T3).
- Aparar os mistos (`domain/ExtraRulesTest`, `core/network/NetworkTest`): saem só os casos de `FitGuard`/`chipForWindow`/`PhotoScale`/`Window`/`EstimateGate`, e o resto fica.
- Registrar a contagem antes e depois.

### 4. Scripts, QA e docs

- `tools/` e `docs/qa/`: remover qualquer passo ou captura `t2`/`t2q`/`t3*` que ainda exista (capturas antigas vão para `docs/qa/_legacy/`, sem apagar).
- `docs/android/README.md` ("Estado atual": T1/T3 sheet, T2 rota cheia) e a matriz `docs/README.md` (ADR-011 marcado como encerrado pelo A12). ADR-011 e ADR-012 não se editam (imutáveis).
- Specs em `docs/produto/specifications/` que ainda citem T1/T2/T3 como vivos: ajustar.

## Validação planejada

1. `git grep -n -E "RouteT2|T2Screen|T2ViewModel|DayViewModel|HomeViewModel\b|EstimateGate|FitGuard|chipForWindow|PhotoCompressor|Stage\.T2"` em `apps/android/app/src` → zero.
2. `:app:testDevDebugUnitTest` verde (a contagem cai só pelos testes removidos); `verifyRoborazziDevDebug` verde.
3. `:app:assembleDevRelease` ok. O APK fica menor, e o tamanho é registrado.
4. Emulador + fake: `capture-chat.sh light` 21 ✓ / 0 ✗, `capture-home.sh light` sem ✗, `diff-gold` splash/o1–o4 ✓ e pixel diff das capturas contra as atuais (só relógio/data).

## Fora de escopo

- Qualquer tela nova ou mudança de UI.
- Rename Nutri → Dieta Bot ([A13](../a13-rename-dieta-bot.md), que vem depois deste).
- Remover `/v1/estimate` e `/v1/fit` do server.
- Migração de banco (nenhuma tabela muda).

## Riscos e controles

- **Apagar algo vivo:** cada arquivo só sai se o compilador aceitar sem ele. Se algo quebrar, o arquivo volta e fica registrado aqui.
- **Import do DataStore legado (A1):** `DayRepository`/`DAY_STORE_NAME` continuam; só sai a UI antiga.

## Critérios de aceite

- Nenhuma referência ao fluxo T0–T3 no código do app.
- Testes, Roborazzi e captura sem regressão.
- Docs sem descrever T1/T2/T3 como vivos.

## Registro de execução

- 28/09/2026 — aprovado pelo dono junto com A13, S7 e A14, para execução em sequência num único goal.
- Removidos (`git rm`): `feature/t2/*` (3), `ui/DayViewModel.kt`, `ui/DayUi.kt`, `feature/home/Home{ViewModel,Screen,Sheets,UiState}.kt`, `core/network/EstimateGate.kt`, `core/network/PhotoCompressor.kt`, `domain/{FitGuard,ChipRule,PhotoScale,Window}.kt`.
- Testes removidos (só cobriam o legado): `ui/DayActionsTest`, `ui/CaptureTest`, `feature/home/HomeViewModelTest`, `domain/ExtraRulesTest` (chip, PhotoScale 1280/70, FitGuard). `core/network/NetworkTest`: saíram os 2 casos de `EstimateGate`.
- `ApiModels.kt`: `NutriApi` ficou com `health()` e `chat()`; saíram `EstimateIn/Out`, `BudgetIn`, `FitIn/Out`, `PortionOut`, `DishOut`. `NetworkModule`: saiu o `gate()`. `MainActivity`: saíram `RouteT2`, o composable, os imports e os ramos `t2`/`t2q`.
- Docs: `docs/produto/README.md`, `docs/android/README.md`, `docs/README.md` (ADR-011 = histórico), `specifications/memoria-push.md`. `tools/check-wires.mjs`/`export-wires.mjs` ficam: listam os wires antigos de `wires/` (referência histórica, não telas).
- **Achado:** `AGENTS.md` ainda diz "Client: ≤1280 JPEG 70", mas o fluxo vivo (A6, `PhotoGate`/`PhotoStore`) manda JPEG sem downscale, q90 só em reencode, ≤ 16 MB, pela spec de foto. É regra de produto: o texto do `AGENTS.md` fica para o dono decidir.
- **Achado de QA (vindo do A10):** depois do onboarding, o Home já abre o diálogo de notificação; o `pm grant` sozinho não fecha o diálogo aberto. `capture-home`, `capture-config` e `capture-photo` agora reiniciam o app depois do grant. O `capture-chat` não mudou: ele já reinicia, e um reinício a mais desalinhou o fluxo dele (testado e revertido).

### Resultados da validação

1. `git grep` por `RouteT2|T2Screen|T2ViewModel|DayViewModel|HomeViewModel|EstimateGate|FitGuard|chipForWindow|PhotoCompressor|Stage.T2|feature.t2` em `apps/android/app/src` → zero. ✅
2. `testDevDebugUnitTest` → **171** testes, 0 falhas (195 − 24 do legado); `verifyRoborazziDevDebug` verde. ✅
3. `assembleDevRelease` ok. APK 18.477.424 → **18.395.504** bytes (−80 KB). ✅
4. Emulador `Medium_Phone` + fake, light: `capture-chat` 21 ✓/0 ✗, `capture-home` 8/0, `capture-config` 23/0, `capture-photo` 10/0; `diff-gold` splash/o1–o4 ✓. Pixel diff contra as capturas anteriores: home 0,25% (relógio/data), chat ≤ 0,08%. ✅

## Encerramento

Depois da implementação, registre resultados reais e aplique o ciclo de vida em `docs/sdd/README.md`.

Só declaração explícita do dono cancelando este plano permite `Cancelado` e a pasta `plans/cancelled/`.
