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
// Usage: node tools/diff-gold.mjs [--max 2] dark/o1 light/o1 ...   (no ids = splash + o1..o4)
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
  : ["dark", "light"].flatMap((t) => ["splash", "o1", "o2", "o3", "o4"].map((id) => `${t}/${id}`));

const WIDTH = 780;
const IGNORE_TOP = 80;
const IGNORE_BOTTOM = 80;
const TOLERANCE = 40;
const RADIUS = 3;
const SEARCH = 48;
const FOOTER = 260; // 130 dp: CTA + gradient + nav
// Golds whose layout contradicts the canonical screen of their group (home1): reported, not
// gated, until regenerated in Stitch. See the A4 and A5 plans in docs/android/plans/completed/.
const GOLD_CONFLICTS = new Set(["home0", "homeX", "chat0", "chatL", "chatG"]);

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
  const conflict = GOLD_CONFLICTS.has(id);
  const ok = pct <= max && inkOk;
  if (!ok && !conflict) failed = true;
  console.log(`  ${ok ? "✓" : conflict ? "~" : "✗"} ${key} ${pct.toFixed(2)}% ink ${inkRatio.toFixed(2)} (content ${content.dy / 2} dp${footerNote}, max ${max}%, ink 0.8-1.25)${conflict ? " [gold conflict: report only]" : ""}`);
}
process.exit(failed ? 1 : 0);
