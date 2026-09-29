#!/usr/bin/env node
// Verifies a Stitch gate (docs/stitch/plans/<gate>*.checks.json) in one run: resolves the gate screens, renders
// their HTML with the exporter's render (loadFrame), runs the checks and, with --report, writes one HTML report
// with the old and new gold side by side, the check results and a fix prompt per failing screen and theme.
//
//   node tools/verify-stitch.mjs st2                 checks only
//   node tools/verify-stitch.mjs st2 --report        + report (default: <os tmpdir>/stitch-report.html)
//   node tools/verify-stitch.mjs st2 --report --out <file.html>
//   node tools/verify-stitch.mjs --fixture tools/fixtures/verify-stitch [--report]   local HTML, no Stitch
//
// Exit code 1 when any check fails. Rules: docs/stitch/README.md § Verificação automática.
import path from "path";
import fs from "fs";
import os from "os";
import { fileURLToPath } from "url";
import { PNG } from "pngjs";
import {
  DARK_SCREENS, LIGHT_SCREENS, root, loadFrame, closeBrowser, listScreens, screenCssHeight, exportOne, gitGold, pngDiff
} from "./export-stitch.mjs";

const THEMES = ["dark", "light"];
const IDS = { dark: DARK_SCREENS, light: LIGHT_SCREENS };
const PLANS = path.join(root, "docs", "stitch", "plans");
const FIXTURE_HEIGHT = 884;

// Gold id → exact Stitch titles, read from the tables of docs/stitch/README.md § Nomes das telas.
export function readTitles() {
  const md = fs.readFileSync(path.join(root, "docs", "stitch", "README.md"), "utf8");
  const titles = {};
  for (const m of md.matchAll(/^\| `(\w+)` \| ([^|]+?) \| ([^|]+?) \|/gm)) {
    titles[m[1]] ??= { dark: m[2].trim(), light: m[3].trim() };
  }
  return titles;
}

function findChecksFile(gate) {
  for (const dir of [PLANS, path.join(PLANS, "completed")]) {
    if (!fs.existsSync(dir)) continue;
    const hit = fs.readdirSync(dir).find((f) => f.endsWith(".checks.json") && (f === `${gate}.checks.json` || f.startsWith(`${gate}-`)));
    if (hit) return path.join(dir, hit);
  }
  throw new Error(`No checks file for gate "${gate}" in docs/stitch/plans/ or plans/completed/`);
}

// ---- in-page measurements (run inside the rendered screen) -------------------------------------------------

// Installed once per page: text lookup by visible text (innerText, so CSS uppercase counts) and text geometry.
const PAGE_HELPERS = () => {
  const norm = (s) => (s || "").replace(/\s+/g, " ").trim();
  window.__vs = {
    norm,
    // Deepest visible element whose visible text contains `t`.
    find(t) {
      let best = null;
      const walk = (el) => {
        for (const c of el.children) {
          if (norm(c.innerText).includes(t)) {
            const r = c.getBoundingClientRect();
            if (r.width > 0 && r.height > 0) { best = c; walk(c); return; }
          }
        }
      };
      if (norm(document.body.innerText).includes(t)) { best = document.body; walk(document.body); }
      return best;
    },
    // Box of the element's text content (not of the full-width block around it).
    textRect(el) {
      const range = document.createRange();
      range.selectNodeContents(el);
      const r = range.getBoundingClientRect();
      return r.width || r.height ? r : el.getBoundingClientRect();
    },
    box: (r) => ({ left: r.left, top: r.top, right: r.right, bottom: r.bottom, width: r.width, height: r.height })
  };
};

