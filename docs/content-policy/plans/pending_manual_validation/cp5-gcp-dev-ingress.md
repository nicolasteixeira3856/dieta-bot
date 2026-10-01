# CP5 — GCP dev ingress, log hygiene and activation

- Status: Pendente aprovação manual (deployed to dev 2026-10-01; OpenAI budget limit pending, owner step 4)
- Approved: 2026-10-01 by the owner: "Aprovo o plano docs\content-policy\plans\cp5-gcp-dev-ingress.md, analise e implemente o plano aprovado."
- Date: 2026-09-30 (rescoped 2026-09-30 for the closed test)
- Owner: `content-policy`; executable owner: server infrastructure.
- Delivery boundary: `infra/gcp/`; related runbooks/indexes may be updated. No edits to `server/`, `apps/android/` or deployment tooling.
- Prerequisites: [CP2](cp2-server-content-controls.md) and [CP3](cp3-server-safety-identifier.md) deployed; [CP4](cp4-android-installation-identity.md) automated acceptance; [CP1](../completed/cp1-closed-test-notice.md) notice delivered to the testers.

## History

Originally "Trusted ingress and controlled dev activation" including security-journal activation, retention schedule enforcement and historical-log disposition. Journal and production retention moved to [CP9](../out_of_scope/cp9-production-audit-and-containment.md) on 2026-09-30 because the app is in a closed test.

## Authorization and objective

Approval authorizes this dev deployment with the existing tool. Narrow forwarded-header trust, bound log retention, provision the safety secret and document containment. No production, no public release.

## Preflight

Record the deployed revision, Caddy and Uvicorn versions and the compose network, without secrets. Confirm only Caddy publishes ports and 8080 is private.

## Implementation

1. **Forwarded headers.** Replace `--forwarded-allow-ips=*` in `infra/gcp/compose.yml` with the compose network's address range (a fixed subnet declared in the compose file), not all RFC1918 and not `*`. Keep Caddy without `trusted_proxies` so it overwrites client `X-Forwarded-For`.
2. **Log rotation.** Docker `json-file` logging with `max-size`/`max-file` for both services. Rotate `/opt/nutri/logs/conversations.jsonl` at 30 days (host `logrotate` from `startup.sh`, or the documented equivalent). Inventory existing log files and workstation copies; delete nothing older without the owner's go-ahead.
3. **Secret.** Provision `SAFETY_ID_SECRET` and `SERVER_ENV=dev` in the VM `.env` through the existing root-only mechanism; never print, commit or dump resolved compose config. `/health` must show `safety_id: on`.
4. **Spend ceiling (owner manual step).** The owner sets a budget limit on the OpenAI project in the dashboard and records here the value and whether it is a hard limit or an alert.
5. **Invite rotation runbook.** Add to [deploy-gcp.md](../../../server/deploy-gcp.md): new invite in the VM `.env`, redeploy, new dev build through A16 (`tools/distribute-dev.ps1`), old invite returns 401. Do not rotate now unless abuse is observed.
6. Deploy with `tools/deploy-gcp.ps1`.

## Validation

