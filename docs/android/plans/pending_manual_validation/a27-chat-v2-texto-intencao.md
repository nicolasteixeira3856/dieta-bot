# Plano — A27 Chat v2: texto da refeição, intenção e histórico de 7 dias

- Estado: Pendente aprovação manual
- Data: 30/09/2026
- Contexto proprietário: `android`
- Código afetado: `apps/android/` (`core/network/ChatModels.kt`, `core/database` — Room v4 → v5, `feature/chat/ChatViewModel.kt`, `feature/chat/PromptBuilder.kt`, `feature/chat/ChatUiState.kt`)
- Pré-requisitos: [S11](../../../server/plans/pending_manual_validation/s11-chat-v2.md) no ar no server de dev. Aceita o [ADR-023](../../../produto/adrs/ADR-023-chat-v2-memoria-v2.md) (decisões 1, 2 e 5, parte client). **Sem gate Stitch**: nenhuma tela ou estado visual novo.

## Gate de autorização

Este plano é exclusivamente documental. A implementação só começa após aprovação explícita que identifique este arquivo:

> Aprovo o plano `docs/android/plans/a27-chat-v2-texto-intencao.md`. Implemente o plano aprovado.

**Primeiro passo da implementação:** confirmar que o S11 está no ar (`POST /v1/chat` do dev devolve `intent` e `estimate.meal_text`). Se não, parar e avisar o dono.

Se a implementação revelar decisão não coberta, pare, atualize os artefatos e peça nova aprovação.

## Objetivo

Corrigir as duas falhas do Chat que não dependem de interface nova:

1. A Home grava a refeição (`meal_text`), nunca a resposta a uma pergunta ("Sempre uso leite semi desnatado").
2. Um plano ("vou fazer pizza, quantas gramas?") não mostra Gravar/Trocar/Pular.

E mandar ao server o histórico dos últimos 7 dias e o que sobra no dia, para o "mesmo de ontem" deixar de depender da memória.

## Fontes de verdade

- [ADR-023](../../../produto/adrs/ADR-023-chat-v2-memoria-v2.md), [ADR-017](../../../produto/adrs/ADR-017-registro-consolidado.md).
- [chat](../../../produto/specifications/chat.md), [api-contract.md](../../../api-contract.md) (depois do S11), [room-v2](../../specifications/room-v2.md).

## Escopo de implementação

### 1. Contrato (`ChatModels.kt`)

- `ChatOut.intent: String? = null` (`log` | `plan` | `question`; null = server antigo, tratado como hoje).
- `ChatEstimate.mealText: String? = null` (`meal_text`).
- `ChatIn.recent: List<ChatRecentMeal> = emptyList()` e `ChatDay.remainingKcal: Int? = null` (`remaining_kcal`).
- `ChatRecentMeal(date, slotId: String?, slotName, text, kcal, p, c, g)`.
- **Não** manda `facts` ainda: para o server, este client continua "legado" na memória (texto `memory` como hoje) até o [A28](a28-memoria-v2.md). Consequência aceita: até o A28, plano chega com `estimate: null` (gramas e total no texto).

### 2. Room v4 → v5 (`chat_message`)

- `estimateMealText TEXT` (nullable) e `intent TEXT` (nullable).
- `MIGRATION_4_5` só com `ALTER TABLE … ADD COLUMN`. Linhas antigas ficam null e continuam funcionando como hoje.
- Schema exportado (`schemas/…/5.json`) e `room-v2.md` atualizados.

### 3. Texto gravado (`ChatViewModel.descriptionOf`)

Ordem:

1. `estimateMealText` da estimativa, quando não vazio.
2. Sem `meal_text` (server antigo ou falha): a **primeira** mensagem do usuário da cadeia daquela estimativa, pulando as respostas a perguntas. Uma mensagem do usuário é resposta quando a mensagem da IA logo antes tinha `estimateQuestion`. Mensagem só com foto → `estimate.text` (regra da foto de hoje).
3. Nada disso → nomes dos itens da estimativa separados por vírgula.

Vale para Gravar, Confirmar do Trocar e Substituir. A linha de memória do A8 usa o mesmo texto (até o A28 trocar a memória).

### 4. Intenção

- `intent` gravado na mensagem da IA.
- Ações (Gravar/Trocar/Pular) só para estimativa com `intent` `log` ou null. `plan` com estimativa: a estimativa fica guardada (para o "Registrar assim" do [A29](../a29-chat-v2-interface.md)), mas a bolha aparece sem barra de ações e sem card. `question`: só a bolha.
- A pergunta de esclarecimento (bolha de pergunta do A19) só aparece em `log`.
- Telemetria `chat_result`: novo parâmetro `intent` (enum).

