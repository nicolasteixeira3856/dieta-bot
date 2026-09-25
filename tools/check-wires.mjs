#!/usr/bin/env node
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";
import { GOLD, outDir } from "./export-wires.mjs";

const REQUIRED = [
  "splash.png",
  "o1.png",
  "o2.png",
  "t0.png",
  "t0d2.png",
  "t0fds.png",
  "t1.png",
  "t1load.png",
  "t2.png",
  "t2q.png",
  "t3quero.png",
  "t3tenho.png",
  "t3ideia.png",
];

const PNG = Buffer.from([0x89, 0x50, 0x4e, 0x47]);
const qaRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..", "docs", "qa");
const MIN_BYTES = 1000;

let failed = 0;
function fail(msg) {
  console.error("FAIL", msg);
  failed += 1;
}
function ok(msg) {
  console.log("OK", msg);
}

if (
  GOLD.length !== REQUIRED.length ||
  GOLD.some((name, i) => name !== REQUIRED[i])
) {
  fail(`GOLD from export-wires.mjs does not match required names: ${GOLD.join(",")}`);
} else {
  ok("export-wires GOLD matches required 13 names");
}

for (const name of REQUIRED) {
  const p = path.join(outDir, name);
  if (!fs.existsSync(p)) {
    fail(`missing ${p}`);
    continue;
  }
  const buf = fs.readFileSync(p);
  const magic = buf.subarray(0, 4);
  const hex = [...magic].map((b) => b.toString(16).padStart(2, "0")).join(" ");
  if (buf.length < MIN_BYTES) fail(`${name} too small: ${buf.length}`);
  if (!magic.equals(PNG)) fail(`${name} not PNG magic (got ${hex})`);
  else ok(`${name}\t${buf.length}\t${hex}`);
}

const loose = fs.readdirSync(qaRoot).filter((f) => {
  const full = path.join(qaRoot, f);
  return fs.statSync(full).isFile() && /\.(png|jpe?g)$/i.test(f);
});
if (loose.length) fail(`loose root images: ${loose.join(",")}`);
else ok("no loose png/jpg in docs/qa root");

if (failed) process.exit(1);
console.log("PASS", GOLD.length, "gold PNGs");
