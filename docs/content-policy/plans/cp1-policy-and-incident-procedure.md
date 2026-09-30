# CP1 — Policy, retention and incident procedure

- Status: Aguardando aprovação
- Date: 2026-09-30
- Owner: `content-policy`
- Delivery boundary: `docs/content-policy/` (documentation only; related indexes may be updated).
- Prerequisites: none.

## Authorization

Approve this exact plan before its delivery. This is a documentation `/goal`, not authorization to deploy, contact authorities, hire a vendor, publish terms or collect additional data.

## Objective

Produce an operationally usable policy pack for the existing closed dev test and an explicit list of requirements before a public launch. Do not mistake a Terms of Use draft for compliance or an automatic immunity clause.

## Sources of truth

[Context](../README.md), [policy](../specifications/content-policy.md), [audit](../specifications/identity-and-audit.md), [sources and legal questions](../sources.md), [SDD](../../sdd/README.md).

## Delivery

1. Map data flows and storage: Android app-private state/photos, GCP request memory, existing conversation logs and workstation downloads, OpenAI requests and provider retention. Identify operator/controller, actual test audience and who has access. Record unknowns explicitly.
2. Create technical `operations/incident-response.md` and `operations/retention-schedule.md` in English. Incident states: received signal, triaged, contained, preservation decision, competent-channel report when required, receipt, disposition, closure. Assign the owner as the initial operational contact and specify when legal advice is required; never invent a lawyer, legal determination, authority endpoint or deadline.
3. Create separate pt-BR drafts for Terms of Use, Privacy Notice, acceptable-use rules and a tester notice under `legal/`, clearly marked as drafts until review/publication. No language mixing inside these user-facing files. Explain AI estimation, scope limits, moderation, IP/installation correlation, existing providers, international transfers, retention/rights and contact. No guarantees of full detection, zero retention or total exclusion of liability.
4. Complete a retention table with purpose, fields, legal basis/applicability, duration, trigger, storage/access, cleanup and hold rules for each class. Propose 30 days for ordinary closed-test security diagnostics only if no longer applicable duty/hold is identified; assess Article 15 access records separately. Do not set all data to 30 days or all data to six months. Record the owner's selected schedule before persistent audit activation in CP5. Missing legal facts remain a gate for the affected activation/public rollout, not a reason to stop writing the rest of the pack.
5. Specify minimal incident references and a chain-of-custody record. Metadata-only logs cannot replace content preservation when legally required. Do not direct a solo developer to download/open suspect images, forward them through chat/email, automatically delete evidence or automatically archive every flagged upload. Establish restricted handling and competent specialist/authority guidance; unknown material must not be sent to another model for confirmation.
6. Define how owner reports/false positives are reviewed using request IDs, bounded metadata and voluntary benign reproduction. Ordinary out-of-scope use is not a security incident or a reason to punish a tester.
7. Produce `legal/review-checklist.md` with source date, applicable questions, decisions, reviewer/date/evidence and remaining issues. Explicitly cover age/audience, current reporting channels/acts, sensitive data and transfers. Propose a restricted dev rollout while unresolved public requirements remain blocked.
8. Record the owner's dev daily provider-call budget and the anticipated per-turn multiplier from CP2. This is an operational configuration input to CP3/CP5, not an assumed monetary allowance. No paid vendor or legal engagement is commissioned by this plan.

## Validation and acceptance

- Walk through four synthetic incidents: off-topic request; benign false positive; credible severe-content signal; audit disk failure. Record decisions without real illegal content or personal data.
- Trace every collected field to a purpose/schedule; ensure ordinary deletion and incident preservation do not contradict each other.
- Validate links and `git diff --check`; ensure all public-copy documents are pt-BR and technical documents English.
- Agent deliverables complete only when files are usable, unknowns are explicit, and the owner has reviewed the operational schedule/contacts. Pending owner review → `Pendente aprovação manual`, not completed.
- External counsel review is not falsely marked as executed. It remains a named public-release/legal activation dependency where required; CP1 completion is not certification of compliance.

## Exclusions and risks

No app/server/infra code, new screen, automatic reporting, paid engagement, public publication or collection. If review requires a new UI, evidence store or legal operation outside these plans, create a separately scoped plan; do not conceal the dependency in CP6.

## Completion

Record artifacts, actual review and outstanding obligations in [validation](../validation/README.md), update links, and apply [SDD](../../sdd/README.md) lifecycle and Git delivery. No implementation work is authorized by this plan's creation.
