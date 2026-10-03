#!/usr/bin/env node
/**
 * Read-only audit: node tools/check-docs.mjs [--root <path>].
 * Fails when a current-state document can contradict the file that owns a fact (SD2 rules).
 *
 * Live set: AGENTS.md, README.md and every docs/**.md except
 *   docs/<ctx>/plans/completed/**, docs/<ctx>/plans/cancelled/** (history),
 *   docs/decisions/**, docs/<ctx>/adrs/** (C5 only),
 *   docs/<ctx>/validation/** (C1 only),
 *   docs/qa/_legacy/**,
 *   docs/sdd/templates/** (placeholders by design).
 * Skill trees are covered by tools/check-skills.mjs.
 *
 * C1 relative links resolve · C2 "no specification" claims · C3 plan links in specs only under Provenance
 * C4 status copies · C5 ADR status line · C6 READMEs link history folders, not files · C7 gold inventory
 *
 * No dependencies beyond tools/export-stitch.mjs (C7). Never writes, fixes or downloads.
 */
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";
import { parseArgs } from "node:util";
import { proseOnly } from "./check-skills.mjs";

const SKIPPED = /^docs\/[^/]+\/plans\/(completed|cancelled)\/|^docs\/decisions\/|^docs\/[^/]+\/adrs\/|^docs\/qa\/_legacy\/|^docs\/sdd\/templates\//;
const VALIDATION = /^docs\/[^/]+\/validation\//;
const ADR_FILE = /^docs\/decisions\/[^/]+\.md$|^docs\/[^/]+\/adrs\/[^/]+\.md$/;
const SPEC_FILE = /^docs\/[^/]+\/specifications\/[^/]+\.md$/;
const PROVENANCE = /^##\s+(Provenance|Proveniência)\s*$/;
const STATUS_WORD = "(conclu[ií]do|proposto|proposta|proposed|pendente|pending|aceito|accepted)";
const PROVENANCE_STATUS = new RegExp("\\([^)]*(?<!\\p{L})" + STATUS_WORD + "(?!\\p{L})[^)]*\\)|`\\s*" + STATUS_WORD + "\\s*`", "iu");
const PROPOSED = /(?<!\p{L})(proposto|proposta|proposed)(?!\p{L})/iu;
const PENDING_PHRASE = /aguardando aprovação|pending approval|pendente de aprovação|em implementação|not current behavior/i;
const NO_SPEC = /sem especificacao|sem specifications|no specification/;
// One link grammar: INLINE_LINK finds targets; LINK_TARGET strips them before the Provenance status test (C4)
const LINK_TARGET_SOURCE = String.raw`\]\(\s*(<[^>\n]+>|(?:\\.|[^\s)])+)(?:\s+(?:"[^"\n]*"|'[^'\n]*'))?\s*\)`;
const INLINE_LINK = new RegExp(String.raw`\[[^\]\n]*` + LINK_TARGET_SOURCE, "g");
const LINK_TARGET = new RegExp(LINK_TARGET_SOURCE, "g");

function posix(p) {
  return p.split(path.sep).join("/");
}

function walk(root, dir, out) {
  const absolute = path.join(root, dir);
  if (!fs.existsSync(absolute)) return;
  for (const entry of fs.readdirSync(absolute, { withFileTypes: true }).sort((a, b) => a.name.localeCompare(b.name))) {
    const rel = dir ? dir + "/" + entry.name : entry.name;
    if (entry.isDirectory()) walk(root, rel, out);
    else if (entry.isFile() && entry.name.toLowerCase().endsWith(".md")) out.push(rel);
  }
}

