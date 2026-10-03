# Especificação — Memória, foto, push, Config

## Contexto e objetivo

Perfil editável depois do onboarding. Foto no Chat. Lembrete no horário do slot. Memória curta cifrada.

## Escopo

Memória local, foto no Chat, tela Config, push exact por slot, treino do dia.

## Fora de escopo

Firebase, Health/Xiaomi, TDEE, multipart, stream.

## Regras — memória

1. Arquivo interno criptografado `filesDir/memory.bin` (AES-256-GCM, chave no Android Keystore). Gravação atômica: crash no meio de uma gravação mantém a memória anterior. Conteúdo JSON `{"v": 2, "next": {"P", "D", "T"}, "facts": [...]}`; `next` sem `T` é lido como 1; o que não é `v: 2` é lido como memória vazia. Um APK anterior ao A38 não lê arquivo com fatos `T` (testers dev só atualizam para frente). Não vai ao server além do campo `facts` do POST (`days_seen`, `last_seen`; slot de rotina fora dos slots de hoje vai `null`; fato temporário vai com `days_seen` 1 e `last_seen` = data de criação) e de `temp_facts: true`, sempre enviado.
2. Fato: `id` estável (`P{n}` permanente, `D{n}` dinâmico, `T{n}` temporário, nunca reutilizado), `category` `preference` | `portion` | `routine`, `key` ≤ 40, `text` ≤ 160, `slot` (só rotina), `source` `explicit` | `promoted` | `observed`, dias distintos em que apareceu (só os dos últimos 21) e, na rotina, kcal/P/C/G do último registro.
3. A IA só propõe (`memory_updates`, ≤ 5 por turno); o app aplica com regras fixas. `add permanent`: mesma `key` → vira esse fato, permanente, com o texto novo (contradição); senão novo `P{n}` se houver vaga (≤ 30), sem vaga é ignorado. `add dynamic`: mesma `key` → reforço; senão novo `D{n}`; acima de 40 sai a dinâmica vista há mais tempo. `reinforce`: hoje entra nos dias; dinâmica troca o texto, permanente não. `replace`: troca o texto; com `kind: permanent` numa dinâmica, vira permanente se houver vaga. `remove`: apaga (pedido explícito). `add temp` ([ADR-029](../adrs/ADR-029-fatos-temporarios-compactacao.md)): só encontra outro temporário — mesma `key` → troca o texto (id e criação mantidos); senão novo `T{n}`; acima de 5 sai o criado há mais tempo. Temporário nunca se junta a permanente ou dinâmico da mesma `key`, e vice-versa. `add temp` de rotina ou com slot é ignorado; `reinforce` de `T` é ignorado; `replace`/`remove` de `T` valem como nos outros tipos. Não há promoção de nem para temporário.
4. Promoção: dinâmica com ≥ 5 dias nos últimos 21 vira permanente (`promoted`), só com vaga. Expiração: dias fora dos 21 saem a cada leitura; dinâmica sem dia sai; permanente nunca expira; temporário sai 3 dias (America/Sao_Paulo) depois de criado (criado 02/10 → some em 05/10). Expiração não é mudança (não mostra `Memória atualizada`). Rotina forte (A29): permanente, ou dinâmica com ≥ 3 dias, com slot e kcal.
5. Quando: preferência, porção, todo temporário e todo `replace`/`remove` na resposta; `add`/`reinforce` de rotina só quando **aquela** estimativa é registrada (registro automático, Registrar, Registrar assim, Confirmar do Trocar, Substituir), com slot e kcal/P/C/G do registro. Estimativa não registrada não vira hábito. Excluir, Editar e Desfazer do recibo revertem essa mudança, e Trocar refeição a reaplica no slot novo, só para o fato que ainda está como o recibo deixou ([chat](chat.md) regra 20). Registro e ações do recibo nunca criam, apagam nem revertem temporário: ele não entra nas imagens de memória do recibo. A mensagem com pelo menos uma mudança aplicada ganha `memoryUpdated` (a da IA, ou o recibo do registro); `memory_used` vira `memoryUsedKinds` (ids `T` ignorados). Nada de linha por Gravar nem `Respondeu "…"`.
6. Sobrevive `wipeToday`. Morre no uninstall.
7. Só no dev ([ADR-019](../adrs/ADR-019-ferramentas-dev.md)): Config → `Memória da IA (dev)` mostra, edita e salva a memória e o perfil do próximo POST. O editor mostra um fato por linha (`P1 | preference | leite | Leite semidesnatado`; rotina com `slot=` e `440 kcal 25P 38C 22G`) e, só leitura, `Permanente n/30 · Dinâmica n/40 · Temporária n/5` e `visto n dias · último dd/MM`. Temporários vêm por último, uma linha só leitura cada (`T1 | temp | portion | lasanha | {texto} · criado dd/MM`); o Salvar ignora linhas `T` (alteradas ou removidas) e mantém os temporários como estão. Edita texto, `key` e categoria e aceita `novo | …` (permanente). Mesmos limites; `key` única dentro de permanente + dinâmico, temporário à parte. Linha removida é recusada com o número da linha e nada é salvo: esquecer é pelo Chat.
8. O usuário vê a memória só no Chat, sem tela própria nem toque para editar: `Memória atualizada` abaixo da resposta (ou do recibo) quando o app aplicou uma mudança naquele turno; `Memória permanente` / `Memória dinâmica` quando a resposta usou fatos desse tipo (`memoryUsedKinds`). Temporário usado não ganha selo.
9. Rotina forte do slot da hora vazio vira o card `O de sempre no {slot}?` ([chat](chat.md) regra 18). **Registrar** grava com os números da rotina (`source` `routine`) e reforça a rotina com esse registro (entra hoje nos dias); **Quase igual** só preenche o composer. O card não vai ao server.

