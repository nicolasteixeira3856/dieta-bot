#!/usr/bin/env node
// Self-hosted Nunito Sans (OFL) for the landing page (ADR-037 § 4): copies the variable-weight woff2 files of the
// latin and latin-ext subsets and the OFL license from @fontsource-variable/nunito-sans into public/fonts/.
// The @font-face rules live in public/css/site.css. No request goes to Google Fonts.
//
// Usage: node tools/copy-fonts.mjs           copies the files
//        node tools/copy-fonts.mjs --check   exits 1 when a file is missing or differs from the package
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const web = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const PKG = path.join(web, "node_modules", "@fontsource-variable", "nunito-sans");
const OUT = path.join(web, "public", "fonts");
const FILES = [
  ["files/nunito-sans-latin-wght-normal.woff2", "nunito-sans-latin-wght.woff2"],
  ["files/nunito-sans-latin-ext-wght-normal.woff2", "nunito-sans-latin-ext-wght.woff2"],
  ["LICENSE", "OFL.txt"],
];

const check = process.argv.includes("--check");
let failed = false;
for (const [from, to] of FILES) {
  const src = path.join(PKG, from);
  const dest = path.join(OUT, to);
  if (!fs.existsSync(src)) throw new Error(`missing ${from} in @fontsource-variable/nunito-sans; run npm --prefix web ci`);
  if (check) {
    const same = fs.existsSync(dest) && Buffer.compare(fs.readFileSync(src), fs.readFileSync(dest)) === 0;
    console.log(`  ${same ? "✓" : "✗"} public/fonts/${to}`);
    if (!same) failed = true;
  } else {
    fs.mkdirSync(OUT, { recursive: true });
    fs.copyFileSync(src, dest);
    console.log(`  ✓ public/fonts/${to}`);
  }
}
if (failed) {
  console.error("\nFonts check failed. Run npm --prefix web run fonts.");
  process.exitCode = 1;
}
