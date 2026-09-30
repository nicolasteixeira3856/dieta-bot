# Plano — A29 Chat v2: plano de refeição, avisos de memória e sugestão da rotina

- Estado: Concluído
- Aprovação manual: 30/09/2026 (dono: "Quero que você passe todos os planos que estão pendentes de validação manual para completo.")
- Aprovado: 30/09/2026 ("Aprovo o plano `docs/android/plans/a29-chat-v2-interface.md`. Implemente o plano aprovado.")
- Implementado: 30/09/2026
- Data: 30/09/2026
- Contexto proprietário: `android`
- Código afetado: `apps/android/` (`feature/chat/*`, `domain/BudgetCalculator.kt` (só leitura), `domain/SlotClock.kt` (só leitura), `core/memory` (leitura da rotina), testes e capturas)
- Pré-requisitos: **[ST6](../../../stitch/plans/completed/st6-chat-v2.md) em `stitch/plans/completed/`** (golds `chatR`, `chatM`, `chatS`). [A27](a27-chat-v2-texto-intencao.md) e [A28](a28-memoria-v2.md) concluídos. Aceita o [ADR-023](../../../produto/adrs/ADR-023-chat-v2-memoria-v2.md) (decisões 3, 7 e 8). **Último plano da série Chat v2.**

## Gate de autorização

Este plano é exclusivamente documental. A implementação só começa após aprovação explícita que identifique este arquivo:

> Aprovo o plano `docs/android/plans/a29-chat-v2-interface.md`. Implemente o plano aprovado.

**Primeiro passo da implementação:** confirmar `docs/stitch/plans/completed/st6-chat-v2.md` e os golds `chatR`, `chatM` e `chatS` em `docs/qa/stitch/{dark,light}/`. Se não, parar e avisar o dono.

Se a implementação revelar decisão não coberta, pare, atualize os artefatos e peça nova aprovação.

## Objetivo

Os três estados visuais novos do Chat, sobre os dados que o A27 e o A28 já guardam:

1. **Plano** (`chatR`): dia projetado calculado pelo app e botão **Registrar assim**.
2. **Memória** (`chatM`): selo `Memória atualizada` e chips de origem `Memória permanente` / `Memória dinâmica`.
3. **Rotina** (`chatS`): card `O de sempre no {slot}?` com **Registrar** e **Quase igual**.

## Fontes de verdade

- Golds `chatR`, `chatM`, `chatS` (dark e light) do [ST6](../../../stitch/plans/completed/st6-chat-v2.md).
- [ADR-023](../../../produto/adrs/ADR-023-chat-v2-memoria-v2.md), [ADR-017](../../../produto/adrs/ADR-017-registro-consolidado.md), [chat](../../../produto/specifications/chat.md).
- Tokens do `AGENTS.md`, skills `dieta-bot-android-ui`, `material3-expressive`, `dieta-bot-android-visual`.

## Escopo de implementação

### 1. Plano de refeição (`chatR`)

- Mensagem da IA com `intent = plan` e estimativa: a bolha mostra o `reply` como texto (sem o card "ENERGIA TOTAL", sem bolha de pergunta).
- Painel do dia projetado dentro da bolha, abaixo do texto, no estilo do gold:
  - `Dia: {comido} → {comido + kcal do plano} de {teto efetivo} kcal`, com números no formato pt-BR (`1.640`).
  - `P {p comido + p}/{alvo P} · C {…}/{alvo C} · G {…}/{alvo G}`, valores nas cores semânticas.
  - Teto efetivo e alvos pelo `BudgetCalculator` e pelo perfil **no momento da exibição** (o painel acompanha registros feitos depois). Acima do teto efetivo, a linha de kcal em `bad`.
- Botão **Registrar assim** no lugar do grupo Gravar/Trocar/Pular, só na última estimativa sem recibo (mesma regra das ações de hoje). Toque:
  - com `suggested_slot` do dia → o mesmo fluxo do Gravar (`record`), inclusive a confirmação de Substituir do ADR-017 e a aplicação da rotina pendente do A28;
  - sem slot → abre o Trocar (sheet `chatT`) sem seleção.
- Recibo igual ao do Gravar.

### 2. Avisos de memória (`chatM`)

- Linha de chips abaixo da bolha da IA, acima do horário, na ordem: `Memória atualizada` (se `memoryUpdated`), `Memória permanente` e/ou `Memória dinâmica` (de `memoryUsedKinds`). Sem nada, sem linha.
- Recibo com `memoryUpdated` (rotina aplicada no registro): o chip `Memória atualizada` abaixo do recibo, mesmo componente.
- Componente `MemoryChip`: 28 dp, raio 14, borda `line`, fundo `surf`; o ícone de check do `Memória atualizada` em gold, os outros em `muted`. Só informativo: sem toque.

