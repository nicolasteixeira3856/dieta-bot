# Especificação — Chat

## Proposed content-policy overlay

[Content handling](../../content-policy/specifications/content-policy.md) proposes fixed scope/safety replies through existing Chat bubbles, with no estimate or memory mutation on refusal. This is pending CP2 approval and implementation, not current behavior. Photos, meal intent and approved layouts remain unchanged. Any future legal acceptance/reporting UI requires its own approved scope and applicable Stitch gate.

## Estado

Vigente desde o [A5](../../android/plans/completed/a5-chat.md): tela Chat aberta pelo FAB da Home, `POST /v1/chat`. Compactação ligada desde o [A5b](../../android/plans/completed/a5b-ligar-compact.md) (server: [S3](../../server/plans/completed/s3-compact.md)).

Um registro por refeição com confirmação ao substituir, Enter pula linha e foto 2048 px desde o [A18](../../android/plans/completed/a18-chat-registro-foto.md) ([ADR-017](../adrs/ADR-017-registro-consolidado.md), [ADR-018](../../android/adrs/ADR-018-foto-2048.md); server: [S8](../../server/plans/completed/s8-chat-json-slot-consolidado.md)): regras 3, 5 e 7 e "Estados e falhas".

Pergunta em bolha própria, bolhas iguais, sem "IA ATIVA" e foto como anexo com preview desde o [A19](../../android/plans/completed/a19-chat-visual.md) (gate [ST1](../../stitch/plans/completed/st1-chat.md)): regras 2, 3, 14 e 15.

Limite de 2000 caracteres com estado de erro (`Texto muito longo`), sem corte silencioso, desde o [A25](../../android/plans/completed/a25-limite-texto-composer.md) ([ADR-022](../adrs/ADR-022-limite-texto-chat.md); server: [S9](../../server/plans/completed/s9-limite-texto-2000.md); gate [ST5](../../stitch/plans/completed/st5-chat-texto-longo.md)): regra 3 e "Estados e falhas".

Intenção (`log` / `plan` / `question`), texto gravado = `meal_text` e histórico de 7 dias no prompt desde o [A27](../../android/plans/completed/a27-chat-v2-texto-intencao.md) ([ADR-023](../adrs/ADR-023-chat-v2-memoria-v2.md); server: [S11](../../server/plans/completed/s11-chat-v2.md)): regras 4, 5, 8, 12 e 14.

Memória em fatos no prompt (`facts`), `memory_updates` aplicados pelo app e origem por mensagem desde o [A28](../../android/plans/completed/a28-memoria-v2.md): regras 8 e 12.

Plano com dia projetado e Registrar assim (`chatR`), avisos de memória (`chatM`) e sugestão da rotina (`chatS`) desde o [A29](../../android/plans/completed/a29-chat-v2-interface.md) ([ADR-023](../adrs/ADR-023-chat-v2-memoria-v2.md) decisões 3, 7 e 8; gate [ST6](../../stitch/plans/completed/st6-chat-v2.md)): regras 4, 5, 7, 16, 17 e 18.

Perguntas antes da estimativa (no máximo 3 rodadas) e **Forçar estimativa** (`chatQ`) desde o [A30](../../android/plans/completed/a30-perguntas-antes-da-estimativa.md) ([ADR-026](../adrs/ADR-026-perguntas-antes-da-estimativa.md); server: [S13](../../server/plans/completed/s13-perguntas-antes-da-estimativa.md); gate [ST7](../../stitch/plans/completed/st7-pergunta-antes-da-estimativa.md)): regras 4, 12 e 14 e "Estados e falhas".

Registro autônomo com recibo que desfaz (`chatE`, `chatF`, `chatG`, `chatU`, `chatD`) desde o [A34](../../android/plans/completed/a34-registro-autonomo.md) ([ADR-028](../adrs/ADR-028-registro-autonomo.md); server: [S14](../../server/plans/completed/s14-registro-autonomo.md), [S15](../../server/plans/completed/s15-registro-casos-dificeis.md); gate [ST9](../../stitch/plans/completed/st9-registro-autonomo.md)): regras 4–7, 12 e 16–20, "Estados e falhas" e critérios. A barra Gravar | Trocar | Pular e o diálogo de pular saíram do Chat; o `chatP` fica só como referência do pulo da Home.

