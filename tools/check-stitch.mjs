#!/usr/bin/env node
import path from "path";
import fs from "fs";
import { fileURLToPath } from "url";
import { DARK_SCREENS, LIGHT_SCREENS } from "./export-stitch.mjs";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const outDark = path.join(root, "docs", "qa", "stitch", "dark");
const outLight = path.join(root, "docs", "qa", "stitch", "light");

const MIN_WIDTH = 780; // 390 dp @ 2x
let failed = false;

function checkDir(dir, screens, theme) {
  console.log(`Checking ${theme} screens in ${dir}...`);
  if (!fs.existsSync(dir)) {
    console.error(`Directory missing: ${dir}`);
    failed = true;
    return;
  }

  for (const name of Object.keys(screens)) {
    const file = path.join(dir, `${name}.png`);
    if (!fs.existsSync(file)) {
      console.error(`  ✗ Missing: ${theme}/${name}.png`);
      failed = true;
    } else {
      const stats = fs.statSync(file);
      if (stats.size === 0) {
        console.error(`  ✗ Empty file: ${theme}/${name}.png`);
        failed = true;
      } else {
        // PNG IHDR: width at byte 16, height at byte 20.
        const head = Buffer.alloc(24);
        const fd = fs.openSync(file, "r");
        fs.readSync(fd, head, 0, 24, 0);
        fs.closeSync(fd);
        const width = head.readUInt32BE(16);
        const height = head.readUInt32BE(20);
        if (width < MIN_WIDTH) {
          console.error(`  ✗ Thumbnail: ${theme}/${name}.png is ${width}x${height} (min width ${MIN_WIDTH})`);
          failed = true;
        } else {
          console.log(`  ✓ ${theme}/${name}.png ${width}x${height} (${(stats.size / 1024).toFixed(1)} KB)`);
        }
      }
    }
  }
}

checkDir(outDark, DARK_SCREENS, "dark");
checkDir(outLight, LIGHT_SCREENS, "light");

if (failed) {
  console.error("\nCheck failed. Some Stitch gold PNGs are missing or empty.");
  process.exit(1);
} else {
  console.log("\nAll 58 Stitch gold PNGs verified successfully (29 dark + 29 light)!");
  process.exit(0);
}
