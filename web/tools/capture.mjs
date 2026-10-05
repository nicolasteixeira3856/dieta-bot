#!/usr/bin/env node
// Browser captures of the landing page against the D11 golds (W1 validation 2–6). Serves web/public on localhost,
// opens Chromium (Playwright) at the gold geometry (2x), and for each id and theme:
//   - writes docs/qa/site/current/{light,dark}/<id>.png (full page);
//   - compares it with docs/qa/figma/{light,dark}/<id>.png (tools/gold-diff.mjs) and writes the diff mask to --out;
//   - reports console errors and horizontal overflow.
// It also checks 768, 1024 and 1920 px (no gold: overflow and overlap report, captures to --out) and the theme
// behavior (light default under a dark system theme, saved choice, blocked storage).
//
// Usage: node tools/capture.mjs [--out <dir>] [--only land,landM,priv]
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { chromium } from "playwright";
import { startServer } from "./preview.mjs";
import { compare, MAX_PCT } from "./gold-diff.mjs";

const web = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const root = path.resolve(web, "..");
const arg = (name) => {
  const i = process.argv.indexOf(name);
  return i > 0 ? process.argv[i + 1] : null;
};
const OUT = path.resolve(arg("--out") ?? path.join(os.tmpdir(), "fibrai-web-capture"));
const ONLY = arg("--only")?.split(",");
const PORT = 4174;
const BASE = `http://127.0.0.1:${PORT}`;

const GOLDS = [
  { id: "land", url: "/", width: 1440 },
  { id: "landM", url: "/", width: 390 },
  { id: "priv", url: "/privacidade/", width: 1440 },
].filter((g) => !ONLY || ONLY.includes(g.id));

// The gold frames hug their content, so the gold captures use a short viewport: the page is never stretched to
// the window (min-height: 100vh) beyond the gold height.
async function openPage(browser, { width, theme, colorScheme = "light", blockStorage = false, height = 900 }) {
  const context = await browser.newContext({ viewport: { width, height }, deviceScaleFactor: 2, colorScheme });
  if (theme) await context.addInitScript((t) => window.localStorage.setItem("fibrai-theme", t), theme);
  if (blockStorage) {
    await context.addInitScript(() => {
      Object.defineProperty(window, "localStorage", { get() { throw new Error("blocked"); } });
    });
  }
  const page = await context.newPage();
  const errors = [];
  page.on("console", (m) => m.type() === "error" && errors.push(m.text()));
  page.on("pageerror", (e) => errors.push(String(e)));
  page.on("requestfailed", (r) => errors.push(`request failed: ${r.url()}`));
  page.on("response", (r) => r.status() >= 400 && errors.push(`HTTP ${r.status()}: ${r.url()}`));
  return { context, page, errors };
}

async function settle(page) {
  await page.evaluate(async () => {
    await document.fonts.ready;
    for (const img of document.images) img.loading = "eager";
    await Promise.all([...document.images].filter((i) => i.offsetParent).map((i) => (i.complete ? null : new Promise((r) => (i.onload = i.onerror = r)))));
  });
}

const server = await startServer(PORT);
const browser = await chromium.launch();
let failed = false;
fs.mkdirSync(OUT, { recursive: true });

