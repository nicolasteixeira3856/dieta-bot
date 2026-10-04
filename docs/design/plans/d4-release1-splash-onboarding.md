# Plan — D4 Release 1: Splash and onboarding

- Status: Aguardando aprovação
- Date: 03/10/2026
- Owning context: `design`
- Affected code: none in `apps/` or `server/`. Figma `Design` → `Release 1` → section "Splash e onboarding", new components in `Componentes`. Repository: `docs/qa/figma/{dark,light}/{splash,o1,o1e,o2,o3,o3t,o3s,o4}.png` and their node ids in `tools/export-figma.mjs`.
- Prerequisites: [D3](completed/d3-release1-home.md) `Concluído` (it adds `Field/Number` and `Sheet/Bottom`).
- Figma MCP budget: ≤ 110 calls. If the cap is reached, stop after a complete screen and resume the next day.

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/d4-release1-splash-onboarding.md. Implemente o plano aprovado.`

## Objective

Draw splash and onboarding O1–O4 with their states in Aero, both themes, with the features the app has today.

## Sources (feature parity, ADR-031 § 4)

| Gold | Stitch title (layout reference only) |
|---|---|
| `splash` | Nutri Splash Screen |
| `o1` | Onboarding 1/4 - Teto do dia |
| `o1e` | Onboarding 1/4 - Teto do dia sem perfil |
| `o2` | Onboarding 2/4 - Compensação de treinos |
| `o3` | Onboarding 3/4 - Distribuição das refeições |
| `o3t` | Onboarding 3/4 - Seletor de horário |
| `o3s` | Onboarding 3/4 - Refeições Sáb e Dom |
| `o4` | Onboarding 4/4 - Alvos de macronutrientes |

Behavior: [perfil-onboarding](../../produto/specifications/perfil-onboarding.md), [ADR-012](../../produto/adrs/ADR-012-chat-home-perfil.md) (4 screens), [ADR-021](../../produto/adrs/ADR-021-refeicoes-por-dia.md). Code: `apps/android/app/src/main/java/com/nutri/android/feature/onboarding/` and the splash.

## Scope

Procedure as [D3 § Scope](completed/d3-release1-home.md#scope) steps 1, 3, 4 and 5 (discovery table first, Light row then Dark clone, owner review, export), applied to the eight golds above. Flow-specific work:

1. **Components:**
   - `Choice/Segmented`, the Aero replacement for the M3 `ButtonGroup` used in O1/O2: glass container with a glossy accent pill for the selected item, labels up to 3 lines;
   - `Option/Card` from D1 for the eat-back choices, with the typed-% field when "Porcentagem personalizada" is selected (default 50, ADR-012);
   - `Stepper/Progress`, 4 steps;
   - `Row/MealSlot`: slot name, time and toggle, for O3;
   - `Dialog/TimeWheel` for `o3t`;
   - `Tabs/Weekday` for `o3s`;
   - `Field/Number` with unit, for O1 and O4.
2. **Splash:**
   - Aero page background, the current mark and the current wordmark/copy ([splash gold](../../qa/stitch/dark/splash.png) and the code);
   - the mark is uploaded with `upload_assets` from `design/brand/`;
   - no new brand: `Branding` stays empty;
   - splash is a cold start ≤ 2 s, not a freeze.
3. **States:** `o1e` (without a profile) follows its own gold geometry, per [ADR-027](../../android/adrs/ADR-027-golds-divergentes.md).

## Out of scope

- Compose ([A41](../../android/plans/a41-splash-onboarding-aero.md)). New branding. Behavior or copy changes.

## Validation

As [D3 § Validation](completed/d3-release1-home.md#validation), with 16 frames and the split-day rule.

## Results

<Filled at Completion.>
