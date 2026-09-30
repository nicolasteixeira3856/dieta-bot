# CP5 — Trusted ingress and controlled dev activation

- Status: Aguardando aprovação
- Date: 2026-09-30
- Owner: `content-policy`; executable owner: server infrastructure.
- Delivery boundary: `infra/gcp/`; related runbooks/indexes may be updated. No edits to `server/`, `apps/android/` or deployment tooling.
- Prerequisites: CP2/CP3/CP4 implementation and automated acceptance recorded, even if integrated manual validation is pending; CP1 owner-reviewed retention schedule, incident contact/procedure and tester notice available. Required legal facts for the intended activation must be resolved, not guessed.

## Authorization and objective

Approval of this plan authorizes its controlled GCP dev deployment using the existing deployment tool. Establish trustworthy IP provenance, activate the minimal audit and validate the approved content controls. It does not authorize public release or unapproved changes in another code folder.

## Sources

[Audit specification](../specifications/identity-and-audit.md), [CP1](cp1-policy-and-incident-procedure.md), [runbook](../../server/deploy-gcp.md), [ADR-013](../../server/adrs/ADR-013-gcp-host.md), [sources](../sources.md).

## Preflight

Record deployed revision, network topology, Caddy/Uvicorn versions and private peer addresses without secrets. Check the actual path: the current GCP service is reached directly through Caddy; do not copy Cloudflare trust assumptions from the separate tower setup. Confirm only Caddy is public and API 8080 is private.

Ensure the owner supplied/approved the data-class retention schedule and has provided the tester notice. Creating a notice draft is not evidence that testers received it. Pending delivery/review stays explicitly manual; do independent config preparation first.

## Implementation and activation

1. Restrict Uvicorn forwarded-header trust to the actual Caddy peer, using a stable private network/address arrangement in `infra/gcp/compose.yml`. Avoid trusting all RFC1918 addresses or `*`. Document topology assumptions and how container recreation preserves trust.
2. Configure/verify Caddy strips or replaces client-controlled forwarding/provenance headers and derives the address from its observed connection. If using an edge-only provenance marker, strip inbound copies and ensure API reachability is limited to the edge; the marker alone is not authentication. Do not introduce another proxy.
3. Bound ingress bytes and header lengths, including bodies without trustworthy Content-Length. Do not enable body/access logging that duplicates full IP/content into uncontrolled stdout. Inventory Docker, proxy and host logs and apply the CP1 schedule to any overlapping personal data.
4. Provision the dedicated HMAC secret through the existing root-only secret mechanism without printing it or committing `.env`. Configure server environment, audit path, required-audit behavior, durations/capacity, restricted volume permissions and minimal logger settings. Disable old conversation capture.
   Provision the CP1 provider-call ceiling and persistent aggregate quota state as well. Validate positive bounded values; a missing value is not an unlimited default.
5. Deploy approved code/config through `tools/deploy-gcp.ps1`; use its supported secret transfer mechanism only when necessary and never dump resolved secret-bearing compose configuration into output. No script modifications in this goal.
6. Verify scheduled retention works at low/no traffic and after restart. Inventory historical conversation files/local downloads; execute only the disposition authorized by CP1 after checking for holds. An unresolved hold blocks deletion, not the rest of the hardening. Do not copy content into this repository.
7. Run bounded synthetic traffic through public HTTPS and private test paths. Observe canonical IP/provenance, server event ID, installation pseudonym and provider request ID linkage. Keep real IPs/identifiers in restricted evidence; commit only redacted results.

## Validation and acceptance

- Validate Caddy/Compose configuration without exposing interpolated secrets; health check after deploy.
- Public attempts to spoof X-Forwarded-For, Forwarded, X-Real-IP and any custom provenance marker cannot control the journal's attributed address. Include comma chains, IPv6, duplicate headers and missing headers. Test before/after container recreation.
- Direct public access to the API is blocked. An untrusted private request is not upgraded to trusted client attribution.
- Through deployed API: ordinary meal succeeds; off-topic math is refused; mocked staging fixtures cover severe blocks without live illegal media; timeout/audit failure do not fail open. No full body/image/provider error reaches logs.
- Controlled temporary-file/fake-clock evidence proves retention; do not alter system time or destroy real evidence. Verify monitoring alerts for capacity/failure reach the owner's existing operational observation path; no paid monitoring service.
- Record the APK version used. If a CP4 test APK has not been distributed, ask the owner for that test-build request at this concrete point and use A16; record API-only verification separately until then. Do not call CP4 manually validated by testing curl alone.

## Rollback and completion

Retain a reviewed prior configuration/revision. Rollback must keep raw logging disabled and model endpoints unavailable when policy/audit cannot operate; never restore an unrestricted service just to turn health green. Preserve evidence/secret versions as required by CP1.

Document real commands/revisions and manual results; move plans only according to their own completed checks. Apply SDD Git delivery; red CI/conflicts stop delivery. Remaining tester validation is pending, not passed.
