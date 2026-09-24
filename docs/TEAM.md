# Time Nutri — opcional

Isto não entra no APK. É o jeito de pedir trabalho no grok-cli.
A sessão já é o líder. Não existe agente Orquestrador.

Constituição do produto continua em AGENTS.md e GOALS.md.
Este arquivo só diz quem opina e quando.

## Liga

Copie para a raiz do repo, quando quiser:

- `.grok/agents/`
- `.agents/skills/`
- `docs/TEAM.md`

`grok inspect` lista Iris, QA, pesquisador, Harper, Benjamin, Lucas, revisor.

Cada papel tem memória em `.grok/memory/roles/`. O teto e o esquecimento estão só em `tools/role_memory.py`. O líder mostra essa memória no briefing e grava uma lição quando você corrige ou quando um erro recorrente fecha. Você não precisa dizer "lembra".
Não use `--no-subagents` na sessão em que quiser o time.
Hermes: docs/HERMES.md.

## Disparo sozinho

A skill `nutri-feature` carrega quando o pedido é feature, fluxo, copy, “e se”, “vale a pena”, “como funcionaria”.
Não precisa digitar `/deep-research`. O líder spawna o pesquisador se o fato não está no repo.

Não carrega em correção de uma linha, Goal 1 (calculator), troca de `.env`, nem bug com causa já vista.

## Rodadas

Máximo 3 filhos por rodada. Filhos não conversam entre si.

Feature, sem código:

1. pesquisador, se a decisão depende de fato externo (porção, API, preço, comportamento de produto alheio).
2. Harper, regra e copy.
3. Iris, tela no celular.

Para aí. Uma decisão. Não implementa até o dono confirmar.
Decisão fechada: skill `nutri-decisao`. Não abra a pasta `decisoes/` inteira.

Código, depois do sim:

- Flutter: Benjamin. API, Docker, túnel: Lucas.
- Os dois juntos só se os arquivos não se cruzam, cada um em worktree.
- QA depois do write: `flutter test` / `flutter analyze`. Tela: skill `nutri-visual`, no emulador. Celular físico só se o dono pedir.
- revisor por último: constituição. Bloqueia ou solta.

## Quem é quem

| Papel | Escreve | Entrega |
|---|---|---|
| pesquisador | não | achado, fonte, efeito no Nutri, risco |
| Harper | não | 1 recomendação, 2 descartes, o que a pessoa faz, o que a IA devolve |
| Iris | não | estados da tela, copy, o que o polegar faz, o que não desenhar |
| Benjamin | Flutter | arquivos, diff mínimo, comando de prova |
| Lucas | server, infra, docs de API | arquivos, curl, aviso se o contrato muda |
| QA | não, salvo teste que falta | passou / falhou, comando, evidência |
| revisor | não | BLOQUEIA, OK-COM-NOTA ou OK |

## Fora

- OPENAI_API_KEY, valor de `.env`, Hetzner, porta 80/443 no roteador.
- Chip no dia 1, cap de eat-back, TDEE, Firebase, Gemini.
- Quarto filho na mesma rodada. Votação em loop.
