# ADR-022 — Mensagem do Chat até 2000 caracteres, com estado de erro

- Estado: Aceito (29/09/2026, com a aprovação do S9)
- Data: 2026-09-29
- Contexto: `produto`
- Substitui: parcialmente o [ADR-020](ADR-020-estados-novos-chat-home-horario.md) e a lista de golds do `AGENTS.md` (adiciona o gold `chatX`; não remove nenhum). Muda o limite de `text` do `POST /v1/chat` no [contrato](../../api-contract.md).

## Contexto

O composer corta o texto em 1000 caracteres sem avisar ([A5](../../android/plans/completed/a5-chat.md)): quem cola um texto maior perde o fim e não sabe. O server recusa `text` acima de 1000 com 422. O composer não tem limite de linhas: o `maxLines = 5` do [A18](../../android/plans/completed/a18-chat-registro-foto.md) só limita a altura da caixa, que rola por dentro.

## Decisão

1. Limite de **2000 caracteres** por mensagem do Chat. O client conta os caracteres do texto que vai ser enviado (sem espaços nas pontas), em code points, como o server.
2. O composer **não corta** o que o usuário digita ou cola. Acima de 2000, o composer entra no estado de erro:
   - borda da caixa do composer na cor `bad`;
   - mensagem `Texto muito longo` logo abaixo da caixa, na cor `bad`;
   - botão enviar desabilitado;
   - botão de foto desabilitado também, porque o texto digitado vai junto como legenda da foto.
   Voltou a 2000 ou menos: o estado some.
3. Sem contador visível. Só o estado de erro.
4. Server: `text` e `messages[].text` do `POST /v1/chat` passam de 1000 para 2000 caracteres. `/v1/estimate` e `/v1/fit` ficam como estão.
5. Gold novo `chatX` (dark e light): Chat com um texto longo demais no composer, no estado de erro. Gate [ST5](../../stitch/plans/completed/st5-chat-texto-longo.md).

## Motivação

Nada some sem aviso. 2000 caracteres cobrem com folga um dia inteiro descrito numa mensagem (300 a 600 caracteres, na prática).

Custo: o histórico leva até 12 mensagens. O pior caso passa de ~12 k para ~24 k caracteres de histórico por turno (≈ 6 k tokens). Aceito pelo dono em troca de não cortar.

## Consequências

### Positivas

- O usuário vê por que não consegue enviar e edita o texto.
- Client e server contam do mesmo jeito.

### Negativas

- Turnos mais caros no pior caso.
- Mais um gold por tema: 19 → 20 (ou 21 → 22, depois do ADR-020).

## Alternativas consideradas

- **5000 caracteres:** até ~60 k caracteres de histórico por turno, mais perto do timeout de 60 s. Rejeitada pelo dono em favor de 2000.
- **Continuar cortando em silêncio:** é o problema.
- **Contador sempre visível:** ruído para a mensagem típica, que é curta.

## Relações

- Planos: [S9](../../server/plans/completed/s9-limite-texto-2000.md), [ST5](../../stitch/plans/completed/st5-chat-texto-longo.md), [A25](../../android/plans/completed/a25-limite-texto-composer.md).
- Specs: [chat](../specifications/chat.md), [v1-chat](../../server/specifications/v1-chat.md).

Depois de aceito, este ADR não se edita.
