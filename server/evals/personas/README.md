# Eval personas

Named fake users for every test that calls the model (owner rule of 09/10/2026, [AGENTS.md § Delivery pace](../../../AGENTS.md#delivery-pace-owner-decision-08102026), plan [S40](../../../docs/server/plans/completed/s40-eval-personas.md)). This file is the single owner of the persona list.

## File

`<id>.json`, the file name is the id:

- `id`: the persona id.
- `summary`: one pt-BR paragraph a reviewer reads to judge an answer as that person; the runner prints it next to each failing answer and writes it into the report.
- `request`: a full `POST /v1/chat` body without `text`, as the Android app sends a normal turn ([HTTP contract](../../../docs/api-contract.md)): `local_time`, `profile`, `facts`, `recent` (the 7 days before `day.date`, oldest first), `recent_days` (newest first, one day over the ceiling or more, one day without record), `recipes`, `day` (today with the next meal still open) and the current capability flags.

All personas share the same reference day, Thursday 2026-10-15; a case that needs another moment overrides `local_time` and `day`.

## Use in a case

A case names `"persona": "<id>"`. The runner merges the persona's `request` under the case's own `request` fields: every case field replaces the persona's (`text`, `day`, `local_time`, flags, `recent`), except `facts`, which are added (a case fact with a persona fact's id replaces it). A case of version `v6` or later (its `since` or a tag) must name a persona; the runner refuses to start otherwise. Older cases without `persona` run as before.

```bash
cd server
.venv/Scripts/python -m evals.run --list                         # personas and their cases, no key, no network
.venv/Scripts/python -m evals.run --dry-run --tag s40            # merged requests, no key, no network
```

Each persona has one smoke case, `s40-<id>-proxima-refeicao` (tags `s40`, `persona`): an open request for the next meal, and one onboarding case, `s41-<id>-perfil` (tags `s41`, `persona`, `"route": "profile"`): the answers that person would give in the onboarding, sent to `POST /v1/profile`; a profile case keeps its own request and takes only the persona's `summary`. The Android fake server of the capture scripts will read the same files in a later client plan; this folder stays their single source.

## Personas

| Id | Who |
| --- | --- |
| `nicolas` | the owner: 4 meals, air fryer, no scale at lunch, dinner is the weak spot, 50 % compensation, `duro` |
| `ana-deficit` | woman in deficit with a scale, 5 meals, protein first, `seco` |
| `pedro-vegetariano` | vegetarian without a scale, household measures, lactose intolerant, 3 meals |
| `marta-diabetes` | type 2 diabetes, 3 meals plus a fixed snack, avoids sugar, microwave only |
| `lucas-noturno` | eats late, trains at night, 4 meals with a 23:00 supper, 100 % compensation |
| `rafael-bulking` | bodybuilder gaining mass, high ceiling, 6 meals, scale, whey, `duro` |
| `clara-nutri` | a plain nutritionist's diet, 5 meals, no inventions, occasional deviations, `seco` |

Personas are hand-written synthetic data: no tester log, no generated answer. A change to a persona is a change to every case that runs as it; record it in the plan that makes it.
