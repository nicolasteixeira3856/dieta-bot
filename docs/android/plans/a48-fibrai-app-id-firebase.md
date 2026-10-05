# Plan — A48 Fibrai technical identity: `app.fibrai.android` everywhere and Firebase `fibrai-dev`

- Status: Aguardando aprovação
- Date: 05/10/2026
- Owning context: `android`
- Executable boundary:
  - `apps/android/` (Gradle, manifests, every Kotlin source and test set, Room schema folder, Roborazzi baselines, `src/dev/google-services.json` which is gitignored);
  - the Android tooling in `tools/`: `distribute-dev.ps1`, `capture-*.sh`, `brand-icons.ps1` if it names the package;
  - the Firebase console/CLI setup below. No `server/` change.
- Related documentation:
  - [ADR-036](../adrs/ADR-036-fibrai-technical-identity.md);
  - the [android README](../README.md) and the [Room specification](../specifications/room-v2.md) (DB file name, schema path);
  - `AGENTS.md` (Product name list of technical IDs, Live stack, `debug.fibrai.hide_dev_tools`, Firebase) at Completion;
  - `docs/content-policy/specifications/identity-and-audit.md` if it names the app id.
- Prerequisites:
  - approval of this plan accepts ADR-036;
  - no other Android plan in implementation while this one runs, because the package move rewrites every file. [A47](pending_manual_validation/a47-chat-meal-updates.md) is either delivered first or rebased after this plan.

  No dependency on D10, A49 or S20.

Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/android/plans/a48-fibrai-app-id-firebase.md. Implemente o plano aprovado.`

## Objective

The app code has no legacy `nutri` or `DietaBot` identifier. The dev app ships as `app.fibrai.android.dev`, registered in a new Firebase project with Crashlytics, Analytics and App Distribution for the current testers. Visible copy does not change here ([A49](a49-fibrai-tali-visible-rename.md)).

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
- [Room specification](../specifications/room-v2.md): DB file `fibrai.db`, class `FibraiDatabase`, schema path.
- The [android README](../README.md), the capture docs and any live spec that names the old package, id or project.

## Out of scope

- Visible strings, the avatar and the splash wordmark ([A49](a49-fibrai-tali-visible-rename.md)).
- Server identifiers, the VM and the Stitch project.
- Prod build or distribution ([production gate](../../content-policy/production-gate.md)).
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

Planning only.
