# Benchmark do Chat — novo prompt × baseline, effort none × low

Medição única, fora do fluxo SDD: nada aqui muda `server/`, `apps/` ou `docs/`. O runner importa funções do server em modo leitura (contrato `ChatIn`, texto de entrada atual, janelas do ADR-043, schema atual) para que a baseline seja exatamente o que o server envia hoje. Primeira execução em 08/10/2026 com autorização do owner: resultados e veredito em [`RESULTADOS_08_10_2026.md`](RESULTADOS_08_10_2026.md).

Origem: brainstorm e conclusão em [`MELHORIAS_CHAT_07_10_2026.md`](MELHORIAS_CHAT_07_10_2026.md) § 5 (numeração final dos planos em § 5.6); resultados da primeira execução em [`RESULTADOS_08_10_2026.md`](RESULTADOS_08_10_2026.md).

## O que se mede

| Pergunta | Como |
|---|---|
| O novo prompt (estado final da Conclusão) resolve os casos que o owner listou? | 155 casos com checagens determinísticas sobre o JSON de saída (ações, slots, faixas de kcal, perguntas, memória, recusas, texto). |
| `reasoning.effort=low` melhora sobre `none` com o mesmo prompt, e a que custo? | Dois braços idênticos exceto o effort; tokens, latência e US$ por chamada. |
| Quanto do ganho é capacidade nova (ações, treino, receitas) e quanto é execução? | Braço baseline (prompt e schema atuais) pontuado com `det_supported`, que ignora o que ele não pode expressar. |
| O reforço de proteína de um prato nomeado (ADR-043 decisão 4) pode sair do server e ir para o modelo? | Família `boost` (8 casos): a regra entra no prompt novo com alimentos livres e variados; checagem determinística dos itens `(opcional)` (presentes só quando devidos, no máximo dois, com gramas e kcal, prato dentro da janela, proteína até o piso, sem carne para vegetariano, nunca para outro dia) e flag `protein_boost_wrong` do juiz. A baseline não tem a regra (o server a aplica em código depois do modelo): conta em `det_supported`. |
| Tom, formato, utilidade e criatividade | Juiz `gpt-6-astra` com rubrica fixa ([`judge/judge_prompt.md`](judge/judge_prompt.md)), nota 1–5 por dimensão e flags (corpo/peso, elogio, "registrei", pular refeição, totais do dia na prosa). |

## Braços e orçamento

| Braço | Prompt | Entrada | Schema | Effort |
|---|---|---|---|---|
| `baseline-none` | `prompts/baseline_prompt_{seco,duro}.md` (server, branch `meal_changes`) | `server/main._chat_text` | `server/llm.chat_format` | none |
| `new-none` | `prompts/new_prompt_{seco,duro}.md` | `prompts/input_format.md` | `prompts/new_schema.json` | none |
| `new-low` | idem | idem | idem | low |

155 casos × 3 repetições × 3 braços = **1.395 disparos** (teto 1.400; o runner recusa acima disso sem `--force` e para em `--max-usd`, padrão US$ 5). Os 10 casos com foto só rodam se o arquivo existir em `media/` (ver [`media/README.md`](media/README.md)); sem as fotos são 145 casos = 1.305 disparos. Tentativas por falha de rede ou limite de taxa (até 3 por disparo) são contadas no resultado, não no teto. O juiz é orçamento à parte (uma chamada por resposta, até 1.395).

Custo esperado das gerações, pela tarifa do `gpt-6-luna` em `server/evals/run.py` (US$ 0,10 / 0,01 cacheado / 0,50 por milhão): o prefixo tem ~43–50 mil caracteres (~12 mil tokens) e a entrada por caso mais 2–6 mil; saída 200–500 (+ raciocínio no `low`). **Entre US$ 0,5 (prefixo cacheado como nos evals do S19, 96 %) e US$ 1,9 (sem cache algum)**; o relatório imprime o custo medido. Juiz: ~5 M tokens de entrada com o contexto completo e as fotos; a tarifa do `gpt-6-astra` não está no repositório e precisa ser passada por ambiente.

## Estrutura

