# Plan — A48 Fibrai technical identity: `app.fibrai.android` everywhere and Firebase `fibrai-dev`

- Status: Concluído
- Date: 05/10/2026
- Owning context: `android`
- Executable boundary:
  - `apps/android/` (Gradle, manifests, every Kotlin source and test set, Room schema folder, Roborazzi baselines, `src/dev/google-services.json` which is gitignored);
  - the Android tooling in `tools/`: `distribute-dev.ps1`, `capture-*.sh`, `brand-icons.ps1` if it names the package;
  - the Firebase console/CLI setup below. No `server/` change.
- Related documentation:
  - [ADR-036](../../adrs/ADR-036-fibrai-technical-identity.md);
  - the [android README](../../README.md) and the [Room specification](../../specifications/room-v2.md) (DB file name, schema path);
  - `AGENTS.md` (Product name list of technical IDs, Live stack, `debug.fibrai.hide_dev_tools`, Firebase) at Completion;
  - `docs/content-policy/specifications/identity-and-audit.md` if it names the app id.
- Prerequisites:
  - approval of this plan accepts ADR-036;
  - no other Android plan in implementation while this one runs, because the package move rewrites every file. [A47](../pending_manual_validation/a47-chat-meal-updates.md) is either delivered first or rebased after this plan.

  No dependency on D10, A49 or S20.

Authorization and delivery follow [SDD](../../../sdd/README.md). Approval: `Aprovo o plano docs/android/plans/a48-fibrai-app-id-firebase.md. Implemente o plano aprovado.`

## Objective

The app code has no legacy `nutri` or `DietaBot` identifier. The dev app ships as `app.fibrai.android.dev`, registered in a new Firebase project with Crashlytics, Analytics and App Distribution for the current testers. Visible copy does not change here ([A49](../a49-fibrai-tali-visible-rename.md)).

## Scope

### 1. Firebase project (owner-assisted)

The owner's Google account owns the project. Accepting Firebase/Google terms and linking Google Analytics are **owner steps in the console**. The agent asks before each one and never accepts terms on the owner's behalf.

1. Create the project `fibrai-dev` ("Fibrai Dev", Spark) with Google Analytics. If the ID is taken, use the suggested suffix and record it.
2. Register the Android app `app.fibrai.android.dev`, nickname "Fibrai Dev". Add the SHA-1 and SHA-256 of the release keystore and the debug key. The agent may use the Firebase MCP: `firebase_create_app`, `firebase_create_android_sha`, `firebase_get_sdk_config`.
3. Put `google-services.json` in `apps/android/app/src/dev/`. It stays gitignored, and its contents are never printed.
4. **App Distribution:**
   - Create the group `testers` in the new project.
   - Read the current members of `testers` in `nutri-bot-dev` with the Firebase CLI and add the same members.
   - Tester emails are never written into the repository, plans or logs.
5. Crashlytics turns on with the first crash upload. Analytics needs no code change.

### 2. Package and identifier move (mechanical, no behavior change)

1. **Gradle:**
   - `namespace = "app.fibrai.android"`;
   - `defaultConfig.applicationId = "app.fibrai.android"`;
   - dev keeps `applicationIdSuffix = ".dev"`.
2. **Packages:**
   - Move every source set from `java/com/nutri/android/` to `java/app/fibrai/android/`: main, dev, prod, debug, test, testDev and androidTest if present.
   - Rewrite every `package` and `import`.
   - Rewrite fully qualified names in KDoc links and code (`com.nutri.android.domain.…`).
   - Rewrite manifest component names and R8/ProGuard rules.
3. **Class names** (`DietaBot*` → `Fibrai*`), with every reference updated:
   - `DietaBotApplication`, `DietaBotTheme`, `DietaBotDatabase`, `DietaBotConverters`, `DietaBotTokens`;
   - the XML themes `Theme.DietaBot*` in `values*/themes.xml` and the manifest.
