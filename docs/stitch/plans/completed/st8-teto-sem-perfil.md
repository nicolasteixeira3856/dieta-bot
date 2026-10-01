# Stitch gate — ST8 Ceiling screen before the profile (`o1e`)

- Status: Concluído
- Date: 01/10/2026
- Owning context: `stitch`
- Project: Stitch `Nutri` (`6282733070135794645`)
- Executes: owner decision of 01/10/2026 (age, height and weight are required on O1; ceiling mode and ceiling fields stay disabled until they are filled). Second owner decision of 01/10/2026: the hint sits right below the body fields, above "MODO DO TETO", and the screen grows taller instead of tightening the layout (prompt 8.2).
- Blocks: [A31 O1: required profile and keyboard flow](../../../android/plans/completed/a31-o1-perfil-obrigatorio-teclado.md)

Screen names: exact Stitch titles ([table](../../README.md#nomes-das-telas-regra-do-dono-29092026)). Each prompt has one block for the dark theme (`V2 Expressive`) and one for light (`V2 Light`).

## ⛔ Owner blocker — Stitch prompt

This step is manual and blocks A31. Prompt 8.1 created the screens; prompt 8.2 (current) fixes the hint position and the screen height. Each prompt is sent twice (dark and light). The existing "Onboarding 1/4 - Teto do dia (V2 Expressive)" / "(V2 Light)" (`o1`) stays unchanged: it is the filled state.

### Prompt 8.1 — new screen: O1 before age, height and weight (`o1e`)

Before sending:

1. Duplicate "Onboarding 1/4 - Teto do dia (V2 Expressive)" (dark) or "Onboarding 1/4 - Teto do dia (V2 Light)" (light) from the screen menu → Duplicate.
2. Rename the copy to the exact title: **Onboarding 1/4 - Teto do dia sem perfil (V2 Expressive)** or **Onboarding 1/4 - Teto do dia sem perfil (V2 Light)**.
3. Select only the renamed copy.

**Dark:**

```text
Screen to edit: "Onboarding 1/4 - Teto do dia sem perfil (V2 Expressive)". It is a copy of "Onboarding 1/4 - Teto do dia (V2 Expressive)". This is the same screen before the user typed age, height and weight: the daily goal cannot be suggested yet, so everything below the body fields is disabled.

1. Body fields under "IDADE, ALTURA E PESO": keep the three boxes with the same size, surface and 1px border #2a3139, but empty. Each shows only a placeholder in the dim color #5c6570, same size as the unit label: "idade · anos", "altura · cm", "peso · kg". No numbers.
2. "MODO DO TETO": keep the card with the three options and "Mesma meta todos os dias" still selected, but the whole card is disabled: draw the card, titles, subtitles and radio buttons at 38% opacity. The selected radio loses its gold fill and becomes a ring in the dim color #5c6570.
3. "META DIÁRIA": keep the field with the same size, surface and border, but disabled at 38% opacity, with no number: only a dash "—" in the dim color #5c6570 where "2000" was. The "kcal" chip stays at 38% opacity too.
4. Right below the "META DIÁRIA" field, left-aligned with it, one line in muted #8b939c, 14px, with a small info outline icon in muted #8b939c before it (no gold): "Preencha idade, altura e peso para ver a meta sugerida."
5. The "Continuar" pill button keeps its position, size and arrow, but is disabled: background #f3f5f7 at 38% opacity and label #111 at 70% opacity.

Keep everything else exactly as it is: status bar, progress bar, "ONBOARDING 1/4 · METABOLISMO", the title "Teto do dia", the subtitle, the "Homem" | "Mulher" toggle with "Homem" selected, section labels and spacing. Gold stays an accent only: the progress bar and "METABOLISMO"; nothing disabled is gold.
```

**Light:**

```text
Screen to edit: "Onboarding 1/4 - Teto do dia sem perfil (V2 Light)". It is a copy of "Onboarding 1/4 - Teto do dia (V2 Light)". This is the same screen before the user typed age, height and weight: the daily goal cannot be suggested yet, so everything below the body fields is disabled.

1. Body fields under "IDADE, ALTURA E PESO": keep the three boxes with the same size, surface and 1px border #d5d2cc, but empty. Each shows only a placeholder in the dim color #8b939c, same size as the unit label: "idade · anos", "altura · cm", "peso · kg". No numbers.
2. "MODO DO TETO": keep the card with the three options and "Mesma meta todos os dias" still selected, but the whole card is disabled: draw the card, titles, subtitles and radio buttons at 38% opacity. The selected radio loses its gold fill and becomes a ring in the dim color #8b939c.
3. "META DIÁRIA": keep the field with the same size, surface and border, but disabled at 38% opacity, with no number: only a dash "—" in the dim color #8b939c where "2000" was. The "kcal" chip stays at 38% opacity too.
4. Right below the "META DIÁRIA" field, left-aligned with it, one line in muted #5c636b, 14px, with a small info outline icon in muted #5c636b before it (no gold): "Preencha idade, altura e peso para ver a meta sugerida."
5. The "Continuar" pill button keeps its position, size and arrow, but is disabled: background #111111 at 38% opacity and label #f3f5f7 at 70% opacity.

Keep everything else exactly as it is: status bar, progress bar, "ONBOARDING 1/4 · METABOLISMO", the title "Teto do dia", the subtitle, the "Homem" | "Mulher" toggle with "Homem" selected, section labels and spacing. Gold stays an accent only: the progress bar and "METABOLISMO"; nothing disabled is gold.
```

After sending: three empty body fields with placeholders, the mode card and the daily goal field faded, a dash instead of "2000", the hint line below the field, and a faded "Continuar".

### Prompt 8.2 — fix: hint under the body fields, taller screen (`o1e`)

Owner decision of 01/10/2026: the hint belongs right below the body fields (it explains them), and the screen grows taller so nothing hides behind the floating "Continuar". This replaces item 4 of prompt 8.1. The `o1e` gold may be taller than 884 dp; like O3, it is a full-page capture.

Before sending: select only the most recent "Onboarding 1/4 - Teto do dia sem perfil (V2 Expressive)" (dark) or "Onboarding 1/4 - Teto do dia sem perfil (V2 Light)" (light).

**Dark:**

```text
Screen to edit: "Onboarding 1/4 - Teto do dia sem perfil (V2 Expressive)".

Fix only this layout:
1. Move the hint line "Preencha idade, altura e peso para ver a meta sugerida." with its small info outline icon from below the "META DIÁRIA" field to right below the three body fields under "IDADE, ALTURA E PESO": 12px under the fields, left-aligned with the first field, above the "MODO DO TETO" label. Keep its style: muted #8b939c, 14px, icon in muted #8b939c, no gold, not faded. Keep the same section gap between the hint and the "MODO DO TETO" label that the fields had before.
2. Nothing sits below the "META DIÁRIA" field anymore.
3. Make the screen taller instead of tightening anything: keep every existing size and spacing, and add height at the bottom so the whole "META DIÁRIA" field, with its "kcal" chip, ends at least 24px above the floating "Continuar" button and its bottom fade. The "Continuar" button stays pinned to the bottom of the screen with the same size, margins and home indicator.
4. The "kcal" chip is not gold: neutral surface and border like the field, text and bolt icon in dim #5c6570, at 38% opacity like the field.
5. "Mesma meta todos os dias" still looks selected: a ring in dim #5c6570 with a dim #5c6570 inner dot, different from the two empty rings.

Keep unchanged: the empty body fields with "idade · anos", "altura · cm", "peso · kg" (not faded); the faded mode card; the faded "META DIÁRIA" field with "—"; the faded "Continuar"; the status bar, progress bar, "ONBOARDING 1/4 · METABOLISMO", "Teto do dia", the subtitle and the "Homem" | "Mulher" toggle with "Homem" selected. Gold only on the progress bar and "METABOLISMO".
```

**Light:**

```text
Screen to edit: "Onboarding 1/4 - Teto do dia sem perfil (V2 Light)".

Fix only this layout:
1. Move the hint line "Preencha idade, altura e peso para ver a meta sugerida." with its small info outline icon from below the "META DIÁRIA" field to right below the three body fields under "IDADE, ALTURA E PESO": 12px under the fields, left-aligned with the first field, above the "MODO DO TETO" label. Keep its style: muted #5c636b, 14px, icon in muted #5c636b, no gold, not faded. Keep the same section gap between the hint and the "MODO DO TETO" label that the fields had before.
2. Nothing sits below the "META DIÁRIA" field anymore.
3. Make the screen taller instead of tightening anything: keep every existing size and spacing, and add height at the bottom so the whole "META DIÁRIA" field, with its "kcal" chip, ends at least 24px above the floating "Continuar" button and its bottom fade. The "Continuar" button stays pinned to the bottom of the screen with the same size, margins and home indicator.
4. The "kcal" chip is not gold: neutral surface and border like the field, text and bolt icon in dim #8b939c, at 38% opacity like the field.
5. "Mesma meta todos os dias" still looks selected: a ring in dim #8b939c with a dim #8b939c inner dot, different from the two empty rings.

Keep unchanged: the empty body fields with "idade · anos", "altura · cm", "peso · kg" (not faded); the faded mode card; the faded "META DIÁRIA" field with "—"; the faded "Continuar"; the status bar, progress bar, "ONBOARDING 1/4 · METABOLISMO", "Teto do dia", the subtitle and the "Homem" | "Mulher" toggle with "Homem" selected. Gold only on the progress bar and "METABOLISMO".
```

After sending: the hint right under the three body fields, then "MODO DO TETO"; nothing under the "META DIÁRIA" field; the whole field visible above "Continuar" on a taller screen; neutral "kcal" chip; the first radio still selected.

## Verification (agent)

Starts when the owner says the prompt ran. Gate checks: [`st8-teto-sem-perfil.checks.json`](st8-teto-sem-perfil.checks.json) ([types](../../README.md#verificação-automática-sv1)).

1. `node tools/verify-stitch.mjs st8 --report --out <scratchpad>/stitch-report.html`: finds `o1e` by exact title, runs the checks at 390 px, dark × light coherence and images, and writes the report. Answers `PASSOU` or `NÃO PASSOU`.
2. `NÃO PASSOU` (including a different title): **stop**, send the report to the owner in one message and keep the state `Aguardando o dono no Stitch`.
3. `PASSOU`: look at the report for the appearance items the script does not measure.
4. Add the `o1e` IDs (Log) to `tools/export-stitch.mjs` and the count (29) to `tools/check-stitch.mjs`.
5. `node tools/export-stitch.mjs --only o1,o1e` + `node tools/check-stitch.mjs` green (`o1` must come back unchanged through the noise filter).
6. Add `o1e.png` to the gold list of `AGENTS.md` (28 → 29) and `docs/qa/README.md`; add the `o1e` row to the name table in `docs/stitch/README.md`.

### Checklist

- [x] `o1e`: title exact in both themes — screen lookup
- [x] `o1e`: the three placeholders, no body numbers — `text has "idade", "altura", "peso"`, `text not "27", "180", "116"`
- [x] `o1e`: no "2000" and no "Sugerido" caption; hint line present without overflow — `text not "2000", "Sugerido"`, `fits "Preencha idade, altura e peso para ver a meta sugerida."`
- [x] `o1e`: hint right below the body fields and above "MODO DO TETO" — `gap` hint ↔ "MODO DO TETO" ≥ 16 px; order — report
- [x] `o1e`: hint and the whole "META DIÁRIA" field above the floating "Continuar" (taller screen) — `visibleAbove` hint and "kcal"
- [x] `o1e`: "kcal" chip neutral (no gold); first radio still reads as selected — report
- [x] `o1e`: header, toggle, section labels and "Continuar" kept — `text has`
- [x] `o1e`: mode card, daily goal field and "Continuar" visibly faded; radio without gold fill — report
- [x] Body fields not faded (they are the active part of the screen) — report
- [x] Gold only on the progress bar and "METABOLISMO" — report
- [x] `o1` unchanged — export noise filter

## Files this gate may touch

- `docs/qa/stitch/{dark,light}/o1e.png`
- `tools/export-stitch.mjs`, `tools/check-stitch.mjs`
- `st8-teto-sem-perfil.checks.json`
- Gold list in `AGENTS.md` and `docs/qa/README.md`; name table in `docs/stitch/README.md`

Nothing in `apps/` or `server/`.

## Log

- 01/10/2026 — gate written; waiting for the owner.
- 01/10/2026 — owner ran prompt 8.1 (dark `a31e81c1467f492ba9db3d65e1290fa8`, light `05bf9c80925f4a0e805f51e8760ed2b3`). Titles, copy, fits and coherence ok. Report review: the hint line sits behind the disabled, translucent "Continuar" (text bottom 822.8 px, button top 772 px). Added the `visibleAbove` check: NÃO PASSOU. Fix prompt sent to the owner; state kept.
- 01/10/2026 — re-check after the fix: NÃO PASSOU. Dark `a31e81c1467f492ba9db3d65e1290fa8` unchanged (same HTML; the edit did not persist). Light became a new screen `90b76571a765420a9f6f2011dc49fe17` (old `05bf9c80925f4a0e805f51e8760ed2b3` still answers under the same title): hint still covered (bottom 773 px, button top ~736 px), the "kcal" chip turned gold and the selected radio lost its dot. Second fix prompt with explicit gaps sent; state kept.
- 01/10/2026 — owner decision: instead of tightening gaps, the hint moves right below the body fields (above "MODO DO TETO") and the screen grows taller. Prompt 8.2 written; checks updated (`gap` hint ↔ "MODO DO TETO", `visibleAbove` hint and "kcal"). Waiting for the owner.
- 01/10/2026 — re-check after 8.2: script PASSOU (light resolved by ID `90b76571a765420a9f6f2011dc49fe17`, now in `tools/export-stitch.mjs`); hint below the body fields, neutral "kcal" chip, selected radio ok. Report review NÃO PASSOU: both screens still 884 px tall (not taller); the "META DIÁRIA" field ends at 804 px (dark) / 788 px (light), inside the bottom fade (760 / 744 px) and 4 / 20 px above "Continuar" (808 px); the dark mode card lost its 38 % fade (opacity 1). Fix prompt sent; state kept.
- 01/10/2026 — owner ran prompt 8.2 again (the first run was still "Generating Screen…"). The HTML frame is `100vh` (844–932 CSS px) while the API still declares 1768 px; the Stitch canvas shows 925 CSS px, and both Stitch screenshots are broken (dark half-rendered, light blank). `tools/export-stitch.mjs`: `o1e` rendered from HTML at 925 CSS px (`CSS_HEIGHT_OVERRIDE`, `RENDER_FROM_HTML`). `verify-stitch st8` PASSOU; report: hint under the body fields, whole "META DIÁRIA" field above "Continuar", neutral "kcal" chip, selected radio, body fields not faded, gold only on the progress bar and "METABOLISMO". Known divergence accepted by the owner: the dark mode card is faded twice (~15 % vs 38 % in light); A31 implements 38 % in both themes. `export-stitch --only o1,o1e`: `o1` restored by the noise filter (0.000 %), `o1e` 780×1850 in both themes; `check-stitch` 58/58. Concluído.
