#!/usr/bin/env node
// Static checks of web/ (W1 validation 1): generated files current, HTML valid, nothing forbidden by ADR-037 § 6.
//   - tokens.css, the screen images and the fonts match their sources (each tool's --check);
//   - html-validate (recommended rules) on every page;
//   - public/ holds no form, analytics, cookie, APK or store link, no inline script or style, and no src/href to
//     another origin.
import fs from "node:fs";
import path from "node:path";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";

const web = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const PUBLIC = path.join(web, "public");
let failed = false;

function run(label, cmd, args) {
  const r = spawnSync(cmd, args, { cwd: web, encoding: "utf8" });
  const ok = r.status === 0;
  if (!ok) failed = true;
  console.log(`${ok ? "✓" : "✗"} ${label}`);
  if (!ok) console.log((r.stdout + r.stderr).trim().replace(/^/gm, "    "));
}

function walk(dir) {
  return fs.readdirSync(dir, { withFileTypes: true }).flatMap((e) => (e.isDirectory() ? walk(path.join(dir, e.name)) : [path.join(dir, e.name)]));
}

run("tokens.css matches docs/design/tokens.json", process.execPath, ["tools/build-tokens.mjs", "--check"]);
run("screens match the app golds", process.execPath, ["tools/build-screens.mjs", "--check"]);
run("fonts match @fontsource-variable/nunito-sans", process.execPath, ["tools/copy-fonts.mjs", "--check"]);
const pages = walk(PUBLIC).filter((f) => f.endsWith(".html")).map((f) => path.relative(web, f));
const htmlValidate = path.join(web, "node_modules", "html-validate", JSON.parse(fs.readFileSync(path.join(web, "node_modules", "html-validate", "package.json"), "utf8")).bin["html-validate"]);
run(`html-validate (${pages.length} pages)`, process.execPath, [htmlValidate, ...pages]);

const FORBIDDEN = [
  [/<form\b/i, "a form"],
  [/analytics|gtag|googletagmanager|plausible|umami/i, "analytics"],
  [/document\.cookie|set-cookie/i, "cookies"],
  [/\.apk\b|play\.google|apps\.apple|testflight/i, "an app download or store link"],
  [/<script(?![^>]*\bsrc=)[^>]*>/i, "an inline script"],
  [/<style\b|\sstyle=/i, "inline style"],
  [/\b(?:src|href)=["'](?:https?:)?\/\//i, "a src/href to another origin"],
  [/@import\s+url\(\s*["']?https?:|url\(\s*["']?https?:/i, "a CSS request to another origin"],
];
const problems = [];
for (const file of walk(PUBLIC).filter((f) => /\.(html|css|js)$/.test(f))) {
  const text = fs.readFileSync(file, "utf8");
  for (const [re, what] of FORBIDDEN) if (re.test(text)) problems.push(`${path.relative(web, file)}: ${what}`);
}
if (problems.length) failed = true;
console.log(`${problems.length ? "✗" : "✓"} production guard (ADR-037 § 6)`);
for (const p of problems) console.log(`    ${p}`);

if (failed) {
  console.error("\nweb check failed.");
  process.exitCode = 1;
} else {
  console.log("\nweb check passed.");
}
