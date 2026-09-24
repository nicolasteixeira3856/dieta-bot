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
Tela Flutter: skill `nutri-ui` para construir, skill `nutri-visual` para provar no emulador. Não use o celular físico.
Decisão de feature ou negócio: skill `nutri-decisao`. Leia só `DECISOES.md` e, se o título bater, o dia linkado.

## Time (opcional)
Papéis em `.grok/agents/`. Procedimento em `.agents/skills/nutri-feature`.
Leia docs/TEAM.md. Você é o líder. Não spawnar um líder.

Feature, fluxo ou “e se”: siga a skill. O pesquisador entra sozinho se o fato não está no repo. Não espere `/deep-research`.
Iris opina a tela. QA prova depois do write. revisor fecha a constituição.
Máximo 3 filhos por rodada. Goal 1 e correção de uma linha: ninguém além de você.
Filho não commita, não dá push, não cita valor de `.env`.
Memória de cada papel: `python tools/role_memory.py show --role NOME`. Grave lição com `nutri-lembrar` quando houver correção. O teto mora em `tools/role_memory.py`.
