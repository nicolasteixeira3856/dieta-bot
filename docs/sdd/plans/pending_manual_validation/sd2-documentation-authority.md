# Plan — SD2 Documentation authority and lifecycle

- State: `Pendente aprovação manual`
- Date: 2026-10-02
- Owner: `sdd` — shared agent workflow maintenance.
- Production code affected: none.
- Prerequisites: none. [SD3](../sd3-docs-checker-and-ignores.md) depends on this plan.

## Authorization gate

This plan is documentation only. Implementation starts only after explicit approval identifying this file:

> Aprovo o plano `docs/sdd/plans/sd2-documentation-authority.md`. Implemente o plano aprovado.

One agent executes it. `apps/`, `server/`, `infra/` and `tools/` are outside its scope. If a step would require a product or architecture decision, record it under Results and stop for the owner.

## Objective

An agent learns the current state of the app from current-state files alone, without replaying plan or ADR history, and those files stop drifting because each fact has one writer.

The review in `MELHORIA_PROCESSO_SDD.md` (owner question, five model reviews, synthesis, 2026-10-02) found that the cost is not the number of files. It is **mixed files**: documents meant to describe the present that also carry history, proposals or copies of facts owned elsewhere. Completed plans are labeled by their folder and stay.

## Owner decisions recorded by this plan (2026-10-02)

1. **Two kinds of file.**
   - *Current-state* files (`AGENTS.md`, `README.md`, context READMEs, specifications, `docs/api-contract.md`, `docs/tokens.md`, `docs/qa/README.md`) are rewritten in place. They hold no history, no proposals and no status of other artifacts.
   - *History* files (plans, ADR bodies, validation evidence) are immutable after closure.
2. **Single writer.** Each fact has one owning file. Other files link to it instead of restating it.
3. **Specification lifecycle.** Planning describes the intended spec change inside the plan. Completion rewrites the affected spec rules in place.
   - Specs carry no "proposed" or "pending" banners.
   - When client and server ship at different times, the spec states the shipped behavior with a rollout qualifier.
4. **ADRs.** The decision body stays immutable. The status line is updated when a successor is accepted, and it may name partial supersession. Specs cite the ADRs behind their rules, so the ADR chain is read only for "why" questions.
5. **Completed plans** stay where they are, unchanged, off the default read path. Delivery evidence stays in each plan's Results. No per-plan validation file.
6. **Removals.**
   - Delete `GOALS.md`, `SETUP.md`, `DECISOES.md`, `README-EXTRACT.md`, `docs/TEAM.md`, `docs/AGENTS-TEAM-SNIPPET.md` and `docs/MIGRACAO-VPS.md`, and rewrite the root `README.md`.
   - Second owner message, same day: also delete `docs/SETUP-WINDOWS.md`, `docs/HERMES.md`, `wires/` (4 files) and `.grok/agents/README.md`.
   - With `wires/` gone, wireframes stop being an allowed layout reference. UI starts from Stitch golds only.
   - Git history keeps the old text.
7. **No RAG** or vector index. Revisit only if an acceptance question below fails after SD2 and SD3.

## Sources of truth

- [AGENTS.md](../../../../AGENTS.md), [SDD policy](../../README.md), [documentation matrix](../../../README.md).
- Code facts used to correct copies:
  - `server/main.py` routes (`/health`, `/v1/estimate`, `/v1/fit`, `/v1/chat`);
  - `apps/android/app/src/main/java/com/nutri/android/core/database/DietaBotDatabase.kt` (schema version and tables);
  - `tools/export-stitch.mjs` (`DARK_SCREENS`, `LIGHT_SCREENS`).
- The status line of each ADR.
- `MELHORIA_PROCESSO_SDD.md`, the review discussion. It is not a source of product truth.

## Discovery evidence

Verified at `5e41fcf`.