### 5. Histórico e saldo (`PromptBuilder`)

- `recent`: registros de `meal_log` dos 7 dias antes de hoje (America/Sao_Paulo), mais antigo primeiro, dentro do dia na ordem do horário do slot. Nome do slot pelo `meal_slot`; slot apagado ou null → `slotId` null, nome `Outros`. `text` cortado em 240. No máximo 42 itens (os mais recentes ficam).
- Consulta nova no `MealLogDao`: `getBetween(from, to)`.
- `day.remaining_kcal` = teto efetivo do dia (`BudgetCalculator`: teto base + crédito de treino) − `eaten_kcal`. Pode ser negativo.

### 6. Testes

- `ChatViewModelTest`: café + pergunta do leite + resposta → Gravar grava o `meal_text`; sem `meal_text`, grava a primeira mensagem da cadeia (não a resposta); só foto → texto da IA.
- `intent: plan` com estimativa → sem ações; `log` e null → ações como hoje.
- `PromptBuilderTest`: `recent` com 7 dias, sem hoje, ordem, `Outros`, corte em 240 e em 42; `remaining_kcal` com e sem treino (eat-back 0/%/100).
- `MigrationTest` 4 → 5 com um banco v4 com mensagens e registros: nada perdido.
- Roborazzi: capturas atuais do Chat sem mudança (`verifyRoborazziDevDebug`).

## Arquivos e áreas afetadas

- `core/network/ChatModels.kt`.
- `core/database/ChatMessageEntity.kt`, `DietaBotDatabase.kt` (v5), `Migrations.kt`, `MealLogDao.kt`, `DayRepository.kt`, schema exportado.
- `feature/chat/ChatViewModel.kt`, `PromptBuilder.kt`, `ChatUiState.kt`, `ChatScreen.kt` (só esconder ações em `plan`).
- `core/telemetry/TelemetryEvents` (parâmetro `intent`).
- Testes em `app/src/test*`.
- Docs: `docs/android/specifications/room-v2.md`, `docs/produto/specifications/chat.md` (regras 4, 5, 8 e 12), este plano.

## Validação planejada

1. `./gradlew.bat :app:testDevDebugUnitTest` verde.
2. `./gradlew.bat :app:verifyRoborazziDevDebug` verde (nenhuma tela mudou).
3. `./gradlew.bat :app:assembleDevRelease`.
4. Emulador com o server de dev, build instalado **por cima** do 0.0.3 com dados: migração sem perda; café "igual ao de ontem" → pergunta → resposta → Gravar → a Home mostra a refeição; "vou fazer pizza de pão sírio, quantas gramas?" → só a bolha.
5. Manual (dono): repetir os dois fluxos no APK distribuído.

## Fora de escopo

- Memória v2, `facts`, selo e chips: [A28](a28-memoria-v2.md), [A29](../a29-chat-v2-interface.md).
- Card do plano, dia projetado e "Registrar assim": [A29](../a29-chat-v2-interface.md).
- Server: [S11](../../../server/plans/pending_manual_validation/s11-chat-v2.md).

## Riscos e controles

- **Migração quebrar o app dos testers:** só `ADD COLUMN`, teste com banco v4, instalação por cima antes de distribuir.
- **Server sem `meal_text` (antes do deploy ou falha):** o fallback da seção 3 já corrige o caso do leite sem o server.
- **Prompt maior com `recent`:** até 42 × 240 caracteres. Aceito no ADR-023.

## Critérios de aceite

- Texto da Home nunca é a resposta a uma pergunta.
- Plano não mostra Gravar/Trocar/Pular.
- "Café igual ao de ontem" funciona com a memória antiga apagada (o histórico vem do `recent`).
- Room v5 migra sem perda.

## Resultados (30/09/2026)

### Entregue

