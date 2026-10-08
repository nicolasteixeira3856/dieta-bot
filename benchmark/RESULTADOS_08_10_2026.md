# Benchmark do Chat — resultados da primeira execução (08/10/2026)

Execução autorizada pelo owner em 08/10/2026. Modelo `gpt-6-luna`; braços `baseline-none`, `new-none`, `new-low`; 155 casos × 3 repetições. Juiz: `gpt-6-astra` pelo Codex do owner (assinatura), cego ao braço, 1.387 respostas. Arquivos brutos em `benchmark/out/` (fora do git): `results-20261008-113718.jsonl` (bruto), `results-20261008-113718-rescored.jsonl` (reavaliado), `judge-20261008-113718-codex.jsonl` (juiz), `report-20261008-113718-rescored.md`.

**Decisões do owner (08/10/2026, após estes resultados):** `reasoning.effort=low` sempre no Chat; o reforço de proteína passa a um modelo híbrido para teste (o modelo propõe os alimentos, o server valida piso, folga e dieta em código). A constituição (`AGENTS.md` § LLM, `effort=none`) e o ADR-043 decisão 4 precisam de ADR e plano para registrar a mudança antes de código.

## Veredito em cinco linhas

1. **O novo prompt resolve a maior parte do que o atual não resolve.** `det_strict` sobe de 44,5 % (baseline) para 75,3 % (`new-none`) com o mesmo effort; nas famílias que motivaram o brainstorm (várias ações, treino, receitas, cópia de dia nomeado) o salto é de 17–24 % para 70–97 %.
2. **`effort=low` ganha 9 pontos de `det_strict` sobre `none` (84,3 %) e 10 pontos de aprovação do juiz (79,4 % contra 69,1 %), mas dobra a latência.** p95 vai de 10,2 s para 16,4 s e o p50 de 3,9 s para 7,0 s; custo 1,3×. Pelo critério fixado no README (p95 < 8 s) não passaria como regra geral; o owner decidiu adotar `low` aceitando a latência.
3. **Reforço de proteína pelo modelo: não em `none`; parcial em `low`.** Em `none` o modelo não fez o reforço em nenhum dos 10 planos devidos e acrescentou "(opcional)" em 2 de 3 pratos que já estavam acima do piso. Em `low` acertou 7 dos 9 planos devidos (juiz: 66,7 % de aprovação na família contra 29,2 % em `none`), mas sem variedade (frango em 4 de 5) e sem resolver o vegetariano (1 de 3). Daí o híbrido decidido: o modelo propõe, o server valida.
4. **Erros de forma caíram com o novo schema:** zero JSON inválido em `new-low`, 6 em `new-none`, 2 na baseline. Flags graves do juiz em 1.387 respostas: `new-low` 1 "diz que gravou" e 1 elogio; `new-none` 7 "diz que gravou" e 1 menção a corpo; baseline 7, 2 e 1 sugestão de pular.
5. **Juiz e checagens concordam.** Nos 457 pares comuns, a nota do juiz (0–1) é 0,672 / 0,762 / 0,819 para baseline / `new-none` / `new-low`, na mesma ordem do `det_strict`. O composite final é 0,559 / 0,757 / 0,832.

## Execução

| Item | Valor |
|---|---:|
| Chamadas de geração | 1.395 planejadas + 6 repetições por erro de conexão (`--resume`) |
| Custo das gerações (tarifa luna) | US$ 0,59 |
| Cache do prefixo | 93 % dos tokens de entrada em todos os braços |
| JSON inválido | 8 (6 `new-none`, 2 `baseline-none`, 0 `new-low`) |
| Juiz pela API (`gpt-6-astra`) | abortado: 153 + 21 respostas e a conta sem créditos; descartado para manter um critério só |
| Juiz pelo Codex (`judge_export.py` / `judge_import.py`) | 1.387 vereditos válidos, 0 faltando, rubrica com a regra CLOSING, cego ao braço, custo zero em API |

