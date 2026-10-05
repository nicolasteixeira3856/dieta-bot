#!/usr/bin/env node
// Exports Figma gold PNGs (ADR-031 § 7): each mapped frame of the file `Design` at 2x (390 px app frame → 780 px,
// 1440 px landing frame → 2880 px) through the Figma REST image export, into docs/qa/figma/{dark,light}/<id>.png.
//
// Needs FIGMA_TOKEN (personal access token, File content: Read-only) in the environment. The token is sent
// only as the X-Figma-Token header to api.figma.com; it is never printed, logged or written.
//
// Usage: node tools/export-figma.mjs [--only home0,home1]
//        node tools/export-figma.mjs --only <id or node id> --dry-run [--out <dir>]
//   --dry-run writes to --out (default: the system temp dir), never to docs/qa/, and also accepts raw node ids
//   ("9:2") to check that the token authenticates and the frame resolves before any gold is mapped.
import path from "path";
import fs from "fs";
import os from "os";
import { execFileSync } from "child_process";
import { fileURLToPath } from "url";
import { PNG } from "pngjs";

export const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
export const FIGMA_DIR = path.join(root, "docs", "qa", "figma");

export const FIGMA_FILE_KEY = "qNiqNN3vk9GpmPL3bcV9W1";

// gold id → Figma node id of its frame on the `Release 1` page (app) or the `Landing page` page (site, D11). Each design
// plan adds its ids after the owner's review OK. A mapped id must exist in both themes.
export const DARK_FRAMES = {
  // Home (D3)
  home0: "40:430",
  home1: "40:472",
  homeX: "40:514",
  homeW: "40:557",
  // Splash and onboarding (D4)
  splash: "54:1233",
  o1: "54:1244",
  o1e: "54:1276",
  o2: "54:1308",
  o3: "54:1332",
  o3t: "54:1362",
  o3s: "54:1395",
  o4: "54:1434",
  // Chat core (D5)
  chat0: "63:2079",
  chatL: "63:2097",
  chatQ: "63:2118",
  chatE: "63:2136",
  chatT: "63:2176",
  chatP: "63:2201",
  chatX: "63:2158",
  // Chat records, photo and memory (D6)
  chatF: "72:3213",
  chatA: "72:3234",
  chatG: "72:3248",
  chatU: "72:3269",
  chatD: "72:3290",
  chatR: "72:3312",
  chatM: "72:3333",
  chatS: "72:3359",
  // Chat meal updates (D9)
  chatI: "123:4230",
  chatIC: "123:4308",
  chatTI: "123:4385",
  cfg: "78:3664",
  cfgS: "78:3695",
  wipe: "78:3723",
  push: "78:3757",
  // Landing site (D11)
  land: "107:853",
  landM: "107:1003",
  priv: "107:1086",
};

export const LIGHT_FRAMES = {
  // Home (D3)
  home0: "39:302",
  home1: "38:229",
  homeX: "39:451",
  homeW: "39:606",
  // Splash and onboarding (D4)
  splash: "51:732",
  o1: "51:743",
  o1e: "51:890",
  o2: "53:801",
  o3: "53:911",
  o3t: "53:1049",
  o3s: "53:1220",
  o4: "53:1345",
  // Chat core (D5)
  chat0: "62:1745",
  chatL: "62:1796",
  chatQ: "62:1842",
  chatE: "62:1930",
  chatT: "63:1911",
  chatP: "63:2023",
  chatX: "62:1998",
  // Chat records, photo and memory (D6)
  chatF: "72:2465",
  chatA: "72:2586",
  chatG: "72:2635",
  chatU: "72:2727",
  chatD: "72:2849",
  chatR: "72:2959",
  chatM: "72:3034",
  chatS: "72:3104",
  // Chat meal updates (D9)
  chatI: "123:3802",
  chatIC: "123:3909",
  chatTI: "123:4014",
  cfg: "78:3425",
  cfgS: "78:3520",
  wipe: "78:3596",
  push: "78:3646",
  // Landing site (D11)
  land: "106:530",
  landM: "106:716",
  priv: "106:833",
};

// PNG width at 2x for the gold ids that are not 390 px app frames (check-figma.mjs). Every other id is 780 px.
export const GOLD_WIDTHS = { land: 2880, priv: 2880 };

const NODE_ID = /^\d+:\d+$/;

function token() {
  const value = process.env.FIGMA_TOKEN;
  if (!value) throw new Error("FIGMA_TOKEN is not set. Set it in the user environment (see docs/qa/README.md) and restart the terminal.");
  return value;
}

// Never echoes a response body that could carry request details; only the status and Figma's own error text.
async function figmaImages(ids) {
  const url = `https://api.figma.com/v1/images/${FIGMA_FILE_KEY}?ids=${encodeURIComponent(ids.join(","))}&format=png&scale=2`;
  const res = await fetch(url, { headers: { "X-Figma-Token": token() } });
  if (res.status === 403) throw new Error("Figma refused the token (403). Check that FIGMA_TOKEN is valid, not expired, and has File content: Read-only.");
  if (res.status === 404) throw new Error(`Figma file ${FIGMA_FILE_KEY} not found (404).`);
  if (!res.ok) throw new Error(`Figma image export failed: HTTP ${res.status}.`);
  const body = await res.json();
  if (body.err) throw new Error(`Figma image export failed: ${body.err}`);
  return body.images || {};
}

async function download(url, dest) {
  const res = await fetch(url);
  if (!res.ok) throw new Error(`Image download failed: HTTP ${res.status}.`);
  fs.writeFileSync(dest, Buffer.from(await res.arrayBuffer()));
}

