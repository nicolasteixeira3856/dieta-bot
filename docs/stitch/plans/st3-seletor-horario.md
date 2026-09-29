# Gate Stitch — ST3 Seletor de horário em rodas

- Estado: Aguardando o dono no Stitch
- Data: 29/09/2026
- Contexto proprietário: `stitch`
- Projeto: Stitch `Nutri` (`6282733070135794645`)
- Executa: [ADR-020](../../produto/adrs/ADR-020-estados-novos-chat-home-horario.md)
- Bloqueia: [A21 Seletor de horário](../../android/plans/a21-seletor-horario.md), e é pré-requisito do [ST4](st4-refeicoes-por-dia.md).

## ⛔ Bloqueio do dono — prompt para o Stitch

Este passo é manual e trava o A21. É um prompt, rodado uma vez por tema.

### Prompt 3.1 — nova tela `o3t`: diálogo com rodas de hora e minuto

Antes de enviar: **duplique** a `o3` do tema (dark `14440390e2d94582af1efa4e8aa4573b`, light `87f1e6f1b9334faeae8f40f6cebf56e5`) e selecione só a cópia. Opcional: anexe um print do alarme da Samsung (One UI) como referência de estilo das rodas.

```text
Show a centered dialog open over this "Distribuição das refeições" screen (dim the screen behind it with the usual scrim). The user tapped the "07:30" time field of "Café da manhã".

Dialog: surface background, 28px corner radius, 1px line border, 24px padding, width = screen width minus 48px.
1. Title "Café da manhã" (18px, semibold, primary text) and, below it, "Horário da refeição" (13px, muted).
2. A time wheel picker, like the Samsung One UI alarm clock: two vertical scroll wheels side by side, "hours" (00–23) and "minutes" (00–59), separated by a large ":".
   - The selected row is centered and highlighted: value 40px, semibold, primary text color, inside a horizontal band with surface2 background and 12px radius that spans both wheels.
   - Two values above and two below the selected one, fading out: 28px at 50% opacity, then 22px at 25% opacity.
   - Show hours 05, 06, [07], 08, 09 and minutes 28, 29, [30], 31, 32.
   - 24-hour format, no AM/PM.
3. Two buttons side by side at the bottom, same width, same 48px height, same text size (16px, semibold): "Cancelar" (outlined, 1px line border, primary text) and "OK" (primary CTA fill, like the "Continuar" button).

No clock dial, no keyboard icon, no other text. Keep the screen behind the dialog unchanged.
```

Depois de enviar: renomeie a tela para `o3t — dark` ou `o3t — light`.

## Verificação (agente)

1. `mcp__stitch__list_screens`: `o3` com o mesmo ID; `o3t` dark e light existem (IDs no Registro).
2. `tools/export-stitch.mjs` + `o3t`; `tools/check-stitch.mjs`: contagem +1 por tema.
3. `node tools/export-stitch.mjs` e `node tools/check-stitch.mjs` verdes.
4. Checklist dark e light. Falhou: **para** e reporta ao dono o que falta.

### Checklist

- [ ] Diálogo centralizado sobre a O3 com scrim.
- [ ] Duas rodas (hora 00–23, minuto 00–59) com ":" no meio, 24 h.
- [ ] Faixa de seleção única atravessando as duas rodas; valor central grande; vizinhos esmaecidos.
- [ ] Sem mostrador de relógio e sem ícone de teclado.
- [ ] "Cancelar" e "OK" lado a lado, mesmo tamanho e mesma fonte.
- [ ] Tokens do `AGENTS.md`; dark e light coerentes.

Checklist verde: lista de golds com `o3t`, estado `Concluído`, mover para `completed/`, índices e entrega git (`docs/st3-stitch-seletor-horario`).

## Arquivos que este gate pode tocar

- `docs/qa/stitch/{dark,light}/o3t.png`
- `tools/export-stitch.mjs`, `tools/check-stitch.mjs`
- `AGENTS.md` (lista de golds), `docs/qa/README.md`, índices em `docs/`

## Registro

- 29/09/2026 — Gate criado. Aguardando o dono.