- Public spoofing: `X-Forwarded-For`, `Forwarded` and `X-Real-IP` with comma chains, IPv6 and duplicates cannot change the address used by the rate limiter (check via rate-limit behavior or the log's client address field if present). Repeat after container recreation.
- Direct public access to 8080 is blocked.
- Through the deployed API with the CP4 APK: ordinary meal and photo succeed; math refused; same installation → same identifier in the dev log across a restart; second installation differs.
- Rotation configuration validated (Docker log options visible in `docker inspect`; logrotate dry run).
- If the CP4 APK is not yet distributed, ask the owner for a test build at this point and use A16. curl alone does not validate CP4.

## Rollback and completion

Keep the prior compose file. Rollback never restores `*` trust. Record real commands and results; redact IPs and identifiers before committing evidence. Apply [SDD](../../../sdd/README.md) lifecycle and Git delivery.

## Results (2026-10-01)

Revision: branch `feat/cp5-gcp-dev-ingress` on top of `master` after `chore(release): 0.0.6-dev`. Environment: GCP dev VM `nutri-api`. IPs, identifiers and the secret are redacted or truncated below.

### Preflight (before the change)

| Item | Value |
| --- | --- |
| Host | Ubuntu 24.04.5 LTS, Docker 29.1.3, Compose 2.40.3 |
| `api` | image built 2026-10-01 10:31 UTC (CP3 deploy), uvicorn 0.54.0, Python 3.12.14, only `8080/tcp` exposed (not published) |
| `caddy` | `caddy:2`, v2.11.4, publishes 80/443 (IPv4 and IPv6) |
| Compose network | `gcp_default`, automatic `172.18.0.0/16` |
| Listening on the host | 22, 80, 443 (plus local resolver/loopback only); no 8080 |
| Docker logs | `json-file`, no options; 48 KB (`api`), 4 KB (`caddy`) |
| `/opt/nutri/logs` | `conversations.jsonl` only, about 0.5 MB, directory from 2026-09-28; no files older than 30 days |
| Workstation copies | `logs/conversations.jsonl` (about 0.3 MB, 2026-09-30) and `logs/evals/` reports (2026-09-30), git-ignored. Nothing deleted |
| `.env` | identical on workstation and VM (hash compared, contents never printed); no `SAFETY_ID_SECRET` / `SERVER_ENV` |
| Startup script | a copy in the VM metadata (`startup-script`), not read from the repo |

Prior compose file kept in git history (`master` before this branch). Rollback = revert this branch; never back to `*`.

### Changes

1. `infra/gcp/compose.yml`: named network `edge` with fixed subnet `172.30.53.0/28`; `--forwarded-allow-ips=172.30.53.0/28`; `json-file` `max-size: 10m`, `max-file: 3` for `api` and `caddy`. `Caddyfile` unchanged in behavior (comment only: no `trusted_proxies`).
2. `infra/gcp/startup.sh`: writes `/etc/logrotate.d/nutri` — daily, `rotate 30`, `maxage 30`, `dateext`, `copytruncate` (the server holds the file open in append mode), `compress`/`delaycompress`; `lastaction` deletes the server's own size backups (`.1`–`.5`) older than 30 days. Metadata updated with `gcloud compute instances add-metadata … startup-script=infra/gcp/startup.sh` and run once with `google_metadata_script_runner startup`.
3. `SAFETY_ID_SECRET` (32 random bytes, hex, generated locally without printing) and `SERVER_ENV=dev` appended to the workstation `.env`, shipped with `./tools/deploy-gcp.ps1 -Env` (root:root 600). Never printed, committed or dumped.
4. Budget limit: **PENDING — owner manual step.** Record here: value, hard limit or alert, date.
5. Runbook: [deploy-gcp.md](../../../server/deploy-gcp.md) gained "Ingress e logs (CP5)", "Segredo do safety identifier" and the rewritten invite rotation (new invite in `.env` + `local.properties`, `deploy-gcp.ps1 -Env`, A16 build, old invite 401). Invite not rotated (no abuse observed).
6. Deployed with `./tools/deploy-gcp.ps1 -Env`: network `gcp_edge` created, both containers recreated, `/health` → 200 `{"ok": true, "model": "gpt-6-luna", "safety_id": "on"}`. The empty `gcp_default` network was removed afterwards.

### Validation

| Check | Result |
| --- | --- |
| Uvicorn command | PASS — `docker inspect` shows `--forwarded-allow-ips=172.30.53.0/28`; `api` 172.30.53.2, `caddy` 172.30.53.3 |
| Public spoofing | PASS — 35 POSTs to `/v1/estimate` from the workstation with a probe invite (401, counts toward the limit, no model call), rotating `X-Forwarded-For` single/comma chain/IPv6/duplicate headers/edge-subnet prefix, `Forwarded` with IPv6, duplicate `X-Real-IP`: 30 × 401 then 5 × 429, so every variant shares one rate-limit key |
| Same after container recreation | PASS — `docker compose up -d --force-recreate`, same probe with a new tag: 30 × 401, 429 from the 31st |
| Direct 8080 | PASS — public `http://<ip>:8080/health` times out; host `127.0.0.1:8080` refused; only Caddy publishes ports |
| Docker log rotation | PASS — `json-file map[max-file:3 max-size:10m]` on both containers |
| Conversation log rotation | PASS — `logrotate -d /etc/logrotate.d/nutri` parses `after 1 days (30 rotations)`, old logs removed; `logrotate.timer` enabled and active. First real rotation: next daily run |
| Secret in container | PASS — `SAFETY_ID_SECRET` and `SERVER_ENV=dev` present (names only checked); `.env` 600 root:root; `/health` `safety_id: on` |
| CP4 APK through the deployed API | PASS on an emulator (`Medium_Phone`) with the distributed dev 0.0.6 APK (A16, 2026-10-01): ordinary meal → estimate (~350 kcal); photo → questions (A30) → estimate (~625 kcal); math → fixed refusal, no card, log policy `out_of_scope`; 6 turns of one installation carry the same `safety_identifier` (pseudonym A, redacted) across an app restart; after uninstall/reinstall a different one (pseudonym B, redacted), unchanged across an `api` container restart |
| Tester devices | PENDING — owner/tester use of 0.0.6 |

Not run: IPv6 client path (the VM has no external IPv6 address; Docker's IPv6 listener would proxy through the edge gateway, inside the trusted range, and Caddy still overwrites `X-Forwarded-For`).

Manual pending: step 4 (OpenAI budget limit). When recorded here: `Concluído`, move to `completed/`, update the plan index and PG1.