function pngSize(file) {
  const b = fs.readFileSync(file);
  return { width: b.readUInt32BE(16), height: b.readUInt32BE(20) };
}

// The committed version (HEAD) of a Figma gold, or null when the gold is new.
function gitGold(theme, id) {
  try {
    return execFileSync("git", ["show", `HEAD:docs/qa/figma/${theme}/${id}.png`], { cwd: root, maxBuffer: 64 << 20, stdio: ["ignore", "pipe", "ignore"] });
  } catch {
    return null;
  }
}

// Noise filter: a re-export rewrites every PNG with invisible byte and pixel noise. A pixel changed when its
// largest RGB channel delta is above NOISE_DELTA; a PNG whose changed pixels stay under NOISE_MAX_PCT (in %)
// of the image is restored from git, so only PNGs with a real visual change stay modified.
export const NOISE_DELTA = 40;
export const NOISE_MAX_PCT = 0.05;

export function listArg(name) {
  const i = process.argv.indexOf(name);
  if (i < 0) return null;
  return (process.argv[i + 1] || "").split(",").map((s) => s.trim()).filter(Boolean);
}

// Pixel diff of two PNG buffers: share (%) of pixels whose largest RGB channel delta is above NOISE_DELTA, and
// the bounding box of those pixels. Images of different sizes are compared by size only.
export function pngDiff(a, b) {
  const pa = PNG.sync.read(a), pb = PNG.sync.read(b);
  if (pa.width !== pb.width || pa.height !== pb.height) {
    return { sameSize: false, pct: 100, sizes: `${pa.width}x${pa.height} → ${pb.width}x${pb.height}`, bbox: null };
  }
  let changed = 0, x0 = Infinity, y0 = Infinity, x1 = -1, y1 = -1;
  for (let y = 0; y < pa.height; y++) {
    for (let x = 0; x < pa.width; x++) {
      const i = (y * pa.width + x) * 4;
      const d = Math.max(Math.abs(pa.data[i] - pb.data[i]), Math.abs(pa.data[i + 1] - pb.data[i + 1]), Math.abs(pa.data[i + 2] - pb.data[i + 2]));
      if (d > NOISE_DELTA) {
        changed++;
        x0 = Math.min(x0, x); x1 = Math.max(x1, x); y0 = Math.min(y0, y); y1 = Math.max(y1, y);
      }
    }
  }
  return { sameSize: true, pct: (100 * changed) / (pa.width * pa.height), bbox: changed ? { x0, y0, x1, y1 } : null };
}

// A PNG with < NOISE_MAX_PCT % changed pixels goes back to its git version.
function filterNoise(theme, id, dest) {
  const old = gitGold(theme, id);
  if (!old) return "new file";
  const d = pngDiff(old, fs.readFileSync(dest));
  if (!d.sameSize) return `size changed ${d.sizes}, kept`;
  const pct = `${d.pct.toFixed(3)}% px changed`;
  if (d.pct < NOISE_MAX_PCT) {
    fs.writeFileSync(dest, old);
    return `${pct}, noise → restored from git`;
  }
  return `${pct}, kept`;
}

// Jobs: [{theme, id, node}] for the requested ids. In a dry run a raw node id is accepted as its own job.
export function jobs(only, dryRun) {
  const known = new Set([...Object.keys(DARK_FRAMES), ...Object.keys(LIGHT_FRAMES)]);
  const list = [];
  for (const [theme, map] of [["dark", DARK_FRAMES], ["light", LIGHT_FRAMES]]) {
    for (const [id, node] of Object.entries(map)) if (!only || only.includes(id)) list.push({ theme, id, node });
  }
  for (const id of only || []) {
    if (known.has(id)) continue;
    if (dryRun && NODE_ID.test(id)) list.push({ theme: "node", id: id.replace(":", "-"), node: id });
    else throw new Error(`Unknown gold id: ${id}${NODE_ID.test(id) ? " (raw node ids only with --dry-run)" : ""}`);
  }
  return list;
}

export async function exportFigma({ only = null, dryRun = false, out = null } = {}) {
  const list = jobs(only, dryRun);
  if (!list.length) {
    console.log("No Figma gold mapped yet (DARK_FRAMES / LIGHT_FRAMES are empty). Nothing to export.");
    return 0;
  }
  const images = await figmaImages([...new Set(list.map((j) => j.node))]);
  for (const job of list) {
    const url = images[job.node];
    if (!url) throw new Error(`${job.theme}/${job.id}: node ${job.node} did not resolve in file ${FIGMA_FILE_KEY}.`);
    const dir = dryRun ? path.join(out || os.tmpdir(), "figma-export", job.theme) : path.join(FIGMA_DIR, job.theme);
    fs.mkdirSync(dir, { recursive: true });
    const dest = path.join(dir, `${job.id}.png`);
    await download(url, dest);
    const { width, height } = pngSize(dest);
    const noise = dryRun ? "dry run" : filterNoise(job.theme, job.id, dest);
    console.log(`  ✓ ${job.theme}/${job.id}.png (${job.node}) ${width}x${height}; ${noise}${dryRun ? " → " + dest : ""}`);
  }
  return list.length;
}

const isMain = process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url);
if (isMain) {
  const dryRun = process.argv.includes("--dry-run");
  const out = listArg("--out")?.[0] ?? null;
  exportFigma({ only: listArg("--only"), dryRun, out })
    .then((count) => {
      if (count) console.log(`\nDone! ${count} Figma frame(s) exported${dryRun ? " (dry run, nothing written to docs/qa/)" : " to docs/qa/figma/{dark,light}/"}.`);
    })
    .catch((err) => {
      console.error(err.message);
      process.exitCode = 1;
    });
}
