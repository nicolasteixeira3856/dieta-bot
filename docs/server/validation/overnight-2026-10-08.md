# Execução autônoma — 07 para 08/10/2026

Relatório da execução autônoma pedida pelo dono em 07/10/2026, pelo [runbook](../../sdd/autonomous-run.md). Ordem aprovada: [S30](../plans/completed/s30-tone-formatting-planned-slot.md) (partes A → B → C), depois [A60](../../android/plans/completed/a60-tone-formatting-planned-skips.md) (partes A → D; a parte E foi entregue pelo A59).

Concluída em 07/10/2026. Nenhuma parte ficou de fora; nenhuma validação falhou duas vezes.

## Resultado

| Plano | Partes | Estado final | PR |
|---|---|---|---|
| [S30](../plans/completed/s30-tone-formatting-planned-slot.md) — tom, formatação e refeição planejada (servidor) | A → B → C, todas entregues | `Concluído` | [#174](https://github.com/nicolasteixeira3856/dieta-bot/pull/174) |
| [A60](../../android/plans/completed/a60-tone-formatting-planned-skips.md) — escolha acima da janela, tom e fechamentos, respostas formatadas, reserva (app) | A → B → C → D, todas entregues; E entregue pelo A59, aceite manual pendente | `Pendente aprovação manual` | [#175](https://github.com/nicolasteixeira3856/dieta-bot/pull/175) |

- **Deploy:** S30 no servidor dev com `tools/deploy-gcp.ps1` (código do head do branch, igual ao mergeado), `GET /health` 200; smoke de cada parte com request ids no Results do S30. Nada em produção.
- **Versão distribuída:** `0.0.18-dev` (versionCode 18, tag `dev-v0.0.18`) pelo Firebase App Distribution ao grupo `testers`, com as notas em pt-BR do A60 e dos pulos do A59 (o A59 ainda não tinha sido distribuído).
- **Room:** v12, uma migração para as quatro partes.
- **Sessão no servidor dev com o app (A60):** uma por parte, mensagens sintéticas; request ids no Results do A60, item 7. Uma chamada de outro aparelho (versão 0.0.17-dev de um testador) apareceu no log no mesmo minuto e foi descartada pelo `safety_identifier`.
- **Defeito achado e corrigido no caminho:** as cores de P/C/G sumiam nas respostas formatadas (o gate JVM tolerava); a captura no emulador mostrou, o `AeroReply.kt` foi corrigido antes do PR.
- **Avaliação:** só o S30 avaliou (o A60 não lista avaliação). Total US$ 0,0664 de US$ 0,50, nunca perto do limite de US$ 0,40. Sem suíte completa, sem pilot runner, sem repetição para desempate.

## Aceites manuais que ficam com você

Num aparelho com a 0.0.18-dev:

1. **A59 (pulos):** mandar um pulo junto com uma refeição ("pulei o almoço, no café comi …") e pular uma refeição que já tem registro (Excluir e pular / Manter registro).
2. **Tom:** escolher Duro (na O5 de um cadastro novo ou em Configurações → Tom da Tali) e ver uma resposta que nomeia o estouro; Seco fica só nos números.
3. **Fechamentos:** às 22h chega a notificação e o cartão do dia (no domingo também o da semana); no dia seguinte o cartão recolhe depois do primeiro registro. Permitir notificações quando o Android pedir.
4. **Acima da janela:** um plano que passa do dia mostra Pode passar · Ajustar para caber.
5. **Reserva:** Reservar para o Jantar num plano, a Home mostra `planejado`, registrar o jantar mostra a diferença no recibo.
6. **Revisão visual no Figma** das divergências de gold (Results do A60, item 6): `chatRB` do D12 desenhado antes dos blocos do D17; `o1`–`o4` ainda com `n/4` e o CTA da O4; `cfgS` sem a linha do tom; `cfgT` claro sem o desfoque do sheet; `chatR`/`chatE` escuros com o primeiro parágrafo invisível.

O PG6 do [production gate](../../content-policy/production-gate.md) continua aberto até os itens 2 e 3 no build dev.

## Observações

- Sentinela do S30 (uma passada): 31/38, falhas registradas no Results do S30; vigiar o par de receitas, que pode perder os itens `(opcional)` com a tabela.
- `s25-log-estouro-duro` terminou em 2/3 no S30.
- Numa sessão do dev, a linha de fechamento do servidor para um lanche em aberto num dia já cheio saiu como `Lanche: a definir ~13 kcal · P 34` (ADR-043); comportamento do servidor, não do app — vale um olhar.

## Custo na OpenAI (teto da sessão: US$ 0,50; S30: US$ 0,30; parar de avaliar em US$ 0,40)

| Relatório (`logs/evals/`) | O que rodou | Custo | Acumulado |
|---|---|---|---|
| `2026-10-07-123535-none.json` | S30-A `--tag s25 --repeat 3` | US$ 0,0073 | US$ 0,0073 |
| `2026-10-07-123605-none.json` | S30-A dois casos com expectativa corrigida, `--repeat 3` | US$ 0,0008 | US$ 0,0081 |
| `2026-10-07-123731-none.json` | S30-A `--tag s25 --repeat 3` após a correção 1 do tom | US$ 0,0073 | US$ 0,0154 |
| `2026-10-07-123803-none.json` | S30-A `--tag cp2 --moderation all --repeat 1` (única passada) | US$ 0,0041 | US$ 0,0195 |
| `2026-10-07-124349-none.json` | S30-B `--tag s26 --repeat 3` | US$ 0,0040 | US$ 0,0235 |
| `2026-10-07-124510-none.json` | S30-B `--tag s26 --repeat 3` após a correção 1 do formato | US$ 0,0039 | US$ 0,0274 |
| `2026-10-07-124951-none.json` | S30-C `--tag s27 --repeat 3` | US$ 0,0040 | US$ 0,0314 |
| `2026-10-07-125120-none.json` | S30 sentinela `--tag s22 --tag s23 --tag s24 --tag recipe --repeat 1` (única) | US$ 0,0204 | US$ 0,0518 |
| `2026-10-07-125801-none.json` | S30-A `--tag s25 --repeat 3` após a correção 2 do tom (achado do smoke) | US$ 0,0073 | US$ 0,0591 |
| `2026-10-07-125952-none.json` | S30-A `--tag s25 --repeat 3` após a última correção do tom | US$ 0,0073 | US$ 0,0664 |

Fora do ledger (chave do servidor dev, sem relatório): dois smokes do S30, cerca de 16 chamadas, ≈ US$ 0,006 estimado.