async function measure(page, frame, check) {
  return page.evaluate(([check, frameEl]) => {
    const { norm, find, textRect, box } = window.__vs;
    const fr = frameEl.getBoundingClientRect();
    const missing = (t) => ({ missing: t });
    switch (check.type) {
      case "text": {
        const text = norm(document.body.innerText) + " " +
          [...document.querySelectorAll("input,textarea")].map((e) => `${e.value} ${e.placeholder}`).join(" ");
        return {
          present: (check.has || []).filter((t) => text.includes(t)),
          absent: (check.not || []).filter((t) => !text.includes(t))
        };
      }
      case "fits": {
        const a = find(check.text);
        if (!a) return missing(check.text);
        let row = a;
        if (check.with) {
          const b = find(check.with);
          if (!b) return missing(check.with);
          row = a;
          while (row && !row.contains(b)) row = row.parentElement;
        }
        const r = row.getBoundingClientRect();
        let overflow = Math.max(0, row.scrollWidth - row.clientWidth, r.right - fr.right, fr.left - r.left);
        for (const d of row.querySelectorAll("*")) {
          const dr = d.getBoundingClientRect();
          if (!dr.width || !dr.height) continue;
          overflow = Math.max(overflow, dr.right - r.right, r.left - dr.left);
        }
        return { height: r.height, overflow };
      }
      case "gap": {
        const ea = find(check.a), eb = find(check.b);
        if (!ea) return missing(check.a);
        if (!eb) return missing(check.b);
        const a = textRect(ea), b = textRect(eb);
        const dx = Math.max(0, b.left - a.right, a.left - b.right);
        const dy = Math.max(0, b.top - a.bottom, a.top - b.bottom);
        return { gap: dx && dy ? Math.hypot(dx, dy) : Math.max(dx, dy) };
      }
      case "visibleAbove": {
        const et = find(check.text), ef = find(check.fixed);
        if (!et) return missing(check.text);
        if (!ef) return missing(check.fixed);
        let f = ef;
        while (f && !["fixed", "sticky"].includes(getComputedStyle(f).position)) f = f.parentElement;
        const t = textRect(et), fx = (f || ef).getBoundingClientRect();
        const overlap = t.right > fx.left && t.left < fx.right && t.bottom > fx.top && t.top < fx.bottom;
        return { text: box(t), fixed: box(fx), overlap, offscreen: t.bottom > innerHeight + 0.5 };
      }
      default:
        return { error: `unknown check type "${check.type}"` };
    }
  }, [check, frame]);
}

async function pageFacts(page, frame) {
  return page.evaluate((frameEl) => {
    // Visible texts, one per text node (chips on the same line stay apart), as displayed (CSS text-transform).
    const lines = new Set();
    const walker = document.createTreeWalker(frameEl, NodeFilter.SHOW_TEXT);
    for (let n = walker.nextNode(); n; n = walker.nextNode()) {
      const el = n.parentElement;
      let t = window.__vs.norm(n.textContent);
      if (!t || !el.checkVisibility({ visibilityProperty: true }) || ["SCRIPT", "STYLE"].includes(el.tagName)) continue;
      const tt = getComputedStyle(el).textTransform;
      if (tt === "uppercase") t = t.toUpperCase();
      else if (tt === "lowercase") t = t.toLowerCase();
      lines.add(t);
    }
    for (const e of frameEl.querySelectorAll("input,textarea")) {
      for (const v of [e.value, e.placeholder]) if (window.__vs.norm(v)) lines.add(window.__vs.norm(v));
    }
    const images = [...document.images].map((i) => i.currentSrc || i.src).filter((s) => /^https?:/.test(s));
    return { texts: [...lines], images };
  }, frame);
}

// ---- check evaluation ----------------------------------------------------------------------------------------

const px = (n) => `${Math.round(n * 10) / 10} px`;

