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
| Close | D8 Archive Stitch ([`completed/`](completed/)) | A45 Remove Material 3 Expressive ([`completed/`](../../android/plans/completed/)) |

Golds per flow (ids unchanged, inventory in [docs/qa/README.md](../../qa/README.md)):

| Flow | Golds |
|---|---|
| Home | `home0`, `home1`, `homeX`, `homeW`, `homeC`, `homeK`, `homeP` |
| Splash and onboarding | `splash`, `o1`, `o1e`, `o2`, `o3`, `o3t`, `o3s`, `o4`, `o5` |
| Chat core | `chat0`, `chatL`, `chatQ`, `chatE`, `chatT`, `chatP`, `chatX` |
| Chat records and memory | `chatF`, `chatA`, `chatG`, `chatU`, `chatD`, `chatR`, `chatM`, `chatS`, `chatRK`, `chatRB`, `chatRL` |
| Config and push | `cfg`, `cfgS`, `wipe`, `cfgR`, `cfgT`, `push` |

## Figma review gate

The gate for every new or changed layout ([ADR-031](../adrs/ADR-031-figma-source-of-truth.md) § 5):

1. The agent builds the flow in Figma and screenshots every frame through the MCP.
2. The plan moves to `Pendente aprovação manual`. The owner opens the file and reviews the flow section. That is the owner's only manual step.
3. Requested fixes are applied in Figma and screenshotted again.
4. After the owner's OK, the agent exports the golds to `docs/qa/figma/{dark,light}/` and closes the plan.

## Meal update follow-up