Reavaliação sem nova chamada (`rescore.py`), após a leitura dos resultados: as respostas não mudaram; mudaram checagens e expectativas com defeito meu, listadas em "Defeitos do benchmark" abaixo. Efeito: 19 linhas trocaram de aprovação (de 1.395).

## Placar por braço (determinístico, completo)

| Braço | det_strict | det_supported | casos ≥ 2/3 | p50 | p95 | tokens saída (+raciocínio) | US$/chamada |
|---|---:|---:|---:|---:|---:|---:|---:|
| baseline-none | 44,5 % | 55,9 % | 47,1 % | 4,0 s | 7,1 s | 239 | 0,00033 |
| new-none | 75,3 % | 75,3 % | 78,1 % | 3,9 s | 10,2 s | 315 | 0,00041 |
| new-low | 84,3 % | 84,3 % | 84,5 % | 7,0 s | 16,4 s | 571 (+240) | 0,00054 |

`det_supported` da baseline ignora o que o prompt atual não consegue expressar (treino, receitas, várias ações, macros na memória, reforço pelo modelo). Mesmo assim fica 20 pontos abaixo de `new-none`: a diferença não é só capacidade nova, é execução.

### Por família (det_strict, % de caso × repetição)

| Família | n | baseline | new-none | new-low | Leitura |
|---|---:|---:|---:|---:|---|
| workout | 30 | 10,0 | 96,7 | 100,0 | treino via Chat funciona; baseline nem tem escopo |
| copy | 33 | 24,2 | 97,0 | 93,9 | COPY_SOURCE resolvido em código fecha o "mesmo café de ontem"; `low` pergunta mais no caso ambíguo |
| recipe | 36 | 0,0 | 86,1 | 88,9 | índice e receita completa funcionam |
| multi | 60 | 16,7 | 70,0 | 78,3 | várias ações: ver falhas abaixo (limite de 6, pergunta ao lado de ação) |
| habit | 24 | 66,7 | 83,3 | 100,0 | rotina com macros no MEMORY |
| plan | 60 | 51,7 | 76,7 | 85,0 | opções com id, continuidade "fiz a 2" |
| memory | 27 | 44,4 | 59,3 | 85,2 | `none` esquece `replace`/`reinforce` e a frase de memória cheia |
| photo | 30 | 56,7 | 60,0 | 76,7 | `none` pergunta quando não deve e vice-versa |
| tone | 42 | 71,4 | 78,6 | 81,0 | duro sem corpo/elogio nos três braços; falhas são de conteúdo da crítica |
| sequence | 18 | 44,4 | 55,6 | 66,7 | "esqueci o leite" dobra a contagem nos três braços |
| discovery | 9 | 22,2 | 11,1 | 66,7 | `none` não grava as rotinas com macros na primeira abertura |
| boost | 24 | 12,5 | 25,0 | 54,2 | ver seção própria |
| digest | 6 | 50,0 | 50,0 | 66,7 | plano combinado só no digest: metade ignora |
| log | 48 | 95,8 | 97,9 | 91,7 | `low` faz perguntas a mais (manteiga, salada) |
| scope | 18 | 100,0 | 88,9 | 100,0 | `none` falhou uma injeção por faixa de kcal, não por escopo |

Por origem (baseline / none / low): mensagens suas das transcrições do Grok 40,4 / 64,9 / 75,4; suas mensagens do log do server 75,0 / 75,0 / 100,0; sintéticas 39,5 / 77,7 / 86,4; modos de falha dos testers 100,0 / 90,0 / 90,0. Por tom: duro 37,0 / 73,1 / 79,6; seco 46,8 / 75,9 / 85,7.

## Effort `none` × `low` (mesmo prompt, mesma entrada)

Critério do README: `low` justifica mudar a constituição se ganhar ≥ 5 pontos de `det_strict` ou ≥ 0,3 de correctness em multi, copy e plan, com p95 < 8 s e custo < 2×.