// One result per check: { ok, label, measured, fix } (fix = English sentence for the fix prompt).
function evaluate(check, m) {
  if (m.error) return [{ ok: false, label: check.type, measured: m.error, fix: null }];
  if (m.missing) {
    return [{ ok: false, label: `${check.type} "${check.text || check.a}"`, measured: `text "${m.missing}" not found`,
      fix: check.fix || `Show the text "${m.missing}" on this screen, as described in the gate.` }];
  }
  switch (check.type) {
    case "text":
      return [
        ...(check.has || []).map((t) => ({ ok: m.present.includes(t), label: `text has "${t}"`,
          measured: m.present.includes(t) ? "present" : "absent", fix: check.fix || `Show the text "${t}" exactly as written.` })),
        ...(check.not || []).map((t) => ({ ok: m.absent.includes(t), label: `text not "${t}"`,
          measured: m.absent.includes(t) ? "absent" : "present", fix: check.fix || `Remove the text "${t}".` }))
      ];
    case "fits": {
      const tall = check.maxHeight != null && m.height > check.maxHeight + 0.5;
      const ok = m.overflow <= 1 && !tall;
      return [{ ok, label: `fits "${check.text}"${check.maxHeight ? ` ≤ ${check.maxHeight} px` : ""}`,
        measured: `height ${px(m.height)}, overflow ${px(m.overflow)}`,
        fix: check.fix || `The row with "${check.text}"${check.with ? ` and "${check.with}"` : ""} must fit in one line inside the 390 px screen, with no element past its edge${check.maxHeight ? `, at most ${check.maxHeight}px tall` : ""}.` }];
    }
    case "gap":
      return [{ ok: m.gap >= check.min - 0.5, label: `gap "${check.a}" ↔ "${check.b}" ≥ ${check.min} px`, measured: px(m.gap),
        fix: check.fix || `Keep at least ${check.min}px of space between "${check.a}" and "${check.b}".` }];
    case "visibleAbove":
      return [{ ok: !m.overlap && !m.offscreen, label: `visibleAbove "${check.text}" / "${check.fixed}"`,
        measured: `text bottom ${px(m.text.bottom)}, fixed top ${px(m.fixed.top)}${m.overlap ? ", covered" : ""}${m.offscreen ? ", off screen" : ""}`,
        fix: check.fix || `The text "${check.text}" must stay fully visible above the fixed "${check.fixed}" button: add bottom space to the content.` }];
    default:
      return [];
  }
}

async function checkImages(urls, cache) {
  const out = [];
  for (const url of urls) {
    if (!cache.has(url)) {
      cache.set(url, await fetch(url).then((r) => r.status).catch((e) => `error ${e.code || e.message}`));
    }
    const status = cache.get(url);
    out.push({ ok: status === 200, label: `image ${url.length > 70 ? url.slice(0, 67) + "..." : url}`, measured: `HTTP ${status}`,
      fix: "One image on this screen is broken (the URL no longer loads). Replace it with a new image of the same content, same size and position." });
  }
  return out;
}

// ---- gate resolution -------------------------------------------------------------------------------------------

function resolveScreen(gold, theme, title, all) {
  const known = IDS[theme][gold];
  if (known) {
    const s = all.find((x) => x.name.split("/").pop() === known);
    if (!s) return { error: `ID ${known} not found in the project` };
    if (s.title !== title) return { screen: s, error: `title is "${s.title}", expected "${title}"` };
    return { screen: s };
  }
  const hits = all.filter((x) => x.title === title);
  if (!hits.length) return { error: `no screen titled "${title}"` };
  if (hits.length > 1) return { error: `${hits.length} screens titled "${title}": ${hits.map((h) => h.name.split("/").pop()).join(", ")}` };
  return { screen: hits[0] };
}

function argValue(name) {
  const i = process.argv.indexOf(name);
  return i < 0 ? null : process.argv[i + 1];
}

