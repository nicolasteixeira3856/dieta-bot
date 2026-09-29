# Especificação — Chat

## Estado

Vigente desde o [A5](../../android/plans/completed/a5-chat.md): tela Chat aberta pelo FAB da Home, `POST /v1/chat`. Compactação ligada desde o [A5b](../../android/plans/completed/a5b-ligar-compact.md) (server: [S3](../../server/plans/completed/s3-compact.md)).

Um registro por refeição com confirmação ao substituir, Enter pula linha e foto 2048 px desde o [A18](../../android/plans/pending_manual_validation/a18-chat-registro-foto.md) ([ADR-017](../adrs/ADR-017-registro-consolidado.md), [ADR-018](../../android/adrs/ADR-018-foto-2048.md); server: [S8](../../server/plans/pending_manual_validation/s8-chat-json-slot-consolidado.md)): regras 3, 5 e 7 e "Estados e falhas".

Mudanças planejadas (29/09/2026, aguardando aprovação): pergunta em bolha própria, bolhas iguais, sem "IA ATIVA", anexo com preview ([A19](../../android/plans/a19-chat-visual.md), gate [ST1](../../stitch/plans/st1-chat.md)). Regras 2, 4 e 14 mudam na entrega desse plano. Limite de 2000 caracteres com estado de erro (`Texto muito longo`), sem corte silencioso ([ADR-022](../adrs/ADR-022-limite-texto-chat.md), [S9](../../server/plans/s9-limite-texto-2000.md), [A25](../../android/plans/a25-limite-texto-composer.md), gate [ST5](../../stitch/plans/st5-chat-texto-longo.md)): regra 3 e "Estados e falhas".

## Contexto e objetivo

Toda interação com a IA. Registro só depois do tap no chip.

## Escopo

Tela Chat, bolhas, composer, chips, prompt do dia, compactação.

## Fora de escopo

Avatar, visto, status, streaming neste corte, tool invisível que grava meal_log.

## Regras funcionais

1. Tela cheia. Back → Home.
2. Bolhas: user à direita, IA à esquerda. Sem foto de perfil.
3. Composer: texto + clipe (foto) + send. Enter pula linha (até 5 linhas visíveis); só a seta envia.
4. Após estimate da IA: barra com “Gravar {slot}” (slot sugerido pelo server; some se não for do perfil), “Trocar” (sheet com os slots, o da hora marcado “(atual)”) e “Pular” (confirmação “Deseja pular o {slot}?”; sem slot sugerido, o da hora). As ações valem só para a última estimativa sem recibo.
5. Tap Gravar (ou Confirmar no Trocar): slot sem registro hoje → `addLog` local com o último estimate (kcal, p, c, g, text). Slot com registro → confirmação no layout do `chatP`: `Substituir {slot}?`, `{slot} tem {kcal antigo} kcal. Fica com {kcal novo} kcal.`, **Substituir** | **Outra refeição** (fecha e abre o Trocar sem seleção). Substituir troca os registros de hoje daquele slot pelo novo numa transação ([ADR-017](../adrs/ADR-017-registro-consolidado.md)). Back ou toque fora só fecha. Sem novo POST.
6. Sem tap: número não entra no contador.
7. Pular: status skipped no slot. Sem kcal. Gravar, Substituir e Pular deixam um recibo na conversa (“Registrado em {slot} · {hora} +{kcal} kcal”, “Atualizado em {slot} · {hora} {kcal} kcal”); recibos não vão ao server.
8. Prompt do turno: perfil + memória ≤ 1,3 k tok + snapshot Room (slots, kcal, P/C/G, pulou, saldo) + ≤ 2 digest + ≤ 12 raw do dia.
9. Ao fechar 12 raw desde o último digest: próximo POST manda `compact=true`. Resumo substitui o bloco. Máx 2 digest. Terceiro bloco substitui o digest mais velho.
10. Snapshot do dia não compacta.
11. Dia SP vira: digest some do prompt. UI do fio 60 d continua com separador de data.
12. A descrição que a IA devolve na foto é o texto da timeline se o user gravar.
13. O client deste corte chama `POST /v1/chat`. Não chama `/v1/estimate` nem `/v1/fit` a partir do Chat.
14. 1 pergunta se confiança ≠ alto. Assunção pergunta 1×.
15. Dia sem mensagens: saudação fixa, cartão “Meta calórica de hoje” (restante / meta) e chips de sugestão que preenchem o composer. Nada disso é gravado nem enviado.

## Estados e falhas

- Timeout 60 s: bolha “Não deu. Toque para tentar de novo.”. Nada gravado; o toque reenvia.
- Foto: o client reduz a 2048 px no lado maior, JPEG q85, sem EXIF ([ADR-018](../../android/adrs/ADR-018-foto-2048.md)). “Foto grande demais.” só quando a foto não cabe na memória para decodificar. Cap de 16 MB fica como defesa.
- Sem rede: bolha de falha. Room intocado.

## Fronteiras e ownership

Comportamento: `produto`. UI e Room: `android`. Contrato HTTP: `server`.

## Decisões relacionadas

- [ADR-012](../adrs/ADR-012-chat-home-perfil.md)
- [ADR-017](../adrs/ADR-017-registro-consolidado.md)
- [ADR-018](../../android/adrs/ADR-018-foto-2048.md)
- [v1-chat](../../server/specifications/v1-chat.md)

## Planos relacionados

- [A5 (Concluído)](../../android/plans/completed/a5-chat.md)
- [A6 (Concluído)](../../android/plans/completed/a6-foto.md)
- [A18 (Pendente aprovação manual)](../../android/plans/pending_manual_validation/a18-chat-registro-foto.md)
- `docs/server/plans/completed/s2-v1-chat.md`
- [S3 (Concluído)](../../server/plans/completed/s3-compact.md)

## Critérios de aceite funcionais

- Gravar sem tap não altera o círculo da Home.
- Trocar o slot e gravar cai no slot certo.
- A 13ª mensagem do dia não manda 13 raw no prompt.