```
benchmark/
  README.md                   este arquivo
  prompts/
    new_instructions.py       regras novas + regras reaproveitadas do server (módulo)
    new_prompt_seco.md        texto integral enviado (gerado por render.py)
    new_prompt_duro.md
    baseline_prompt_seco.md   texto integral do server hoje (gerado)
    baseline_prompt_duro.md
    new_schema.json           saída estruturada do novo prompt (actions[])
    input_format.md           blocos de entrada do novo prompt + regra do COPY_SOURCE
    render.py                 regenera os .md
  personas/*.json             6 perfis (1 real: o owner; 5 inventados), com memória, semana típica, receitas
  build_cases.py              gera cases/ a partir de personas × cenários (determinístico)
  cases/*.json                155 casos (um disparo cada, por braço e repetição)
  scoring.py                  checagens determinísticas + mapeamento da baseline para a forma de ações
  run.py                      runner (dry-run disponível); resultados em out/ (fora do git)
  judge.py                    juiz LLM sobre um results.jsonl
  report.py                   agrega resultados e julgamentos em report-<stamp>.md
  rescore.py                  reavalia um results.jsonl com o scoring e os casos atuais, sem chamada
  judge_export.py             exporta os julgamentos como arquivos para um agente (Codex) julgar
  judge_import.py             valida e junta os vereditos do agente num judge-<stamp>-codex.jsonl
  RESULTADOS_08_10_2026.md    resultados e veredito da primeira execução
  judge/                      rubrica e schema do juiz
  media/                      fotos (não versionadas) para os 10 casos com imagem
```

## Casos

Gerados por `build_cases.py` (rodar de novo recria `cases/`). Famílias e contagem:

| Família | n | O que cobre |
|---|---:|---|
| habit | 8 | "de sempre" com rotina (macros no MEMORY), sem rotina com dois dias iguais, sem fonte, variação da rotina |
| copy | 10 | "de ontem", "de anteontem", dia da semana, semana passada, fonte ausente, fonte ambígua |
| multi | 17 | dia inteiro numa mensagem, pulo + refeição, refeição + plano, treino + refeição, 2 logs + pulo, extra sem slot, clarificação só na ação com dúvida |
| sequence | 6 | o "dia 2" do Grok como sequência roteirizada (treino, café de sempre, correção, almoço, farinha de sementes, chocolate) |
| workout | 10 | kcal declaradas, somar × substituir, sem número, distância, pulseira, fora de escopo |
| recipe | 12 | pedido de receita, lembrar por nome, ambígua, inexistente, comer receita salva (com e sem mudança), adaptar, "ficou boa", "salva" |
| plan | 20 | sem ideia (2 opções com id), "quanto de iogurte", "fiz a 2", opções no digest, gramas por item, maionese, "cabem quantos", buffet sem balança, dia honesto que estoura, pipoca × Doritos, ingredientes de casa, bruschetta, duas variações, janela zero, rótulo para amanhã |
| boost | 8 | reforço de proteína de um prato nomeado feito pelo modelo: com folga, prato que já tem frango (outro alimento), acima do piso, sem folga, vegetariano, tom duro, "tá bom?", outro dia |
| log | 15 | quantificado, sem porção, indisponível, rótulo em fato temporário, acréscimo, revisão, outro dia, só total, resposta a pergunta, ceia com marca, cerveja + espetinhos |
| photo | 10 | prato, cobertura visível, mamão, chocolate, cerveja, espetinhos, rótulo, pizza pronta com elogio, sem comida, energético |
| tone | 12 | duro: estouro, padrão da semana, isca de corpo, pular refeição, plano combinado, pulo e treino sem crítica; seco: sem crítica; elogio sem elogio de volta |
| memory | 9 | preferência redeclarada, trocada, rotina declarada com macros, esquecer, memória cheia, rótulo temporário, "o mesmo whey de sempre", equipamento, liked |
| discovery | 3 | primeira abertura: perguntas, resposta com rotinas, usuário pula |
| scope | 6 | matemática, override misturado, injeção na memória, sinal de segurança, poema + memória, identidade |
| digest | 2 | pergunta aberta só no digest, plano combinado só no digest |

Origem dos textos (contagem impressa por `build_cases.py`): `grok` (suas mensagens das transcrições), `real-owner` (suas mensagens no log do server de dev, instalações `v1_ec1fea060`, `v1_9615563db` e `v1_d7fd00baf`), `synthetic-from-tester` (modo de falha dos testers reconstruído com outros alimentos, números e frases; nenhum texto de tester copiado), `synthetic`.

