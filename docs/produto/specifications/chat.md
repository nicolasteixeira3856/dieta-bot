# Especificação — Chat

## Estado

Vigente desde o [A5](../../android/plans/completed/a5-chat.md): tela Chat aberta pelo FAB da Home, `POST /v1/chat`. Compactação ligada desde o [A5b](../../android/plans/completed/a5b-ligar-compact.md) (server: [S3](../../server/plans/completed/s3-compact.md)).

## Contexto e objetivo

Toda interação com a IA. Registro só depois do tap no chip.

## Escopo

Tela Chat, bolhas, composer, chips, prompt do dia, compactação.

## Fora de escopo

Avatar, visto, status, streaming neste corte, tool invisível que grava meal_log.

## Regras funcionais

1. Tela cheia. Back → Home.
2. Bolhas: user à direita, IA à esquerda. Sem foto de perfil.
3. Composer: texto + clipe (foto) + send.
4. Após estimate da IA: barra com “Gravar {slot}” (slot sugerido pelo server; some se não for do perfil), “Trocar” (sheet com os slots, o da hora marcado “(atual)”) e “Pular” (confirmação “Deseja pular o {slot}?”; sem slot sugerido, o da hora). As ações valem só para a última estimativa sem recibo.
5. Tap Gravar: `addLog` local com o último estimate (kcal, p, c, g, text). Sem novo POST.
6. Sem tap: número não entra no contador.
7. Pular: status skipped no slot. Sem kcal. Gravar e Pular deixam um recibo na conversa (“Registrado em {slot} · {hora} +{kcal} kcal”); recibos não vão ao server.
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
- JPEG > 16 MB: recusa no client. Não sobe.
- Sem rede: bolha de falha. Room intocado.

## Fronteiras e ownership

Comportamento: `produto`. UI e Room: `android`. Contrato HTTP: `server`.

## Decisões relacionadas

- [ADR-012](../adrs/ADR-012-chat-home-perfil.md)
- [v1-chat](../../server/specifications/v1-chat.md)

## Planos relacionados

- [A5 (Concluído)](../../android/plans/completed/a5-chat.md)
- `docs/android/plans/a6-foto.md`
- `docs/server/plans/completed/s2-v1-chat.md`
- [S3 (Concluído)](../../server/plans/completed/s3-compact.md)

## Critérios de aceite funcionais

- Gravar sem tap não altera o círculo da Home.
- Trocar o slot e gravar cai no slot certo.
- A 13ª mensagem do dia não manda 13 raw no prompt.
