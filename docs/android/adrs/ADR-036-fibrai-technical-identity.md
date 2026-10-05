# ADR-036 — Technical identity: applicationId `app.fibrai.android`, Firebase project for Fibrai dev

- Status: Proposed (2026-10-05; accepted with the owner's approval of [A48](../plans/a48-fibrai-app-id-firebase.md))
- Date: 2026-10-05
- Context: `android`
- Replaces, when A48 completes:
  - from [ADR-016](../../produto/adrs/ADR-016-nome-dieta-bot.md), the `applicationId` and Firebase project items of "Fica nutri";
  - from [ADR-014](ADR-014-flavors-firebase-dev.md), the dev `applicationId` and the Firebase project `nutri-bot-dev`.
- Surviving scope:
  - ADR-014 still owns the flavor split, Firebase in dev only, the `Telemetry` interface and events without user text.
  - ADR-016 still keeps the other `nutri` IDs.

## Context

ADR-016 left the final package for "a future ADR before prod", once the name was final. [ADR-034](../../produto/adrs/ADR-034-fibrai-brand-tali-assistant.md) fixed the brand "Fibrai", and the owner bought `fibrai.app` (see the [ADR-034](../../produto/adrs/ADR-034-fibrai-brand-tali-assistant.md) clearance). The owner asked to change the app id and to register the app in a new Firebase project with Crashlytics, Analytics and App Distribution for the current testers.

## Decision

1. **`applicationId`:**
   - `app.fibrai.android` for `prod`. It is the reverse of the owned domain `fibrai.app`.
   - `app.fibrai.android.dev` for `dev`. The `.dev` suffix is unchanged.
2. **These stay `nutri`:**
   - the Gradle `namespace` and every Kotlin package (`com.nutri.android`);
   - `nutri.db` and the release keystore `nutri-release.jks`, the same key for the new id;
   - the server, the VM `nutri-api`, the Stitch project and extras/actions `com.nutri.*`.
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
- Keeping the namespace and packages avoids touching more than 100 Kotlin files and the Room schema for a change the user never sees.

## Consequences

### Positive

- The id matches the brand and the owned domain, so no rename is needed before prod.
- Crashlytics and Analytics restart clean under the new project.

### Negative

- **Testers install a new app.** The old "Dieta Bot Dev" app (`com.nutri.android.dev`) does not upgrade to the new id. Its Room data does not migrate, so profile, meals and memory are lost, and the testers redo onboarding. A48 says this in the release notes.
- Crashlytics and Analytics history stays in `nutri-bot-dev`.
- The code `namespace` (`com.nutri.android`) differs from the `applicationId`. Android supports this; code that reads the package must use `BuildConfig.APPLICATION_ID` or `context.packageName`, never the namespace.
- The `prod` value changes in Gradle without a prod build. The [production gate](../../content-policy/production-gate.md) is not touched: no prod build, distribution or store work.

## Alternatives considered

- **Keep `com.nutri.android`:** rejected by the owner.
- **`com.fibrai.android`:** valid, but the project owns `fibrai.app`, not `fibrai.com` (registered to someone else). Reverse-DNS of an owned domain avoids an id that suggests another owner.
- **Rename the namespace and packages too:** high churn, no user value. It can be revisited before 1.0.0.
- **Reuse `nutri-bot-dev` with a second Android app:** possible, but the owner asked for a new project, and a project named after the old brand would remain.

## Relations

- [ADR-014](ADR-014-flavors-firebase-dev.md), [ADR-015](../../server/adrs/ADR-015-log-conversa-dev.md) (X-Request-Id), [ADR-016](../../produto/adrs/ADR-016-nome-dieta-bot.md), [ADR-034](../../produto/adrs/ADR-034-fibrai-brand-tali-assistant.md).
- Plan: [A48](../plans/a48-fibrai-app-id-firebase.md).

Once accepted, this ADR is not edited. A later change needs a new ADR that declares the replacement.
