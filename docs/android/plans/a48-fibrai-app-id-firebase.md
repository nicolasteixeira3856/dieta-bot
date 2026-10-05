# Plan — A48 applicationId `app.fibrai.android` and Firebase project `fibrai-dev`

- Status: Aguardando aprovação
- Date: 05/10/2026
- Owning context: `android`
- Executable boundary:
  - `apps/android/`: `app/build.gradle.kts` applicationId, `src/dev/google-services.json` (gitignored), and code that hard-codes the package;
  - `tools/distribute-dev.ps1`: Firebase project, app and console URL.
  - The Firebase console/CLI setup below.
  - No `server/` change.
- Related documentation: [ADR-036](../adrs/ADR-036-fibrai-technical-identity.md); the [android README](../README.md); `AGENTS.md` "Live stack" and "Do not" Firebase lines at Completion; `docs/content-policy/specifications/identity-and-audit.md` if it names the app id.
- Prerequisites: approval of this plan accepts ADR-036. No dependency on D10, A49 or S20, so it can run first.

Authorization and delivery follow [SDD](../../sdd/README.md). Approval: `Aprovo o plano docs/android/plans/a48-fibrai-app-id-firebase.md. Implemente o plano aprovado.`

## Objective

Ship the dev app as `app.fibrai.android.dev`, registered in a new Firebase project with Crashlytics, Analytics and App Distribution for the current testers. The visible name does not change here ([A49](a49-fibrai-tali-visible-rename.md)).

## Scope

### 1. Firebase project (owner-assisted)

The owner's Google account owns the project. Accepting Firebase/Google terms and linking Google Analytics are **owner steps in the console**. The agent asks before each one and never accepts terms on the owner's behalf.

1. Create the project `fibrai-dev` ("Fibrai Dev", Spark) with Google Analytics enabled. If the ID is taken, use the suggested suffix and record it.
2. Register the Android app `app.fibrai.android.dev`, nickname "Fibrai Dev". Add the SHA-1 and SHA-256 of `nutri-release.jks` and the debug key. The agent may use the Firebase MCP (`firebase_create_app`, `firebase_create_android_sha`, `firebase_get_sdk_config`).
3. Download `google-services.json` into `apps/android/app/src/dev/`. It stays gitignored, and its contents are never printed.
4. **App Distribution:**
   - Create the group `testers` in `fibrai-dev`.
   - Read the current members of `testers` in `nutri-bot-dev` with the Firebase CLI and add the same members.
   - Tester emails are never written into the repository, plans or logs.
5. Crashlytics is enabled automatically on the first crash upload. Analytics needs no code change.

### 2. Gradle and code

1. `defaultConfig.applicationId = "app.fibrai.android"`. The dev flavor keeps `applicationIdSuffix = ".dev"`. `namespace` stays `com.nutri.android`.
2. Search `apps/android/` for hard-coded `com.nutri.android` used as the **package name**: manifest placeholders, `packageName` comparisons, intent package, FileProvider authority, notification channel or shortcut ids. Replace each with `BuildConfig.APPLICATION_ID`, `context.packageName` or `${applicationId}`. Kotlin package declarations and `com.nutri.*` action/extra names stay as they are.
3. No Room or schema change. `nutri.db` stays and is created fresh in the new app.

### 3. Distribution tool

1. `tools/distribute-dev.ps1`:
   - `$FirebaseProject` = the new project ID;
   - `$FirebaseApp` = the new app ID (read from the MCP or console);
   - the console URL uses `app.fibrai.android.dev`.
2. Keep the version bump, tag format and `-Notes` rules unchanged.

### 4. Tester notice

The first release notes after this plan say, in pt-BR, that this is a new app:
- Install it from Firebase App Tester.
- Uninstall the old "Dieta Bot Dev".
- Onboarding starts over, and old local meals and memory do not carry over.

The notes are written at distribution time, which happens when the owner asks for a test build (A16).

## Intended documentation changes at Completion

- `AGENTS.md`:
  - "Technical IDs stay `nutri`" drops `applicationId` and Firebase `nutri-bot-dev` from its list;
  - "Flavors dev (`app.fibrai.android.dev`…) / prod (`app.fibrai.android`)";
  - "dev = Firebase `fibrai-dev`".
- The [android README](../README.md) and any live spec that names the old id or project.

## Out of scope

- Visible strings and assets ([A49](a49-fibrai-tali-visible-rename.md)).
- Renaming the namespace or Kotlin packages.
- Prod build or distribution ([production gate](../../content-policy/production-gate.md)).
- Deleting `nutri-bot-dev`: an owner action, after testers move over.
- Migrating tester data.

## Validation

1. `./gradlew testDevDebugUnitTest verifyRoborazziDevDebug assembleDevRelease` passes.
2. `aapt2 dump badging` (or `apkanalyzer`) on the dev release APK shows `package: name='app.fibrai.android.dev'`.
3. Emulator: install the new APK next to the old app. Both install as separate apps; the new one opens to the splash and onboarding.
4. A forced test crash and one Analytics event appear in `fibrai-dev` (Crashlytics, DebugView), checked through the Firebase MCP.
5. With the owner's go: `./tools/distribute-dev.ps1 -Notes <file>` reaches the `testers` group of `fibrai-dev`. Results record the version and tag.
6. `node tools/check-docs.mjs` passes.

## Results

Planning only.
