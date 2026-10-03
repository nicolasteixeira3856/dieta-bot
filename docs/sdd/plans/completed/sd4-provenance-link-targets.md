# Plan — SD4 Provenance status check ignores link targets

- State: `Concluído`
- Date: 2026-10-03
- Owner: `sdd` — shared agent workflow maintenance.
- Code affected: `tools/check-docs.mjs`, `tools/check-docs.test.mjs`. Optional link-style conversion in the four specifications named in step 3. No app, server or infra code.
- Prerequisites: None. [SD3](sd3-docs-checker-and-ignores.md) is `Concluído`.

## Authorization gate

Implementation starts only after explicit approval identifying this file:

> Aprovo o plano `docs/sdd/plans/sd4-provenance-link-targets.md`. Implemente o plano aprovado.

One agent executes it.

## Objective

A specification can link a plan in `plans/pending_manual_validation/` from its Provenance section with a normal inline link, as `docs/sdd/README.md` § Completion requires, without a false C4 finding.

## Discovery evidence

Verified at `0a8f663`.

- `tools/check-docs.mjs:31` builds `PROVENANCE_STATUS` as `\([^)]*(?<!\p{L})STATUS_WORD(?!\p{L})[^)]*\)` or a backticked status word. Lines 161–166 test it on each raw line after the Provenance heading.
- The `(...)` alternative also matches the target of an inline link. In `[A38](../../android/plans/pending_manual_validation/a38-x.md)`, `pending` is preceded by `/` and followed by `_`, neither a letter, so it counts as a whole word.
- Reproduced on a disposable `--root` fixture with only that Provenance line: `docs/server/specifications/v1-chat.md:7 C4 status marker in a Provenance entry`.
- [SD3](sd3-docs-checker-and-ignores.md) § C4 defines the marker as "a parenthetical or backticked status word". A folder name in a link target is neither; this is a checker defect, not a rule change.
- The A38 delivery worked around it with reference-style links (`[A38][a38]` plus `[a38]: <path>` at the end of the file) in `docs/android/specifications/room-v2.md`, `docs/produto/specifications/chat.md`, `docs/produto/specifications/memoria-push.md` and `docs/server/specifications/v1-chat.md`. Those definitions are not on `master` at `0a8f663`. C1 (`INLINE_LINK`) and C3 do not see reference definitions, so the workaround leaves the link unchecked.

## Implementation scope

### 1. `tools/check-docs.mjs`

- Before testing `PROVENANCE_STATUS` on a Provenance line, remove inline link targets: replace each `](target)` with `]`, using the same target grammar as `INLINE_LINK` (angle-bracket targets, escaped characters, optional title). Link text stays, so `[A1 (Concluído)](…)` still fails.
- Only the C4 Provenance test uses the stripped line. C1, C3, C6 and the other C4 checks keep their current input.
- Update the header comment only if the C4 description there needs it. No other behavior change.

### 2. `tools/check-docs.test.mjs`

Extend the C4 Provenance test:

- Passing: a Provenance entry `- [A38](../../android/plans/pending_manual_validation/a38-x.md) — client storage` with the target file present in the fixture.
- Passing: the same with an angle-bracket target and a title, to cover the full `INLINE_LINK` grammar.
- Failing (unchanged): `[A1 (Concluído)](…)` and a backticked `` `pending` ``.
- Failing: a status marker after a pending-folder link on the same line (`- [A38](…/pending_manual_validation/…) (pendente)`), proving the strip does not hide real markers.

### 3. Optional: inline links again

If `master` at implementation time carries the A38 reference definitions in the four specifications above, convert each `[A38][a38]` back to an inline link and delete its `[a38]: …` definition, so C1 and C3 cover it. The target path and entry title do not change. If the definitions are absent, this step does nothing and Results says so.

## Out of scope

- Changing `STATUS_WORD`, the C4 rule text in SD3 or the Completion rule.
- Validating reference-style link definitions in C1 or C3.
- Any spec content, rule or other Provenance entry.
- Moving A38 or any other plan.

## Validation

1. `node --test tools/check-docs.test.mjs` passes, including the new fixtures.
2. `node tools/check-docs.mjs` passes on the repository.
3. The disposable `--root` fixture from Discovery reports no C4 finding.
4. `git diff --check` is clean.

## Results

Implemented 2026-10-03 on branch `fix/sd4-provenance-link-targets`, from `master` @ `0a8f663`. No manual validation was planned.

### Checker

- `tools/check-docs.mjs`: `INLINE_LINK` and the new `LINK_TARGET` are built from one `LINK_TARGET_SOURCE`, so the link grammar is defined once. The C4 Provenance test runs on the line with each `](target)` replaced by `]`. C1, C3, C6 and the other C4 checks are unchanged. The header comment needed no change.

### Tests

- New test: a Provenance entry linking `plans/pending_manual_validation/`, inline and as an angle-bracket target with a title, gives no finding.
- New failing case in the existing C4 Provenance test: `[A38](…/pending_manual_validation/…) (pendente)` still fails.
- Against the `0a8f663` checker the new passing test fails (12/13), proving that it covers the defect.

### Step 3

Not applied. `git grep "^\[a38\]:" origin/master -- docs` finds nothing at `0a8f663`: the A38 reference-style links are not on `master`. Whoever later delivers A38 can use inline links.

### Validation

- `node --test tools/check-docs.test.mjs`: 13/13.
- `node tools/check-docs.mjs`: pass (36 live files, 39 link-checked).
- The Discovery fixture (`--root`) no longer reports C4. Its only finding is C7, because it has no `docs/qa/README.md`.
- `node tools/check-skills.mjs`: pass. `node --test tools/check-skills.test.mjs`: 14/14.
- `git diff --check`: clean.