Autorização e limite: o uso das suas próprias mensagens neste benchmark foi decidido por você em 08/10/2026 (pergunta e resposta na sessão). O ADR-033 governa exemplos globais do prompt e fixtures dos evals do server: **nenhum caso desta pasta pode ser copiado para `server/evals/` nem para `server/chat_instructions.py`**, e os exemplos dentro das regras novas de `prompts/new_instructions.py` são de autoria independente (sem alimentos, números ou frases das transcrições). Famílias adicionadas depois da revisão: par seco/duro no mesmo contexto, treino seguido de plano com crédito, limite de seis ações, pulo e refeição no mesmo slot, refeição num slot reservado, dois dias nomeados numa mensagem. Em 08/10/2026 o owner pediu o reforço de proteína pelo modelo (família `boost`, 8 casos); para caber no teto de 1.400 disparos saíram três duplicatas por persona do mesmo texto (`plan-sem-ideia-ana`, `plan-fiz-a-2-ana`, `plan-cabe-quanto-ana`), cobertas pelas outras personas.

Cada caso tem `expect.summary` (uma linha legível, também dada ao juiz) e checagens. Sequências multi-turno usam HISTORY fixo, nunca a resposta anterior do modelo: cada disparo é independente e repetível.

## Métricas (report.py)

- `det_strict`: todas as checagens determinísticas passam, por caso × repetição. Inclui invariantes estruturais dos braços novos (1–6 ações, ids únicos, campos coerentes com o tipo). Regex de texto sem distinção de acento (o server escreve `terca`, os casos `terça`).
- `det_supported`: idem ignorando o que a baseline não consegue expressar (treino, receita, opções com id, macros na memória, contagem de várias ações, escopo de treino, reforço de proteína pelo modelo), listado por caso em `expect.baseline_unsupported`.
- `items_sum` (kcal da estimativa = soma dos itens) é checagem branda: o server recalcula (ADR-042); aparece no relatório, não conta na aprovação.
- `casos ≥ 2/3`: aprovação por maioria das repetições.
- Juiz: média 1–5 em correctness, numbers, tone, format, usefulness, creativity (só planos); pass/partial/fail; taxa de cada flag.
- `composite` = 0,5 × det_strict + 0,5 × nota do juiz (média de correctness, numbers, tone, format e usefulness escalada para 0–1; **zero** na resposta que tiver flag grave: corpo/peso, pular refeição, diz que gravou).
- Comparação pareada: só os pares caso × repetição julgados em todos os braços; cobertura do juiz por braço.
- Latência p50/p95, tokens de entrada (cacheados), saída (+ raciocínio), US$ por chamada, tentativas, JSON inválido.
- Quebra por família, origem e tom; os 12 piores casos de cada braço com a checagem que mais falhou.

Critério de leitura sugerido: `new-low` justifica a mudança de constituição (`effort=none`) só se ganhar ≥ 5 pontos percentuais de `det_strict` ou ≥ 0,3 de correctness sobre `new-none` nas famílias multi, copy e plan, com p95 abaixo de 8 s e custo abaixo do dobro.

## Como rodar

```bash
python benchmark/build_cases.py
```

```bash
python benchmark/prompts/render.py
```

```bash
python benchmark/run.py --dry-run
```

```bash
python benchmark/run.py --arm baseline-none new-none new-low --repeat 3
```

```bash
JUDGE_PRICE_INPUT=<usd/1M> JUDGE_PRICE_OUTPUT=<usd/1M> python benchmark/judge.py benchmark/out/results-<stamp>.jsonl
```

```bash
python benchmark/report.py benchmark/out/results-<stamp>.jsonl benchmark/out/judge-<stamp>.jsonl
```

Depois de corrigir uma checagem ou uma expectativa, `python benchmark/rescore.py benchmark/out/results-<stamp>.jsonl` reavalia o arquivo com o `scoring.py` e os `cases/` atuais sem nova chamada (gera `-rescored.jsonl`, uma linha por braço × caso × repetição, descartando as linhas de erro que um `--resume` substituiu). O juiz aceita `--resume` para continuar um arquivo `judge-<stamp>.jsonl` interrompido.