## Contexto e objetivo

Toda interação com a IA. Uma refeição de hoje dita com clareza é registrada sozinha e deixa um recibo; o recibo mais recente de cada refeição desfaz, exclui, troca ou edita o registro com um toque. Na dúvida, um único **Registrar**. Outro dia nunca é registrado.

## Escopo

Tela Chat, bolhas, composer, chips, prompt do dia, compactação.

## Fora de escopo

Avatar, visto, status, streaming neste corte. Registro de outro dia ([A35](../../android/plans/out_of_scope/a35-registro-retroativo.md), `Fora de escopo`). Toggle de registro automático. Refazer depois de Desfazer ou Excluir. Editar ou excluir pela timeline da Home.

## Regras funcionais

1. Tela cheia. Back → Home.
2. Bolhas: user à direita, IA à esquerda, todas com o mesmo raio (16 dp nos 4 cantos). Rótulo da IA: ícone + "Dieta Bot AI", sem selo. Sem foto de perfil.
3. Composer: texto + câmera (foto) + send. Enter pula linha (até 5 linhas visíveis); só a seta envia. A foto (câmera ou galeria) vira anexo no composer, com miniatura e ✕; nada sai antes do toque em enviar. Uma foto por vez: outra substitui a anterior. Send ativo com texto ou anexo; manda os dois juntos. ✕, troca ou sair do Chat com o anexo apagam o arquivo. Texto até 2000 caracteres (code points do texto sem espaços nas pontas; emoji conta 1), nada é cortado. Acima de 2000 (`chatX`): borda da caixa em `bad`, `Texto muito longo` logo abaixo, enviar e câmera desabilitados; sem contador. Com mais de uma linha a caixa tem cantos de 24 dp. A câmera do composer e o chip `Tirar foto do prato` fecham o teclado antes de abrir a escolha de foto ([A32](../../android/plans/completed/a32-chat-rolagem-paginacao.md)); fechar a escolha não reabre o teclado.amp; O toque em enviar fecha o teclado e tira o foco do composer ([A37](../../android/plans/pending_manual_validation/a37-enviar-fecha-teclado.md)).
4. Marca de registro ([ADR-028](../adrs/ADR-028-registro-autonomo.md)): o app manda `auto_record: true` em todo turno e o server devolve `record` (`auto` | `ask` | `none`) e, num pulo por texto, `intent: skip` com `skip_slot`. O app confere de novo: `auto` de `log` só vale com estimativa liberada (sem pergunta pendente), slot sugerido entre os slots de hoje e estimativa registrável; se falhar, vira `ask`. Server sem `record` com estimativa `log` = `ask`: nada é registrado sem toque. `plan` não registra sozinho (**Registrar assim**, regra 16); `question` e pergunta antes da estimativa não registram. Uma ação automática por resposta; reenviar uma falha é um envio novo.
5. `auto` (refeição clara de hoje, ou foto sem texto): slot vazio → registro local na hora (kcal, P/C/G, texto da regra 12), sem novo POST, recibo `Registrado em {slot} · {hora}` `+{kcal} kcal` (`chatG`; foto: `chatF`). Slot pulado → o registro substitui o pulo sem perguntar (o pulo não tem números a perder). Slot com registro → nada muda; logo abaixo da resposta, `Substituir {slot}?`, `{slot} tem {kcal antigo} kcal. Fica com {kcal novo} kcal.`, **Substituir** (tokens de CTA) | **Outra refeição** (contorno) (`chatU`). Substituir troca os registros de hoje daquele slot pelo novo numa transação ([ADR-017](../adrs/ADR-017-registro-consolidado.md)), recibo `Atualizado em {slot} · {hora}` `{antigo} → {novo} kcal`. Outra refeição abre o Trocar sem seleção; slot vazio registra, slot com registro pergunta de novo. A pergunta expira no próximo envio, na virada do dia ou quando o slot muda por outro caminho: no lugar dela, a marca `Não registrado`.
6. `ask` (intenção em dúvida, ex.: "pudim de leite com calda"): a estimativa mantém `Deseja registrar essa refeição no {slot}?` e um único **Registrar** no lugar das ações (`chatE`). O toque segue o caminho da regra 5, sem novo POST; sem slot de hoje, abre o Trocar sem seleção. Registrar some no próximo envio e a estimativa fica `Não registrado`. Sem toque, número não entra no contador.
7. Pulo por texto ("pulei o café", ou um pulo firme anunciado, "hoje não vou jantar"; "acho que não vou jantar" e "ainda não almocei" não pulam, [S15](../../server/plans/completed/s15-registro-casos-dificeis.md)): slot vazio de hoje → status skipped, sem kcal, recibo `Pulado {slot}` com só **Desfazer**. Slot já registrado ou pulado: nada muda e a resposta fica `Não registrado` (o Excluir do recibo remove um registro). O pulo da Home (toque num slot vazio) não mudou. Recibos não vão ao server.
8. Perfil e snapshot só incluem slots do dia em America/Sao_Paulo (ADR-021/A24), assim como sugestão da hora, Trocar e o pulo por texto. Logs de slots de outros grupos continuam nos totais. Prompt do turno: perfil + memória em fatos (`facts`, ≤ 30 permanentes + ≤ 40 dinâmicos, regras em [memoria-push](memoria-push.md)) + snapshot Room (slots, kcal, P/C/G, pulou, saldo, `remaining_kcal` = teto efetivo − comido, pode ser negativo) + `recent` (registros dos 7 dias antes de hoje, mais antigo primeiro, no dia pela hora do slot; slot apagado ou nulo = `Outros`; texto ≤ 240; ≤ 42, ficam os mais recentes) + ≤ 2 digest + ≤ 12 raw do dia.
9. Ao fechar 12 raw desde o último digest: próximo POST manda `compact=true`. Resumo substitui o bloco. Máx 2 digest. Terceiro bloco substitui o digest mais velho.
10. Snapshot do dia não compacta.
11. Dia SP vira: digest some do prompt. UI do fio 60 d continua com separador de data. O Chat abre já na última mensagem, sem rolagem animada. O fio carrega as 20 mensagens mais recentes e, ao rolar para cima, mais 20 por vez, com o indicador de carregamento centralizado no topo, até os 60 dias ([A32](../../android/plans/completed/a32-chat-rolagem-paginacao.md)). Mensagem nova com o fio rolado para cima não move a tela; no fim do fio, ou no próprio envio, o fio acompanha a mensagem nova. Registrar, rodadas de pergunta, Forçar estimativa e o registro usam a conversa inteira do dia, não só a página carregada. Uma resposta pode chegar com recibo ou pergunta logo abaixo: o fio acompanha os dois quando está no fim.
12. Texto da timeline ao registrar: o `meal_text` da estimativa (a refeição inteira, corrigida pela conversa). Sem `meal_text`: a primeira mensagem do usuário da cadeia daquela estimativa, pulando as respostas a perguntas (mensagem logo depois de uma pergunta, com ou sem estimativa; até 3 rodadas); se essa mensagem tem foto, a descrição que a IA devolveu. Nada disso: nomes dos itens. Nunca a resposta a uma pergunta. O registro não escreve linha de memória: aplica a rotina que a IA propôs para aquela estimativa, se houver, e guarda no recibo a imagem antes/depois de cada fato que mudou.
13. O client deste corte chama `POST /v1/chat`. Não chama `/v1/estimate` nem `/v1/fit` a partir do Chat.
14. Perguntas antes da estimativa (só em `log`, [ADR-026](../adrs/ADR-026-perguntas-antes-da-estimativa.md)): com dúvida material, a IA manda só a pergunta, com todas as dúvidas da refeição numa mensagem, e nenhuma estimativa (`chatQ`). Bolha própria com o rótulo "Dieta Bot AI": barra gold à esquerda, ícone de ajuda, texto 18 sp em cor primária, horário abaixo; sem card, números ou ações. No máximo 3 rodadas: o app manda `clarify_rounds` (perguntas seguidas no fim da conversa de hoje, pulando as respostas; para em estimativa, recibo ou outra resposta da IA) e o server libera a estimativa no 4º turno. Da 2ª pergunta seguida em diante, **Forçar estimativa** (pill única no lugar das ações, ícone de avanço) envia `Pode estimar assim.` com `force_estimate` pelo envio normal (carregando, falha e repetir como sempre; repetir mantém o pedido); some durante o envio e com foto anexada. A estimativa vem uma vez, sem pergunta (`chatE`). Mensagens antigas com estimativa + pergunta seguem com a bolha da pergunta logo abaixo.
15. Dia sem mensagens: saudação fixa, cartão “Meta calórica de hoje” (restante / meta) e chips de sugestão que preenchem o composer. Com foto anexada, os chips somem. Nada disso é gravado nem enviado.
16. Plano (`chatR`): a bolha mostra o texto da IA linha a linha (P/C/G nas cores dos macros) e, dentro dela, o dia projetado calculado pelo app, nunca pela IA: `Dia: {comido} → {comido + plano} de {teto efetivo} kcal` e `P {x}/{alvo} · C {y}/{alvo} · G {z}/{alvo}` (valores depois do plano). Teto e alvos pelo perfil e pelo `BudgetCalculator` no momento da exibição: o painel acompanha registros feitos depois. Acima do teto efetivo, a linha de kcal em `bad`. Plano já registrado conta uma vez (o painel parte de comido − plano). Plano de outro dia: só o texto. **Registrar assim** (um botão no lugar das ações) com slot sugerido do dia = o caminho da regra 5 nesse slot, `Substituir {slot}?` incluído; sem slot, abre o Trocar sem seleção. O recibo do plano tem as ações da regra 19.
17. Avisos de memória (`chatM`): abaixo da bolha da IA, acima do horário, um chip por linha na ordem `Memória atualizada` (o app aplicou alguma mudança de memória naquele turno), `Memória permanente`, `Memória dinâmica` (a resposta usou fatos desse tipo, `memory_used` conferido pelo app). Recibo que aplicou uma rotina leva `Memória atualizada` logo abaixo; o recibo que reverte uma mudança não repete o aviso. Chips de 28 dp, raio 14, só informativos (sem toque); gold só no check do `Memória atualizada`. Sem aviso, sem linha.
18. Sugestão da rotina (`chatS`): ao abrir o Chat e a cada minuto, se o slot da hora está sem registro e sem pulo hoje e existe uma rotina forte desse slot (permanente, ou dinâmica vista em ≥ 3 dias; várias → a vista em mais dias), um card no fim da conversa (dia vazio: logo abaixo do cartão da meta): `O de sempre no {slot}?`, texto da rotina, `{kcal} kcal · {P}P · {C}C · {G}G` do último registro, chip de origem e dois botões. **Registrar**: registro local com esses números (`source` `routine`) no slot, recibo com as ações da regra 19, reforço da rotina e `Memória atualizada` no recibo, sem POST. **Quase igual**: o texto da rotina substitui o composer, cursor no fim, foco e teclado; nada é gravado. O card some no primeiro envio da tela, com o slot gravado ou pulado ou com a hora em outro slot. Não vai para `chat_message` nem para o prompt.
19. Ações do recibo (`chatG`, `chatF`, `chatD`): só o recibo ativo mais recente de cada slot que ele tocou (em qualquer dia) e só enquanto o slot está exatamente como o recibo deixou; um recibo antigo, sem dados de desfazer, não tem ações. Botões empilhados abaixo do recibo, 44 dp, raio 14, superfície + linha de 1 dp, ícone à esquerda, rótulo 15 sp semibold; Excluir em `bad`. Conjunto: primeiro registro (texto, plano, rotina) → Excluir · Trocar refeição · Editar; foto → Excluir · Trocar refeição; substituição e movimento → Desfazer · Excluir · Trocar refeição · Editar (sem Editar na foto); restauração → Excluir · Trocar refeição · Editar; registro sobre um pulo → Desfazer à frente; pulo → Desfazer. **Excluir**: apaga o registro do slot e reverte a memória do recibo, sem confirmação; marca `Excluído`. **Desfazer**: cada slot tocado volta ao "antes" (registro anterior, registro movido de volta, pulo removido ou de volta) e a memória é revertida; slot que termina com registro ganha o recibo `Restaurado em {slot} · {hora}` `{kcal} kcal`, que fica com as ações; marca `Desfeito`. **Trocar refeição**: Trocar com o slot do registro marcado "(atual)"; slot vazio move o registro (recibo `Movido para {slot} · {hora}`, o antigo fica `Movido`; a memória do recibo é revertida e a rotina dele vale no slot novo); slot com registro pergunta abaixo do recibo (regra 5) e Substituir move e substitui numa transação. **Editar**: apaga o registro como o Excluir, marca `Removido para editar` e põe o texto do registro no composer (troca o rascunho), com foco, teclado e cursor no fim. Recibo marcado fica a 50 % com a marca (ícone + rótulo `muted`) no fim da linha do título. Sem refazer.
20. Memória do recibo: cada recibo guarda a imagem antes/depois dos fatos que o registro mudou. Excluir, Editar, Desfazer e Trocar refeição revertem só o fato que ainda é igual à imagem "depois" (fato adicionado sai, removido volta, alterado recupera texto e dias); fato mudado depois por outro turno fica como está.

