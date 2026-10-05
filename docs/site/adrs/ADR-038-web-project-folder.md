# ADR-038 — Landing code lives in the `web/` project at the repository root

- Status: Proposed (2026-10-05; accepted with the owner's approval of [W1](../plans/w1-landing-site.md))
- Date: 2026-10-05
- Context: `site`
- Replaces: part of [ADR-037](ADR-037-landing-site.md) § Decision:
  - the code folder `site/` of item 1;
  - the deploy script name `tools/deploy-site.ps1` of item 2.

  Everything else in ADR-037 stays in force: static site, Cloudflare Worker, light by default, no tracking, empty `/privacidade` and "Contato".

## Context

ADR-037 named the landing code folder `site/`. While planning the HTML/CSS/JS delivery, the owner asked for "um novo projeto chamado web" at the repository root to hold the landing page code (2026-10-05).

## Decision

1. **The landing page code is the npm project `web/`** at the repository root, next to `apps/` and `server/`. It is `private` and has no runtime dependencies. Its dev dependencies are build and QA tools only.
2. **Layout:**
   - `web/public/` is the only folder served: HTML, CSS, JavaScript, fonts and images;
   - `web/tools/` holds the generators and QA scripts;
   - `web/package.json` holds the scripts.
   - Hosting config (`web/wrangler.jsonc`) and the deploy script `tools/deploy-web.ps1` arrive with the hosting plan [W2](../plans/w2-landing-hosting.md).
3. **The documentation context keeps the name `site`** (`docs/site/`). Browser captures keep the ADR-037 folder `docs/qa/site/current/{dark,light}/`. Only the code folder and the deploy script name change.
4. **No framework and no bundler**, as in ADR-037. The npm project exists for repeatable scripts (token CSS, screen images, fonts, preview, captures, checks), not for a build chain. `web/public/` is valid as served, with no compile step.

## Motivation

- This is the owner's explicit decision.
- `web/` says what the folder holds (the web client) next to `apps/android/` and `server/`, and it leaves room for future web pages without renaming.
- A `package.json` scoped to `web/` keeps its tools (Playwright, image and HTML checkers) out of the app and server toolchains.

## Consequences

### Positive

- One folder for everything the landing ships and checks, so a client `/goal` touches a single folder.

### Negative

- The context name (`site`) and the code folder (`web/`) differ. The [site README](../README.md) and the [docs matrix](../../README.md) state the mapping.
- AGENTS "Live stack" gains `web/` at W1 Completion.

## Alternatives considered

- **Keep `site/`:** rejected by the owner.
- **Rename the context to `web` too:** it would mean moving `docs/site/` and its links with no benefit to the code. It can be its own documentation plan later.

## Relations

- [ADR-037](ADR-037-landing-site.md) (partially replaced), [ADR-030](../../design/adrs/ADR-030-own-design-system-aero.md) (Aero tokens).
- Plans: [W1](../plans/w1-landing-site.md) (web project), [W2](../plans/w2-landing-hosting.md) (hosting and domains).

Once accepted, this ADR is not edited. A later change needs a new ADR that declares the replacement.