| Location | Finding | Fix in this plan |
| --- | --- | --- |
| `README.md` | Says "Nutri", "Home tower + Cloudflare Tunnel. No VPS", screens T0–T3, `docker compose` on the tower. Contradicts ADR-012, ADR-013 and ADR-016. | Rewrite (step 8). |
| `GOALS.md`, `SETUP.md`, `README-EXTRACT.md` | Flutter goals, Flutter setup, wire-based `/goal 10`. | Delete. |
| `DECISOES.md` | Indexes `decisoes/`, which does not exist. | Delete. |
| `docs/TEAM.md`, `docs/AGENTS-TEAM-SNIPPET.md` | Retired; TEAM names "AGENTS.md e GOALS.md" as constitution and puts new ADRs in `docs/decisions/`. | Delete. |
| `docs/MIGRACAO-VPS.md` | Tombstone pointing to ADR-013. | Delete. |
| `docs/SETUP-WINDOWS.md` | One-time setup log (raw output, Flutter on PATH). | Delete. |
| `docs/HERMES.md` | Model-switching notes; mentions `TUNNEL_TOKEN` from the tower era. | Delete. |
| `.grok/agents/README.md` | Tombstone for retired personas; only file in `.grok/agents/`. | Delete. |
| `wires/`, `AGENTS.md:89`, `.stitch/DESIGN.md:11`, `dieta-bot-android-ui` skill | Wireframes kept as a fallback layout reference; Stitch golds are the only implemented source. | Delete `wires/`; remove the fallback wording. |
| `AGENTS.md` Live stack | Contract listed as `/health`, `/v1/estimate`, `/v1/fit`; `/v1/chat` missing. | Link to the contract. |
| `AGENTS.md` Live stack | Room "for profile + day + meal_log"; the database has 7 tables. | Link to the Room spec. |
| `AGENTS.md` Product | "Screens: … Nothing else" names 19 ids; the gold list in the same file has 31. | Rule plus link to the gold inventory. |
| `AGENTS.md` Visual QA, `docs/qa/README.md` | Gold list kept in two files; `docs/qa/README.md` says "18 telas". | `docs/qa/README.md` owns the list. |
| `AGENTS.md` Tokens, `docs/tokens.md` | Same hex values in both. | `docs/tokens.md` owns values. |
| `docs/produto/README.md:5,36` | ADR-024 "proposed"; "Sem especificacao viva … Sem ADR local". ADR-024 is Accepted; 4 specs and local ADRs exist. | Correct by link. |
| `docs/produto/specifications/chat.md:3–5` | CP2 overlay "pending … not current behavior"; CP2 is deployed per the content-policy spec and `v1-chat.md`. | Rules-only rewrite. |
| `docs/server/specifications/v1-chat.md:169` | ADR-028 "proposto"; ADR-028 is Accepted. | Rules-only rewrite. |
| `docs/android/README.md:38,51,46` | "Sem specifications/"; "Room v7"; route sends Room questions to ADR-010. The spec and code say v8. | Route to the Room spec. |
| Specifications | "desde o plano X" narratives: chat 27 plan links, v1-chat 20, memoria-push 14, content-policy 8, home-timeline 6, perfil-onboarding 4, identity-and-audit 3, room 2, refusal-copy 1. | Rules-only rewrite with Provenance section. |
| `docs/README.md`, context READMEs | Completed-plan lists with prose (`docs/README.md:58–102`, `android/README.md:124–182`, and similar in server, stitch, content-policy). | One link to `plans/completed/`. |
| `docs/decisions/001–011` | No status line; 003 (RN) and 011 (T2/T3) describe dead flows. | Add status lines. |
| Skills | `dieta-bot-android-memory` updates the live spec at decision time. `android-architecture` reads the Room spec "with the accepted changes linked from the Android index". `docs/sdd/templates/stitch-gate.md:58` and the SDD Gate Stitch section update two gold lists. | Align with the new lifecycle and single writer. |
| `docs/sdd/templates/plan.md` | 11 sections, repeated verbatim in every plan; a 2 KB plan still opens 9 sections. | Slim template. |

## Implementation scope

### 1. Baseline, before any edit

For each acceptance question below, walk the default route (AGENTS → `docs/README.md` → context README → owning spec → cited ADR). Record in Results:

- the files needed to answer correctly;
- their bytes;
- whether `plans/completed/` had to be opened.

Also record the bytes of `AGENTS.md`, `docs/README.md` and every context README.

### 2. Constitution — `AGENTS.md`

- **Live stack:** `server/` links to [the HTTP contract](../../../api-contract.md) instead of listing routes. The Room line says Room 2.6.x for local state, with the schema owned by the Room spec. "No DataStore for day state" stays.
- **Product:**
  - Screens: ADR-012 and its accepted successors; every product screen has a gold listed in `docs/qa/README.md`; nothing else.
  - Every other product rule stays.
- **Tokens:**
  - Keeps: two themes, follow the system, no dynamic color, no wallpaper, no toggle.
  - Colors and type/shape values move to `docs/tokens.md`, which becomes their single owner. The skills already link it.
