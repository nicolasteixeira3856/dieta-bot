# CP5 — GCP dev ingress, log hygiene and activation

- Status: Aguardando aprovação
- Date: 2026-09-30 (rescoped 2026-09-30 for the closed test)
- Owner: `content-policy`; executable owner: server infrastructure.
- Delivery boundary: `infra/gcp/`; related runbooks/indexes may be updated. No edits to `server/`, `apps/android/` or deployment tooling.
- Prerequisites: [CP2](pending_manual_validation/cp2-server-content-controls.md) and [CP3](pending_manual_validation/cp3-server-safety-identifier.md) deployed; [CP4](pending_manual_validation/cp4-android-installation-identity.md) automated acceptance; [CP1](pending_manual_validation/cp1-closed-test-notice.md) notice delivered to the testers.

## History

Originally "Trusted ingress and controlled dev activation" including security-journal activation, retention schedule enforcement and historical-log disposition. Journal and production retention moved to [CP9](out_of_scope/cp9-production-audit-and-containment.md) on 2026-09-30 because the app is in a closed test.

## Authorization and objective

Approval authorizes this dev deployment with the existing tool. Narrow forwarded-header trust, bound log retention, provision the safety secret and document containment. No production, no public release.

## Preflight

Record the deployed revision, Caddy and Uvicorn versions and the compose network, without secrets. Confirm only Caddy publishes ports and 8080 is private.

## Implementation

1. **Forwarded headers.** Replace `--forwarded-allow-ips=*` in `infra/gcp/compose.yml` with the compose network's address range (a fixed subnet declared in the compose file), not all RFC1918 and not `*`. Keep Caddy without `trusted_proxies` so it overwrites client `X-Forwarded-For`.
2. **Log rotation.** Docker `json-file` logging with `max-size`/`max-file` for both services. Rotate `/opt/nutri/logs/conversations.jsonl` at 30 days (host `logrotate` from `startup.sh`, or the documented equivalent). Inventory existing log files and workstation copies; delete nothing older without the owner's go-ahead.
3. **Secret.** Provision `SAFETY_ID_SECRET` and `SERVER_ENV=dev` in the VM `.env` through the existing root-only mechanism; never print, commit or dump resolved compose config. `/health` must show `safety_id: on`.
4. **Spend ceiling (owner manual step).** The owner sets a budget limit on the OpenAI project in the dashboard and records here the value and whether it is a hard limit or an alert.
5. **Invite rotation runbook.** Add to [deploy-gcp.md](../../server/deploy-gcp.md): new invite in the VM `.env`, redeploy, new dev build through A16 (`tools/distribute-dev.ps1`), old invite returns 401. Do not rotate now unless abuse is observed.
6. Deploy with `tools/deploy-gcp.ps1`.

## Validation

- Public spoofing: `X-Forwarded-For`, `Forwarded` and `X-Real-IP` with comma chains, IPv6 and duplicates cannot change the address used by the rate limiter (check via rate-limit behavior or the log's client address field if present). Repeat after container recreation.
- Direct public access to 8080 is blocked.
- Through the deployed API with the CP4 APK: ordinary meal and photo succeed; math refused; same installation → same identifier in the dev log across a restart; second installation differs.
- Rotation configuration validated (Docker log options visible in `docker inspect`; logrotate dry run).
- If the CP4 APK is not yet distributed, ask the owner for a test build at this point and use A16. curl alone does not validate CP4.

## Rollback and completion

Keep the prior compose file. Rollback never restores `*` trust. Record real commands and results; redact IPs and identifiers before committing evidence. Apply [SDD](../../sdd/README.md) lifecycle and Git delivery.
