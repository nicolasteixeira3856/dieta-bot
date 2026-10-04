# Plan — A41 Splash and onboarding on Aero

- Status: Aguardando aprovação
- Date: 03/10/2026
- Owning context: `android`
- Affected code: `apps/android/` only:
  - splash, `feature/onboarding/`, onboarding components in `core/designsystem/aero/`, tests;
  - docs updated with the delivery: the source of `splash`, `o1`, `o1e`, `o2`, `o3`, `o3t`, `o3s` and `o4` in `docs/qa/README.md`, captures.
- Prerequisites: [A40](a40-home-aero.md) and [D4](../../design/plans/d4-release1-splash-onboarding.md) `Concluído`.

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/android/plans/a41-splash-onboarding-aero.md. Implemente o plano aprovado.`

## Objective

Move the splash and O1–O4, with their states, onto Aero, matching the Figma golds, with no behavior change.

## Scope

Procedure as [A40 § Scope](a40-home-aero.md#scope) steps 1–5, for the eight golds of this flow (`tools/capture-onboarding.sh`). Flow-specific work:

1. **`AeroSegmented`** replaces the M3 `ButtonGroup` / `ExpressiveButtonGroup` in O1 and O2: same three eat-back modes, typed % default 50, no cap (AGENTS "Product").
2. **`AeroTimeWheelDialog`** replaces `TimeWheelDialog`'s Material look (`o3t`); the measured `o3t` regression (docs/qa § Regressão) is re-done against the Figma gold.
3. **Splash** keeps its timing (cold start ≤ 2 s, not a freeze) and the current mark.
4. **Spec:** [perfil-onboarding](../../produto/specifications/perfil-onboarding.md) changes only its visual references.

## Out of scope

As A40.

## Validation

As [A40 § Validation](a40-home-aero.md#validation), with `capture-onboarding.sh` and 16 images.

## Results

<Filled at Completion.>
