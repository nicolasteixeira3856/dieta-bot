# Plan — S20 Tali identity in the model instructions

- Status: Concluído
- Date: 05/10/2026
- Owning context: `server`
- Executable boundary: `server/` only. The name lines of the model instructions (`server/llm.py`: `_SCOPE_RULES`, `_CHAT_INSTRUCTIONS` and any other instruction block that names the product), their assembly tests and evaluation cases that assert the name.
- Related documentation: [server Chat specification](../../specifications/v1-chat.md) if it quotes the name; [server README](../../README.md) plan list.
- Prerequisites:
  - approval of [D10](../../../design/plans/completed/d10-fibrai-tali-rename.md), which accepts [ADR-035](../../../produto/adrs/ADR-035-tali-in-app-identity.md);
  - [S19](s19-generalizable-chat-instructions.md) delivered or cancelled, because both edit the same instructions and must not run at the same time.

Authorization and delivery follow [SDD](../../../sdd/README.md). Approval: `Aprovo o plano docs/server/plans/s20-tali-prompt-identity.md. Implemente o plano aprovado.`

## Objective

The model knows it is Tali, the Fibrai assistant. It says so only when asked, and nothing else in its behavior changes.

## Scope

1. **Name lines:**
   - "Dieta Bot" in the scope rule → "Fibrai".
   - "You are Dieta Bot, a meal-tracking chat assistant." → "You are Tali, the meal-tracking chat assistant of the Fibrai app."
   - Add one rule: "Use the name Tali only when the user asks who you are or what this app is; no persona, no greetings beyond the existing rules." Tone rules stay as they are.
2. **Scope copy:** "a question about this app" stays in scope. The fixed refusal copy the server substitutes (ADR-024) does not mention a name today; if discovery finds one, it changes to "Fibrai".
3. **Tests:**
   - `server/tests/test_chat.py` asserts "Tali" and "Fibrai" in the assembled instructions and asserts "Dieta Bot" is absent.
   - Evaluation cases that assert the old name are updated.
   - Add one synthetic eval: "quem é você?" expects a reply that names Tali and stays one short line.
4. **Dev deploy:** after the tests pass, `tools/deploy-gcp.ps1` to the dev VM (ADR-013). This is dev, not production.

## Out of scope

- Any behavior, schema or contract change: no `docs/api-contract.md` change.
- Client copy ([A49](../../../android/plans/completed/a49-fibrai-tali-visible-rename.md)).
- Server identifiers, logger names and the VM (`nutri` stays per [ADR-036](../../../android/adrs/ADR-036-fibrai-technical-identity.md)).

## Validation

1. `server/.venv/Scripts/python -m pytest server/tests` passes.
2. The eval runner passes the existing suite plus the new identity case.
3. A dev-server smoke test: one Chat turn with "quem é você?" through the dev URL returns a Tali reply; one normal meal turn is unchanged.
4. `node tools/check-docs.mjs` passes.

## Results

Implemented on 2026-10-05 on `feat/s20-tali-identity` from master `52eac76` (S19 closed by the owner in [PR #131](https://github.com/nicolasteixeira3856/dieta-bot/pull/131)).

### Delivered

- **Name lines** (`server/chat_instructions.py`, rules `product` and `product_meal_changes`, so both Chat branches):
  - "You are Dieta Bot, a meal-tracking chat assistant." → "You are Tali, the meal-tracking chat assistant of the Fibrai app."
  - New sentence right after it: "Use the name Tali only when the user asks who you are or what this app is; no persona, no greetings beyond the existing rules."
  - "SCOPE: Dieta Bot only helps" → "SCOPE: Fibrai only helps". The same change is in `server/llm.py` `_SCOPE_RULES`, so the estimate and fit routes use it too.
  - The rule owner now cites server Chat 3h. Tone rules, the compact branch and every other rule are unchanged.
- **Refusal copy:** the fixed copy ([refusal-copy](../../../content-policy/specifications/refusal-copy.pt-BR.md)) names no product, so it is unchanged.
- **ADR-033 record:**
  - General rule: the assistant's identity ([ADR-035](../../../produto/adrs/ADR-035-tali-in-app-identity.md)), owned by [server Chat](../../specifications/v1-chat.md) rule 3h.
  - No example and no cue was added or changed; the registry inventory is the one S19 left.
  - The two sentences were written from ADR-035 and this plan, not from any conversation.
- **Tests:**
  - `test_chat_instructions_name_the_assistant_tali` replaces the S7 test. It asserts the Tali sentence, the "only when asked" rule and "SCOPE: Fibrai", and that "Dieta Bot" and "Nutri" are absent from the instructions sent to the model.
  - `test_every_instruction_branch_uses_the_fibrai_names` checks the `legacy` and `meal_changes` branches and the estimate/fit instructions.
- **Evaluation:**
  - New synthetic case `quem-e-voce` (tags `s20`, `identity`): "quem é você?" must be a question with no estimate and no refusal, name Tali, not name "Dieta Bot" or "Nutri", and fit one line of at most 160 characters.
  - `saudacao` gains `reply_not: ["Tali"]`, so a plain greeting must not volunteer the name.
  - New evaluator key `reply_max_chars` (nonblank, one line, at most N characters), covered in `test_evals.py` and documented in the [server README](../../README.md#chat-evaluation).
- **Docs:** server Chat rule 3h and Provenance; the server README (product name, eval key, plan list).

### Validation

1. `server/.venv/Scripts/python -m pytest server/tests`: **329 passed, 384 subtests passed**.
2. Evaluation (`gpt-6-luna`, effort `none`):
   - Identity cases, 3 repetitions each: `quem-e-voce` 3/3, `saudacao` 3/3, `ajuda-do-app` 3/3.
     - Replies to "quem é você?": "Sou a Tali, assistente de refeições do Fibrai." and two close variants.
     - Greeting replies did not name Tali. "o que esse app faz?" was answered with "O Fibrai ajuda a acompanhar refeições…".
   - **Full suite: not completed, owner override.** The provider quota was shared with another agent and rate-limited. The agent ran the suite (192 cases × 3, one worker) through a scratchpad wrapper that only lengthened the rate-limit retries (up to 20, 10 s apart) and counted progress; the evaluator itself was unchanged.
     - At 195/576 repetitions (about 75 minutes) the owner told the agent to stop the tests, complete the plan and merge, and recorded the override ("pode anotar no plano que eu dei override nos testes").
     - Partial counts: 2 repetitions failed a check, 0 ended in an error. The run was stopped before its report was written, so the failing cases are not identified.
     - No comparison with the [S19](s19-generalizable-chat-instructions.md#evidence-on-the-final-prefix) baseline was made. This delivery does not claim the regression suite passed.
3. **Dev deploy and HTTP smoke test: not performed.** The owner said not to deploy for now. The next `tools/deploy-gcp.ps1` from master ships the S19 instructions (never deployed) together with S20. The smoke test of Validation 3 ("quem é você?" and one meal turn through the dev URL) is still owed at that deploy.
4. `node tools/check-docs.mjs`: passed.

### Closure by owner decision (2026-10-05)

The owner closed the plan with the override above. Open items, none tracked as active work:

- the full regression suite on this prefix;
- the dev deploy and its smoke test.
