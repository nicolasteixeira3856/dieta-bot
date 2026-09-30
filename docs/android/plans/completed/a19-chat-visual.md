# Plano — A19 Chat: pergunta em bolha própria, bolhas iguais, anexo com preview

- Estado: Concluído
- Aprovação manual: 30/09/2026 (dono: "Quero que você passe todos os planos que estão pendentes de validação manual para completo.")
- Data: 29/09/2026
- Contexto proprietário: `android`
- Código afetado: `apps/android/` (`feature/chat`)
- Pré-requisitos: **[ST1](../../../stitch/plans/completed/st1-chat.md) em `stitch/plans/completed/`** (golds novos de chat + `chatA`). [A18](a18-chat-registro-foto.md) concluído (mesmo arquivo `ChatScreen.kt`; evita conflito).

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
- [chat](../../../produto/specifications/chat.md), [ADR-020](../../../produto/adrs/ADR-020-estados-novos-chat-home-horario.md).

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

## Resultados (29/09/2026)

Aprovado pelo dono em 29/09/2026 ("Aprovo o plano `docs/android/plans/a19-chat-visual.md`. Implemente o plano aprovado."). Gate conferido antes do código: `st1-chat.md` em `stitch/plans/completed/`, `node tools/check-stitch.mjs` lista `chatA` (48 golds, 24 por tema).

### Implementado

- Bolhas: `BubbleShape = RoundedCornerShape(16.dp)` para usuário, IA, saudação, loading, falha e a bolha de foto (era 24 dp). `AiLabel` sem o pill "IA ATIVA"; padding do rótulo 4/9 dp (o gold subiu a bolha 2 dp quando o pill saiu).
- Pergunta: `ChatItem.Question(estimateId, text, time)` logo depois do `Assistant` cuja estimativa tem `estimateQuestion` não vazia. `QuestionBubble`: largura da bolha da IA (88%), `card` + borda `line`, barra gold de 3 dp à esquerda (recortada pelo raio), ícone `HelpOutline` gold 18 dp, texto 16 sp W400 em `text`, horário só embaixo dela (a bolha da estimativa perde o horário). 5 dp entre as duas bolhas, 16 dp no resto do fio (espaço por item em vez de `spacedBy`). Room, prompt e `openQuestion` (A8) sem mudança.
- Anexo: `Local.attachment` no `ChatViewModel`. `onPhoto(Ready)` (galeria e câmera) só guarda o caminho; outra foto substitui e apaga a anterior; `removeAttachment()` (✕) apaga o arquivo; `onCleared` apaga o anexo não enviado. `send()` aceita texto, anexo ou os dois e chama `post(text, attachment)` (o fluxo de antes, com `pendingPhoto` para o retry). `canSend` = texto não vazio **ou** anexo.
- Composer (`chatA`): com anexo, o pill vira caixa de raio 28 dp; `AttachmentThumb` 64 dp, raio 12, borda `line`, ✕ 22 dp em `surf2` no canto superior direito (tags `chat-attachment`, `chat-attachment-remove`); botões 40 dp e 11 dp embaixo, como no gold. Sem anexo, o composer é o de antes. Com anexo, os chips de sugestão do dia vazio somem (o gold `chatA` não os mostra).
- Achado no loop visual e corrigido: `produceState` guarda o valor anterior quando a chave muda, então trocar a foto anexada mostrava a miniatura da primeira. `AttachmentThumb` agora fica em `key(path)`.
- Spec [chat](../../../produto/specifications/chat.md): regras 2, 3, 14 e 15.
- `tools/capture-chat.sh`: o `chatE` é semeado com a pergunta ("Os pães tinham manteiga ou requeijão?"); `chatT`/`chatP` sem ela, como nos golds. `tools/capture-photo.sh`: bloco `chatA` (anexar sem POST, ✕ apaga, 2ª foto substitui, captura, enviar = 1 POST com texto + foto, sair do Chat apaga o anexo) e `send` depois de cada foto dos passos seguintes; a foto do `chatA` é a miniatura do próprio gold, como a do `chatF`. `tools/diff-gold.mjs`: `chatA` em `GOLD_CONFLICTS` com gate por região do composer.
- Testes: `ChatViewModelTest` (+6: foto escolhida não faz POST até enviar; câmera vira anexo; ✕ apaga e desliga o enviar; 2ª foto apaga a 1ª; sair do Chat apaga o anexo; pergunta vira item logo abaixo). `StitchGoldTest`: `chatA` dark/light com região `COMPOSER_ATTACHED`; `chatE` com a pergunta. Roborazzi: `chatQuestion` e `chatA` (dark/light), `chatReplace` regravado (bolhas 16 dp).

