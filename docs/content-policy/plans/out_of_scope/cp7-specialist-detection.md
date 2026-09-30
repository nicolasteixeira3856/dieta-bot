# CP7 — Specialized illegal-image detection

- Status: Fora de escopo
- Date: 2026-09-30
- Owner: `content-policy`
- Prospective executable boundary: `server/`; any independent infrastructure/client integration needs a separate plan.
- Authority: owner instruction on 2026-09-30 to plan within current scope/budget and allow future implementations in `out_of_scope/`.
- Reason: financial and business scope. Additional specialized detection platforms may not fit the present operating budget/capacity.
- This plan is not approved, implemented, cancelled or runnable with `/goal`.

## Future objective

Evaluate specialized CSAM detection of known material and unknown material, operational incident support, and integration only if sustainable and legally appropriate. Candidate capabilities include perceptual-hash matching and dedicated classifiers, as described in [OpenAI guidance](../../sources.md). No provider is selected and no quotation/free entitlement is assumed.

## Deferred scope

- Compare eligible services, access requirements, total cost, coverage, latency, retention, data transfer and human-review/reporting requirements.
- Review provider contracts and an incident-preservation integration with counsel.
- Design a server adapter, bounded timeouts and failure behavior, and safely supplied test fixtures. No collection/generation of illegal imagery.
- Evaluate false positives, novel-content limitations and operational load before a pilot.

## Residual risk while deferred

General moderation and food-scope checks do not constitute specialized illegal-image detection. `sexual/minors` in the public moderation API is text-only. IP/safety identifiers help correlation; neither detects the image's legality nor identifies its author conclusively. Deferral is not a legal exemption or a reason to discard mandatory incident duties.

## Conditions to reconsider

Owner expressly reopens the plan after a viable cost/access option, public-audience change, incident, or legal requirement materially changes the decision. Required inputs: selected provider/capability, confirmed terms/costs, revised risk/legal assessment, data-flow/retention design, concrete scope and validation.

## Re-entry

Move this same ID to `plans/`, preserve the dated deferral reason/history, update every index/link/dependency and mark `Aguardando aprovação`. Obtain a new explicit approval naming the moved file before implementation. Availability of money or a tool does not reactivate it automatically. If a requirement becomes mandatory, record it as a rollout blocker; do not silently leave a mandatory control dismissed for budget reasons.

## Future acceptance

Provider-backed evidence of detection scope, safe false-positive tests, failure containment, operable reporting/preservation process and sustainable measured cost. These criteria are provisional and must be made executable when reactivated.