- **Visual QA:**
  - The gold filename list becomes a link to `docs/qa/README.md`.
  - Remove "Layout creation can use wireframes as initial reference (prefer Stitch via MCP, fallback to wireframes)".
  - In *Do not*, "implementation based on wires instead of Stitch gold PNGs" becomes "UI implementation from any source other than the Stitch gold PNGs".
  - The "Do not ignore" list keeps its items by name ("remaining size", "sheet radius", "CTA color"), with values in tokens.
- **How to work:** add two rules.
  - Current state is read from AGENTS → `docs/README.md` → context README → owning spec. `plans/completed/` and `plans/cancelled/` are history, opened only for provenance, a Stitch gate check or an explicit history question.
  - Single writer: state a fact only in its owning file; link elsewhere.
- **Unchanged:** every gate, prohibition, formula, production-gate trigger, approval rule and delivery rule. Language stays English.

### 3. SDD policy and templates

In `docs/sdd/README.md`, keeping its language:

- **Precedence:** replace "Decisão explícita que muda comportamento documentado atualiza a spec viva na etapa de Planning" with the Completion rule.
- **Artifact responsibility:**
  - Add the current-state/history distinction and the single-writer rule.
  - The Validation row covers cross-plan evidence. Single-plan evidence lives in the plan's Results.
- **Discovery:** step 4 lists active plans in `plans/`, `pending_manual_validation/` and `out_of_scope/`. Completed and cancelled plans are opened only for the reasons in the AGENTS rule.
- **Planning:**
  - Step 2 becomes "describe the intended spec change in the plan".
  - Step 6 indexes only active state.
- **Completion:**
  - Step 2 rewrites the affected spec rules in place, with a rollout qualifier when deployment is partial, adds the plan to the spec's Provenance, and removes any wording made stale by the delivery.
  - Context READMEs list only active, pending and out-of-scope plans.
- **ADRs:**
  - The body is immutable.
  - The status line is updated when a successor is accepted and may name partial supersession.
  - The successor states the surviving scope of the predecessor.
- **Gate Stitch:** the gold list is updated only in `docs/qa/README.md`, not in `AGENTS.md`.
- **Link maintenance:** READMEs link the `plans/completed/` directory, not individual completed plans.

Templates:

- **`plan.md`:**
  - Header (state, date, owner, affected code, prerequisites).
  - One authorization line linking SDD.
  - Goal, Scope, Out of scope, Validation, Results.
- **`specification.md`:** rules in present tense, then a trailing `## Provenance` with `- [ID](path) — title` entries and no status words.
- **`context-readme.md`:** routes to specs, active plans, out-of-scope plans and the ADR index; one link to `plans/completed/`; no version or status copies.
- **`adr.md`:** a required `- Status:` line, with the supersession wording above.
- **`stitch-gate.md:58`:** gold list in `docs/qa/README.md` only.

### 4. Specifications: rules only

All nine specs under `docs/*/specifications/`:

- Remove "Estado", "desde o plano X", proposal banners and other narrative.
- State each rule in the present tense.
- ADR links that justify a rule may stay inline.
- Plan links move to a trailing `## Provenance` (`## Proveniência` is accepted in pt-BR files).
- Add a rollout qualifier where client and server versions differ (pattern: "Sem cliente v4 …" in `v1-chat.md`).
- `chat.md` states the deployed content-policy behavior by linking the content-policy spec.

Keep every rule. Before editing each spec, number its rules. After editing, record in Results a before → after count and any rule that could not be placed. A conflict between a spec and code or an ADR is recorded as a finding for the owner, never resolved by choosing one.

The filename `room-v2.md` stays (see Out of scope).

### 5. Indexes and READMEs

- **`docs/README.md`:**
  - The matrix "Plano ativo" column lists only active, pending and out-of-scope plans, plus a `plans/completed/` link.
  - The numbered completed list is removed.
  - The ADR table drops status parentheticals (status lives in each ADR).
  - The "Outros docs" table drops the deleted files.
- **Context READMEs** (`produto`, `android`, `server`, `stitch`, `content-policy`, `sdd`) and `plans/README.md` indexes: the same rule. In particular:
  - correct `produto/README.md:5,36`;
  - correct `android/README.md:38,46,51`;
  - keep `out_of_scope/` and the [production gate](../../../content-policy/production-gate.md) on the route.
- **`docs/qa/README.md`:** becomes the owner of the gold inventory (exact ids per theme, matching `tools/export-stitch.mjs`); the counts "18 telas" go away.

### 6. ADR status lines

