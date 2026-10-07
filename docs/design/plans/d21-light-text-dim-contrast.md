# Plan — D21 Light `text/dim` contrast

- Status: Aguardando aprovação
- Date: 07/10/2026
- Owning context: `design`
- Affected code: none in `apps/` or `server/`. Figma `Design` → the `Color` collection (variable `text/dim`, mode Light: value and description) and the Contraste section of `Cores e tipografia`; no frame, component or layout change. Repository: `docs/design/tokens.json` (MCP read), `docs/tokens.md` (`node tools/gen-tokens.mjs`), `web/public/css/tokens.css` (`npm --prefix web run tokens`), the site phone screens `web/public/img/screens/light/*.webp` (`npm --prefix web run screens`), the Light golds listed below in `docs/qa/figma/light/`, a `--keep` flag in `tools/export-figma.mjs` and a new read-only `tools/contrast-gold.mjs`.
- Prerequisites: D20 `Concluído` ([history](completed/)). [A61](../../android/plans/pending_manual_validation/a61-chat-copy-scroll-capture-inline-actions.md) merged: A61 recaptures the same Light Chat golds, and the app takes the new value on its next build because `AeroTokens.kt` is generated from `docs/design/tokens.json`.
- Figma MCP budget: ≤ 15 calls (at most 120 a day, ADR-031 § 6).

