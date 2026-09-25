#!/usr/bin/env node
import path from "path";
import fs from "fs";
import { pathToFileURL, fileURLToPath } from "url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const html = path.join(root, "wires", "nutri-wires-expressive.html");
const outDir = path.join(root, "docs", "qa", "wire");

const GOLD = [
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

const SCREENS = GOLD.map((file) => [file.replace(/\.png$/, ""), file]);

async function exportWires() {
  const { chromium } = await import("playwright");
  fs.mkdirSync(outDir, { recursive: true });
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1280, height: 900 } });
  await page.goto(pathToFileURL(html).href);
  for (const [id, file] of SCREENS) {
    await page.evaluate((g) => {
      go = g;
      render();
    }, id);
    await new Promise((r) => setTimeout(r, 120));
    await page.locator("#phone").screenshot({ path: path.join(outDir, file) });
    console.log("wrote", file);
  }
  await browser.close();
}

export { GOLD, SCREENS, outDir, html, exportWires };

const isMain =
  process.argv[1] &&
  path.resolve(process.argv[1]) === fileURLToPath(import.meta.url);

if (isMain) {
  exportWires().catch((err) => {
    console.error(err);
    process.exit(1);
  });
}