- **`docs/decisions/001–011`:** insert one `- Status:` line under the title, with values determined from successors and recorded in Results. Expected:
  - 003 superseded by 005;
  - 011 superseded (flow removed in A12);
  - 010 accepted, with implementation details governed by the Room spec;
  - the rest accepted.
- **Context ADRs that are partially replaced** (e.g. ADR-012 and ADR-017 by ADR-028; ADR-015 by ADR-024): confirm the successor names the surviving scope, and add the partial supersession to the predecessor's status line.
- No body edits.

### 7. Skills, four trees

- **`dieta-bot-android-memory`:** durable decisions go to the plan and, if architectural, an ADR at Planning; specs change at Completion.
- **`android-architecture`:** read the Room spec; drop "with the accepted changes linked from the Android index".
- **`dieta-bot-stitch` and any skill restating the gate's gold-list step:** `docs/qa/README.md` only.
- **`dieta-bot-android-ui` and any skill mentioning wireframes:** remove wireframes as a design reference.
- Any other skill sentence that restates the old lifecycle or a value now owned elsewhere.
- Apply identical bytes in `.agents`, `.grok`, `.hermes` and `.claude`, then run `node tools/check-skills.mjs`. Only tracked files are edited, so the current `.gitignore` entries do not block the commit (SD3 removes them).

### 8. Removals and root README

- **Delete** every file in owner decision 6, and remove or repoint every live reference to them:
  - `docs/README.md` "Outros docs" (TEAM, HERMES, MIGRACAO-VPS, wires rows);
  - each other's mutual references.
- **`.stitch/DESIGN.md:11`:** remove the wireframe fallback sentence (repository copy only; the owner re-uploads to Stitch if needed).
- The wire export tools in `tools/` become orphaned; [SD3](../sd3-docs-checker-and-ignores.md) deletes them, because `tools/` is outside this plan's boundary.
- **Exempt history:** links and mentions in completed plans and in immutable ADR bodies stay as history: ADR-013 (`MIGRACAO-VPS.md`), ADR-007 and ADR-008 (`wires/`, `export-wires.mjs`). `docs/qa/_legacy/` is not touched.
- **Rewrite `README.md`** (target ≤ 2 KB, no copied stack details, versions, screens or infra):
  - name (Dieta Bot, technical id `nutri` per ADR-016);
  - one-paragraph job;
  - "Start here" links to `AGENTS.md`, `docs/README.md`, `docs/server/deploy-gcp.md` and `docs/content-policy/production-gate.md`;
  - folder map, without `wires/`.
- **Delete `MELHORIA_PROCESSO_SDD.md`.** It must already be in git history through the planning commit of SD2/SD3; if it is not, stop and ask the owner.

### 9. Close the plan

- Record Results.
- Move this plan by the lifecycle rules.
- Update `docs/sdd/plans/README.md` and `docs/sdd/README.md`.
- Deliver through the git flow. Commits by area: constitution and SDD, specs, READMEs, ADR status lines, skills, removals.

## Out of scope

- Editing, moving, renaming or deleting any plan in `completed/`, `cancelled/` or `out_of_scope/`.
- ADR bodies.
- Renaming `room-v2.md`: completed plans and ADRs link it, and their bodies are not edited. The title, not the filename, carries the version.
- Any product, formula, screen, contract or behavior decision; any code under `apps/`, `server/`, `infra/` or `tools/`.
- Translating `docs/sdd/README.md` or other docs.
- Moving procedures (Stitch gate, git delivery, test builds) out of `AGENTS.md`.
- Wire tooling in `tools/` (`export-wires.mjs`, `check-wires.mjs`, `export-painel-wires.mjs`): deleted by SD3.
- `docs/qa/_legacy/`.
- `tools/check-docs.mjs`, `.gitignore`, `.ignore`: [SD3](../sd3-docs-checker-and-ignores.md).
- RAG, embeddings or any index service.

## Validation

**Automated (agent):**

1. `node tools/check-skills.mjs` passes.
2. `node --test tools/check-skills.test.mjs` passes.
3. A scratch link check (not committed) over every changed or live Markdown file reports zero broken relative links, except the recorded history links in completed plans and the ADR-013 body.
4. `git grep` finds no live reference to the deleted files outside completed plans and ADR bodies.
5. No specification contains a plan link outside its Provenance section.
6. Every file in `docs/decisions/` and `docs/*/adrs/` has a status line in its first lines.
7. The step 1 route walk is repeated. Record files, bytes and whether `plans/completed/` was opened, next to the baseline.

