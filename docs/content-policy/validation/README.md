# Content-policy validation

Status: planning only. Runtime checks below are NOT RUN for the proposed controls. Existing protections/read-only findings do not count as implemented CP deliveries. Never commit live request bodies, user photos, IPs or pseudonyms here.

## Acceptance matrix

| ID | Scenario | Expected evidence | Owner | Current result |
| --- | --- | --- | --- | --- |
| V01 | Off-topic math; legitimate food arithmetic | Refusal vs allowed answer, three benign live repeats | CP2 | NOT RUN |
| V02 | Short contextual food reply/greeting | No false refusal or loss of intended meal slot | CP2 | NOT RUN |
| V03 | Direct/indirect injection; mixed prompt; text in benign image | No rule override, scope leakage or unchecked output | CP2 | NOT RUN |
| V04 | Poisoned memory/history/digest/profile fields | No malicious memory/digest propagation; recovery path | CP2 | NOT RUN |
| V05 | Severe policy categories and modality coverage | Mocked verdicts; no unsupported-image negative inference | CP2 | NOT RUN |
| V06 | Known/suspected CSAM signal | No further model/moderation transmission; incident procedure invoked | CP1/CP2 | NOT RUN |
| V07 | Provider refusal, invalid JSON, text-only fallback, timeout | Fixed safe output; no estimate/memory; no fail-open | CP2 | NOT RUN |
| V08 | Estimate/fit/chat/compact and old APK | Controls cover every route, compatible refusal shapes | CP2/CP4 | NOT RUN |
| V09 | Oversize/chunked body, malformed image, decoded-pixel bomb | Rejected before model, bounded memory | CP2/CP5 | NOT RUN |
| V10 | Stable/new/legacy/spoofed installation ID | Provider serialization, pseudonym semantics, legacy coverage | CP3/CP4 | NOT RUN |
| V11 | All Responses stages including compact | Same safety identifier, separate provider request linkage | CP3 | NOT RUN |
| V12 | Proxy spoofing, IPv6, direct access, container restart | Actual trusted-edge evidence, no public API port | CP5 | NOT RUN |
| V13 | Log/error sentinel leakage and duplicate request IDs | No forbidden fields; unique server event IDs | CP2/CP3 | NOT RUN |
| V14 | Retention, low traffic, restart, legal hold, full disk | Fake-clock/temp-file evidence; required-audit safe failure | CP3/CP5 | NOT RUN |
| V15 | Rotating UUID, shared IP, rate limits and denylist expiry | Independent limits; no permanent classifier-based bans | CP3 | NOT RUN |
| V16 | Latency/token/call limits and legitimate evaluator baseline | Bounded cost and shared deadline; baseline comparison | CP2 | NOT RUN |
| V17 | Daily wipe/update/reinstall/redirect/telemetry | Identity lifecycle and destination restriction, no tracking leakage | CP4 | NOT RUN |
| V18 | Incident tabletop; notice; retention approval | Dated owner review, applicability and unresolved legal questions | CP1/CP6 | NOT RUN |
| V19 | Integrated APK and API | No card/memory on refusal; meal/photo still usable | CP5/CP6 | NOT RUN |
| V20 | Public-launch readiness | Separate technical/legal/notice/age/preservation outcomes | CP6 | NOT RUN |

## Recording rules

Each implemented plan records exact revision, environment/APK, command, time, pass/fail/not-run and reproducible sanitized evidence. Separate fake-provider tests from real-provider benign evals, emulator checks from tester checks, and document review from legal approval. Any required manual test not done keeps its plan pending.

Test fixture policy: benign synthetic media only, mocked severe verdicts, no real/synthetic illegal sexual material, no production log replay to models. An ordinary policy block is not proof of crime; metadata cannot prove image content. Specialist coverage remains [out of scope](../plans/out_of_scope/cp7-specialist-detection.md).

## Documentation delivery checks

Planning delivery, 2026-09-30:

- Read-only inspection of current server, Android network/compaction flow and GCP configuration; no application, deployment or secret changes.
- Relative Markdown file-target validation across the 28 created/modified Markdown files: 429 links checked, zero missing file targets. External URL availability and anchor semantics are not claimed by this file-target check.
- Six active plans use `Aguardando aprovação`; CP7 alone uses `Fora de escopo` in `plans/out_of_scope/`. ADR-024/025 remain proposed.
- `git diff --check` passed. No runtime test suite was run for this documentation-only delivery; V01–V20 remain NOT RUN.
