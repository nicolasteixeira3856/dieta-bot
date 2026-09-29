#!/usr/bin/env node
// Regression of tools/verify-stitch.mjs against local HTML fixtures that reproduce the real ST1/ST2 failures.
// No Stitch, no key: `node --test tools/verify-stitch.test.mjs`. Serves the fixture images on 127.0.0.1:8799.
import test from "node:test";
import assert from "node:assert/strict";
import http from "http";
import path from "path";
import fs from "fs";
import os from "os";
import { spawn } from "child_process";
import { fileURLToPath } from "url";

const here = path.dirname(fileURLToPath(import.meta.url));
const PNG_1PX = Buffer.from("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=", "base64");

function run(args) {
  return new Promise((resolve) => {
    const p = spawn(process.execPath, [path.join(here, "verify-stitch.mjs"), ...args], { cwd: path.join(here, "..") });
    let out = "";
    p.stdout.on("data", (d) => (out += d));
    p.stderr.on("data", (d) => (out += d));
    p.on("close", (code) => resolve({ code, out }));
  });
}

test("fixture failures of ST1/ST2 are caught", async (t) => {
  // Expired Stitch image URLs answer 403: /expired.png reproduces it, /ok.png is a valid image.
  const server = http.createServer((req, res) => {
    if (req.url === "/ok.png") { res.writeHead(200, { "Content-Type": "image/png" }); res.end(PNG_1PX); }
    else { res.writeHead(403); res.end(); }
  });
  await new Promise((r) => server.listen(8799, "127.0.0.1", r));
  t.after(() => server.close());

  const report = path.join(fs.mkdtempSync(path.join(os.tmpdir(), "sv1-")), "stitch-report.html");
  const { code, out } = await run(["--fixture", "tools/fixtures/verify-stitch", "--report", "--out", report]);
  const line = (re) => out.split("\n").find((l) => re.test(l)) || "";

  assert.equal(code, 1, out);
  // "IA ATIVA" left on the screen.
  assert.match(line(/dark\/chatE .*text not "IA ATIVA"/), /^FALHA/);
  assert.match(line(/light\/chatE .*text not "IA ATIVA"/), /^ok/);
  // Training row too wide at 390 px.
  assert.match(line(/dark\/home1 .*fits "Treino de hoje"/), /^FALHA .*overflow [1-9]/);
  assert.match(line(/light\/home1 .*fits "Treino de hoje"/), /^ok/);
  // Image URL answering 403.
  assert.match(line(/light\/chatF .*image .*expired/), /^FALHA .*HTTP 403/);
  assert.match(line(/dark\/chatF .*image .*ok\.png/), /^ok .*HTTP 200/);
  // Dark without "PROTEÍNA", light with it.
  assert.match(line(/dark\/chatE .*coherence/), /^FALHA .*only light: .*"PROTEÍNA"/);
  assert.match(line(/home1 .*coherence/), /^ok/);

  // Report: fix prompt per theme, with the exact Stitch title and the keep-unchanged list.
  const html = fs.readFileSync(report, "utf8");
  assert.match(html, /Screen to edit: &quot;Estimate com botões de ação \(V2 Expressive\)&quot;/);
  assert.match(html, /Remove the text &quot;IA ATIVA&quot;/);
  assert.match(html, /Screen to edit: &quot;Foto de refeição e estimativa no Chat \(V2 Light\)&quot;/);
  assert.match(html, /Keep unchanged: &quot;Chat Dieta Bot&quot;/);
  assert.doesNotMatch(html, /Screen to edit: &quot;Home no dia - 1300 kcal \(V2 Light Timeline\)&quot;/);
});

test("pixel diff: noise stays under the threshold, a real change is cropped", async () => {
  const { PNG } = await import("pngjs");
  const { pngDiff, NOISE_MAX_PCT } = await import("./export-stitch.mjs");
  const { cropPng } = await import("./verify-stitch.mjs");
  const make = (paint) => {
    const p = new PNG({ width: 200, height: 200 });
    p.data.fill(255);
    paint(p);
    return PNG.sync.write(p);
  };
  const base = make(() => {});
  const noisy = make((p) => { for (let i = 0; i < p.data.length; i += 4) p.data[i] = 230; }); // Δ 25 everywhere
  const changed = make((p) => { for (let y = 50; y < 60; y++) for (let x = 20; x < 80; x++) p.data[(y * 200 + x) * 4 + 1] = 0; });

  const n = pngDiff(base, noisy);
  assert.equal(n.pct, 0);
  assert.ok(n.pct < NOISE_MAX_PCT);

  const d = pngDiff(base, changed);
  assert.equal(d.pct, 1.5); // 600 of 40 000 px
  assert.deepEqual(d.bbox, { x0: 20, y0: 50, x1: 79, y1: 59 });
  const crop = PNG.sync.read(cropPng(changed, d.bbox, 10));
  assert.deepEqual([crop.width, crop.height], [80, 30]);
});
