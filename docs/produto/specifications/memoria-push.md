# Especificação — Memória, foto, push, Config

## Contexto e objetivo

Perfil editável depois do onboarding. Foto no Chat. Lembrete no horário do slot. Memória curta cifrada. A Config, o diálogo de reinício, a foto e os avisos de memória no Chat e o lembrete seguem os golds da fonte declarada no [inventário](../../qa/README.md#golds), desenhados no Figma `Design` com o design system Aero ([ADR-030](../../design/adrs/ADR-030-own-design-system-aero.md)).

## Escopo

Memória local, foto no Chat, tela Config, push exact por slot, treino do dia.

## Fora de escopo

Firebase, Health/Xiaomi, TDEE, multipart, stream.

## Regras — memória

1. Arquivo interno criptografado `filesDir/memory.bin` (AES-256-GCM, chave no Android Keystore). Gravação atômica: crash no meio de uma gravação mantém a memória anterior. Conteúdo JSON `{"v": 2, "next": {"P", "D", "T"}, "facts": [...], "tombstones": [{"key", "deleted"}]}`; `next` sem `T` é lido como 1; o que não é `v: 2` é lido como memória vazia. Um APK anterior ao A38 não lê arquivo com fatos `T` (testers dev só atualizam para frente). Não vai ao server além do campo `facts` do POST (`days_seen`, `last_seen`; slot de rotina fora dos slots de hoje vai `null`; fato temporário vai com `days_seen` 1 e `last_seen` = data de criação) e de `temp_facts: true`, sempre enviado.
2. Fato: `id` estável (`P{n}` permanente, `D{n}` dinâmico, `T{n}` temporário, nunca reutilizado), `category` `preference` | `portion` | `routine` | `equipment` (um equipamento que o usuário declarou: permanente, sem slot) | `liked` (um prato que o usuário aprovou depois de comer: dinâmico, com o slot e os números daquela refeição) ([ADR-051](../adrs/ADR-051-plan-option-identity-and-chat-discovery.md)), `key` ≤ 40, `text` ≤ 160, `slot` (rotina e `liked`), `source` `explicit` | `promoted` | `observed` | `declared`, dias distintos em que apareceu (só os dos últimos 21) e, na rotina, kcal/P/C/G do último registro (ou os que o modelo estimou numa rotina declarada).
3. A IA só propõe (`memory_updates`, ≤ 5 por turno); o app aplica com regras fixas. `add permanent`: mesma `key` → vira esse fato, permanente, com o texto novo (contradição); senão novo `P{n}` se houver vaga (≤ 50, [ADR-057](../adrs/ADR-057-conversational-onboarding.md) decisão 8), sem vaga é ignorado. `add dynamic`: mesma `key` → reforço; senão novo `D{n}`; acima de 40 sai a dinâmica vista há mais tempo. `reinforce`: hoje entra nos dias; dinâmica troca o texto, permanente não. `replace`: troca o texto; com `kind: permanent` numa dinâmica, vira permanente se houver vaga. `remove`: apaga (pedido explícito). `add temp` ([ADR-029](../adrs/ADR-029-fatos-temporarios-compactacao.md)): só encontra outro temporário — mesma `key` → troca o texto (id e criação mantidos); senão novo `T{n}`; acima de 5 sai o criado há mais tempo. Temporário nunca se junta a permanente ou dinâmico da mesma `key`, e vice-versa. `add temp` de rotina ou com slot é ignorado; `reinforce` de `T` é ignorado; `replace`/`remove` de `T` valem como nos outros tipos. Não há promoção de nem para temporário.
4. Promoção: dinâmica com ≥ 5 dias nos últimos 21 vira permanente (`promoted`), só com vaga. Expiração: dias fora dos 21 saem a cada leitura; dinâmica sem dia sai, menos a rotina declarada, que vive 21 dias desde a criação; permanente nunca expira; temporário sai 3 dias (America/Sao_Paulo) depois de criado (criado 02/10 → some em 05/10). Expiração não é mudança (não mostra `Memória atualizada`). Rotina forte (A29): permanente, ou dinâmica com ≥ 3 dias, com slot e kcal.
5. Quando: preferência, porção, todo temporário e todo `replace`/`remove` na resposta; `add` de rotina declarada (`declared: true`, a resposta à descoberta do Chat) na hora, com os números da proposta e sem contar um dia registrado; `equipment` e `liked` na hora; os outros `add`/`reinforce` de rotina só quando **aquela** estimativa é registrada (registro automático, Registrar, Registrar assim, Confirmar do Trocar, Substituir), com slot e kcal/P/C/G do registro. Estimativa não registrada não vira hábito. Um extra nunca aplica rotina; um registro num dia anterior aplica a rotina com aquela data como dia visto ([ADR-058](../adrs/ADR-058-extras-and-history.md) decisão 6). Excluir, Editar e Desfazer do recibo revertem essa mudança, e Trocar refeição a reaplica no slot novo, só para o fato que ainda está como o recibo deixou ([chat](chat.md) regra 20). Registro e ações do recibo nunca criam, apagam nem revertem temporário: ele não entra nas imagens de memória do recibo. A mensagem com pelo menos uma mudança aplicada ganha `memoryUpdated` (a da IA, ou o recibo do registro); `memory_used` vira `memoryUsedKinds` (ids `T` ignorados). Nada de linha por Gravar nem `Respondeu "…"`.
6. Sobrevive `wipeToday`. Morre no uninstall.
6a. Fatos do onboarding ([ADR-057](../adrs/ADR-057-conversational-onboarding.md), [perfil-onboarding](perfil-onboarding.md) regra 7): os até 30 fatos que `POST /v1/profile` devolve, na ordem de prioridade do servidor, entram como declarados (`source` `declared`, sem dia registrado): a rotina como dinâmica declarada no slot do perfil com os quatro números (vive 21 dias desde a criação, como a rotina declarada da descoberta), os outros como permanentes. Uma `key` que já existe (não temporária) recebe o texto novo, sem duplicar; uma `key` com lápide é pulada; sem vaga, o resto fica de fora. `O que a Tali sabe` mostra todos com `Declarado · dd/MM`.
7. **O que a Tali sabe** (`memL`, [ADR-053](../adrs/ADR-053-visible-memory-screen.md)): Config → `Da Tali` → **O que a Tali sabe** lista todos os fatos em três grupos com uma linha de explicação: `FIXAS` (permanentes que não são rotina: `Valem até você mudar ou apagar.`), `ROTINAS` (rotinas, pratos aprovados e o que foi aprendido: `Aprendidas com o que você registra.`) e `TEMPORÁRIAS` (`Saem sozinhas na data indicada.`); grupo vazio não aparece; memória vazia: `A Tali ainda não guardou nada sobre você.` Cada fato: chip da categoria (`Preferência`, `Porção`, `Equipamento`, `Rotina · {slot}`, `Prato aprovado · {slot}`), lápis e lixeira, o texto, os números `{kcal} kcal · {P}P · {C}C · {G}G` de uma rotina ou prato aprovado com os quatro, e a origem: `Declarado · dd/MM` (fato explícito, rotina declarada ou sem dia registrado), `Registrado N dias · último dd/MM`, `Até dd/MM · criado dd/MM` (temporário: o dia em que sai). Lápis: o texto num campo com **Cancelar** | **Salvar**; salvar troca só o texto (mesma categoria, slot e tipo; vazio não muda nada) e vale no próximo POST. Lixeira: `Apagar da memória?`, `A Tali esquece: {texto}`, **Apagar** | **Cancelar**. Fato apagado (não temporário) deixa uma lápide com a `key` e o dia; um `add` da IA com a mesma `key` é recusado até a próxima compactação guardada, quando as lápides saem. Sem chamada ao modelo. `memory_fact_deleted`, `memory_fact_corrected` (`kind`, `category`). Não há mais ferramenta de memória no dev ([ADR-019](../adrs/ADR-019-ferramentas-dev.md) parcialmente substituído pelo ADR-053).
8. No Chat: `Memória atualizada` abaixo da resposta (ou do recibo) quando o app aplicou uma mudança naquele turno; `Memória permanente` / `Memória dinâmica` quando a resposta usou fatos desse tipo (`memoryUsedKinds`). Temporário usado não ganha selo.
9. Rotina forte do slot da hora vazio vira o card `O de sempre no {slot}?` ([chat](chat.md) regra 18). **Registrar** grava com os números da rotina (`source` `routine`) e reforça a rotina com esse registro (entra hoje nos dias); **Quase igual** só preenche o composer. O card não vai ao server.

## Regras — foto

1. Chat: `TakePicture` + `PickVisualMedia`.
2. Câmera e galeria, qualquer formato (HEIC, WebP, PNG, JPEG): decode com subsample, rotação EXIF aplicada, lado maior reduzido a 2048 px (sem ampliar), JPEG q85, EXIF não copiado ([ADR-018](../../android/adrs/ADR-018-foto-2048.md)). Preview na bolha usa subsample.
3. Cap 16 MB no JPEG, só como defesa. Copy “Foto grande demais.” quando a foto não cabe na memória para decodificar.
4. Timeout 60 s. Foto não persiste no server.

## Regras — Config

1. Edita teto (3 modos) e slots (nome + hora, 2–6 por grupo) à vontade. Modo das refeições independente do teto: Todos os dias, Seg–Sex · Sáb–Dom, Cada dia. same mantém as linhas individuais da Config; os outros mostram uma linha por grupo com quantidade e intervalo. Toque abre editor em tela cheia, com cabeçalho da Config, o passo a passo por grupo, cópia e confirmação de descarte (`cfgS`).
2. Campo “treino hoje” kcal. Null = crédito 0. Some no rollover SP. Também pode ser informado pela Home (linha "Treino de hoje", [home-timeline](home-timeline.md) regra 11): mesmo campo `day.workoutKcal`, mesmo editor (campo 28 pt + linha de crédito ao vivo), mesma validação.
3. Política eat-back 0% / % / 100%.
4. Alvos P/C/G editáveis.
5. Mudou teto: diálogo “Reiniciar registros de hoje?”. Default sim. Confirmar → `wipeToday` (meal_log + skip + digest de hoje). Cancelar → teto não é salvo. Chat UI fica. Prompt do dia recomeça: `wipeToday` grava em `chat_message` um marcador `wiped` (nunca desenhado, nunca enviado) e o prompt só leva raw depois dele.
6. Mudou nome/hora, modo ou dias: não apaga logs. IDs mantidos continuam associados; logs sem slot do dia vão para "Outros" (ADR-021 regra 7).
7. Back → Home.
8. Copy dos sheets: "Treino de hoje" sem texto de apoio abaixo do título; "Horários das refeições": "Mudar nome ou horário não apaga o que você já registrou hoje."
9. Editor de refeições em tela cheia: Continuar por grupo, Salvar na última etapa; Voltar recua ou cancela na primeira. Todo sheet termina no par `Salvar` / `Cancelar` do `Sheet/Bottom`: `Button/Primary` e a pílula secundária, de largura total. Salvar vibra (confirmação), Cancelar vibra leve. A vibração segue a configuração de vibração ao toque do sistema. Controles tocáveis têm ripple.
10. Campos de texto da Config (sheets de edição e editor de refeições): o cursor vai para o fim do valor ao receber foco, e o campo focado fica visível acima do teclado. O sheet nunca passa da altura livre entre a barra de status e o teclado: o título e o par Salvar / Cancelar continuam visíveis e o conteúdo rola. No editor de refeições, o botão fixo do rodapé (Continuar / Salvar) fica atrás do teclado enquanto ele está aberto e volta quando ele fecha.
11. Resetar app ([ADR-040](../adrs/ADR-040-home-card-gestures-app-reset.md)): bloco "Dados" (o último do gold `cfg`), linha "Resetar app" / "Apaga tudo e refaz o onboarding". Toque abre o diálogo `cfgR` ("Resetar o app?", **Apagar tudo** / **Cancelar**). Cancelar ou back não muda nada. Apagar tudo apaga, nesta ordem, como uma reinstalação: marca o onboarding como não feito, cancela os lembretes e o resync e limpa as notificações e preferências de push, apaga o arquivo de memória e as fotos do Chat, esvazia todas as tabelas do Room (as respostas do onboarding incluídas). Depois abre as boas-vindas do onboarding sem back stack (back sai do app). Fica o installation id ([ADR-025](../../content-policy/adrs/ADR-025-safety-correlation-audit.md)). Um reset interrompido termina no próximo cold start, antes do onboarding; respostas salvas de um onboarding em andamento não contam como reset interrompido. Falha: o diálogo fecha e a Config continua. Telemetria `app_reset` só com enums.

12. Tom da Tali (`cfg`, `cfgT`, [ADR-044](../adrs/ADR-044-assistant-tone-and-closures.md)): linha `Tom da Tali` com o valor atual (`Seco` | `Duro`) no bloco de metas, depois dos macros. O toque abre o sheet `Tom da Tali` com as duas opções (`Seco` · `Só os números. Sem opinião.`, selo `PADRÃO`; `Duro` · `Cobra o que estourou e o que faltou. Sem rodeio.`) e o par `Salvar` / `Cancelar`. Salvar grava o tom sem wipe e sem confirmação: o próximo turno já vai com ele. Telemetria `tone_set` (`seco` | `duro`, `onboarding` | `config`), só enums; o tom não é chave do Crashlytics.

13. Receitas ([ADR-052](../adrs/ADR-052-saved-recipes.md)): bloco `Da Tali` antes de `Dados`, linha **Receitas** (`cfg`). A lista (`rcpL`) mostra as receitas salvas no Chat, a mais recente primeiro, cada uma com nome e `{kcal} kcal · {P}P · {C}C · {G}G` da versão atual; sem receita, a linha `Nenhuma receita salva. Peça uma receita no Chat e toque em Salvar receita.` O toque abre a receita (`rcpD`): nome, `Versão {n} · salva em {d de mês}`, os totais, `INGREDIENTES` na tabela `Item` · `Gramas`, `MODO DE PREPARO` em passos numerados e **Excluir receita** (`status/bad`), que pergunta antes (`Excluir receita?`, `{nome} sai da sua lista e a Tali deixa de conhecê-la. Os registros feitos com ela continuam.`, **Excluir** | **Cancelar**) e volta para a lista. Receita não é fato de memória. `recipe_deleted`.

14. Avisos e meta ([ADR-057](../adrs/ADR-057-conversational-onboarding.md) decisões 9–10): bloco `Avisos e meta` depois de `Dados` (as linhas usam `Config/Row`, sem gold próprio; o gold `cfg` termina em `Dados`): **Notificações** (`Ligadas` | `Desligadas`, `Lembretes das refeições e fechamentos`; o toque alterna na hora), **Fechamentos às** (`HH:mm`; o toque abre o diálogo de rodas, OK grava e os alarmes acompanham) e **Meta de peso** (`{kg} kg até dd/MM/aaaa`, `{kg} kg` ou `Nenhuma`; o toque abre o sheet com `Peso` em kg e `Data` opcional `dd/mm/aaaa`; vazio tira a meta). O app confere a meta com os limites de segurança da [política de conteúdo](../../content-policy/specifications/content-policy.md#onboarding-profile) (IMC da meta ≥ 18,5 pela altura do perfil, data depois de hoje, no máximo 1 % do peso atual por semana): fora deles, a nota `Fora dos limites de segurança: …` em `status/bad` e **Salvar** desligado. Telemetria `notifications_set` (`enabled`, `from`) e `goal_set` (`set` | `none`, `from`).

## Regras — push

1. Exact alarm no horário de cada slot do dia corrente em America/Sao_Paulo (inexato quando a permissão não permite exato). Com os avisos desligados (`profile.notificationsEnabled = 0`, pergunta 16 do onboarding ou Config regra 14) nenhum lembrete é agendado nem mostrado.
2. Só dispara se o slot ainda não tem log nem skip.
3. Copy: “{nome}. Ainda não registrou.”
4. Ações: Registrar → Chat. Pular → skip + cancela o alarm daquele slot. O app controla só o ícone pequeno (garfo e faca), o título, as ações e a cor `accent/default` do tema do sistema; o resto é o modelo do sistema.
5. Fds: só os slots cujo days inclui sábado/domingo. Não cair para “só almoço + janta” se ele cadastrou mais. Reagendamento às 00:05 SP, no boot, ao iniciar e ao editar; o fluxo em execução troca o dia em até 30 s. Alarmes de grupos removidos são cancelados.
6. Sem Firebase.
7. Fechamentos ([ADR-044](../adrs/ADR-044-assistant-tone-and-closures.md), [ADR-057](../adrs/ADR-057-conversational-onboarding.md) decisão 10): um alarme no horário do perfil (`profile.closureTime`, padrão 22:00 America/Sao_Paulo) todo dia (exato quando a permissão deixa, inexato senão), reagendado no mesmo fluxo dos lembretes (00:05, boot, início, edição, mudança do horário ou dos avisos); se o fechamento do dia ainda não existe depois desse horário, ele roda na hora. Com os avisos desligados o fechamento roda e fica no card da Home, sem notificação. No alarme o app calcula do Room os números do dia (comido e P/C/G, alvos, teto efetivo, treino, cada refeição com estado e kcal) e, no domingo, os da semana (de segunda até hoje: totais por dia, se teve registro, e a refeição que mais passou da sua parte do teto); pede o texto em `POST /v1/close` com o tom; guarda `{data, período, números, texto}` uma vez por dia ou semana; e notifica `Fechamento do dia` ou `Fechamento da semana` com a primeira linha do texto (o app controla ícone pequeno, título, texto e cor; o resto é o modelo do sistema). Dia sem registro: nenhum texto é pedido e a notificação diz `Nenhum registro.`. Sem rede: `Sem o texto: sem rede.` e uma nova tentativa no próximo início do app. O fechamento não registra nem muda nada. No flavor dev, o broadcast `app.fibrai.android.dev.RUN_CLOSURE` com o extra `period` (`day` | `week`) roda o fechamento na hora, como o alarme ([ADR-019](../adrs/ADR-019-ferramentas-dev.md)). Telemetria `closure` (`day` | `week`, `text` | `fallback` | `offline` | `empty`) e `closure_opened` (`card` | `notification`).

## Fronteiras e ownership

Comportamento: `produto`. Client: `android`.

## Decisões relacionadas

- [ADR-012](../adrs/ADR-012-chat-home-perfil.md)
- [ADR-018](../../android/adrs/ADR-018-foto-2048.md)
- [ADR-019](../adrs/ADR-019-ferramentas-dev.md)
- [ADR-021](../adrs/ADR-021-refeicoes-por-dia.md)
- [ADR-023](../adrs/ADR-023-chat-v2-memoria-v2.md)
- [ADR-029](../adrs/ADR-029-fatos-temporarios-compactacao.md)
- [ADR-040](../adrs/ADR-040-home-card-gestures-app-reset.md)

## Critérios de aceite funcionais

- Memória ≤ 50 fatos permanentes + ≤ 40 dinâmicos + ≤ 5 temporários, texto ≤ 160 por fato; o onboarding contribui com no máximo 30.
- Temporário vale para estimativas dos dias seguintes até ter 3 dias; registros e ações do recibo nunca o removem nem o restauram.
- Push não dispara se o slot já foi gravado ou pulado.
- Wipe do teto não apaga dias anteriores nem o arquivo de memória.
- Resetar app deixa o Room vazio, sem memória, fotos nem lembretes, e abre as boas-vindas do onboarding; o installation id continua.
- A Config oferece a captura de tela com rolagem do sistema (Android 12+) ([ADR-048](../adrs/ADR-048-chat-actions-in-thread-copy-scroll-capture.md)).

## Proveniência

- [A3](../../android/plans/completed/a3-config-wipe-treino.md) — Config + wipe + treino do dia
- [A6](../../android/plans/completed/a6-foto.md) — foto camera + picker
- [A7](../../android/plans/completed/a7-push.md) — push no horario do slot
- [A8](../../android/plans/completed/a8-memoria.md) — memoria criptografada
- [A8b](../../android/plans/completed/a8b-memoria-gravacao-atomica.md) — Memória com gravação atômica
- [A18](../../android/plans/completed/a18-chat-registro-foto.md) — Chat: refeição consolidada, teclado e foto 2048 px
- [A20](../../android/plans/completed/a20-polimento-geral.md) — Polimento: feedback de toque, botões dos sheets, respiro de scroll, Config
- [A22](../../android/plans/completed/a22-treino-home.md) — Treino na Home
- [A23](../../android/plans/completed/a23-editor-memoria-dev.md) — Editor de memória e perfil da IA (só dev)
- [A24](../../android/plans/completed/a24-refeicoes-por-dia.md) — Refeições por dia da semana
- [A28](../../android/plans/completed/a28-memoria-v2.md) — Memória v2: fatos permanentes e dinâmicos
- [A29](../../android/plans/completed/a29-chat-v2-interface.md) — Chat v2: plano de refeição, avisos de memória e sugestão da rotina
- [A34](../../android/plans/completed/a34-registro-autonomo.md) — Autonomous record, receipts with actions
- [A38](../../android/plans/completed/a38-fatos-temporarios-compactacao.md) — Temp facts on the device, suggested slot in the history, compaction that keeps the open tail
- [A43](../../android/plans/completed/a43-chat-records-memory-aero.md) — Chat records and memory on Aero
- [A44](../../android/plans/completed/a44-config-push-aero.md) — Config and push on Aero
- [A46](../../android/plans/completed/a46-input-cursor-keyboard.md) — Cursor no fim do valor e campo acima do teclado
- [A53](../../android/plans/completed/a53-config-app-reset.md) — Config: resetar o app
- [A61](../../android/plans/completed/a61-chat-copy-scroll-capture-inline-actions.md) — captura de tela com rolagem
- [A67](../../android/plans/completed/a67-plan-options-and-discovery.md) — fatos `equipment` e `liked`, rotina declarada na descoberta
- [A68](../../android/plans/completed/a68-saved-recipes.md) — Config → Receitas, lista e receita
- [A69](../../android/plans/completed/a69-visible-memory.md) — O que a Tali sabe: fatos com origem, apagar com lápide, corrigir
- [A72](../../android/plans/pending_manual_validation/a72-extras-and-history.md) — um extra não vira rotina; um registro de outro dia conta aquele dia e não traz temporário nem prato aprovado
- [A71](../../android/plans/pending_manual_validation/a71-conversational-onboarding.md) — teto permanente 50, fatos declarados do onboarding, horário do fechamento, avisos ligados ou desligados, meta na Config