**Manual (owner):** in a fresh session of any agent, ask the four acceptance questions. Answers must be correct and reached without opening `plans/completed/`. Until the owner confirms, the plan is `Pendente aprovação manual`.

## Acceptance questions

1. What does Chat do with a photo plus a question?
2. Which Room schema version is live, and which tables does it have?
3. Which production blockers are open?
4. What does ADR-012 still govern after ADR-028?

## Acceptance criteria

- No live document contradicts a fact owned by another file; the Discovery table is closed row by row.
- `AGENTS.md` no longer enumerates routes, tables, screens, token values or gold filenames.
- Specs contain rules plus a Provenance section; rule counts before and after are recorded.
- READMEs list no individual completed plan.
- Every file in owner decision 6 is deleted, `README.md` is rewritten, and `MELHORIA_PROCESSO_SDD.md` is removed with its content preserved in history.
- SDD policy, templates and skills describe the same lifecycle.
- The route walk answers all four questions without `plans/completed/`, with bytes recorded.

## Risks and controls

- **A rule is lost in the spec rewrite:** numbered before/after mapping per spec, recorded in Results; unplaced rules stop the step.
- **Lifecycle change while a plan is in flight:** no active plan exists at planning time. If one exists at implementation start, it keeps the old lifecycle and the change is recorded.
- **Skills revert the lifecycle:** the skills are updated in the same delivery and `check-skills.mjs` gates the mirror.
- **Large diff:** commits by area; the PR summary lists each area.

## Results

Implemented 2026-10-02 on branch `docs/sd2-documentation-authority`, from `master` @ `51cdded`. Automated validation passed; the owner's fresh-session check is pending.

### Route walk (bytes on the default route, `git show 51cdded` vs working tree)

| Question | Files on the route | Before | After | Stale text met before |
| --- | --- | --- | --- | --- |
| 1. Chat, photo plus a question | AGENTS, `docs/README.md`, produto README, `chat.md`, `v1-chat.md` | 73,725 | 62,576 (−16 %) | `chat.md` banner: content policy "pending … not current behavior"; `v1-chat.md`: ADR-028 "proposto" |
| 2. Room version and tables | AGENTS, `docs/README.md`, android README, Room spec | 47,793 | 34,050 (−29 %) | AGENTS "profile + day + meal_log"; android README "Room v7", "Sem specifications/", route to ADR-010 |
| 3. Open production blockers | AGENTS, `docs/README.md`, content-policy README, production gate | 35,006 | 26,593 (−25 %) | none wrong; long status narratives |
| 4. ADR-012 after ADR-028 | AGENTS, `docs/README.md`, produto README, ADR-012, ADR-028 | 44,364 | 37,240 (−17 %) | ADR-012 had no supersession note; the reader had to open every successor |

None of the four answers needed `plans/completed/`, before or after. Before, two answers met contradicting text on the way; after, none. Entry points: `docs/README.md` 12,702 → 6,262 B; android README 14,888 → 8,296; server README 11,398 → 4,963; stitch README 12,107 → 9,535; content-policy plans index 4,238 → 1,204. `docs/sdd/README.md` grew 15,496 → 18,007 (new rules). Specifications 88,542 → 82,960 B.

### Discovery table

Every row is closed. Additional contradictions found and fixed during implementation:

- `docs/server/README.md`: "Compose + Cloudflare Tunnel na torre (dev)"; ADR-028 "proposto"; "Docker nao muda neste plano".
- `docs/stitch/README.md`: ADR-028 "(proposto)"; gold list owned by AGENTS.
- `docs/content-policy/README.md`: "proposed architecture decisions … not deployed".
- `docs/android/README.md`: "ADRs vigentes … 011".
- `docs/qa/README.md`: "18 telas", "36 telas"; link to the deleted `SETUP-WINDOWS.md` (now points to the `android-cli` skill).
- Room spec: "pending owner/tester update validation" (a status copy) replaced by a link to the A24 validation.

### Specifications (numbered rules before → after)

