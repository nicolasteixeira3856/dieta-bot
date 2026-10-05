#!/usr/bin/env node
// Lighthouse Accessibility on both pages in both themes (W1 validation 5). Serves web/public locally, opens the
// Playwright Chromium (persistent profile, remote-debugging port) and saves the theme in localStorage, then runs
// Lighthouse (npx lighthouse@13.5.0, not a project dependency) against that browser with storage reset off. The
// theme Lighthouse saw is checked from the brightness of its full-page screenshot.
//
// Usage: node tools/lighthouse.mjs [--min 95]
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { spawn } from "node:child_process";
import { chromium } from "playwright";
import sharp from "sharp";
import { startServer } from "./preview.mjs";

const PORT = 4175;
const DEBUG_PORT = 9333;
const BASE = `http://127.0.0.1:${PORT}`;
const i = process.argv.indexOf("--min");
const MIN = i > 0 ? Number(process.argv[i + 1]) : 95;
const npx = process.platform === "win32" ? "npx.cmd" : "npx";

// Async on purpose: the preview server runs in this process and must keep answering while Lighthouse loads pages.
function run(cmd, args, options) {
  return new Promise((resolve) => {
    const child = spawn(cmd, args, options);
    let output = "";
    child.stdout.on("data", (d) => (output += d));
    child.stderr.on("data", (d) => (output += d));
    const timer = setTimeout(() => child.kill(), 180000);
    child.on("close", (status) => {
      clearTimeout(timer);
      resolve({ status, output });
    });
  });
}

async function brightness(dataUri) {
  const buf = Buffer.from(dataUri.split(",")[1], "base64");
  const { channels } = await sharp(buf).stats();
  return (channels[0].mean + channels[1].mean + channels[2].mean) / 3;
}

const server = await startServer(PORT);
let failed = false;
try {
  for (const theme of ["light", "dark"]) {
    const profile = fs.mkdtempSync(path.join(os.tmpdir(), `fibrai-lh-${theme}-`));
    const browser = await chromium.launchPersistentContext(profile, { headless: true, args: [`--remote-debugging-port=${DEBUG_PORT}`] });
    const page = browser.pages()[0] ?? (await browser.newPage());
    await page.goto(BASE + "/");
    await page.evaluate((t) => window.localStorage.setItem("fibrai-theme", t), theme);
    for (const url of ["/", "/privacidade/"]) {
      const out = path.join(profile, "report.json");
      const r = await run(
        npx,
        ["--yes", "lighthouse@13.5.0", BASE + url, "--only-categories=accessibility", "--disable-storage-reset", "--output=json",
          `--output-path=${out}`, "--quiet", `--port=${DEBUG_PORT}`],
        { shell: process.platform === "win32" },
      );
      if (r.status !== 0 || !fs.existsSync(out)) {
        failed = true;
        console.log(`✗ ${theme} ${url}: lighthouse failed ${r.output.slice(-600)}`);
        continue;
      }
      const report = JSON.parse(fs.readFileSync(out, "utf8"));
      const score = Math.round(report.categories.accessibility.score * 100);
      const failing = Object.values(report.audits).filter((a) => a.score === 0 && a.scoreDisplayMode === "binary").map((a) => a.id);
      const shot = report.fullPageScreenshot?.screenshot?.data ?? report.audits["final-screenshot"]?.details?.data;
      const seen = shot ? ((await brightness(shot)) < 100 ? "dark" : "light") : "unknown";
      const ok = score >= MIN && seen === theme;
      if (!ok) failed = true;
      console.log(`${ok ? "✓" : "✗"} ${theme} ${url}: accessibility ${score}, rendered ${seen}${failing.length ? ` (failing: ${failing.join(", ")})` : ""}`);
      fs.rmSync(out, { force: true });
    }
    await browser.close();
    fs.rmSync(profile, { recursive: true, force: true, maxRetries: 5, retryDelay: 200 });
  }
} finally {
  server.close();
}
if (failed) process.exitCode = 1;
