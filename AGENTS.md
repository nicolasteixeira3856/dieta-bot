# Nutri — constituição do repo

App Flutter de encaixe alimentar. IA no servidor. Número + tom seco. Sem coach.

## Stack
- Flutter: flutter_bloc (Cubit), go_router, get_it, result_dart
- Camadas oficiais: View → Cubit → Repository → Service
- API: FastAPI, gpt-6-luna, reasoning.effort=none
- Sem Firebase, sem Gemini, sem TDEE, sem cap de eat-back, sem chave no APK

## Produto congelado
- Home híbrida, chat é sheet
- Onboarding 2 telas: teto + eat-back
- Eat-back: 0% | % digitável default 50 | 100%. SEM cap
- Home dia 1: zero chips. Chip nasce no 2º log da janela, pergunta 1x, dá pra remover
- 1 pergunta se confiança ≠ alto
- Fds: cobra só almoço + janta + fechamento
- Treino digitado. Sem número no dia, crédito = 0
- Foto no T1. Foto sobe, estima, apaga no server
- Disclaimer: estimativa, não consulta

## LLM
LLM_MODEL=gpt-6-luna
LLM_EFFORT=none
Lê .env. Nunca printa OPENAI_API_KEY. Nunca commita .env.
Flutter leva só API_PUBLIC_URL + INVITE_CODE.

## Infra S0
Torre em casa + Cloudflare Tunnel. Sem porta no roteador.
Migração VPS: docs/MIGRACAO-VPS.md — não executar agora.

## Como trabalhar
Siga GOALS.md um bloco por vez.
Não invente tela, lib ou regra fora do AGENTS.md / GOALS.md.
Não use computer-use em painel de cloud/roteador.
Teste antes de marcar done.
