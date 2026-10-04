#!/usr/bin/env node
// Compare emulator screencaps (docs/qa/android/current/{theme}/<id>.png) with the gold of the id's source in the
// inventory of docs/qa/README.md (docs/qa/stitch/{theme}/<id>.png or docs/qa/figma/{theme}/<id>.png).
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
import { parseGoldInventory } from "./check-docs.mjs";

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
const PHONE = 1688; // 844 dp
const MIN_INK = 400; // px: below this a zone has no content to compare for presence
// Stitch golds whose layout contradicts the app: reported, not gated (none left after A44).
const GOLD_CONFLICTS = new Set();
const REGIONS = {};
// Bottom-anchored and centred-dialog regions of Stitch golds (none left after A40-A43).
const BOTTOM_REGIONS = {};
const CENTER_REGIONS = {};

// Figma golds (ADR-031) are bare frames: no status bar, the page at the frame height. The Stitch exceptions above
// are calibrated on Stitch geometry and apply to `stitch` ids only.
// homeW (A40): the 1414 dp page puts the sheet at its end and the blurred Home above it; on the 844 dp phone the
// sheet covers the lower Home. The screen is reported; the blurred Home on top (gold rows 40-450 dp) and the sheet
// (bottom-anchored, 1061 dp to 40 dp above the page end) are gated.
// o3t (A41): the frame puts Dialog/TimeWheel 300 dp down a 1268 dp page; the app centres it on the screen. The
// dialog box (border, title, wheels, actions) is gated, aligned on the screen centre.
// chatL (A42): the capture sends a suggestion chip (adb types no accents), shorter than the frame's message, so
// the screen is reported and the loading bubble with its label is gated.
// chatQ (A42): the thread is taller than the phone's room (bars take 48 dp), so the list scrolls a few dp while the
// header moves with the status bar: the screen is reported, header and thread are gated each with its own offset.
// chatS (A43): the scene seeds another day (meta 2.000, 100 %), so the screen is reported and the header and the
// routine card are gated.
// Long threads (A43: chatF, chatG, chatD, chatR, chatM, chatU): the frame shows the whole thread, the phone shows its
// newest part above the composer. The screen is reported; the header is gated from the top and the thread tail
// (everything the phone shows under the header, down to the composer) from the bottom. `tail` is the first gold row
// the tail box may take (chatF: under the photo, whose sample crop is the frame's own).
const HEADER_BOX = [0, 48, 780, 136];
const FIGMA = {
  // push (A44): the frame draws a lock screen with Notification/Push; the app only posts the notification and
  // SystemUI draws the screen and the card. Reported, never gated.
  conflicts: new Set(["homeW", "chatL", "chatQ", "chatF", "chatG", "chatD", "chatR", "chatM", "chatU", "chatS", "push"]),
  // chatP (A42): Dialog/Confirm centred over the blurred Home (the capture's Home is scrolled to the Lanche card).
  // wipe (A44): Dialog/Confirm Tone=Danger centred over the blurred Config.
  center: { o3t: { dark: [48, 600, 732, 1388], light: [48, 600, 732, 1388] }, chatP: { dark: [48, 614, 732, 1074], light: [48, 614, 732, 1074] }, wipe: { dark: [48, 424, 732, 1264], light: [48, 424, 732, 1264] } },
  regions: {
    homeW: [0, 80, 780, 900],
    chatL: [0, 500, 780, 740],
    chatQ: [HEADER_BOX, [0, 168, 780, 1296]],
    chatF: HEADER_BOX, chatG: HEADER_BOX, chatD: HEADER_BOX, chatR: HEADER_BOX, chatM: HEADER_BOX, chatU: HEADER_BOX, chatS: [HEADER_BOX, [32, 728, 748, 1256]],
  },
  tail: { chatF: 660, chatG: 0, chatD: 0, chatR: 0, chatM: 0, chatU: 0 },
  // Bottom-anchored zone of each Chat frame, px: its bottom stack read from the frame (actions + composer for chatQ /
  // chatE, chips + composer for chat0, chips + the too-long box for chatX, the attached composer for chatA, the
  // meal sheet for chatT) plus 48 dp, the status and navigation bars that the phone takes from the gap between the
  // thread and that stack.
  footer: { chat0: 364, chatL: 268, chatQ: 384, chatE: 384, chatT: 1368, chatX: 578, chatA: 436 },
  bottom: { homeW: { dark: [0, 2122, 780, 2748], light: [0, 2122, 780, 2748] } },
};
const STITCH = { conflicts: GOLD_CONFLICTS, regions: REGIONS, bottom: BOTTOM_REGIONS };

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
  // Background: the median of the box (one sampled pixel moves with the blurred page behind a glass dialog).
  const vals = [];
  for (let y = y0; y < y1; y += 8) for (let x = x0; x < x1; x += 8) {
    const ay = y + dy;
    if (ay >= 0 && ay < img.h) vals.push(img.planes.map((p) => p[ay * img.w + x]));
  }
  const bg = [0, 1, 2].map((c) => vals.map((v) => v[c]).sort((a, b) => a - b)[vals.length >> 1]);
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

