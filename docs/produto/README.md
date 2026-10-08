# produto

Este README roteia. Estado, versão e data ficam no arquivo dono.

## Proposito

Comportamento visivel do Dieta Bot: job, telas, copy, onboarding, slots, o que entra no prompt da IA.

## Tipo e ownership

- Tipo: produto.
- Codigo principal: nenhum. Implementacao mora em android e server.
- Consumidores: [android](../android/README.md), [server](../server/README.md).

## Escopo

- Job: encaixar a proxima refeicao no saldo do dia.
- Telas, copy, onboarding, chips/slots, memoria de produto.
- O que o modelo ve (perfil, memoria, snapshot do dia).

## Fora de escopo

- Schema Room, Compose, push — [android](../android/README.md).
- Rotas HTTP, LLM, host — [server](../server/README.md).
- Política de conteúdo (escopo, recusas, moderação) — [content-policy](../content-policy/README.md). UI de aceite legal ou de idade exige plano de produto próprio e plano de design (gate Figma), e está bloqueada pelo [production gate](../content-policy/production-gate.md).

## Fronteiras e dependencias

- Constituicao vigente: [`AGENTS.md`](../../AGENTS.md).
- Visual: [`docs/tokens.md`](../tokens.md) e os golds do Figma `Design` ([inventário](../qa/README.md)).
- Nao duplicar contrato HTTP nem schema. Link.

## Como usar esta documentacao

Segue [docs/sdd/README.md](../sdd/README.md).

1. AGENTS.md
2. a spec do assunto (índice abaixo)
3. os ADRs que a spec cita
4. docs/tokens.md e os golds do Figma `Design` ([design](../design/README.md)) para assuntos visuais

## Indice

### Especificacoes

- [chat](specifications/chat.md) — Chat: composer, registro, recibos, perguntas, memória visível.
- [home-timeline](specifications/home-timeline.md) — Home painel e timeline.
- [perfil-onboarding](specifications/perfil-onboarding.md) — splash e onboarding O1–O4.
- [memoria-push](specifications/memoria-push.md) — memória, foto, Config e push.

### ADRs

Status: a linha de status de cada ADR. Histórico em `docs/decisions/` (ver [matriz](../README.md)).

