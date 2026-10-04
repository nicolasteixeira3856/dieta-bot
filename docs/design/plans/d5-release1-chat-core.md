# Plan — D5 Release 1: Chat core

- Status: Aguardando aprovação
- Date: 03/10/2026
- Owning context: `design`
- Affected code: none in `apps/` or `server/`. Figma `Design` → `Release 1` → section "Chat", new components in `Componentes`. Repository: `docs/qa/figma/{dark,light}/{chat0,chatL,chatQ,chatE,chatT,chatP,chatX}.png` and their node ids in `tools/export-figma.mjs`.
- Prerequisites: [D4](d4-release1-splash-onboarding.md) `Concluído` (it adds `Choice/Segmented` and `Dialog/TimeWheel` patterns reused here).
- Figma MCP budget: ≤ 110 calls, with the same split-day rule.

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/d5-release1-chat-core.md. Implemente o plano aprovado.`

## Objective

Draw the core Chat states in Aero, both themes: empty, loading, question before the estimate, estimate, slot picker, skip confirmation and text-too-long error.

## Sources (feature parity, ADR-031 § 4)

| Gold | Stitch title (layout reference only) |
|---|---|
| `chat0` | Chat vazio |
| `chatL` | Chat loading - Estimando |
| `chatQ` | Chat com pergunta antes da estimativa |
| `chatE` | Estimate com botões de ação |
| `chatT` | Selecionar refeição - Bottom Sheet |
| `chatP` | Diálogo de confirmação para pular refeição |
| `chatX` | Chat com texto longo demais |

Behavior: [chat](../../produto/specifications/chat.md), [ADR-022](../../produto/adrs/ADR-022-limite-texto-chat.md), [ADR-026](../../produto/adrs/ADR-026-perguntas-antes-da-estimativa.md), [ADR-028](../../produto/adrs/ADR-028-registro-autonomo.md). Code: `apps/android/app/src/main/java/com/nutri/android/feature/chat/ChatScreen.kt`.

Parity points already known (ADR-031 § 4):

- no microphone in the composer;
- no tune button in the header (the app keeps only its space);
- the composer placeholder is "Descreva sua refeição ou envie foto...";
- the bot label is "Dieta Bot AI".

## Scope

Procedure as [D3 § Scope](d3-release1-home.md#scope) steps 1, 3, 4 and 5, applied to the seven golds above. Flow-specific work:

1. **Components:**
   - `Chat/Header`: back button, title with dot, subtitle, 44 px spacer;
   - `Chat/Composer` states: `Default`, `Blocked`, `TooLong` (counter and error copy per ADR-022);
   - `Chat/Estimate`: energy, macros with semantic colors, and the actions the spec defines for `chatE`;
   - `Chat/Question` with the "Forçar estimativa" bar (ADR-026);
   - `Loader/Aero`, the replacement for the M3 `LoadingIndicator`;
   - `Dialog/Confirm` for `chatP`;
   - slot list rows in `Sheet/Bottom` for `chatT`;
   - `Chip/Date` for the day separator.
2. **The pilot Chat draft** is replaced by `chatE` and deleted.

## Out of scope

- Photo, receipts, replacement, undo, plan, memory and routine states ([D6](d6-release1-chat-records-memory.md)). Compose ([A42](../../android/plans/a42-chat-core-aero.md)).

## Validation

As [D3 § Validation](d3-release1-home.md#validation), with 14 frames.

## Results

<Filled at Completion.>