Not a flow plan: one variable value in one mode, found in the D20 owner review ([D20 § Owner review](completed/d20-figma-review-inline-actions.md#owner-review)). It goes through the Figma review gate because every Light gold that shows `text/dim` changes. Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/d21-light-text-dim-contrast.md. Implemente o plano aprovado.`

## Objective

Every text run painted in `text/dim` in the golds of the [inventory](../../qa/README.md#golds) reaches WCAG AA 4.5:1 against the pixels really behind it, in both themes, with the smallest token change. The `text/dim` description stops claiming more than it measures.

## Finding

The D20 review measured the time in the user bubble (`text/dim` on `surface/tint` over the page gradient) at 3.69:1 in Light. A sweep of all 47 app golds and the 3 site golds with the method below (07/10/2026) shows the problem is wider than the bubble and Light only:

| Element (Light) | Golds | Background measured | Contrast now |
|---|---|---|---|
| `{n} Refeições` beside the timeline section label, straight on the page gradient | `home0`, `home1`, `homeX`, `homeC`, `homeK`, `homeP` (and the phone screen in `land`, `landM`) | `#88d4f4`–`#9eddf6` (app capture `#86d1f2`) | 3.42–3.78 (app 3.32) |
| Mode label `Seg–Sex · Sáb–Dom`, straight on the page gradient | `cfgS` | `#8fd8f5` (`bg/mid`) | 3.56 |
| Time in the user bubble (`surface/tint` over the upper gradient) | every Chat gold with a user message, `chatCP` selected included | `#a9d7f7`–`#b4dcf8` | 3.68–3.89 (selected 4.15) |
| Time in the Tali bubble (`surface/glass` over the upper or middle gradient) | `chat0`, `chatX`, `chatA`, `chatS`, `chatSD`, `chatM` | `#c6eafa`–`#cde9f9` | 4.36–4.45 |
| `MICRO & MACRONUTRIENTES` in the estimating card | `chatL` | `#cde9f9` | 4.45 |

Not counted as failures:

- Text under a modal scrim (`chatP`, `chatT`, `chatTI`, `homeW`, `cfgR`): content behind a modal is inactive (1.66–1.78 measured, by design).
- Non-text in `text/dim` (WCAG 1.4.11, 3:1): the empty, skipped and planned timeline node markers (3.69–4.48), the skip icon in its well (`chatSK`, 3.85 Light, 3.65 Dark), the composer icons when blocked.
- Dark: every `text/dim` text run is ≥ 4.65 (worst: the user-bubble time, 4.67–4.72; the composer icon on `surface/2`, 4.65). Dark does not change.

## Decision proposed

**Light `text/dim` `#546a7d` → `#435463`.** Same hue and saturation (HSL hue 208°, saturation about 20 %), lightness 41 % → 33 %. One value, one mode, no layout change. Dark stays `#819eb8`.

| `text/dim` Light on | `#546a7d` (now) | `#435463` (proposed) |
|---|---|---|
| Worst real background in the golds (`#88d4f4`, Home) | 3.42 | 4.75 |
| Worst in the app captures (`#86d1f2`, Home) | 3.32 | 4.62 |
| `surface/tint` over `bg/top` (user bubble at the top of the thread, composited `#a6d6f7`) | 3.64 | 5.06 |
| `surface/glass` over `bg/top` (Tali bubble at the top, composited `#c2e6fa`) | 4.26 | 5.93 |
| `bg/mid` (`#8fd8f5`) | 3.56 | 4.95 |
| `surface/2` / `bg/page` | 4.61 / 5.11 | 6.41 / 7.11 |
| `bg/top` (`#4fb6f0`), no glass | 2.49 | 3.45 |

`#435463` is the lightest value of that hue that keeps the worst measured background (the app's Home, `#86d1f2`) at ≥ 4.6, a 0.1 margin for where the gradient lands on a real screen. Text in `text/dim` must still not sit straight on the gradient above its middle stop (`bg/top` → `bg/mid`), the same rule `bg/top` already states; no gold does that today.

New description (Figma and the JSON): `Tertiary text: timestamps, placeholders, units. 4.5:1 on every surface and on the page gradient from bg/mid down; never straight on bg/top. Light is darker than text/muted: hierarchy comes from size and weight.`

Alternatives measured and rejected:

- **Bubble time (or every failing run) in `text/muted`:** `text/muted` (`#486781`) fails on the same backgrounds: 3.89–3.93 in the user bubble, 3.51–3.62 on the Home gradient, 3.77 on `cfgS`. Not enough on its own.
- **Darken `text/muted` and `text/dim` together to keep dim lighter than muted:** two variables, and `text/muted` has its own, larger problem (below) that a value change does not fix. Kept for a separate plan.

Side effect for the owner to accept: Light `text/dim` (luminance 0.084) becomes darker than `text/muted` (0.127), 1.31:1 apart. The two were already only 1.06:1 apart since D1 ([D1 § Delivered](completed/d1-figma-file-foundation.md)), whose rule stands: flow plans keep the hierarchy through size and weight, not through that colour gap. Where both sit side by side (the Home section label in `text/muted` next to `{n} Refeições` in `text/dim`) the count reads slightly darker than its label.

## Measurement method

Sample the exported gold PNGs (2x, 780 px wide; site 2880 / 780 px) for glyph pixels painted in the token, then measure against the pixels around them:

1. Ink: pixels whose largest RGB channel differs from the token's hex (`#546a7d` Light, `#819eb8` Dark) by ≤ 3. These are the glyph cores of a solid-colour text run; antialiased edges and alpha-faded (disabled) runs drop out, which is intended.
2. Runs: dilate the ink mask by an 11 × 11 px box and take connected components; keep components with ≥ 4 ink pixels (≥ 25 for the summary; the smaller ones are ` · ` separators and stray icon pixels).
3. Background: a 6 px ring around each run's bounding box, minus pixels within 40 of the ink colour; the per-channel median is the background.
4. Contrast: WCAG 2.x relative luminance of the token's hex against that median, `(L1 + 0.05) / (L2 + 0.05)`.
5. Classify each run: text, non-text (marker, icon, separator: 3:1 rule) or behind a scrim (inactive); every text run must be ≥ 4.5.
6. The same pass runs on the emulator captures in `docs/qa/android/current/{theme}/` (the app's `AeroColors.textDim` is the same hex), so the gold and the app are checked with one rule.

The sweep above was made with a scratch implementation of this method (outside the repository). The implementation of this plan adds it as `tools/contrast-gold.mjs`: read-only, `pngjs` as the other tools, never writes under `docs/qa/`.

```bash
node tools/contrast-gold.mjs --token text/dim --theme light
```

```bash
node tools/contrast-gold.mjs --token text/dim --theme light --dir docs/qa/android/current/light
```

It reads the token's hex from `docs/design/tokens.json`, prints every run (id, box, background, contrast, run class by size) sorted by contrast, and exits non-zero when a text-sized run outside the scrim list (`chatP`, `chatT`, `chatTI`, `homeW`, `cfgR`, `wipe`, `cfgT`, `o3t`) is under 4.5.

## Scope

1. **Discovery (read only):**
   - add `tools/contrast-gold.mjs` and rerun the sweep; the table of the Finding is refreshed in Results with its output before any write;
   - read the `text/dim` variable in Figma (value per mode, description, which styles or components bind it) and the Contraste section of `Cores e tipografia`.
2. **Figma** (`Design`, one write):
   - `text/dim` mode Light: `#435463`; description as above; mode Dark unchanged;
   - the Contraste section: the `text/dim` row with the new value and its worst pair; nothing else on that page;
   - read-back: the value, the description, Dark unchanged, no other variable touched.
3. **Token mirror:**
   - `docs/design/tokens.json` read again through the MCP (`readAt` 2026-10-…), only `text/dim` differs;
   - `node tools/gen-tokens.mjs` → `docs/tokens.md`;
   - `npm --prefix web run tokens` → `web/public/css/tokens.css`.
4. **Owner review** (Figma review gate):
   - review images of the changed Light frames through `node tools/export-figma.mjs --only <ids> --dry-run` (no MCP calls), with the contrast table of the dry-run export;
   - the plan goes to `pending_manual_validation/` until the owner's OK in Figma.
5. **Export** after the OK:
   - `tools/export-figma.mjs` gets `--keep`: write the listed ids as exported, without the git restore of the noise filter (it still logs the changed share). Needed because the change moves each ink pixel by 17–26 per channel, under `NOISE_DELTA` 40, so the filter would restore every file. `--keep` requires `--only`;
   - `node tools/export-figma.mjs --only <ids> --keep` with the Light ids where the sweep finds `text/dim` ink: on 07/10/2026 `o1`, `o1e`, `o3`, `o3s`, `o4`, `home0`, `home1`, `homeX`, `homeW`, `homeC`, `homeK`, `homeP`, `chat0`, `chatL`, `chatQ`, `chatE`, `chatT`, `chatP`, `chatX`, `chatCP`, `chatCC`, `chatF`, `chatG`, `chatA`, `chatR`, `chatM`, `chatS`, `chatU`, `chatD`, `chatRK`, `chatRB`, `chatRL`, `chatSK`, `chatSD`, `chatI`, `chatIC`, `chatTI`, `cfg`, `cfgS`, `push`, and the site `land`, `landM`, `priv`. The script exports both themes of an id; the Dark files are restored with `git restore docs/qa/figma/dark/` (no Dark value changed), and a Light file with no `#435463` ink after export is restored the same way;
   - `npm --prefix web run screens` (the phone screens are cut from `o1`, `home1`, `chatE`);
   - `node tools/check-figma.mjs`.

## Out of scope

- Compose and Roborazzi (client follow-up below). Dark values. Any layout, copy or component change. `text/muted` (finding below).

## Client follow-up

A client plan in [android](../../android/README.md), written after this plan's OK and run after it. The app needs no Kotlin token edit: `generateAeroTokens` writes `AeroColors.textDim` from `docs/design/tokens.json` on the next build. Its scope:

- `TokensTest`: besides `bg/page`, `textDim` ≥ 4.5 in both themes on `surface/2`, on `bg/mid`, and on `surface/tint` and `surface/glass` composited over `bg/top` (the worst bubble backgrounds).
- Roborazzi: `recordRoborazziDevDebug` for the Light baselines that render `textDim` (they fail `verifyRoborazziDevDebug` from the first build after this plan's merge until re-recorded), then `verifyRoborazziDevDebug`.
- Evidence: the change is under the gold gate's Δ 40, so `diff-gold.mjs` and `GoldTest` do not see it. Fresh Light captures of the touched flows (Home `home0`/`home1`, Chat `chatR`/`chatQ`/`chatL`, Config `cfgS`) and `tools/contrast-gold.mjs --dir docs/qa/android/current/light` with every text run ≥ 4.5 and no `#546a7d` ink left.
- Check each `textDim` use against the variable its gold binds: the sweep finds the section labels of `cfg` and the onboarding in `text/muted` in the golds, while `ConfigScreen.kt` and `OnboardingAeroScreens.kt` paint `labelSection` in `textDim`; the plan confirms by Figma read-back and follows the gold.
- A test build (`tools/distribute-dev.ps1`, notes under Ajustes).

Site: the tokens, the phone screens and the golds change in this plan; `npm --prefix web run check` and `npm --prefix web run capture` against `land`, `landM`, `priv` are part of its validation.

## Finding outside this plan: Light `text/muted`

The same sweep with `text/muted` (`#486781`) finds about 250 Light runs under 4.5 outside scrims, all on the bare page gradient or glass over its top: screen headers and the date over `bg/top` (2.66–2.80: Home, Chat, Config headers), onboarding titles' sub-copy and labels (2.77–3.58), the timeline section label on Home (3.62–4.0), Config section labels (3.51–4.46). A value change cannot fix the `bg/top` cases (4.5:1 on the measured `#52b7f0` needs a luminance ≤ 0.054, against 0.022 for `text/primary`), so it is a layout decision (glass behind the header, `text/primary` at the top, or the gradient), for its own plan if the owner wants it. Dark `text/muted` was not swept for this plan.

## Validation

1. The refreshed contrast table exists in Results before the Figma write.
2. Figma read-back: Light `text/dim` = `#435463`, the description, Dark `#819eb8`, no other variable changed.
3. `tools/contrast-gold.mjs --token text/dim` on the exported Light golds: every text run ≥ 4.5, none `#546a7d` left; Dark run unchanged (≥ 4.65).
4. `git diff --stat docs/qa/figma/`: only Light files of the list above; `docs/design/tokens.json` differs only in `text/dim` and `readAt`.
5. Manual owner OK in Figma.
6. `node tools/check-figma.mjs`, `node tools/check-docs.mjs`, `npm --prefix web run check` pass.

## Results

<Filled at Completion: refreshed contrast table, MCP calls used, owner OK date, exported files.>
