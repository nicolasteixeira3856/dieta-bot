# CP9 — Production audit, retention and containment

- Status: Fora de escopo
- Date: 2026-09-30
- Owner: `content-policy`
- Prospective delivery boundary: `server/`. GCP activation of the journal needs a separate `infra/gcp/` plan or a CP5 successor.
- Authority: owner decision on 2026-09-30 to keep only closed-test controls active and defer production work.
- Reason: **the app is in a closed test**. With two testers and an invite, the dev conversation log, invite rotation and the OpenAI project budget cover debugging and containment. A security journal with legal hold, in-app quotas and a denylist are disproportionate now.
- Production blocker: **yes**, [PG3](../../production-gate.md).

This plan is not approved, implemented or cancelled. Do not run `/goal` while it remains out of scope.

## History

Split on 2026-09-30 from the original CP3 "Safety identifier and restricted audit", CP5 journal activation and the context-wide moderation of the original CP2.

## Future objective and scope

Implement the [production profile](../../specifications/identity-and-audit.md#production-profile-deferred) and the context items of the [content policy production profile](../../specifications/content-policy.md#production-profile-deferred):

1. Minimal security journal with the exact allowlist, server event IDs, provider request IDs and IP provenance (`trusted_edge | direct_peer | unverified`).
2. Retention from the CP8 schedule; cleanup independent of traffic; bounded disk; legal-hold exclusion; required-audit failure → 503 before generation.
3. Retire ADR-015 raw conversation capture in production, with no environment switch that re-enables it. Propose a successor ADR.
4. Moderate every model-bound context field (history, memory, facts, recent meals, digests, profile names) with field provenance, so off-topic history does not poison later valid turns while unsafe current content still blocks.
5. Mandatory `SAFETY_ID_SECRET`; legacy per-request pseudonym for missing headers.
6. Per-installation and global limits, bounded concurrency, persisted daily provider-call ceiling, expiring owner-operated denylist (403 `access_restricted`).
7. Disposition of historical closed-test logs and copies under the CP8 procedure.

## Residual risk and dependencies

While deferred: no tamper-evident audit, no hold mechanism, client-held context not re-moderated, no in-app spend ceiling. Depends on [CP8](cp8-public-legal-pack.md) for the retention schedule.

## Reconsideration conditions

The owner starts production work, the audience grows beyond known testers, or abuse appears that invite rotation and the budget limit cannot contain.

## Re-entry

Preserve the ID and dated history. Refresh defaults against measured closed-test traffic. Move to `plans/` as `Aguardando aprovação`, update indexes/links and obtain explicit approval naming the active plan. Follow [SDD](../../../sdd/README.md).

## Future acceptance

Fake-clock/temp-dir retention tests, sentinel leakage tests, spoofing and quota tests, restart/downtime behavior, as in rows V13–V15 of the [matrix](../../validation/README.md). Close PG3 in the production gate.