export async function verify({ gate = null, fixture = null, report = false, out = null } = {}) {
  const spec = JSON.parse(fs.readFileSync(fixture ? path.join(fixture, "checks.json") : findChecksFile(gate), "utf8"));
  const titles = readTitles();
  const all = fixture ? [] : await listScreens();
  const results = []; // { gold, theme, title, id, lines: [{ok, warn, label, measured, fix}], texts }
  const imageCache = new Map();

  for (const entry of spec.screens) {
    for (const theme of entry.themes || THEMES) {
      const title = titles[entry.gold]?.[theme] || `${entry.gold} (${theme})`;
      const res = { gold: entry.gold, theme, title, id: null, lines: [], texts: null, screen: null };
      results.push(res);
      if (!titles[entry.gold]) res.lines.push({ ok: false, label: "title", measured: `gold "${entry.gold}" missing from docs/stitch/README.md § Nomes das telas`, fix: null });
      let html, cssHeight;
      if (fixture) {
        const file = path.join(fixture, theme, `${entry.gold}.html`);
        if (!fs.existsSync(file)) { res.lines.push({ ok: false, label: "fixture", measured: `missing ${file}`, fix: null }); continue; }
        html = fs.readFileSync(file, "utf8");
        cssHeight = spec.fixtureHeight || FIXTURE_HEIGHT;
      } else {
        const r = resolveScreen(entry.gold, theme, title, all);
        res.screen = r.screen || null;
        res.id = r.screen?.name.split("/").pop() || null;
        res.lines.push({ ok: !r.error, label: "screen", measured: r.error || `${res.id} "${title}"`,
          fix: r.error ? `Rename the screen to the exact title "${title}".` : null });
        if (!r.screen?.htmlCode?.downloadUrl) continue;
        html = await (await fetch(r.screen.htmlCode.downloadUrl)).text();
        cssHeight = screenCssHeight(r.screen);
      }
      let loaded;
      try {
        loaded = await loadFrame(html, cssHeight);
      } catch (err) {
        res.lines.push({ ok: false, label: "render", measured: err.message, fix: "The screen must be a single 390px-wide phone frame." });
        continue;
      }
      const { page, frame } = loaded;
      try {
        await page.evaluate(PAGE_HELPERS);
        for (const check of entry.checks || []) {
          if (check.themes && !check.themes.includes(theme)) continue;
          res.lines.push(...evaluate(check, await measure(page, frame, check)));
        }
        const facts = await pageFacts(page, frame);
        res.texts = facts.texts;
        res.lines.push(...(await checkImages(facts.images, imageCache)));
      } finally {
        await page.close();
      }
    }
  }

  // Dark × light coherence: same visible texts on both themes of a gold. `exceptions`: texts that differ on
  // purpose (ignored). `known`: divergences older than the gate, reported as a warning with their note.
  const exceptions = new Set(spec.coherence?.exceptions || []);
  const known = spec.coherence?.known || [];
  const q = (a) => a.map((t) => `"${t}"`).join(", ") || "—";
  for (const gold of new Set(results.map((r) => r.gold))) {
    const [d, l] = THEMES.map((t) => results.find((r) => r.gold === gold && r.theme === t));
    if (!d?.texts || !l?.texts) continue;
    const isKnown = (t) => known.find((k) => k.gold === gold && k.text === t);
    const diff = (a, b) => a.texts.filter((t) => !b.texts.includes(t) && !exceptions.has(t));
    const warned = [...diff(d, l), ...diff(l, d)].filter(isKnown);
    const onlyDark = diff(d, l).filter((t) => !isKnown(t));
    const onlyLight = diff(l, d).filter((t) => !isKnown(t));
    const ok = !onlyDark.length && !onlyLight.length;
    for (const res of [d, l]) {
      for (const t of warned) res.lines.push({ ok: true, warn: true, label: "coherence known", measured: `"${t}": ${isKnown(t).note}` });
    }
    for (const [res, mine, other, otherTitle] of [[d, onlyDark, onlyLight, l.title], [l, onlyLight, onlyDark, d.title]]) {
      res.lines.push({ ok, label: "coherence dark × light", measured: ok ? "same texts" : `only dark: ${q(onlyDark)} | only light: ${q(onlyLight)}`,
        fix: `This screen must show the same texts as "${otherTitle}". Texts only on this screen: ${q(mine)}. Texts missing here: ${q(other)}. Use the texts from the gate prompt; change nothing else.` });
    }
  }

  // Duplicate titles in list_screens: a warning only (the API also lists hidden versions).
  if (!fixture) {
    for (const r of results) {
      const n = all.filter((s) => s.title === r.title).length;
      if (n > 1) r.lines.push({ ok: true, warn: true, label: "uniqueTitles", measured: `${n} screens with this title in list_screens (hidden versions?)` });
    }
  }

  let failed = 0;
  for (const r of results) {
    for (const l of r.lines) {
      if (!l.ok) failed++;
      const tag = l.ok ? (l.warn ? "aviso" : "ok") : "FALHA";
      console.log(`${tag.padEnd(5)}  ${`${r.theme}/${r.gold}`.padEnd(12)}  ${l.label}: ${l.measured}`);
    }
  }
  console.log(failed ? `\n${spec.gate || gate}: NÃO PASSOU (${failed} falha(s))` : `\n${spec.gate || gate || "fixture"}: PASSOU`);

  if (report) {
    const file = out || path.join(os.tmpdir(), "stitch-report.html");
    if (path.resolve(file).startsWith(path.join(root, "docs", "qa"))) throw new Error("The report never goes in docs/qa/.");
    await writeReport(file, spec, results, !!fixture);
    console.log(`Report: ${file}`);
  }
  await closeBrowser();
  return failed;
}