### 3. Sugestão da rotina (`chatS`)

- Condição (avaliada ao abrir o Chat e a cada mudança do dia): hoje, slot da hora (`SlotClock`) sem registro e sem pulo, e uma **rotina forte** desse slot (`MemoryRules`, A28). Mais de uma → a com mais dias vistos.
- Card no fim da conversa (dia vazio: entre o card da meta e os chips de sugestão, como no gold), com: `O de sempre no {slot}?`, texto da rotina, `{kcal} kcal · {P}P · {C}C · {G}G`, chip de origem (`Memória permanente` ou `Memória dinâmica`) e os dois botões.
- **Registrar:** `addLog` com texto, kcal e P/C/G da rotina, `source` `routine`, no slot da rotina; recibo `Registrado em {slot} · {hora} +{kcal} kcal`; reforço da rotina (`FactMemory.reinforceRoutine`) e `Memória atualizada` no recibo. Sem POST.
- **Quase igual:** texto da rotina no composer (substitui o que estava), cursor no fim, foco e teclado aberto. Nada é gravado.
- O card some quando o usuário envia qualquer mensagem, quando o slot é gravado ou pulado, ou quando a hora passa para outro slot. Não entra em `chat_message` nem no prompt.
- Telemetria: `routine_suggestion` com `action` (`shown`, `record`, `edit`). Só enums.

### 4. Captura e comparação

- `tools/capture-chat.sh`: cenas novas `chatR`, `chatM`, `chatS` com o `tools/fake-chat-server.mjs` (respostas fixas com `intent: plan`, `memory_used`, `memory_updates`) e memória semeada pelo editor dev (rotina de café com 5 dias).
- Capturas em `docs/qa/android/current/{dark,light}/`, `diff-gold.mjs` e `StitchGoldTest`, diff escrito (layout, tokens, tamanho de texto, raio, CTA, cores semânticas) e iteração até bater.
- Roborazzi: baselines novas para os três estados; as antigas do Chat sem mudança.

### 5. Testes

- `ChatViewModelTest`: Registrar assim com e sem slot; plano num slot já gravado → confirmação de Substituir; painel projetado muda depois de um registro; sugestão aparece e some em cada condição; Registrar da rotina grava os números da rotina e reforça; Quase igual preenche o composer sem gravar.
- `ProjectedDayTest` (domain): soma, teto efetivo com eat-back 0/%/100, acima do teto.
- Compose UI test: chips na ordem certa; sem linha quando não há memória.

### 6. Specs

- [chat](../../../produto/specifications/chat.md): regras novas do plano, avisos de memória e sugestão da rotina; lista de estados com `chatR`, `chatM`, `chatS`.
- [memoria-push](../../../produto/specifications/memoria-push.md): o que o usuário vê da memória (selo e chips).

## Arquivos e áreas afetadas

- `feature/chat/ChatScreen.kt`, `ChatUiState.kt`, `ChatViewModel.kt`, componentes novos (`PlanPanel`, `MemoryChip`, `RoutineSuggestionCard`).
- `domain/ProjectedDay.kt` (novo).
- `tools/capture-chat.sh`, `tools/fake-chat-server.mjs`.
- Testes e baselines Roborazzi; capturas em `docs/qa/android/current/`.
- Docs: specs acima, este plano, índices.

## Validação planejada

1. `:app:testDevDebugUnitTest`, `:app:verifyRoborazziDevDebug` verdes.
2. Capturas `chatR`, `chatM`, `chatS` dark e light batendo com os golds (regra de UI DONE do `AGENTS.md`).
3. `:app:assembleDevRelease`, instalado por cima do build do A28.
4. Emulador com o server de dev: pizza "vou fazer" → painel + Registrar assim → recibo na Janta; "sempre uso leite semidesnatado" → `Memória atualizada`; café seguinte → `Memória permanente`; 3 dias de café → card da rotina de manhã; Registrar e Quase igual.
5. Manual (dono): uso real por alguns dias no APK distribuído.

## Fora de escopo

- Tela de memória para o usuário final (ADR-023: só dev).
- Tocar num chip para ver ou editar a memória.
- Sugestão de rotina por push.
- Server e regras de memória: [S11](../../../server/plans/completed/s11-chat-v2.md), [A28](a28-memoria-v2.md).

## Riscos e controles

- **Painel diferente da Home:** mesma fonte (`BudgetCalculator` e Room) e teste com eat-back.
- **Sugestão insistente:** só com slot vazio, só rotina forte, some no primeiro envio. Se incomodar no uso real, o dono decide um "não sugerir hoje" num plano próximo.
- **Registrar da rotina com números velhos:** usa o último registro da rotina; "Quase igual" existe para o dia diferente.

## Critérios de aceite

