# 001 — Monorepo para comparar Android e React Native

Flutter arquivado, não apagado. O app continua em `legacy/flutter/`.

Monorepo com pastas isoladas: `server/`, `legacy/flutter/`, `apps/android/`, `apps/rn/`. Um goal mexe numa pasta.

Comparação só Android. iOS fica fora deste experimento.

Critério de vitória, nos dois clients:

- Visual no emulador igual ao wire (tokens, não "parecido").
- User registra a refeição em <15s.
- TDD das fórmulas verde.

grok-cli: 1 agente, 1 /goal, 1 pasta.

Room/SQLite fora deste experimento. Persistência do dia = DataStore/JSON (Android) e AsyncStorage/JSON (RN).

Settings extra e Push 16:30 fora deste experimento.

T3 Encaixe entra: é o job. CTA "o que cabe agora", POST /v1/fit.