## Estados e falhas

- Estados de tela (golds do [ADR-012](../adrs/ADR-012-chat-home-perfil.md) + ADR-023 + ADR-028): `chat0`, `chatL`, `chatE` (Registrar), `chatT`, `chatF` (foto registrada sozinha), `chatG` (recibo com ações), `chatA`, `chatX`, `chatR` (plano), `chatM` (avisos de memória), `chatS` (sugestão da rotina), `chatQ` (pergunta antes da estimativa, ADR-026), `chatU` (Substituir dentro da conversa), `chatD` (substituição desfeita e restauração). `chatP` saiu do Chat (ADR-028); o gold fica como referência do pulo.

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
- [v1-chat](../../server/specifications/v1-chat.md)

## Planos relacionados

- [A5 (Concluído)](../../android/plans/completed/a5-chat.md)
- [A6 (Concluído)](../../android/plans/completed/a6-foto.md)
- [A18 (Concluído)](../../android/plans/completed/a18-chat-registro-foto.md)
- [A19 (Concluído)](../../android/plans/completed/a19-chat-visual.md)
- [A25 (Concluído)](../../android/plans/completed/a25-limite-texto-composer.md)
- [A27 (Concluído)](../../android/plans/completed/a27-chat-v2-texto-intencao.md)
- [A28 (Concluído)](../../android/plans/completed/a28-memoria-v2.md)
- [A29 (Concluído)](../../android/plans/completed/a29-chat-v2-interface.md)
- [A30 (Concluído)](../../android/plans/completed/a30-perguntas-antes-da-estimativa.md)
- [A32 (Concluído)](../../android/plans/completed/a32-chat-rolagem-paginacao.md)
- [A34 (Concluído)](../../android/plans/completed/a34-registro-autonomo.md)
- [A35 (Fora de escopo)](../../android/plans/out_of_scope/a35-registro-retroativo.md)
- `docs/server/plans/completed/s2-v1-chat.md`
- [S3 (Concluído)](../../server/plans/completed/s3-compact.md)

## Critérios de aceite funcionais

- Refeição clara de hoje é registrada sem toque e deixa recibo com as ações (`chatG`; foto `chatF`).
- Refeição em dúvida mostra só Registrar (`chatE`); sem toque, o círculo da Home não muda.
- Refeição para um slot já registrado pergunta dentro da conversa (`chatU`); nada muda até Substituir.
- Desfazer uma substituição traz o registro anterior com o recibo `Restaurado` (`chatD`).
- Excluir, Trocar refeição e Editar agem só pelo recibo mais recente do slot e revertem a mudança de memória daquele registro.
- "pulei o café" pula o slot com recibo; outro dia nunca é registrado.
- Trocar o slot e registrar cai no slot certo.
- A 13ª mensagem do dia não manda 13 raw no prompt.
