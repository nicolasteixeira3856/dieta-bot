---
name: dieta-bot-android-decisao
description: Decide se uma ideia Nutri é feature, hipótese ou violação da constituição. Use antes de spec ou código novo, ou quando o pedido reabrir Flutter, RN, TDEE, cap, tela extra.
---

# dieta-bot-android-decisao

1. Lê `AGENTS.md`. Se o pedido toca decisão congelada, recusa e cita a linha.
2. Feature nova exige 3 opções A/B/C antes de código.
3. Cada ideia em 3 linhas: user em <15s / sistema devolve / VALIDADO ou HIPÓTESE.
4. Stack viva é só `apps/android/` + `server/`. Flutter e RN estão mortos.
5. Não inventa tela fora de O1 O2 T0 T1 T2 T3.
6. ADR em `docs/decisions/` se a decisão ficar.