Juiz por agente em vez de API (decisão do owner em 08/10/2026, para usar a assinatura e não a tarifa do astra): `python benchmark/judge_export.py benchmark/out/results-<stamp>-rescored.jsonl` grava um `.md` por resposta em `out/judge-jobs/` (rubrica inteira + a mesma entrada do `judge.py`, foto ao lado quando há), com id opaco; o mapa id → braço fica fora da pasta, para o juiz seguir cego. O agente escreve `<id>.json` no formato de `judge/judge_schema.json`; `python benchmark/judge_import.py <stamp>` valida e junta tudo em `out/judge-<stamp>-codex.jsonl` para o `report.py`.

Requisitos: `OPENAI_API_KEY` no ambiente ou no `.env` da raiz (lido por `server/config.load_settings`, como os evals do server; nunca impresso), `openai` e `pydantic` instalados (os do server), `Pillow` opcional para as fotos. `out/` é ignorado pelo git.

## Limites conhecidos

- O braço baseline recebe exatamente o texto do server, mas sem moderação, shaping (`reply_format`, `estimate_total`, `protein_boost`) e sem o `adjust_retry` / `BUDGET_TARGET`; mede o modelo, não a rota. O reforço de proteína dos pratos nomeados (ADR-043 decisão 4) é código do server hoje (`server/protein_boost.py`, depois do modelo); por decisão do owner em 08/10/2026 o benchmark o cobra do modelo no prompt novo (regra PROTEIN BOOST em `prompts/new_instructions.py`, família `boost`), com alimentos livres em vez dos três fixos do server. A baseline, sem a regra, é pontuada à parte em `det_supported`. O server não muda; a janela e o piso usados nas checagens vêm das mesmas funções de `server/meal_window.py`.
- Cada disparo é independente: sequências usam HISTORY fixo escrito à mão. O benchmark não mede continuidade entre respostas geradas, aplicação de lotes no app, Desfazer nem rodadas de clarificação acumuladas.
- A baseline recebe menos contexto (sem macros da rotina, RECENT_DAYS, RECIPES, COPY_SOURCE) e outro schema: a diferença para os braços novos é prompt + contexto + schema juntos; só `new-none` × `new-low` isola o effort.
- O novo prompt é um alvo de benchmark, não o prompt de produção: as regras novas não passaram pelo registro de proveniência do server nem pelos evals existentes.
- `COPY_SOURCE` é resolvido por regex simples (ver `input_format.md`); o Plano 1 pode escolher outra implementação.
- O juiz é um modelo: suas notas servem para comparar braços entre si, não como medida absoluta.
- Fotos dependem de arquivos que o owner fornece; sem eles os 10 casos são pulados.

## Revisões

### GPT-6 (effort: não exposto)

Revisão estática em 08/10/2026. Identificação disponível nesta sessão: GPT-6; effort não informado. Li os arquivos na ordem solicitada, as seis personas, 20 casos das famílias exigidas e os comparativos do servidor. Nenhum script, teste, dry-run ou modelo foi executado.

**1. Validade**

**Executar com correções.** A estrutura é aproveitável: separa braços, guarda respostas brutas, varia perfis e repete casos. Contudo, hoje mede aderência de uma geração a expectativas parcialmente defeituosas, não a entrega integral da seção 5.

Há cobertura nominal de ações múltiplas, cópia, treino, receitas, descoberta e tom. Faltam dependências executáveis, aplicação/Desfazer de lotes, rodadas por refeição, receita versionada com rendimento/porção e descoberta seguida de uso real da memória. HISTORY roteirizado não mede continuidade entre respostas geradas.

Não sustenta “o modelo não é o gargalo”, confiabilidade do produto, superioridade sobre Grok nem melhora atribuível exclusivamente ao prompt: a baseline recebe menos informação e outro schema. Apenas new-none versus new-low isola effort. O avaliador atual passa por `chat_reply`; este usa saída bruta, sem soma, reforço proteico, retenção de estimativa, fechamento corrigido ou ajuste. Isso limita inclusive a comparação de utilidade com o servidor atual.

**2. Prompt novo**

Em [new_instructions.py](prompts/new_instructions.py):

