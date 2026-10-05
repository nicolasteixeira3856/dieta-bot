# Plan — W1 Landing page code in `web/`

- Status: Aguardando aprovação
- Date: 05/10/2026
- Owning context: `site`
- Executable boundary: the new npm project `web/` (`package.json`, `public/`, `tools/`). Captures go in `docs/qa/site/current/{dark,light}/`. At Completion, the documentation listed in step 8. No `apps/`, `server/` or root `tools/` change, and no hosting: [W2](w2-landing-hosting.md).
- Related documentation: [site README](../README.md), [ADR-037](../adrs/ADR-037-landing-site.md), [ADR-038](../adrs/ADR-038-web-project-folder.md), [D11](../../design/plans/completed/d11-landing-page.md) (layout, copy, contrast), the [gold inventory](../../qa/README.md#golds), [docs/tokens.md](../../tokens.md).
- Prerequisites:
  - approval of this plan accepts [ADR-038](../adrs/ADR-038-web-project-folder.md);
  - [D11](../../design/plans/completed/d11-landing-page.md) `Concluído` (golds `land`, `landM`, `priv`): met;
  - [D10](../../design/plans/pending_manual_validation/d10-fibrai-tali-rename.md) `Concluído`. Its scope item 7 refreshes the phone screens of `land` and `landM`, and its re-exported `o1`, `home1` and `chatE` golds show Fibrai and Tali. Without it the page would ship the old name.

Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/site/plans/w1-landing-site.md. Implemente o plano aprovado.`

## Objective

Build the Fibrai landing page as static HTML, CSS and a little vanilla JavaScript in `web/`, matching the D11 golds in both themes. The result runs locally and is ready for [W2](w2-landing-hosting.md) to publish.

## Sources

| Source | Gives |
|---|---|
| Golds `land` (desktop 1440), `landM` (mobile 390), `priv` (desktop 1440) in `docs/qa/figma/{light,dark}/` | layout, the only visual reference |
| [D11 Results](../../design/plans/completed/d11-landing-page.md#final-composition-2026-10-05) | structure, exact pt-BR copy, the WCAG fix (hero and nav texts in `text/primary`) |
| `docs/design/tokens.json` | every color per mode, radius, spacing, blur, text style (including `Web/*`), paint and effect style |
| Golds `o1`, `home1`, `chatE` | the three phone screens |
| `@phosphor-icons/core` 2.1.1 | sun, moon and info icons (the SVGs used in Figma) |

Copy is taken verbatim from the gold and D11 Results. A copy change is a D11 follow-up in Figma first, not a code edit.

## Scope

1. **Project skeleton (`web/`):**

   ```text
   web/
   ├── package.json        private, "type": "module", scripts below
   ├── public/             served as is (W2 publishes this folder)
   │   ├── index.html
   │   ├── privacidade/index.html
   │   ├── css/tokens.css  generated
   │   ├── css/site.css
   │   ├── js/theme-init.js
   │   ├── js/theme.js
   │   ├── fonts/          Nunito Sans woff2 + OFL.txt
   │   └── img/screens/{light,dark}/{o1,home1,chatE}.webp   generated
   └── tools/
       ├── build-tokens.mjs
       ├── build-screens.mjs
       ├── copy-fonts.mjs
       ├── preview.mjs
       └── capture.mjs
   ```

   - Scripts: `tokens` / `tokens:check`, `screens` / `screens:check`, `fonts`, `preview`, `capture`, `check` (every `--check` + HTML validation + forbidden-content grep).
   - Dev dependencies, pinned in `package-lock.json`:
     - `playwright` (same major as `tools/`) for captures;
     - `pngjs` for the gold diff;
     - `sharp` for the WebP screens;
     - `html-validate`;
     - `@fontsource-variable/nunito-sans` (OFL) as the font source.
   - No runtime dependency. `web/node_modules/` is git-ignored.
2. **Tokens (`web/tools/build-tokens.mjs`):**
   - **Input and output:** reads `docs/design/tokens.json`, writes `web/public/css/tokens.css` with a "generated, do not edit" header. `--check` fails when the file is stale.
   - **Color variables:** `bg/page` → `--bg-page`. Light values go on `:root`, Dark values on `:root[data-theme="dark"]`.
   - **Shape:** `--radius-card`, `--radius-pill`, `--space-*`, `--blur-glass`.
   - **Paint styles** become gradients per mode (`--paint-background-page`, `--paint-surface-glass`).
   - **Effect style `Glass`** becomes `--glass-blur` and `--glass-shadow`.
   - **Text styles** become classes `.t-<name>` (for example `.t-web-display`, `.t-body`, `.t-label-section` with uppercase and 8 % tracking) and matching custom properties.
   - No value is typed by hand in `site.css`. Layout numbers that are not tokens (1200 px content width, the 120 px desktop margin, the 300 px phone screen) are named in one block at the top of `site.css`.
3. **Pages:**
   - **`index.html`** (one `h1`, `lang="pt-BR"`, semantic landmarks):
     - `header`: wordmark "Fibrai", nav anchors `#como-funciona`, `#tali`, `#contato`, theme switch;
     - `main`:
       - hero (eyebrow, `h1`, lead, three facts as a list, disclaimer);
       - a `figure` with the three phones;
       - `section#como-funciona` (three steps as an ordered list);
       - `section#tali` (avatar + text);
       - the estimate note;
       - `section#contato` (title only);
     - `footer`: wordmark, disclaimer, link `/privacidade`.
   - **`privacidade/index.html`** (`priv`): same header and footer, `h1` "Política de privacidade", "Em breve.", `<meta name="robots" content="noindex">`.
   - **Head:** `<title>` and the meta description from the lead. The favicon is `<link rel="icon" href="data:,">` because there is no logo yet (ADR-034), so the browser makes no 404 request. No Open Graph image.
4. **Layout and look:**
   - **Breakpoints:**
     - below 768 px the page follows `landM` (20 px margins, one phone `home1`, stacked steps, `Web/Heading` H1, `Body` lead, wrapping facts);
     - from 768 px it follows `land`, with the content capped at 1200 px and centered.
     - `priv` uses the same breakpoint.
   - **Phones:** a 316 × 665 frame (8 px `surface/2` bezel, 1 px `border/glass` edge, 44 px radius) around the screen image.
     - The side phones (`o1`, `chatE`) sit 48 px lower and are hidden below 768 px with `loading="lazy"`.
     - Each phone carries both theme images. CSS shows the one that matches `data-theme`.
   - **Glass cards and chips:** `backdrop-filter: blur(var(--blur-glass))` with the glass fill. An `@supports not (backdrop-filter: blur(1px))` fallback uses the opaque `surface/2`. Body text never sits on glass without that fallback (ADR-031 § 3).
   - **Decorative bubbles:** absolutely positioned `aria-hidden` elements with the radial gradient and `border/glass` ring, at the gold positions, never over text.
   - **Icons:** inline Phosphor SVG with `currentColor`, colored through tokens.
   - **Motion:** only the theme crossfade (`motion/base`), turned off under `prefers-reduced-motion: reduce`.
5. **Theme switch:**
   - **First paint:** `js/theme-init.js` loads synchronously in `<head>`. It sets `data-theme` from `localStorage` (try/catch) and otherwise from **light**, ignoring `prefers-color-scheme`. It is an external file because the W2 CSP allows `'self'` scripts only.
   - **Toggle:** `js/theme.js` toggles the theme, saves it in `localStorage` (try/catch) and updates the button.
   - **Button:** a `<button>` whose accessible name changes between "Ativar tema escuro" and "Ativar tema claro", with `aria-pressed` reflecting dark. The current theme's segment is filled, as in `Web/ThemeSwitch`.
6. **Assets:**
   - **Screens (`build-screens.mjs`):** crops the top 844 pt (1688 px) of `docs/qa/figma/{light,dark}/{o1,home1,chatE}.png`, exactly what the phone shows. It writes 600 px wide WebP (2× of the 300 px screen), each under 300 KB. `--check` fails when an image is stale against its gold. Rerun after any gold re-export (D10, a future art swap).
   - **Fonts (`copy-fonts.mjs`):** copies the Nunito Sans variable woff2 (latin and latin-ext subsets) and `OFL.txt` from the font package into `public/fonts/`. `@font-face` uses `font-display: swap`. No request goes to Google Fonts.
   - **Images:** alt text describes each screen in pt-BR (for example "Tela Home do Fibrai com o saldo do dia e a linha do tempo das refeições").
7. **Production guard (ADR-037 § 6):**
   - no analytics, cookies, forms, sign-up, invite, download link, store badge or APK;
   - no third-party request of any kind (fonts, scripts, images);
   - no inline `<script>`, `<style>` or `style=` attribute, because the W2 CSP allows only `'self'`.
8. **Documentation at Completion:**
   - `AGENTS.md`:
     - Live stack gains `web/` (static landing, npm scripts, ADR-037/038);
     - the Folder law gains `docs/qa/site/current/{dark,light}/`;
     - the Visual QA notes say that `land`, `landM` and `priv` are compared with browser captures, not the emulator.
   - `docs/README.md`: the matrix row for `site` points to `web/`.
   - [site README](../README.md): the live page and how to preview it.
   - [docs/qa/README.md](../../qa/README.md): the site capture folder and its gate.

## Out of scope

- Hosting, domains, security headers and deploy: [W2](w2-landing-hosting.md).
- Privacy policy text and contact data ([CP8](../../content-policy/plans/out_of_scope/cp8-public-legal-pack.md), owner).
- Copy or layout changes not in the golds (a D11 follow-up in Figma first).
- Any way to install the app, analytics, a logo or final avatar art, languages other than pt-BR.

## Validation

1. **Generated files:** `npm --prefix web run check` passes. It runs `tokens:check`, `screens:check`, `html-validate` on both pages and a grep of `web/public` that finds no `<form`, `analytics`, `gtag`, `cookie`, `.apk` or `play.google`, and no `src`/`href` pointing to another origin (SVG `xmlns` URIs are not requests).
2. **Captures:** with `npm --prefix web run preview` (localhost), `npm --prefix web run capture` takes full-page Playwright captures:
   - `land` and `priv` at 1440 px, `landM` at 390 px;
   - light and dark;
   - saved to `docs/qa/site/current/{light,dark}/{land,landM,priv}.png` at 2× (the gold geometry).
3. **Gold comparison:** a written diff list against each gold (layout, tokens, type size, radius, glass, phones, switch state, semantic colors), iterated until each pair passes the [docs/qa gate](../../qa/README.md#gate): blurred diff ≤ 2 %, content ink 0.8×–1.25×. Browser font rasterization is ignored, as for the emulator. The capture script reports the diff, reusing the pngjs blur-diff method of `tools/diff-gold.mjs`.
4. **Responsive:** at 768, 1024 and 1920 px there is no horizontal scroll and no overlap, and a reported capture goes to the plan Results (no gold).
5. **Accessibility:**
   - Lighthouse Accessibility ≥ 95 on both pages, both themes (local `npx lighthouse`);
   - no console errors;
   - visible keyboard focus on the links and the switch;
   - the switch's name and `aria-pressed` update;
   - text contrast at the D11 ratios (AA).
6. **Theme behavior:**
   - first load is light even with a dark system theme;
   - the choice survives a reload;
   - no flash of the wrong theme;
   - with `localStorage` blocked the page still renders light and the switch still works for the session.
7. `node tools/check-docs.mjs` passes after the documentation updates.

## Results

Planning only.
