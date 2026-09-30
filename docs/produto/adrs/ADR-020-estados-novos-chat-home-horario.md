# ADR-020 — Estados novos: anexo no Chat, treino na Home, seletor de horário

- Estado: Aceito (condição do próprio ADR: ST1, ST2 e ST3 concluídos, em `stitch/plans/completed/`; registrado em 30/09/2026)
- Data: 2026-09-29
- Contexto: `produto`
- Substitui: parcialmente o [ADR-012](ADR-012-chat-home-perfil.md) (lista fechada de telas) e a lista de 18 golds do `AGENTS.md`. Adiciona estados; não remove nenhum.

## Contexto

O feedback dos testers pede três coisas que nenhum gold cobre:

1. **Anexo com preview no Chat.** Hoje a foto sai assim que o usuário escolhe na galeria ou tira com a câmera. O dono quer um anexo no composer, que deixa continuar escrevendo e só sai no enviar.
2. **Treino na Home.** O kcal de treino só é informado na Config. O dono quer acesso rápido na Home e o número visível ali, abaixo dos macros, com um 🔥.
3. **Seletor de horário.** O `TimePicker` padrão (relógio) do Material é ruim de usar. O dono quer rodas de hora e minuto, no estilo do alarme da Samsung, dentro de um diálogo.

Também mudam golds existentes (sem id novo): a pergunta da IA sai do card e vira uma bolha própria, as bolhas de IA e de usuário ficam com o mesmo raio, e o badge "IA ATIVA" sai.

## Decisão

Golds novos (dark + light), que entram na lista do `AGENTS.md`:

| id | Estado | Gate |
|---|---|---|
| `chatA` | Chat com foto anexada no composer (miniatura + ✕), texto sendo digitado, botão enviar ativo | [ST1](../../stitch/plans/completed/st1-chat.md) |
| `homeW` | Home com o sheet "Treino de hoje" aberto a partir do atalho 🔥 | [ST2](../../stitch/plans/completed/st2-home-treino.md) |
| `o3t` | O3 com o diálogo de horário em rodas aberto | [ST3](../../stitch/plans/completed/st3-seletor-horario.md) |

Golds alterados: `chat0`, `chatL`, `chatE`, `chatT`, `chatP`, `chatF`, `chatG` (bolhas e badge), `chatE` (pergunta em bolha separada), `home0` e `home1` (linha de treino abaixo dos macros).

O diálogo de horário é o mesmo na O3 e na Config (editar horário de refeição). A Config não ganha gold próprio para ele.

## Motivação

Cada pedido muda o que o usuário vê. Sem gold, a implementação cairia em "baseado em wire", o que a constituição proíbe.

## Consequências

### Positivas

- A lista de golds volta a descrever o app inteiro.

### Negativas

- 18 → 21 golds por tema. `check-stitch.mjs` e o loop visual crescem.

## Alternativas consideradas

- **Implementar sem gold:** proibido pelo `AGENTS.md`.
- **Um gold por variação:** por exemplo, o seletor de horário também na Config. Seria redundante: o componente é o mesmo.

## Relações

- Gates: [ST1](../../stitch/plans/completed/st1-chat.md), [ST2](../../stitch/plans/completed/st2-home-treino.md), [ST3](../../stitch/plans/completed/st3-seletor-horario.md).
- Implementação: [A19](../../android/plans/completed/a19-chat-visual.md), [A22](../../android/plans/completed/a22-treino-home.md), [A21](../../android/plans/completed/a21-seletor-horario.md).

Depois de aceito, este ADR não se edita.
