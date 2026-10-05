# ADR-036 — Technical identity: `app.fibrai.android` everywhere in the app, Firebase project for Fibrai dev

- Status: Accepted (2026-10-05, explicit owner approval of A48)
- Date: 2026-10-05
- Context: `android`
- Replaces, when A48 completes:
  - from [ADR-016](../../produto/adrs/ADR-016-nome-dieta-bot.md), every app-side item of "Fica nutri" (package, `applicationId`, Firebase project, `nutri.db`, `nutri-release.jks`, extras and actions `com.nutri.*`) and the `Nutri*`/`DietaBot*` class-name rule;
  - from [ADR-014](ADR-014-flavors-firebase-dev.md), the dev `applicationId` and the Firebase project `nutri-bot-dev`.
- Surviving scope:
  - ADR-014 still owns the flavor split, Firebase in dev only, the `Telemetry` interface and events without user text.
  - ADR-016 still keeps the server-side and external IDs: server identifiers and loggers, VM `nutri-api` and `/opt/nutri`, Stitch project `Nutri`, the repository name `dieta-bot`.

## Context

ADR-016 left the final package for "a future ADR before prod", once the name was final. [ADR-034](../../produto/adrs/ADR-034-fibrai-brand-tali-assistant.md) fixed the brand "Fibrai", and the owner bought `fibrai.app` (see the [ADR-034](../../produto/adrs/ADR-034-fibrai-brand-tali-assistant.md) clearance). The owner asked to change the app id and to register the app in a new Firebase project with Crashlytics, Analytics and App Distribution for the current testers.

## Decision

1. **`applicationId`:**
   - `app.fibrai.android` for `prod`. It is the reverse of the owned domain `fibrai.app`.
   - `app.fibrai.android.dev` for `dev`. The `.dev` suffix is unchanged.
2. **No legacy identifier in the app code (owner decision, 2026-10-05).** Everything under `apps/android/` and the Android tooling moves to Fibrai:
   - Gradle `namespace` and every Kotlin package (main, flavors, debug and tests): `com.nutri.android` → `app.fibrai.android`, with the source folders moved to match.
   - Class names: `DietaBot*` → `Fibrai*` (`FibraiApplication`, `FibraiTheme`, `FibraiDatabase`, `FibraiTokens`, `Theme.Fibrai`, …).
   - Room file `nutri.db` → `fibrai.db`. The exported schema folder follows the new database class name, and the migration tests read it there. Schema versions and migrations are unchanged.
   - Intent actions, extras and component names: `com.nutri.android.*` → `app.fibrai.android.*`, `nutri_*` extras → `fibrai_*`.
   - SharedPreferences and DataStore names (`nutri_push`, `nutri_day`), the Android Keystore alias (`nutri_memory_v2`) and the dev system property `debug.nutri.hide_dev_tools` → `fibrai_*` / `debug.fibrai.hide_dev_tools`.
   - Release keystore file `nutri-release.jks` → `fibrai-release.jks`, **same key and alias**, so signing does not change. The file and its local properties are the owner's and stay out of git.
   - Capture and distribution scripts in `tools/` that address the package or components.
   - Renaming storage names costs nothing now: the new `applicationId` is a fresh install with no data to carry over.
   - Outside the app, these stay `nutri` until their own decision: server identifiers and loggers, VM `nutri-api` and `/opt/nutri`, Stitch `Nutri` and the repository `dieta-bot`.
3. **New Firebase project for dev:**
   - Project ID `fibrai-dev`, display name "Fibrai Dev", Spark plan. If the ID is taken, use the console's suggested suffix and record the real ID in A48 Results.
   - One Android app: `app.fibrai.android.dev`.
   - Crashlytics and Analytics, exactly as ADR-014 allows: dev only, enums and numbers only.
   - App Distribution with the group `testers`, whose members are the current `testers` of `nutri-bot-dev`.
   - `prod` still has no Firebase.
4. `tools/distribute-dev.ps1` targets the new project, app and group. A16 versioning (`0.0.N`, tags `dev-vX`) continues without reset.
5. **`nutri-bot-dev` is frozen.** No new builds or events go to it. Deleting it is the owner's manual action, outside any plan.

## Motivation

- Changing the `applicationId` creates a new app in Android and Firebase. Doing it now, during a closed test with two testers, costs the least it will ever cost.
- The owner wants no legacy identifier in the app code. The fresh install that the new `applicationId` forces is the cheapest moment to also rename packages, storage names and the database file.

## Consequences

### Positive

- The id matches the brand and the owned domain, so no rename is needed before prod.
- Crashlytics and Analytics restart clean under the new project.

### Negative

- **Testers install a new app.** The old "Dieta Bot Dev" app (`com.nutri.android.dev`) does not upgrade to the new id. Its Room data does not migrate, so profile, meals and memory are lost, and the testers redo onboarding. A48 says this in the release notes.
- Crashlytics and Analytics history stays in `nutri-bot-dev`.
- A large mechanical refactor touches about 160 Kotlin files, the manifests, tests, Roborazzi baselines, the Room schema folder and the scripts. It needs a clean tree and no parallel Android plan in flight (A47).
- The `namespace` (`app.fibrai.android`) equals the prod `applicationId` but not the dev one (`.dev`). Code that needs the installed package must keep using `BuildConfig.APPLICATION_ID` or `context.packageName`.
- The `prod` value changes in Gradle without a prod build. The [production gate](../../content-policy/production-gate.md) is not touched: no prod build, distribution or store work.

## Alternatives considered

- **Keep `com.nutri.android`:** rejected by the owner.
- **`com.fibrai.android`:** valid, but the project owns `fibrai.app`, not `fibrai.com` (registered to someone else). Reverse-DNS of an owned domain avoids an id that suggests another owner.
- **Keep the namespace, packages and storage names as `nutri`:** less churn, but the owner rejected leaving legacy identifiers in the code.
- **Reuse `nutri-bot-dev` with a second Android app:** possible, but the owner asked for a new project, and a project named after the old brand would remain.

## Relations

- [ADR-014](ADR-014-flavors-firebase-dev.md), [ADR-015](../../server/adrs/ADR-015-log-conversa-dev.md) (X-Request-Id), [ADR-016](../../produto/adrs/ADR-016-nome-dieta-bot.md), [ADR-034](../../produto/adrs/ADR-034-fibrai-brand-tali-assistant.md).
- Plan: [A48](../plans/completed/a48-fibrai-app-id-firebase.md).

Once accepted, this ADR is not edited. A later change needs a new ADR that declares the replacement.
