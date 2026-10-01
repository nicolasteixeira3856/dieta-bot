# CP6 — Integrated production readiness

- Status: Fora de escopo
- Date: 2026-09-30
- Owner: `content-policy`
- Prospective delivery boundary: `docs/content-policy/` (documentation and controlled validation requests; no production-code fixes).
- Authority: owner decision on 2026-09-30 to keep only closed-test controls active and defer production work.
- Reason: **the app is in a closed test** (dev flavor, invite, two testers). Integrated public-launch readiness has nothing to certify until production is on the table. Each closed-test plan validates itself.
- Production blocker: **yes**, [PG5](../../production-gate.md).

This plan is not approved, implemented or cancelled. Do not run `/goal` while it remains out of scope.

## History

Created on 2026-09-30 as active plan "Integrated validation and rollout readiness" (`plans/cp6-validation-and-readiness.md`). Deferred the same day by the owner and narrowed to production readiness.

## Future objective and scope

Produce `validation/readiness.md`, dated, with independent outcomes: technical readiness, tester acceptance, privacy/notice readiness, incident-response readiness and public-launch readiness. Run last, after CP8 and CP9 and the CP7 decision.

1. Populate the [validation matrix](../../validation/README.md) with revision/APK, command, date, outcome and redacted evidence per row; reuse valid evidence.
2. Exercise the full flow with the prod-candidate build: valid meal and photo, short contextual reply, off-topic refusal, benign injection in text and image, refusal recovery, memory/digest containment, legacy ID, two installations, restart, denylist expiry.
3. Confirm the safety identifier reaches every Responses call, raw UUID/IP never reach prompts or Firebase, edge provenance holds, retention and failure behavior work.
4. Incident tabletop with an apparent severe signal: stop, locate metadata, decide preservation/escalation, report channel/receipt/deletion. Never file a real report for a synthetic case.
5. False positives and live benign adversarial results reported separately from mock wiring tests. No "zero failures = 100% protection" claim.
6. Each unresolved requirement gets an owner and closure evidence.

## Residual risk and dependencies

Depends on [CP8](cp8-public-legal-pack.md), [CP9](cp9-production-audit-and-containment.md) and the [CP7](cp7-specialist-detection.md) decision. Deferral does not waive any duty that applies to the closed test.

## Reconsideration conditions

The owner starts production work (see the [production gate](../../production-gate.md) triggers) and CP8 and CP9 are `Concluído`.

## Re-entry

Preserve the ID and dated history. Refresh scope and validation. Move to `plans/` as `Aguardando aprovação`, update indexes/links and obtain explicit approval naming the active plan. Follow [SDD](../../../sdd/README.md).

## Future acceptance

Every check PASS/FAIL/NOT RUN; public-launch outcome PASS with evidence; code defects become scoped follow-up plans in their owning folder. Close PG5 in the production gate.