- L55–72: “in the order the user stated them” conflita com skips “after the other actions”. ADR-047 determina log primeiro, skips em ordem do perfil e log prevalecendo no mesmo slot; a montagem também omite a regra `skips` atual.
- L33 restringe treino a kcal declaradas, mas L174 pede esclarecer treino sem número. L64–79 exige objeto para log e pergunta em `estimate.question`; L94/102 manda `estimate null` sem fonte. Falta representação coerente da pendência. ADR-026 exige controle por refeição, ausente neste contrato.
- L66–68 manda subtrair logs; L179 diz que treino “never changes any estimate”. Não há tratamento consistente de revisão versus delta, refeição pendente e atualização de reservas. `WINDOWS/BUDGET` são calculados antes do lote (`run.py`, L196). ADR-043 exige aritmética determinística.
- L124 cita “PROTEIN BOOST below”, inexistente; no servidor essa complementação é código. ADR-039/043 ficam sem cobertura equivalente, inclusive `BUDGET_TARGET`, que o novo serializador não envia.
- L192 manda copiar ingredientes do índice mesmo sem receita completa; o índice só possui ingredientes-chave. O schema não traz receita estruturada, versão, rendimento ou porção.
- L208 pede “source declared”, proibido pelo schema fechado, que não declara `source`. Rotina declarada sem registro precisa da mudança prevista sobre ADR-023. L216 exige macros nulos fora de rotina; L219–220 exige números em `liked`.
- Descoberta exige perguntas em bullets (L203), mas FORMAT proíbe marcadores em resposta apenas de pergunta (prompt renderizado, L84): conflito com ADR-045.
- WEEK PATTERN (L223) permite crítica de dias incompletos marcados como registrados; não distingue ausência de registro de alimentação insuficiente. Isso enfraquece ADR-044 e a promessa da seção 5. ADR-046 é importado, mas suas transições e diferenças numéricas não são verificadas.

O prefixo renderizado tem 48.393 caracteres. Ordenar ações, simular estado e resolver essas contradições é carga adicional; sem execução, não afirmo que `none` necessariamente falhe.

**3. Casos e proveniência**

Falhas concretas em [build_cases.py](build_cases.py):

- L257 aceita ±5% numa cópia que deveria preservar kcal/P/C/G e alimentos exatamente.
- L311 impõe “café da tarde” → Jantar sem regra inequívoca; L321 afirma que só jantar tem dúvida, embora a pasta de amendoim também esteja sem porção.
- L337 exige ausência de pergunta apesar de legumes sem quantidade; precisa justificar materialidade.
- L549 não cria janela zero: Elisa tem 480 kcal restantes e 330 reservadas, deixando **150**.
- L632/640: `gord` rejeita “gordura”; L547: `clara` rejeita “sem separar a clara”. São falsos negativos.
- [carla.json](personas/carla.json), L43: receita anuncia 310 kcal, itens somam 305; [nicolas.json](personas/nicolas.json), L69: 470 versus 537. Copiar tudo e somar corretamente tornam-se incompatíveis.

Treino simples de Nicolas repete a sequência; várias duplicações por persona não constituem modos de falha independentes. Nicolas concentra 58/143 casos. Faltam pares com os dois tons no mesmo contexto, múltiplas referências de dias, treino seguido de plano com crédito, limite de seis ações, rodadas/forçar, conflito log/skip e consumo/substituição de reserva.

A origem declarada confere numericamente: 38 Grok, cinco real-owner, dez synthetic-from-tester, 90 synthetic. A seção 5.2 proíbe expressamente transformar suas transcrições em fixtures; L303 e L434 reproduzem mensagens dela. O ADR-033 distingue contexto pessoal de exemplo global: o problema não é simplesmente enviar um fixture como entrada.

Há exemplos concretos dentro das regras sem proveniência declarada: treino de 610 kcal (L169), “pipoca ou Doritos?” (L125) e “ficou muito boa”/“entra pra lista” (L218) sobrepõem-se às transcrições. Não basta chamar exemplos de regra; precisam de auditoria e autoria independente. Vocabulário comum isolado não prova infração.

A auditoria dos logs locais encontrou a pergunta “Com faz uma equação do segundo grau?” literalmente, sem autoria identificável; pulo+café coincide com o eval S29; o poema rotulado real-owner coincide com `cp2-smoke`. Essas origens não certificam ausência de texto de tester. Corrigir a rastreabilidade e substituir reproduções por situações sintéticas independentes.

