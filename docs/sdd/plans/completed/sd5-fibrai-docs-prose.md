# Plan — SD5 Fibrai in documentation and skills prose

- Status: Concluído (owner approval on 09/10/2026: "Executa o SD5"; delivered the same day)
- Date: 05/10/2026
- Owning context: `sdd`
- Executable boundary: documentation and skills prose only:
  - the live docs (`README.md`, context READMEs, live specifications, `docs/qa/README.md`, `docs/tokens.md` via its generator source, the tester notice);
  - the four skills trees (`.agents/skills`, `.grok/skills`, `.hermes/skills`, `.claude/skills`).

  No code.
- Prerequisite: [A49](../../../android/plans/completed/a49-fibrai-tali-visible-rename.md) `Concluído`. The app must say Fibrai before the docs describe it that way.

Authorization and delivery follow [SDD](../../README.md). Approval: `Aprovo o plano docs/sdd/plans/sd5-fibrai-docs-prose.md. Implemente o plano aprovado.`

## Objective

Live documentation and skills call the product "Fibrai" and the assistant "Tali". History stays as written.

## Scope

1. Replace "Dieta Bot" with "Fibrai" in live state files and skills prose where it names the product today.
2. **What stays:**
   - accepted ADR bodies and completed or cancelled plans, which are history;
   - skill **names** (`dieta-bot-*`), because renaming a skill changes its invocation, and those names are not product copy;
   - the repo name `dieta-bot`.
3. **`docs/content-policy/legal/tester-notice.pt-BR.md`:** product name → "Fibrai". Name change only, no new disclosure; any other wording change belongs to a content-policy plan.
4. **Tokens docs:** `docs/design/tokens.json` descriptions that say "Dieta Bot" are Figma variable descriptions. Change them in Figma during the next design plan that touches variables. Otherwise leave them, and `docs/tokens.md` stays generated.
5. Synchronize the four skills trees.

## Validation

1. `node tools/check-skills.mjs` and `node tools/check-docs.mjs` pass.
2. Outside history folders, accepted ADRs, `tokens.json`/`tokens.md` and skill names, `grep -rn "Dieta Bot"` returns only intended mentions: the ADR-016/034 references and the repo name.

## Results

Delivered 09/10/2026 on `docs/sd5-fibrai-docs-prose`. Documentation and skills prose only; no code.

- **Skills (four trees, identical):** "Dieta Bot" → "Fibrai" in the `description` and the heading of every skill that named the product (14 skills per tree) and in `android-cli/references/journeys.md`. Skill names (`dieta-bot-*`) unchanged.
- **Live docs:** `README.md` (title and the product-name line, now Fibrai/Tali with ADR-034, ADR-036 and ADR-016 for `nutri`), `AGENTS.md` title (`Fibrai — repo constitution (dieta-bot)`), `docs/README.md`, the `content-policy`, `design`, `produto`, `qa` and `sdd` READMEs, `content-policy/specifications/identity-and-audit.md`.
- **Tester notice** (`content-policy/legal/tester-notice.pt-BR.md`): the five mentions → "Fibrai"; name change only.
- **Left as intended:** accepted ADRs and their references (ADR-016 in `docs/README.md` and the `produto` README), history folders and plans (a line of A61's Results), `apps/android/CHANGELOG.md` (written by `tools/distribute-dev.ps1`; its old entries are history), `docs/design/tokens.json` / `docs/tokens.md` (Figma variable descriptions, scope item 4), the GCP billing account name `Dieta-Bot` in `docs/server/deploy-gcp.md` (a real resource name) and the repository name `dieta-bot`.

### Validation

1. `node tools/check-skills.mjs`: passed (26 skills, 136 files per tree, bytes identical). `node tools/check-docs.mjs`: passed. YAML front matter of the 104 `SKILL.md` files parsed with `name` and `description`.
2. `grep -rn "Dieta Bot"` outside history folders, accepted ADRs and the tokens files: only the intended mentions above.

