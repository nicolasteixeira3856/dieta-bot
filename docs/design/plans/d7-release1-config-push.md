# Plan — D7 Release 1: Config and push

- Status: Aguardando aprovação
- Date: 03/10/2026
- Owning context: `design`
- Affected code: none in `apps/` or `server/`. Figma `Design` → `Release 1` → section "Config e push". Repository: `docs/qa/figma/{dark,light}/{cfg,cfgS,wipe,push}.png` and their node ids in `tools/export-figma.mjs`.
- Prerequisites: [D6](d6-release1-chat-records-memory.md) `Concluído`.
- Figma MCP budget: ≤ 80 calls.

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/d7-release1-config-push.md. Implemente o plano aprovado.`

## Objective

Draw the profile/day settings, the per-day meals settings, the wipe dialog and the meal reminder notification in Aero, both themes. This closes the `Release 1` page.

## Sources (feature parity, ADR-031 § 4)

| Gold | Stitch title (layout reference only) |
|---|---|
| `cfg` | Configurações do perfil e dia |
| `cfgS` | Configurações com refeições por dia |
| `wipe` | Reiniciar registros de hoje - Diálogo Wipe |
| `push` | Notificação do sistema - Lembrete de refeição |

Behavior: [memoria-push](../../produto/specifications/memoria-push.md) (Config, push), [ADR-021](../../produto/adrs/ADR-021-refeicoes-por-dia.md). Code: the Config and push packages under `feature/`.

## Scope

Procedure as [D3 § Scope](d3-release1-home.md#scope) steps 1, 3, 4 and 5, applied to the four golds above. Flow-specific work:

1. **Config:** reuse the onboarding components (`Field/Number`, `Choice/Segmented`, `Row/MealSlot`, `Tabs/Weekday`) and `Dialog/Confirm` for `wipe`.
2. **Push:**
   - the notification is drawn by the system;
   - the app controls only the small icon, the title, the text, the actions and the accent color;
   - the frame shows a neutral lock screen with the notification, and only those fields follow Aero.
3. **Dev tools:** `Memória da IA (dev)` stays without a gold ([ADR-019](../../produto/adrs/ADR-019-ferramentas-dev.md)).
4. **Review:** after the owner's OK, a final readability pass on `Release 1` as a whole (section order Splash e onboarding → Home → Chat → Config e push, spacing) with one page screenshot.

## Out of scope

- Compose ([A44](../../android/plans/a44-config-push-aero.md)). Behavior or copy changes.

## Validation

As [D3 § Validation](d3-release1-home.md#validation), with 8 frames and the page screenshot.

## Results

<Filled at Completion.>
