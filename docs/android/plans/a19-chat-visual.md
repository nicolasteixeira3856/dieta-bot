# Plano — A19 Chat: pergunta em bolha própria, bolhas iguais, anexo com preview

- Estado: Aguardando aprovação
- Data: 29/09/2026
- Contexto proprietário: `android`
- Código afetado: `apps/android/` (`feature/chat`)
- Pré-requisitos: **[ST1](../../stitch/plans/st1-chat.md) em `stitch/plans/completed/`** (golds novos de chat + `chatA`). [A18](a18-chat-registro-foto.md) concluído (mesmo arquivo `ChatScreen.kt`; evita conflito).

## Gate de autorização

Este plano é exclusivamente documental. A implementação só começa após aprovação explícita que identifique este arquivo:

> Aprovo o plano `docs/android/plans/a19-chat-visual.md`. Implemente o plano aprovado.

**Primeiro passo da implementação:** confirmar que `docs/stitch/plans/completed/st1-chat.md` existe e que `node tools/check-stitch.mjs` lista `chatA`. Se não, parar e avisar o dono.

## Objetivo

Levar para o app os golds do ST1:

- A pergunta da IA vira uma bolha própria, com destaque.
- Todas as bolhas têm o mesmo raio.
- O badge "IA ATIVA" sai.
- A foto passa a ser anexada no composer, com preview, e só sai no enviar.

## Fontes de verdade

- Golds `chat0`, `chatL`, `chatE`, `chatT`, `chatP`, `chatF`, `chatG`, `chatA` (dark + light) depois do ST1.
- [chat](../../produto/specifications/chat.md), [ADR-020](../../produto/adrs/ADR-020-estados-novos-chat-home-horario.md).

## Escopo de implementação

### 1. Bolhas e rótulo

- `UserShape` e `BotShape` → `RoundedCornerShape(16.dp)` nos 4 cantos (valor final medido no gold).
- `AiLabel`: remove o pill "IA ATIVA"; ícone e "Dieta Bot AI" ficam.

### 2. Pergunta em bolha própria

- `ChatItem.Assistant` com `estimate.question` não nula gera **dois itens** no fio: a bolha de sempre (texto + card + "Deseja registrar…") e um `ChatItem.Question` logo abaixo.
- `QuestionBubble`: barra gold de 3 dp à esquerda, ícone de ajuda gold, texto em cor primária, horário só nela (medidas do gold `chatE`).
- Só apresentação: nenhuma mudança no Room nem no prompt. O `openQuestion` do A8 continua igual.

### 3. Anexo com preview (`chatA`)

- `onPhoto(Ready)` deixa de chamar `post`. Guarda `Local.attachment = path` e mostra a miniatura no composer.
- ✕ na miniatura: apaga o arquivo e limpa o anexo.
- Uma foto por vez: escolher outra substitui (a anterior é apagada).
- Enviar ativo com texto **ou** anexo. Enviar → `post(text, attachment)` (o fluxo de hoje).
- Sair do Chat com anexo não enviado: arquivo apagado em `onCleared` (como o `pendingPhoto`).
- Câmera: mesma regra. A foto tirada vira anexo, não sai direto.

### 4. Captura e QA

- `tools/capture-chat.sh` e `tools/capture-photo.sh`: novo estado `chatA`; `chatF` segue o gold novo.
- Loop visual de todas as telas de chat, dark e light, com a lista de diffs no plano.

### 5. Spec

- `chat.md`: regra 2 (bolhas iguais), regra 3 (anexo antes de enviar), regra 14 (pergunta em bolha separada).

## Arquivos e áreas afetadas

- `feature/chat/ChatScreen.kt`, `ChatUiState.kt`, `ChatViewModel.kt`, `ChatPhoto.kt`.
- `tools/capture-chat.sh`, `tools/capture-photo.sh`.
- Testes Roborazzi e `ChatViewModelTest`.

## Validação planejada

1. `testDevDebugUnitTest`: escolher foto não dispara POST; enviar dispara 1 POST com texto + imagem; ✕ apaga o arquivo; sair do Chat apaga o anexo.
2. `verifyRoborazziDevDebug` com os estados novos (`Question`, `chatA`).
3. Capturas no emulador vs gold: `chat0`, `chatL`, `chatE`, `chatT`, `chatP`, `chatF`, `chatG`, `chatA`, dark e light, com diff escrito.
4. Manual (dono): anexar foto, escrever e enviar.

## Fora de escopo

- Comportamento de registro e foto 2048: [A18](a18-chat-registro-foto.md).
- Mais de uma foto por mensagem.

## Riscos e controles

- **Arquivo órfão de anexo:** limpeza em ✕, troca e `onCleared`; teste cobre os três casos.

## Critérios de aceite

- As 8 telas de chat batem com o gold, nos dois temas.
- Foto nunca sai sem o toque em enviar.

## Encerramento

Registre resultados e aplique o ciclo de vida de `docs/sdd/README.md`, com a entrega git (§ 6).
