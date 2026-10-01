# Stitch gate — ST7 Question before the estimate (`chatE`, `chatQ`)

- Status: Concluído
- Date: 30/09/2026
- Owning context: `stitch`
- Project: Stitch `Nutri` (`6282733070135794645`)
- Executes: [ADR-026](../../../produto/adrs/ADR-026-perguntas-antes-da-estimativa.md) (decisions 1, 4 and 7)
- Blocks: [A30 Questions before the estimate](../../../android/plans/completed/a30-perguntas-antes-da-estimativa.md)

Screen names: exact Stitch titles ([table](../../README.md#nomes-das-telas-regra-do-dono-29092026)). Each prompt has one block for the dark theme (`V2 Expressive`) and one for light (`V2 Light`).

## ⛔ Owner blocker — Stitch prompts

This step is manual and blocks A30. Two prompts, each sent twice (dark and light). Run 7.1 first: it duplicates the screen that 7.2 then edits. It can run in parallel with [S13](../../../server/plans/completed/s13-perguntas-antes-da-estimativa.md).

### Prompt 7.1 — new screen: second question with Forçar estimativa (`chatQ`)

Before sending:

1. Duplicate "Estimate com botões de ação (V2 Expressive)" (dark) or "Estimate com botões de ação (V2 Light)" (light) from the screen menu → Duplicate. Do it before prompt 7.2: the copy must still have the question bubble with the gold bar, because this screen reuses its style.
2. Rename the copy to the exact title: **Chat com pergunta antes da estimativa (V2 Expressive)** or **Chat com pergunta antes da estimativa (V2 Light)**.
3. Select only the renamed copy.

**Dark:**

```text
Screen to edit: "Chat com pergunta antes da estimativa (V2 Expressive)". It is a copy of "Estimate com botões de ação (V2 Expressive)". The user logged a dinner; the assistant still has doubts, so it asks before estimating. No estimate is shown yet. This is the assistant's second question.

Conversation, top to bottom (keep the header and the date pill):
1. User bubble, time "20:12": "Jantei macarrão com frango ao molho branco"
2. The "Dieta Bot AI" label, then a question bubble in exactly the style of the existing question bubble (gold left bar #e8b86d, help icon in gold, text 18px in the primary text color #f3f5f7, surface #171b20, 16px radius): "O molho branco levou creme de leite ou requeijão? E o macarrão, foi 1 prato raso ou fundo?" Time "20:12" below it, muted #8b939c, 12px.
3. User bubble, time "20:14": "Creme de leite, prato fundo"
4. The "Dieta Bot AI" label, then a second question bubble in the same style: "O frango foi grelhado ou empanado?" Time "20:15" below it.

Remove the assistant bubble with the "ENERGIA TOTAL" card, the PROTEÍNA / CARBO / GORDURA chips and the "Deseja registrar essa refeição no Café da manhã?" line. Remove the old question "Os pães tinham manteiga ou requeijão?".

Replace the three-button group "Gravar café" | "Trocar" | "Pular" with a single full-width pill button in the same position, same height, same background #171b20 and 1px border #2a3139 as that group: a fast-forward icon and the label "Forçar estimativa" in the primary text color #f3f5f7, semibold. No other buttons.

Keep the composer unchanged. Gold stays an accent only: the gold left bar and the help icons; no gold background on the button.
```

**Light:**

```text
Screen to edit: "Chat com pergunta antes da estimativa (V2 Light)". It is a copy of "Estimate com botões de ação (V2 Light)". The user logged a dinner; the assistant still has doubts, so it asks before estimating. No estimate is shown yet. This is the assistant's second question.

Conversation, top to bottom (keep the header and the date pill):
1. User bubble, time "20:12": "Jantei macarrão com frango ao molho branco"
2. The "Dieta Bot AI" label, then a question bubble in exactly the style of the existing question bubble (gold left bar #b8873d, help icon in gold, text 18px in the primary text color #14161a, surface #ffffff, 16px radius): "O molho branco levou creme de leite ou requeijão? E o macarrão, foi 1 prato raso ou fundo?" Time "20:12" below it, muted #5c636b, 12px.
3. User bubble, time "20:14": "Creme de leite, prato fundo"
4. The "Dieta Bot AI" label, then a second question bubble in the same style: "O frango foi grelhado ou empanado?" Time "20:15" below it.

Remove the assistant bubble with the "ENERGIA TOTAL" card, the PROTEÍNA / CARBO / GORDURA chips and the "Deseja registrar essa refeição no Café da manhã?" line. Remove the old question "Os pães tinham manteiga ou requeijão?".

Replace the three-button group "Gravar café" | "Trocar" | "Pular" with a single full-width pill button in the same position, same height, same background #ffffff and 1px border #d5d2cc as that group: a fast-forward icon and the label "Forçar estimativa" in the primary text color #14161a, semibold. No other buttons.

Keep the composer unchanged. Gold stays an accent only: the gold left bar and the help icons; no gold background on the button.
```

After sending: two user bubbles, two question bubbles with the gold bar, no estimate card, and the single "Forçar estimativa" button above the composer.

### Prompt 7.2 — the estimate loses the question bubble (`chatE`)

Before sending: prompt 7.1 is done (the copy exists). Select only "Estimate com botões de ação (V2 Expressive)" (dark) or "Estimate com botões de ação (V2 Light)" (light).

**Dark:**

```text
Screen to edit: "Estimate com botões de ação (V2 Expressive)".

1. Remove the separate question bubble ("Os pães tinham manteiga ou requeijão?", with the gold left bar and the help icon) entirely.
2. Move the message time "20:15" to right below the assistant bubble, left-aligned with it, muted #8b939c, 12px, where the question bubble's time was.

Keep everything else exactly as it is: the header, the date pill, the user bubble, the "Dieta Bot AI" label, the assistant bubble with the "ENERGIA TOTAL" card and "Deseja registrar essa refeição no Café da manhã?", the "Gravar café" | "Trocar" | "Pular" button group and the composer.
```

**Light:**

```text
Screen to edit: "Estimate com botões de ação (V2 Light)".

1. Remove the separate question bubble ("Os pães tinham manteiga ou requeijão?", with the gold left bar and the help icon) entirely.
2. Move the message time "20:15" to right below the assistant bubble, left-aligned with it, muted #5c636b, 12px, where the question bubble's time was.

Keep everything else exactly as it is: the header, the date pill, the user bubble, the "Dieta Bot AI" label, the assistant bubble with the "ENERGIA TOTAL" card and "Deseja registrar essa refeição no Café da manhã?", the "Gravar café" | "Trocar" | "Pular" button group and the composer.
```

After sending: the screen shows one estimate bubble with its time below, and no question bubble.

## Verification (agent)

Starts when the owner says the prompts ran. Gate checks: [`st7-pergunta-antes-da-estimativa.checks.json`](st7-pergunta-antes-da-estimativa.checks.json) ([types](../../README.md#verificação-automática-sv1)).

1. `node tools/verify-stitch.mjs st7 --report --out <scratchpad>/stitch-report.html`: finds `chatE` by ID and `chatQ` by exact title, runs the checks at 390 px, dark × light coherence and images, and writes the before/after report. Answers `PASSOU` or `NÃO PASSOU`.
2. `NÃO PASSOU` (including a different title): **stop**, send the report to the owner in one message and keep the state `Aguardando o dono no Stitch`.
3. `PASSOU`: look at the report (before/after) for the appearance items the script does not measure.
4. Add the `chatQ` IDs (Log) to `tools/export-stitch.mjs` and the count (28) to `tools/check-stitch.mjs`.
5. `node tools/export-stitch.mjs --only chatE,chatQ` + `node tools/check-stitch.mjs` green.
6. Add `chatQ.png` to the gold list of `AGENTS.md` (27 → 28) and `docs/qa/README.md`.

### Checklist

- [x] `chatE`: no question bubble; estimate card and Gravar/Trocar/Pular unchanged — `text not "Os pães tinham manteiga ou requeijão?"`, `text has "ENERGIA TOTAL", "Gravar café"`
- [x] `chatE`: time right below the assistant bubble — report
- [x] `chatQ`: title exact in both themes — screen lookup
- [x] `chatQ`: both questions and both user messages — `text has`
- [x] `chatQ`: no estimate, no Gravar/Trocar/Pular — `text not "ENERGIA TOTAL", "Gravar", "Trocar", "Pular", "Deseja registrar"`
- [x] `chatQ`: "Forçar estimativa" on one line, full-width pill above the composer — `fits "Forçar estimativa"` + report
- [x] `chatQ`: question bubbles keep the gold left bar and help icon of the old `chatE` bubble — report
- [x] Gold only as accent; no gold button background — report

## Files this gate may touch

- `docs/qa/stitch/{dark,light}/chatE.png`, `docs/qa/stitch/{dark,light}/chatQ.png`
- `tools/export-stitch.mjs`, `tools/check-stitch.mjs`
- `st7-pergunta-antes-da-estimativa.checks.json`
- Gold list in `AGENTS.md` and `docs/qa/README.md`; name table in `docs/stitch/README.md`

Nothing in `apps/` or `server/`.

## Log

- 30/09/2026 — gate written; waiting for the owner.
- 30/09/2026 — owner ran 7.1 and 7.2 in both themes. `node tools/verify-stitch.mjs st7 --report`: **PASSOU** (every check ok in both themes, dark × light coherent). Report reviewed: `chatE` keeps card, actions and composer, time right under the bubble; `chatQ` has two gold-bar question bubbles with help icon, no estimate, single full-width "Forçar estimativa" pill (fast-forward icon, no gold background) above the composer. New IDs: `chatQ` dark `14d834bd30cd40f7a58b36735e0aded9`, light `40bda18602174f2daee83f684e51f2bc`. `node tools/export-stitch.mjs --only chatE,chatQ` (chatE 0.76%/0.77% px changed; chatQ new) + `node tools/check-stitch.mjs`: 56 golds (28 + 28). Gold list updated in `AGENTS.md` and `docs/qa/README.md`. Unblocks [A30](../../../android/plans/completed/a30-perguntas-antes-da-estimativa.md).