## Regras — foto

1. Chat: `TakePicture` + `PickVisualMedia`.
2. Câmera e galeria, qualquer formato (HEIC, WebP, PNG, JPEG): decode com subsample, rotação EXIF aplicada, lado maior reduzido a 2048 px (sem ampliar), JPEG q85, EXIF não copiado ([ADR-018](../../android/adrs/ADR-018-foto-2048.md)). Preview na bolha usa subsample.
3. Cap 16 MB no JPEG, só como defesa. Copy “Foto grande demais.” quando a foto não cabe na memória para decodificar.
4. Timeout 60 s. Foto não persiste no server.

## Regras — Config

1. Edita teto (3 modos) e slots (nome + hora, 2–6 por grupo) à vontade. Modo das refeições independente do teto: Todos os dias, Seg–Sex · Sáb–Dom, Cada dia. same mantém as linhas individuais da Config; os outros mostram uma linha por grupo com quantidade e intervalo. Toque abre editor em tela cheia, com cabeçalho da Config e o mesmo passo a passo, cópia e confirmação de descarte da O3.
2. Campo “treino hoje” kcal. Null = crédito 0. Some no rollover SP. Também pode ser informado pela Home (linha "Treino de hoje", [home-timeline](home-timeline.md) regra 11): mesmo campo `day.workoutKcal`, mesmo editor (campo 28 pt + linha de crédito ao vivo), mesma validação.
3. Política eat-back 0% / % / 100%.
4. Alvos P/C/G editáveis.
5. Mudou teto: diálogo “Reiniciar registros de hoje?”. Default sim. Confirmar → `wipeToday` (meal_log + skip + digest de hoje). Cancelar → teto não é salvo. Chat UI fica. Prompt do dia recomeça: `wipeToday` grava em `chat_message` um marcador `wiped` (nunca desenhado, nunca enviado) e o prompt só leva raw depois dele.
6. Mudou nome/hora, modo ou dias: não apaga logs. IDs mantidos continuam associados; logs sem slot do dia vão para "Outros" (ADR-021 regra 7).
7. Back → Home.
8. Copy dos sheets: "Treino de hoje" sem texto de apoio abaixo do título; "Horários das refeições": "Mudar nome ou horário não apaga o que você já registrou hoje."
9. Editor de refeições em tela cheia: Continuar por grupo, Salvar na última etapa; Voltar recua ou cancela na primeira. Todo sheet termina no par `Salvar` / `Cancelar`: dois botões pill de largura total e 52 dp. Salvar vibra (confirmação), Cancelar vibra leve. A vibração segue a configuração de vibração ao toque do sistema. Controles tocáveis têm ripple.

## Regras — push

1. Exact alarm no horário de cada slot do dia corrente em America/Sao_Paulo (inexato quando a permissão não permite exato).
2. Só dispara se o slot ainda não tem log nem skip.
3. Copy: “{nome}. Ainda não registrou.”
4. Ações: Registrar → Chat. Pular → skip + cancela o alarm daquele slot.
5. Fds: só os slots cujo days inclui sábado/domingo. Não cair para “só almoço + janta” se ele cadastrou mais. Reagendamento às 00:05 SP, no boot, ao iniciar e ao editar; o fluxo em execução troca o dia em até 30 s. Alarmes de grupos removidos são cancelados.
6. Sem Firebase.

## Fronteiras e ownership

Comportamento: `produto`. Client: `android`.

## Decisões relacionadas

- [ADR-012](../adrs/ADR-012-chat-home-perfil.md)
- [ADR-018](../../android/adrs/ADR-018-foto-2048.md)
- [ADR-019](../adrs/ADR-019-ferramentas-dev.md)
- [ADR-021](../adrs/ADR-021-refeicoes-por-dia.md)
- [ADR-023](../adrs/ADR-023-chat-v2-memoria-v2.md)
- [ADR-029](../adrs/ADR-029-fatos-temporarios-compactacao.md)

## Critérios de aceite funcionais

- Memória ≤ 30 fatos permanentes + ≤ 40 dinâmicos + ≤ 5 temporários, texto ≤ 160 por fato.
- Temporário vale para estimativas dos dias seguintes até ter 3 dias; registros e ações do recibo nunca o removem nem o restauram.
- Push não dispara se o slot já foi gravado ou pulado.
- Wipe do teto não apaga dias anteriores nem o arquivo de memória.

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
- [A38][a38] — Temp facts on the device, suggested slot in the history, compaction that keeps the open tail

[a38]: ../../android/plans/pending_manual_validation/a38-fatos-temporarios-compactacao.md