- **Contrato** (`ChatModels.kt`): `ChatOut.intent`, `ChatEstimate.mealText` (`meal_text`), `ChatIn.recent` (`ChatRecentMeal`), `ChatDay.remainingKcal` (`remaining_kcal`). Sem `facts`: para o server o client continua legado na memória.
- **Room v5**: `chat_message.estimateMealText` e `chat_message.intent` (nullable); `MIGRATION_4_5` só com dois `ADD COLUMN`; `schemas/…/5.json` exportado; `MealLogDao.getBetween(from, to)` e `DayRepository.recentLogs()`.
- **Texto gravado** (`ChatViewModel.descriptionOf`): `meal_text` → primeira mensagem do usuário da cadeia (volta enquanto a mensagem é resposta a uma estimativa com pergunta; recibo ou wipe no meio encerra a cadeia; foto → texto da IA) → nomes dos itens. Vale para Gravar, Confirmar do Trocar, Substituir e a linha de memória do A8.
- **Intenção**: gravada na mensagem da IA (só `log`/`plan`/`question`; outro valor vira null). Card, bolha de pergunta, barra de ações, `record`/`confirmReplace` e a resposta à pergunta (linha "Respondeu …" da memória) só para estimativa `log` ou null. `plan` guarda a estimativa em Room, mostra só a bolha. Um `plan` depois de um `log` aberto mantém as ações do `log`. `ChatScreen.kt` sem mudança: já esconde card e barra quando o estado não os traz.
- **Telemetria** `chat_result`: `intent` (`log` | `plan` | `question` | `none`) nos resultados `ok`.
- **Prompt** (`PromptBuilder`): `recent` com os 7 dias antes de hoje, mais antigo primeiro, no dia pela hora do slot (`Outros` por último), `slot_id` null e `Outros` para slot nulo ou apagado, texto cortado em 240 code points, no máximo 42 (ficam os mais recentes); `day.remaining_kcal` = `metaOn(hoje)` (teto base + crédito) − comido, pode ser negativo.
- **Docs**: [room-v2](../../specifications/room-v2.md) (v5), [chat](../../../produto/specifications/chat.md) regras 4, 5, 8, 12 e 14 vigentes.

### Validação executada

1. Primeiro passo: `POST /v1/chat` no server de dev (`X-Request-Id: a27-check-1`, corpo sem `facts`, com `recent` e `remaining_kcal`) → 200, `intent: log`, `estimate.meal_text` "2 ovos mexidos, 1 pão francês c/ manteiga, 200 ml leite" vindo do `recent`. S11 no ar.
2. `./gradlew.bat :app:testDevDebugUnitTest`: 274 testes, 0 falhas. Novos: `ChatViewModelTest` (meal_text depois da resposta do leite, primeira mensagem da cadeia sem meal_text, server antigo com meal_text vazio, só foto → texto da IA, `plan` sem card/ações/pergunta e `record` recusado, `plan` depois de `log` aberto, `question` só bolha, `recent` e `remaining_kcal` no POST, `intent` na telemetria), `PromptBuilderTest` (7 dias sem hoje, ordem, `Outros`, corte em 240 e em 42, `remaining_kcal` com eat-back 0/%/100 e negativo), `MigrationV4V5Test` (banco v4 com mensagens, estimativa, recibo, registro e digest: nada perdido, colunas novas null). `MigrationV1V2Test` e `RoomV2Test` passam a esperar v5.
3. `./gradlew.bat :app:verifyRoborazziDevDebug`: verde, nenhuma captura mudou.
4. `./gradlew.bat :app:assembleDevRelease`: APK gerado.
5. Emulador (Medium_Phone, server de dev): APK 0.0.3 gerado da tag `dev-v0.0.3`, onboarding, "almocei 2 ovos mexidos e 1 pao frances" → pergunta da manteiga → "com manteiga" (estimativa aberta, sem Gravar). Build A27 instalado **por cima**: Home e Chat intactos (migração v4 → v5 sem perda). Gravar a estimativa antiga (linha v4, sem `meal_text`) → a Home mostra "almocei 2 ovos mexidos e 1 pao frances", não "com manteiga" (fallback da seção 3). "cafe igual ao de ontem" → o server pergunta o que foi (sem registro de ontem no emulador) → "2 ovos mexidos, 1 pao frances e 200 ml de leite" → estimativa com pergunta do leite → "Sempre uso leite semi desnatado" (a IA respondeu sem estimativa) → Trocar → Cafe → a Home mostra "2 ovos mexidos com manteiga, 1 pão francês, 200 ml de leite" (`meal_text`). "vou fazer pizza de pao sirio, quantas gramas?" → só a bolha com gramas e total, sem card e sem Gravar/Trocar/Pular.

### Observações (fora do escopo, não corrigidas aqui)

- No emulador o slot "Cafe" ficou às 20:00 (o seletor de horário não respondeu ao `adb swipe`), e o modelo associou o café ao Almoço. Comportamento do server/modelo com um perfil atípico; não afeta o client.
- "Café igual ao de ontem" com histórico real não foi exercitado no emulador (sem registro do dia anterior); está coberto pela request do passo 1 e pelos testes do `recent`.

### Pendente (manual, dono)

- Validação 5: repetir os dois fluxos (café → pergunta → resposta → Gravar; pizza "vou fazer") no APK distribuído, de preferência com um café registrado no dia anterior. Depois disso: `Concluído` e `completed/`.

## Encerramento

Depois da implementação, registre resultados reais e aplique o ciclo de vida em `docs/sdd/README.md`, incluindo a entrega git (§ 6).

Só declaração explícita do dono cancelando este plano permite `Cancelado` e a pasta `plans/cancelled/`.
