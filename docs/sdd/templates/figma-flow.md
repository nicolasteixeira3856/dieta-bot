# Plan — D<n> Release <r>: <Flow>

- Status: Aguardando aprovação
- Date: <DD/MM/YYYY>
- Owning context: `design`
- Affected code: none in `apps/` or `server/`. Figma `Design` → `Release <r>` → section "<Flow>", new components in `Componentes`. Repository: `docs/qa/figma/{dark,light}/{<ids>}.png` and the <Flow> node ids in `tools/export-figma.mjs`.
- Prerequisites: <earlier design plans> `Concluído`.
- Figma MCP budget: ≤ <calls> calls (at most 120 a day, ADR-031 § 6).

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/<file>.md. Implemente o plano aprovado.`

## Objective

Draw the <Flow> states in Aero, in both themes, from the design system, with the features the app has today and nothing more.

## Sources (feature parity, ADR-031 § 4)

| Gold | Reference (current gold or frame, layout only) | Behavior source |
|---|---|---|
| `<id>` | <current Figma frame, or none for a new state> | <live specification> |

Code: `<apps/android/... feature package>`.

## Scope

1. **Discovery (read only):**
   - per gold, list every visible element and classify it: `app` (exists in code or spec), `gold-only` (dropped) or `copy` (exact pt-BR text from the code);
   - the table goes into Results before any write;
   - an element in the spec but missing from the gold is drawn from the spec.
2. **Components** (in `Componentes`, readability rule):
   - <new component>: <variants, tokens>.

   Reuse the existing components and extend them with properties or variants where a state needs it.
3. **Screens** (section "<Flow>" on `Release <r>`):
   - Light row: <ids>, in product order, 390 px wide;
   - Dark row: clones with the `Dark` mode, no other change;
   - frame names `<id> · <screen title> · Light|Dark`; spacing per ADR-031 § 3;
   - the pilot draft of this flow, if any, is replaced and deleted.
4. **Owner review** (Figma review gate):
   - one screenshot per frame is sent to the owner;
   - the plan goes to `pending_manual_validation/` until the owner's OK in Figma;
   - fixes follow, with one screenshot per changed frame.
5. **Export** after the OK:
   - fill the <Flow> ids in `tools/export-figma.mjs`;
   - `node tools/export-figma.mjs --only <ids>`;
   - add new ids to the inventory of `docs/qa/README.md`;
   - `node tools/check-figma.mjs`.

## Out of scope

- Compose (<A-id>). Behavior or copy changes. Other flows.

## Validation

1. The discovery table exists in Results before the first write.
2. A read-back confirms instances (not detached frames) for every DS component, no raw hex fills outside the variables, and no overlapping nodes.
3. Visual: one screenshot per frame; manual owner OK.
4. `check-figma` and `check-docs` pass.

## Results

<Filled at Completion: discovery table, MCP calls used, owner OK date, exported files.>
