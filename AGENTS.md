# Nutri — constituição do repo

Um agente. Job: encaixar a próxima refeição no saldo do dia, sobretudo a janta.

Número + tom seco. Sem coach.

## Produto

- chat é sheet, não home.
- home dia 1 zero chips.
- chip no 2º log estável da mesma janela, pergunta 1x, removível.
- onboarding 2 telas: teto + eat-back.
- Eat-back: 0% | % digitável default 50 | 100%. SEM cap.
- treino digitado. sem número no dia, crédito = 0.
- 1 pergunta se confiança ≠ alto.
- foto T1 desde o dia 1. ≤1280 JPEG 70 no client. sobe, estima, apaga no server.
- Fds: cobra só almoço + janta + fechamento.
- disclaimer estimativa, não consulta.

## Fórmulas

Timezone: America/Sao_Paulo.

- teto_efetivo = teto_base + credito
- credito = 0 se política 0 OU se treino do dia não informado
- credito = treino_kcal * pct/100 se parcial; = treino_kcal se 100%
- orçamento_janela = max(0, teto_efetivo − comido − reserva_próximas)

## LLM

LLM só no server: gpt-6-luna, reasoning.effort=none.
O client leva só API_PUBLIC_URL + INVITE_CODE. Com header X-Invite.
Há zero chave OpenAI no APK/bundle.
O server lê .env. Nunca printar OPENAI_API_KEY. Nunca commitar .env.

## Stacks do monorepo

- server/ FastAPI intacto
- legacy/flutter/ arquivo e baseline de regra
- apps/android/ Kotlin + Compose + Material 3 Expressive
- apps/rn/ Expo SDK 57 + TS + Expo Router + NativeWind v4 + React Native Reusables

## Como trabalhar

1 /goal = 1 pasta, e não editar duas stacks no mesmo goal, e não editar server/ num goal de client.
A validação visual no emulador Android via adb obrigatória para marcar UI DONE.
E toda decisão técnica ou de negócio vira ADR em docs/decisions/.
Não usar computer-use em painel de cloud/roteador.
Testar antes de marcar done.

## NÃO FAZER

Firebase, Gemini, TDEE, cap de eat-back, Health/Xiaomi, chave no client, VPS, porta no roteador, iOS neste experimento, tela fora de O1 O2 T0 T1 T2 T3.
