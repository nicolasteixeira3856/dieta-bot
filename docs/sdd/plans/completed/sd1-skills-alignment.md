# Plan — SD1 Repository skill alignment

- State: `Concluído`
- Date: 2026-09-29
- Owner: `sdd` — shared agent workflow maintenance.
- Production code affected: none.
- Prerequisites: the current constitution, context specifications and accepted decisions listed below; an isolated checkout if another chat is using the repository.

## Authorization gate

The owner approved this delivery by identifying the original plan path below. The completed plan now lives under completed/; creating or reading a plan alone does not authorize rewriting skills, changing repository policy, or implementing the validation tool.

Implementation requires explicit approval identifying this file:

> Aprovo o plano `docs/sdd/plans/sd1-skills-alignment.md`. Implemente o plano aprovado.

One agent executes this maintenance delivery. Client and server source remain outside its scope. If resolving a conflict would require a new product or architecture decision, record the unresolved item and return to Planning instead of changing behavior.

## Objective

Make the repository's skills guide agents toward the current Dieta Bot workflow, remove obsolete instructions, and detect divergence between the four required skill copies before delivery. Preserve skill names and useful Google resources. Keep maintained local instructions in English and product copy in pt-BR.

## Sources of truth

Follow the precedence in [SDD](../../README.md#precedência): latest explicit owner decision, [AGENTS.md](../../../../AGENTS.md), live context specification, applicable accepted ADR, approved plan, current implementation. Skills provide execution guidance; they do not supersede these sources or authorize additional work.

- [Documentation matrix](../../../README.md), [Android context](../../../android/README.md), [product context](../../../produto/README.md), [server context](../../../server/README.md).
- [ADR-012: current screen structure](../../../produto/adrs/ADR-012-chat-home-perfil.md), [Chat specification](../../../produto/specifications/chat.md), [Home specification](../../../produto/specifications/home-timeline.md).
- [Room specification](../../../android/specifications/room-v2.md) and current [Gradle configuration](../../../../apps/android/app/build.gradle.kts). Historical [ADR-010](../../../decisions/010-room.md) remains immutable; its original Java/kapt implementation is not a mandate to restore that implementation.
- [ADR-014: flavors and dev telemetry](../../../android/adrs/ADR-014-flavors-firebase-dev.md), [ADR-018: photo processing](../../../android/adrs/ADR-018-foto-2048.md), [ADR-022: Chat text limit](../../../produto/adrs/ADR-022-limite-texto-chat.md).
- [HTTP contract](../../../api-contract.md), [server Chat specification](../../../server/specifications/v1-chat.md), [client timeouts](../../../../apps/android/app/src/main/java/com/nutri/android/core/network/NetworkModule.kt).
- [Tokens](../../../tokens.md), [QA inventory](../../../qa/README.md), [Stitch workflow and exact screen titles](../../../stitch/README.md).
- Current [Roborazzi tests](../../../../apps/android/app/src/test/java/com/nutri/android/ui/RoborazziSmokeTest.kt) and [Stitch comparison tests](../../../../apps/android/app/src/test/java/com/nutri/android/ui/StitchGoldTest.kt).
- Official Google skill sources and the installed Android CLI's `--help` output when refreshing CLI instructions. Record the upstream revision and local adaptations in the implementation evidence.

Some live documents still contain historical paragraphs or inconsistent summaries. For example, the HTTP contract's introductory text says 1000 characters for every route, while its Chat section and ADR-022 specify 2000 for Chat. Resolve skill guidance using the precedence above and record discrepancies; this plan does not authorize a broad rewrite of product specifications or accepted ADRs.

## Discovery evidence

The repository contains 26 skills per required root and 136 files per root at discovery. Fourteen skills are maintained locally: seven project workflows and seven engineering skills. Twelve are Google skills with supporting resources.

The earlier synchronization audit found the same file inventory in all four roots. Only `dieta-bot-stitch/SKILL.md` differed by bytes: LF in `.agents`/`.hermes`, CRLF in `.grok`/`.claude`. Its text was identical. Repeat the inventory at implementation start; another chat may have updated it since discovery.

Android CLI 1.0.16457483 was tested in this chat: device layout JSON and PNG capture succeeded on `emulator-5554`. The executable was found through the persisted user PATH at `C:\Users\Nicolas\AppData\AndroidCLI\android.exe`, although this chat's process PATH did not resolve `android`. This is environment evidence, not a portable hardcoded install path.

| Priority | Skill or area | Verified problem | Planned correction |
| --- | --- | --- | --- |
| P1 | `dieta-bot-android-feature` | Requires DataStore/JSON, explicitly rejects Room, and specifies 1280 px / JPEG q70. | Use Room for structured profile/day/meal state; apply ADR-018: longest side at most 2048 px, JPEG q85, rotation applied, EXIF stripped, 16 MB guard. Reference current photo and persistence workflows instead of freezing old examples. |
| P1 | `dieta-bot-android-decision` | Lists T0–T3, sends new ADRs to `docs/decisions/`, and mandates refusal even when the owner explicitly changes a decision. | Use current screen/spec/ADR references, new ADRs under their owning context, and SDD precedence. Preserve accepted ADR history and explicit approval of the named implementation plan. Remove unrelated mandatory A/B/C and three-line formats unless the actual task calls for alternatives. |
| P1 | `dieta-bot-android-memory` | Writes every new decision to the historical ADR directory. | Route durable architectural decisions to `docs/<context>/adrs/`; link their live specifications and indices. Do not edit accepted historical ADRs or treat the skill as authorization to implement. |
| P1 | `screenshot-testing` | Starts with recording and shows `captureRoboImage` targeting `docs/qa/stitch/`; claims a fixed 0.5% tolerance and roughly three-second execution. | Verify existing baselines first. Roborazzi baselines belong to `apps/android/app/src/test/snapshots/`; Stitch PNGs are read-only inputs to app comparisons. Baseline recording must be deliberate and reviewed. Describe the actual tests and thresholds; remove unsupported timing claims. |
| P1 | `material3-expressive` | Forbids FAB, uses T1/T3 instructions, specifies only dark CTA colors, and checks `releaseCompileClasspath` without the dev flavor. | Preserve the Home Chat FAB and current screens; route theme tokens through both palettes; use dev variant configurations and tasks; preserve stable BOM plus the approved Material 3 override. |
| P1 | `dieta-bot-android-qa`, `android-architecture`, feature workflow | Uses generic `test` / `compileDebugKotlin` and treats an emulator capture as an optional fallback to JVM screenshots. | Use `assembleDevRelease`, `testDevDebugUnitTest`, `verifyRoborazziDevDebug`; retain fresh emulator captures in both themes, matching golds and a written diff for changed UI. Separate automated success from manual validation still pending. |
| P1 | `kotlin-clean`, feature workflow | Treats all network timeouts as 20 s and turns a network failure into a low-confidence portion question. | Reference the current timeout boundaries: connect 20 s, read/write 60 s, call 65 s. Preserve the Chat retry/failure behavior and distinguish network errors from estimate confidence. Do not change networking code. |
| P1 | `fastapi-security` | Gives `ChatIn.text` a 1000-character example and generic body-limit guidance that can be mistaken for the current contract. | Keep 2000 code points for Chat text/history, 1000 for estimate/fit, 24 MB JSON body and 16 MB photo guards, with references to the owning contract/specification. Do not add endpoints or claim an absent `Content-Length` header proves body size safety. |
| P2 | `android-architecture`, `room-ksp-coroutines` | “Zero kapt” is stated globally although Hilt still uses kapt and Room uses KSP; the illustrative profile entity includes daily workout state. | Scope the Kotlin/KSP rule to Room; retain the existing Hilt pipeline; distinguish profile state from date-keyed workout state. Avoid introducing a schema, database-version change, or navigation migration through instructions. |
| P2 | `compose-stability`, UI workflow | Suggests annotations/read-only wrappers as a general guarantee of immutability and compiler-report locations/options without verification. | Require the annotation's promise to match the underlying data; preserve existing dependencies. Verify report commands against the current build before documenting them. Do not prescribe a new library or annotate mutable state to silence warnings. |
| P2 | `dieta-bot-android-ui`, `dieta-bot-android-visual` | Blanket “no chip on day 1” rules can conflict with current Chat suggestion chips; capture guidance covers only dark. | Use the matching Home/Chat specification and gold for day-one state; preserve numbers-first copy, both themes, semantic macro colors, splash rules and required diff categories. |
| P2 | `dieta-bot-stitch` | Requires a host-specific `SendUserFile` tool and recurring live edit probes whenever another model reads the skill. | Provide the report through available local file links/previews; use the API/scripts when direct MCP is unavailable. Keep owner-run gate prompts, exact Stitch titles and failure stops. Limit live edits to explicit owner authorization; do not rerun mutation probes during routine reading. |
| P2 | Google `android-cli` references | `SKILL.md` marks `layout --diff` as a deprecated no-op, but `references/interact.md` promises incremental differences. | Refresh the CLI entrypoint/relevant interaction references from the official source and correct any remaining contradiction as a documented local adaptation. Use current JSON inspection options and live `--help`; handle executable discovery and Windows capabilities without machine-specific paths. |
| P2 | Google skill applicability | `adaptive`, `styles`, `navigation-3`, `testing-setup` can prompt unplanned navigation, dependency or test-stack changes; `play-policy-insights` mandates delegation when available. | Keep the official skills. Add narrowly scoped repository guidance to AGENTS so generic examples do not override golds, stack pins, plan approval or the one-agent rule. For Play-policy work, use its documented sequential path under the constitution. No unrelated upstream tutorial rewrite. |

## Implementation scope

### 1. Establish a safe inventory

1. Re-read current AGENTS, SDD and relevant source references after approval.
2. Inspect active checkouts and local changes. If another chat is active, use an isolated worktree from up-to-date `master`; do not switch its branch, alter its emulator state, or include its files in this delivery.
3. Inventory all skill names, relative supporting-file paths, frontmatter, hashes and newline differences in all four roots. Inspect skill symlinks before writing; do not follow a link into a global/user skill directory.
4. Keep a before/after audit table in this plan with each corrected instruction and its authoritative source. Treat unresolved product/document conflicts as explicit findings, not silent policy changes.

### 2. Correct the fourteen local skills

Use `.agents/skills` as the editing source for this delivery, then mirror the finished files. This is an execution convention, not a change to discovery or a new global policy.

- Rewrite only the necessary instructions and frontmatter descriptions in English. Preserve English source identifiers and pt-BR product copy in examples.
- Link to live specifications and workflow documents instead of repeating long screen lists, schemas, token tables, release procedures or changing library versions.
- Retain useful local constraints, including one agent, a named approved plan, scope boundaries, no secrets in APK/logs, dev variants, Room, both themes and required Stitch comparison.
- Remove obsolete behavior and unsupported universal requirements. Skill guidance must not reopen a frozen product decision or imply that merely triggering a skill grants approval.
- Preserve all fourteen skill names and applicable optional metadata.

### 3. Integrate the Google skills without changing the app

- Preserve the twelve installed Google skill names, licenses, author metadata, scripts and useful reference files.
- Add a concise applicability note in the existing AGENTS Skills section: official recipes apply within the approved project scope; they do not authorize stack/SDK upgrades, new screens, extra navigation, new test frameworks, global skill installation or additional agents.
- Limit substantive Google skill edits in this delivery to `android-cli/SKILL.md` and its interaction/journey references where a verified current command or behavior differs. Record the source revision and local deviations; do not blindly copy machine-global skills into the repository.
- Document portable executable lookup: current PATH, persisted Windows user PATH/install location, then a full-path invocation. Do not hardcode Nicolas's paths or install/reinitialize the CLI merely because PATH is stale.
- Treat screenshots/layout as observation. Navigating, changing system theme, installing instrumentation or APKs and resetting app data are distinct operations; coordinate any device checks with active chats and use an idle/test device for this delivery.
- Keep clean screenshots for gold comparison; annotated diagnostic screenshots stay in scratch output. Do not describe `--diff` as incremental when it is a deprecated no-op.

### 4. Synchronize and prevent drift

Add a small dependency-free Node tool, `tools/check-skills.mjs`, with a documented `--root <path>` option for isolated fixture checks.

It must:

- Compare the names and complete relative file inventory of `.agents/skills`, `.grok/skills`, `.hermes/skills`, `.claude/skills`.
- Compare file bytes and report missing, extra and changed files with a nonzero exit code. Do not silently ignore newline differences, caches or unexpected files.
- Require a `SKILL.md` for each skill directory; flag retired skill names listed in AGENTS.
- Check existing relative Markdown file references within skills, distinguishing links from code examples and runtime paths. An external URL, fragment or template placeholder is not a local missing-file error.
- Stay read-only: no synchronization, deletion, normalization, downloads or regeneration as a side effect. This checker does not claim to validate YAML semantics, product correctness or remote links.

Add LF checkout rules for Markdown under these four roots in `.gitattributes`. Apply the same finished files to all four roots and normalize only their Markdown files as needed. Preserve unrelated attribute rules and binary assets. Inspect existing divergences before choosing a version; `.agents` is not automatically the winner for concurrent edits.

Validate frontmatter separately using the available skill-creator validator or an equivalent YAML-aware check. Its path is environment-dependent and must not become a hardcoded repository dependency.

### 5. Record evidence and deliver

- Record the final inventory, corrected findings, preserved upstream resources, unresolved out-of-scope document issues and the real validation results in this plan.
- Update its lifecycle and the SDD/global indices in the same delivery. A skill-only delivery has no new UI or tester-APK validation requirement.
- Follow the existing git delivery workflow: branch from up-to-date `master`, scoped commits, push, PR to `master`, green required checks, merge commit, delete branch, return the delivery checkout to updated `master`. Attach the PR to this chat. If working in an isolated worktree, leave the other chat's checkout untouched.
- Red CI or conflicts stop delivery and must be reported. Do not use this maintenance plan to distribute an APK or deploy the server.

## Files and areas affected after approval

- Four skill roots, limited to the fourteen local `SKILL.md` files per root and verified CLI entrypoint/reference updates described above.
- `AGENTS.md`: the existing Skills section's applicability guidance only; no product, formula, screen, telemetry or delivery-policy changes.
- `.gitattributes`: LF rules for Markdown in the four skill roots only.
- `tools/check-skills.mjs` and `tools/check-skills.test.mjs`.
- This plan, its eventual lifecycle path, [SDD plan index](../README.md), [SDD README](../../README.md) and [global index](../../../README.md).
- Other skill Markdown files may change line endings only when necessary for byte-identical copies. Supporting resources otherwise remain unchanged except the specified CLI references.

## Planned validation

1. **Inventory and synchronization:** run `node tools/check-skills.mjs`; all 26 skills and every supporting file must match in all four roots. No retired names or broken concrete local references.
2. **Checker behavior:** run `node --test tools/check-skills.test.mjs`. In disposable fixture roots, verify identical trees pass and changed content, CRLF/LF mismatch, extra/missing skill or supporting file, broken concrete references and retired names fail. External links, anchors and code examples must not produce false missing-file errors. Verify checker failure leaves fixture bytes unchanged.
3. **Skill format:** validate required YAML metadata and naming for the fourteen rewritten skills and refreshed CLI skill; preserve Google metadata and licenses. Confirm maintained local prose is English and product examples remain pt-BR.
4. **Workflow review:** use a written scenario matrix, with no production side effects. Cover: day-state persistence; photo upload; Home Chat FAB; both-theme visual QA; network failure versus confidence question; Chat 2000 versus other-route 1000; a new ADR; an owner-requested decision change; named-plan approval; Stitch gate/report with unavailable MCP; CLI layout without `--diff`; a generic Google navigation/dependency suggestion; one-agent Play-policy execution; and a test-build request routed to the release script with human notes. Record expected versus guided actions; do not approve based only on string searches or successful frontmatter parsing.
5. **Readonly gold inventory:** `node tools/check-stitch.mjs`, plus before/after hashes proving no Stitch gold, Roborazzi baseline or emulator capture was changed.
6. **Scope and links:** `git diff --check`; confirm documentation links and lifecycle paths. Review the diff allowlist. No changes under `apps/android/` or `server/`, global skill directories, secret/config files or deployment scripts.
7. **CLI commands:** compare updated guidance with installed `layout --help` and `screen capture --help`. An actual device capture is optional and only on a coordinated idle device; the successful access check in this chat does not authorize interrupting another agent's test. Store any diagnostics outside QA gold/current folders.

Full Android builds, Android regression suites, server tests and live Stitch edit probes are not required for this instructional maintenance delivery because their implementations and assets remain unchanged. If an unexpected production change becomes necessary, it needs a separate approved plan.

## Out of scope

- App/server behavior, source, tests, dependencies, database schemas or library/SDK upgrades.
- New or changed screens, gold generation, baseline re-recording or relaxed visual thresholds.
- Accepted ADR edits, a broad repair of older documentation, new product decisions or additional skill names.
- Uninstalling Google skills, altering user-global Codex/Agents skills, or changing skill discovery priorities outside this repository.
- App Distribution, release version increments, server deployment, external messages and additional agents.
- Scheduled maintenance/automations or a new CI pipeline. The checker is a local delivery command in this scope; CI wiring can be proposed separately.

## Risks and controls

- **Instruction drift:** keep dynamic details at their owning specification and record conflicts before resolving guidance.
- **Reference assets overwritten:** distinguish Roborazzi baselines from imported Stitch golds, default to verification and enforce unchanged hashes.
- **Concurrent chat changes:** isolate writes, re-read current files before applying changes, and preserve unrelated work.
- **Upstream divergence:** preserve official resources and log the narrowly scoped CLI refresh/adaptation instead of modifying all Google recipes.
- **False confidence:** byte equality and metadata validation prove structure only; the scenario review checks whether the instructions lead to the correct workflow.
- **Host-specific tooling:** use capability discovery and normal file links rather than requiring a tool that another agent host may not expose.

## Acceptance criteria

- Every verified obsolete instruction in the discovery table is corrected or explicitly deferred with a reason and source conflict.
- All 26 skill sets and complete supporting-file inventories are byte-identical across the four roots.
- Local instructions are concise, in English, and subordinate to current owner decisions, constitution, specifications and approval gates.
- Room, photos, screen/navigation rules, both themes, dev tasks, ADR paths, route-specific limits and failure handling match their current authoritative sources.
- No guidance records app images over imported Stitch golds or claims JVM checks alone finish changed UI.
- Google resources remain available; generic migrations and delegation do not override the project's scope and one-agent rule.
- CLI guidance matches tested capabilities and does not promise `--diff` behavior absent from the installed version.
- Structural checks, meaningful checker fixture tests and the workflow scenario review pass with evidence.
- Only approved maintenance files are delivered through the git workflow; app/server/global skills/secrets/visual assets remain unchanged.

## Execution record

Planning performed on 2026-09-29: constitution/SDD, current local skill instructions, selected Google instructions and CLI references, photo/text ADRs, specifications, client timeout configuration and actual screenshot test destinations inspected. No implementation was performed before explicit approval.

Implementation authorized by the owner with the exact statement in the authorization gate. Executed on 2026-09-29 in an isolated managed worktree from master dc2eeda54e79cb6dc7e5a73411a73e60a55b470b (A22/A26 already included), on branch codex/sd1-skills-alignment. The other checkout and its device state were preserved.

### Inventory and source audit

The fresh inventory found 26 skills / 136 files per root. Fourteen files in the existing mirrors differed only by line endings; no concurrent semantic differences were selected away. The .agents and .hermes trees were ignored and absent from a fresh Git checkout. They are explicitly included in this scoped delivery so a fresh checkout has all four mirrors; unrelated ignore rules remain unchanged.

Fourteen local SKILL.md files were rewritten in English. The twelve Google skill names and their metadata/licenses/resources remain available. The CLI entrypoint and two references were refreshed from [the official pinned source](https://github.com/android/skills/tree/42dc2270e96032bd860bb94511e440aa00a43125/devtools/android-cli), revision 42dc2270e96032bd860bb94511e440aa00a43125. Local CLI adaptations: portable executable discovery without reinstall/init for stale PATH; explicit device coordination; current tree/flat/full layout semantics; deprecated no-op --diff; --screenshot for screen resolve; UTF-8 layout files; clean versus annotated PNGs; shell-specific input and correctly fenced examples; approved idle-device journeys and measured splash behavior.

The 119 other files per tree were checked against the pre-change checkout: 105 Markdown files preserve content apart from LF normalization, and 14 non-Markdown resources preserve original bytes. No global/user skill copy was edited. AGENTS received only applicability and checker guidance in its Skills section. LF attributes apply only to Markdown within the four skill roots.

| Corrected skill | Previous guidance replaced | Guided behavior and authority reviewed |
| --- | --- | --- |
| android-architecture | Global zero-kapt / old persistence/navigation examples | Room Kotlin/KSP, existing Hilt kapt, pure domain/UDF and typed current routes; AGENTS, Android index, delivered A0 and Gradle. |
| compose-stability | Annotation/wrapper guarantees and unverified report command | Honest nested immutability, existing dependencies and verified compiler configuration before report claims; actual Gradle has no report destination. |
| dieta-bot-android-decision | T0–T3, historical ADR destination, mandatory refusal/formats | Current specs/successors, SDD owner precedence, named-plan approval and context ADRs. |
| dieta-bot-android-feature | DataStore/no Room, 1280/q70, generic tasks/errors | Room, ADR-018 2048/q85/rotation/EXIF/guard, current timeouts/retry, dev checks and device QA. |
| dieta-bot-android-memory | All new ADRs under historical decisions | Owning live specification and context ADR successor; accepted history unchanged, no implied implementation approval. |
| dieta-bot-android-qa | Generic/unflavored tests, optional emulator validation | Dev tasks, fresh dark/light captures and written gold diff, manual-pending distinction and notes-based distribution script. |
| dieta-bot-android-ui | Blanket day-one chip ban / stale screens | Current Home/Chat states and FAB, gold/spec-specific shapes and both palettes; Chat spec explicitly retains suggestion chips. |
| dieta-bot-android-visual | Dark-only/incomplete capture guidance | A26-compatible CLI bounds/UTF-8, coordinated device, both themes, capture offsets and unchanged 2% region/report-only behavior. |
| dieta-bot-stitch | Host-specific report tool and routine mutation probes | Available file link/preview, existing scripts/API, exact titles, owner gate and failure stop; only authorized bounded live edits. |
| fastapi-security | Chat 1000 and misleading generic body-limit example | Chat/history 2000 code points, estimate/fit 1000, current guards and honest header-limit caveat; ADR-022 and contract. |
| kotlin-clean | Universal 20 s and network error as confidence question | Connect 20 / read-write 60 / call 65 s, retry state distinct from estimate confidence; NetworkModule and Chat spec. |
| material3-expressive | No FAB, T1/T3, one CTA palette, unflavored dependency view | Home Chat FAB, current controls, both token palettes, stable BOM with approved override and devReleaseCompileClasspath. |
| room-ksp-coroutines | Daily workout in profile / processor and schema assumptions | Date-keyed day state, durable profile, Room KSP vs Hilt kapt and explicit non-destructive migrations; live schema/successors. |
| screenshot-testing | Record first, write renders over gold, fixed tolerance/time | Verify existing app snapshots first; separate imported golds/device captures; actual 0.01f Roborazzi and independent 2% Stitch metrics/report-only limits. |

### Workflow scenario review

This is a manual instruction-path review against the cited current sources, with no app/device/server side effects. “Pass” means the instructions guide the expected action; it does not claim that an Android feature was implemented or tested.

| Scenario | Expected action | Action guided by revised instructions | Result |
| --- | --- | --- | --- |
| Persist today's workout/meal state | Room, date keyed; profile survives rollover | feature + architecture + Room link current schema/successors and preserve migrations/transactions | Pass |
| Upload a photo | Rotate, <=2048 px, JPEG q85, strip EXIF, <=16 MB | feature cites ADR-018 and preserves guards; no client model key | Pass |
| Open Chat from Home | Preserve the Home FAB and approved route | UI + Expressive + architecture explicitly retain the FAB and current specs | Pass |
| Validate a changed screen | Fresh captures in both themes, matching golds, bounds and written diff | QA + visual require the complete workflow; JVM is insufficient; offsets/report-only limits retained | Pass |
| Network failure versus uncertain estimate | Retry error versus one confidence question | feature + Kotlin distinguish these paths and current timeout boundaries | Pass |
| Validate Chat and estimate text | Chat/history 2000 code points; estimate/fit 1000 | FastAPI cites ADR-022 and documents stale introductory summary | Pass |
| Record a new architectural decision | Context ADR and live spec/index; accepted history immutable | memory + decision require successor/path ownership and current unused ID | Pass |
| Owner explicitly changes an old decision | Apply SDD precedence; document successor during Planning | decision allows explicit owner change rather than mandatory refusal | Pass |
| Start implementation from a proposal | Wait for approval naming the plan | feature + decision + architecture preserve the documentary Planning gate | Pass |
| Verify a Stitch gate without direct MCP | Existing API/scripts, available report link, exact titles; stop on failure | Stitch skill gives this path and blocks dependent app work until completion | Pass |
| Inspect layout with current CLI | Fresh tree/flat/full JSON; --diff cannot provide deltas | CLI + interact + visual use tested flags, UTF-8 and coordinated observation | Pass |
| Apply a generic Navigation 3/dependency recipe | Preserve approved scope/stack/golds; no automatic migration | AGENTS applicability + architecture/Expressive prevent an unplanned change | Pass |
| Run a Play-policy audit when delegation tools exist | One agent performs the documented sequential mode | AGENTS overrides generic Mode A with repository sequential execution | Pass |
| Owner requests a phone test APK | distribute-dev.ps1 with human pt-BR -Notes; script controls version/tag | QA points to distribution guidance and prohibits raw log/hand-edited versions | Pass |

Additional review: read-only Kotlin collections are not assumed immutable; no unsupported compiler-report property is promised. Roborazzi records only within approved baseline scope, never to imported golds. Gold height differences are handled by the current QA tool rather than a claim that every exported gold is 780x1688.

### Validation evidence

- node tools/check-skills.mjs: pass, 26 skills / 136 files in each of four roots, complete inventories/bytes match, no retired names or broken concrete relative Markdown references.
- Full fresh staged checkout (1142 tracked files exported through Git checkout rules): all four roots pass the same checker with zero findings. This proves the two formerly ignored mirrors and LF rules survive checkout, rather than relying on machine-local files.
- node --test tools/check-skills.test.mjs: 14 tests pass, zero failures. Disposable fixtures cover byte corruption, CRLF/LF, missing root/skill/file/entrypoint, extra skill/cache, retired exact/glob names, broken inline/reference links, valid parent/image/encoded-space/angle links, external/template/runtime/code exclusions, and CLI options. Passing and failing CLI/API checks preserve fixture bytes.
- YAML-aware skill-creator quick_validate.py: all 26 canonical skills pass; all mirrors are byte-identical. The validator is an environment tool, not a new repository dependency.
- node tools/check-stitch.mjs: all 48 gold PNGs pass. First attempt lacked pngjs in this fresh worktree; npm ci --prefix tools --ignore-scripts --no-audit --no-fund restored existing locked dependencies, then inventory passed. No dependency/lockfile edit or browser download.
- Before/after SHA-256 audit: all 111 files under Stitch golds, current Android captures and Roborazzi snapshots retain the same paths/bytes. Sorted visual inventory digest: 6e83c0b44fdfa5bb8002cdda67a0b3faf24e20f9ac6875212529c01fe5732d37.
- Installed CLI 1.0.16457483 help inspected for layout, screen capture and screen resolve. Confirms tree default, --flat/--full/--output, deprecated no-op --diff and required --screenshot. No active device call or state change during this implementation.
- Documentation/link/scope review and whitespace checks for substantive changes: pass before delivery. The full git diff --cached --check also reports inherited upstream whitespace in previously ignored Google resources newly tracked in .agents/.hermes. Their existing .grok counterparts contain the same lines; those resources were deliberately preserved instead of rewritten outside scope. CLI help-excerpt trailing spaces were cleaned within its approved refresh. The only production paths referenced are read-only evidence; no app/server/test/asset/deploy/version/config/global-skill edits.
- Full Android/server suites, APK distribution and live Stitch mutation probes were not run: this delivery changes instructions/tooling only and requires no manual device validation.

### Preserved out-of-scope inconsistencies

The contract intro has the historical all-route 1000 summary; its current Chat section and ADR-022 govern 2000. AGENTS' short server-route list omits the accepted Chat route, already present in its live contract/specification. ADR-010 and parts of the Room-v2 document describe older implementations; the delivered Kotlin/KSP refactor and accepted schema/consolidation successors remain authoritative. Some Stitch gold comparisons intentionally report known conflicting regions without assertions. This delivery records these issues and preserves their documents/tests/assets rather than treating instructional maintenance as approval to change product behavior.

### Delivery

Local acceptance checks completed. Delivery follows the scoped PR workflow; the delivery PR and final repository verification are recorded by this chat.