try {
  console.log("Gold captures (docs/qa/site/current):");
  for (const gold of GOLDS) {
    for (const theme of ["light", "dark"]) {
      const { context, page, errors } = await openPage(browser, { width: gold.width, theme, height: 600 });
      await page.goto(BASE + gold.url);
      await settle(page);
      const overflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
      const dest = path.join(root, "docs", "qa", "site", "current", theme, `${gold.id}.png`);
      fs.mkdirSync(path.dirname(dest), { recursive: true });
      await page.screenshot({ path: dest, fullPage: true });
      const r = compare(dest, path.join(root, "docs", "qa", "figma", theme, `${gold.id}.png`));
      if (r.mask) fs.writeFileSync(path.join(OUT, `diff-${theme}-${gold.id}.png`), r.mask);
      const ok = r.ok && !errors.length && overflow <= 0;
      if (!ok) failed = true;
      const detail = r.reason ?? `${r.pct.toFixed(2)}% ink ${r.inkRatio.toFixed(2)} ${r.size} (gold ${r.goldSize})`;
      console.log(`  ${ok ? "✓" : "✗"} ${theme}/${gold.id} ${detail} (max ${MAX_PCT}%, ink 0.8-1.25)${overflow > 0 ? ` overflow ${overflow}px` : ""}${errors.length ? ` errors: ${errors.join(" | ")}` : ""}`);
      await context.close();
    }
  }

  if (!ONLY) {
    console.log("\nResponsive (no gold, reported):");
    for (const width of [768, 1024, 1920]) {
      for (const url of ["/", "/privacidade/"]) {
        const { context, page, errors } = await openPage(browser, { width });
        await page.goto(BASE + url);
        await settle(page);
        const overflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
        // Overlap: text boxes that intersect another text box.
        const overlaps = await page.evaluate(() => {
          const boxes = [...document.querySelectorAll("h1, h2, h3, p, li, a, span.t-body-strong, button")]
            .filter((e) => e.offsetParent && !e.closest(".bubbles") && !e.matches(".skip-link"))
            .map((e) => ({ e, r: e.getBoundingClientRect() }));
          let n = 0;
          for (let i = 0; i < boxes.length; i++) for (let j = i + 1; j < boxes.length; j++) {
            const a = boxes[i], b = boxes[j];
            if (a.e.contains(b.e) || b.e.contains(a.e)) continue;
            if (a.r.left < b.r.right - 1 && b.r.left < a.r.right - 1 && a.r.top < b.r.bottom - 1 && b.r.top < a.r.bottom - 1) n++;
          }
          return n;
        });
        const name = `${url === "/" ? "home" : "priv"}-${width}.png`;
        await page.screenshot({ path: path.join(OUT, name), fullPage: true });
        const ok = overflow <= 0 && !overlaps && !errors.length;
        if (!ok) failed = true;
        console.log(`  ${ok ? "✓" : "✗"} ${url} at ${width}px: overflow ${Math.max(0, overflow)}px, overlaps ${overlaps}${errors.length ? `, errors: ${errors.join(" | ")}` : ""}`);
        await context.close();
      }
    }

    console.log("\nTheme behavior:");
    const checks = [];
    {
      const { context, page } = await openPage(browser, { width: 1440, colorScheme: "dark" });
      await page.goto(BASE + "/");
      checks.push(["light by default under a dark system theme", (await page.getAttribute("html", "data-theme")) === "light"]);
      const button = page.locator("[data-theme-switch]");
      checks.push(["button name 'Ativar tema escuro', not pressed", (await button.getAttribute("aria-label")) === "Ativar tema escuro" && (await button.getAttribute("aria-pressed")) === "false"]);
      await button.click();
      checks.push(["click switches to dark and updates the button", (await page.getAttribute("html", "data-theme")) === "dark" && (await button.getAttribute("aria-label")) === "Ativar tema claro" && (await button.getAttribute("aria-pressed")) === "true"]);
      await page.reload();
      checks.push(["the choice survives a reload", (await page.getAttribute("html", "data-theme")) === "dark"]);
      await page.goto(BASE + "/privacidade/");
      checks.push(["the choice carries to /privacidade", (await page.getAttribute("html", "data-theme")) === "dark"]);
      await context.close();
    }
    {
      const { context, page, errors } = await openPage(browser, { width: 1440, blockStorage: true });
      await page.goto(BASE + "/");
      const light = (await page.getAttribute("html", "data-theme")) === "light";
      await page.locator("[data-theme-switch]").click();
      const toggles = (await page.getAttribute("html", "data-theme")) === "dark";
      checks.push(["blocked storage: light, switch still works, no error", light && toggles && !errors.length]);
      await context.close();
    }
    {
      const { context, page } = await openPage(browser, { width: 1440, theme: "dark" });
      // No flash: the attribute is set by the head script before the stylesheet paints anything.
      const atFirstPaint = await page.goto(BASE + "/").then(() => page.evaluate(() => document.documentElement.dataset.theme));
      checks.push(["saved dark is applied by the head script", atFirstPaint === "dark"]);
      await context.close();
    }
    {
      // Keyboard: the skip link comes first, then every nav link and the switch show a visible focus ring.
      const { context, page } = await openPage(browser, { width: 1440 });
      await page.goto(BASE + "/");
      const focused = [];
      for (let k = 0; k < 6; k++) {
        await page.keyboard.press("Tab");
        focused.push(await page.evaluate(() => {
          const e = document.activeElement;
          const s = getComputedStyle(e);
          const r = e.getBoundingClientRect();
          return { text: (e.getAttribute("aria-label") || e.textContent).trim(), ring: s.outlineStyle !== "none" && parseFloat(s.outlineWidth) >= 2, onScreen: r.top >= 0 && r.bottom <= innerHeight };
        }));
      }
      const names = focused.map((f) => f.text);
      const expected = ["Pular para o conteúdo", "Fibrai", "Como funciona", "Tali", "Contato", "Ativar tema escuro"];
      checks.push([`keyboard order ${names.join(" → ")}`, JSON.stringify(names) === JSON.stringify(expected)]);
      checks.push(["visible focus ring on each, on screen", focused.every((f) => f.ring && f.onScreen)]);
      await context.close();
    }
    for (const [name, ok] of checks) {
      if (!ok) failed = true;
      console.log(`  ${ok ? "✓" : "✗"} ${name}`);
    }
  }
} finally {
  await browser.close();
  server.close();
}

console.log(`\nDiff masks and responsive captures: ${OUT}`);
if (failed) {
  console.error("Capture check failed.");
  process.exitCode = 1;
}
