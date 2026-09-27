# Plano — A5 Chat + chips + prompt (Concluído)

- Estado: Concluído
- Data: 25/09/2026
- Data de conclusão: 27/09/2026
- Contexto proprietario: `android`
- Codigo afetado: `apps/android/`
- Pre-requisitos: [S2](../../../server/plans/completed/s2-v1-chat.md) + [A4](a4-home-painel.md) + [chat.md](../../../produto/specifications/chat.md)

## Gate de autorizacao

> Aprovado pelo usuário via comando:
> `/goal Aprovo o plano docs/android/plans/a5-chat.md. Analise e implemente o plano aprovado.`

## Objetivo

Chat funcional: POST /v1/chat, bolhas, chips Gravar/Trocar/Pular, Room messages, compact client-side.

## Fontes de verdade

- Visual Gold (Stitch): `docs/qa/stitch/dark/{chat0,chatL,chatE,chatT,chatP,chatF,chatG}.png` e `docs/qa/stitch/light/{chat0,chatL,chatE,chatT,chatP,chatF,chatG}.png`
- spec [chat.md](../../../produto/specifications/chat.md)
- ADR-012

## Escopo de implementacao

### 1. ChatScreen

- LazyColumn invertido. Bolha user direita, IA esquerda. Sem avatar.
- Composer TextField + clip (A6 liga; neste plano disabled) + send.
- Send: insert message user, POST /v1/chat, insert assistant. Falha: bolha nao deu, tenta de novo.
- Chips se estimate != null: Gravar {slot}, Trocar lista slots, Pular {slot}.
- Tap Gravar: addLog com estimate. Sem 2o POST.
- suggested_slot fora da lista: esconde Gravar, mostra so Trocar.

### 2. PromptBuilder

- perfil + snapshot dia + <=12 raw + <=2 digest.
- rawCount >= 12 → proximo send compact=true, grava day_digest, digest nao vira bolha.
- Max 2 digest. 3o compact upsert seq=1.
- Se S3 fora: compact no-op, corta raw velhas do PROMPT.
- memory="" ate A8.

### 3. Rede

- postChat. OkHttp 60s. ChatIn/ChatOut. Nao chama estimate/fit.
- UI 60d com separador de data.

## Validacao planejada

- Unit PromptBuilder 12 msgs → compact; 13a raw fora do IN.
- Unit Gravar chama addLog, nao 2o POST.
- Validacao visual Compose vs Stitch Gold: capturas emulador `docs/qa/android/current/{dark,light}/{chat0,chatL,chatE,chatT,chatP,chatF,chatG}.png` comparadas pixel a pixel contra `docs/qa/stitch/{dark,light}/{chat0,chatL,chatE,chatT,chatP,chatF,chatG}.png` com diff list aprovada.

## Fora de escopo

Foto A6. Push. Memoria A8. Streaming. Apagar T1/T2/T3.

## Criterios de aceite

- Circulo da Home muda so apos Gravar.
- Trocar slot grava no escolhido.
- ChatScreen nao chama /v1/estimate.

## Resultado da implementação

### Entregue

- **Rede.** `ChatIn`/`ChatOut` espelham `server/main.py`; `NutriApi.chat` (`POST /v1/chat`). OkHttp: read/write 60 s, call 65 s. O Chat só conhece a interface `ChatService` (sem `/v1/estimate` nem `/v1/fit`).
- **`PromptBuilder`** (puro): perfil (meta do dia com crédito de treino, alvos P/C/G, eat-back, slots `id`/`HH:MM`) + `memory=""` + snapshot do dia (eaten P/C/G, status `eaten`/`skipped`/`empty` por slot) + ≤ 2 digests + ≤ 12 raw do dia depois do último digest. Recibos nunca vão ao server. Texto ≤ 1000.
- **Compact.** Caminho implementado (`needsCompact` com 12 raw → `compact=true` → `upsertDigest` → novo prompt), desligado por `PromptBuilder.COMPACT_ENABLED = false` enquanto o S3 não existe (server responde 400). Desligado: as raw mais velhas saem do prompt.
- **`ChatViewModel`.** Envio fica só na VM até o server responder; sucesso grava user + assistant (estimate, slot sugerido, pergunta, itens). Falha (timeout 60 s, rede, resposta vazia): bolha "Não deu. Toque para tentar de novo.", Room intocado; retry reenvia. Ações sobre a última estimativa sem recibo: Gravar {slot sugerido} (some se o slot não é do perfil), Trocar (sheet), Pular (confirmação). Gravar = `addLog` local com a estimativa + recibo; sem 2º POST. Texto da timeline = o que o user escreveu (descrição da IA se veio de foto, A6).
- **`ChatScreen`** (canônico chatE): cabeçalho centralizado, separador de data 60 d, bolhas, cartão de estimativa (energia + P/C/G com cores semânticas), barra Gravar/Trocar/Pular, composer (foto desabilitada até o A6), dia vazio com saudação + "Meta calórica de hoje" + chips de sugestão, carregando, falha, recibo "Registrado em {slot}". Sheet de refeição e confirmação de pulo como camadas da tela, com blur no fundo; sheet raio 22 (AGENTS).
- **Room v3.** `chat_message` ganha `estimateSlotId`, `estimateQuestion`, `estimateItems` (`MIGRATION_2_3`, schema `3.json`). Recibos: `role` `logged`/`skipped`.
- **`DaySnapshot.metaOn`** compartilhado entre Home e Chat.
- **QA.** `tools/fake-chat-server.mjs` (fake de `/v1/chat`, modo `hang`), `tools/capture-chat.sh` (fluxo real + capturas), build com `-PAPI_PUBLIC_URL=...`.