const inventory = parseGoldInventory(fs.readFileSync(path.join(root, "docs/qa/README.md"), "utf8"));
if (!inventory) throw new Error("docs/qa/README.md has no gold inventory under '## Golds'");

let failed = false;
for (const key of ids) {
  const [theme, id] = key.split("/");
  const source = inventory.sources.get(id);
  if (!source) {
    console.error(`  ✗ ${key}: '${id}' is not in the gold inventory of docs/qa/README.md`);
    failed = true;
    continue;
  }
  const goldFile = path.join(root, "docs/qa", source, theme, `${id}.png`);
  const appFile = path.join(root, "docs/qa/android/current", theme, `${id}.png`);
  if (!fs.existsSync(appFile)) {
    console.error(`  ✗ ${key}: missing ${path.relative(root, appFile)}`);
    failed = true;
    continue;
  }
  const appRaw = load(appFile);
  const goldRaw = load(goldFile);
  const rules = source === "figma" ? FIGMA : STITCH;
  const stitch = source === "stitch";
  if (goldRaw.w !== WIDTH && rules.conflicts.has(id)) {
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
  const center = stitch ? CENTER_REGIONS[id]?.[theme] : FIGMA.center[id]?.[theme];
  if (center) {
    let bestDy = 0;
    // Stitch: the dialog is centred in the gold too; Figma: only in the app.
    const shift = stitch ? Math.round((appRaw.h - goldRaw.h) / 2) : Math.round((appRaw.h - (center[3] - center[1])) / 2) - center[1];
    const part = regionScore(app, gold, center, shift, (dy) => { bestDy = dy; });
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
  // A Figma frame of the phone height (844 dp) is a phone screen without bars: content and footer, no band.
  const fullPage = goldRaw.h !== 1768 && !(source === "figma" && goldRaw.h === PHONE);
  const phone = fullPage ? appRaw.h : PHONE;
  // Bottom-anchored stack of the frame (CTA, actions + composer, chips, sheet), in px.
  const footerPx = (stitch ? undefined : FIGMA.footer[id]) ?? FOOTER;
  const content = bestShift(app, gold, goldTop, IGNORE_TOP, phone - footerPx);
  let differ = content.differ;
  let total = content.total;
  let footerNote = "";
  if (!fullPage) {
    const footer = bestShift(app, gold, goldTop, phone - footerPx, phone - IGNORE_BOTTOM);
    differ += footer.differ;
    total += footer.total;
    footerNote = `, footer ${footer.dy / 2} dp`;
  }
  const pct = (100 * differ) / total;
  // Content presence: a blank or half-drawn capture has little ink and must fail even when
  // the gold itself is mostly background (splash).
  const appInk = ink(app, content.dy, IGNORE_TOP, phone - footerPx);
  const goldInk = ink(gold, goldTop, IGNORE_TOP, phone - footerPx);
  const inkRatio = appInk / Math.max(1, goldInk);
  // A zone with no ink in the gold (chatT dark: the blurred thread under the scrim) has no presence to check;
  // the capture must then be as blank.
  const blankZone = goldInk < MIN_INK && appInk < MIN_INK;
  const inkOk = blankZone || (inkRatio >= 0.8 && inkRatio <= 1.25);
  const conflict = rules.conflicts.has(id);
  const ok = pct <= max && inkOk;
  if (!ok && !conflict) failed = true;
  const bottom = rules.bottom[id]?.[theme];
  const own = rules.regions[id];
  const ownBoxes = own ? (Array.isArray(own[0]) ? own : [own]) : [];
  // Thread tail (Figma long threads): bottom-aligned on the capture's bottom minus the 24 dp navigation bar.
  const tail = stitch ? undefined : FIGMA.tail[id];
  const tailBox = tail === undefined ? null : [0, Math.max(tail, goldRaw.h - (appRaw.h - 48 - 184) + 32), 780, goldRaw.h - 40];
  const boxes = [
    ...ownBoxes.map((box, i) => [i === 0 ? "region" : `region${i}`, () => regionScore(app, gold, box)]),
    ...(tailBox ? [["thread tail", () => regionScore(app, gold, tailBox, appRaw.h - 48 - goldRaw.h)]] : []),
    ...(bottom ? [[stitch ? "region" : "bottom region", () => regionScore(app, gold, bottom, appRaw.h - goldRaw.h)]] : []),
  ];
  // Stitch: one region per id (the bottom one wins, as before); Figma: every region is gated.
  for (const [name, run] of stitch ? boxes.slice(-1) : boxes) {
    const part = run();
    const partOk = part <= max;
    const reportOnly = false;
    if (!partOk && !reportOnly) failed = true;
    console.log(`  ${partOk ? "✓" : reportOnly ? "~" : "✗"} ${key} ${name} ${part.toFixed(2)}% (max ${max}%)${reportOnly ? " [report only]" : ""}`);
  }
  console.log(`  ${ok ? "✓" : conflict ? "~" : "✗"} ${key} ${pct.toFixed(2)}% ink ${inkRatio.toFixed(2)} (content ${content.dy / 2} dp${footerNote}, max ${max}%, ink 0.8-1.25)${conflict ? " [gold conflict: report only]" : ""}`);
}
process.exit(failed ? 1 : 0);
