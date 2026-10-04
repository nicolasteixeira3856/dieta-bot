# Design migration plans

Migration from Material 3 Expressive and Stitch to the Aero design system in Figma ([ADR-030](../adrs/ADR-030-own-design-system-aero.md), [ADR-031](../adrs/ADR-031-figma-source-of-truth.md)). Plan state: the State line of each plan and its folder. This index does not authorize implementation.

## Order

Each flow is one design plan (Figma, within one day of MCP budget) followed by one client plan (Compose). A flow's client plan starts only after its design plan.

| Step | Design (this context) | Client ([android](../../android/README.md)) |
|---|---|---|
| Foundation | D1 Figma file foundation ([`completed/`](completed/)) | A39 Aero foundation in Compose ([`pending_manual_validation/`](../../android/plans/pending_manual_validation/)) |
| Tooling | D2 Figma tooling and Stitch deprecation ([`completed/`](completed/)) | — |
| Home | D3 Release 1 — Home ([`completed/`](completed/)) | A40 Home on Aero ([`pending_manual_validation/`](../../android/plans/pending_manual_validation/)) |
| Splash and onboarding | D4 Release 1 — Splash and onboarding ([`completed/`](completed/)) | [A41 Splash and onboarding on Aero](../../android/plans/a41-splash-onboarding-aero.md) |
| Chat core | D5 Release 1 — Chat core ([`completed/`](completed/)) | [A42 Chat core on Aero](../../android/plans/a42-chat-core-aero.md) |
| Chat records and memory | D6 Release 1 — Chat records and memory ([`completed/`](completed/)) | [A43 Chat records and memory on Aero](../../android/plans/a43-chat-records-memory-aero.md) |
| Config and push | D7 Release 1 — Config and push ([`completed/`](completed/)) | [A44 Config and push on Aero](../../android/plans/a44-config-push-aero.md) |
| Close | [D8 Archive Stitch](d8-archive-stitch.md) | [A45 Remove Material 3 Expressive](../../android/plans/a45-remove-material3.md) |

Golds per flow (ids unchanged, inventory in [docs/qa/README.md](../../qa/README.md)):

| Flow | Golds |
|---|---|
| Home | `home0`, `home1`, `homeX`, `homeW` |
| Splash and onboarding | `splash`, `o1`, `o1e`, `o2`, `o3`, `o3t`, `o3s`, `o4` |
| Chat core | `chat0`, `chatL`, `chatQ`, `chatE`, `chatT`, `chatP`, `chatX` |
| Chat records and memory | `chatF`, `chatA`, `chatG`, `chatU`, `chatD`, `chatR`, `chatM`, `chatS` |
| Config and push | `cfg`, `cfgS`, `wipe`, `push` |

## Figma review gate

The gate that replaces the owner-run Stitch prompt for a migrated flow ([ADR-031](../adrs/ADR-031-figma-source-of-truth.md) § 5):

1. The agent builds the flow in Figma and screenshots every frame through the MCP.
2. The plan moves to `Pendente aprovação manual`. The owner opens the file and reviews the flow section. That is the owner's only manual step.
3. Requested fixes are applied in Figma and screenshotted again.
4. After the owner's OK, the agent exports the golds to `docs/qa/figma/{dark,light}/` and closes the plan.

## History

Completed plans move to `completed/`, created with the first completed plan.
