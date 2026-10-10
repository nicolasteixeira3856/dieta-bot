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
| Splash and onboarding | `splash`, `o1`, `o1e`, `o2`, `o3`, `o3t`, `o3s`, `o4`, `o5` (`o1`–`o5` retired by D27: `ob0`–`ob6`) |
| Chat core | `chat0`, `chatL`, `chatQ`, `chatE`, `chatT`, `chatP`, `chatX`, `chatCP`, `chatCC` |
| Chat records and memory | `chatF`, `chatA`, `chatG`, `chatU`, `chatD`, `chatR`, `chatM`, `chatS`, `chatRK`, `chatRB`, `chatRL` |
| Config and push | `cfg`, `cfgS`, `wipe`, `cfgR`, `cfgT`, `push` |
| Recipes (`Release 2`) | `rcpL`, `rcpD` |
| Visible memory (`Release 2`) | `memL`; `cfg` redrawn on `Release 2` |
| Plan options (`Release 2`) | `chatO` |

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
| Client | A50 (cancelled, now A60 ([history](../../android/plans/completed/))) — the choice in the Chat | S21 on dev, D12, A47, A48, A49 |

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
| Server | S25 (cancelled, now S30 ([`completed/`](../../server/plans/completed/))) — tone per user and `/v1/close` | S24 |
| Client | A55 (cancelled, now A60 ([history](../../android/plans/completed/))) — tone choice, day and week closure | D16, S25 on dev, A50, A54 |

## Rich replies follow-up

Behavior: [ADR-045](../../produto/adrs/ADR-045-rich-replies-in-chat-bubbles.md). Order (prerequisites live in each plan):

| Step | Plan | Depends on |
|---|---|---|
| Design | D17 — emphasis, lists and a table in the bubbles (`chatR`, `chatE`, `chatRK`) ([`completed/`](completed/)) | D5, D6, D12 |
| Server | S26 (cancelled, now S30 ([`completed/`](../../server/plans/completed/))) — reply formatting subset | S24 |
| Client | A57 (cancelled, now A60 ([history](../../android/plans/completed/))) — rendering the subset | D17, S26 on dev, A50, A54, A55 |

## Planned meal follow-up

Behavior: [ADR-046](../../produto/adrs/ADR-046-planned-meal-reservation.md). Order (prerequisites live in each plan):

| Step | Plan | Depends on |
|---|---|---|
| Design | D18 — reserve action and planned timeline state (`chatR`, `chatRL`, `homeP`) ([`completed/`](completed/)) | D3, D6, D12, D17 |
| Server | S27 (cancelled, now S30 ([`completed/`](../../server/plans/completed/))) — planned slot in DAY | S24 |
| Client | A58 (cancelled, now A60 ([history](../../android/plans/completed/))) — reserve a plan for its meal | D18, S27 on dev, A50, A54, A55, A57 |

## Skips next to other actions follow-up

Behavior: [ADR-047](../../produto/adrs/ADR-047-skips-alongside-other-actions.md). Order (prerequisites live in each plan):

| Step | Plan | Depends on |
|---|---|---|
| Server | S29 — skip slots next to any intent, delivered ([history](../../server/plans/completed/)) | — |
| Design | D19 — two receipts and the delete proposal (`chatSK`, `chatSD`), delivered ([history](completed/)) | D6, D9 |
| Client | A59 ([history](../../android/plans/completed/)) (part E of A60 ([history](../../android/plans/completed/))) — skips next to other actions in the Chat | D19, S29 on dev |

Deferred by owner decision (2026-10-06): goal weight and date, [A56](../../android/plans/out_of_scope/a56-goal-weight.md).

## Figma review and Chat actions follow-up

Behavior: [ADR-048](../../produto/adrs/ADR-048-chat-actions-in-thread-copy-scroll-capture.md). Order (prerequisites live in each plan):

| Step | Plan | Depends on |
|---|---|---|
| Design | D20 — stale frames, Chat actions in the thread, copying messages (`chatCP`, `chatCC`), delivered ([history](completed/)) | D12, D16–D19, A60 merged |
| Client | A61 ([history](../../android/plans/completed/)) — actions in the thread, copying messages, scrolling screenshot | D20 (parts A and B) |
| Design | D22 — `chatM` with Registrar in the thread (missed by D20), delivered ([history](completed/)) | D20, A61 merged |
| Client | A62 — `chatM` gated whole in `GoldTest` and captured on the emulator, delivered ([history](../../android/plans/completed/)) | D22 |

## Text contrast follow-up

Finding: the D20 owner review ([history](completed/)). Order (prerequisites live in each plan):

| Step | Plan | Depends on |
|---|---|---|
| Design | D21 — Light `text/dim` contrast (Light golds that show `text/dim`, site tokens and phone screens), delivered ([history](completed/)) | D20, A61 merged |
| Client | A63 — Light `textDim` in the app: contrast test, `cfgS` mode label, Roborazzi re-record, Light captures, delivered ([history](../../android/plans/completed/)) | D21 |

## Fibrai brand follow-up

Names: [ADR-034](../../produto/adrs/ADR-034-fibrai-brand-tali-assistant.md). Recommended order (prerequisites live in each plan):