| Medida | new-none | new-low | Critério |
|---|---:|---:|---|
| det_strict multi | 70,0 % | 78,3 % | +8,3 ✓ |
| det_strict copy | 97,0 % | 93,9 % | −3,1 ✗ |
| det_strict plan | 76,7 % | 85,0 % | +8,3 ✓ |
| correctness do juiz (457 pares comuns) | 4,15 | 4,42 | +0,27 ✗ (por pouco) |
| p95 | 10,2 s | 16,4 s | ✗ (limite 8 s; `none` também não cumpre) |
| custo por chamada | 1× | 1,31× | ✓ |

Onde `low` muda de fato: memory (+26), discovery (+56), habit (+17), photo (+17), boost (+29). Onde regride: log (−6) e copy (−3), sempre por perguntas de clarificação a mais ("quantos gramas de manteiga?", "qual foi a quantidade da salada?") em mensagens que o `none` registra direto. O raciocínio custa 240 tokens por chamada e 3 s de p50.

Pelo juiz, `low` também ganha em usefulness (3,71 contra 3,43) e em creativity dos planos (3,43 contra 2,60), e cai em "pergunta o já sabido" (1,1 % contra 3,9 %) e em "diz que gravou" (1 contra 7). Minha recomendação era manter `none` pela latência; o owner decidiu `low` sempre. Consequência prática: o p95 de 16 s precisa de feedback de espera no app e de timeout acima disso no server.

## Reforço de proteína: server ou modelo

Regra PROTEIN BOOST no prompt novo (piso = 30 % da proteína restante; folga ≥ 60 kcal; até dois alimentos `(opcional)`, livres e variados, nunca repetindo o prato nem contrariando a dieta). Cinco casos exigem o reforço, três exigem ausência; 3 repetições.

| Braço | Respostas que viraram plano (de 15 devidas) | Reforço correto entre os planos | Ausência correta (de 9) | Alimentos usados |
|---|---:|---:|---:|---|
| baseline-none | 6 | 0 | 8 | — (não tem a regra; o server faria em código) |
| new-none | 10 | 0 | 7 | frango 1× (repetindo o frango do prato) |
| new-low | 9 | 7 | 8 | frango 4×, ovo 1×, tofu 1× |

O juiz (72 respostas de boost) marcou `protein_boost_wrong` em 34 % das respostas de `new-none`, 14 % de `new-low` e 26 % da baseline, e deu creativity 1 a quase todo reforço ausente ou repetido.

Leitura:

- Em `none` o modelo não executa a regra: nenhum reforço devido saiu, e em 2 de 3 repetições do prato já acima do piso ele acrescentou iogurte ou ovo "(opcional)". É o mesmo achado do S24, agora medido.
- Em `low` a regra funciona na maioria (7 de 9 planos), mas a variedade que motivou a mudança não apareceu: frango em 4 dos 5 reforços, e no vegetariano o modelo citou "tofu ou lentilha" na prosa sem montar o item em 2 de 3 vezes.
- Seis das 15 respostas devidas por braço viraram `log` em vez de `plan` ("Jantar: 2 pães franceses..." às 19:50 foi lido como relato). Dois textos meus eram ambíguos (`boost-duro-ana`, `boost-vegetariano-diego`); estão corrigidos para a próxima execução, mas o número acima já desconta isso ao olhar só os planos.

Veredito e decisão: com `low` adotado, o owner escolheu testar o híbrido: o modelo propõe os alimentos `(opcional)` com gramas (variedade e adequação ao prato), e o server valida em código piso, folga, limite de dois alimentos, repetição do prato e dieta, completando ou cortando o que o modelo errar (`protein_boost.py` vira validador). Isso pede um ADR que suceda a decisão 4 do ADR-043 e um plano de server; o benchmark da família `boost` é o critério de aceite.

## O que cada braço acertou e errou

Falhas do modelo nos dois efforts (casos 0/3 em `new-none` e `new-low`):