- Behavior: [ADR-032](../../produto/adrs/ADR-032-acrescimos-e-correcoes-de-refeicoes.md).
- Server: [meal-change contract](../../api-contract.md#meal-change-capability).
- Design: D9 — Chat meal updates ([`completed/`](completed/)).
- Client: A47 — Chat meal updates ([`completed/`](../../android/plans/completed/)).

Prerequisites and execution boundaries live in each plan. This follow-up does not change the migration plans above.

## Plan budget follow-up

Behavior: [ADR-039](../../produto/adrs/ADR-039-plan-cooking-and-budget-choice.md). Order (prerequisites live in each plan):

| Step | Plan | Depends on |
|---|---|---|
| Server | S21 — cooking help and the over-budget choice in a plan, delivered ([history](../../server/plans/completed/)) | S19, S20 |
| Design | D12 — plan over budget: the choice (`chatRB`) ([`completed/`](completed/)) | S21 approved |
| Client | [A50](../../android/plans/a50-plan-budget-choice.md) — the choice in the Chat | S21 on dev, D12, A47, A48, A49 |

## Home gestures and app reset follow-up

Behavior: [ADR-040](../../produto/adrs/ADR-040-home-card-gestures-app-reset.md). Order (prerequisites live in each plan):

| Step | Plan | Depends on |
|---|---|---|
| Design | D14 — Home: empty card copy ([`completed/`](completed/)) | — |
| Design | D15 — Config: app reset (`cfg`, `cfgR`) ([`completed/`](completed/)) | — |
| Client | A52 — Home: tap to record, long press to skip ([`completed/`](../../android/plans/completed/)) | D14 |
| Client | A53 — Config: app reset ([`completed/`](../../android/plans/completed/)) | D15 |

## Tone and closures follow-up

Behavior: [ADR-043](../../produto/adrs/ADR-043-plan-objective-protein-and-meal-window.md) and [ADR-044](../../produto/adrs/ADR-044-assistant-tone-and-closures.md). Order (prerequisites live in each plan):

| Step | Plan | Depends on |
|---|---|---|
| Client fix | A54 — auto-record of an addition into an empty meal ([`completed/`](../../android/plans/completed/)) | A47 |
| Server | S24 — protein-first plan inside the meal window ([`completed/`](../../server/plans/completed/)) | S23 |
| Design | D16 — tone choice and closures (`o5`, `cfgT`, `homeC`, `homeK`, `cfg`) ([`completed/`](completed/)) | D3, D4, D7 |
| Server | [S25](../../server/plans/s25-tone-and-closures.md) — tone per user and `/v1/close` | S24 |
| Client | [A55](../../android/plans/a55-tone-choice-and-closures.md) — tone choice, day and week closure | D16, S25 on dev, A50, A54 |

## Rich replies follow-up

Behavior: [ADR-045](../../produto/adrs/ADR-045-rich-replies-in-chat-bubbles.md). Order (prerequisites live in each plan):

| Step | Plan | Depends on |
|---|---|---|
| Design | D17 — emphasis, lists and a table in the bubbles (`chatR`, `chatE`, `chatRK`) ([`completed/`](completed/)) | D5, D6, D12 |
| Server | [S26](../../server/plans/s26-reply-formatting-subset.md) — reply formatting subset | S24 |
| Client | [A57](../../android/plans/a57-rich-reply-rendering.md) — rendering the subset | D17, S26 on dev, A50, A54, A55 |

## Planned meal follow-up

Behavior: [ADR-046](../../produto/adrs/ADR-046-planned-meal-reservation.md). Order (prerequisites live in each plan):

| Step | Plan | Depends on |
|---|---|---|
| Design | D18 — reserve action and planned timeline state (`chatR`, `chatRL`, `homeP`) ([`completed/`](completed/)) | D3, D6, D12, D17 |
| Server | [S27](../../server/plans/s27-planned-slot.md) — planned slot in DAY | S24 |
| Client | [A58](../../android/plans/a58-planned-meal-reservation.md) — reserve a plan for its meal | D18, S27 on dev, A50, A54, A55, A57 |

## Skips next to other actions follow-up

Behavior: [ADR-047](../../produto/adrs/ADR-047-skips-alongside-other-actions.md). Order (prerequisites live in each plan):

| Step | Plan | Depends on |
|---|---|---|
| Server | S29 — skip slots next to any intent, delivered ([history](../../server/plans/completed/)) | — |
| Design | D19 — two receipts and the delete proposal (`chatSK`, `chatSD`), delivered ([history](completed/)) | D6, D9 |
| Client | [A59](../../android/plans/a59-skips-with-other-actions.md) — skips next to other actions in the Chat | D19, S29 on dev |

Deferred by owner decision (2026-10-06): goal weight and date, [A56](../../android/plans/out_of_scope/a56-goal-weight.md).

## Fibrai brand follow-up

Names: [ADR-034](../../produto/adrs/ADR-034-fibrai-brand-tali-assistant.md). Recommended order (prerequisites live in each plan):

| Step | Plan | Depends on |
|---|---|---|
| Technical identity | A48 — `app.fibrai.android` everywhere and Firebase `fibrai-dev` ([`completed/`](../../android/plans/completed/)) ([ADR-036](../../android/adrs/ADR-036-fibrai-technical-identity.md)) | no parallel Android plan |
| Golds | D10 — Fibrai and Tali in the golds ([ADR-035](../../produto/adrs/ADR-035-tali-in-app-identity.md)) ([`completed/`](completed/)) | — |
| Model | S20 — Tali identity in the model instructions ([`completed/`](../../server/plans/completed/)) | D10 approved, S19 |
| Client | A49 — Fibrai and Tali in the app ([`completed/`](../../android/plans/completed/)) | D10, A48 |
| Docs | [SD5](../../sdd/plans/sd5-fibrai-docs-prose.md) — Fibrai in documentation and skills prose | A49 |
| Landing design | D11 — Landing page ([ADR-037](../../site/adrs/ADR-037-landing-site.md)) ([`completed/`](completed/)) | — |
| Landing code | W1 — Landing page code in `web/` ([ADR-038](../../site/adrs/ADR-038-web-project-folder.md)) ([`completed/`](../../site/plans/completed/)) | D11, D10 |
| Landing hosting | [W2](../../site/plans/w2-landing-hosting.md) — Landing hosting on fibrai.app | W1 |
| Logo design | D13 — Oat seed logo in `Branding`; logo-only `splash` golds ([`completed/`](completed/)) | D10 |
| Logo client | A51 — Logo-only splash in Compose ([`completed/`](../../android/plans/completed/)) | D13 |

## History

Completed plans move to `completed/`, created with the first completed plan.
