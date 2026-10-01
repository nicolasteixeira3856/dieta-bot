#!/usr/bin/env node
// Compare emulator screencaps (docs/qa/android/current/{theme}/<id>.png) with the Stitch gold
// (docs/qa/stitch/{theme}/<id>.png).
//
// Capture on an AVD set to the gold geometry (390 dp @ 2x):
//   adb shell wm size 780x1688 && adb shell wm density 320
//
// Metric (same as StitchGoldTest): both images are box-blurred (3 passes, radius 3 px) so glyph
// rasterisation is ignored (AGENTS: ignore font raster); then the share of pixels whose max channel
// delta > 40 is reported. Top 40 dp and bottom 40 dp (clock, battery, nav, home pill) are ignored.
// The system status bar height varies per device, so the best single vertical offset in +-24 dp
// is searched before scoring. Content presence is checked too: the ink (pixels off the page
// background) of the capture must be 0.8-1.25x the gold's, so a blank capture fails.
//
// Usage: node tools/diff-gold.mjs [--max 2] dark/o1 light/o1 ...   (no ids = splash + o1, o1e, o2..o4)
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";
import { PNG } from "pngjs";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const args = process.argv.slice(2);
let max = 2;
const maxAt = args.indexOf("--max");
if (maxAt >= 0) {
  max = Number(args[maxAt + 1]);
  args.splice(maxAt, 2);
}
const ids = args.length
  ? args
  : ["dark", "light"].flatMap((t) => ["splash", "o1", "o1e", "o2", "o3", "o4"].map((id) => `${t}/${id}`));

const WIDTH = 780;
const IGNORE_TOP = 80;
const IGNORE_BOTTOM = 80;
const TOLERANCE = 40;
const RADIUS = 3;
const SEARCH = 48;
const FOOTER = 260; // 130 dp: CTA + gradient + nav
// Golds whose layout contradicts the canonical screen of their group (home1): reported, not
// gated, until regenerated in Stitch. See the A4 and A5 plans in docs/android/plans/completed/.
// push: the gold is a drawn lock screen; the app only posts a notification and SystemUI draws the
// lock screen and the card (A7). Reported, never gated.
// chatA: a copy of chat0 (same other header), only its composer is new (A19). chatX too (A25).
// homeW: a 1350 dp page with the sheet at its bottom; only the sheet is compared (A22).
// chatS: a copy of chat0 too; only the routine card is compared (A29).
const GOLD_CONFLICTS = new Set(["home0", "homeX", "chat0", "chatL", "chatG", "chatF", "chatA", "chatX", "push", "homeW", "chatS"]);
// Parts of a conflict gold that are gated on their own (gold px box x0, y0, x1, y1; best
// vertical offset). chatF: the photo bubble (A6); the rest of chatF is the chatG generation.
// chatA: the composer with the attached thumbnail (A19). chatX: the 5-line composer with the red
// border and "Texto muito longo" (A25). chatS: the routine card, title to buttons (A29).
// chatQ: the Forçar estimativa bar (A30), gated in both themes on top of the dark screen gate.
const REGIONS = { chatF: [214, 368, 746, 734], chatA: [32, 1388, 748, 1664], chatX: [32, 1344, 748, 1668], chatS: [32, 798, 748, 1302], chatQ: [32, 1436, 748, 1544] };
// Bottom-anchored regions, per theme: the gold bottom is matched to the capture bottom first.
// homeW: the "Treino de hoje" sheet, top edge to 40 dp above the page end (home pill, A22).
const BOTTOM_REGIONS = { homeW: { dark: [0, 2012, 780, 2620], light: [0, 2026, 780, 2644] } };
// ST3 is a centered dialog on a 1103 dp export. At 844 dp it moves by half the
// height difference. Gate the complete dialog (border, title, wheels, actions),
// with the unchanged 2% threshold; the underlying O3 has its own comparison.
const CENTER_REGIONS = { o3t: { dark: [48, 710, 732, 1498], light: [48, 722, 732, 1486] } };
// Regions reported, not gated: light chatA / chatX draw the composer on the page colour (chat0
// generation) while the canonical light chatE uses the card colour.
const REGION_REPORT_ONLY = new Set(["light/chatA", "light/chatX"]);
// Whole screens reported, not gated, in one theme. light/chatQ: the light gold draws the question
// bubbles tighter than dark from the same ST7 prompt (line ~22 dp vs 24.5 dp, icon gap 10 vs 12 dp,
// ~3 dp less padding); the app follows dark. Its Forçar estimativa bar stays gated (REGIONS).
const SCREEN_REPORT_ONLY = new Set(["light/chatQ"]);

function load(file) {
  const png = PNG.sync.read(fs.readFileSync(file));
  return { w: png.width, h: png.height, d: png.data };
}

