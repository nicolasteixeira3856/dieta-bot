import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import os from "node:os";
import { createHash } from "node:crypto";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import { checkSkills, SKILL_ROOTS, relativeMarkdownTargets } from "./check-skills.mjs";

const checker = fileURLToPath(new URL("./check-skills.mjs", import.meta.url));
function write(root, file, bytes) {
  const target = path.join(root, file);
  fs.mkdirSync(path.dirname(target), { recursive: true });
  fs.writeFileSync(target, bytes);
}
function fixture(t) {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), "dieta-skills-"));
  t.after(() => {
    const resolved = path.resolve(root);
    assert.equal(path.dirname(resolved), path.resolve(os.tmpdir()));
    assert.match(path.basename(resolved), /^dieta-skills-/);
    fs.rmSync(resolved, { recursive: true, force: true });
  });
  write(root, "AGENTS.md", "Retired: nutri-*, debate-feature, dieta-bot-android-decisao, dieta-bot-android-lembrar.\n");
  for (const mirror of SKILL_ROOTS) {
    write(root, mirror + "/current/SKILL.md", "---\nname: current\ndescription: Fixture.\n---\n[Guide](references/guide.md)\n");
    write(root, mirror + "/current/references/guide.md", "Current guide.\n");
    write(root, mirror + "/current/payload.bin", Buffer.from([0, 13, 255, 10]));
  }
  return root;
}
function snapshot(root) {
  const result = {};
  function visit(dir, prefix = "") {
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
      if (entry.isDirectory()) visit(path.join(dir, entry.name), prefix + entry.name + "/");
      else if (entry.isFile()) result[prefix + entry.name] = createHash("sha256").update(fs.readFileSync(path.join(dir, entry.name))).digest("hex");
    }
  }
  visit(root);
  return result;
}
function hasFinding(result, text) {
  assert.ok(result.errors.some((error) => error.includes(text)), JSON.stringify(result.errors));
}

test("identical complete trees pass through API and CLI --root", (t) => {
  const root = fixture(t);
  const before = snapshot(root);
  assert.deepEqual(checkSkills(root).errors, []);
  const run = spawnSync(process.execPath, [checker, "--root", root], { encoding: "utf8" });
  assert.equal(run.status, 0, run.stderr);
  assert.match(run.stdout, /3 files/);
  assert.deepEqual(snapshot(root), before);
});

test("content corruption fails, exits nonzero and leaves every fixture byte unchanged", (t) => {
  const root = fixture(t);
  write(root, ".claude/skills/current/payload.bin", Buffer.from([0, 13, 254, 10]));
  const before = snapshot(root);
  hasFinding(checkSkills(root), "Changed bytes: .claude/skills/current/payload.bin");
  const run = spawnSync(process.execPath, [checker, "--root", root], { encoding: "utf8" });
  assert.equal(run.status, 1);
  assert.match(run.stderr, /No files changed/);
  assert.deepEqual(snapshot(root), before);
});

test("CRLF versus LF is a byte divergence", (t) => {
  const root = fixture(t);
  write(root, ".grok/skills/current/references/guide.md", "Current guide.\r\n");
  hasFinding(checkSkills(root), "Changed bytes: .grok/skills/current/references/guide.md");
});

test("missing entire root is reported", (t) => {
  const root = fixture(t);
  fs.renameSync(path.join(root, ".hermes/skills"), path.join(root, ".hermes/absent"));
  hasFinding(checkSkills(root), "Missing skill root: .hermes/skills");
});

test("missing and extra skill directories are reported", (t) => {
  const root = fixture(t);
  fs.renameSync(path.join(root, ".claude/skills/current"), path.join(root, ".claude/skills/other"));
  const result = checkSkills(root);
  hasFinding(result, "Missing skill: .claude/skills/current");
  hasFinding(result, "Extra skill: .claude/skills/other");
});

test("missing supporting files are reported", (t) => {
  const root = fixture(t);
  fs.renameSync(path.join(root, ".grok/skills/current/payload.bin"), path.join(root, "moved.bin"));
  hasFinding(checkSkills(root), "Missing file: .grok/skills/current/payload.bin");
});

