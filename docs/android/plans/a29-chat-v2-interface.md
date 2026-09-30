# Plano — A29 Chat v2: plano de refeição, avisos de memória e sugestão da rotina

- Estado: Aguardando aprovação
- Data: 30/09/2026
- Contexto proprietário: `android`
- Código afetado: `apps/android/` (`feature/chat/*`, `domain/BudgetCalculator.kt` (só leitura), `domain/SlotClock.kt` (só leitura), `core/memory` (leitura da rotina), testes e capturas)
- Pré-requisitos: **[ST6](../../stitch/plans/st6-chat-v2.md) em `stitch/plans/completed/`** (golds `chatR`, `chatM`, `chatS`). [A27](a27-chat-v2-texto-intencao.md) e [A28](a28-memoria-v2.md) concluídos. Aceita o [ADR-023](../../produto/adrs/ADR-023-chat-v2-memoria-v2.md) (decisões 3, 7 e 8). **Último plano da série Chat v2.**

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

- Golds `chatR`, `chatM`, `chatS` (dark e light) do [ST6](../../stitch/plans/st6-chat-v2.md).
- [ADR-023](../../produto/adrs/ADR-023-chat-v2-memoria-v2.md), [ADR-017](../../produto/adrs/ADR-017-registro-consolidado.md), [chat](../../produto/specifications/chat.md).
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

- [chat](../../produto/specifications/chat.md): regras novas do plano, avisos de memória e sugestão da rotina; lista de estados com `chatR`, `chatM`, `chatS`.
- [memoria-push](../../produto/specifications/memoria-push.md): o que o usuário vê da memória (selo e chips).

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
- Server e regras de memória: [S11](../../server/plans/s11-chat-v2.md), [A28](a28-memoria-v2.md).

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
