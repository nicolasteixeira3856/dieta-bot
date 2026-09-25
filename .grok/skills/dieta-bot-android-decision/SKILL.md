---
name: dieta-bot-android-decision
description: Decide whether a Nutri idea is a feature, a hypothesis, or a constitution violation. Use before a new spec or code, or when the request reopens Flutter, RN, TDEE, a cap, or an extra screen.
---

# dieta-bot-android-decision

1. Read `AGENTS.md`. If the request touches a frozen decision, refuse and cite the line.
2. A new feature needs 3 options A/B/C before code.
3. Each idea in 3 lines: user in <15s / system returns / VALIDATED or HYPOTHESIS.
4. Live stack is only `apps/android/` + `server/`. Flutter and RN are dead.
5. Do not invent a screen outside splash O1 O2 T0 T1 T2 T3.
6. ADR in `docs/decisions/` if the decision sticks.
