// Blurred gold diff for the site captures, the method of tools/diff-gold.mjs and docs/qa/README.md § Gate:
// both images box-blurred (3 passes, radius 3 px) so glyph rasterization does not count; a pixel differs when its
// largest channel delta is over 40; pass at ≤ 2 % with content ink between 0.8× and 1.25× of the gold.
import fs from "node:fs";
import { PNG } from "pngjs";

export const TOLERANCE = 40;
export const RADIUS = 3;
export const MAX_PCT = 2;

export function load(file) {
  const png = PNG.sync.read(fs.readFileSync(file));
  return { w: png.width, h: png.height, d: png.data };
}

function blurred({ w, h, d }) {
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

// Ink: pixels away from the local page color. The page is a vertical gradient, so the reference is the median of
// each 64 px band rather than one global color.
function ink(img, rows) {
  let n = 0;
  const band = 64;
  for (let y0 = 0; y0 < rows; y0 += band) {
    const y1 = Math.min(rows, y0 + band);
    const bg = [0, 1, 2].map((c) => {
      const v = [];
      for (let y = y0; y < y1; y += 4) for (let x = 0; x < img.w; x += 8) v.push(img.planes[c][y * img.w + x]);
      v.sort((a, b) => a - b);
      return v[v.length >> 1];
    });
    for (let y = y0; y < y1; y++) for (let x = 0; x < img.w; x++) {
      const i = y * img.w + x;
      let m = 0;
      for (let c = 0; c < 3; c++) m = Math.max(m, Math.abs(img.planes[c][i] - bg[c]));
      if (m > TOLERANCE) n++;
    }
  }
  return n;
}

/** Compares a capture with its gold. Returns pct, ink ratio, sizes and a diff mask PNG buffer. */
export function compare(captureFile, goldFile) {
  const appRaw = load(captureFile);
  const goldRaw = load(goldFile);
  if (appRaw.w !== goldRaw.w) return { ok: false, reason: `width ${appRaw.w} vs gold ${goldRaw.w}` };
  const app = blurred(appRaw);
  const gold = blurred(goldRaw);
  const rows = Math.min(app.h, gold.h);
  const mask = new PNG({ width: gold.w, height: rows });
  let differ = 0;
  for (let y = 0; y < rows; y++) for (let x = 0; x < gold.w; x++) {
    const i = y * gold.w + x;
    let m = 0;
    for (let c = 0; c < 3; c++) m = Math.max(m, Math.abs(app.planes[c][i] - gold.planes[c][i]));
    const bad = m > TOLERANCE;
    if (bad) differ++;
    const o = i * 4;
    mask.data[o] = bad ? 255 : goldRaw.d[o] >> 2;
    mask.data[o + 1] = bad ? 0 : goldRaw.d[o + 1] >> 2;
    mask.data[o + 2] = bad ? 0 : goldRaw.d[o + 2] >> 2;
    mask.data[o + 3] = 255;
  }
  const pct = (differ / (rows * gold.w)) * 100;
  const inkRatio = ink(app, rows) / Math.max(1, ink(gold, rows));
  const sameHeight = app.h === gold.h;
  const ok = sameHeight && pct <= MAX_PCT && inkRatio >= 0.8 && inkRatio <= 1.25;
  return { ok, pct, inkRatio, size: `${appRaw.w}x${appRaw.h}`, goldSize: `${goldRaw.w}x${goldRaw.h}`, sameHeight, mask: PNG.sync.write(mask) };
}