/** RGB planes, box blurred. */
function blurred(img) {
  const { w, h, d } = img;
  let planes = [0, 1, 2].map((c) => {
    const p = new Float32Array(w * h);
    for (let i = 0; i < w * h; i++) p[i] = d[i * 4 + c];
    return p;
  });
  const pass = (src, horizontal) => {
    const out = new Float32Array(src.length);
    const lines = horizontal ? h : w;
    const len = horizontal ? w : h;
    const n = 2 * RADIUS + 1;
    for (let line = 0; line < lines; line++) {
      const at = (i) => {
        const c = Math.min(len - 1, Math.max(0, i));
        return horizontal ? src[line * w + c] : src[c * w + line];
      };
      let sum = 0;
      for (let i = -RADIUS; i <= RADIUS; i++) sum += at(i);
      for (let i = 0; i < len; i++) {
        out[horizontal ? line * w + i : i * w + line] = sum / n;
        sum += at(i + RADIUS + 1) - at(i - RADIUS);
      }
    }
    return out;
  };
  for (let k = 0; k < 3; k++) planes = planes.map((p) => pass(pass(p, true), false));
  return { w, h, planes };
}

/** [differ, total] over gold phone rows [y0, y1), app shifted by dy. */
function score(app, gold, goldTop, dy, y0, y1) {
  let differ = 0;
  let total = 0;
  for (let y = y0; y < y1; y++) {
    const ay = y + dy;
    const gy = y + goldTop;
    if (ay < 0 || ay >= app.h || gy >= gold.h) continue;
    for (let x = 0; x < WIDTH; x++) {
      const ai = ay * app.w + x;
      const gi = gy * gold.w + x;
      let m = 0;
      for (let c = 0; c < 3; c++) m = Math.max(m, Math.abs(app.planes[c][ai] - gold.planes[c][gi]));
      total++;
      if (m > TOLERANCE) differ++;
    }
  }
  return [differ, total];
}

/** Blurred diff of a gold box against the app, best vertical offset in ±64 dp. */
function regionScore(app, gold, [x0, y0, x1, y1], shift = 0, onBest = null) {
  let best = Infinity;
  let bestDy = shift;
  for (let dy = shift - 128; dy <= shift + 128; dy += 2) {
    let differ = 0;
    let total = 0;
    for (let y = y0; y < y1; y += 2) {
      const ay = y + dy;
      if (ay < 0 || ay >= app.h) continue;
      for (let x = x0; x < x1; x += 2) {
        const ai = ay * app.w + x;
        const gi = y * gold.w + x;
        let m = 0;
        for (let c = 0; c < 3; c++) m = Math.max(m, Math.abs(app.planes[c][ai] - gold.planes[c][gi]));
        total++;
        if (m > TOLERANCE) differ++;
      }
    }
    if (total > 0 && (100 * differ) / total < best) {
      best = (100 * differ) / total;
      bestDy = dy;
    }
  }
  onBest?.(bestDy);
  return best;
}

/** Dialog content presence, using its surface at the left midpoint as background. */
function dialogInk(img, [x0, y0, x1, y1], dy = 0) {
  const sample = (Math.round((y0 + y1) / 2) + dy) * img.w + x0 + 8;
  const bg = img.planes.map((p) => p[sample]);
  let count = 0;
  for (let y = y0 + 16; y < y1 - 16; y++) for (let x = x0 + 16; x < x1 - 16; x++) {
    const ay = y + dy;
    if (ay < 0 || ay >= img.h) continue;
    const at = ay * img.w + x;
    if (img.planes.some((p, c) => Math.abs(p[at] - bg[c]) > TOLERANCE)) count++;
  }
  return count;
}

/** Pixels that stand out from the page background (median of the compared rows). */
function ink(img, top, y0, y1) {
  const vals = [];
  for (let y = y0; y < y1; y += 8) for (let x = 0; x < WIDTH; x += 8) {
    const i = (y + top) * img.w + x;
    vals.push([img.planes[0][i], img.planes[1][i], img.planes[2][i]]);
  }
  const bg = [0, 1, 2].map((c) => vals.map((v) => v[c]).sort((a, b) => a - b)[vals.length >> 1]);
  let n = 0;
  for (let y = y0; y < y1; y++) for (let x = 0; x < WIDTH; x++) {
    const i = (y + top) * img.w + x;
    let m = 0;
    for (let c = 0; c < 3; c++) m = Math.max(m, Math.abs(img.planes[c][i] - bg[c]));
    if (m > TOLERANCE) n++;
  }
  return n;
}