**4. Pontuação**

[scoring.py](scoring.py), L140–159: `det_supported` continua cobrando contagem de múltiplos logs e scope de treino da baseline, embora ela não os suporte. Inversamente, marcar a ação inteira como unsupported por conter `options` ou `recipe_id` dispensa também kcal/slot que seriam verificáveis. Separar capacidade, informação disponível e execução em checks individuais, com denominadores explícitos.

O pareamento ignora ordem; não verifica soma, macros copiados, orçamento pós-lote, delta completo, igualdade slot/suggested_slot ou ids únicos. `question absent` aceita estimativa ausente; `has_macros` só verifica kcal. Regex não normaliza acentos: baseline recebe “terca”, enquanto casos exigem “terça”. Reaproveitar princípios de `server/evals/checks.py`: verificações positivas, números finitos, conservação do delta, formato e variabilidade entre repetições.

[judge.py](judge.py), L28–41: juiz não recebe fotos, RECENT/COPY_SOURCE, receitas, digests, atualizações de memória nem ids dos slots; corta fatos em 12 e histórico em seis. Não consegue verificar o que sua rubrica exige. Omitir o braço ajuda, mas o schema revela capacidades.

“Careful human coach”, lacuna e próximo passo podem premiar elaboração desnecessária. Definir concisão por tarefa, exceções de tom e penalidade por conteúdo supérfluo; calibrar com avaliação humana. O composite (`report.py`, L84) ignora numbers/format e não veta flags graves. Publicar dimensões separadas, falhas críticas, cobertura do juiz e comparação pareada; `--sample` hoje sorteia casos diferentes entre braços.

**5. Orçamento e execução**

143 × 3 × 3 = **1.287**; sem dez imagens, **1.197**. Os dez arquivos existem localmente. Julgar todas as respostas adiciona até 1.287: **2.574 chamadas lógicas**, antes de retries.

As tarifas coincidem com `server/evals/run.py`, L48–50: US$0,10/0,01/0,50 por milhão. Com 12 mil tokens de entrada e 200–500 de saída, gerações custariam US$1,67–1,87 sem cache ou US$0,28–0,48 com entrada totalmente cacheada, antes do raciocínio adicional. US$1,0–1,6 é possível, mas não demonstrado. O README confunde prefixo de 12 mil caracteres com tokens; cache desde a segunda chamada não é garantido, especialmente com schemas variáveis.

`run.py`, L293/387: quatro tentativas externas somam-se aos dois retries padrão do SDK instalado; o teto conta jobs, não tentativas. L316 apaga a espera da latência; JSON inválido retorna `error=None`. Faltam retomada, manifesto/hash de entradas, limite de gasto/tokens e contabilização de erros.

Schema estrito não garante invariantes: sem limite de ações, campos condicionais ou validação local; L230 libera ids quando enums esvaziam. Fotos são reduzidas e têm EXIF removido, mas faltam correção de orientação, guarda de 16 MB e hash/proveniência. Imports do servidor podem criar `__pycache__` fora da pasta. O juiz usa tarifa Luna por padrão para Astra: deve exigir tarifa explícita.

**6. Correções prioritárias e execução proposta**

1. **P0:** proveniência e expectativas: `benchmark/build_cases.py:303`, L549/L632; `benchmark/personas/nicolas.json:69`.
2. **P0:** contrato e regras coerentes: `benchmark/prompts/new_instructions.py:55`, L192/L200; `benchmark/prompts/new_schema.json:15`.
3. **P0:** justiça e invariantes: `benchmark/scoring.py:60`, L140; contexto: `benchmark/judge.py:28`; rubrica: `benchmark/judge/judge_prompt.md:13`.
4. **P1:** execução reproduzível: `benchmark/run.py:293`, L390; amostragem pareada: `benchmark/judge.py:58`; relatório: `benchmark/report.py:84`.

Depois das correções: **baseline-none → new-none → new-low**, rotacionando essa ordem por caso/repetição. Usaria **60 casos sintéticos estratificados × 3 repetições × 3 braços = 540 gerações**, mais **180 julgamentos pareados** (uma repetição predefinida por caso/braço): **720 disparos lógicos**, com tentativas limitadas e contabilizadas separadamente.
