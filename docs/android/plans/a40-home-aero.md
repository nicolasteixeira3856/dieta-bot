# Plan — A40 Home on Aero

- Status: Aguardando aprovação
- Date: 03/10/2026
- Owning context: `android`
- Affected code: `apps/android/` only:
  - `feature/home/`, Home-specific components in `core/designsystem/aero/`, tests;
  - docs updated with the delivery: the source of `home0`, `home1`, `homeX` and `homeW` in `docs/qa/README.md`, captures in `docs/qa/android/current/{dark,light}/`.
- Prerequisites: [A39](a39-aero-foundation.md) and [D3](../../design/plans/completed/d3-release1-home.md) `Concluído` (Figma golds in `docs/qa/figma/`).

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/android/plans/a40-home-aero.md. Implemente o plano aprovado.`

## Objective

Move the Home screen and its four states onto Aero, matching the Figma golds, with no behavior change.

## Scope

1. **Build:**
   - the Home is wrapped in `AeroTheme`, using the A39 components plus the D3 additions (`AeroRingDay`, `AeroSheet`, `AeroFieldNumber`, `AeroHeaderDay`);
   - the workout sheet (`homeW`) uses `AeroSheet`;
   - testTags and the capture flow (`tools/capture-home.sh`) are unchanged.
2. **Values:** exact values (color, radius, spacing, type) come from the Figma variables and component properties read through the MCP, not measured from the PNG.
3. **Visual loop** ([docs/qa/README.md](../../qa/README.md) § Loop, against `docs/qa/figma/`):
   - capture → `diff-gold` → diff list in Results → fix, until each of the 8 images passes the gate (≤ 2 %);
   - glass blur is rasterized differently by Figma and Android: when a difference is only blur grain inside a glass surface, it is listed as such and not counted.
4. **Switch:** in the same delivery, the source of the four Home ids becomes `figma` in the inventory.
5. **Spec:** [home-timeline](../../produto/specifications/home-timeline.md) changes only its visual references (gold source, tokens); behavior text stays. A40 joins its Provenance.

## Out of scope

- Other flows; Material removal ([A45](a45-remove-material3.md)); behavior or copy.

## Validation

1. `testDevDebugUnitTest`, `verifyRoborazziDevDebug` (baselines re-recorded for Home only), `assembleDevRelease`.
2. `tools/capture-home.sh dark|light` + `node tools/diff-gold.mjs dark/home1 …`: every Home gold passes.
3. Manual, on the owner's phone:
   - scroll smoothness of the timeline with blur (no visible jank);
   - number legibility outdoors in both themes.

   The plan waits in `pending_manual_validation/` for this.
4. `node tools/check-docs.mjs` passes.

## Results

<Filled at Completion.>
