# Especificação — Chat

## Estado

Vigente desde o [A5](../../android/plans/completed/a5-chat.md): tela Chat aberta pelo FAB da Home, `POST /v1/chat`. Compactação ligada desde o [A5b](../../android/plans/completed/a5b-ligar-compact.md) (server: [S3](../../server/plans/completed/s3-compact.md)).

Um registro por refeição com confirmação ao substituir, Enter pula linha e foto 2048 px desde o [A18](../../android/plans/pending_manual_validation/a18-chat-registro-foto.md) ([ADR-017](../adrs/ADR-017-registro-consolidado.md), [ADR-018](../../android/adrs/ADR-018-foto-2048.md); server: [S8](../../server/plans/pending_manual_validation/s8-chat-json-slot-consolidado.md)): regras 3, 5 e 7 e "Estados e falhas".

Pergunta em bolha própria, bolhas iguais, sem "IA ATIVA" e foto como anexo com preview desde o [A19](../../android/plans/pending_manual_validation/a19-chat-visual.md) (gate [ST1](../../stitch/plans/completed/st1-chat.md)): regras 2, 3, 14 e 15.

Limite de 2000 caracteres com estado de erro (`Texto muito longo`), sem corte silencioso, desde o [A25](../../android/plans/pending_manual_validation/a25-limite-texto-composer.md) ([ADR-022](../adrs/ADR-022-limite-texto-chat.md); server: [S9](../../server/plans/completed/s9-limite-texto-2000.md); gate [ST5](../../stitch/plans/completed/st5-chat-texto-longo.md)): regra 3 e "Estados e falhas".

Intenção (`log` / `plan` / `question`), texto gravado = `meal_text` e histórico de 7 dias no prompt desde o [A27](../../android/plans/pending_manual_validation/a27-chat-v2-texto-intencao.md) ([ADR-023](../adrs/ADR-023-chat-v2-memoria-v2.md); server: [S11](../../server/plans/pending_manual_validation/s11-chat-v2.md)): regras 4, 5, 8, 12 e 14.

Memória em fatos no prompt (`facts`), `memory_updates` aplicados pelo app e origem por mensagem desde o [A28](../../android/plans/pending_manual_validation/a28-memoria-v2.md): regras 8 e 12.

## Mudanças decididas, ainda não vigentes (ADR-023)

O [ADR-023](../adrs/ADR-023-chat-v2-memoria-v2.md) (30/09/2026) muda este Chat. As regras abaixo só passam a valer quando cada plano for implementado. Até lá, vale o texto das regras numeradas.

- **Plano** (`chatR`): painel do dia projetado calculado pelo app e botão Registrar assim ([A29](../../android/plans/a29-chat-v2-interface.md)).
- **Avisos de memória** (`chatM`): `Memória atualizada` e chips `Memória permanente` / `Memória dinâmica` ([A29](../../android/plans/a29-chat-v2-interface.md)).
- **Sugestão da rotina** (`chatS`): `O de sempre no {slot}?` com Registrar e Quase igual ([A29](../../android/plans/a29-chat-v2-interface.md)).

## Contexto e objetivo

Toda interação com a IA. Registro só depois do tap no chip.

## Escopo

Tela Chat, bolhas, composer, chips, prompt do dia, compactação.

## Fora de escopo

Avatar, visto, status, streaming neste corte, tool invisível que grava meal_log.

## Regras funcionais

