# Especificação — Chat

## Contexto e objetivo

Toda interação com a IA. Uma refeição de hoje dita com clareza é registrada sozinha e deixa um recibo; o recibo mais recente de cada refeição desfaz, exclui, troca ou edita o registro com um toque. Na dúvida, um único **Registrar**. Outro dia nunca é registrado. Layout: golds da fonte declarada no [inventário](../../qa/README.md#golds), desenhados no Figma `Design` com o design system Aero ([ADR-030](../../design/adrs/ADR-030-own-design-system-aero.md)).

## Escopo

Tela Chat, bolhas, composer, chips, prompt do dia, compactação.

## Fora de escopo

Avatar, visto, status, streaming neste corte. Registro de outro dia. UI de aceite legal ou de denúncia. Toggle de registro automático. Refazer depois de Desfazer ou Excluir. Editar ou excluir pela timeline da Home.

## Regras funcionais

1. Tela cheia. Back → Home.
2. Bolhas: user à direita, IA à esquerda, todas com o mesmo raio (16 dp nos 4 cantos). Rótulo da IA: avatar da Tali (20 dp, círculo com anel de vidro) + "Tali", sem selo. Cabeçalho: avatar da Tali (32 dp) + "Tali" com o ponto de destaque sobre "ASSISTENTE DE REFEIÇÕES", centralizados ([ADR-035](../adrs/ADR-035-tali-in-app-identity.md)). A legenda sob a bolha de carregamento diz "Tali".
3. Composer: texto + câmera (foto) + send. Enter pula linha (até 5 linhas visíveis); só a seta envia. A foto (câmera ou galeria) vira anexo no composer, com miniatura e ✕; nada sai antes do toque em enviar. Uma foto por vez: outra substitui a anterior. Send ativo com texto ou anexo; manda os dois juntos. ✕, troca ou sair do Chat com o anexo apagam o arquivo. Texto até 2000 caracteres (code points do texto sem espaços nas pontas; emoji conta 1), nada é cortado. Acima de 2000 (`chatX`): borda da caixa em `bad`, `Texto muito longo` logo abaixo, enviar e câmera desabilitados; sem contador. Com mais de uma linha a caixa tem cantos de 24 dp. A câmera do composer e o chip `Tirar foto do prato` fecham o teclado antes de abrir a escolha de foto; fechar a escolha não reabre o teclado. O toque em enviar fecha o teclado e tira o foco do composer.
4. Marca de registro ([ADR-028](../adrs/ADR-028-registro-autonomo.md)): o app manda `auto_record: true` em todo turno e o server devolve `record` (`auto` | `ask` | `none`) e, num pulo por texto, `intent: skip` com `skip_slot`. O app confere de novo: `auto` de `log` só vale com estimativa liberada (sem pergunta pendente), slot sugerido entre os slots de hoje e estimativa registrável; se falhar, vira `ask`. Server sem `record` com estimativa `log` = `ask`: nada é registrado sem toque. `plan` não registra sozinho (**Registrar assim**, regra 16); `question` e pergunta antes da estimativa não registram. Uma ação automática por resposta; reenviar uma falha é um envio novo.
5. `auto` (refeição clara de hoje, ou foto sem texto): slot vazio → registro local na hora (kcal, P/C/G, texto da regra 12), sem novo POST, recibo `Registrado em {slot} · {hora}` `+{kcal} kcal` (`chatG`; foto: `chatF`). Slot pulado → o registro substitui o pulo sem perguntar (o pulo não tem números a perder). Slot com registro → nada muda; logo abaixo da resposta, `Substituir {slot}?`, `{slot} tem {kcal antigo} kcal. Fica com {kcal novo} kcal.`, **Substituir** (tokens de CTA) | **Outra refeição** (contorno) (`chatU`). Substituir troca os registros de hoje daquele slot pelo novo numa transação ([ADR-017](../adrs/ADR-017-registro-consolidado.md)), recibo `Atualizado em {slot} · {hora}` `{antigo} → {novo} kcal`. Outra refeição abre o Trocar sem seleção; slot vazio registra, slot com registro pergunta de novo. A pergunta expira no próximo envio, na virada do dia ou quando o slot muda por outro caminho: no lugar dela, a marca `Não registrado`.
6. `ask` (intenção em dúvida, ex.: "pudim de leite com calda"): a estimativa mantém `Deseja registrar essa refeição no {slot}?` e um único **Registrar** no lugar das ações (`chatE`). O toque segue o caminho da regra 5, sem novo POST; sem slot de hoje, abre o Trocar sem seleção. Registrar some no próximo envio e a estimativa fica `Não registrado`. Sem toque, número não entra no contador.
7. Pulo por texto ("pulei o café", ou um pulo firme anunciado, "hoje não vou jantar"; "acho que não vou jantar" e "ainda não almocei" não pulam): slot vazio de hoje → status skipped, sem kcal, recibo `Pulado {slot}` com só **Desfazer**. Slot já registrado ou pulado: nada muda e a resposta fica `Não registrado` (o Excluir do recibo remove um registro). O pulo da Home (toque num slot vazio) não mudou. Recibos não vão ao server.
8. Perfil e snapshot só incluem slots do dia em America/Sao_Paulo (ADR-021/A24), assim como sugestão da hora, Trocar e o pulo por texto. Logs de slots de outros grupos continuam nos totais. Prompt do turno: perfil + memória em fatos (`facts`, ≤ 30 permanentes + ≤ 40 dinâmicos + ≤ 5 temporários, regras em [memoria-push](memoria-push.md); `temp_facts: true` sempre) + snapshot Room (slots, kcal, P/C/G, pulou, saldo, `remaining_kcal` = teto efetivo − comido, pode ser negativo) + `recent` (registros dos 7 dias antes de hoje, mais antigo primeiro, no dia pela hora do slot; slot apagado ou nulo = `Outros`; texto ≤ 240; ≤ 42, ficam os mais recentes) + ≤ 2 digest + ≤ 12 raw do dia. No histórico, a resposta da IA com refeição sugerida (estimativa, ou pergunta antes da estimativa com `question_slot` do server, guardado quando é slot de hoje) termina com a linha `[refeição sugerida: {nome do slot}]`, nome do perfil de hoje; slot que não é de hoje não ganha linha. O texto é cortado antes e a linha entra depois, dentro dos 2000.
9. Ao fechar 12 raw desde o último digest: o próximo POST manda antes `compact=true` só com o bloco mais antigo ([ADR-029](../adrs/ADR-029-fatos-temporarios-compactacao.md)). Ficam raw as 4 mais novas; com pergunta antes da estimativa em aberto, desde a mensagem do usuário que a originou, se for mais antiga. O bloco tem até 12 raw; enquanto sobrarem mais de 12, um segundo bloco vai no mesmo envio (no máximo 2). Sequência aberta de 12 ou mais: não compacta. O digest guarda o id da última mensagem resumida (`coversUntilId`) e o histórico continua depois dela; digest anterior ao A38 corta pela hora de criação. Nada é resumido duas vezes. Dia e último wipe são lidos antes da chamada; se mudaram, o digest não é gravado. Falha não grava nada e o turno vai com as 12 raw mais novas. Máx 2 digest. Terceiro bloco substitui o digest mais velho.
10. Snapshot do dia não compacta.
11. Dia SP vira: digest some do prompt. UI do fio 60 d continua com separador de data. O Chat abre já na última mensagem, sem rolagem animada. O fio carrega as 20 mensagens mais recentes e, ao rolar para cima, mais 20 por vez, com o indicador de carregamento centralizado no topo, até os 60 dias. Mensagem nova com o fio rolado para cima não move a tela; no fim do fio, ou no próprio envio, o fio acompanha a mensagem nova. Registrar, rodadas de pergunta, Forçar estimativa e o registro usam a conversa inteira do dia, não só a página carregada. Uma resposta pode chegar com recibo ou pergunta logo abaixo: o fio acompanha os dois quando está no fim.
12. Texto da timeline ao registrar: o `meal_text` da estimativa (a refeição inteira, corrigida pela conversa). Sem `meal_text`: a primeira mensagem do usuário da cadeia daquela estimativa, pulando as respostas a perguntas (mensagem logo depois de uma pergunta, com ou sem estimativa; até 3 rodadas); se essa mensagem tem foto, a descrição que a IA devolveu. Nada disso: nomes dos itens. Nunca a resposta a uma pergunta. O registro não escreve linha de memória: aplica a rotina que a IA propôs para aquela estimativa, se houver, e guarda no recibo a imagem antes/depois de cada fato que mudou.
13. O client deste corte chama `POST /v1/chat`. Não chama `/v1/estimate` nem `/v1/fit` a partir do Chat.
14. Questions before the estimate (`log` only, [ADR-026](../adrs/ADR-026-perguntas-antes-da-estimativa.md)): a material, answerable doubt shows only the question, with all useful doubts together and no estimate (`chatQ`). An explicitly unavailable detail is not asked again, even as an approximation; a useful alternative such as size is allowed. If the user cannot supply the alternatives either, estimate the whole meal with a brief assumption and honest confidence, preserving foods and portions. Current corrections override earlier unavailability. [Server Chat rules 4 and 7](../../server/specifications/v1-chat.md#functional-rules) own clarification and digest handling. The question uses its own bubble labeled with the Tali avatar and "Tali": accent left bar, question icon, Body/Strong primary-color text and the time inside the bubble; no card, numbers or actions. At most three rounds: the app sends `clarify_rounds` (consecutive questions at the end of today's conversation, skipping user answers; stop at an estimate, receipt or another assistant response), and the server releases on the fourth turn. From the second consecutive question, **Forçar estimativa** (one pill in the action position, forward icon) sends `Pode estimar assim.` with `force_estimate` through the normal send/loading/failure/retry flow; retry preserves the request. It disappears during sending and with an attached photo. The estimate appears once, without a question (`chatE`). Older messages containing both estimate and question retain the question bubble below.
15. Dia sem mensagens: saudação fixa, cartão “Meta calórica de hoje” (restante / meta) e chips de sugestão que preenchem o composer. Com foto anexada, os chips somem. Nada disso é gravado nem enviado.
16. Plano (`chatR`): a bolha mostra o texto da IA linha a linha (P/C/G nas cores dos macros) e, dentro dela, o dia projetado calculado pelo app, nunca pela IA: `Dia: {comido} → {comido + plano} de {teto efetivo} kcal` e `P {x}/{alvo} · C {y}/{alvo} · G {z}/{alvo}` (valores depois do plano). Teto e alvos pelo perfil e pelo `BudgetCalculator` no momento da exibição: o painel acompanha registros feitos depois. Acima do teto efetivo, a linha de kcal em `bad`. Plano já registrado conta uma vez (o painel parte de comido − plano). Plano de outro dia: só o texto. **Registrar assim** (um botão no lugar das ações) com slot sugerido do dia = o caminho da regra 5 nesse slot, `Substituir {slot}?` incluído; sem slot, abre o Trocar sem seleção. O recibo do plano tem as ações da regra 19.
17. Avisos de memória (`chatM`): abaixo da bolha da IA, acima do horário, um chip por linha na ordem `Memória atualizada` (o app aplicou alguma mudança de memória naquele turno), `Memória permanente`, `Memória dinâmica` (a resposta usou fatos desse tipo, `memory_used` conferido pelo app). Recibo que aplicou uma rotina leva `Memória atualizada` logo abaixo; o recibo que reverte uma mudança não repete o aviso. Chips (`Chip/Memory`) de 28 dp em pílula, só informativos (sem toque); accent só no check do `Memória atualizada`. Sem aviso, sem linha.
18. Sugestão da rotina (`chatS`): ao abrir o Chat e a cada minuto, se o slot da hora está sem registro e sem pulo hoje e existe uma rotina forte desse slot (permanente, ou dinâmica vista em ≥ 3 dias; várias → a vista em mais dias), um card no fim da conversa (dia vazio: logo abaixo do cartão da meta): `O de sempre no {slot}?`, texto da rotina, `{kcal} kcal · {P}P · {C}C · {G}G` do último registro, chip de origem e dois botões. **Registrar**: registro local com esses números (`source` `routine`) no slot, recibo com as ações da regra 19, reforço da rotina e `Memória atualizada` no recibo, sem POST. **Quase igual**: o texto da rotina substitui o composer, cursor no fim, foco e teclado; nada é gravado. O card some no primeiro envio da tela, com o slot gravado ou pulado ou com a hora em outro slot. Não vai para `chat_message` nem para o prompt.
19. Ações do recibo (`chatG`, `chatF`, `chatD`): só o recibo ativo mais recente de cada slot que ele tocou (em qualquer dia) e só enquanto o slot está exatamente como o recibo deixou; um recibo antigo, sem dados de desfazer, não tem ações. Botões empilhados abaixo do recibo (`Chat/ReceiptAction`): 44 dp, vidro, ícone à esquerda, rótulo no estilo `Button`; Excluir em `status/bad`. Conjunto: primeiro registro (texto, plano, rotina) → Excluir · Trocar refeição · Editar; foto → Excluir · Trocar refeição; substituição e movimento → Desfazer · Excluir · Trocar refeição · Editar (sem Editar na foto); restauração → Excluir · Trocar refeição · Editar; registro sobre um pulo → Desfazer à frente; pulo → Desfazer. **Excluir**: apaga o registro do slot e reverte a memória do recibo, sem confirmação; marca `Excluído`. **Desfazer**: cada slot tocado volta ao "antes" (registro anterior, registro movido de volta, pulo removido ou de volta) e a memória é revertida; slot que termina com registro ganha o recibo `Restaurado em {slot} · {hora}` `{kcal} kcal`, que fica com as ações; marca `Desfeito`. **Trocar refeição**: Trocar com o slot do registro marcado "(atual)"; slot vazio move o registro (recibo `Movido para {slot} · {hora}`, o antigo fica `Movido`; a memória do recibo é revertida e a rotina dele vale no slot novo); slot com registro pergunta abaixo do recibo (regra 5) e Substituir move e substitui numa transação. **Editar**: apaga o registro como o Excluir, marca `Removido para editar` e põe o texto do registro no composer (troca o rascunho), com foco, teclado e cursor no fim. Recibo marcado fica a 50 % com a marca (ícone + rótulo `muted`) no fim da linha do título. Sem refazer.
20. Memória do recibo: cada recibo guarda a imagem antes/depois dos fatos que o registro mudou. Excluir, Editar, Desfazer e Trocar refeição revertem só o fato que ainda é igual à imagem "depois" (fato adicionado sai, removido volta, alterado recupera texto e dias); fato mudado depois por outro turno fica como está.
21. Recusa de conteúdo: pedido fora do escopo, conteúdo bloqueado ou sinal de risco recebe uma resposta fixa numa bolha comum da IA, sem estimativa, sem mudança de memória e sem registro ([política de conteúdo](../../content-policy/specifications/content-policy.md); textos em [refusal-copy](../../content-policy/specifications/refusal-copy.pt-BR.md)). Moderação indisponível cai na bolha de falha. Nenhuma tela ou controle novo.

22. Meal additions and revisions ([ADR-032](../adrs/ADR-032-acrescimos-e-correcoes-de-refeicoes.md)). The server resolves the operation and the meal from the eating context and explicit targets, preserves the DAY aggregate on an addition, re-estimates a revision as a whole, never forces an unknown operation and keeps descriptions complete within the [HTTP limits](../../api-contract.md#meal-change-capability) ([server Chat](../../server/specifications/v1-chat.md)). The app sends `meal_changes: true` on every normal turn and acts only on the structured `meal_change`; reply text, food names and button copy never decide an operation, a meal or a number. It keeps the slot states the request's DAY carried and stores the proposal (operation, day, latest wipe, source and destination states, added food, picked destination) with the answer before any record.
    - Addition to a meal with a record (`chatI`): the bubble shows only the added food with `+{kcal} kcal` and `+` macros, no prose. Below it: `Já registrado no {slot}` {kcal}, `Total do {slot}` {kcal} with the resulting P/C/G in the macro colours, `Adicionar ao {slot}?`, **Adicionar** over **Escolher outra refeição**. Nothing changes before Adicionar, which writes one record: the recorded text, `; `, the added food, with numbers summed by the app (the card and the record use the same values). Receipt `Atualizado em {slot}` `{antes} → {depois} kcal`.
    - **Escolher outra refeição** (`chatTI`) opens the meal picker for the added food only: title `Escolher outra refeição`, `Só o acréscimo vai para a refeição escolhida. O {slot} fica como está.`, the added food with `+{kcal} kcal`, nothing picked, no `(atual)`. An empty meal gets only the addition at once (a skip is cleared, with Desfazer); another meal with a record shows `chatI` again with that meal's numbers and writes nothing; the source meal brings back its own confirmation. The source is never written and must still be what the proposal was built on.
    - An addition to an empty or skipped meal follows rules 4–6 with the added food alone; with no meal, Registrar opens the picker with nothing picked and no `(atual)`.
    - Revision (`chatIC`): the revised meal and its `NOVO TOTAL` (no `~`), then `Atualizar {slot}?`, `Antes`, `Novo total`, **Atualizar** | **Cancelar**, and no other meal. Atualizar replaces the source while it is unchanged; Cancelar leaves `Não registrado` with Room and memory as they were.
    - One active proposal. A new send, a day change, a wipe, or a change of its source or destination by another path makes it `Não registrado`, as does an answer that arrives after such a change (with no routine). Adicionar, Atualizar and the picker commit in one transaction that rechecks the day, the wipe, the source, the destination and that the answer is still undecided; a double tap, a retry or a recreated screen never applies it twice.
    - The immediately preceding addition still open when the user sends again goes back as `pending_addition`, taken before the send expires it, only while its source still matches DAY; a retry carries the same one. A recorded, cancelled or expired proposal never goes back, and a proposal never counts as eaten in DAY.
    - A server answer without `meal_change` keeps rules 5–6 and 19 as they are. Missing, malformed or contradictory metadata on a recordable answer shows the reply with no card and `Não registrado`, with no memory change. A destination whose complete description would pass 2000 code points is never cut: nothing is written and the note `Não registrado: a descrição ficaria longa demais.` shows.
    - Desfazer, Excluir, Editar and Trocar refeição keep rule 19 on the consolidated record; Desfazer of a rerouted addition never touches its source. A routine of the answer applies once, after the record, to the meal that actually received it.

## Estados e falhas

- Estados de tela (golds do [ADR-012](../adrs/ADR-012-chat-home-perfil.md) + ADR-023 + ADR-028): `chat0`, `chatL`, `chatE` (Registrar), `chatT`, `chatF` (foto registrada sozinha), `chatG` (recibo com ações), `chatA`, `chatX`, `chatR` (plano), `chatM` (avisos de memória), `chatS` (sugestão da rotina), `chatQ` (pergunta antes da estimativa, ADR-026), `chatU` (Substituir dentro da conversa), `chatD` (substituição desfeita e restauração), `chatI` (acréscimo a uma refeição registrada, ADR-032), `chatTI` (destino do acréscimo), `chatIC` (correção da refeição). `chatP` saiu do Chat (ADR-028); o gold fica como referência do pulo.

- Timeout 60 s: bolha “Não deu. Toque para tentar de novo.”. Nada registrado; o toque reenvia.
- Ação de recibo cujo slot mudou no meio (outro caminho): a transação confere o estado e não grava nada; os botões somem.
- Foto: o client reduz a 2048 px no lado maior, JPEG q85, sem EXIF ([ADR-018](../../android/adrs/ADR-018-foto-2048.md)). “Foto grande demais.” só quando a foto não cabe na memória para decodificar. Cap de 16 MB fica como defesa.
- Sem rede: bolha de falha. Room intocado.
- Texto acima de 2000 caracteres: estado de erro do `chatX`, nada sai (0 POST, nem com foto). Apagar até 2000 volta a enviar.

## Fronteiras e ownership

Comportamento: `produto`. UI e Room: `android`. Contrato HTTP: `server`.

## Decisões relacionadas

- [ADR-012](../adrs/ADR-012-chat-home-perfil.md)
- [ADR-017](../adrs/ADR-017-registro-consolidado.md)
- [ADR-018](../../android/adrs/ADR-018-foto-2048.md)
- [ADR-026](../adrs/ADR-026-perguntas-antes-da-estimativa.md)
- [ADR-028](../adrs/ADR-028-registro-autonomo.md)
- [ADR-021](../adrs/ADR-021-refeicoes-por-dia.md)
- [ADR-022](../adrs/ADR-022-limite-texto-chat.md)
- [ADR-023](../adrs/ADR-023-chat-v2-memoria-v2.md)
- [ADR-029](../adrs/ADR-029-fatos-temporarios-compactacao.md)
- [ADR-024](../../content-policy/adrs/ADR-024-content-safety-boundaries.md)
- [v1-chat](../../server/specifications/v1-chat.md)

## Critérios de aceite funcionais

- Refeição clara de hoje é registrada sem toque e deixa recibo com as ações (`chatG`; foto `chatF`).
- Refeição em dúvida mostra só Registrar (`chatE`); sem toque, o círculo da Home não muda.
- Refeição para um slot já registrado pergunta dentro da conversa (`chatU`); nada muda até Substituir.
- Acréscimo a uma refeição registrada mostra o alimento acrescentado e o total da refeição separados (`chatI`); Adicionar soma uma vez, e outra refeição recebe só o acréscimo (`chatTI`).
- Correção pergunta Atualizar (`chatIC`); Cancelar não muda nada.
- Desfazer uma substituição traz o registro anterior com o recibo `Restaurado` (`chatD`).
- Excluir, Trocar refeição e Editar agem só pelo recibo mais recente do slot e revertem a mudança de memória daquele registro.
- "pulei o café" pula o slot com recibo; outro dia nunca é registrado.
- Trocar o slot e registrar cai no slot certo.
- A 13ª mensagem do dia não manda 13 raw no prompt.
- Depois de uma compactação, a pergunta em aberto e a refeição dela continuam no histórico.
- "Registra na refeição de hoje" depois de uma estimativa ou pergunta que sugeriu um slot registra nesse slot.

## Proveniência

- [S18](../../server/plans/completed/s18-meal-additions-and-revisions.md) — meal additions and revisions

- [S17](../../server/plans/completed/s17-unavailable-meal-details.md) — Unavailable meal details and useful clarification alternatives

- [A5](../../android/plans/completed/a5-chat.md) — Chat + chips + prompt
- [A5b](../../android/plans/completed/a5b-ligar-compact.md) — Ligar compact no Chat
- [S2](../../server/plans/completed/s2-v1-chat.md) — POST /v1/chat
- [S3](../../server/plans/completed/s3-compact.md) — compact digest
- [A6](../../android/plans/completed/a6-foto.md) — foto camera + picker
- [A18](../../android/plans/completed/a18-chat-registro-foto.md) — Chat: refeição consolidada, teclado e foto 2048 px
- [S8](../../server/plans/completed/s8-chat-json-slot-consolidado.md) — Chat: JSON garantido, slot sugerido e refeição consolidada
- [A19](../../android/plans/completed/a19-chat-visual.md) — Chat: pergunta em bolha própria, bolhas iguais, anexo com preview
- [ST1](../../stitch/plans/completed/st1-chat.md) — Chat: bolhas, pergunta separada, anexo com preview
- [A25](../../android/plans/completed/a25-limite-texto-composer.md) — Composer: limite de 2000 caracteres com estado de erro
- [S9](../../server/plans/completed/s9-limite-texto-2000.md) — Texto do Chat até 2000 caracteres
- [ST5](../../stitch/plans/completed/st5-chat-texto-longo.md) — Chat: texto longo demais no composer (`chatX`)
- [A27](../../android/plans/completed/a27-chat-v2-texto-intencao.md) — Chat v2: texto da refeição, intenção e histórico de 7 dias
- [S11](../../server/plans/completed/s11-chat-v2.md) — Chat v2 no server: intenção, texto da refeição, memória estruturada e histórico recente
- [A28](../../android/plans/completed/a28-memoria-v2.md) — Memória v2: fatos permanentes e dinâmicos
- [A29](../../android/plans/completed/a29-chat-v2-interface.md) — Chat v2: plano de refeição, avisos de memória e sugestão da rotina
- [ST6](../../stitch/plans/completed/st6-chat-v2.md) — Chat v2: plano de refeição, memória e sugestão da rotina (`chatR`, `chatM`, `chatS`)
- [A30](../../android/plans/completed/a30-perguntas-antes-da-estimativa.md) — Questions before the estimate, Forçar estimativa
- [S13](../../server/plans/completed/s13-perguntas-antes-da-estimativa.md) — Questions before the estimate, 3-round hard stop
- [ST7](../../stitch/plans/completed/st7-pergunta-antes-da-estimativa.md) — Question before the estimate (`chatE`, `chatQ`)
- [CP2](../../content-policy/plans/completed/cp2-server-content-controls.md) — Server scope and content controls
- [A32](../../android/plans/completed/a32-chat-rolagem-paginacao.md) — Chat: opens at the bottom, reverse paging, keyboard off for the photo
- [A34](../../android/plans/completed/a34-registro-autonomo.md) — Autonomous record, receipts with actions
- [S14](../../server/plans/completed/s14-registro-autonomo.md) — Record mark (`record`) and skip by text
- [S15](../../server/plans/completed/s15-registro-casos-dificeis.md) — Hard record cases: skip by text, photo with a question
- [ST9](../../stitch/plans/completed/st9-registro-autonomo.md) — Autonomous record (`chatE`, `chatF`, `chatG`, `chatU`, `chatD`)
- [A37](../../android/plans/completed/a37-enviar-fecha-teclado.md) — Send closes the keyboard
- [A38](../../android/plans/completed/a38-fatos-temporarios-compactacao.md) — Temp facts on the device, suggested slot in the history, compaction that keeps the open tail
- [A35](../../android/plans/out_of_scope/a35-registro-retroativo.md) — Retroactive record (a meal of another day)
- [A42](../../android/plans/completed/a42-chat-core-aero.md) — Chat core on Aero
- [A43](../../android/plans/completed/a43-chat-records-memory-aero.md) — Chat records and memory on Aero
- [A47](../../android/plans/pending_manual_validation/a47-chat-meal-updates.md) — Chat meal updates
- [A49](../../android/plans/completed/a49-fibrai-tali-visible-rename.md) — Fibrai and Tali in the app
