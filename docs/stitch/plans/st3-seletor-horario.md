# Gate Stitch — ST3 Seletor de horário em rodas

- Estado: Aguardando o dono no Stitch
- Data: 29/09/2026
- Contexto proprietário: `stitch`
- Projeto: Stitch `Nutri` (`6282733070135794645`)
- Executa: [ADR-020](../../produto/adrs/ADR-020-estados-novos-chat-home-horario.md)
- Bloqueia: [A21 Seletor de horário](../../android/plans/a21-seletor-horario.md); é pré-requisito do [ST4](st4-refeicoes-por-dia.md).

Nomes das telas: títulos exatos do Stitch ([tabela](../README.md#nomes-das-telas-regra-do-dono-29092026)). Cada prompt tem um bloco para o tema escuro (`V2 Expressive`) e outro para o claro (`V2 Light`).

## ⛔ Bloqueio do dono — prompt para o Stitch

Este passo é manual e trava o A21. Um prompt, enviado duas vezes (dark e light).

### Prompt 3.1 — tela nova: diálogo com rodas de hora e minuto (`o3t`)

Antes de enviar:

1. Duplique "Onboarding 3/4 - Distribuição das refeições (V2 Expressive)" (dark) ou "Onboarding 3/4 - Distribuição das refeições (V2 Light)" (light).
2. Renomeie a cópia para o título exato: **Onboarding 3/4 - Seletor de horário (V2 Expressive)** ou **Onboarding 3/4 - Seletor de horário (V2 Light)**.
3. Selecione só a cópia renomeada. Opcional: anexe um print do alarme da Samsung (One UI) como referência de estilo das rodas.

**Dark:**

```text
Screen to edit: "Onboarding 3/4 - Seletor de horário (V2 Expressive)". It is a copy of "Onboarding 3/4 - Distribuição das refeições (V2 Expressive)".

Show a centered dialog open over this screen (dim the screen behind it with the usual scrim). The user tapped the "07:30" time field of "Café da manhã".

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

**Light:**

```text
Screen to edit: "Onboarding 3/4 - Seletor de horário (V2 Light)". It is a copy of "Onboarding 3/4 - Distribuição das refeições (V2 Light)".

Show a centered dialog open over this screen (dim the screen behind it with the usual scrim). The user tapped the "07:30" time field of "Café da manhã".

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

## Verificação (agente)

1. `mcp__stitch__list_screens`: "Onboarding 3/4 - Distribuição das refeições" (dark e light) com os mesmos IDs; "Onboarding 3/4 - Seletor de horário (V2 Expressive)" e "(V2 Light)" existem com esses títulos exatos (IDs no Registro).
2. `tools/export-stitch.mjs` + `o3t`; `tools/check-stitch.mjs`: contagem + 1 por tema.
3. `node tools/export-stitch.mjs` e `node tools/check-stitch.mjs` verdes.
4. Checklist dark e light. Falhou (inclusive título diferente): **para** e reporta ao dono o que falta.

### Checklist

- [ ] Diálogo centralizado sobre a O3, com scrim.
- [ ] Duas rodas (hora 00–23, minuto 00–59) com ":" no meio, formato 24 h.
- [ ] Faixa de seleção única atravessando as duas rodas; valor central grande e vizinhos esmaecidos.
- [ ] Sem mostrador de relógio e sem ícone de teclado.
- [ ] "Cancelar" e "OK" lado a lado, com o mesmo tamanho e a mesma fonte.
- [ ] Tokens do `AGENTS.md`; dark e light coerentes.

Checklist verde: lista de golds com `o3t`, `docs/stitch/README.md` com o título na tabela principal, estado `Concluído`, mover para `completed/`, índices e entrega git (`docs/st3-stitch-seletor-horario`).

## Arquivos que este gate pode tocar

- `docs/qa/stitch/{dark,light}/o3t.png`
- `tools/export-stitch.mjs`, `tools/check-stitch.mjs`
- `AGENTS.md` (lista de golds), `docs/qa/README.md`, `docs/stitch/README.md`, índices em `docs/`

## Registro

- 29/09/2026 — Gate criado. Aguardando o dono.
- 29/09/2026 — Prompt reescrito com os títulos reais do Stitch (pedido do dono).