function bestShift(app, gold, goldTop, y0, y1) {
  let best = { dy: 0, differ: Infinity, total: 1 };
  for (let dy = -SEARCH; dy <= SEARCH; dy += 2) {
    const [differ, total] = score(app, gold, goldTop, dy, y0, y1);
    if (total > 0 && differ / total < best.differ / best.total) best = { dy, differ, total };
  }
  return best;
}

let failed = false;
for (const key of ids) {
  const [theme, id] = key.split("/");
  const goldFile = path.join(root, "docs/qa/stitch", theme, `${id}.png`);
  const appFile = path.join(root, "docs/qa/android/current", theme, `${id}.png`);
  if (!fs.existsSync(appFile)) {
    console.error(`  ✗ ${key}: missing ${path.relative(root, appFile)}`);
    failed = true;
    continue;
  }
  const appRaw = load(appFile);
  const goldRaw = load(goldFile);
  if (goldRaw.w !== WIDTH && GOLD_CONFLICTS.has(id)) {
    console.log(`  ~ ${key} gold is ${goldRaw.w}x${goldRaw.h}, not a phone capture [gold conflict: report only]`);
    continue;
  }
  if (appRaw.w !== WIDTH || goldRaw.w !== WIDTH) {
    console.error(`  ✗ ${key}: width must be ${WIDTH} (app ${appRaw.w}, gold ${goldRaw.w}). Use wm size 780x1688 + density 320.`);
    failed = true;
    continue;
  }
  const app = blurred(appRaw);
  const gold = blurred(goldRaw);
  const center = CENTER_REGIONS[id]?.[theme];
  if (center) {
    let bestDy = 0;
    const part = regionScore(app, gold, center, Math.round((appRaw.h - goldRaw.h) / 2), (dy) => { bestDy = dy; });
    const inkRatio = dialogInk(app, center, bestDy) / Math.max(1, dialogInk(gold, center));
    const partOk = part <= max && inkRatio >= 0.8 && inkRatio <= 1.25;
    if (!partOk) failed = true;
    console.log(`  ${partOk ? "✓" : "✗"} ${key} centered dialog ${part.toFixed(2)}% ink ${inkRatio.toFixed(2)} (max ${max}%, ink 0.8-1.25)`);
    continue;
  }
  // 844 dp phone inside a 884 dp page: 20 dp bands. Full-page golds (O3) have no band.
  const goldTop = goldRaw.h === 1768 ? 40 : 0;
  // Content is top-anchored (status bar height varies), the CTA footer is bottom-anchored
  // (nav bar height varies): each region gets its own vertical offset.
  // Full-page golds scroll past the fixed CTA, so there only the content above the footer counts.
  const fullPage = goldRaw.h !== 1768;
  const phone = fullPage ? appRaw.h : 1688;
  const content = bestShift(app, gold, goldTop, IGNORE_TOP, phone - FOOTER);
  let differ = content.differ;
  let total = content.total;
  let footerNote = "";
  if (!fullPage) {
    const footer = bestShift(app, gold, goldTop, phone - FOOTER, phone - IGNORE_BOTTOM);
    differ += footer.differ;
    total += footer.total;
    footerNote = `, footer ${footer.dy / 2} dp`;
  }
  const pct = (100 * differ) / total;
  // Content presence: a blank or half-drawn capture has little ink and must fail even when
  // the gold itself is mostly background (splash).
  const appInk = ink(app, content.dy, IGNORE_TOP, phone - FOOTER);
  const goldInk = ink(gold, goldTop, IGNORE_TOP, phone - FOOTER);
  const inkRatio = appInk / Math.max(1, goldInk);
  const inkOk = inkRatio >= 0.8 && inkRatio <= 1.25;
  const conflict = GOLD_CONFLICTS.has(id) || SCREEN_REPORT_ONLY.has(key);
  const ok = pct <= max && inkOk;
  if (!ok && !conflict) failed = true;
  const bottom = BOTTOM_REGIONS[id]?.[theme];
  if (REGIONS[id] || bottom) {
    const part = bottom ? regionScore(app, gold, bottom, appRaw.h - goldRaw.h) : regionScore(app, gold, REGIONS[id]);
    const partOk = part <= max;
    const reportOnly = REGION_REPORT_ONLY.has(key);
    if (!partOk && !reportOnly) failed = true;
    console.log(`  ${partOk ? "✓" : reportOnly ? "~" : "✗"} ${key} region ${part.toFixed(2)}% (max ${max}%)${reportOnly ? " [report only]" : ""}`);
  }
  console.log(`  ${ok ? "✓" : conflict ? "~" : "✗"} ${key} ${pct.toFixed(2)}% ink ${inkRatio.toFixed(2)} (content ${content.dy / 2} dp${footerNote}, max ${max}%, ink 0.8-1.25)${conflict ? " [gold conflict: report only]" : ""}`);
}
process.exit(failed ? 1 : 0);
