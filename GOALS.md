# GOALS.md

Rodar UM por vez. Critério de done no final de cada bloco.
Nunca ler .env pra citar chave. Nunca commitar .env.
Nunca Firebase, TDEE, cap de eat-back, Gemini, Hetzner hoje.
LLM = gpt-6-luna + reasoning.effort=none.

## GOAL 1

Implemente `lib/domain/rules/budget_calculator.dart` + `test/budget_calculator_test.dart`.

Regras:
- teto_base vem do perfil (mesmo todos os dias | util/fds | 7 dias)
- credito_treino = 0 se politica 0 OU se treino do dia não foi informado
- credito_treino = treino_kcal * (pct/100) se parcial
- credito_treino = treino_kcal se 100%
- SEM cap de crédito
- teto_efetivo = teto_base + credito
- orcamento_janela = max(0, teto_efetivo - consumido - reserva_proximas)

Sem UI, sem rede, sem widget.
DONE quando `flutter test test/budget_calculator_test.dart` passa.

## GOAL 2

Crie `server/` FastAPI:
- GET /health
- POST /v1/estimate
- POST /v1/fit

Contrato em docs/api-contract.md.
Leia `.env` na raiz. NÃO printar OPENAI_API_KEY.
Modelo fixo: gpt-6-luna, reasoning.effort=none.
Foto: se vier image_b64, estima e DESCARTA.
Timeout 20s. Falha → confiança baixa + pergunta "descreve em 1 linha".
Use server/Dockerfile + server/docker-compose.yml + server/requirements.txt.

DONE quando:
- `curl -s localhost:8080/health` responde ok + model gpt-6-luna
- POST /v1/estimate com texto "2 paes, ovo, cafe com leite" devolve kcal + P + confianca

## GOAL 3

Flutter mínimo, stack travada: go_router + get_it + flutter_bloc (Cubit) + result_dart.

Rotas: /onboarding/teto /onboarding/treino /
Telas: O1 teto (3 chips), O2 eat-back 0% | % digitável default 50 | 100% SEM cap,
T0 Home LIMPA (zero chips), T1 sheet texto+foto, T2 card curto.

T1 chama POST /v1/estimate. Base URL = API_PUBLIC_URL (--dart-define). NUNCA a key OpenAI.
Home limpa: saldo, card próxima janela, campo texto/foto, CTA "o que cabe agora".
Copy dos wires se wires/nutri-wires.html existir.

DONE quando `flutter analyze` limpo no que você criou e a Home abre sem chip.

## GOAL 4 — torre + tunnel (só depois de 2 e 3)

NÃO criar VPS. NÃO abrir porta 80/443 no roteador.
Documente e, se Docker estiver no ar, ligue cloudflared apontando pra :8080.
Quick tunnel primeiro (URL *.trycloudflare.com).
Named tunnel: SETUP.md seção C + TUNNEL_TOKEN no .env + profile named-tunnel.

DONE quando o celular na rede 4G chama GET {TUNNEL}/health e recebe ok.
Registre (não execute) a migração: docs/MIGRACAO-VPS.md.

## NÃO FAZER
Hetzner hoje, Firebase, Gemini, chave no APK, computer-use no roteador,
git push --force, TDEE, cap +300.
