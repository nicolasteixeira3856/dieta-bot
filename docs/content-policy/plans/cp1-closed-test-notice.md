# CP1 — Closed-test notice and incident note

- Status: Aguardando aprovação
- Date: 2026-09-30 (rescoped 2026-09-30 for the closed test)
- Owner: `content-policy`
- Delivery boundary: `docs/content-policy/` (documentation only; related indexes may be updated).
- Prerequisites: none.

## History

Originally "Policy, retention and incident procedure" with a full public legal pack. On 2026-09-30 the owner deferred the public pack because the app is in a closed test. That part now lives in [CP8](out_of_scope/cp8-public-legal-pack.md) and blocks production through the [production gate](../production-gate.md).

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

Public legal pack, LGPD/Marco Civil/ECA Digital applicability, age assurance, reporting channels, chain of custody and retention schedule for production: [CP8](out_of_scope/cp8-public-legal-pack.md). No app, server or infra code.

## Completion

Record artifacts in [validation](../validation/README.md), update links, apply [SDD](../../sdd/README.md) lifecycle and Git delivery.
