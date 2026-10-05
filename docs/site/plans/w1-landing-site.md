# Plan — W1 Landing site on fibrai.app

- Status: Aguardando aprovação
- Date: 05/10/2026
- Owning context: `site`
- Executable boundary:
  - `site/`: HTML, CSS, theme-switch JS, self-hosted fonts, images, Worker config and the token generator script;
  - `tools/deploy-site.ps1`;
  - Cloudflare configuration of the zones `fibrai.app` and `fibrai.com.br`.
  - No `apps/` or `server/` change.
- Related documentation: [site README](../README.md), [ADR-037](../adrs/ADR-037-landing-site.md), the [gold inventory](../../qa/README.md#golds), `AGENTS.md` (Live stack, Folder law) at Completion, `docs/README.md` matrix.
- Prerequisites:
  - [D11](../../design/plans/completed/d11-landing-page.md) `Concluído` with exported golds `land`, `landM` and `priv`;
  - [D10](../../design/plans/d10-fibrai-tali-rename.md) golds exported, so the app screenshots show Fibrai and Tali;
  - owner setup: a Cloudflare API token scoped to Workers Scripts:Edit and the `fibrai.app` zone (Workers Routes, DNS), saved as `CLOUDFLARE_API_TOKEN` in the owner's user environment, plus `CLOUDFLARE_ACCOUNT_ID`. The token is never printed or committed.

Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/site/plans/w1-landing-site.md. Implemente o plano aprovado.`

## Technical dependency (open)

| Dependency | Needed for | State on 05/10/2026 |
|---|---|---|
| `fibrai.com.br` registration at Registro.br (owner purchase, waiting for the confirmation e-mail) | Step 5, the redirect only | Pending. The owner has not received the Registro.br e-mail. |

Steps 1–4 do not wait for it. If it is still pending when steps 1–4 are done, the plan stays open in `pending_manual_validation/`. The rest is delivered, and the redirect is done when the owner confirms the registration.

## Objective

Publish a static Aero landing page at `https://fibrai.app`, matching the D11 golds, with an empty `/privacidade` page and an empty contact section, light by default with a dark/light switch.

## Scope

1. **Tokens:**
   - `site/tools/build-tokens.mjs` reads `docs/design/tokens.json` and writes `site/public/tokens.css`. Each variable becomes a CSS custom property per mode, `:root` for Light and `[data-theme="dark"]` for Dark. The text styles become classes.
   - Generated, never edited by hand. A `--check` flag fails when the CSS is stale.
2. **Pages:**
   - `site/public/index.html`, the home, following `land` (desktop) and `landM` (mobile, breakpoint from D11).
   - `site/public/privacidade/index.html` following `priv`: the empty placeholder, `noindex`.
   - Semantic HTML, pt-BR (`lang="pt-BR"`). One `h1`. Alt text on the app screenshots.
   - The app screenshots come from D11's exported web assets, as WebP or PNG optimized under 300 KB each.
3. **Look:**
   - Nunito Sans woff2 self-hosted under `site/public/fonts/`, with its OFL license file.
   - Aero glass uses `backdrop-filter` plus a `@supports` opaque fallback.
   - Phosphor icons are inline SVG.
   - `prefers-reduced-motion` disables decorative motion.
4. **Theme switch:**
   - `site/public/theme.js`. The default is **light**, ignoring the system theme.
   - The button toggles `data-theme` on `<html>` and remembers the choice in `localStorage`, wrapped in try/catch.
   - A tiny inline head script applies the saved choice before first paint, so there is no flash.
   - The button has an accessible name ("Ativar tema escuro" / "Ativar tema claro").
5. **Hosting and domains:**
   - `site/wrangler.jsonc`: a Worker named `fibrai-site` with static assets from `site/public/`.
   - Custom domain `fibrai.app`; a `www.fibrai.app` → apex 301 rule.
   - Security headers: HSTS, which `.app` requires; `Content-Security-Policy` with self only; `X-Content-Type-Options`; `Referrer-Policy: strict-origin-when-cross-origin`.
   - `tools/deploy-site.ps1` runs `build-tokens --check`, then `npx wrangler deploy`.
   - **After the dependency closes:**
     - add the zone `fibrai.com.br` in the same Cloudflare account and give its nameservers to the owner for Registro.br;
     - add proxied placeholder records for `@` and `www`;
     - add a Redirect Rule with a 301 to `https://fibrai.app` + path, keeping the query string;
     - enable DNSSEC and hand the DS record to the owner.
6. **Production guard (ADR-037 § 6):**
   - No analytics, cookies, forms, sign-up, invite, download, store badge or APK.
   - The footer links `/privacidade`.
   - "Contato" has no content.
7. **Documentation at Completion:**
   - `AGENTS.md` Live stack gains `site/` (static, Cloudflare, ADR-037).
   - The Folder law gains `docs/qa/site/current/{dark,light}/`.
   - The site README lists the live page.

## Out of scope

- Privacy policy text and contact data ([CP8](../../content-policy/plans/out_of_scope/cp8-public-legal-pack.md), owner).
- Any way to install the app.
- Logo and final avatar art.
- Languages other than pt-BR.
- Analytics.

## Validation

1. `node site/tools/build-tokens.mjs --check` passes.
2. Local preview of desktop 1440 px and mobile 390 px, light and dark:
   - captures go to `docs/qa/site/current/{dark,light}/{land,landM,priv}.png`;
   - a written diff list against the D11 golds;
   - iterate until they match.
3. Lighthouse (local) Accessibility ≥ 95 and no console errors. HTML validates. No horizontal scroll at 390 px. Tab focus is visible on the switch and the links.
4. After deploy:
   - `curl -I https://fibrai.app` shows 200, HSTS and CSP;
   - `www` returns a 301;
   - `/privacidade` returns 200 with `noindex`.
   - After the dependency: `curl -I https://fibrai.com.br/x` returns a 301 to `https://fibrai.app/x`.
5. `grep` in `site/` finds no `<form`, `analytics`, `gtag`, `.apk` or `play.google`.
6. `node tools/check-docs.mjs` passes.

## Results

Planning only.
