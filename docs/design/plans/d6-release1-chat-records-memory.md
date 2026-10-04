# Plan — D6 Release 1: Chat records, photo and memory

- Status: Aguardando aprovação
- Date: 03/10/2026
- Owning context: `design`
- Affected code: none in `apps/` or `server/`. Figma `Design` → `Release 1` → section "Chat" (second row block), new components in `Componentes`. Repository: `docs/qa/figma/{dark,light}/{chatF,chatA,chatG,chatU,chatD,chatR,chatM,chatS}.png` and their node ids in `tools/export-figma.mjs`.
- Prerequisites: [D5](d5-release1-chat-core.md) `Concluído`.
- Figma MCP budget: ≤ 110 calls, with the same split-day rule.

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/d6-release1-chat-records-memory.md. Implemente o plano aprovado.`

## Objective

Draw the remaining Chat states in Aero, both themes: photo, attachment, receipt, pending replacement, undone record, meal plan, memory notices and routine suggestion.

## Sources (feature parity, ADR-031 § 4)

| Gold | Stitch title (layout reference only) |
|---|---|
| `chatF` | Foto de refeição e estimativa no Chat |
| `chatA` | Chat com foto anexada |
| `chatG` | Confirmação pós-gravação com recibo duplo-check |
| `chatU` | Chat com substituição pendente |
| `chatD` | Chat com registro desfeito |
| `chatR` | Chat com plano de refeição |
| `chatM` | Chat com memória atualizada |
| `chatS` | Chat com sugestão da rotina |

Behavior: [chat](../../produto/specifications/chat.md) (rules 17–19 and the receipt rules), [memoria-push](../../produto/specifications/memoria-push.md), [ADR-017](../../produto/adrs/ADR-017-registro-consolidado.md), [ADR-018](../../android/adrs/ADR-018-foto-2048.md), [ADR-023](../../produto/adrs/ADR-023-chat-v2-memoria-v2.md), [ADR-028](../../produto/adrs/ADR-028-registro-autonomo.md). Code: `feature/chat/`.

## Scope

Procedure as [D3 § Scope](completed/d3-release1-home.md#scope) steps 1, 3, 4 and 5, applied to the eight golds above. Flow-specific work:

1. **Components:**
   - `Chat/Receipt`: double check, Desfazer/Excluir/Trocar refeição/Editar per ADR-028; states `Saved`, `Undone`, `ReplacePending`;
   - `Chat/Photo` bubble;
   - `Chat/Composer` state `Attached` (thumbnail and ✕);
   - `Sheet/PhotoSource` (Tirar foto / Escolher da galeria);
   - `Chip/Memory`: 28 px, radius 14, the three labels of chat rule 17, informative only;
   - `Card/Routine` with Registrar / Quase igual;
   - `Card/MealPlan`.
2. **Meal photo:**
   - the Plugin API cannot fetch images, so the photo used by `chatF`/`chatA` is a file the owner provides, uploaded with `upload_assets`;
   - without one, the agent asks before generating an image (Figma AI credits);
   - the photo is content, not a UI screenshot.

## Out of scope

- Compose ([A43](../../android/plans/a43-chat-records-memory-aero.md)). Behavior or copy changes.

## Validation

As [D3 § Validation](completed/d3-release1-home.md#validation), with 16 frames.

## Results

<Filled at Completion.>