### Decisões tomadas na implementação

- **Gold canônico = chatE** (estimativa com ações; fundo de chatT e chatP). As telas do Stitch são gerações diferentes: chat0/chatL usam outro cabeçalho (corpo sozinho 1,2–1,4%), chatF/chatG outro estilo de bolha, chatG dark é render desktop 2560×2048. Ficam em `GOLD_CONFLICTS` (reportadas, sem gate).
- **ADR-012 × gold.** ADR: "sem avatar, sem visto, sem status". O gold canônico tem ponto de status, pílula "IA ATIVA", rótulo "Nutri AI" e ✓✓. Pela precedência do SDD, `AGENTS.md` (seguir o gold) vem antes do ADR: os elementos foram mantidos como decoração. **Pendente de decisão do dono.**
- **Controles sem função omitidos:** microfone e "ajustes" (tune) do gold não têm funcionalidade nem plano; o espaço do tune fica vazio para manter o título centralizado.
- **Room v3** não estava no plano: sem ela o chip Gravar/pergunta/itens se perdem ao reabrir o app. Cabe no "estimate*" da spec room-v2.
- **Pular pede confirmação** (chatP), a spec só diz "Pular {slot}". Sem slot sugerido, Pular usa o slot da hora.
- **chatF (foto)** fica com o A6 (bolha de imagem depende da câmera/picker).

## Validação executada

1. `./gradlew.bat :app:testDebugUnitTest`: 117/117. Inclui `PromptBuilderTest` (12 raw → compact quando ligado; desligado, a 13ª sai e ficam 12; digest corta raw; recibos fora; perfil/snapshot; texto ≤ 1000), `ChatViewModelTest` (envio grava as duas mensagens e oferece Gravar no slot sugerido; sem tap nada conta; Gravar = `addLog` sem 2º POST; Trocar grava no escolhido; slot fora do perfil esconde Gravar; falha não toca o Room e retry recupera; Pular pede confirmação e não soma kcal; dia vazio não chama o server), `MigrationV1V2Test` (v2 → v3).
2. Emulador (`wm size 780x1688`, `density 320`) contra `tools/fake-chat-server.mjs`, `tools/capture-chat.sh dark|light`: FAB → Chat; carregando enquanto o server segura; **dark:** timeout real de 60 s → bolha de falha → toque → estimativa; **light:** app morto no meio do request → nada gravado; Trocar abre o sheet; Pular pede confirmação; Gravar mostra o recibo **sem 2º POST** (contador do fake); Home mostra 380; `meal_log` = Café da manhã, 380 kcal, 36 C, 16 G, `user`.
3. Gate visual (borrado ≤ 2% + tinta 0,8–1,25):

| Tela | dark emulador | light emulador | dark JVM | light JVM |
|---|---|---|---|---|
| chatE (canônica) | 1,34% ✓ | 1,33% ✓ | 1,63% ✓ | 1,63% ✓ |
| chatT | 1,42% ✓ | 1,90% ✓ | 1,28% ✓ | 1,86% ✓ |
| chatP | 0,43% ✓ | 0,63% ✓ | 0,39% ✓ | 0,81% ✓ |
| chat0 | 4,73% (conflito) | 4,91% (conflito) | 4,93% | 5,52% |
| chatL | 5,15% (conflito) | 4,85% (conflito) | 5,36% | 5,86% |
| chatG | gold desktop | 8,90% (conflito) | — | 9,11% |
| chatF | A6 | A6 | — | — |

4. Onboarding e home1 seguem no gate (0,69–1,80%).

## Lista de diffs (gold × app)

- **Tokens, raio, rasterização:** como no A2.
- **Sheet:** raio 22 (AGENTS) × 28 do gold.
- **Omitidos:** microfone, botão tune, sugestão "Salada Caesar" mantida, chip "Tirar foto do prato" desabilitado até o A6.
- **Dados das capturas:** data do emulador (hoje); chatE/T/P semeados com a conversa do gold; chatP pergunta sobre o slot sugerido (Café da manhã), o gold usa "Lanche da tarde".
- **Conflitos entre golds (decidir no Stitch; hoje segue chatE):** chat0 (título à esquerda + pílula "IA ATIVA" no topo), chatL (título + pílula + tune), chatF/chatG (cabeçalho com bolt "IA Ativa", "Nutri AI" dentro da bolha, recibo), chatG dark desktop, chatT/chatP (fundo de outra tela, cabeçalho "Nutri").

## Encerramento

Ciclo SDD.
