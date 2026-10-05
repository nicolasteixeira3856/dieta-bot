# Plan — S20 Tali identity in the model instructions

- Status: Aguardando aprovação
- Date: 05/10/2026
- Owning context: `server`
- Executable boundary: `server/` only. The name lines of the model instructions (`server/llm.py`: `_SCOPE_RULES`, `_CHAT_INSTRUCTIONS` and any other instruction block that names the product), their assembly tests and evaluation cases that assert the name.
- Related documentation: [server Chat specification](../specifications/v1-chat.md) if it quotes the name; [server README](../README.md) plan list.
- Prerequisites:
  - approval of [D10](../../design/plans/completed/d10-fibrai-tali-rename.md), which accepts [ADR-035](../../produto/adrs/ADR-035-tali-in-app-identity.md);
  - [S19](completed/s19-generalizable-chat-instructions.md) delivered or cancelled, because both edit the same instructions and must not run at the same time.

Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/server/plans/s20-tali-prompt-identity.md. Implemente o plano aprovado.`

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
- Client copy ([A49](../../android/plans/completed/a49-fibrai-tali-visible-rename.md)).
- Server identifiers, logger names and the VM (`nutri` stays per [ADR-036](../../android/adrs/ADR-036-fibrai-technical-identity.md)).

## Validation

1. `server/.venv/Scripts/python -m pytest server/tests` passes.
2. The eval runner passes the existing suite plus the new identity case.
3. A dev-server smoke test: one Chat turn with "quem é você?" through the dev URL returns a Tali reply; one normal meal turn is unchanged.
4. `node tools/check-docs.mjs` passes.

## Results

Planning only.