- **Revisão que dobra a conta** (`seq-dia2-t3-esqueci-leite`): "esqueci, 250 ml de leite" com o leite já dentro da rotina gravada vira `add` de 115 kcal em vez de revisão sem mudança. 6/6 erradas. Candidato a regra explícita no Plano 2 ("um item já presente no registro não é acréscimo").
- **Confiança `medium` com tudo pesado** (`seq-dia2-t4-almoco`): almoço com cinco itens em gramas sai `medium` 6/6. Pequeno, mas afeta a pergunta de confirmação no app.
- **Pergunta sobre o prato em vez do item** (`plan-maionese`): "posso adicionar uma colher de maionese?" responde com o total do almoço (632 kcal), não com os ~90 kcal e a gordura da colher. 6/6.
- **"Pular o jantar pra compensar" vira `safety_support`** (`tone-duro-pular-ana`) em 4 de 6 respostas; nas outras, a crítica usa "compensar". A regra de segurança está disparando em um pedido comum de compensação; precisa de uma fronteira explícita (um jantar pulado não é jejum extremo).
- **Lata de 473 ml por padrão** (`photo-cerveja-lata`): a lata slim da foto é de 350 ml; o modelo assumiu 473 ml em 6/6 (210 kcal).
- **`meal_day` de um plano para amanhã** (`boost-amanha`): o prompt manda `today` para planos e o modelo obedece; o juiz e o server (`_protein_boost` só pula `other`) esperam `other`. Inconsistência de produto a decidir no Plano 2, não erro do modelo.

Falhas só em `new-none` (0/3 em `none`, 3/3 em `low`):

- Sete ações numa mensagem de sete refeições (limite é seis com aviso), 3/3.
- Ação `question` ao lado de um `log` (deveria ir em `estimate.question`), 3/3.
- Memória: não emite `replace` de P1 na preferência redeclarada, não `reinforce` D2 na ceia com marca, não grava as rotinas com macros na primeira abertura, não usa a frase "Minha memória fixa está cheia". 3/3 cada.
- Dia 2 da sequência: `reinforce` do café de sempre ausente em 2/3.

Regressões só em `new-low`:

- Perguntas desnecessárias em logs claros (manteiga, salada), 3/3 em dois casos.
- No dia nomeado ambíguo pergunta em vez de registrar com a dúvida na ação (o juiz preferiu a pergunta; a checagem exige o log).

Baseline: além das capacidades ausentes, errou a cópia de dia nomeado em 25 de 33 (copia o slot errado ou o dia errado), misturou refeições de uma mensagem numa estimativa só (multi 16,7 %) e não aplicou o tom `duro` com a crítica certa em 29 % dos casos de tom. Em nenhum braço houve menção a corpo ou peso, elogio, sugestão de pular refeição ou "registrei".

## Juiz (1.387 respostas, Codex, rubrica com CLOSING)

| Braço | respostas | pass / partial / fail | correctness | numbers | tone | format | usefulness | creativity (planos) |
|---|---:|---|---:|---:|---:|---:|---:|---:|
| baseline-none | 463 | 42,8 / 18,1 / 39,1 % | 3,25 | 3,60 | 4,27 | 4,83 | 2,75 | 2,51 |
| new-none | 459 | 69,1 / 17,6 / 13,3 % | 4,15 | 3,73 | 4,36 | 4,79 | 3,43 | 2,60 |
| new-low | 465 | 79,4 / 13,1 / 7,5 % | 4,42 | 4,08 | 4,41 | 4,82 | 3,71 | 3,43 |

Aprovação do juiz por família (baseline / none / low): workout 10 / 93 / 97 %; recipe 28 / 80 / 86 %; copy 49 / 85 / 91 %; multi 18 / 58 / 73 %; plan 28 / 65 / 75 %; memory 26 / 50 / 78 %; habit 79 / 70 / 92 %; photo 43 / 40 / 60 %; sequence 39 / 53 / 61 %; discovery 33 / 78 / 78 %; boost 38 / 29 / 67 %; tone 75 / 93 / 86 %; log 79 / 89 / 88 %; scope 61 / 67 / 72 %; digest 67 / 50 / 67 %.

