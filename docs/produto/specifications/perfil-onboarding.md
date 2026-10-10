# Especificacao — Perfil e onboarding

## Contexto e objetivo

Perfil persistente que a IA recebe em todo turno. O onboarding é um chat guiado com a Tali ([ADR-057](../adrs/ADR-057-conversational-onboarding.md)): as perguntas são do app, sem chamada ao modelo enquanto o usuário responde; uma única chamada `POST /v1/profile` no fim monta o perfil e os fatos declarados. Layout da splash e de `ob0`–`ob6`: golds da fonte declarada no [inventário](../../qa/README.md#golds), desenhados no Figma `Design` com o design system Aero ([ADR-030](../../design/adrs/ADR-030-own-design-system-aero.md)).

## Escopo

Boas-vindas, chat guiado (16 perguntas), resumo, montagem, sucesso e erro. Edição posterior = Config. TMB só sugere.

## Fora de escopo

TDEE de manutenção como meta oculta. Nutricionista. Health/Xiaomi. 2 g/kg. Foto, áudio ou turno do modelo dentro do onboarding. Refazer o onboarding de quem já o terminou. Editar as respostas livres depois do perfil montado (os fatos se editam em `O que a Tali sabe`).

## Regras funcionais

1. Boas-vindas (`ob0`): primeira tela depois da splash com `onboardingDone = 0`. Logo com animação lenta (flutua e respira, Compose, tokens Aero), `Bem-vindo ao Fibrai`, `Umas perguntas rápidas e a Tali monta o seu perfil.` e o CTA **Vamos começar** com a seta. Voltar sai do app.
2. Chat guiado (`ob1`, `ob1e`, `ob2`): no topo, o progresso (`Onboarding/Progress`): a seção (`PERFIL` 1–5, `REFEIÇÕES` 6–7, `ROTINA` 8–15, `AVISOS` 16), `{n} de 16` e a barra com n/16 preenchido. Bolhas da Tali (rótulo com o avatar, 308 dp de largura) e do usuário (à direita, até 308 dp), a hora em cada bolha. As respostas fechadas aparecem como botões (`Onboarding/QuickReply`) abaixo da pergunta ativa; o toque envia. Composer só de texto (sem câmera), placeholder `Escreva sua resposta`; nas perguntas livres, no teto proposto e nas refeições, o contador `{n}/2000` abaixo da caixa (acima de 2000 o contador fica em `status/bad` e o envio desliga). O fio abre na mensagem da Tali anterior à última mensagem do usuário: cada troca começa no topo da tela, e as anteriores ficam acima, roláveis.
3. Perguntas, nesta ordem (ADR-057 decisão 3): 1 sexo (`Feminino` · `Masculino`); 2 idade (13–120); 3 altura (100–250 cm; `1,80` vale 180); 4 peso (25–400 kg, uma casa decimal); 5 teto e macros: a Tali diz a TMB (Mifflin-St Jeor: homem 10p+6,25a−5i+5; mulher 10p+6,25a−5i−161) e o IMC (kg/m², uma casa) como informação e propõe o teto = TMB arredondada a 10 com 30/40/30 (4/4/9 kcal/g); o composer vem preenchido com `{teto} kcal · P {p} · C {c} · G {g}` para editar ou enviar (aceita também um número só: teto com 30/40/30); 6 número de refeições (`2`–`6`); 7 nomes e horários: **Usar o padrão** (os horários padrão da quantidade com o nome usual da hora: Café da manhã, Lanche da manhã, Almoço, Lanche, Jantar, Ceia) ou uma linha `nome HH:mm` por refeição (também `19h30`, separadas por linha, `;` ou `,`), na quantidade da pergunta 6; 8 compensação do treino (`0 %` · `50 %` · `100 %` ou outro valor 0–100); 9 tom (`Seco` pré-selecionado, `Duro`, um exemplo de cada); 10 restrições (`Nenhuma`, `Sem lactose`, `Sem glúten`, `Vegetariano`, `Vegano`, `Diabetes`, `Hipertensão` ou texto livre); 11 medidas (`Balança` · `Medidas caseiras` ou texto); 12 comidas habituais e quando (texto); 13 o que não come (texto ou **Pular**); 14 equipamentos (`Air fryer`, `Micro-ondas`, `Panela de pressão`, `Nenhum` ou texto); 15 meta de peso (`60 kg até 30/04/2027`, a data é opcional, ou **Pular**); 16 avisos: `Sim` · `Não`, ou um horário (`21:30`, `sim, 21:30`) para o fechamento (padrão 22:00). As respostas fechadas aceitam variações em pt-BR sem acento; uma resposta que o app não entende acrescenta `Desculpa, não entendi. Pode repetir?` e repete os botões (`ob1e`). As livres valem até 2000 caracteres (code points).
4. Um `sim` na pergunta 16 pede a permissão de notificações do sistema ali mesmo (Android 13+); a Home não pergunta de novo.
5. Toda resposta é gravada no aparelho na hora (`onboarding_answer`, [Room](../../android/specifications/room-v2.md)). Voltar no chat desfaz a última resposta e repete a pergunta com ela no composer; na primeira pergunta, volta às boas-vindas.
6. Resumo (`ob3`): `Resumo do seu perfil`, `Confira antes de montar. Toque no lápis para corrigir.` e um bloco por grupo (`Onboarding/SummaryBlock`): Corpo (sexo · idade · altura · peso; `TMB {n} kcal · IMC {x}`), Teto e macros, Refeições (duas por linha), Treino, Tom, Restrições, Medidas, Comidas (com `Não gosta: …`), Equipamentos, Meta de peso (`Sem meta`), Avisos (`Ligados · fechamento às {HH:mm}` | `Desligados`). O toque num bloco reabre as perguntas dele no chat, cada uma com a resposta no composer, e volta ao resumo; mudar a quantidade de refeições pede as refeições de novo. Legenda `IMC e TMB são informação. Estimativa, não consulta.` e o CTA **Confirmar e montar o perfil**. Nada vai ao servidor antes do toque. Voltar no resumo reabre a pergunta 16.
7. Montagem (`ob4`): tela cheia com o logo animado, `Estamos montando o seu perfil`, `Só mais alguns instantes…` e o loader, enquanto `POST /v1/profile` roda (até 30 s no app: os 25 s do servidor e a rede). No 200, numa ordem que sobrevive ao processo morrer: as refeições (todos os dias, modo `same`) e o perfil (teto em `same`, compensação, alvos, corpo, tom, meta aceita, horário do fechamento, avisos, primeiro dia = hoje); os fatos do servidor na memória cifrada como declarados ([memoria-push](memoria-push.md) regra 2); por fim `onboardingDone = 1` e as respostas apagadas. Depois, sucesso.
8. Sucesso (`ob5`): tela `status/good`, check, `Perfil pronto`, `A Tali já sabe o seu teto, as suas refeições e o que você come.` e **Ir para a Home**. Meta recusada pelo servidor (`goal_refused`): a mesma tela com a linha `Meta de peso não aplicada.`; o perfil fica sem meta.
9. Erro (`ob6`): qualquer falha (400 de conteúdo, 422, 5xx, tempo esgotado, sem rede): tela `status/bad`, `!`, `Não deu certo`, `Tente de novo mais tarde. Suas respostas ficam salvas.`, **Tentar de novo** (a mesma chamada) sobre **Voltar ao chat** (o resumo), empilhados.
10. Retomada (ADR-057 decisão 7): fechar o app em qualquer ponto reabre no mesmo lugar, pela fase em `profile.onboardingPhase`: no chat (o fio refeito das respostas, na próxima pergunta), no resumo, na montagem (a chamada é refeita) ou no erro. Só sai do onboarding pelo sucesso, pelo reset do app ou por reinstalar. Quem tem `onboardingDone = 1` nunca vê o onboarding.
11. Prefixo do chat: teto vigente, eat-back, alvos P/C/G, lista nome+hora só dos slots do dia em America/Sao_Paulo (ADR-021), o tom (`profile.tone`) e a meta aceita (`profile.goal`), em todo turno normal e de compactação.
12. Disclaimer na Home com a copy ("Estimativa nutricional, nao substitui consulta medica ou nutricional."). A splash não tem disclaimer (desde A51); o resumo tem a legenda da regra 6.
13. Nome visível ([ADR-034](../adrs/ADR-034-fibrai-brand-tali-assistant.md)): a splash mostra só o logo (semente de aveia, 160 dp, centralizado); as boas-vindas dizem "Fibrai" no título; o app aparece como "Fibrai" ("Fibrai Dev" no flavor dev) no launcher e nas notificações.
14. Telemetria (ADR-057 decisão 11), só enums e números: `onboarding_started`, `onboarding_step` (`step`: o nome da pergunta), `onboarding_resumed` (`phase`, `answered`), `onboarding_complete` (`slots`, `eat`, `facts`, `goal` set | refused | none, `notifications`), `onboarding_error` (`reason`: `blocked` | `network` | `server` | `timeout`).

## Estados e falhas

- Resposta não entendida: `ob1e`; nada é gravado.
- Erro da montagem: `ob6`; as respostas ficam.
- Teto < 800: aceito se o usuário digitou.
- Reinstalação: Room some. Onboarding de novo.
- Resetar app na Config ([memoria-push](memoria-push.md) Config regra 11): como reinstalação, boas-vindas de novo. Respostas salvas de um onboarding em andamento nunca são tomadas por um reset interrompido.

## Fronteiras e ownership

Dono: produto. Implementação: `android`. Rota: [HTTP contract](../../api-contract.md#post-v1profile).

## Decisoes relacionadas

- [ADR-012](../adrs/ADR-012-chat-home-perfil.md)
- [ADR-021](../adrs/ADR-021-refeicoes-por-dia.md)
- [ADR-044](../adrs/ADR-044-assistant-tone-and-closures.md)
- [ADR-057](../adrs/ADR-057-conversational-onboarding.md)

## Criterios de aceite funcionais

- Boas-vindas, 16 perguntas nesta ordem, resumo, montagem e sucesso; o tom começa em Seco e chega ao servidor no primeiro turno.
- O teto e os macros vêm propostos no composer e são editáveis.
- Uma resposta fechada inválida repete a pergunta com os botões.
- Matar o processo na pergunta 9 reabre na pergunta 9; um 500 mostra `ob6` e **Tentar de novo** chega a `ob5`.
- Perfil, refeições e fatos declarados lidos de volta após kill do processo; as respostas cruas somem.
- O resumo oferece a captura de tela com rolagem do sistema (Android 12+) quando passa da tela ([ADR-048](../adrs/ADR-048-chat-actions-in-thread-copy-scroll-capture.md)).

## Proveniência

- [A2](../../android/plans/completed/a2-onboarding-perfil.md) — Onboarding perfil
- [A21](../../android/plans/completed/a21-seletor-horario.md) — Seletor de horário em rodas
- [ST3](../../stitch/plans/completed/st3-seletor-horario.md) — Seletor de horário em rodas
- [A24](../../android/plans/completed/a24-refeicoes-por-dia.md) — Refeições por dia da semana
- [ST4](../../stitch/plans/completed/st4-refeicoes-por-dia.md) — Refeições por dia da semana
- [A31](../../android/plans/completed/a31-o1-perfil-obrigatorio-teclado.md) — O1: required profile and keyboard flow
- [ST8](../../stitch/plans/completed/st8-teto-sem-perfil.md) — Ceiling screen before the profile (`o1e`)
- [A41](../../android/plans/completed/a41-splash-onboarding-aero.md) — Splash e onboarding no Aero
- [A46](../../android/plans/completed/a46-input-cursor-keyboard.md) — Cursor no fim do valor e campo acima do teclado
- [A49](../../android/plans/completed/a49-fibrai-tali-visible-rename.md) — Fibrai e Tali no app
- [A53](../../android/plans/completed/a53-config-app-reset.md) — Config: resetar o app
- [A60](../../android/plans/completed/a60-tone-formatting-planned-skips.md) — O5: o tom da Tali
- [D20](../../design/plans/completed/d20-figma-review-inline-actions.md) — contador `n/5` nos golds da O1–O4
- [A61](../../android/plans/completed/a61-chat-copy-scroll-capture-inline-actions.md) — contador `n/5`, captura de tela com rolagem
- [D27](../../design/plans/completed/d27-conversational-onboarding.md) — golds `ob0`–`ob6`
- [S41](../../server/plans/pending_manual_validation/s41-onboarding-profile.md) — `POST /v1/profile`
- [A71](../../android/plans/pending_manual_validation/a71-conversational-onboarding.md) — onboarding conversacional no app
