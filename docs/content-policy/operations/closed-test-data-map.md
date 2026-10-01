# Closed-test data map

Delivered by [CP1](../plans/pending_manual_validation/cp1-closed-test-notice.md) on 2026-10-01. Scope: the dev flavor (`com.nutri.android.dev`), the GCP dev VM `nutri-api` and the owner's workstation, during the closed test. Tester-facing summary: [tester notice](../legal/tester-notice.pt-BR.md). Production retention belongs to [CP8](../plans/out_of_scope/cp8-public-legal-pack.md) and [CP9](../plans/out_of_scope/cp9-production-audit-and-containment.md).

Facts below were read from the code on 2026-10-01 (revision `bdc5251`). "Unknown" means nobody has checked it; do not fill it with a guess.

## Data classes

| # | Data class | Contents | Where it lives | Who can access | Purpose | Retention |
| --- | --- | --- | --- | --- | --- | --- |
| D1 | Android Room database | Profile (ceiling, eat-back, macro targets, slots), days, meal logs, slot skips, day digests, chat messages with the local photo path | App-private storage (`nutri.db`); `allowBackup=false` | The tester on the device; the app | Day state and Chat thread | Until uninstall or clear-data. `Zerar hoje` deletes today's meal logs, skips and digest but keeps chat messages, profile and slots |
| D2 | Android memory file | AI memory facts (eating habits), JSON in encrypted `memory.bin` | `filesDir`, app-private | The tester on the device; the app | Context for the next estimate | Facts expire by the memory rules (ADR-023); file kept until uninstall or clear-data; `Zerar hoje` keeps it |
| D3 | Android photos | JPEG q85, ≤2048 px, EXIF stripped (ADR-018) | `filesDir/photos`, app-private | The tester on the device; the app | Upload body and Chat thumbnail | Unsent or replaced attachments deleted at once. Sent photos stay until uninstall or clear-data |
| D4 | Request memory on the GCP VM | Request body: text, base64 photo, history, memory, profile, day state | `api` container process memory | The running process; root on the VM | Moderation and generation for one turn | Request lifetime. The photo is detached from the body on entry and dropped after the call; never written to disk |
| D5 | Dev conversation log (ADR-015) | Per turn: timestamp, request ID, route, app version/env, prompt and input text (history, memory, profile), photo presence and base64 length, raw model output, response, policy code, error, derived `safety_identifier` (D11, null while inactive; never the raw UUID), latency. Invite and API key redacted. `policy_blocked` turns (input, scope or output stage): metadata only (request ID, route, policy code, flagged categories). `out_of_scope` and `safety_support` turns are logged in full | `/opt/nutri/logs/conversations.jsonl` (+ `.1`–`.5`) on `nutri-api`, directory mode 700 | Owner via IAP SSH (`gcloud compute ssh`) and project IAM principals with VM/SSH rights | Debugging, scope-drift detection | Today: size rotation, 20 MB × 6 files (about 120 MB). No time limit, so old lines can outlive 30 days at low traffic. 30-day rotation pending [CP5](../plans/cp5-gcp-dev-ingress.md) |
| D6 | Docker logs (`api`) | Uvicorn startup/errors and access lines: client IP (from Caddy's `X-Forwarded-For`), method, path, status. Server warnings carry route and exception type, no body | Docker `json-file` on the VM | Root on the VM; owner via SSH | Operations, rate-limit debugging | No `max-size`/`max-file` set: unbounded until container removal. Bounding pending CP5 |
| D7 | Docker logs (`caddy`) | Caddy runtime and TLS logs. No access log (no `log` directive in the Caddyfile) | Docker `json-file` on the VM | Root on the VM; owner via SSH | TLS and proxy operations | Unbounded until container removal; pending CP5 |
| D8 | Rate-limit state | Client IP + invite key, per-route counters | `api` process memory (slowapi) | The running process | Throttling | Window length; lost on restart |
| D9 | Workstation downloads | Copies of D5 from `tools/pull-conversations.ps1 -Download` | `logs/` in the owner's repo checkout (git-ignored) | The owner's Windows account | Offline debugging | Manual. No automatic deletion. Delete after the investigation. Current copies: unknown (not inventoried; CP5 inventories them) |
| D10 | OpenAI requests | Responses calls: instructions, user block (text, history, memory, profile, day), photo; `store=False`. Moderation calls: current text, photo and generated output | OpenAI API (project of the dev server key) | OpenAI under its API data policy; owner sees usage only | Generation and moderation | Not stored for app retrieval (`store=False`). Provider abuse-monitoring retention: unknown for this project (governed by OpenAI's API policy; not verified, no zero-retention agreement) |
| D11 | OpenAI `safety_identifier` | HMAC pseudonym of the installation UUID | Sent on Responses calls; D5 record | OpenAI; owner via D5 | Abuse correlation | Server code deployed to dev 2026-10-01 ([CP3](../plans/pending_manual_validation/cp3-server-safety-identifier.md)) but inactive: no secret yet ([CP5](../plans/cp5-gcp-dev-ingress.md)) and no header from the APK yet ([CP4](../plans/cp4-android-installation-identity.md)). Retention follows D5 and D10 |
| D12 | Firebase telemetry (dev, ADR-014) | Crashlytics crashes, breadcrumbs and keys; Analytics events with enums and numbers (screen, route, status, latency, text-length bucket, has_photo, kcal). No user text, no photo. `X-Request-Id` links a crash to D5 (ADR-015) | Firebase project `nutri-bot-dev` | Owner and project members | Crash and usage diagnosis | Unknown: project retention settings not inspected |
| D13 | Secrets on the VM | `OPENAI_API_KEY`, invite code | `/opt/nutri/.env`, root-only | Root on the VM | Auth and provider access | Until rotated. Never in logs (D5 redacts them) |

## Unknowns

- D9: whether downloaded log copies exist on the workstation today, and how old they are.
- D10: OpenAI abuse-monitoring retention that applies to this project.
- D12: Crashlytics and Analytics retention configured in `nutri-bot-dev`.
- IAM: the exact list of principals with SSH/OS Login rights on `nutri-api` was not listed in this delivery.
- D6/D7: actual size and age of the current Docker logs on the VM.

Close an unknown by recording the checked value, date and source here. CP5 closes D5–D7 and D9 inventory.

## Rules

- Never copy D4, D5 or D9 contents into the repo, an issue, a chat with another model or this folder. Evidence uses request IDs and counts only.
- A tester's deletion request: remove their D5/D9 lines by request ID and time window after confirming which installation they used; D1–D3 are deleted by clear-data or uninstall on their phone. Record only the date and the fact that it was done.
- Severe-content signal: follow the [incident note](closed-test-incident.md); it overrides the deletion rule above until legal guidance is received.
