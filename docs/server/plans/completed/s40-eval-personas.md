# Plan — S40 Eval personas: every model-calling test runs as a fake user

- Status: Concluído (09/10/2026; approved in the owner's session message)
- Date: 09/10/2026
- Owning context: `server`
- Affected code: `server/evals/` only (`personas/`, `run.py`, `cases/*.json`, `checks.py`); documentation: `AGENTS.md` § Delivery pace (rule already recorded by owner decision), [server README](../../README.md) § Chat evaluation. No change in `server/app` or in the model instructions.
- Prerequisites: none. [S41](../pending_manual_validation/s41-onboarding-profile.md), [S42](../s42-extras-and-other-day.md) and every later server plan write their cases against these personas.
- Related documentation: [ADR-057](../../../produto/adrs/ADR-057-conversational-onboarding.md) (the onboarding output is the persona schema), [HTTP contract](../../../api-contract.md) (`profile`, `facts`, `recent`, `recent_days`), [chat benchmark decisions](../../../../benchmark/RESULTADOS_08_10_2026.md).

Authorization and delivery follow [SDD](../../../sdd/README.md). Approval: `Aprovo o plano docs/server/plans/s40-eval-personas.md. Implemente o plano aprovado.`

## Objective

Owner rule of 09/10/2026: every test that calls the model, in dev now and in production later, runs as a named fake user, so that a prompt, product, app or profile-structure change can be measured as real usefulness for a person, not as an isolated string. Dev is for breaking, production never.

## Scope

1. **Persona files.** `server/evals/personas/<id>.json`, one per persona, with the exact request fields a client sends on a normal turn: `profile` (ceiling, targets, eat-back, slots, tone, and `goal` once S41 adds it), `facts` (permanent and dynamic, in the memory schema: restrictions, routines with macros, equipment, preferences, portions), `recent` (7 days of meals in that persona's habit), `recent_days` (the 7 totals, with one or two days over and one without record), `recipes` index when the persona has any, and a `summary` paragraph in pt-BR that a reviewer reads to judge an answer. Seven personas, named by the owner:
   - `nicolas`: the owner's own profile and habits (4 meals, air fryer, no scale at lunch, weak spot dinner, 50 % compensation, `duro`);
   - `ana-deficit`: a woman in deficit with a scale, 5 meals, protein-first, `seco`;
   - `pedro-vegetariano`: vegetarian without a scale, household measures, lactose intolerant, 3 meals;
   - `marta-diabetes`: diabetes, 3 meals plus a fixed snack, avoids sugar, microwave only;
   - `lucas-noturno`: eats late, trains at night, 4 meals with a 23:00 supper, 100 % compensation;
   - `rafael-bulking`: bodybuilder gaining mass, high ceiling, 6 meals, scale, whey, `duro`;
   - `clara-nutri`: a plain nutritionist's diet, 5 meals, no inventions, occasional deviations, `seco`.
2. **Cases reference personas.** A case gets `"persona": "<id>"`; `run.py` merges the persona into `request` before the case's own `request` fields (the case may override `day`, `local_time`, `text`, and add facts). Existing cases keep working: a case without `persona` runs as today. New cases must name a persona (`checks.py` fails a case file without one that is tagged `v6` or later).
3. **Fixture reuse for the app.** `server/evals/personas/` is the single source: the Android fake server used by the capture scripts (`tools/capture-*.sh`) loads the same files for the emulator scenes in a later client plan; this plan only writes them and documents the path.
4. **Rule in the constitution.** `AGENTS.md` § Delivery pace already states the rule (owner decision 09/10/2026): model-calling tests run as a named persona from `server/evals/personas/`; a smoke that cannot name its persona is not run. This plan delivers the files that make the rule executable.
5. **Judging usefulness.** The report of `run.py` prints the persona `summary` next to each failing answer so the Codex judge (decision of 08/10/2026) reads the answer as that person. No automatic score beyond the existing checks.
6. **Documentation at Completion.** [server README](../../README.md) § Chat evaluation describes the personas and the `persona` field; the persona ids and their one-line descriptions live in `server/evals/personas/README.md` (single owner of the list).

## Out of scope

- Any model call. The personas are written by hand from the schema; no answer is generated to build them.
- Rewriting existing cases to personas (they migrate when a plan touches them).
- A persona for the content-policy cases (CP2 fixtures stay as they are).
- The Android fake server (client plan).

## Validation

1. `python -m pytest server/tests -q` passes; a new unit test loads each persona, validates it against the request model of `POST /v1/chat` (HTTP 422 on any field out of limits) and checks that `recent_days` and `recent` are consistent (same dates, totals ≥ the sum of the meals of that day).
2. `python -m evals.run --list` shows the seven personas and the cases that use them; `--dry-run` (no network) merges a persona into a case and prints the request. Zero model calls in this plan (cap: 0).
3. `node tools/check-docs.mjs` passes.

## Results

Delivered on 09/10/2026 in one unattended session (report: `docs/server/validation/s40-2026-10-09.md`). Zero model calls, zero network evals; no change in `server/app` code or in the model instructions, so no dev deploy.

- **Personas (scope 1).** Seven files in `server/evals/personas/` (`nicolas`, `ana-deficit`, `pedro-vegetariano`, `marta-diabetes`, `lucas-noturno`, `rafael-bulking`, `clara-nutri`), hand-written synthetic data. Each is `{id, summary, request}`: `request` is a full normal turn of the current Android client without `text` (`local_time`, `profile`, `facts`, `recent`, `recent_days`, `recipes`, `day` and the capability flags of `PromptBuilder`), all on the reference day Thursday 2026-10-15 with the next meal open. `profile` follows the app encoding (slot ids `"1"`…, `eat_back` `zero` | `partial 50%` | `full`); `goal` waits for S41. `recent` covers the 7 previous days oldest first; `recent_days` newest first, with one or two days over the ceiling and one without record, `over_slot`/`missing_slots` computed the way the app does; the effective ceiling of each day carries the workout credit of the persona's eat-back. Routine facts carry their four numbers. `ana-deficit` and `rafael-bulking` have a recipe index. The owner profile reuses the 2030 kcal / P 165 C 195 G 68 targets of the benchmark with the plan's 50 % compensation and `duro`.
- **Cases (scope 2).** A case may name `"persona"`; `run.load_cases` merges the persona's request under the case's own fields (the case wins; `facts` are added, a same-id fact replaces the persona's) and keeps `persona_summary`. `checks.case_file_errors` refuses a case whose `since` or tag is `v6` or later without a known persona, and an unknown persona on any case; `load_cases` stops before any call on such a file. Seven new smoke cases `s40-<id>-proxima-refeicao` (since `v6`, tags `s40`, `persona`): an open request for the next meal, expecting one `plan` action on the open dinner (supper for `lucas-noturno`) and no refusal; `pedro-vegetariano` also bans meat terms. Not run (cap 0); they are the persona smoke of later plans. Existing cases unchanged.
- **App fixtures (scope 3).** `server/evals/personas/README.md` documents the folder as the single source the capture scripts' fake server will read (A71, A72).
- **Judge (scope 5).** The terminal report prints `persona <id>: <summary>` under each failing case; the JSON report carries `persona` and `persona_summary` per case.
- **Runner.** `--list` (personas, profile line, cases of each) and `--dry-run` (merged request of the selected cases, validated by `ChatIn`/`CloseIn`) run before the key is read and make no request.
- **Docs (scopes 4, 6).** `AGENTS.md` already carried the rule; unchanged. Server README § Chat evaluation gains § Personas and the `v6`/persona case fields; the persona list and one-line descriptions live only in `server/evals/personas/README.md`. Links to this plan from S41, S42, A71, A72 and the design and product indexes point to history.
- **Unit tests:** `server/.venv/Scripts/python -m pytest server/tests -q` → 638 passed, 595 subtests. New `test_s40_personas.py` (12): the seven ids and the README list agree; each persona validates against `ChatIn` (the model of `POST /v1/chat`, whose failures are the HTTP 422) and an out-of-limit fact is rejected; `recent` and `recent_days` share the recorded dates and each day's totals are ≥ the sum of its meals, newest first, a day over and a day without record; today's eaten total adds up with a meal open; merge rules; the `v6` persona rule; every case file loads; `--list` and `--dry-run` return 0 without a call; a failing persona case prints its summary. `test_evals.py` accepts since `v6`.
- **Runner checks:** `--list` → 7 personas, 7 persona cases, 319 cases without persona; `--dry-run --tag s40` → 7 merged requests.
- `node tools/check-docs.mjs` passes.