4. **Storage and system names.** No data to keep: the new app id is a fresh install.
   - Room: `"nutri.db"` → `"fibrai.db"`.
   - Move `app/schemas/com.nutri.android.core.database.DietaBotDatabase/` to the folder Room generates for `app.fibrai.android.core.database.FibraiDatabase`, so the history JSONs stay byte-identical. Update the `SCHEMA_V*` paths in the migration tests.
   - Prefs and DataStore: `nutri_push`, `nutri_day` → `fibrai_push`, `fibrai_day`.
   - Keystore alias: `nutri_memory_v2` → `fibrai_memory_v2`.
   - Intent: actions `com.nutri.android.push.*` → `app.fibrai.android.push.*`; extras `nutri_tela`, `nutri_open`, `nutri_slot` → `fibrai_*`; the dev test-crash action → `app.fibrai.android.dev.TEST_CRASH`.
   - Dev system property: `debug.nutri.hide_dev_tools` → `debug.fibrai.hide_dev_tools`.
5. **Telemetry screen ids:**
   - `screenName()` maps route class names. It is updated for the new package, so `o1`, `o4`, `chat`, `cfg` stay identical.
   - The ADR-012 event values sent to Analytics do not change.
6. **Keystore file:**
   - The owner renames `nutri-release.jks` to `fibrai-release.jks`. Same key, same alias.
   - Gradle and local properties point to the new name, and secrets are never printed.
   - The agent asks the owner to do the local rename and confirms the build signs with the same SHA as before.

### 3. Tooling

1. `tools/distribute-dev.ps1`:
   - project and app = the new Firebase IDs;
   - the console URL uses `app.fibrai.android.dev`.

   The version bump, tag format and `-Notes` rules are unchanged.
2. `tools/capture-*.sh`: package and component names (`app.fibrai.android.dev/app.fibrai.android.MainActivity`), extras and the hide-dev-tools property.
3. Roborazzi:
   - baselines whose file names include the old package or class names are renamed;
   - pixels must stay identical, because this plan does not change visuals.

### 4. Tester notice

The first release notes after this plan say, in pt-BR, that this is a new app:
- Install it from Firebase App Tester.
- Uninstall the old "Dieta Bot Dev".
- Onboarding starts over, and old local meals and memory do not carry over.

They are written at distribution time (A16).

## Intended documentation changes at Completion

- `AGENTS.md`:
  - The technical-ID sentence becomes: app identifiers are `app.fibrai.android`, and `nutri` remains only in server/VM/Stitch IDs, citing ADR-036.
  - Flavors: `app.fibrai.android.dev` / `app.fibrai.android`.
  - Telemetry dev: Firebase `fibrai-dev`.
  - The hide-dev-tools property name.
- [Room specification](../../specifications/room-v2.md): DB file `fibrai.db`, class `FibraiDatabase`, schema path.
- The [android README](../../README.md), the capture docs and any live spec that names the old package, id or project.

## Out of scope

- Visible strings, the avatar and the splash wordmark ([A49](../a49-fibrai-tali-visible-rename.md)).
- Server identifiers, the VM and the Stitch project.
- Prod build or distribution ([production gate](../../../content-policy/production-gate.md)).
- Deleting `nutri-bot-dev`: an owner action, after the testers move over.
- Migrating tester data.

## Validation

1. `./gradlew testDevDebugUnitTest verifyRoborazziDevDebug assembleDevRelease` passes. Roborazzi shows no pixel diff, and the migration tests read the moved schema JSONs.
2. **Legacy scan**, case-sensitive, over `apps/android` (excluding `build/`) and the Android scripts in `tools/`:

   ```text
   com\.nutri|com/nutri|DietaBot|nutri_|nutri\.db|debug\.nutri|nutri-release|nutri-bot-dev|Theme\.Nutri
   ```

   It returns nothing. The pt-BR words "nutricional" and "macronutrientes" are copy and stay.
3. `aapt2 dump badging` on the dev release APK shows `package: name='app.fibrai.android.dev'`, and `apksigner verify --print-certs` shows the same certificate SHA-256 as before the move.
4. Emulator:
   - The new APK installs next to the old app as a separate app and opens to splash → onboarding.
   - A slot reminder and its actions (skip/slot) work.
   - The dev tools row hides with `debug.fibrai.hide_dev_tools=1`.
   - The capture scripts run.
5. A forced test crash (the new action) and one Analytics event appear in the new project, checked through the Firebase MCP.
6. With the owner's go: `./tools/distribute-dev.ps1 -Notes <file>` reaches the new `testers` group. Results record the version and tag.
7. `node tools/check-docs.mjs` passes.

## Results

