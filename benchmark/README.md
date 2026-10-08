# Benchmark do Chat — novo prompt × baseline, effort none × low

Medição única, fora do fluxo SDD: nada aqui muda `server/`, `apps/` ou `docs/`. O runner importa funções do server em modo leitura (contrato `ChatIn`, texto de entrada atual, janelas do ADR-043, schema atual) para que a baseline seja exatamente o que o server envia hoje. **Nenhuma execução foi feita ainda**: o owner dispara.

Origem: brainstorm e conclusão em [`../MELHORIAS_CHAT_07_10_2026.md`](../MELHORIAS_CHAT_07_10_2026.md) § 5.

## O que se mede

| Pergunta | Como |
|---|---|
| O novo prompt (estado final da Conclusão) resolve os casos que o owner listou? | 143 casos com checagens determinísticas sobre o JSON de saída (ações, slots, faixas de kcal, perguntas, memória, recusas, texto). |
| `reasoning.effort=low` melhora sobre `none` com o mesmo prompt, e a que custo? | Dois braços idênticos exceto o effort; tokens, latência e US$ por chamada. |
| Quanto do ganho é capacidade nova (ações, treino, receitas) e quanto é execução? | Braço baseline (prompt e schema atuais) pontuado com `det_supported`, que ignora o que ele não pode expressar. |
| Tom, formato, utilidade e criatividade | Juiz `gpt-6-astra` com rubrica fixa ([`judge/judge_prompt.md`](judge/judge_prompt.md)), nota 1–5 por dimensão e flags (corpo/peso, elogio, "registrei", pular refeição, totais do dia na prosa). |

## Braços e orçamento

| Braço | Prompt | Entrada | Schema | Effort |
|---|---|---|---|---|
| `baseline-none` | `prompts/baseline_prompt_{seco,duro}.md` (server, branch `meal_changes`) | `server/main._chat_text` | `server/llm.chat_format` | none |
| `new-none` | `prompts/new_prompt_{seco,duro}.md` | `prompts/input_format.md` | `prompts/new_schema.json` | none |
| `new-low` | idem | idem | idem | low |

143 casos × 3 repetições × 3 braços = **1.287 disparos** (teto 1.400; o runner recusa acima disso sem `--force`). Os 10 casos com foto só rodam se o arquivo existir em `media/` (ver [`media/README.md`](media/README.md)); sem as fotos são 133 casos = 1.197 disparos. O juiz é orçamento à parte (uma chamada por resposta, ~1.300).

Custo esperado das gerações, pela tarifa do `gpt-6-luna` em `server/evals/run.py`: entrada ~12 k tokens (prefixo de ~12 k chars cacheado a partir da segunda chamada do mesmo braço e tom), saída 200–500 (+ raciocínio no `low`): **≈ US$ 1,0–1,6** no total. Juiz: ~3,4 M tokens de entrada; abaixo de US$ 1 se a tarifa for a do luna; a do astra não está registrada no repositório.

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
  cases/*.json                143 casos (um disparo cada, por braço e repetição)
  scoring.py                  checagens determinísticas + mapeamento da baseline para a forma de ações
  run.py                      runner (dry-run disponível); resultados em out/ (fora do git)
  judge.py                    juiz LLM sobre um results.jsonl
  report.py                   agrega resultados e julgamentos em report-<stamp>.md
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
| plan | 23 | sem ideia (2 opções com id), "quanto de iogurte", "fiz a 2", opções no digest, gramas por item, maionese, "cabem quantos", buffet sem balança, dia honesto que estoura, pipoca × Doritos, ingredientes de casa, bruschetta, duas variações, janela zero, rótulo para amanhã |
| log | 15 | quantificado, sem porção, indisponível, rótulo em fato temporário, acréscimo, revisão, outro dia, só total, resposta a pergunta, ceia com marca, cerveja + espetinhos |
| photo | 10 | prato, cobertura visível, mamão, chocolate, cerveja, espetinhos, rótulo, pizza pronta com elogio, sem comida, energético |
| tone | 12 | duro: estouro, padrão da semana, isca de corpo, pular refeição, plano combinado, pulo e treino sem crítica; seco: sem crítica; elogio sem elogio de volta |
| memory | 9 | preferência redeclarada, trocada, rotina declarada com macros, esquecer, memória cheia, rótulo temporário, "o mesmo whey de sempre", equipamento, liked |
| discovery | 3 | primeira abertura: perguntas, resposta com rotinas, usuário pula |
| scope | 6 | matemática, override misturado, injeção na memória, sinal de segurança, poema + memória, identidade |
| digest | 2 | pergunta aberta só no digest, plano combinado só no digest |

Origem dos textos: 38 `grok` (suas mensagens das transcrições), 5 `real-owner` (suas mensagens no log do server de dev), 10 `synthetic-from-tester` (modo de falha dos testers reconstruído com outros alimentos e números; nenhum texto de tester copiado), 90 `synthetic`. Nenhum caso é exemplo de prompt (ADR-033).

Cada caso tem `expect.summary` (uma linha legível, também dada ao juiz) e checagens. Sequências multi-turno usam HISTORY fixo, nunca a resposta anterior do modelo: cada disparo é independente e repetível.

## Métricas (report.py)

- `det_strict`: todas as checagens determinísticas passam, por caso × repetição.
- `det_supported`: idem ignorando o que a baseline não consegue expressar (treino, receita, opções com id, macros na memória).
- `casos ≥ 2/3`: aprovação por maioria das repetições.
- Juiz: média 1–5 em correctness, numbers, tone, format, usefulness, creativity (só planos); pass/partial/fail; taxa de cada flag.
- `composite` = 0,5 × det_strict + 0,5 × (média de correctness, tone e usefulness − 1) / 4.
- Latência p50/p95, tokens de entrada (cacheados), saída (+ raciocínio), US$ por chamada.
- Quebra por família, origem e tom; os 12 piores casos de cada braço com a checagem que mais falhou.

Critério de leitura sugerido: `new-low` justifica a mudança de constituição (`effort=none`) só se ganhar ≥ 5 pontos percentuais de `det_strict` ou ≥ 0,3 de correctness sobre `new-none` nas famílias multi, copy e plan, com p95 abaixo de 8 s e custo abaixo do dobro.

## Como rodar (quando o owner autorizar)

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
python benchmark/judge.py benchmark/out/results-<stamp>.jsonl
```

```bash
python benchmark/report.py benchmark/out/results-<stamp>.jsonl benchmark/out/judge-<stamp>.jsonl
```

Requisitos: `OPENAI_API_KEY` no ambiente (nunca no repositório), `openai` e `pydantic` instalados (os do server), `Pillow` opcional para as fotos. `out/` é ignorado pelo git.

## Limites conhecidos

- O braço baseline recebe exatamente o texto do server, mas sem moderação, shaping (`reply_format`, `estimate_total`, `protein_boost`) e sem o `adjust_retry`; mede o modelo, não a rota.
- O novo prompt é um alvo de benchmark, não o prompt de produção: as regras novas não passaram pelo registro de proveniência do server nem pelos evals existentes.
- `COPY_SOURCE` é resolvido por regex simples (ver `input_format.md`); o Plano 1 pode escolher outra implementação.
- O juiz é um modelo: suas notas servem para comparar braços entre si, não como medida absoluta.
- Fotos dependem de arquivos que o owner fornece; sem eles os 10 casos são pulados.
