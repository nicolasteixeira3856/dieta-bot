# ADR-027 — Visual QA when Stitch golds disagree

- Status: Accepted (01/10/2026, owner decision after A31)
- Date: 2026-10-01
- Context: `android`
- Extends: [ADR-008](../../decisions/008-visual-qa.md) and [ADR-009](../../decisions/009-visual-match.md) (gold comparison). Replaces nothing.

## Context

During [A31](../plans/completed/a31-o1-perfil-obrigatorio-teclado.md) the Stitch golds disagreed with each other and with the plan:

- The state gold `o1e` (O1 before the profile) is tighter than the base gold `o1`: sex toggle 9–13 dp higher, toggle → body label 6–10 dp less, mode group → "META DIÁRIA" 5–8 dp less.
- The dark and light `o1e` golds, generated from the same ST8 prompt, differ from each other by up to 16 dp.
- The dark `o1e` gold draws the disabled mode titles dimmer than the 38 % opacity its own prompt and the plan specify; the light gold matches 38 %.
- `Modifier.alpha` (graphics layer) is not drawn by the Robolectric capture used by `StitchGoldTest`, so a disabled state looked fully opaque on the JVM.

The owner approved the A31 resolutions and asked to make them the rule, so later validation cycles do not stop at the same barrier.

## Decision

1. **State gold vs base gold.** When the gold of a state variant (for example `o1e`) has different geometry from the base gold of the same screen (`o1`), each state follows its own gold: the variant takes its own spacing, the base keeps its own. A layout reflow when the state changes is accepted. The base screen is never moved to fit the variant.
2. **Dark vs light gold.** When the dark and light golds of one screen disagree on geometry, the app keeps one layout for both themes with the average of the two golds, provided both themes pass the 2 % gate. If the average cannot pass both, the existing practice applies: follow dark, report the light screen only (`SCREEN_REPORT_ONLY`), gate the new part by region, and record it in the plan.
3. **Spec vs gold rendering.** When the approved plan, specification or [tokens](../../tokens.md) give a normative value (opacity, token colour, copy) and a gold renders it differently, the plan/spec value wins. The difference goes into the written diff list; it is not a gold failure while the gate passes.
4. **Disabled dimming.** Disabled controls dim through an explicit save layer (`Modifier.disabledAlpha` in `feature/onboarding/OnboardingChrome.kt`, 38 %), not `Modifier.alpha`, so device and JVM gold renders agree.
5. **Change.** These rules change only through a future approved plan that states the change explicitly. A chat request or a new gold does not change them by itself.

## Motivation

Stitch generates every screen and theme separately, so small geometry drift between golds is normal. Without a rule, each conflict stops a delivery and needs an owner decision; with it, the agent resolves the conflict the same way every time and records it.

## Consequences

### Positive

- Conflicting golds no longer block a plan when the documented resolution passes the gate.
- Spec values stay stable across themes even when one gold drifts.
- JVM and device renders of disabled states match.

### Negative

- A state change can move content a few dp (accepted reflow).
- With averaged geometry, each theme keeps a residual offset of a few dp against its own gold.

## Alternatives considered

- **Regenerate the golds in Stitch until they agree:** a manual owner gate per drift, with no guarantee of convergence.
- **Force the base geometry on the variant:** the variant fails its gold gate.
- **Follow the gold over the spec:** opacity and colours would differ between themes for the same state.

## Relations

- Plan: [A31](../plans/completed/a31-o1-perfil-obrigatorio-teclado.md). Gate: [ST8](../../stitch/plans/completed/st8-teto-sem-perfil.md).
- QA workflow: [docs/qa/README.md](../../qa/README.md) § Gate.

Once accepted, this ADR is not edited.
