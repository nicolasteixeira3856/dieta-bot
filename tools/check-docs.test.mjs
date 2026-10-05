import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import os from "node:os";
import { createHash } from "node:crypto";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import { checkDocs, linksByLine } from "./check-docs.mjs";

const checker = fileURLToPath(new URL("./check-docs.mjs", import.meta.url));

function write(root, file, text) {
  const target = path.join(root, file);
  fs.mkdirSync(path.dirname(target), { recursive: true });
  fs.writeFileSync(target, text);
}

const BASE = {
  "AGENTS.md": "# Constitution\n\nMatrix: [docs](docs/README.md).\n",
  "README.md": "# App\n\n[AGENTS](AGENTS.md)\n",
  "docs/README.md": "# Matrix\n\n- [produto](produto/README.md)\n- [android plans](android/plans/)\n",
  "docs/produto/README.md": "# produto\n\n- [chat](specifications/chat.md)\n- [ADR-001](adrs/ADR-001-x.md)\n- Histórico: [`plans/completed/`](../android/plans/completed/)\n",
  "docs/produto/specifications/chat.md": "# Chat\n\n1. Rule ([ADR-001](../adrs/ADR-001-x.md)).\n\n## Proveniência\n\n- [A1](../../android/plans/completed/a1-x.md) — Pending meal is not a skip\n",
  "docs/produto/adrs/ADR-001-x.md": "# ADR-001\n\n- Status: Accepted\n\nBody.\n",
  "docs/decisions/001-old.md": "# 001\n\n- Status: Superseded by [ADR-001](../produto/adrs/ADR-001-x.md).\n",
  "docs/android/plans/completed/a1-x.md": "# Plan — A1 x\n\n- State: `Concluído`\n\nOld [gone](nowhere.md) link stays history.\n",
  "docs/android/validation/a1.md": "# Evidence\n\n[A1](../plans/completed/a1-x.md)\n",
  "docs/sdd/templates/specification.md": "## Provenance\n\n- [<ID>](<caminho do plano>) — <título>\n",
  "docs/qa/README.md": "# QA\n\n## Golds\n\n```text\nsplash.png · chat0.png\n```\n\n## Other\n\nchatX.png is mentioned here and ignored.\n",
  "tools/export-figma.mjs": "export const DARK_FRAMES = { splash: '1:1', chat0: '1:2' };\nexport const LIGHT_FRAMES = { splash: '1:3', chat0: '1:4' };\n",
};

function fixture(t, overrides = {}) {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), "dieta-docs-"));
  t.after(() => fs.rmSync(root, { recursive: true, force: true }));
  for (const [file, text] of Object.entries({ ...BASE, ...overrides })) {
    if (text !== null) write(root, file, text);
  }
  return root;
}

function digest(root) {
  const hash = createHash("sha256");
  (function visit(dir) {
    for (const entry of fs.readdirSync(dir, { withFileTypes: true }).sort((a, b) => a.name.localeCompare(b.name))) {
      const target = path.join(dir, entry.name);
      if (entry.isDirectory()) visit(target);
      else hash.update(entry.name).update(fs.readFileSync(target));
    }
  })(root);
  return hash.digest("hex");
}

async function findings(t, overrides) {
  return (await checkDocs(fixture(t, overrides))).findings;
}

function only(list, check) {
  assert.ok(list.length > 0, "expected a finding");
  for (const f of list) assert.match(f, new RegExp(" " + check + " "), f);
}

test("a consistent fixture passes through API and CLI --root", async (t) => {
  const root = fixture(t);
  assert.deepEqual((await checkDocs(root)).findings, []);
  const run = spawnSync(process.execPath, [checker, "--root", root], { encoding: "utf8" });
  assert.equal(run.status, 0, run.stderr);
  assert.match(run.stdout, /Docs check passed/);
});

test("C1: broken relative links fail in live and validation files; history, code and URLs are skipped", async (t) => {
  only(await findings(t, { "docs/README.md": "# M\n\n[x](missing.md)\n" }), "C1");
  only(await findings(t, { "docs/android/validation/a1.md": "[x](../nope.md)\n" }), "C1");
  assert.deepEqual(await findings(t, {
    "docs/README.md": "# M\n\n`[x](missing.md)`\n\n```\n[y](missing.md)\n```\n\n[web](https://example.com) [anchor](#top) [folder](produto/)\n",
  }), []);
});

test("C2: a README claiming no specification fails only when specifications/ has files", async (t) => {
  only(await findings(t, { "docs/produto/README.md": "# produto\n\nSem especificação viva.\n\n[chat](specifications/chat.md)\n" }), "C2");
  assert.deepEqual(await findings(t, { "docs/android/README.md": "# android\n\nSem especificacao neste contexto.\n" }), []);
});

test("C3: plan links in a spec are allowed only under Provenance", async (t) => {
  only(await findings(t, {
    "docs/produto/specifications/chat.md": "# Chat\n\n1. Since [A1](../../android/plans/completed/a1-x.md).\n\n## Proveniência\n\n- [A1](../../android/plans/completed/a1-x.md) — x\n",
  }), "C3");
  only(await findings(t, { "docs/produto/specifications/chat.md": "# Chat\n\n1. Rule, [A1](../../android/plans/completed/a1-x.md).\n" }), "C3");
});

