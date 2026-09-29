---
name: compose-stability
description: Review Compose state stability and recomposition in Dieta Bot when changing UiState, complex composables or lists, or investigating rendering delays.
---

# Compose stability

Work within [AGENTS](../../../AGENTS.md) and the approved plan. Check actual state types and compiler/build configuration before prescribing annotations or dependencies.

- Immutable state must remain immutable through its nested values. Use @Immutable only when that promise is true; use @Stable only when its change-notification contract is satisfied.
- Kotlin List is a read-only interface, not proof of immutability. An annotated wrapper does not make a mutable backing collection immutable. Use defensive copies or existing immutable data structures as appropriate.
- Preserve existing dependencies. Adding kotlinx.collections.immutable needs to be within the approved scope; it is not an automatic prerequisite.
- Pass state and callbacks to children. Avoid mutable captures that invalidate the intended stability contract.
- Use derivedStateOf when it reduces meaningful recompositions for frequently changing inputs, such as scrolling; do not wrap every calculation.

For compiler stability reports, first inspect the current Compose compiler configuration. The repository does not currently configure a reports/metrics destination; an arbitrary Gradle property is not evidence that reports were produced. Configure and verify reports only within a requested, approved performance investigation, then inspect the generated artifacts.

Validate changed behavior with the relevant dev tests. Changed UI also needs the visual workflow; stable annotations and passing tests do not prove a gold match.
