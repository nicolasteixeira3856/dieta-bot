#!/usr/bin/env node
// Read-only: every gold id mapped in tools/export-figma.mjs (either theme) has both PNGs in docs/qa/figma/{dark,light}/,
// each 780 px wide (390 px frame at 2x). No network, no token.
import path from "path";
import fs from "fs";
import { DARK_FRAMES, LIGHT_FRAMES, FIGMA_DIR } from "./export-figma.mjs";

const WIDTH = 780;
const ids = [...new Set([...Object.keys(DARK_FRAMES), ...Object.keys(LIGHT_FRAMES)])];
let failed = false;

for (const id of ids) {
  for (const [theme, map] of [["dark", DARK_FRAMES], ["light", LIGHT_FRAMES]]) {
    if (!map[id]) {
      console.error(`  ✗ ${theme}/${id}: mapped in the other theme only`);
      failed = true;
      continue;
    }
    const file = path.join(FIGMA_DIR, theme, `${id}.png`);
    if (!fs.existsSync(file) || fs.statSync(file).size < 24) {
      console.error(`  ✗ Missing or empty: ${theme}/${id}.png`);
      failed = true;
      continue;
    }
    const head = fs.readFileSync(file).subarray(0, 24);
    const width = head.readUInt32BE(16);
    const height = head.readUInt32BE(20);
    if (head.readUInt32BE(0) !== 0x89504e47 || width !== WIDTH) {
      console.error(`  ✗ ${theme}/${id}.png is ${width}x${height}, expected a PNG ${WIDTH} px wide`);
      failed = true;
    } else {
      console.log(`  ✓ ${theme}/${id}.png ${width}x${height}`);
    }
  }
}

if (failed) {
  console.error("\nCheck failed. Some Figma gold PNGs are missing or have the wrong size.");
  process.exitCode = 1;
} else {
  console.log(`\nAll ${ids.length * 2} Figma gold PNGs verified (${ids.length} dark + ${ids.length} light).`);
}