| Spec | Rules | Notes |
| --- | --- | --- |
| `produto/specifications/chat.md` | 20 → 21 | Rule 21 states the deployed content-policy behavior that was in the "proposed overlay" banner. Fixed `teclado.amp;` typo. 28 plans in Proveniência. |
| `server/specifications/v1-chat.md` | 29 → 29 | Facts that lived only in "Estado" moved into the body: the 2000-character `text`/`messages[].text` limit (IN), the `X-Client-Instance-Id` header and content controls (Escopo), the no-`clarify_rounds` qualifier (rule 5c), production audit out of scope. 14 plans in Proveniência. |
| `produto/specifications/home-timeline.md` | 11 → 11 | 48 dp disclaimer clearance (rule 10) and `home1` canonical layout moved from "Estado". |
| `produto/specifications/memoria-push.md` | 28 → 28 | `filesDir/memory.bin` path, dev editor location and profile editing (rule 7), ripple (Config rule 9) moved from "Estado". |
| `produto/specifications/perfil-onboarding.md` | 7 → 7 | "Room v2" copy dropped. |
| `android/specifications/room-v2.md` | 13 → 13 | "Status" narrative → "Schema version 8". |
| `content-policy/specifications/content-policy.md` | 7 → 7 | Root-cause narrative → one-line design principle; plan links to blockers → the production gate. |
| `content-policy/specifications/identity-and-audit.md` | — | "Status" narrative → authority line; CP5 sentences in present tense with their owners (`infra/gcp/compose.yml`, `deploy-gcp.md`). |
| `content-policy/specifications/refusal-copy.pt-BR.md` | — | CP2 references moved to Proveniência. |

No rule was dropped. No spec/code conflict required an owner decision.

### ADR status lines

- `docs/decisions/` (new status lines):
  - 001: partially superseded by 005.
  - 002: partially superseded by 004.
  - 003: superseded by 005.
  - 004, 005, 007: accepted.
  - 008: partially superseded by AGENTS § Visual QA and ADR-027.
  - 009: superseded by ADR-012 and AGENTS § Visual QA.
  - 010: accepted; schema in the Room spec.
  - 011: superseded by ADR-012.
- Status lines extended with partial supersession:
  - ADR-012, by ADR-017, 018, 020, 021 and 028;
  - ADR-017, by ADR-028;
  - ADR-020, by ADR-022;
  - ADR-015, by ADR-024.

### Skills

The four trees are identical. Edited:

- `dieta-bot-android-memory` (lifecycle, single writer);
- `dieta-bot-android-decision` (spec at Completion);
- `android-architecture` and `room-ksp-coroutines` (Room spec states the current schema);
- `dieta-bot-android-ui` (no wireframes);
- `dieta-bot-stitch` (gold inventory in `docs/qa/README.md`, no wireframes).

### Removals

Deleted:

- `GOALS.md`, `SETUP.md`, `DECISOES.md`, `README-EXTRACT.md`;
- `docs/TEAM.md`, `docs/AGENTS-TEAM-SNIPPET.md`, `docs/MIGRACAO-VPS.md`, `docs/SETUP-WINDOWS.md`, `docs/HERMES.md`;
- `wires/` (4 files), `.grok/agents/README.md`;
- `MELHORIA_PROCESSO_SDD.md` (in history at `52caa34`).

`README.md` was rewritten (1,234 B). The wireframe fallback was removed from `.stitch/DESIGN.md`. History mentions of deleted files remain in completed plans and in the bodies of ADR-007, ADR-008 and ADR-013.

### Validation

- `node tools/check-skills.mjs`: pass, 26 skills / 136 files in each of the four roots.
- `node --test tools/check-skills.test.mjs`: 14/14 pass.
- Scratch link check over `AGENTS.md`, `README.md` and every live `docs/**/*.md` (not committed): zero broken relative links after this move.
- `git grep` for the deleted names outside completed plans, ADR bodies and `docs/qa/_legacy/`: only `tools/package.json` ("wireframes" in its description), which is SD3's boundary.
- No specification has a plan link outside its Provenance section. Every file in `docs/decisions/` and `docs/*/adrs/` has a status line.
- No live line links an accepted ADR as proposed or a completed plan as pending. No README claims "no specification" next to a non-empty `specifications/`.
- `git diff --check`: clean.

Pending (owner): in a fresh session of any agent, ask the four acceptance questions. Answers must be correct and must not open `plans/completed/`.

### Candidates for later (not in scope)

- `docs/api-contract.md` opens with a history banner ("Content controls (CP2, 2026-09-30, …)").
- Some rules keep plan IDs as plain text in rollout qualifiers ("antes do S14", "(S15)"). They are not links and carry meaning; a later spec rewrite can phrase them by client version.
- `docs/content-policy/operations/*` and `docs/content-policy/validation/README.md` cite completed plans as evidence. That is allowed: they are evidence, not current-state files.