// ---- report ----------------------------------------------------------------------------------------------------

export function cropPng(buf, bbox, pad) {
  const src = PNG.sync.read(buf);
  const x0 = Math.max(0, bbox.x0 - pad), y0 = Math.max(0, bbox.y0 - pad);
  const x1 = Math.min(src.width - 1, bbox.x1 + pad), y1 = Math.min(src.height - 1, bbox.y1 + pad);
  const dst = new PNG({ width: x1 - x0 + 1, height: y1 - y0 + 1 });
  PNG.bitblt(src, dst, x0, y0, dst.width, dst.height, 0, 0);
  return PNG.sync.write(dst);
}

const dataUri = (buf) => `data:image/png;base64,${buf.toString("base64")}`;
const esc = (s) => String(s).replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" })[c]);

async function beforeAfter(r, tmp) {
  if (!r.screen) return `<p class="muted">Sem imagem (tela não resolvida ou fixture).</p>`;
  const dest = path.join(tmp, r.theme);
  fs.mkdirSync(dest, { recursive: true });
  await exportOne(r.theme, r.gold, r.screen, dest);
  const now = fs.readFileSync(path.join(dest, `${r.gold}.png`));
  const old = gitGold(r.theme, r.gold);
  if (!old) return `<figure><figcaption>Novo gold</figcaption><img src="${dataUri(now)}" style="width:390px"></figure>`;
  const d = pngDiff(old, now);
  if (!d.sameSize) {
    return `<p>Tamanho mudou: ${d.sizes}</p><div class="pair"><figure><figcaption>Antes (git)</figcaption><img src="${dataUri(old)}" style="width:390px"></figure>` +
      `<figure><figcaption>Depois (Stitch)</figcaption><img src="${dataUri(now)}" style="width:390px"></figure></div>`;
  }
  if (!d.bbox) return `<p class="muted">Sem mudança visual (${d.pct.toFixed(3)}% px).</p>`;
  const w = Math.round((Math.min(PNG.sync.read(now).width - 1, d.bbox.x1 + 48) - Math.max(0, d.bbox.x0 - 48) + 1) / 2);
  return `<p>${d.pct.toFixed(3)}% px mudaram; recorte da região alterada.</p><div class="pair">` +
    `<figure><figcaption>Antes (git)</figcaption><img src="${dataUri(cropPng(old, d.bbox, 48))}" style="width:${w}px"></figure>` +
    `<figure><figcaption>Depois (Stitch)</figcaption><img src="${dataUri(cropPng(now, d.bbox, 48))}" style="width:${w}px"></figure></div>`;
}

function fixPrompt(r, spec, entry) {
  const fixes = [...new Set(r.lines.filter((l) => !l.ok && l.fix).map((l) => l.fix))];
  if (!fixes.length) return "";
  const keep = [...new Set([...(spec.keep || []), ...(entry.keep || []),
    ...(entry.checks || []).filter((c) => c.type === "text").flatMap((c) => c.has || [])])];
  const text = `Screen to edit: "${r.title}".\n\nFix only this on the screen:\n${fixes.map((f, i) => `${i + 1}. ${f}`).join("\n")}\n\n` +
    `Keep unchanged${keep.length ? `: ${keep.map((k) => `"${k}"`).join(", ")},` : ""} and everything else on this screen (colors, text, spacing, header, status bar).`;
  return `<h4>Prompt de correção — ${r.theme === "dark" ? "Dark" : "Light"}</h4><pre>${esc(text)}</pre>`;
}