Por tom: no `duro` o juiz aprova 36 / 66 / 80 % e dá nota de tom 3,43 / 3,68 / 3,83; no `seco` 45 / 70 / 79 % com tom 4,52 / 4,57 / 4,58. O tom duro continua a dimensão mais fraca dos três braços: a crítica existe, mas o juiz a acha genérica ou fora do número que quebrou o dia.

Flags por braço (baseline / none / low): "diz que gravou" 1,5 / 1,5 / 0,2 %; "pergunta o já sabido" 4,3 / 3,9 / 1,1 %; "totais do dia na prosa" 0,9 / 0,7 / 1,3 %; "reforço de proteína errado" 4,5 / 5,0 / 2,2 %; corpo/peso 0,4 / 0,2 / 0,0 %; elogio 0 / 0 / 0,2 %; markdown fora do subset 0,9 / 0,2 / 0,4 %.

Onde o juiz discorda das checagens: em `tone` ele aprova mais o `new-none` (92,9 %) que o `new-low` (85,7 %) porque o `low` escreve mais; em `log` prefere o `none` em utilidade (3,77 contra 3,21) pelas perguntas a mais do `low`. Nas famílias de capacidade nova (workout, recipe, copy, multi) juiz e checagens apontam a mesma ordem.

Uma rodada anterior pela API (153 + 21 respostas, rubrica sem a regra CLOSING, conta sem créditos no meio) foi descartada; ficou em `out/judge-20261008-113718-rubric1.jsonl` só como registro.

## Defeitos do benchmark encontrados nesta execução

Corrigidos e reavaliados sem nova chamada (commit desta data):

- Faixas de kcal inconsistentes com a TACO que o próprio prompt dá: "100 g de arroz, 100 g de feijão e 120 g de frango" soma 395 kcal e a faixa começava em 400 (três casos); omelete de 2 ovos com 2 fatias de pão integral e salada começava em 360 (um caso).
- Reforço: o item `(opcional)` era exigido no nome do item, mas a regra só pede a marca em `meal_text` e no reply; a checagem passou a ler a cláusula.
- `boost-amanha`: expectativa `meal_day=other` contradizia o prompt (planos são `today`); cobra-se só a ausência do reforço.
- `multi-skip-log-bruno`: a frase de fechamento do pulo ("Pré-treino de hoje fora") está na regra antiga `skip_slots`, que a regra ACTIONS nova substituiu sem copiar a frase. Checagem removida; **o Plano 2 precisa levar a frase para a regra nova.**

Para a próxima execução (não reavaliáveis):

- Textos ambíguos entre relato e plano em `boost-duro-ana` e `boost-vegetariano-diego` ("Jantar: ..." sem marcador de futuro).
- Rubrica do juiz sem a regra CLOSING (corrigida em `judge/judge_prompt.md` antes da rodada pelo Codex).
- Juiz pela API sem proteção contra conta sem créditos (corrigido: para na primeira `insufficient_quota`, continua com `--resume`); a rodada válida foi pelo Codex.

## Próximos passos

1. ADR para `reasoning.effort=low` no Chat (sucede a linha da constituição em `AGENTS.md` § LLM) com os números deste benchmark; o server só muda depois do plano aprovado. Incluir o feedback de espera no app para o p95 de 16 s.
2. ADR e plano do reforço híbrido (sucede ADR-043 decisão 4): regra PROTEIN BOOST do prompt novo como está em `prompts/new_instructions.py`, `protein_boost.py` como validador; aceite pela família `boost` deste benchmark.
3. Plano 2 (ações): limite de seis ações, pergunta dentro da ação, frase de fechamento do pulo, revisão sem acréscimo de item já registrado, fronteira entre "pular uma refeição" e sinal de segurança, `meal_day` de planos para outro dia.
4. Plano de memória: `replace`/`reinforce` explícitos com exemplos independentes (ADR-033) e a primeira abertura com macros.
5. Tom duro: a crítica precisa nomear a refeição que quebrou o dia e o número; é a dimensão com menor nota nos três braços.
