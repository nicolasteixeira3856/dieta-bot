# Plan — W2 Landing hosting on fibrai.app

- Status: Aguardando aprovação
- Date: 05/10/2026
- Owning context: `site`
- Executable boundary:
  - `web/wrangler.jsonc` and the Worker config in `web/`;
  - `tools/deploy-web.ps1`;
  - Cloudflare configuration of the zones `fibrai.app` and `fibrai.com.br`.
  - No change to `web/public/` content (that is [W1](w1-landing-site.md)), `apps/` or `server/`.
- Related documentation: [site README](../README.md), [ADR-037](../adrs/ADR-037-landing-site.md), [ADR-038](../adrs/ADR-038-web-project-folder.md), [production gate](../../content-policy/production-gate.md).
- Prerequisites:
  - [W1](w1-landing-site.md) `Concluído`;
  - owner setup:
    - a Cloudflare API token scoped to Workers Scripts:Edit and the `fibrai.app` zone (Workers Routes, DNS), saved as `CLOUDFLARE_API_TOKEN` in the owner's user environment;
    - `CLOUDFLARE_ACCOUNT_ID`.
    - The token is never printed or committed.

Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/site/plans/w2-landing-hosting.md. Implemente o plano aprovado.`

## Technical dependency (open)

| Dependency | Needed for | State on 05/10/2026 |
|---|---|---|
| `fibrai.com.br` registration at Registro.br (owner purchase, waiting for the confirmation e-mail) | Step 4, the redirect only | Pending. The owner has not received the Registro.br e-mail. |

Steps 1–3 do not wait for it. If it is still pending when steps 1–3 are done, the plan stays open in `pending_manual_validation/`. The redirect follows once the owner confirms the registration.

## Objective

Publish `web/public/` at `https://fibrai.app` through a Cloudflare Worker with static assets (ADR-037 § 2). Redirect `www.fibrai.app`, and later `fibrai.com.br`, to the apex.

## Scope

1. **Worker (`web/wrangler.jsonc`):**
   - a Worker named `fibrai-site` with static assets from `web/public/`;
   - custom domain `fibrai.app`;
   - `wrangler` as a dev dependency of `web/` (pinned), run through `npm --prefix web exec`.
2. **Headers and redirects** (a `web/public/_headers` file, or the Worker if `_headers` cannot cover it):
   - HSTS, which `.app` requires;
   - `Content-Security-Policy: default-src 'self'; img-src 'self' data:; style-src 'self'; script-src 'self'; font-src 'self'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'`;
   - `X-Content-Type-Options: nosniff`;
   - `Referrer-Policy: strict-origin-when-cross-origin`;
   - `/privacidade` also sends `X-Robots-Tag: noindex`;
   - a `www.fibrai.app` → apex 301 rule that keeps path and query.
3. **Deploy script (`tools/deploy-web.ps1`):**
   1. checks that the two environment variables are set, without printing them;
   2. runs `npm --prefix web ci` and `npm --prefix web run check`;
   3. runs `npm --prefix web exec -- wrangler deploy`.
   - A failed check stops the deploy.
4. **After the dependency closes:**
   - add the zone `fibrai.com.br` in the same Cloudflare account and give its nameservers to the owner for Registro.br;
   - add proxied placeholder records for `@` and `www`;
   - add a Redirect Rule: 301 to `https://fibrai.app` + path, keeping the query string;
   - enable DNSSEC and hand the DS record to the owner.
5. **Production guard (ADR-037 § 6):** deploy only what W1 produced. Adding any way to get the app, analytics or data collection is a production trigger ([production gate](../../content-policy/production-gate.md)).
6. **Documentation at Completion:**
   - the [site README](../README.md) lists the live URL and `tools/deploy-web.ps1`;
   - `AGENTS.md` Live stack names the host (Cloudflare Worker, ADR-037/038).

## Out of scope

- Page content and layout ([W1](w1-landing-site.md)).
- E-mail on the domains, analytics, any app distribution.

## Validation

1. `tools/deploy-web.ps1` runs end to end. The token never appears in output or logs.
2. After deploy:
   - `curl -I https://fibrai.app` shows 200, HSTS, the CSP and `nosniff`;
   - `curl -I https://www.fibrai.app/x?y=1` returns a 301 to `https://fibrai.app/x?y=1`;
   - `/privacidade` returns 200 with `noindex`.
3. The live pages load with no console error or CSP violation, in both themes. A live capture at 1440 and 390 px matches the W1 local captures.
4. After the dependency: `curl -I https://fibrai.com.br/x` returns a 301 to `https://fibrai.app/x`, and DNSSEC validates.
5. `node tools/check-docs.mjs` passes.

## Results

Planning only.