Approved by the owner on 2026-10-05 ("Aprovo o plano docs/android/plans/a48-fibrai-app-id-firebase.md. Implemente o plano aprovado."), which accepts ADR-036. Implemented on `feat/a48-fibrai-app-id` from master `ed6a6ba` (A47 already merged, so nothing was rebased).

### Firebase (`fibrai-dev`)

- The owner created the project in the console and linked Google Analytics. Verified through the Firebase API: project `fibrai-dev` (number `353075659895`) `ACTIVE`, Analytics property `557470003`. The ID needed no suffix.
- Android app `app.fibrai.android.dev`, display name "Fibrai Dev": `1:353075659895:android:05bc4f6ec8410711727272`. Registered through the Firebase Management REST API (gcloud token), so no `.firebaserc` was written into the repository.
- SHA-1 and SHA-256 of the release key (read from the signed APK, no password used) and of the local debug key: 4 certificates registered.
- `apps/android/app/src/dev/google-services.json` written straight from the API to the file. Its contents were never printed. The file is gitignored; the old one is kept only as a local backup outside the repository.
- App Distribution: group `testers` created in `fibrai-dev`. Its members were copied from `testers` of `nutri-bot-dev` by a script that prints only counts: 3 → 3, same set. No email is written anywhere.
- `nutri-bot-dev` is untouched (frozen per ADR-036).

### Code move

- `namespace` and `applicationId` = `app.fibrai.android` (dev `.dev`). Six source sets moved with `git mv` (main, dev, prod, debug, test, testDev; no androidTest). 179 files rewritten.
- Class and file names: `FibraiApplication`, `FibraiDatabase`, `FibraiConverters`, `FibraiTokens`, `FibraiClick`, `FibraiApi`, `Theme.Fibrai` and its four `themes.xml`. Log tags renamed to `FibraiMemory` and `FibraiInstallation`.
- Storage and system names: `fibrai.db`, `fibrai_push`, `fibrai_day`, keystore alias `fibrai_memory_v2`, actions `app.fibrai.android.push.*`, extras `fibrai_*`, `app.fibrai.android.dev.TEST_CRASH`, `debug.fibrai.hide_dev_tools`. Gradle root project `fibrai-android`.
- Schema history moved to `app/schemas/app.fibrai.android.core.database.FibraiDatabase/`, byte-identical (git rename at 100 %).
- `screenName()` maps by the route class name only, so the ADR-012 ids are unchanged.
- Tooling: `tools/distribute-dev.ps1` targets project `fibrai-dev`, the new app id and console URL. Seven `tools/capture-*.sh` scripts use the new package, component, DB file, extras, property and QA photo prefix. The `CN=Nutri` check stays: it is the certificate subject of the same key.
- Docs: AGENTS (technical-ID line, flavors, Firebase, property), android README, Room spec, the production gate's prod id, the code links of three skills in all four trees. ADR-014 and ADR-016 status lines record the partial replacement by ADR-036.

### Validation

1. `testDevDebugUnitTest` (after `clean`): **548 tests, 545 passed**. The 3 failures are the GoldTests `chatF_dark`, `chatF_light` and `chatL_dark` that already failed on master before this plan (D10 Tali golds, fixed by A49). The migration tests read the moved schema JSONs.
   - `verifyRoborazziDevDebug`: all 56 Roborazzi captures `unchanged`; the task reports the same 3 GoldTests.
   - `assembleDevRelease`: built.
2. Legacy scan (`com\.nutri|com/nutri|DietaBot|nutri_|nutri\.db|debug\.nutri|nutri-release|nutri-bot-dev|Theme\.Nutri`) over `apps/android` (excluding `build/` and the untracked `.idea`) and the Android scripts in `tools/`: **no match**.
3. `aapt2`: `package: name='app.fibrai.android.dev' versionCode='12' versionName='0.0.12-dev'`. `apksigner`: `CN=Nutri, O=Nicolas Teixeira, C=BR`, SHA-256 `c9978f23…7b2b8a1d`, the same certificate as before the move. The label is still "Dieta Bot Dev" (A49).
4. Emulator (`Medium_Phone`):
   - The dev debug APK installed next to `com.nutri.android.dev` as a separate app; the launcher resolves `app.fibrai.android.dev/app.fibrai.android.MainActivity`.
   - `capture-onboarding.sh`: splash → onboarding, keyboard/cursor checks and relaunch all ✓.
   - `capture-config.sh`: all checks ✓. `debug.fibrai.hide_dev_tools=1` hides "Memória da IA (dev)" and `0` shows it.
   - `capture-push.sh`: exact and inexact scheduling, notifications only for empty slots, Pular stores the skip and Registrar opens the Chat, all ✓ with the new actions.
   - The regenerated captures were discarded: this plan changes no visuals.
