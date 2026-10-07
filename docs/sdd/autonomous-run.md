# Autonomous run — several approved plans in one unattended session

Owner decision of 07/10/2026, after two overnight runs: the owner approves a batch of plans in one message and goes away; one agent delivers them in sequence, under the rules of [SDD](README.md) and [AGENTS.md](../../AGENTS.md), without any step that needs the owner. This file is the runbook the batch prompt points to. It changes no gate: a plan still needs its approval by name, and a design plan still ends at the Figma review.

## What makes a plan runnable unattended

A plan enters a batch only when all of these hold. The agent checks them before starting the plan and skips the plan (recording why) when one fails.

1. Status `Aguardando aprovação` and the approval sentence of the plan in the batch message.
2. Every prerequisite plan `Concluído`, or delivered earlier in the same batch (a server prerequisite also deployed to the dev VM).
3. No validation item that needs the owner: no Figma review (its golds are already exported), no "owner confirms on a device". Such items are written as `Manual acceptance (after delivery)` in the plan and are performed by the owner later; they do not block Completion of the automated part, and the plan goes to `pending_manual_validation/` with the state `Pendente aprovação manual`.
4. A real-clock behavior (a 22:00 closure, a rollover) has a dev-only trigger or a fixed test clock, per [ADR-019](../produto/adrs/ADR-019-ferramentas-dev.md); the validation uses it, never waits for the clock.
5. For a server plan, an evaluation budget written in its Validation section (below).

## Order inside a batch

One `/goal` = one folder, one plan at a time, in the order the batch message gives. Server plans first (their client plans need them deployed), then client plans in the dependency order their prerequisites state. Each plan ends with the git delivery of SDD before the next starts: branch, PR, merge, master updated.

A plan that fails a validation twice stays `Em implementação` with "what is missing" in Results, and the agent moves to the next plan that does not depend on it. Dependents are skipped and recorded. Red CI or a merge conflict: resolve once by rebasing on master; a second failure stops that plan.

## Evaluation budget (server plans)

The provider cost of a whole batch is capped: **US$ 0.50 per unattended session** (owner decision, 07/10/2026), read from the `cost_usd` of every `logs/evals/*.json` report the session wrote, summed in a ledger the agent keeps in the report file (below). The cap replaces "the whole suite once per prompt change" of the server README for unattended work.

| Step of a server plan | Model calls | Typical cost |
| --- | --- | --- |
| Unit tests, dev deploy, three-turn HTTP smoke | 0 (smoke: 3) | ≈ 0 |
| The plan's own cases, `--tag <plan> --repeat 3` | 3 × cases | US$ 0.005–0.02 |
| The sentinel set after a prompt change: `--tag s22 --tag s23 --tag s24 --tag recipe --repeat 1` | ≈ 35 | ≈ US$ 0.015 |
| A moderation-dependent plan: `--tag cp2 --moderation all --repeat 1`, once | ≈ 20 | ≈ US$ 0.01 plus the daily moderation quota |

Rules:

- Never the whole suite, never the pilot runner, never `--moderation all` outside the single CP2 pass of a plan that touches refusals, never a repeat to settle a flaky case (record it by name).
- Each server plan states its own ceiling in Validation; the sum of the ceilings in a batch stays under US$ 0.50 with margin for one retry.
- At US$ 0.40 accumulated, no further evaluation: the remaining server plans finish with unit tests and the smoke, Results say the evaluation is pending by budget, and the client plans continue.
- A case that fails 3/3 on the plan's own tag is a defect to fix (twice at most); a sentinel case that fails once is recorded, not chased.

## Client plans

Visual QA runs against the exported golds with the emulator and `tools/capture-*.sh` (partial validation: the flows the plan touches). The dev server is used for the one-turn checks the plan lists; request ids go to Results, never user content. At the end of the batch, when at least one client plan was delivered, the agent ships a dev build with `tools/distribute-dev.ps1 -Notes <pt-BR notes>`; a failed build is retried once after the remaining plans.

## Limits and resume

If the session hits its usage limit, the agent schedules a wakeup for the reset and resumes: the state is in git (`master`, the plan's branch, Results) and in the report file. A delivered step is never redone; a merged PR is never reopened.

## Report

The batch writes `docs/<context>/validation/<run-id>.md` (context of the first plan, or `server` when mixed) with: the order run, each plan's end state and PR, the evaluation ledger (every report file, its cost, the running total), deploys, the build version, what was skipped and why, and the manual acceptances the owner has to do. It is the first thing the owner reads.

## Batch message

The owner's message lists the approval sentence of every plan in order and names this runbook. Nothing in it can waive a gate, a blocker of the [production gate](../content-policy/production-gate.md) or the cost cap.
