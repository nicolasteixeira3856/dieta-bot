#!/usr/bin/env node
// Read-only WCAG contrast of one colour token on the pixels really behind it, sampled from PNGs (D21).
//
// Method: ink = pixels whose largest RGB channel is within 3 of the token's hex (the glyph cores of a solid run;
// antialiased edges and alpha-faded runs drop out). The ink mask is dilated by an 11 x 11 px box and split into
// connected runs. Background = per-channel median of the dilated run box grown by 6 px, minus pixels within 40 of
// the ink. Contrast = WCAG 2.x relative luminance of the token's hex against that median.
//
// Run classes: `scrim` (an id drawn behind a modal: inactive content), `marker` (square ink box ≥ 40 px: timeline nodes),
// `separator` (≤ 13 x 13 px: ` · `), `line` (otherwise ≤ 4 px wide or tall: icon strokes such as the skip minus, A63),
// `small` (< 25 ink px: icon pieces), else `text`. Only `text` runs are gated:
// the exit code is 1 when one is under --min (default 4.5).
//
// Usage: node tools/contrast-gold.mjs --token text/dim --theme light [--dir <png folder>] [--ids a,b] [--hex #rrggbb]
//          [--as #rrggbb] [--min 4.5] [--all]
//   --dir defaults to docs/qa/figma/<theme>; ids default to the inventory of docs/qa/README.md found in --dir.
//   --hex samples another colour (e.g. the old value, to prove no ink of it is left); the token still names it.
//   --as scores a proposed colour against the backgrounds found for the sampled one (before a token change).
//   Without --all only runs under 5.0 are listed. Never writes.
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";
import { PNG } from "pngjs";
import { parseGoldInventory } from "./check-docs.mjs";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
export const SCRIM_IDS = ["chatP", "chatT", "chatTI", "homeW", "cfgR", "wipe", "cfgT", "o3t"];
const INK_TOL = 3, DILATE = 11, RING = 6, BG_TOL = 40, MIN_INK = 4, TEXT_INK = 25;

function arg(name) {
  const i = process.argv.indexOf(name);
  return i < 0 ? null : process.argv[i + 1];
}

export function hexRgb(hex) {
  const h = hex.replace("#", "");
  return [0, 2, 4].map((i) => parseInt(h.slice(i, i + 2), 16));
}

export function luminance([r, g, b]) {
  const ch = (c) => {
    c /= 255;
    return c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4;
  };
  return 0.2126 * ch(r) + 0.7152 * ch(g) + 0.0722 * ch(b);
}

export function contrast(a, b) {
  const la = luminance(a), lb = luminance(b);
  return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
}

export function tokenHex(name, theme) {
  const tokens = JSON.parse(fs.readFileSync(path.join(root, "docs/design/tokens.json"), "utf8"));
  const mode = theme === "dark" ? "Dark" : "Light";
  for (const c of tokens.collections) {
    const v = c.variables.find((x) => x.name === name);
    if (v) return v.values[mode].slice(0, 7);
  }
  throw new Error(`Unknown token: ${name}`);
}

// Box dilation of a 0/1 mask, separable (rows then columns) with running sums.
function dilate(mask, w, h, k) {
  const r = k >> 1, tmp = new Uint8Array(w * h), out = new Uint8Array(w * h);
  for (let y = 0; y < h; y++) {
    let sum = 0;
    for (let x = -r; x < w; x++) {
      if (x + r < w) sum += mask[y * w + x + r];
      if (x - r - 1 >= 0) sum -= mask[y * w + x - r - 1];
      if (x >= 0) tmp[y * w + x] = sum > 0 ? 1 : 0;
    }
  }
  for (let x = 0; x < w; x++) {
    let sum = 0;
    for (let y = -r; y < h; y++) {
      if (y + r < h) sum += tmp[(y + r) * w + x];
      if (y - r - 1 >= 0) sum -= tmp[(y - r - 1) * w + x];
      if (y >= 0) out[y * w + x] = sum > 0 ? 1 : 0;
    }
  }
  return out;
}

const median = (a) => {
  a.sort((p, q) => p - q);
  const m = a.length >> 1;
  return a.length % 2 ? a[m] : (a[m - 1] + a[m]) / 2;
};