| Step | Plan | Depends on |
|---|---|---|
| Technical identity | A48 — `app.fibrai.android` everywhere and Firebase `fibrai-dev` ([`completed/`](../../android/plans/completed/)) ([ADR-036](../../android/adrs/ADR-036-fibrai-technical-identity.md)) | no parallel Android plan |
| Golds | D10 — Fibrai and Tali in the golds ([ADR-035](../../produto/adrs/ADR-035-tali-in-app-identity.md)) ([`completed/`](completed/)) | — |
| Model | S20 — Tali identity in the model instructions ([`completed/`](../../server/plans/completed/)) | D10 approved, S19 |
| Client | A49 — Fibrai and Tali in the app ([`completed/`](../../android/plans/completed/)) | D10, A48 |
| Docs | SD5 — Fibrai in documentation and skills prose ([`completed/`](../../sdd/plans/completed/)) | A49 |
| Landing design | D11 — Landing page ([ADR-037](../../site/adrs/ADR-037-landing-site.md)) ([`completed/`](completed/)) | — |
| Landing code | W1 — Landing page code in `web/` ([ADR-038](../../site/adrs/ADR-038-web-project-folder.md)) ([`completed/`](../../site/plans/completed/)) | D11, D10 |
| Landing hosting | [W2](../../site/plans/w2-landing-hosting.md) — Landing hosting on fibrai.app | W1 |
| Logo design | D13 — Oat seed logo in `Branding`; logo-only `splash` golds ([`completed/`](completed/)) | D10 |
| Logo client | A51 — Logo-only splash in Compose ([`completed/`](../../android/plans/completed/)) | D13 |

## Chat brainstorm follow-up (Release 2)

Behavior: [ADR-052](../../produto/adrs/ADR-052-saved-recipes.md), [ADR-053](../../produto/adrs/ADR-053-visible-memory-screen.md), [ADR-051](../../produto/adrs/ADR-051-plan-option-identity-and-chat-discovery.md). Order (prerequisites live in each plan):

| Step | Plan | Depends on |
|---|---|---|
| Design | D23 — Recipes (`rcpL`, `rcpD`, `chatRK`), delivered ([history](completed/)) | D22, ADR-052 accepted (S38) |
| Client | A68 ([history](../../android/plans/completed/)) — saved recipes | D23, S38 on dev, A67 |
| Design | D24 — "O que a Tali sabe" (`memL`, `cfg`), delivered ([history](completed/)) | D23, ADR-053 accepted (A69) |
| Client | A69 ([history](../../android/plans/completed/)) — memory screen | D24, A68 |
| Design | D25 — plan option control (`chatO`), delivered ([history](completed/)) | D24, ADR-051 accepted (S37) |
| Client | A67 ([history](../../android/plans/completed/)) — options and discovery | D25, S37 on dev, A66 |

## Plan decision follow-up

Behavior: [ADR-056](../../produto/adrs/ADR-056-plan-decision-line-and-option-budget.md). Order (prerequisites live in each plan):

| Step | Plan | Depends on |
|---|---|---|
| Server | [S39](../../server/plans/pending_manual_validation/s39-plan-decision-line-and-option-budget.md) — decision line, budget on the chosen option | S37, S38 on dev |
| Design | D26 — decision line and per-option fit (`chatO` redrawn), delivered ([history](completed/)) | D25, ADR-056 accepted (S39) |
| Client | [A70](../../android/plans/pending_manual_validation/a70-option-fit-and-projection.md) — fit and day lines per option | D26, S39 on dev, A67 |

## Day 0 and history follow-up (Release 2)

Behavior: [ADR-057](../../produto/adrs/ADR-057-conversational-onboarding.md), [ADR-058](../../produto/adrs/ADR-058-extras-and-history.md) (brainstorm of 09/10/2026). Order (prerequisites live in each plan):

| Step | Plan | Depends on |
|---|---|---|
| Server | S40 — eval personas, delivered ([history](../../server/plans/completed/)) | — |
| Design | D27 — conversational onboarding (`ob0`–`ob6`; `o1`–`o5` retired), delivered ([history](completed/)) | D26, ADR-057 accepted |
| Server | [S41](../../server/plans/pending_manual_validation/s41-onboarding-profile.md) — `POST /v1/profile` | S40 |
| Client | [A71](../../android/plans/pending_manual_validation/a71-conversational-onboarding.md) — conversational onboarding in the app | D27, S41 on dev, A70 merged |
| Design | D28 — Home day strip, past day and extras (`home1` redrawn, `homeH`, `homeE`, `chatGX`), delivered ([history](completed/)) | D27, ADR-058 accepted |
| Server | [S42](../../server/plans/pending_manual_validation/s42-extras-and-other-day.md) — extras and a record in a named past day | S40 |
| Client | [A72](../../android/plans/pending_manual_validation/a72-extras-and-history.md) — extras, the strip and the past-day record | D28, S42 on dev, A71 |

## Landing screens follow-up

The landing phones are clones of app golds; D27 retired `o1` and D28 redrew `home1`. Order (prerequisites live in each plan):

| Step | Plan | Depends on |
|---|---|---|
| Landing design | D29 — `ob3` and the new `home1` in the phones of `land` and `landM`, delivered ([history](completed/)) | D27, D28 |
| Landing code | W3 — re-cut the phone screens in `web/`, delivered ([history](../../site/plans/completed/)) | D29 |

## Chat gold drift follow-up

The app shows two states that their golds do not draw: the `Extra` row of Trocar (ADR-058, A72) and the day balance on the extra receipt (A64). Order (prerequisites live in each plan):

| Step | Plan | Depends on |
|---|---|---|
| Design | [D30](d30-chat-extra-row-and-balance-line.md) — `chatT` with the Extra row, `chatGX` with the balance line | D28, D29 |
| Client | [A73](../../android/plans/a73-chat-gold-gates.md) — `chatT` and `chatGX` gated again | D30, A72 merged |

## History

Completed plans move to `completed/`, created with the first completed plan.
