# CP6 — Integrated validation and rollout readiness

- Status: Aguardando aprovação
- Date: 2026-09-30
- Owner: `content-policy`
- Delivery boundary: `docs/content-policy/`; source inspection and controlled validation requests against server, Android and infra, plus related index/lifecycle updates. No production-code fixes in this goal.
- Prerequisites: CP5 activation evidence and a CP4 test APK; CP1–CP4 deliverables available. Outstanding manual checks may be performed here and attributed to their originating plans.

## Objective and authorization

Approve this plan to produce an honest integrated result: implemented controls, observed limitations, remaining legal/public-release dependencies and the next scoped fixes if needed. It cannot certify legal compliance or turn deferred specialist detection into delivered coverage.

## Execution

1. Populate the [validation matrix](../validation/README.md) with exact revision/APK version, test command, date, outcome and redacted evidence per row. Reuse valid evidence; do not rerun suites without a change, failure or unresolved concern.
2. Exercise the complete closed-dev flow: valid meal/photo, short contextual reply, off-topic math, harmless instruction override in image/text, refusal recovery, memory/digest containment, optional legacy ID, two installations, restart and expiring restriction. Use only benign synthetic uploads and mocked severe verdicts.
3. Confirm the same installation safety identifier reaches all Responses stages, raw UUID/IP never reach provider prompts/Firebase, and edge provenance remains valid. Review error paths, disk/retention behavior and residual logs/local copies using metadata only.
4. Run the CP1 incident tabletop with an apparent severe-content signal: stop ordinary processing, locate minimal metadata, decide appropriate preservation/escalation, record required report channel/receipt/deletion steps. Never file a real report for a synthetic case or acquire illegal media.
5. Review false positives and live benign adversarial results separately from mock wiring tests. Report the lack of specialist image detection and the resettable/spoofable identity. Do not report zero observed failures as 100% protection.
6. Produce a dated `validation/readiness.md` with independent outcomes: technical closed-dev readiness, tester manual acceptance, privacy/notice readiness, incident response readiness and public-launch readiness. Any unresolved requirement has an owner and closure evidence, not a vague green status.
7. If public launch is contemplated, verify reviewed legal text, publication/acceptance mechanism, age/audience decisions, applicable access-log/reporting duties and unresolved preservation capability. These are explicit future delivery/legal dependencies; passing CP6 technical checks does not authorize launch. New UX needs a separate implementation plan and applicable Stitch gate.

## Acceptance

- All executed checks distinguish PASS/FAIL/NOT RUN; failures are reproducible without exposing personal/illegal content.
- Required unfinished manual checks keep their originating plans pending. No blanket completion of CP1–CP5.
- CP6 can complete as a validation delivery with a documented NOT READY public-launch decision, provided every required validation activity was actually performed and all findings have concrete disposition. A missing required validation activity is pending, not a negative result.
- Code defects generate narrowly scoped follow-up plans in the owning boundary; do not silently repair multiple code folders inside this documentation goal.
- Check links and `git diff --check`; apply SDD lifecycle and Git delivery.

## Exclusions

No new vendor, automatic law-enforcement submission, public release, app distribution unless separately requested, raw log export into docs, layout change or claim of legal immunity.
