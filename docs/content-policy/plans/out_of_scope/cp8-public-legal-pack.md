# CP8 — Public legal pack

- Status: Fora de escopo
- Date: 2026-09-30
- Owner: `content-policy`
- Prospective delivery boundary: `docs/content-policy/` (documentation). Publication or acceptance UI needs its own Android plan and a Stitch gate.
- Authority: owner decision on 2026-09-30 to keep only closed-test controls active and defer production work.
- Reason: **the app is in a closed test** with two known testers, covered by the [CP1](../completed/cp1-closed-test-notice.md) notice. Public Terms, Privacy Policy and legal review are needed only for an open audience.
- Production blocker: **yes**, [PG2](../../production-gate.md).

This plan is not approved, implemented or cancelled. Do not run `/goal` while it remains out of scope.

## History

Split on 2026-09-30 from the original CP1 "Policy, retention and incident procedure".

## Future objective and scope

1. Data-flow map for the production audience: operator/controller, providers, international transfers, access.
2. `operations/incident-response.md` (English): received signal, triaged, contained, preservation decision, competent-channel report when required, receipt, disposition, closure. Chain-of-custody record. Never direct a solo developer to open, download or forward suspect images, or to send them to another model.
3. `operations/retention-schedule.md` (English): per data class, purpose, fields, legal basis, duration, trigger, storage/access, cleanup and hold. Marco Civil Art. 15 access records assessed separately from conversation bodies. Not everything at 30 days, not everything at six months. Feeds [CP9](cp9-production-audit-and-containment.md).
4. pt-BR drafts under `legal/`: Terms of Use, Privacy Policy, acceptable use. AI estimate, scope limits, moderation, IP/installation correlation, providers, transfers, retention, rights, contact. No guarantee of full detection, zero retention or total exclusion of liability.
5. `legal/review-checklist.md` with the questions in [sources](../../sources.md#legal-questions-for-cp8): operating person/entity and Art. 15 applicability (it targets providers organized as legal entities with economic purpose), audience and minors (ECA Digital, age assurance), health data as sensitive data under LGPD, lawful bases, transfers, current reporting channel and deadlines (Decree 12.880/2026), Consumer Protection Code duties.
6. External counsel review recorded with reviewer, date and evidence. Never marked as done without it.

## Residual risk and dependencies

The closed test runs without reviewed public terms. Acceptable only while the audience is the two named testers who received the CP1 notice. Opening the audience without CP8 is blocked by the production gate.

## Reconsideration conditions

The owner starts production work, or the audience grows beyond known testers, or a legal requirement applies earlier.

## Re-entry

Preserve the ID and dated history. Refresh the legal sources (they were reviewed on 2026-09-30). Move to `plans/` as `Aguardando aprovação`, update indexes/links and obtain explicit approval naming the active plan. Follow [SDD](../../../sdd/README.md).

## Future acceptance

Reviewed documents, signed checklist, published texts and an acceptance path delivered by its own approved plan. Close PG2 in the production gate.