export function runs(file, hex, score = hex) {
  const png = PNG.sync.read(fs.readFileSync(file));
  const { width: w, height: h, data } = png;
  const ink = hexRgb(hex), scored = hexRgb(score);
  const near = (i, tol) =>
    Math.max(Math.abs(data[i] - ink[0]), Math.abs(data[i + 1] - ink[1]), Math.abs(data[i + 2] - ink[2])) <= tol;
  const core = new Uint8Array(w * h);
  for (let p = 0; p < w * h; p++) core[p] = near(p * 4, INK_TOL) ? 1 : 0;
  const grown = dilate(core, w, h, DILATE);
  const seen = new Uint8Array(w * h), stack = new Int32Array(w * h), out = [];
  for (let start = 0; start < w * h; start++) {
    if (!grown[start] || seen[start]) continue;
    let top = 0, x0 = w, y0 = h, x1 = -1, y1 = -1, count = 0;
    stack[top++] = start;
    seen[start] = 1;
    while (top) {
      const p = stack[--top], x = p % w, y = (p - x) / w;
      if (core[p]) {
        count++;
        if (x < x0) x0 = x;
        if (x > x1) x1 = x;
        if (y < y0) y0 = y;
        if (y > y1) y1 = y;
      }
      for (const q of [x > 0 ? p - 1 : -1, x < w - 1 ? p + 1 : -1, y > 0 ? p - w : -1, y < h - 1 ? p + w : -1]) {
        if (q >= 0 && grown[q] && !seen[q]) {
          seen[q] = 1;
          stack[top++] = q;
        }
      }
    }
    if (count < MIN_INK) continue;
    // The ring sits around the dilated run box: ink box + DILATE / 2 + RING.
    const pad = (DILATE >> 1) + RING, ring = [[], [], []];
    for (let y = Math.max(0, y0 - pad); y <= Math.min(h - 1, y1 + pad); y++) {
      for (let x = Math.max(0, x0 - pad); x <= Math.min(w - 1, x1 + pad); x++) {
        const i = (y * w + x) * 4;
        if (near(i, BG_TOL)) continue;
        for (let c = 0; c < 3; c++) ring[c].push(data[i + c]);
      }
    }
    if (ring[0].length < 20) continue;
    const bg = ring.map((c) => Math.round(median(c)));
    out.push({ box: [x0, y0, x1 - x0 + 1, y1 - y0 + 1], ink: count, bg, cr: contrast(scored, bg) });
  }
  return out;
}

export function classify(id, run) {
  const [, , bw, bh] = run.box;
  if (SCRIM_IDS.includes(id)) return "scrim";
  if (bw >= 40 && bh >= 40 && Math.abs(bw - bh) <= 4) return "marker";
  if (bw <= 13 && bh <= 13) return "separator";
  if (bw <= 4 || bh <= 4) return "line";
  if (run.ink < TEXT_INK) return "small";
  return "text";
}

const isMain = process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url);
if (isMain) {
  const token = arg("--token") ?? "text/dim";
  const theme = arg("--theme") ?? "light";
  const hex = arg("--hex") ?? tokenHex(token, theme);
  const as = arg("--as") ?? hex;
  const min = Number(arg("--min") ?? 4.5);
  const dir = path.resolve(root, arg("--dir") ?? `docs/qa/figma/${theme}`);
  const inventory = [...parseGoldInventory(fs.readFileSync(path.join(root, "docs/qa/README.md"), "utf8")).ids];
  const ids = (arg("--ids")?.split(",").map((s) => s.trim()) ?? inventory).filter((id) => fs.existsSync(path.join(dir, `${id}.png`)));
  const rows = [];
  for (const id of ids) for (const r of runs(path.join(dir, `${id}.png`), hex, as)) rows.push({ id, cls: classify(id, r), ...r });
  rows.sort((a, b) => a.cr - b.cr);
  const hexOf = (rgb) => "#" + rgb.map((v) => v.toString(16).padStart(2, "0")).join("");
  console.log(`${token} ${theme} ${hex}${as !== hex ? " scored as " + as : ""} in ${path.relative(root, dir) || "."} (${ids.length} PNGs)\n`);
  for (const r of rows) {
    if (!process.argv.includes("--all") && r.cr >= 5) continue;
    const mark = r.cls === "text" ? (r.cr < min ? "✗" : "✓") : "·";
    console.log(`  ${mark} ${r.cr.toFixed(2).padStart(5)} ${r.id.padEnd(7)} ${r.cls.padEnd(9)} box ${r.box.join(",").padEnd(18)} ink ${String(r.ink).padStart(5)} bg ${hexOf(r.bg)}`);
  }
  const text = rows.filter((r) => r.cls === "text");
  const fails = text.filter((r) => r.cr < min);
  const worst = text[0];
  console.log(`\n${rows.length} runs in ${new Set(rows.map((r) => r.id)).size} PNGs; ${text.length} text runs; worst text ${worst ? worst.cr.toFixed(2) + " (" + worst.id + ")" : "—"}; ${fails.length} under ${min}.`);
  if (fails.length) process.exitCode = 1;
}