- [ADR-012](adrs/ADR-012-chat-home-perfil.md) — Chat tela, Home painel, perfil nomeado.
- [ADR-016](adrs/ADR-016-nome-dieta-bot.md) — nome "Dieta Bot".
- [ADR-017](adrs/ADR-017-registro-consolidado.md) — um registro por refeição.
- [ADR-019](adrs/ADR-019-ferramentas-dev.md) — telas de ferramenta só no dev.
- [ADR-020](adrs/ADR-020-estados-novos-chat-home-horario.md) — golds novos `chatA`, `homeW`, `o3t`.
- [ADR-021](adrs/ADR-021-refeicoes-por-dia.md) — refeições por dia da semana.
- [ADR-022](adrs/ADR-022-limite-texto-chat.md) — mensagem do Chat até 2000 caracteres, estado de erro.
- [ADR-023](adrs/ADR-023-chat-v2-memoria-v2.md) — Chat v2 (intenção, texto da refeição, plano) e Memória v2 (permanente + dinâmica).
- [ADR-026](adrs/ADR-026-perguntas-antes-da-estimativa.md) — perguntas antes da estimativa, até 3 rodadas, Forçar estimativa.
- [ADR-028](adrs/ADR-028-registro-autonomo.md) — registro autônomo no Chat com recibo reversível.
- [ADR-029](adrs/ADR-029-fatos-temporarios-compactacao.md) — fatos temporários (3 dias), compactação que mantém o fim aberto, dia da refeição.
- [ADR-032](adrs/ADR-032-acrescimos-e-correcoes-de-refeicoes.md) — explicit meal additions, revisions and destination semantics.
- [ADR-034](adrs/ADR-034-fibrai-brand-tali-assistant.md) — marca "Fibrai", assistente "Tali".
- [ADR-035](adrs/ADR-035-tali-in-app-identity.md) — Fibrai e Tali no app: nome visível, rótulo e bolha de avatar.
- [ADR-039](adrs/ADR-039-plan-cooking-and-budget-choice.md) — cooking help in a plan; over-budget check by the server, Pode passar / Ajustar para caber.
- [ADR-040](adrs/ADR-040-home-card-gestures-app-reset.md) — empty Home card: tap opens the Chat, long press skips; app reset in Config.
- [ADR-041](adrs/ADR-041-reference-portions-in-chat-instructions.md) — reference portions (TACO) in the Chat instructions; open requests answered as plans; fully quantified first message estimated, not questioned.
- [ADR-043](adrs/ADR-043-plan-objective-protein-and-meal-window.md) — a plan targets the protein gap inside the meal window; closing lines per remaining slot.
- [ADR-044](adrs/ADR-044-assistant-tone-and-closures.md) — assistant tone chosen by the user (`seco` | `duro`); day and week closure.
- [ADR-045](adrs/ADR-045-rich-replies-in-chat-bubbles.md) — emphasis, lists and a small table inside the Chat bubbles.
- [ADR-046](adrs/ADR-046-planned-meal-reservation.md) — a plan can be reserved for its meal before it is eaten.
- [ADR-047](adrs/ADR-047-skips-alongside-other-actions.md) — skips stated next to other actions; a skip over a record asks to delete it.
- [ADR-048](adrs/ADR-048-chat-actions-in-thread-copy-scroll-capture.md) — Chat actions in the thread under their message; messages selected and copied as in WhatsApp; long screens allow a scrolling screenshot.
- [ADR-049](adrs/ADR-049-workout-energy-via-chat.md) — workout energy reported through the Chat; the app records it.
- [ADR-050](adrs/ADR-050-typed-actions-per-message.md) — typed actions per Chat message, one receipt each.
- [ADR-051](adrs/ADR-051-plan-option-identity-and-chat-discovery.md) — plan options with ids; routine discovery on the first opening.
- [ADR-052](adrs/ADR-052-saved-recipes.md) — saved recipes.
- [ADR-053](adrs/ADR-053-visible-memory-screen.md) — "O que a Tali sabe": the memory as a product screen.
- [ADR-055](adrs/ADR-055-protein-boost-hybrid.md) — protein boost: the model proposes, the server validates.

### Planos

This context has no `plans/`. Deliveries belong to [Android](../android/README.md), [server](../server/README.md) and [design](../design/README.md); the Stitch history is routed by its [README](../stitch/README.md).

Meal updates: D9 and A47 (history: [`completed/`](../design/plans/completed/), [`completed/`](../android/plans/completed/)).

Chat after the brainstorm of 07/10/2026 (`benchmark/MELHORIAS_CHAT_07_10_2026.md` § 5, benchmark `benchmark/RESULTADOS_08_10_2026.md`), in execution order: [S33](../server/plans/pending_manual_validation/s33-chat-context-effort-low.md) → [S34](../server/plans/pending_manual_validation/s34-protein-boost-hybrid.md) → [A64](../android/plans/a64-chat-context-fields-day-balance.md) → [CP10](../content-policy/plans/cp10-workout-in-scope-and-skip-boundary.md) → [S35](../server/plans/s35-workout-via-chat.md) → [A65](../android/plans/a65-workout-via-chat.md) → [S36](../server/plans/s36-typed-actions.md) → [A66](../android/plans/a66-typed-actions-batch.md) → [S37](../server/plans/s37-plan-options-and-discovery.md) → [A67](../android/plans/a67-plan-options-and-discovery.md) → D23 ([history](../design/plans/completed/)) → [S38](../server/plans/s38-saved-recipes.md) → [A68](../android/plans/a68-saved-recipes.md) → D24 ([history](../design/plans/completed/)) → [A69](../android/plans/a69-visible-memory.md) (ADR-049 to ADR-055).

Protein-first plan, tone, formatting, planned meal and skips: S24 ([`completed/`](../server/plans/completed/), ADR-043); S30 ([`completed/`](../server/plans/completed/), server, ADR-044/045/046) and [A60](../android/plans/pending_manual_validation/a60-tone-formatting-planned-skips.md) (app, ADR-039/044/045/046/047); its part E, the skips, by [A59](../android/plans/pending_manual_validation/a59-skips-with-other-actions.md); the auto-record defect A54 ([`completed/`](../android/plans/completed/)). Deferred by owner decision: goal weight, [A56](../android/plans/out_of_scope/a56-goal-weight.md).
