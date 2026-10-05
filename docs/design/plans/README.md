# Design migration plans

Migration from Material 3 Expressive and Stitch to the Aero design system in Figma ([ADR-030](../adrs/ADR-030-own-design-system-aero.md), [ADR-031](../adrs/ADR-031-figma-source-of-truth.md)). Plan state: the State line of each plan and its folder. This index does not authorize implementation.

## Order

Each flow is one design plan (Figma, within one day of MCP budget) followed by one client plan (Compose). A flow's client plan starts only after its design plan.

| Step | Design (this context) | Client ([android](../../android/README.md)) |
|---|---|---|
| Foundation | D1 Figma file foundation ([`completed/`](completed/)) | A39 Aero foundation in Compose ([`completed/`](../../android/plans/completed/)) |
| Tooling | D2 Figma tooling and Stitch deprecation ([`completed/`](completed/)) | — |
| Home | D3 Release 1 — Home ([`completed/`](completed/)) | A40 Home on Aero ([`completed/`](../../android/plans/completed/)) |
| Splash and onboarding | D4 Release 1 — Splash and onboarding ([`completed/`](completed/)) | A41 Splash and onboarding on Aero ([`completed/`](../../android/plans/completed/)) |
| Chat core | D5 Release 1 — Chat core ([`completed/`](completed/)) | A42 Chat core on Aero ([`completed/`](../../android/plans/completed/)) |
| Chat records and memory | D6 Release 1 — Chat records and memory ([`completed/`](completed/)) | A43 Chat records and memory on Aero ([`completed/`](../../android/plans/completed/)) |
| Config and push | D7 Release 1 — Config and push ([`completed/`](completed/)) | A44 Config and push on Aero ([`completed/`](../../android/plans/completed/)) |
| Close | [D8 Archive Stitch](d8-archive-stitch.md) | A45 Remove Material 3 Expressive ([`completed/`](../../android/plans/completed/)) |

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

## Meal update follow-up

- Behavior: [ADR-032](../../produto/adrs/ADR-032-acrescimos-e-correcoes-de-refeicoes.md).
- Server: [meal-change contract](../../api-contract.md#meal-change-capability).
- Design: [D9 — Chat meal updates](d9-chat-meal-updates.md).
- Client: [A47 — Chat meal updates](../../android/plans/a47-chat-meal-updates.md).

Prerequisites and execution boundaries live in each plan. This follow-up does not change the migration plans above.

## Fibrai brand follow-up

Names: [ADR-034](../../produto/adrs/ADR-034-fibrai-brand-tali-assistant.md). Recommended order (prerequisites live in each plan):

| Step | Plan | Depends on |
|---|---|---|
| Technical identity | [A48](../../android/plans/a48-fibrai-app-id-firebase.md) — `app.fibrai.android` everywhere and Firebase `fibrai-dev` ([ADR-036](../../android/adrs/ADR-036-fibrai-technical-identity.md)) | no parallel Android plan |
| Golds | [D10](d10-fibrai-tali-rename.md) — Fibrai and Tali in the golds ([ADR-035](../../produto/adrs/ADR-035-tali-in-app-identity.md)) | — |
| Model | [S20](../../server/plans/s20-tali-prompt-identity.md) — Tali identity in the model instructions | D10 approved, S19 |
| Client | [A49](../../android/plans/a49-fibrai-tali-visible-rename.md) — Fibrai and Tali in the app | D10, A48 |
| Docs | [SD5](../../sdd/plans/sd5-fibrai-docs-prose.md) — Fibrai in documentation and skills prose | A49 |
| Landing design | D11 — Landing page ([ADR-037](../../site/adrs/ADR-037-landing-site.md)) ([`completed/`](completed/)) | — |
| Landing code | [W1](../../site/plans/w1-landing-site.md) — Landing page code in `web/` ([ADR-038](../../site/adrs/ADR-038-web-project-folder.md)) | D11, D10 |
| Landing hosting | [W2](../../site/plans/w2-landing-hosting.md) — Landing hosting on fibrai.app | W1 |

## History

Completed plans move to `completed/`, created with the first completed plan.
