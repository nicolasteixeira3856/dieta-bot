import path from "path";
import fs from "fs";
import { pathToFileURL, fileURLToPath } from "url";
import { chromium } from "playwright";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const html = path.join(root, "wires", "nutri-wires-painel.html");
const outDir = path.join(root, "docs", "qa", "wire", "painel");
const outDirBase = path.join(root, "docs", "qa", "wire");

const SCREENS = [
  "splash", "o1", "o2", "o3", "o4",
  "home0", "home1", "homeX",
  "chat0", "chatL", "chatE", "chatT", "chatP", "chatF", "chatG",
  "cfg", "wipe", "push"
];

async function run() {
  fs.mkdirSync(outDir, { recursive: true });
  fs.mkdirSync(outDirBase, { recursive: true });
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1400, height: 1000 } });
  await page.goto(pathToFileURL(html).href);
  await page.waitForTimeout(500);

  for (const theme of ["dark", "light"]) {
    await page.evaluate((th) => {
      document.body.className = th;
      document.getElementById("th-dark").classList.toggle("on", th === "dark");
      document.getElementById("th-light").classList.toggle("on", th === "light");
    }, theme);

    for (const id of SCREENS) {
      await page.evaluate((screenId) => {
        render(screenId);
      }, id);
      await page.waitForTimeout(100);
      const filename = (theme === "light" ? "light-" : "") + id + ".png";
      const filePath = path.join(outDir, filename);
      await page.locator("#phone").screenshot({ path: filePath });
      // also copy to outDirBase
      fs.copyFileSync(filePath, path.join(outDirBase, filename));
      console.log("Exported:", filename);
    }
  }

  await browser.close();
  console.log("All wire screens exported successfully!");
}

run().catch((err) => {
  console.error(err);
  process.exit(1);
});