### Validação automatizada

- `testDevDebugUnitTest`: 199 testes, 195 passam. Os 4 que falham são `StitchGoldTest` `home1` e `o3` (dark e light) e falham igual na `master` sem este plano (conferido com `git stash`): golds do ST2/ST4, dos planos A22/A24.
- `verifyRoborazziDevDebug --tests RoborazziSmokeTest`: 12/12.
- `StitchGoldTest` (JVM, borrado): `chatE` 1,88 / 1,92 % (antes 2,84 / 2,56 % com o espaçamento inicial), `chatT` 1,27 / 1,74 %, `chatP` 0,25 / 0,64 %, região `chatF` 1,89 / 1,60 %, região `chatA` dark 1,74 %.

### Emulador (Medium_Phone, 780x1688 @ 320 dpi, fake server)

- `tools/capture-chat.sh dark|light`: todos os ✓, inclusive "follow-up question in its own bubble".
- `tools/capture-photo.sh dark`: todos os ✓ (anexo sem POST, ✕ apaga o arquivo, 2ª foto substitui, enviar = 1 POST com texto + foto 1000x1000, sair do Chat apaga o anexo sem POST, câmera vira anexo e só sai no enviar, 2048 px / 50 MP / 16 MB / WebP).
- `tools/capture-photo.sh light`: todos os ✓ menos a câmera: o app de câmera do emulador travou ("Can't connect to the camera", depois preview preso) mesmo após reboot. Os passos sem câmera rodaram de uma cópia temporária do script, todos ✓. A câmera → anexo ficou coberta pelo dark e pelo teste `capturedPhoto_isAttached_noPost`.
- `node tools/diff-gold.mjs` (emulador vs gold):

| Tela | Dark | Light |
|---|---|---|
| `chatE` | ✓ 1,76 % | ✓ 1,71 % |
| `chatT` | ✓ 1,39 % | ✓ 1,82 % |
| `chatP` | ✓ 0,36 % | ✓ 0,51 % |
| `chatF` região da foto | ✓ 1,99 % | ✓ 1,64 % |
| `chatA` região do composer | ✓ 1,75 % | ~ 5,65 % (report only) |
| `chat0` | ~ 5,02 % | ~ 4,72 % |
| `chatL` | ~ 5,37 % | ~ 4,96 % |
| `chatG` | ~ 10,45 % | ~ 8,94 % |
| `chatF` tela inteira | ~ 11,29 % | ~ 11,32 % |
| `chatA` tela inteira | ~ 5,14 % | ~ 5,37 % |

`~` = gold em `GOLD_CONFLICTS` (outra geração do Stitch: header de `chat0`/`chatL`/`chatA`, `chatG`/`chatF`), medido e não bloqueante, como antes.

### Lista de diffs (captura vs gold)

- Layout: bolhas com 4 cantos iguais em todas as telas, como os golds. Pergunta em bolha própria 5 dp abaixo da estimativa, horário só nela (`chatE`). Composer com anexo: miniatura em cima, câmera + texto + enviar embaixo (`chatA`). `chatA` gold usa o header da geração do `chat0` (título à esquerda, "Assistente de refeições"): o app mantém o header canônico do `chatE`.
- Tokens: bolhas `card` + `line`; barra, ícone de ajuda e enviar em `gold`; ✕ em `surf2` com ícone `text`. `chatA` light: o gold pinta o composer na cor da página (geração do `chat0`), o `chatE` light canônico usa `card`; o app segue o `chatE`, por isso a região light fica report only (JVM e emulador). A câmera do composer fica `muted` como no `chatE` (o `chatA` a desenha em `text`).
- Tipo: pergunta 16 sp W400 em cor primária; resto sem mudança. Legenda do `chatA` capturada como "almoco" (adb não digita "ç"; só raster).
- Raio: bolhas 16 dp; miniatura 12 dp; composer com anexo 28 dp; sheet 22 sem mudança.
- ButtonGroup / barra de ações e CTA: sem mudança (Gravar café · Trocar · Pular; enviar gold).
- Timeline: fora deste plano.
- Macros semânticos: P/C/G do card de estimativa sem mudança.
- Removido: pill "IA ATIVA" em todas as telas.

### Pendente (dono)

- Anexar foto, escrever e enviar no APK (validação manual 4).

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
