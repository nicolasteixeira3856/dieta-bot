# Stitch gate — ST8 Ceiling screen before the profile (`o1e`)

- Status: Aguardando o dono no Stitch
- Date: 01/10/2026
- Owning context: `stitch`
- Project: Stitch `Nutri` (`6282733070135794645`)
- Executes: owner decision of 01/10/2026 (age, height and weight are required on O1; ceiling mode and ceiling fields stay disabled until they are filled)
- Blocks: [A31 O1: required profile and keyboard flow](../../android/plans/a31-o1-perfil-obrigatorio-teclado.md)

Screen names: exact Stitch titles ([table](../README.md#nomes-das-telas-regra-do-dono-29092026)). Each prompt has one block for the dark theme (`V2 Expressive`) and one for light (`V2 Light`).

## ⛔ Owner blocker — Stitch prompt

This step is manual and blocks A31. One prompt, sent twice (dark and light). The existing "Onboarding 1/4 - Teto do dia (V2 Expressive)" / "(V2 Light)" (`o1`) stays unchanged: it is the filled state.

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

## Verification (agent)

Starts when the owner says the prompt ran. Gate checks: [`st8-teto-sem-perfil.checks.json`](st8-teto-sem-perfil.checks.json) ([types](../README.md#verificação-automática-sv1)).

1. `node tools/verify-stitch.mjs st8 --report --out <scratchpad>/stitch-report.html`: finds `o1e` by exact title, runs the checks at 390 px, dark × light coherence and images, and writes the report. Answers `PASSOU` or `NÃO PASSOU`.
2. `NÃO PASSOU` (including a different title): **stop**, send the report to the owner in one message and keep the state `Aguardando o dono no Stitch`.
3. `PASSOU`: look at the report for the appearance items the script does not measure.
4. Add the `o1e` IDs (Log) to `tools/export-stitch.mjs` and the count (29) to `tools/check-stitch.mjs`.
5. `node tools/export-stitch.mjs --only o1,o1e` + `node tools/check-stitch.mjs` green (`o1` must come back unchanged through the noise filter).
6. Add `o1e.png` to the gold list of `AGENTS.md` (28 → 29) and `docs/qa/README.md`; add the `o1e` row to the name table in `docs/stitch/README.md`.

### Checklist

- [ ] `o1e`: title exact in both themes — screen lookup
- [ ] `o1e`: the three placeholders, no body numbers — `text has "idade", "altura", "peso"`, `text not "27", "180", "116"`
- [ ] `o1e`: no "2000" and no "Sugerido" caption; hint line present without overflow — `text not "2000", "Sugerido"`, `fits "Preencha idade, altura e peso para ver a meta sugerida."`
- [ ] `o1e`: header, toggle, section labels and "Continuar" kept — `text has`
- [ ] `o1e`: mode card, daily goal field and "Continuar" visibly faded; radio without gold fill — report
- [ ] Body fields not faded (they are the active part of the screen) — report
- [ ] Gold only on the progress bar and "METABOLISMO" — report
- [ ] `o1` unchanged — export noise filter

## Files this gate may touch

- `docs/qa/stitch/{dark,light}/o1e.png`
- `tools/export-stitch.mjs`, `tools/check-stitch.mjs`
- `st8-teto-sem-perfil.checks.json`
- Gold list in `AGENTS.md` and `docs/qa/README.md`; name table in `docs/stitch/README.md`

Nothing in `apps/` or `server/`.

## Log

- 01/10/2026 — gate written; waiting for the owner.
