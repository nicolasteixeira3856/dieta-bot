# Plan — S26 Reply formatting subset

- Status: Aguardando aprovação
- Date: 06/10/2026
- Owning context: `server`
- Executable boundary: `server/` only: one formatting rule in `chat_instructions.py` (both capability branches), a pure shaping module for the subset, the reply limits, the dev log, tests and evaluation checks. No route signature change; `reply` stays a string.
- Related documentation: [ADR-045](../../produto/adrs/ADR-045-rich-replies-in-chat-bubbles.md) (accepted on 2026-10-06 with the approval of D17 and this plan), [ADR-042](../adrs/ADR-042-estimate-total-is-server-arithmetic.md), [server Chat specification](../specifications/v1-chat.md), [HTTP contract](../../api-contract.md), [content policy](../../content-policy/specifications/content-policy.md).
- Prerequisites: [S24](s24-protein-first-plan.md) delivered (its two-option and closing-line replies are what the subset formats). Independent of S25.

Approving this plan accepts ADR-045. Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/server/plans/s26-reply-formatting-subset.md. Implemente o plano aprovado.`

## Objective

The reply carries the ADR-045 subset and nothing outside it: bold on the decisive numbers, the dish and a verdict; bullets for options, ingredients and foods to avoid; numbered steps for preparation; one two-column table of at most six rows for portions.

## Scope

### 1. Instructions

- A `format` rule, last in both branches before the tone block of S25 when it exists: the subset, where each marker is allowed, and the bounds (bold only on the numbers that decide, the dish name and a one-word verdict; bullets for options, ingredients and foods to avoid; steps only in a recipe; one table, `| Item | Gramas |`, at most six rows; nothing else). Question-only replies, refusals and the compact digest carry no markers.
- ADR-033 record in Results; no example added.

### 2. Shaping (new module `server/reply_format.py`)

- Pure function over `reply`: keeps the subset; removes any other markup (headings, links, images, code fences, inline code, emphasis with `_` or single `*`, nested lists, a second table, HTML); converts a table beyond six rows into bullets; strips bold from a sentence that is entirely bold; collapses more than two consecutive blank lines. The reply the app receives is always inside the subset.
- Runs after the ADR-042 total rewrite (a rewritten total keeps its bold) and before output moderation.
- Refusal copy, the fallback line and the compact digest never pass through it (they have no markers).
- Dev log: `format_stripped` (count of removed markers) or null. Numbers only.

### 3. Limits

- Reply limits (`reply_max_chars` in the evaluator and any server cap) are rechecked with the markers: the plan's two options with bullets and a closing line per slot must fit. The limit changes only if a measured case needs it, recorded in Results.

### 4. Evaluation

- Checks: `reply_format` (markers inside the subset only; bold count at most the number of numbers plus two; at most one table) added to `server/evals/checks.py` with unit tests.
- Cases, tag `s26`: a plan with two options (expect bullets and bold totals); a recipe (expect the table and numbered steps); a log (expect bold on the total only); a question-only turn (expect no marker); a model reply with a heading and a link (unit test with a fake transport: both removed).
- Regression: `--tag s24 --repeat 3`, `--tag s22 --repeat 3`, then the full suite at `--repeat 1` against the latest full run.

### 5. Dev deploy

After validation: `tools/deploy-gcp.ps1`, code only; smoke: one plan, one recipe, one question.

### Specification changes at Completion

- [v1-chat](../specifications/v1-chat.md): rule 3 (format rule), rule 5 (shaping step and what is removed), observability. Provenance line.
- [HTTP contract](../../api-contract.md): the `reply` subset, as data the client may render.
- ADR-045 status to Accepted (if not already by D17 or A57).

## Out of scope

- The client renderer (A57), the golds (D17), tone (S25), production (blocked by the [production gate](../../content-policy/production-gate.md)).

## Validation

1. `server/.venv/Scripts/python -m pytest server/tests -q` passes, including `test_reply_format.py` (every removal rule, the six-row table, bold on rewritten totals, refusal and compact untouched).
2. Evaluation as in scope 4; the `s26` cases 3/3 each.
3. Dev deploy and smoke; request ids recorded.
4. `node tools/check-docs.mjs` passes.

## Results

Planning only.
