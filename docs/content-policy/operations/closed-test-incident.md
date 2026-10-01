# Closed-test incident note

Delivered by [CP1](../plans/pending_manual_validation/cp1-closed-test-notice.md) on 2026-10-01. One page for the owner. Closed test only; the production procedure belongs to [CP8](../plans/out_of_scope/cp8-public-legal-pack.md) and [CP9](../plans/out_of_scope/cp9-production-audit-and-containment.md). Data locations: [data map](closed-test-data-map.md).

## 1. Ordinary off-topic use

Math, code, homework, politics or other non-meal requests are not an incident. The server returns the fixed scope refusal ([CP2](../plans/pending_manual_validation/cp2-server-content-controls.md)). No punishment, no warning to the tester, no invite rotation. If the refusal did not happen (the model answered off-topic), treat it as a CP2 eval miss: note the request ID, add a benign synthetic case to the eval set in a CP2 follow-up.

## 2. False-positive report

A tester says a normal meal or photo was refused or blocked.

1. Get the approximate time and route from the tester. Never ask them to resend the photo through another channel.
2. Find the request ID in the dev log (`tools/pull-conversations.ps1 -RequestId <id>` or by time with `-Tail`). Read the `policy` code and category booleans.
3. Reproduce with benign synthetic input (a typed meal or a generic food photo), not the tester's content.
4. If it reproduces, fix it in a CP2 follow-up plan (prompt, schema or category table). Record the case as a false positive in [validation](../validation/README.md), with the request ID only.

## 3. Credible severe-content signal

Triggers: a moderation flag on a sexual or minor-related category (log `policy.code` = `policy_blocked` with `sexual` or `sexual/minors` in `policy.categories`, or `policy.severe` true), or a tester or anyone else reporting such material.

Do:

- Keep only metadata: request ID, timestamp, route, policy code, category booleans. The server already logs a flagged turn without body, image or generated text (D5).
- If abuse is ongoing (repeated flags, a leaked APK, unknown traffic), rotate the invite with the CP5 procedure in [deploy-gcp.md](../../server/deploy-gcp.md) once CP5 adds it. Until then: new invite in the VM `.env`, redeploy with `tools/deploy-gcp.ps1`, new dev build through A16, confirm the old invite returns 401.
- Seek legal guidance before any report to an authority and before deleting anything related to the signal. Pause the deletion rule of the data map for those records.
- Record in a private note (not the repo) the date, request IDs and the actions taken.

Do not:

- Open, download, view, copy or forward the material, including D5 lines around it or a tester's screenshot.
- Send it to another model, moderation endpoint or classifier to "confirm".
- Accuse a tester or infer a person from an IP or pseudonym. A moderation flag is not proof of a crime.
- Contact a tester about the content beyond acknowledging the report.

No lawyer, reporting channel or deadline is named here because none has been chosen. Do not invent one; choosing them is CP8 work.

## 4. Synthetic walkthrough (2026-10-01)

Desk check of this note against three made-up cases. No real request, content or personal data.

| Case | Input (synthetic) | Expected path | Result |
| --- | --- | --- | --- |
| Off-topic | "Resolve x² − 5x + 6 = 0" | §1: fixed scope refusal, no incident, no action on the tester | PASS: §1 covers it; matches CP2 V01 |
| Benign false positive | Typed "200 g de peito de frango grelhado" refused as `policy_blocked` (hypothetical) | §2: request ID → log metadata → reproduce with benign input → CP2 follow-up, record as false positive | PASS: every step names a tool or file; no tester content needed |
| Severe signal | Log line with `policy_blocked`, `severe: true`, `sexual/minors` in categories, no body (mocked) | §3: keep metadata, no download or second model, rotate invite if ongoing, legal guidance before report or deletion | PASS: the log already holds metadata only; the note forbids opening or forwarding; no invented contact |
