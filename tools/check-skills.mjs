#!/usr/bin/env node
/**
 * Read-only audit: node tools/check-skills.mjs [--root <path>].
 * No dependencies, YAML parsing, remote-link checks or repairs.
 */
import fs from "node:fs";
import path from "node:path";
import { createHash } from "node:crypto";
import { fileURLToPath } from "node:url";
import { parseArgs } from "node:util";

export const SKILL_ROOTS = [".agents/skills", ".grok/skills", ".hermes/skills", ".claude/skills"];

function inventory(root, errors, label) {
  const files = new Map();
  const skills = [];
  if (!fs.existsSync(root)) {
    errors.push("Missing skill root: " + label);
    return { files, skills };
  }
  if (!fs.lstatSync(root).isDirectory() || fs.lstatSync(root).isSymbolicLink()) {
    errors.push("Skill root must be a real directory: " + label);
    return { files, skills };
  }
  function visit(dir, prefix = "") {
    for (const entry of fs.readdirSync(dir, { withFileTypes: true }).sort((a, b) => a.name.localeCompare(b.name))) {
      const relative = prefix + entry.name;
      const target = path.join(dir, entry.name);
      if (entry.isSymbolicLink()) {
        errors.push("Unsupported symlink: " + label + "/" + relative);
      } else if (entry.isDirectory()) {
        if (!prefix) {
          skills.push(entry.name);
          const skillFile = path.join(target, "SKILL.md");
          if (!fs.existsSync(skillFile) || !fs.lstatSync(skillFile).isFile()) {
            errors.push("Missing SKILL.md: " + label + "/" + entry.name);
          }
        }
        visit(target, relative + "/");
      } else if (entry.isFile()) {
        files.set(relative, createHash("sha256").update(fs.readFileSync(target)).digest("hex"));
      } else {
        errors.push("Unsupported entry: " + label + "/" + relative);
      }
    }
  }
  visit(root);
  return { files, skills };
}

export function proseOnly(markdown) {
  let fence = null;
  const lines = [];
  for (const line of markdown.split(/\r?\n/)) {
    const marker = line.match(/^ {0,3}(\x60{3,}|~{3,})/);
    if (fence) {
      if (marker && marker[1][0] === fence[0] && marker[1].length >= fence.length &&
          /^ {0,3}(\x60+|~+)\s*$/.test(line)) fence = null;
      lines.push("");
    } else if (marker) {
      fence = marker[1];
      lines.push("");
    } else {
      lines.push(/^( {4}|\t)/.test(line) ? "" : line);
    }
  }
  return lines.join("\n").replace(/(\x60+)[\s\S]*?\1/g, "");
}

/** Inline links/images and reference definitions outside Markdown code. */
export function relativeMarkdownTargets(markdown) {
  const prose = proseOnly(markdown);
  const targets = [];
  const inline = /\[[^\]\n]*\]\(\s*(<[^>\n]+>|(?:\\.|[^\s)])+)(?:\s+(?:"[^"\n]*"|'[^'\n]*'))?\s*\)/g;
  const definitions = /^ {0,3}\[[^\]\n]+\]:\s*(<[^>\n]+>|\S+)/gm;
  for (const regex of [inline, definitions]) {
    for (const match of prose.matchAll(regex)) {
      let target = match[1].replace(/^<|>$/g, "").replace(/\\([() ])/g, "$1");
      if (!target || /^(?:#|\/|\\|~|[a-z][a-z0-9+.-]*:)/i.test(target)) continue;
      if (/[\x24{}<>*]/.test(target) || /%[A-Z_][A-Z0-9_]*%/.test(target)) continue;
      target = target.split(/[?#]/)[0];
      if (!target) continue;
      try { targets.push(decodeURIComponent(target)); }
      catch { targets.push(target); }
    }
  }
  return [...new Set(targets)];
}

function retiredPatterns(repoRoot, errors) {
  const constitution = path.join(repoRoot, "AGENTS.md");
  if (!fs.existsSync(constitution)) {
    errors.push("Missing AGENTS.md: retired skill names cannot be checked.");
    return [];
  }
  const line = fs.readFileSync(constitution, "utf8").match(/^Retired:\s*(.+)$/m);
  if (!line) {
    errors.push("Missing Retired: list in AGENTS.md.");
    return [];
  }
  return line[1].trim().replace(/\.$/, "").split(/[,·]/).map((item) => {
    const glob = item.trim().replaceAll(String.fromCharCode(96), "");
    const escaped = glob.split("*").map((part) => part.replace(/[.*+?^\x24{}()|[\]\\]/g, "\\$&")).join(".*");
    return new RegExp("^" + escaped + "$");
  });
}

export function checkSkills(repoRoot) {
  const root = path.resolve(repoRoot);
  const errors = [];
  const retired = retiredPatterns(root, errors);
  const trees = SKILL_ROOTS.map((label) => ({ label, ...inventory(path.join(root, label), errors, label) }));
  const canonical = trees[0];
  for (const tree of trees) {
    for (const name of tree.skills) {
      if (retired.some((pattern) => pattern.test(name))) errors.push("Retired skill: " + tree.label + "/" + name);
    }
    if (tree !== canonical) {
      for (const name of canonical.skills) {
        if (!tree.skills.includes(name)) errors.push("Missing skill: " + tree.label + "/" + name);
      }
      for (const name of tree.skills) {
        if (!canonical.skills.includes(name)) errors.push("Extra skill: " + tree.label + "/" + name);
      }
      for (const [file, hash] of canonical.files) {
        if (!tree.files.has(file)) errors.push("Missing file: " + tree.label + "/" + file);
        else if (tree.files.get(file) !== hash) errors.push("Changed bytes: " + tree.label + "/" + file + " (vs " + canonical.label + ")");
      }
      for (const file of tree.files.keys()) {
        if (!canonical.files.has(file)) errors.push("Extra file: " + tree.label + "/" + file);
      }
    }
    for (const file of tree.files.keys()) {
      if (!file.toLowerCase().endsWith(".md")) continue;
      const absolute = path.join(root, tree.label, file);
      for (const target of relativeMarkdownTargets(fs.readFileSync(absolute, "utf8"))) {
        if (!fs.existsSync(path.resolve(path.dirname(absolute), target))) {
          errors.push("Broken Markdown reference: " + tree.label + "/" + file + " -> " + target);
        }
      }
    }
  }
  return { root, errors, inventories: trees.map(({ label, skills, files }) => ({ root: label, skills: skills.length, files: files.size })) };
}

function main() {
  const { values } = parseArgs({ options: { root: { type: "string" }, help: { type: "boolean" } } });
  if (values.help) {
    console.log("Usage: node tools/check-skills.mjs [--root <path>]\nRead-only: complete inventories/bytes, retired names and relative Markdown links.\nDoes not validate YAML, product correctness or remote URLs.");
    return;
  }
  const root = values.root ?? path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
  const result = checkSkills(root);
  for (const row of result.inventories) console.log(row.root + ": " + row.skills + " skills, " + row.files + " files");
  for (const error of result.errors) console.error(error);
  if (result.errors.length) {
    console.error("Skill check failed: " + result.errors.length + " finding(s). No files changed.");
    process.exitCode = 1;
  } else {
    console.log("Skill check passed: all four inventories and file bytes match; no retired names or broken concrete relative Markdown references.");
  }
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  try { main(); }
  catch (error) {
    console.error("Skill check could not finish: " + error.message + ". No files changed.");
    process.exitCode = 2;
  }
}