5. Release APK: the `app.fibrai.android.dev.TEST_CRASH` broadcast crashed the receiver ("crashlytics test"). On relaunch Crashlytics initialized for `app.fibrai.android.dev` and sent its batch; Analytics initialized and logged events for `app.fibrai.android.dev`. Checked through the Firebase MCP (Crashlytics `topIssues`): issue `d477b16f76278ed2982fe042cb9eb87c`, `TestCrashReceiver.onReceive`, `RuntimeException - crashlytics test`, FATAL, version `0.0.12-dev`, in `fibrai-dev`.
6. Distribution: not run. `tools/distribute-dev.ps1` requires a green `testDevDebugUnitTest`, and the 3 pre-existing GoldTests stay red until A49.
7. `node tools/check-docs.mjs` and `node tools/check-skills.mjs`: passed.

### Pending at implementation (closed below)

- **Owner, keystore:** rename `nutri-release.jks` to `fibrai-release.jks` at the repository root and set `storeFile` in `key.properties`. `*.jks` stays gitignored. After that, the agent confirms the signature SHA-256 is unchanged.
- **Owner, Analytics:** confirm the events in the `fibrai-dev` console (Realtime or DebugView). The local gcloud token has no Analytics scope.
- **Distribution (Validation 6):** with A49, through `distribute-dev.ps1` to the new `testers` group. The first notes say it is a new app: install "Fibrai Dev" from App Tester, uninstall "Dieta Bot Dev", onboarding starts over, old meals and memory stay in the old app.
- **Not changed, flagged:**
  - `docs/content-policy/legal/tester-notice.pt-BR.md` (version 1) names `com.nutri.android.dev`.
  - `docs/content-policy/operations/closed-test-data-map.md` D12 says retention is configured in `nutri-bot-dev`; the same retention must be set in `fibrai-dev`.
  - Both are content-policy documents. The notice is versioned legal text, so the owner decides the new notice version and the retention setting.

### Closure (2026-10-05)

- **Keystore:** the owner renamed the file to `fibrai-release.jks` and pointed `storeFile` at it. The key's only alias is still `nutri`, as ADR-036 keeps the same key and alias. The owner had set `keyAlias=fibrai`, which failed packaging, so the agent restored `keyAlias=nutri` in the gitignored `key.properties`. The release signs with SHA-256 `c9978f23…7b2b8a1d`, the same certificate as before.
- **Analytics:** confirmed by the owner in the `fibrai-dev` console.
- **Content documents (owner request):**
  - tester notice version 2: `app.fibrai.android.dev`, Firebase `fibrai-dev`, four testers, and the installation pseudonym that is active since dev 0.0.6;
  - the data map now names `fibrai.db` and `fibrai-dev`. D12 retention is still unknown.
  - Both are linked from [PR #128](https://github.com/nicolasteixeira3856/dieta-bot/pull/128).
- **App Distribution group:** at the owner's request the group `testers` is now displayed as "Testers". It holds exactly the four testers the owner listed (the three earlier members are among them; nobody removed). No email is written in the repository.
- **Validation 6, distribution, under an owner exception:** the owner explicitly authorized a one-time distribution without the `testDevDebugUnitTest` gate, because of the 3 known GoldTests (`chatF` ×2, `chatL` dark) that A49 fixes.
  - The agent ran a scratchpad copy of `tools/distribute-dev.ps1` whose only differences were the repository path and that gate. Every other step ran: version, `assembleDevRelease`, `CN=Nutri` and version checks, App Distribution with the notes, CHANGELOG, `chore(release)` commit, tag and push.
  - Result: **0.0.13-dev (versionCode 13)** to "Testers" in `fibrai-dev`, tag `dev-v0.0.13`.
  - The notes tell testers it is a new app: install it from App Tester, uninstall the old "Dieta Bot Dev", onboarding starts over and old data does not carry over.
- With these, the owner closed A48 ("já comece o A49"). The 3 GoldTests remain the known A49 debt.