test("C4: status markers in Provenance fail; free-text titles do not", async (t) => {
  only(await findings(t, {
    "docs/produto/specifications/chat.md": "# Chat\n\n1. Rule.\n\n## Provenance\n\n- [A1 (Concluído)](../../android/plans/completed/a1-x.md)\n",
  }), "C4");
  only(await findings(t, {
    "docs/produto/specifications/chat.md": "# Chat\n\n1. Rule.\n\n## Provenance\n\n- [A1](../../android/plans/completed/a1-x.md) — x, `pending`\n",
  }), "C4");
  only(await findings(t, {
    "docs/android/plans/pending_manual_validation/a38-x.md": "# Plan — A38 x\n",
    "docs/produto/specifications/chat.md": "# Chat\n\n1. Rule.\n\n## Provenance\n\n- [A38](../../android/plans/pending_manual_validation/a38-x.md) (pendente) — x\n",
  }), "C4");
});

test("C4: a link target into pending_manual_validation/ is not a Provenance status marker", async (t) => {
  assert.deepEqual(await findings(t, {
    "docs/android/plans/pending_manual_validation/a38-x.md": "# Plan — A38 x\n",
    "docs/produto/specifications/chat.md": "# Chat\n\n1. Rule.\n\n## Provenance\n\n- [A38](../../android/plans/pending_manual_validation/a38-x.md) — client storage\n"
      + "- [A38](<../../android/plans/pending_manual_validation/a38-x.md> \"A38\") — angle target with a title\n",
  }), []);
});

test("C4: an accepted ADR called proposed and a completed plan called pending fail; rule words do not", async (t) => {
  only(await findings(t, { "docs/produto/README.md": "# p\n\n[ADR-001](adrs/ADR-001-x.md) is proposed.\n[chat](specifications/chat.md)\n" }), "C4");
  only(await findings(t, { "docs/guide.md": "# G\n\n[A1](android/plans/completed/a1-x.md) aguardando aprovação.\n" }), "C4");
  assert.deepEqual(await findings(t, {
    "docs/produto/specifications/chat.md": "# Chat\n\n1. Refeição pendente e proposed memory facts ([ADR-001](../adrs/ADR-001-x.md)) seguem a regra; modo independente.\n\n## Proveniência\n\n- [A1](../../android/plans/completed/a1-x.md) — x\n",
  }), []);
  assert.deepEqual(await findings(t, {
    "docs/produto/adrs/ADR-001-x.md": "# ADR-001\n\n- Status: Proposto\n",
    "docs/produto/README.md": "# p\n\n[ADR-001](adrs/ADR-001-x.md) is proposed.\n[chat](specifications/chat.md)\n",
  }), []);
});

test("C5: every ADR needs a status line in its first 10 lines", async (t) => {
  only(await findings(t, { "docs/decisions/001-old.md": "# 001\n\nNo status here.\n" }), "C5");
  only(await findings(t, { "docs/produto/adrs/ADR-001-x.md": "# ADR\n" + "\n".repeat(12) + "- Status: Accepted\n" }), "C5");
});

test("C6: READMEs link history folders, not individual history plans", async (t) => {
  only(await findings(t, { "docs/produto/README.md": "# p\n\n[A1](../android/plans/completed/a1-x.md)\n[chat](specifications/chat.md)\n" }), "C6");
  assert.deepEqual(await findings(t, { "docs/android/validation/README.md": "[A1](../plans/completed/a1-x.md)\n" }), []);
});

test("C7: the gold inventory must equal both theme maps of export-figma.mjs", async (t) => {
  // mapped in one theme beyond the inventory
  only(await findings(t, { "tools/export-figma.mjs": "export const DARK_FRAMES = { splash: 1, chat0: 2, chatX: 3 };\nexport const LIGHT_FRAMES = { splash: 1, chat0: 2 };\n" }), "C7");
  // in the inventory, missing from the map
  only(await findings(t, { "docs/qa/README.md": "# QA\n\n## Golds\n\n```text\nsplash.png · chat0.png · home0.png\n```\n" }), "C7");
  // mapped, missing from the inventory
  only(await findings(t, { "docs/qa/README.md": "# QA\n\n## Golds\n\n```text\nsplash.png\n```\n" }), "C7");
  // mapped in one theme only
  only(await findings(t, { "tools/export-figma.mjs": "export const DARK_FRAMES = { splash: 1, chat0: 2 };\nexport const LIGHT_FRAMES = { splash: 1 };\n" }), "C7");
  only(await findings(t, { "docs/qa/README.md": "# QA\n\nNo inventory.\n" }), "C7");
  only(await findings(t, { "tools/export-figma.mjs": null }), "C7");
});

test("C7: every id is listed once", async (t) => {
  only(await findings(t, { "docs/qa/README.md": "# QA\n\n## Golds\n\n```text\nsplash.png · chat0.png\nchat0.png\n```\n" }), "C7");
});

test("failing runs exit 1 and leave every fixture byte unchanged", (t) => {
  const root = fixture(t, { "docs/README.md": "# M\n\n[x](missing.md)\n" });
  const before = digest(root);
  const run = spawnSync(process.execPath, [checker, "--root", root], { encoding: "utf8" });
  assert.equal(run.status, 1);
  assert.match(run.stderr, /docs\/README\.md:3 C1 broken link -> missing\.md/);
  assert.equal(digest(root), before);
});

test("CRLF files give the same findings as LF", async (t) => {
  const text = "# p\r\n\r\nSem especificação.\r\n\r\n[chat](specifications/chat.md)\r\n";
  only(await findings(t, { "docs/produto/README.md": text }), "C2");
  assert.deepEqual(linksByLine("a\r\n[x](y.md)\r\n").links.map(({ line, target }) => ({ line, target })), [{ line: 2, target: "y.md" }]);
});

test("unknown CLI options fail without changing the fixture", (t) => {
  const root = fixture(t);
  const before = digest(root);
  const run = spawnSync(process.execPath, [checker, "--root", root, "--fix"], { encoding: "utf8" });
  assert.notEqual(run.status, 0);
  assert.equal(digest(root), before);
});