- Plano mostra o dia projetado certo e registra em um toque.
- O usuário vê quando a memória mudou e de qual memória veio a resposta.
- Café de sempre em um toque, ou editado a partir da rotina.
- Capturas batem com `chatR`, `chatM` e `chatS` nos dois temas.

## Encerramento

Depois da implementação, registre resultados reais e aplique o ciclo de vida em `docs/sdd/README.md`, incluindo a entrega git (§ 6).

Só declaração explícita do dono cancelando este plano permite `Cancelado` e a pasta `plans/cancelled/`.

## Resultados da implementação (30/09/2026)

Pré-requisito conferido antes do código: `docs/stitch/plans/completed/st6-chat-v2.md` e os golds `chatR`, `chatM`, `chatS` em `docs/qa/stitch/{dark,light}/`.

### O que mudou

- `domain/ProjectedDay.kt` (novo): `Macros` e `ProjectedDay.of(budget, eaten, plan, targets)`; teto efetivo pelo `BudgetCalculator` (eat-back 0 / % / 100, sem teto de crédito); `over` quando passa do teto. `core/database/DayBudget.kt` ganhou `DaySnapshot.budgetOn(date)` (o `metaOn` usa a mesma entrada; comportamento igual).
- `ChatViewModel`:
  - Plano (`intent = plan` com estimativa) de hoje leva `ProjectedDay` recalculado a cada render (acompanha registros novos). Regra das ações única para `log` e `plan`: só a **última** estimativa sem recibo tem barra; se é plano, `EstimateActions.plan` → **Registrar assim**. `recordPlan` usa o `record` do Gravar (Substituir do ADR-017 e rotina pendente do A28 incluídos); sem slot → Trocar sem seleção.
  - Avisos: `MemoryNotice` (atualizada / permanente / dinâmica) na bolha da IA a partir de `memoryUpdated` e `memoryUsedKinds`; `Receipt.memoryUpdated`.
  - Rotina: fatos lidos do `FactMemory` ao abrir e depois de cada mudança; tick por minuto para a troca de slot. Card quando o slot da hora não tem registro nem pulo e há rotina forte (`MemoryRules.isStrongRoutine`) desse slot; várias → mais dias vistos. `recordRoutine` (`addLog` com os números da rotina, `source = routine`, `reinforceRoutine`, recibo com `Memória atualizada`, sem POST); `editRoutine` (texto no composer, `focusComposer`). Some no primeiro envio da tela, com o slot gravado ou pulado, ou com a hora em outro slot. Telemetria `routine_suggestion` (`shown` uma vez por card, `record`, `edit`) e `meal_saved` com `from = routine`.
- UI: `ChatV2Components.kt` com `PlanText`, `PlanPanel`, `MemoryChip`/`MemoryChips`, `RoutineSuggestionCard`; `PlanBar` no lugar da barra de três botões. O composer passou a `TextFieldValue`: texto vindo de fora (chip, Quase igual) entra com o cursor no fim; foco e teclado pelo `focusComposer`.
- `tools/fake-chat-server.mjs`: modos `plan` e `routine`; `/__calls` devolve os `facts` recebidos. `tools/capture-chat.sh`: cenas A29 (`SCENES=v2` roda só onboarding + A29). `tools/diff-gold.mjs`: `chatS` compara só o card (cópia do `chat0`, outro cabeçalho).

### Decisões de implementação (dentro do plano)

- **Chips em coluna**, um por linha, 8 dp entre eles e 5 dp abaixo da bolha: é o que os golds `chatM` desenham (dark e light), embora caibam dois por linha.
- **Plano registrado conta uma vez.** Achado no teste com o server de dev: depois do Registrar assim, o painel somava o plano sobre o próprio registro ("335 → 670"). Um plano cujo primeiro recibo seguinte (antes de outra estimativa) é Registrado/Atualizado projeta a partir de `comido − plano` ("0 → 335").
- **Plano de outro dia** mostra só o texto (não há dia para projetar).
- **Texto do plano** em linhas: lista com marcador um pouco afastada dos parágrafos; `40P · 38C · 12G` e `P 17 g` (como o modelo escreve) nas cores dos macros.
- **Métricas por tema do `chatR`**: os golds de cada tema medem diferente (dark: linha 23 dp, painel 13,5/12 sp sobre `phone`; light: linha 22 dp, painel 15/13 sp sobre `panel`). O código segue cada gold.
- **Respiro final do fio**: 8 → 2 dp. No `chatR` o conteúdo passa da altura e a rolagem automática subia tudo 4 dp; o gold tem 10 dp entre o último horário e a barra (2 + 8 do rodapé). Sem mudança nas telas que cabem (baselines antigas iguais).
- Selo do recibo centralizado sob o recibo (o recibo é centralizado).

### Validação automatizada