async function writeReport(file, spec, results, fixture) {
  const tmp = fs.mkdtempSync(path.join(os.tmpdir(), "stitch-verify-"));
  const sections = [];
  for (const r of results) {
    const entry = spec.screens.find((s) => s.gold === r.gold);
    const failed = r.lines.filter((l) => !l.ok).length;
    const rows = r.lines.map((l) => `<tr class="${l.ok ? (l.warn ? "warn" : "ok") : "fail"}"><td>${l.ok ? (l.warn ? "aviso" : "ok") : "FALHA"}</td><td>${esc(l.label)}</td><td>${esc(l.measured)}</td></tr>`).join("");
    sections.push(`<section><h3>${esc(r.title)} <code>${r.theme}/${r.gold}</code> ${failed ? `<span class="bad">${failed} falha(s)</span>` : `<span class="good">ok</span>`}</h3>` +
      (fixture ? "" : await beforeAfter(r, tmp)) + `<table>${rows}</table>${fixPrompt(r, spec, entry)}</section>`);
  }
  fs.rmSync(tmp, { recursive: true, force: true });
  const total = results.reduce((n, r) => n + r.lines.filter((l) => !l.ok).length, 0);
  fs.mkdirSync(path.dirname(path.resolve(file)), { recursive: true });
  fs.writeFileSync(file, `<!doctype html><html lang="pt-BR"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Stitch ${esc(spec.gate || "fixture")}</title><style>
:root{--bg:#f4f3f0;--surf:#fff;--text:#14161a;--muted:#5c636b;--line:#d5d2cc;--good:#1f8a4c;--bad:#c14d40;--gold:#b8873d}
@media (prefers-color-scheme:dark){:root{--bg:#0b0d10;--surf:#171b20;--text:#f3f5f7;--muted:#8b939c;--line:#2a3139;--good:#7dda9a;--bad:#e07a6a;--gold:#e8b86d}}
body{background:var(--bg);color:var(--text);font:14px/1.45 system-ui,sans-serif;margin:0 auto;max-width:980px;padding:16px}
section{background:var(--surf);border:1px solid var(--line);border-radius:14px;padding:16px;margin:16px 0}
h1{font-size:22px}h3{margin:0 0 8px;font-size:16px}h4{margin:16px 0 6px}code{color:var(--muted)}
.good{color:var(--good)}.bad,.fail td:first-child{color:var(--bad);font-weight:600}.warn td:first-child{color:var(--gold)}.muted{color:var(--muted)}
table{border-collapse:collapse;width:100%;margin-top:8px}td{border-top:1px solid var(--line);padding:4px 8px;vertical-align:top;word-break:break-word}
.pair{display:flex;gap:16px;flex-wrap:wrap}figure{margin:0}figcaption{color:var(--muted);font-size:12px}img{max-width:100%;border:1px solid var(--line);border-radius:6px}
pre{white-space:pre-wrap;background:var(--bg);border:1px solid var(--line);border-radius:10px;padding:12px;font-size:13px}
</style></head><body><h1>Gate ${esc(spec.gate || "fixture")}: ${total ? `<span class="bad">não passou (${total})</span>` : `<span class="good">passou</span>`}</h1>
${sections.join("\n")}</body></html>`);
}

const isMain = process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url);
if (isMain) {
  const fixture = argValue("--fixture");
  const gate = fixture ? null : process.argv.slice(2).find((a) => !a.startsWith("--") && a !== argValue("--out"));
  if (!gate && !fixture) {
    console.error("Usage: node tools/verify-stitch.mjs <gate> [--report] [--out <file.html>] | --fixture <dir> [--report]");
    process.exit(2);
  }
  verify({ gate, fixture, report: process.argv.includes("--report"), out: argValue("--out") })
    .then((failed) => process.exit(failed ? 1 : 0))
    .catch((err) => { console.error(err); process.exit(2); });
}