test("extra supporting files and caches are included in full inventory", (t) => {
  const root = fixture(t);
  write(root, ".hermes/skills/current/__pycache__/unexpected.pyc", Buffer.from([1, 2]));
  hasFinding(checkSkills(root), "Extra file: .hermes/skills/current/__pycache__/unexpected.pyc");
});

test("every direct skill directory requires SKILL.md even when mirrors match", (t) => {
  const root = fixture(t);
  for (const mirror of SKILL_ROOTS) write(root, mirror + "/empty/README.md", "No entrypoint.\n");
  const result = checkSkills(root);
  assert.equal(result.errors.filter((error) => error.startsWith("Missing SKILL.md")).length, 4);
});

test("retired exact names and globs are derived from AGENTS", (t) => {
  const root = fixture(t);
  for (const mirror of SKILL_ROOTS) {
    write(root, mirror + "/nutri-old/SKILL.md", "Retired.\n");
    write(root, mirror + "/debate-feature/SKILL.md", "Retired.\n");
  }
  const result = checkSkills(root);
  assert.equal(result.errors.filter((error) => error.startsWith("Retired skill")).length, 8);
});

test("missing constitution or retired list prevents an unqualified pass", (t) => {
  const root = fixture(t);
  fs.renameSync(path.join(root, "AGENTS.md"), path.join(root, "moved-agents.md"));
  hasFinding(checkSkills(root), "Missing AGENTS.md");
  write(root, "AGENTS.md", "# No retired inventory.\n");
  hasFinding(checkSkills(root), "Missing Retired:");
});

test("broken concrete links in entrypoints and supporting Markdown fail", (t) => {
  const root = fixture(t);
  for (const mirror of SKILL_ROOTS) {
    write(root, mirror + "/current/SKILL.md", "[Missing](missing.md#section)\n");
    write(root, mirror + "/current/references/guide.md", "[Reference][missing]\n[missing]: absent.md\n");
  }
  assert.equal(checkSkills(root).errors.filter((error) => error.startsWith("Broken Markdown reference")).length, 8);
});

test("valid parent, encoded-space, angle and image links are resolved", (t) => {
  const root = fixture(t);
  write(root, "docs/guide with spaces.md", "Guide.\n");
  for (const mirror of SKILL_ROOTS) {
    write(root, mirror + "/current/SKILL.md", "[Parent](../../../AGENTS.md#rules)\n[Space](../../../docs/guide%20with%20spaces.md)\n[Angle](<../../../docs/guide with spaces.md>)\n![Binary](payload.bin)\n");
  }
  assert.deepEqual(checkSkills(root).errors, []);
});

test("external, fragment, runtime and template paths and code examples are excluded", (t) => {
  const root = fixture(t);
  const tick = String.fromCharCode(96);
  const prose = [
    "[HTTPS](https://example.invalid/nope.md)", "[Mail](mailto:owner@example.invalid)",
    "[Anchor](#missing)", "[Network](//example.invalid/path)",
    "[Runtime](/runtime/screenshot.png)", "[Template](<scratchpad>/shot.png)",
    "[Variable](" + String.fromCharCode(36) + "{OUTPUT}/shot.png)", "[Placeholder]({theme}/shot.png)",
    "[Env](%TEMP%/shot.png)", tick + "[inline](nope.md)" + tick,
    "    [indented](nope.md)", tick.repeat(3) + "md", "[fenced](nope.md)", tick.repeat(3),
    "~~~md", "[tilde](nope.md)", "~~~", "[Good](references/guide.md)"
  ].join("\n");
  for (const mirror of SKILL_ROOTS) write(root, mirror + "/current/SKILL.md", prose);
  assert.deepEqual(relativeMarkdownTargets(prose), ["references/guide.md"]);
  assert.deepEqual(checkSkills(root).errors, []);
});

test("unknown CLI options fail without inspecting or changing fixture", (t) => {
  const root = fixture(t);
  const before = snapshot(root);
  const run = spawnSync(process.execPath, [checker, "--root", root, "--repair"], { encoding: "utf8" });
  assert.equal(run.status, 2);
  assert.deepEqual(snapshot(root), before);
});