1. Tela cheia. Back → Home.
2. Bolhas: user à direita, IA à esquerda, todas com o mesmo raio (16 dp nos 4 cantos). Rótulo da IA: ícone + "Dieta Bot AI", sem selo. Sem foto de perfil.
3. Composer: texto + câmera (foto) + send. Enter pula linha (até 5 linhas visíveis); só a seta envia. A foto (câmera ou galeria) vira anexo no composer, com miniatura e ✕; nada sai antes do toque em enviar. Uma foto por vez: outra substitui a anterior. Send ativo com texto ou anexo; manda os dois juntos. ✕, troca ou sair do Chat com o anexo apagam o arquivo. Texto até 2000 caracteres (code points do texto sem espaços nas pontas; emoji conta 1), nada é cortado. Acima de 2000 (`chatX`): borda da caixa em `bad`, `Texto muito longo` logo abaixo, enviar e câmera desabilitados; sem contador. Com mais de uma linha a caixa tem cantos de 24 dp.
4. Após estimate da IA: barra com “Gravar {slot}” (slot sugerido pelo server; some se não for dos slots do dia), “Trocar” (sheet com só os slots do dia, o da hora marcado “(atual)”) e “Pular” (confirmação “Deseja pular o {slot}?”; sem slot sugerido, o da hora). As ações valem só para a última estimativa sem recibo com intenção `log` (ou sem intenção, server antigo). `plan` com estimativa: a estimativa fica guardada, mas a bolha aparece sem card e sem ações. `question`: só a bolha.
5. Tap Gravar (ou Confirmar no Trocar): slot sem registro hoje → `addLog` local com o último estimate (kcal, p, c, g, texto da regra 12). Slot com registro → confirmação no layout do `chatP`: `Substituir {slot}?`, `{slot} tem {kcal antigo} kcal. Fica com {kcal novo} kcal.`, **Substituir** | **Outra refeição** (fecha e abre o Trocar sem seleção). Substituir troca os registros de hoje daquele slot pelo novo numa transação ([ADR-017](../adrs/ADR-017-registro-consolidado.md)). Back ou toque fora só fecha. Sem novo POST.
6. Sem tap: número não entra no contador.
7. Pular: status skipped no slot. Sem kcal. Gravar, Substituir e Pular deixam um recibo na conversa (“Registrado em {slot} · {hora} +{kcal} kcal”, “Atualizado em {slot} · {hora} {kcal} kcal”); recibos não vão ao server.
8. Perfil e snapshot só incluem slots do dia em America/Sao_Paulo (ADR-021/A24), assim como sugestão da hora, Trocar e Pular. Logs de slots de outros grupos continuam nos totais. Prompt do turno: perfil + memória em fatos (`facts`, ≤ 30 permanentes + ≤ 40 dinâmicos, regras em [memoria-push](memoria-push.md)) + snapshot Room (slots, kcal, P/C/G, pulou, saldo, `remaining_kcal` = teto efetivo − comido, pode ser negativo) + `recent` (registros dos 7 dias antes de hoje, mais antigo primeiro, no dia pela hora do slot; slot apagado ou nulo = `Outros`; texto ≤ 240; ≤ 42, ficam os mais recentes) + ≤ 2 digest + ≤ 12 raw do dia.
9. Ao fechar 12 raw desde o último digest: próximo POST manda `compact=true`. Resumo substitui o bloco. Máx 2 digest. Terceiro bloco substitui o digest mais velho.
10. Snapshot do dia não compacta.
11. Dia SP vira: digest some do prompt. UI do fio 60 d continua com separador de data.
12. Texto da timeline ao gravar: o `meal_text` da estimativa (a refeição inteira, corrigida pela conversa). Sem `meal_text`: a primeira mensagem do usuário da cadeia daquela estimativa, pulando as respostas a perguntas (mensagem logo depois de uma estimativa com pergunta); se essa mensagem tem foto, a descrição que a IA devolveu. Nada disso: nomes dos itens. Nunca a resposta a uma pergunta. Gravar não escreve linha de memória: aplica a rotina que a IA propôs para aquela estimativa, se houver.
13. O client deste corte chama `POST /v1/chat`. Não chama `/v1/estimate` nem `/v1/fit` a partir do Chat.
14. 1 pergunta se confiança ≠ alto (só em `log`), numa bolha própria logo abaixo da estimativa (barra gold à esquerda, ícone de ajuda, texto em cor primária). O horário fica só nela. Assunção pergunta 1×.
15. Dia sem mensagens: saudação fixa, cartão “Meta calórica de hoje” (restante / meta) e chips de sugestão que preenchem o composer. Com foto anexada, os chips somem. Nada disso é gravado nem enviado.

## Estados e falhas

- Timeout 60 s: bolha “Não deu. Toque para tentar de novo.”. Nada gravado; o toque reenvia.
- Foto: o client reduz a 2048 px no lado maior, JPEG q85, sem EXIF ([ADR-018](../../android/adrs/ADR-018-foto-2048.md)). “Foto grande demais.” só quando a foto não cabe na memória para decodificar. Cap de 16 MB fica como defesa.
- Sem rede: bolha de falha. Room intocado.
- Texto acima de 2000 caracteres: estado de erro do `chatX`, nada sai (0 POST, nem com foto). Apagar até 2000 volta a enviar.

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
- [A19 (Pendente aprovação manual)](../../android/plans/pending_manual_validation/a19-chat-visual.md)
- [A25 (Pendente aprovação manual)](../../android/plans/pending_manual_validation/a25-limite-texto-composer.md)
- [A27 (Pendente aprovação manual)](../../android/plans/pending_manual_validation/a27-chat-v2-texto-intencao.md)
- `docs/server/plans/completed/s2-v1-chat.md`
- [S3 (Concluído)](../../server/plans/completed/s3-compact.md)

## Critérios de aceite funcionais

- Gravar sem tap não altera o círculo da Home.
- Trocar o slot e gravar cai no slot certo.
- A 13ª mensagem do dia não manda 13 raw no prompt.
