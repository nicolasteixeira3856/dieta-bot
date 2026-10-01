# Stitch gate — ST9 Autonomous record (`chatE`, `chatF`, `chatG`, `chatU`, `chatD`)

- Status: Aguardando o dono no Stitch
- Date: 01/10/2026
- Owning context: `stitch`
- Project: Stitch `Nutri` (`6282733070135794645`)
- Executes: [ADR-028](../../produto/adrs/ADR-028-registro-autonomo.md) (decisions 2, 3, 5 and 10)
- Blocks: [A34 Autonomous record](../../android/plans/a34-registro-autonomo.md)

Screen names: exact Stitch titles ([table](../README.md#nomes-das-telas-regra-do-dono-29092026)). Each prompt has one block for the dark theme (`V2 Expressive`) and one for light (`V2 Light`).

## ⛔ Owner blocker — Stitch prompts

This step is manual and blocks A34. Five prompts, each sent twice (dark and light). **Order matters:** 9.1 and 9.2 duplicate screens that 9.3 and 9.4 then edit. It can run in parallel with [S14](../../server/plans/s14-registro-autonomo.md).

### Prompt 9.1 — new screen: replace confirmation inside the Chat (`chatU`)

Before sending:

1. Duplicate "Estimate com botões de ação (V2 Expressive)" (dark) or "Estimate com botões de ação (V2 Light)" (light) from the screen menu → Duplicate.
2. Rename the copy to the exact title: **Chat com substituição pendente (V2 Expressive)** or **Chat com substituição pendente (V2 Light)**.
3. Select only the renamed copy.

**Dark:**

```text
Screen to edit: "Chat com substituição pendente (V2 Expressive)". It is a copy of "Estimate com botões de ação (V2 Expressive)". The dinner was already recorded with 380 kcal. The user adds a dessert; the assistant re-estimates the whole dinner and, because the dinner already has a record, asks inside the conversation before replacing it.

Conversation, top to bottom (keep the header and the date pill):
1. User bubble, time "21:02": "Também comi um pudim de leite no jantar"
2. The "Dieta Bot AI" label, then the assistant bubble with the same style as now: text "Juntei o pudim ao jantar. A estimativa total é de:", the "ENERGIA TOTAL" card with "~620 kcal", and the three macro chips "PROTEÍNA 30g P" (#4ec994), "CARBO 82g C" (#e58e42), "GORDURA 19g G" (#e8b86d). Remove the line "Deseja registrar essa refeição no Café da manhã?". Time "21:02" below the bubble, muted #8b939c, 12px.
3. Right below that time, left-aligned with the assistant bubble and with the same width, a confirmation card: surface #171b20, 1px border #2a3139, radius 14px, 16px padding. Title "Substituir Jantar?" 17px semibold in the primary text color #f3f5f7. Below it, 15px muted #8b939c: "Jantar tem 380 kcal. Fica com 620 kcal." Below, two buttons side by side with 8px gap, each 44px high, fully rounded: "Substituir" (background #f3f5f7, text #111111, semibold) and "Outra refeição" (transparent background, 1px border #2a3139, text #f3f5f7).

Remove the three-button group "Gravar café" | "Trocar" | "Pular" entirely: there is no action bar above the composer. Keep the composer unchanged. Gold stays an accent only; no gold background on any button.
```

**Light:**

```text
Screen to edit: "Chat com substituição pendente (V2 Light)". It is a copy of "Estimate com botões de ação (V2 Light)". The dinner was already recorded with 380 kcal. The user adds a dessert; the assistant re-estimates the whole dinner and, because the dinner already has a record, asks inside the conversation before replacing it.

Conversation, top to bottom (keep the header and the date pill):
1. User bubble, time "21:02": "Também comi um pudim de leite no jantar"
2. The "Dieta Bot AI" label, then the assistant bubble with the same style as now: text "Juntei o pudim ao jantar. A estimativa total é de:", the "ENERGIA TOTAL" card with "~620 kcal", and the three macro chips "PROTEÍNA 30g P" (#1b7a4b), "CARBO 82g C" (#c2651e), "GORDURA 19g G" (#b8873d). Remove the line "Deseja registrar essa refeição no Café da manhã?". Time "21:02" below the bubble, muted #5c636b, 12px.
3. Right below that time, left-aligned with the assistant bubble and with the same width, a confirmation card: surface #ffffff, 1px border #d5d2cc, radius 14px, 16px padding. Title "Substituir Jantar?" 17px semibold in the primary text color #14161a. Below it, 15px muted #5c636b: "Jantar tem 380 kcal. Fica com 620 kcal." Below, two buttons side by side with 8px gap, each 44px high, fully rounded: "Substituir" (background #111111, text #f3f5f7, semibold) and "Outra refeição" (transparent background, 1px border #d5d2cc, text #14161a).

Remove the three-button group "Gravar café" | "Trocar" | "Pular" entirely: there is no action bar above the composer. Keep the composer unchanged. Gold stays an accent only; no gold background on any button.
```

After sending: one user bubble, the estimate bubble without the question line, the confirmation card below it with Substituir and Outra refeição, and no action bar.

### Prompt 9.2 — new screen: undone replacement and restored dinner (`chatD`)

Before sending:

1. Duplicate "Confirmação pós-gravação com recibo duplo-check (V2 Expressive)" (dark) or "Confirmação pós-gravação com recibo duplo-check (V2 Light)" (light) from the screen menu → Duplicate.
2. Rename the copy to the exact title: **Chat com registro desfeito (V2 Expressive)** or **Chat com registro desfeito (V2 Light)**.
3. Select only the renamed copy.

**Dark:**

```text
Screen to edit: "Chat com registro desfeito (V2 Expressive)". It is a copy of "Confirmação pós-gravação com recibo duplo-check (V2 Expressive)". The assistant replaced the dinner (380 → 620 kcal); the user tapped "Desfazer", so the dinner went back to 380 kcal.

Conversation, top to bottom (keep the header and the date pill):
1. User bubble, time "21:02": "Também comi um pudim de leite no jantar"
2. The "Dieta Bot AI" label, then the assistant bubble in the same style as now, text "Juntei o pudim ao jantar.", the estimate card with "~620 kcal" and the chips "30g P" (#4ec994), "82g C" (#e58e42), "19g G" (#e8b86d). Time "21:02" below it.
3. First receipt, same card style as the existing receipt but at 50% opacity: title "Atualizado em Jantar · 20:00", the chip "380 → 620 kcal" in the same chip style as "+380 kcal". At the right end of the title row, a small muted #8b939c label with an undo icon: "Desfeito". No buttons under this receipt.
4. 12px below, second receipt, full opacity, same card style: a restore icon in the circle instead of the double check, title "Restaurado em Jantar · 20:00", chip "380 kcal".
5. Right below the second receipt, aligned with it and with the same width, three stacked buttons with 8px gap, each 44px high, radius 14px, surface #171b20, 1px border #2a3139, icon on the left and 15px semibold label:
   - delete icon + "Excluir", icon and label in #e07a6a
   - swap icon + "Trocar refeição", icon muted #8b939c, label #f3f5f7
   - pencil icon + "Editar", icon muted #8b939c, label #f3f5f7

Remove the old user message "2 pães franceses com 2 ovos mexidos no café da manhã", the old estimate and the old receipt "Registrado em Café da manhã". Keep the composer unchanged; no action bar above it. Gold stays an accent only.
```

**Light:**

```text
Screen to edit: "Chat com registro desfeito (V2 Light)". It is a copy of "Confirmação pós-gravação com recibo duplo-check (V2 Light)". The assistant replaced the dinner (380 → 620 kcal); the user tapped "Desfazer", so the dinner went back to 380 kcal.

Conversation, top to bottom (keep the header and the date pill):
1. User bubble, time "21:02": "Também comi um pudim de leite no jantar"
2. The "Dieta Bot AI" label, then the assistant bubble in the same style as now, text "Juntei o pudim ao jantar.", the estimate card with "~620 kcal" and the chips "30g P" (#1b7a4b), "82g C" (#c2651e), "19g G" (#b8873d). Time "21:02" below it.
3. First receipt, same card style as the existing receipt but at 50% opacity: title "Atualizado em Jantar · 20:00", the chip "380 → 620 kcal" in the same chip style as "+380 kcal". At the right end of the title row, a small muted #5c636b label with an undo icon: "Desfeito". No buttons under this receipt.
4. 12px below, second receipt, full opacity, same card style: a restore icon in the circle instead of the double check, title "Restaurado em Jantar · 20:00", chip "380 kcal".
5. Right below the second receipt, aligned with it and with the same width, three stacked buttons with 8px gap, each 44px high, radius 14px, surface #ffffff, 1px border #d5d2cc, icon on the left and 15px semibold label:
   - delete icon + "Excluir", icon and label in #c14d40
   - swap icon + "Trocar refeição", icon muted #5c636b, label #14161a
   - pencil icon + "Editar", icon muted #5c636b, label #14161a

Remove the old user message "2 pães franceses com 2 ovos mexidos no café da manhã", the old estimate and the old receipt "Registrado em Café da manhã". Keep the composer unchanged; no action bar above it. Gold stays an accent only.
```

After sending: the dimmed "Atualizado" receipt marked "Desfeito" with no buttons, the "Restaurado" receipt with Excluir / Trocar refeição / Editar stacked below it.

### Prompt 9.3 — unsure estimate: one Registrar button (`chatE`)

Before sending: prompt 9.1 is done. Select only "Estimate com botões de ação (V2 Expressive)" (dark) or "Estimate com botões de ação (V2 Light)" (light).

**Dark:**

```text
Screen to edit: "Estimate com botões de ação (V2 Expressive)". The assistant is not sure the user wants this meal recorded, so it offers one button.

Replace the three-button group "Gravar café" | "Trocar" | "Pular" with a single full-width pill button in the same position, same height, same background #171b20 and 1px border #2a3139 as that group: a check-circle icon and the label "Registrar" in the primary text color #f3f5f7, semibold. No other buttons.

Keep everything else exactly as it is: the header, the date pill, the user bubble, the "Dieta Bot AI" label, the assistant bubble with the "ENERGIA TOTAL" card and "Deseja registrar essa refeição no Café da manhã?", the time below the bubble and the composer. Gold stays an accent only; no gold background on the button.
```

**Light:**

```text
Screen to edit: "Estimate com botões de ação (V2 Light)". The assistant is not sure the user wants this meal recorded, so it offers one button.

Replace the three-button group "Gravar café" | "Trocar" | "Pular" with a single full-width pill button in the same position, same height, same background #ffffff and 1px border #d5d2cc as that group: a check-circle icon and the label "Registrar" in the primary text color #14161a, semibold. No other buttons.

Keep everything else exactly as it is: the header, the date pill, the user bubble, the "Dieta Bot AI" label, the assistant bubble with the "ENERGIA TOTAL" card and "Deseja registrar essa refeição no Café da manhã?", the time below the bubble and the composer. Gold stays an accent only; no gold background on the button.
```

After sending: the estimate with a single "Registrar" pill above the composer.

### Prompt 9.4 — automatic record: receipt with stacked actions (`chatG`)

Before sending: prompt 9.2 is done. Select only "Confirmação pós-gravação com recibo duplo-check (V2 Expressive)" (dark) or "Confirmação pós-gravação com recibo duplo-check (V2 Light)" (light).

**Dark:**

```text
Screen to edit: "Confirmação pós-gravação com recibo duplo-check (V2 Expressive)". The assistant recorded the breakfast by itself; the receipt now carries the actions to fix it.

Right below the receipt "Registrado em Café da manhã · 07:30" with "+380 kcal", aligned with it and with the same width, add three stacked buttons with 8px gap, each 44px high, radius 14px, surface #171b20, 1px border #2a3139, icon on the left and 15px semibold label:
- delete icon + "Excluir", icon and label in #e07a6a
- swap icon + "Trocar refeição", icon muted #8b939c, label #f3f5f7
- pencil icon + "Editar", icon muted #8b939c, label #f3f5f7

Keep everything else exactly as it is: the header, the date pill, the user bubble, the "Dieta Bot AI" label, the estimate bubble, the receipt and the composer. No action bar above the composer. Gold stays an accent only.
```

**Light:**

```text
Screen to edit: "Confirmação pós-gravação com recibo duplo-check (V2 Light)". The assistant recorded the breakfast by itself; the receipt now carries the actions to fix it.

Right below the receipt "Registrado em Café da manhã · 07:30" with "+380 kcal", aligned with it and with the same width, add three stacked buttons with 8px gap, each 44px high, radius 14px, surface #ffffff, 1px border #d5d2cc, icon on the left and 15px semibold label:
- delete icon + "Excluir", icon and label in #c14d40
- swap icon + "Trocar refeição", icon muted #5c636b, label #14161a
- pencil icon + "Editar", icon muted #5c636b, label #14161a

Keep everything else exactly as it is: the header, the date pill, the user bubble, the "Dieta Bot AI" label, the estimate bubble, the receipt and the composer. No action bar above the composer. Gold stays an accent only.
```

After sending: the receipt with Excluir / Trocar refeição / Editar stacked below it.

### Prompt 9.5 — photo recorded automatically: receipt without Editar (`chatF`)

Before sending: select only "Foto de refeição e estimativa no Chat (V2 Expressive)" (dark) or "Foto de refeição e estimativa no Chat (V2 Light)" (light).

**Dark:**

```text
Screen to edit: "Foto de refeição e estimativa no Chat (V2 Expressive)". The assistant recorded the lunch photo by itself.

1. In the estimate card, replace the large gold text "Dieta Bot AI" next to "ENERGIA ESTIMADA" with "~680 kcal" in the same style as "~380 kcal" on "Estimate com botões de ação (V2 Expressive)".
2. Remove the line "Deseja registrar essa refeição no Almoço?".
3. Remove the three-button group "Gravar almoço" | "Trocar" | "Pular" entirely: no action bar above the composer.
4. Below the assistant bubble add a receipt in exactly the style of the receipt on "Confirmação pós-gravação com recibo duplo-check (V2 Expressive)": double-check icon, title "Registrado em Almoço · 12:30", chip "+680 kcal".
5. Right below the receipt, aligned with it and with the same width, two stacked buttons with 8px gap, each 44px high, radius 14px, surface #171b20, 1px border #2a3139, icon on the left and 15px semibold label:
   - delete icon + "Excluir", icon and label in #e07a6a
   - swap icon + "Trocar refeição", icon muted #8b939c, label #f3f5f7
   No "Editar" button.

Keep the header, the date pill, the photo bubble, the "Dieta Bot AI" label, the macro chips and the composer. The screen may grow taller; do not shrink anything. Gold stays an accent only.
```

**Light:**

```text
Screen to edit: "Foto de refeição e estimativa no Chat (V2 Light)". The assistant recorded the lunch photo by itself.

1. In the estimate card, replace the large gold text "Dieta Bot AI" next to "ENERGIA ESTIMADA" with "~680 kcal" in the same style as "~380 kcal" on "Estimate com botões de ação (V2 Light)".
2. Remove the line "Deseja registrar essa refeição no Almoço?".
3. Remove the three-button group "Gravar almoço" | "Trocar" | "Pular" entirely: no action bar above the composer.
4. Below the assistant bubble add a receipt in exactly the style of the receipt on "Confirmação pós-gravação com recibo duplo-check (V2 Light)": double-check icon, title "Registrado em Almoço · 12:30", chip "+680 kcal".
5. Right below the receipt, aligned with it and with the same width, two stacked buttons with 8px gap, each 44px high, radius 14px, surface #ffffff, 1px border #d5d2cc, icon on the left and 15px semibold label:
   - delete icon + "Excluir", icon and label in #c14d40
   - swap icon + "Trocar refeição", icon muted #5c636b, label #14161a
   No "Editar" button.

Keep the header, the date pill, the photo bubble, the "Dieta Bot AI" label, the macro chips and the composer. The screen may grow taller; do not shrink anything. Gold stays an accent only.
```

After sending: the photo estimate with "~680 kcal", the receipt and only Excluir / Trocar refeição below it.

## Verification (agent)

Starts when the owner says the prompts ran. Gate checks: [`st9-registro-autonomo.checks.json`](st9-registro-autonomo.checks.json) ([types](../README.md#verificação-automática-sv1)).

1. `node tools/verify-stitch.mjs st9 --report --out <scratchpad>/stitch-report.html`: finds `chatE`, `chatF`, `chatG` by ID and `chatU`, `chatD` by exact title, runs the checks at 390 px, dark × light coherence and images, and writes the before/after report. Answers `PASSOU` or `NÃO PASSOU`.
2. `NÃO PASSOU` (including a different title): **stop**, send the report to the owner in one message and keep the state `Aguardando o dono no Stitch`.
3. `PASSOU`: look at the report (before/after) for the appearance items the script does not measure.
4. Add the `chatU` and `chatD` IDs (Log) to `tools/export-stitch.mjs` and the count (31) to `tools/check-stitch.mjs`.
5. `node tools/export-stitch.mjs --only chatE,chatF,chatG,chatU,chatD` + `node tools/check-stitch.mjs` green.
6. Add `chatU.png` and `chatD.png` to the gold list of `AGENTS.md` (29 → 31) and `docs/qa/README.md`. Both titles are already in the name table of [stitch/README.md](../README.md#nomes-das-telas-regra-do-dono-29092026) (the verifier reads them from there).

### Checklist

- [ ] `chatU`: title exact in both themes — screen lookup
- [ ] `chatU`: user message, estimate ~620, confirmation card with "Substituir Jantar?", "Jantar tem 380 kcal. Fica com 620 kcal.", Substituir and Outra refeição — `text has`
- [ ] `chatU`: no action bar, no "Deseja registrar" — `text not "Gravar", "Pular", "Deseja registrar"`
- [ ] `chatU`: confirmation card below the bubble, left-aligned, Substituir as the light/dark CTA — report
- [ ] `chatD`: title exact in both themes — screen lookup
- [ ] `chatD`: "Atualizado em Jantar · 20:00" with "380 → 620 kcal" and "Desfeito"; "Restaurado em Jantar · 20:00" with "380 kcal" — `text has`
- [ ] `chatD`: Excluir, Trocar refeição, Editar stacked under the restored receipt only; first receipt dimmed — `text has` + report
- [ ] `chatD`: old breakfast content gone — `text not "2 pães franceses", "Café da manhã"`
- [ ] `chatE`: single "Registrar" pill, no Gravar/Trocar/Pular; card and question line kept — `text has "Registrar", "ENERGIA TOTAL", "Deseja registrar"`, `text not "Gravar café", "Pular"`, `fits "Registrar"`
- [ ] `chatG`: Excluir, Trocar refeição, Editar under the receipt; no action bar — `text has`, `text not "Gravar", "Pular"`
- [ ] `chatF`: "~680 kcal" in the card; receipt "Registrado em Almoço · 12:30" "+680 kcal"; Excluir and Trocar refeição only — `text has`, `text not "Editar", "Gravar almoço", "Deseja registrar"`
- [ ] Excluir in the `bad` color, other actions neutral, no gold button background — report
- [ ] Every action label on one line — `fits`

## Files this gate may touch

- `docs/qa/stitch/{dark,light}/chatE.png`, `chatF.png`, `chatG.png`, `chatU.png`, `chatD.png`
- `tools/export-stitch.mjs`, `tools/check-stitch.mjs`
- `st9-registro-autonomo.checks.json`
- Gold list in `AGENTS.md` and `docs/qa/README.md`; name table in `docs/stitch/README.md`

Nothing in `apps/` or `server/`.

## Log

- 01/10/2026 — gate written; waiting for the owner.