/** Relative link targets per line, outside Markdown code. Same exclusions as check-skills. */
export function linksByLine(markdown) {
  const lines = proseOnly(markdown).split("\n");
  const found = [];
  lines.forEach((text, index) => {
    for (const match of text.matchAll(INLINE_LINK)) {
      let target = match[1].replace(/^<|>$/g, "").replace(/\\([() ])/g, "$1");
      if (!target || /^(?:#|\/|\\|~|[a-z][a-z0-9+.-]*:)/i.test(target)) continue;
      if (/[\x24{}<>*]/.test(target) || /%[A-Z_][A-Z0-9_]*%/.test(target)) continue;
      target = target.split(/[?#]/)[0];
      if (!target) continue;
      try { target = decodeURIComponent(target); } catch { /* keep raw */ }
      found.push({ line: index + 1, target, end: match.index + match[0].length });
    }
  });
  return { lines, links: found };
}

function fold(text) {
  return text.normalize("NFD").replace(/\p{M}/gu, "").toLowerCase();
}

function adrStatus(file) {
  return fs.readFileSync(file, "utf8").split(/\r?\n/).slice(0, 10).find((l) => /^- (Status|Estado):/.test(l));
}

async function goldInventory(root, findings) {
  const qa = "docs/qa/README.md";
  const qaFile = path.join(root, qa);
  if (!fs.existsSync(qaFile)) {
    findings.push(qa + ":0 C7 missing gold inventory file");
    return;
  }
  const text = fs.readFileSync(qaFile, "utf8");
  const section = text.match(/^## Golds\s*$([\s\S]*?)(?=^## |(?![\s\S]))/m);
  const block = section && section[1].match(/^ {0,3}(`{3,}|~{3,})[^\n]*\n([\s\S]*?)^ {0,3}\1/m);
  if (!block) {
    findings.push(qa + ":0 C7 no fenced id list under '## Golds'");
    return;
  }
  const listed = new Set([...block[2].matchAll(/([A-Za-z0-9]+)\.png/g)].map((m) => m[1]));
  let screens;
  try {
    screens = await import(pathToFileURL(path.join(root, "tools", "export-stitch.mjs")).href);
  } catch (error) {
    findings.push("tools/export-stitch.mjs:0 C7 cannot load the gold map: " + error.message);
    return;
  }
  for (const [theme, map] of [["dark", screens.DARK_SCREENS], ["light", screens.LIGHT_SCREENS]]) {
    const ids = new Set(Object.keys(map ?? {}));
    for (const id of ids) if (!listed.has(id)) findings.push(qa + ":0 C7 " + theme + " gold '" + id + "' is in export-stitch.mjs but not in the inventory");
    for (const id of listed) if (!ids.has(id)) findings.push(qa + ":0 C7 " + theme + " gold '" + id + "' is in the inventory but not in export-stitch.mjs");
  }
}

export async function checkDocs(repoRoot) {
  const root = path.resolve(repoRoot);
  const findings = [];
  const all = [];
  for (const top of ["AGENTS.md", "README.md"]) if (fs.existsSync(path.join(root, top))) all.push(top);
  walk(root, "docs", all);

  const live = all.filter((f) => !SKIPPED.test(f) && !VALIDATION.test(f));
  const linked = all.filter((f) => !SKIPPED.test(f));

  for (const rel of linked) {
    const absolute = path.join(root, rel);
    const markdown = fs.readFileSync(absolute, "utf8");
    const { lines, links } = linksByLine(markdown);
    const dir = path.dirname(absolute);
    const isLive = live.includes(rel);
    const isSpec = SPEC_FILE.test(rel);
    const isReadme = path.basename(rel) === "README.md";
    const provenanceAt = isSpec ? lines.findIndex((l) => PROVENANCE.test(l)) + 1 : 0;

    for (const { line, target, end } of links) {
      const resolved = path.resolve(dir, target);
      const repoPath = posix(path.relative(root, resolved));
      // C1
      if (!fs.existsSync(resolved)) {
        findings.push(rel + ":" + line + " C1 broken link -> " + target);
        continue;
      }
      if (!isLive) continue;
      const text = lines[line - 1];
      const inPlans = /(^|\/)plans\//.test(repoPath);
      // C3
      if (isSpec && inPlans && (provenanceAt === 0 || line < provenanceAt)) {
        findings.push(rel + ":" + line + " C3 plan link outside the Provenance section -> " + target);
      }
      // C4: an accepted ADR described as proposed
      // the status word must follow the link closely ("[ADR-028](…) (proposto)", "[ADR-024](…) is proposed")
      const after = text.slice(end, end + 40).split("[")[0];
      if (ADR_FILE.test(repoPath) && PROPOSED.test(after)) {
        const status = adrStatus(resolved);
        if (status && /accepted|aceito/i.test(status)) {
          findings.push(rel + ":" + line + " C4 calls an accepted ADR proposed -> " + target);
        }
      }
      // C4: a completed plan described as pending
      if (/(^|\/)plans\/completed\//.test(repoPath) && PENDING_PHRASE.test(text)) {
        findings.push(rel + ":" + line + " C4 calls a completed plan pending -> " + target);
      }
      // C6
      if (isReadme && /(^|\/)plans\/(completed|cancelled)\/[^/]+$/.test(repoPath) && fs.statSync(resolved).isFile()) {
        findings.push(rel + ":" + line + " C6 README links an individual history plan; link the folder -> " + target);
      }
    }

    if (!isLive) continue;
    // C4: Provenance entries carry no status marker
    if (isSpec && provenanceAt > 0) {
      markdown.split(/\r?\n/).slice(provenanceAt).forEach((text, i) => {
        if (/^##\s/.test(text)) return;
        // a folder name in a link target (plans/pending_manual_validation/) is not a status marker
        if (PROVENANCE_STATUS.test(text.replace(LINK_TARGET, "]"))) findings.push(rel + ":" + (provenanceAt + 1 + i) + " C4 status marker in a Provenance entry");
      });
    }
    // C2
    if (isReadme) {
      const specs = path.join(dir, "specifications");
      const hasSpecs = fs.existsSync(specs) && fs.readdirSync(specs).some((f) => f.toLowerCase().endsWith(".md"));
      if (hasSpecs) {
        lines.forEach((text, i) => {
          if (NO_SPEC.test(fold(text))) findings.push(rel + ":" + (i + 1) + " C2 claims no specification, but specifications/ has files");
        });
      }
    }
  }

  // C5
  for (const rel of all.filter((f) => ADR_FILE.test(f))) {
    if (!adrStatus(path.join(root, rel))) findings.push(rel + ":1 C5 no '- Status:' or '- Estado:' line in the first 10 lines");
  }

  // C7
  await goldInventory(root, findings);

  return { root, findings, files: { live: live.length, linked: linked.length } };
}

async function main() {
  const { values } = parseArgs({ options: { root: { type: "string" }, help: { type: "boolean" } } });
  if (values.help) {
    console.log("Usage: node tools/check-docs.mjs [--root <path>]\nRead-only: links, spec Provenance, status copies, ADR status lines, README routing and the gold inventory.\nDoes not judge semantic freshness, anchors or remote URLs.");
    return;
  }
  const root = values.root ?? path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
  const result = await checkDocs(root);
  for (const finding of result.findings) console.error(finding);
  if (result.findings.length) {
    console.error("Docs check failed: " + result.findings.length + " finding(s). No files changed.");
    process.exitCode = 1;
  } else {
    console.log("Docs check passed: " + result.files.live + " live files, " + result.files.linked + " files link-checked; no broken links, misplaced plan links, status copies, missing ADR status lines, history links in READMEs or gold inventory drift.");
  }
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main().catch((error) => {
    console.error("Docs check could not finish: " + error.message + ". No files changed.");
    process.exitCode = 2;
  });
}
