# ADR-037 — Landing site at fibrai.app: static, Aero, Cloudflare

- Status: Proposed (2026-10-05; accepted with the owner's approval of [D11](../../design/plans/d11-landing-page.md))
- Date: 2026-10-05
- Context: `site`
- Replaces: nothing. It opens the `site` context and its code folder `site/`.

## Context

The owner bought `fibrai.app` on Cloudflare and is buying `fibrai.com.br` at Registro.br. The owner wants a landing page that:

- explains the app and shows screens;
- has a privacy-policy link that stays empty for now and is a hard block for prod;
- has an empty contact section;
- follows the app's Aero look (Frutiger Aero, same palette, same typography);
- has a dark/light switch that starts in light.

The app has no web surface today. AGENTS allows only `apps/android/` and `server/` as live code.

## Decision

1. **A new context `site` and code folder `site/`.** The site is static: HTML, CSS and a small vanilla JavaScript file for the theme switch. There is no framework, no build server and no backend.
2. **Hosting:** a Cloudflare Worker with static assets serves `fibrai.app`. `www.fibrai.app` redirects to the apex. Deployment is `wrangler deploy` through `tools/deploy-site.ps1`, with a `CLOUDFLARE_API_TOKEN` scoped to this zone and Workers in the owner's user environment. The token is never printed or committed.
3. **`fibrai.com.br`** becomes a Cloudflare zone in the same account with a 301 redirect to `https://fibrai.app`, path kept. This depends on the Registro.br registration, tracked in [W1](../plans/w1-landing-site.md).
4. **Look:**
   - Aero tokens come from `docs/design/tokens.json`, the same source as the app, generated into CSS custom properties. No hand-copied values.
   - Nunito Sans is self-hosted (OFL), with no Google Fonts request.
   - Glass uses `backdrop-filter` with an opaque fallback. Phosphor SVG icons.
   - The layout comes from the Figma `Design` page "Landing page" ([D11](../../design/plans/d11-landing-page.md)), with desktop and mobile golds in both themes.
5. **Theme:**
   - The site starts in **light** whatever the system theme is.
   - A visible switch toggles dark/light, and the choice is remembered in `localStorage`.
   - This differs on purpose from the app, which follows the system with no toggle (AGENTS § Tokens).
6. **Privacy and production:**
   - No analytics, cookies, forms, sign-up, invite, download link, store badge or APK.
   - `/privacidade` exists as an empty placeholder page linked from the footer.
   - Its content is the pt-BR Privacy Policy owned by [CP8](../../content-policy/plans/out_of_scope/cp8-public-legal-pack.md) and blocked by PG2 of the [production gate](../../content-policy/production-gate.md).
   - Adding any way to get the app, or any data collection, to the site is a production trigger.
7. **Contact:** a "Contato" section exists with no address, form or link until the owner decides.

## Motivation

- A static site has almost no security or privacy surface and nothing to keep running. Cloudflare already holds the domain.
- Generating CSS from the same `tokens.json` keeps the site and app on one palette without copying values.
- No tracking means nothing that collects personal data, so the empty privacy page cannot mislead anyone in the meantime.

## Consequences

### Positive

- One more surface for the brand, at no hosting cost on Cloudflare's free tier.

### Negative

- New tooling: `wrangler`, which `tools/deploy-site.ps1` runs through npx, and a Cloudflare API token in the user environment.
- Visual QA for the site uses browser captures in `docs/qa/site/current/{dark,light}/`, a new folder. AGENTS "Folder law" changes at W1 Completion.
- App screenshots on the site come from the exported Figma golds. They must be refreshed when those golds change.

## Alternatives considered

- **Cloudflare Pages:** works, but new Cloudflare static projects are steered to Workers static assets. One Worker also covers future headers and redirects.
- **A framework such as Astro or Next:** rejected. A one-page site does not need a build chain.
- **Host on the GCP VM with Caddy (ADR-013):** rejected. It mixes the dev API host with a public site and adds load to an e2-micro.
- **Follow the system theme:** rejected by the owner, who wants light by default.

## Relations

- [ADR-030](../../design/adrs/ADR-030-own-design-system-aero.md) (Aero), [ADR-031](../../design/adrs/ADR-031-figma-source-of-truth.md) (Figma source), [ADR-034](../../produto/adrs/ADR-034-fibrai-brand-tali-assistant.md) (brand).
- Plans: [D11](../../design/plans/d11-landing-page.md), [W1](../plans/w1-landing-site.md).
- [Production gate](../../content-policy/production-gate.md) PG2 (privacy policy).

Once accepted, this ADR is not edited. A later change needs a new ADR that declares the replacement.
