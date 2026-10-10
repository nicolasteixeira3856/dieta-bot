#!/usr/bin/env node
// Phone screens of the landing page, cut from the app golds (W1 § 6): the top 844 pt (1688 px at 2x) of
// docs/qa/figma/{light,dark}/{ob3,home1,chatE}.png, which is what the D11 phone frame shows, written as 600 px wide
// WebP (2x of the 300 px screen) into public/img/screens/{light,dark}/<id>.webp. Rerun after a gold re-export.
//
// Usage: node tools/build-screens.mjs           writes the six images
//        node tools/build-screens.mjs --check   exits 1 when an image is missing, stale or over 300 KB
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import sharp from "sharp";

const web = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const GOLDS = path.resolve(web, "..", "docs", "qa", "figma");
const OUT = path.join(web, "public", "img", "screens");
export const SCREENS = ["ob3", "home1", "chatE"];
const GOLD_WIDTH = 780;
const SCREEN_HEIGHT = 1688; // 844 pt at 2x
const WIDTH = 600;
const MAX_BYTES = 300 * 1024;

async function render(theme, id) {
  const file = path.join(GOLDS, theme, `${id}.png`);
  const meta = await sharp(file).metadata();
  if (meta.width !== GOLD_WIDTH) throw new Error(`${theme}/${id}.png is ${meta.width} px wide, expected ${GOLD_WIDTH}`);
  const height = Math.min(SCREEN_HEIGHT, meta.height);
  return sharp(file)
    .extract({ left: 0, top: 0, width: GOLD_WIDTH, height })
    .resize({ width: WIDTH })
    .webp({ quality: 82, effort: 6 })
    .toBuffer();
}

const check = process.argv.includes("--check");
let failed = false;
for (const theme of ["light", "dark"]) {
  for (const id of SCREENS) {
    const buf = await render(theme, id);
    const dest = path.join(OUT, theme, `${id}.webp`);
    const rel = path.relative(web, dest).split(path.sep).join("/");
    if (buf.length > MAX_BYTES) {
      console.error(`  ✗ ${rel} is ${(buf.length / 1024).toFixed(0)} KB, over 300 KB`);
      failed = true;
    }
    if (check) {
      const same = fs.existsSync(dest) && Buffer.compare(fs.readFileSync(dest), buf) === 0;
      if (!same) {
        console.error(`  ✗ ${rel} is missing or stale against docs/qa/figma/${theme}/${id}.png`);
        failed = true;
      } else console.log(`  ✓ ${rel} ${(buf.length / 1024).toFixed(0)} KB`);
    } else {
      fs.mkdirSync(path.dirname(dest), { recursive: true });
      fs.writeFileSync(dest, buf);
      console.log(`  ✓ ${rel} ${(buf.length / 1024).toFixed(0)} KB`);
    }
  }
}
if (failed) {
  console.error(check ? "\nScreens check failed. Run npm --prefix web run screens." : "\nSome screens are over budget.");
  process.exitCode = 1;
}
