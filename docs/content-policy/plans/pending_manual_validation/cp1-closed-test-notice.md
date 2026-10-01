# CP1 — Closed-test notice and incident note

- Status: Pendente aprovação manual (approved by the owner 2026-10-01; documents delivered 2026-10-01; tester delivery pending)
- Date: 2026-09-30 (rescoped 2026-09-30 for the closed test)
- Owner: `content-policy`
- Delivery boundary: `docs/content-policy/` (documentation only; related indexes may be updated).
- Prerequisites: none.

## History

Originally "Policy, retention and incident procedure" with a full public legal pack. On 2026-09-30 the owner deferred the public pack because the app is in a closed test. That part now lives in [CP8](../out_of_scope/cp8-public-legal-pack.md) and blocks production through the [production gate](../../production-gate.md).

## Authorization

Approve this exact plan before its delivery. Documentation only: no deploy, no authority contact, no vendor, no publication, no new collection.

## Objective

Give the two closed-test testers an honest notice of what is collected, and give the owner a short procedure for a severe-content signal. No Terms of Use, no Privacy Policy, no compliance claim.

## Delivery

1. `legal/tester-notice.pt-BR.md` (pt-BR only): what the app sends to the server and to OpenAI (text, photo, history, memory), that photos are not stored, that dev conversation logs exist for debugging with 30-day rotation, that moderation-flagged turns are not logged in full, that an installation pseudonym and IP are used for abuse correlation, that estimates are not advice, and a contact. No guarantees of full detection or zero retention.
2. `operations/closed-test-data-map.md` (English): each data class (Android app-private state and photos, request memory on GCP, dev conversation log, Docker/Caddy logs, workstation downloads from `tools/pull-conversations.ps1`, OpenAI requests with `store=False`), where it lives, who can access it, and retention. Unknowns stay explicit.
3. `operations/closed-test-incident.md` (English), one page:
   - Ordinary off-topic use is not an incident. No punishment of testers.
   - False-positive report: find the request ID in the dev log, reproduce with benign input, adjust in a CP2 follow-up.
   - Credible severe-content signal (moderation flag on a sexual or minor-related category, or a tester report): do not open, download or forward the material; do not send it to another model; rotate the invite (CP5 procedure) if abuse is ongoing; keep only metadata; seek legal guidance before any report or deletion. Never invent a lawyer, channel or deadline.
4. Record in this plan the date the owner delivered the notice to each tester (manual step).

## Validation and acceptance

- Walk through three synthetic cases: off-topic request, benign false positive, severe-content signal. No real content or personal data.
- Each field in the data map has a purpose and a retention line.
- Links and `git diff --check`; tester notice in pt-BR, technical files in English.
- Documents done → `Pendente aprovação manual` until the owner confirms the notice was delivered to both testers; then `Concluído`.

## Exclusions

Public legal pack, LGPD/Marco Civil/ECA Digital applicability, age assurance, reporting channels, chain of custody and retention schedule for production: [CP8](../out_of_scope/cp8-public-legal-pack.md). No app, server or infra code.

## Completion

Record artifacts in [validation](../../validation/README.md), update links, apply [SDD](../../../sdd/README.md) lifecycle and Git delivery.

## Results (2026-10-01)

Delivered artifacts:

- [legal/tester-notice.pt-BR.md](../../legal/tester-notice.pt-BR.md) — pt-BR notice, version 1.
- [operations/closed-test-data-map.md](../../operations/closed-test-data-map.md) — 13 data classes, each with purpose and retention; unknowns listed.
- [operations/closed-test-incident.md](../../operations/closed-test-incident.md) — one-page note with the synthetic walkthrough.

Facts were read from the code at revision `bdc5251` (server logging, moderation, compose, Caddyfile, `tools/pull-conversations.ps1`, Android Room, memory and photo storage, telemetry). Findings that the notice states honestly instead of claiming the planned state:

- The dev conversation log rotates by size today (20 MB × 6 files), not by age. The 30-day rotation is a [CP5](../cp5-gcp-dev-ingress.md) deliverable; the notice says it is not active yet and will be updated.
- Docker logs (`api` access lines with client IP, `caddy`) have no size limit yet (CP5).
- The installation pseudonym is not deployed ([CP3](../cp3-server-safety-identifier.md)/[CP4](../cp4-android-installation-identity.md)); the notice describes it as coming in a next version.
- `safety_support` and `out_of_scope` turns are logged in full; only `policy_blocked` turns are metadata-only.
- Sent photos stay on the phone (app-private) until uninstall or clear-data; the server never writes them.
- Contact is the owner through the channel that delivered the invite; no e-mail is published in the repo.

| Check | Result |
| --- | --- |
| Synthetic walkthrough: off-topic, benign false positive, severe signal | PASS — recorded in the [incident note](../../operations/closed-test-incident.md#4-synthetic-walkthrough-2026-10-01); no real content or personal data |
| Data map: each class has purpose and retention | PASS — D1–D13; unknown retention marked "Unknown" (D10, D12) or pending CP5 |
| Language: notice pt-BR, technical files English | PASS |
| Relative links in `docs/content-policy/` | PASS — checked with a local script |
| `git diff --check` | PASS |
| Notice delivered to both testers | PENDING — owner manual step below |

## Tester delivery (owner manual step)

The owner sends [tester-notice.pt-BR.md](../../legal/tester-notice.pt-BR.md) version 1 to each tester and records the date here. No names or contact data in this table.

| Tester | Date delivered | Channel kind |
| --- | --- | --- |
| Tester 1 | PENDING | — |
| Tester 2 | PENDING | — |

When both rows have a date: set Status to `Concluído`, move this file to `plans/completed/`, update V17 and the plan index.