- `:app:testDevDebugUnitTest`: **339 testes, 0 falhas**. Novos: `ProjectedDayTest` (6: soma do gold, eat-back 0 / 50% / 100%, treino ausente, acima do teto), `ChatMemoryChipsTest` (4: ordem atualizada → permanente → dinâmica abaixo da bolha, só o tipo usado, sem memória sem linha, selo sob o recibo), `ChatViewModelTest` (Registrar assim com slot, sem slot → Trocar vazio, slot ocupado → Substituir, painel acompanha registro posterior e fica `bad` acima do teto, plano registrado conta uma vez, avisos na bolha e no recibo, card aparece por último e reporta `shown` uma vez, fraca ou de outro slot sem card, mais dias vence, some no envio / pulo / registro / troca de hora, Registrar grava os números e reforça sem POST, Quase igual preenche o composer sem gravar). Reescritos do A27: plano agora tem Registrar assim; plano depois de log aberto fica com a barra.
- `StitchGoldTest` (JVM): `chatR` área da bolha do plano + barra 1,37% dark / 1,96% light (tela inteira só informada: 2,21% / 2,15%; o cabeçalho, a bolha do usuário e o microfone do composer são da geração do `chatE`); `chatM` tela inteira 1,92% / 1,82%; `chatS` card 0,78% / 1,14%. `chatX` segue em 0,82% (o cursor inicial do composer continua no começo).
- `:app:verifyRoborazziDevDebug`: verde. Baselines novas `chatR`, `chatM`, `chatS` (dark e light); as antigas do Chat sem mudança.
- `:app:assembleDevRelease`: verde.

### Validação no emulador

Capturas em `docs/qa/android/current/{dark,light}/chat{R,M,S}.png` (AVD 780×1688, densidade 320, `SCENES=v2 tools/capture-chat.sh` com o fake). `node tools/diff-gold.mjs`:

| Tela | Dark | Light |
| --- | --- | --- |
| `chatR` (tela inteira) | 1,68% ✓ | 1,92% ✓ |
| `chatM` (tela inteira) | 1,75% ✓ | 1,57% ✓ |
| `chatS` (card) | 0,84% ✓ | 1,16% ✓ |

Diff escrito. Restante, ignorável pela regra do AGENTS: relógio e ícones da barra de status; raster da fonte; seta `→` do painel com outro desenho; ícone de "dinâmica" (`Sync` do Material; o gold usa um laço parecido); no `chatS` o cabeçalho, a data e os números do cartão da meta vêm da geração do `chat0` (só o card é comparado). Iguais ao gold: raio 14 do painel, dos chips e do card; chips de 28 dp; CTA `Registrar` em `ctaBg`/`ctaText`; `Quase igual` em `surf2` com borda `line`; gold só no check do `Memória atualizada`; P/C/G nas cores semânticas; barra `Registrar assim` com a altura e o raio da barra de três botões.

Fluxo no emulador (fake, dark e light, todos ✓): 3 cafés em 3 dias passados (relógio do aparelho por `cmd alarm set-time`, sem root) → sem card antes de 3 dias; `Memória atualizada` na resposta com a preferência nova e nos recibos com a rotina; chips de origem no 3º dia; hoje 08:10 → card `O de sempre no Café da manhã?` com `Memória dinâmica`; Quase igual → composer com o texto e teclado aberto; Registrar → recibo com `Memória atualizada`, 0 POST, card some, `meal_log` `Café da manhã, 440, 25, 38, 22, routine`. Plano "vou fazer pizza" → painel + só Registrar assim → recibo na Janta, `meal_log` `Jantar, 420`.

`:app:assembleDevRelease` instalado **por cima** do APK do A28 (release de 30/09 16:54), server de dev:

1. "vou fazer uma pizza de pão sírio na janta. Quantas gramas de cada item?" → `plan`: gramas no texto, painel `Dia: 0 → 335 de 2.000 kcal`, só Registrar assim; toque → `Registrado em Jantar +335 kcal`. Aqui apareceu a contagem dupla, corrigida e conferida no mesmo aparelho ("0 → 335" com o recibo).
2. "lembra que eu sempre uso leite semidesnatado" → resposta com `Memória atualizada`.
3. "tomei um café com leite e um pão francês com manteiga" → estimativa com `Memória atualizada` e `Memória permanente` (o leite veio da memória); a pergunta de quantidade na bolha própria.
4. Card da rotina com o server real não exercido: precisa de 3 dias de café gravado com rotina proposta pelo modelo. Coberto pelo fluxo do fake acima.

### Pendente (dono)

- Uso real por alguns dias no APK distribuído (validação 5): plano "vou fazer…" com Registrar assim, chips aparecendo quando a memória é usada, e o card do café depois de 3 dias gravando o mesmo café.
