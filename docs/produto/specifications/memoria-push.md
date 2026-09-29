# Especificação — Memória, foto, push, Config

## Estado

Memória vigente desde o [A8](../../android/plans/completed/a8-memoria.md): `filesDir/memory.bin` cifrado com AES-256-GCM (chave no Android Keystore) e gravação atômica desde o [A8b](../../android/plans/completed/a8b-memoria-gravacao-atomica.md), ≤ 4000 chars, 1 linha por Gravar ou assunção respondida, enviada em `memory` em todo POST /v1/chat. Foto no Chat desde o [A6](../../android/plans/completed/a6-foto.md): câmera + galeria; desde o [A18](../../android/plans/pending_manual_validation/a18-chat-registro-foto.md) reduzida a 2048 px no lado maior, JPEG q85, sem EXIF ([ADR-018](../../android/adrs/ADR-018-foto-2048.md)). Push vigente desde o [A7](../../android/plans/completed/a7-push.md): alarme por slot vazio, exato quando permitido (senão inexato), Registrar/Pular. Config vigente desde o [A3](../../android/plans/completed/a3-config-wipe-treino.md): teto, eat-back, alvos, slots e treino do dia editáveis; wipe do teto. Desde o [A20](../../android/plans/pending_manual_validation/a20-polimento-geral.md): Salvar/Cancelar do mesmo tamanho nos sheets, toque com ripple + vibração e a copy dos sheets abaixo.

Desde o [A22](../../android/plans/pending_manual_validation/a22-treino-home.md): o treino do dia também pode ser informado pela Home, com o mesmo editor.

Mudanças planejadas (29/09/2026, aguardando aprovação): editor de memória só no dev ([ADR-019](../adrs/ADR-019-ferramentas-dev.md), [A23](../../android/plans/a23-editor-memoria-dev.md)); push por dia da semana ([A24](../../android/plans/a24-refeicoes-por-dia.md)).

## Contexto e objetivo

Perfil editável depois do onboarding. Foto no Chat. Lembrete no horário do slot. Memória curta cifrada.

## Escopo

Memória local, foto no Chat, tela Config, push exact por slot, treino do dia.

## Fora de escopo

Firebase, Health/Xiaomi, TDEE, multipart, stream.

## Regras — memória

1. Arquivo interno criptografado (AES-256-GCM, chave no Android Keystore). Gravação atômica: crash no meio de uma gravação mantém a memória anterior. Não vai ao server além do campo `memory` do POST.
2. Conteúdo: gostos, marcas, assunções confirmadas, o que costuma pular. Sem transcrição.
3. Teto ~1,3 k tok junto com o perfil. O que não ocorre cede lugar ao que ocorre.
4. Atualiza quando o user confirma Gravar (`{slot}: {descrição} ({kcal} kcal)`), Substituir (`{slot} (atualizado): {descrição} ({kcal} kcal)`) ou quando responde assunção (`Respondeu "{pergunta}": {resposta}`). Não a cada prosa. Acima de 4000 chars, as linhas mais velhas saem.
5. Sobrevive `wipeToday`. Morre no uninstall.

## Regras — foto

1. Chat: `TakePicture` + `PickVisualMedia`.
2. Câmera e galeria, qualquer formato (HEIC, WebP, PNG, JPEG): decode com subsample, rotação EXIF aplicada, lado maior reduzido a 2048 px (sem ampliar), JPEG q85, EXIF não copiado ([ADR-018](../../android/adrs/ADR-018-foto-2048.md)). Preview na bolha usa subsample.
3. Cap 16 MB no JPEG, só como defesa. Copy “Foto grande demais.” quando a foto não cabe na memória para decodificar.
4. Timeout 60 s. Foto não persiste no server.

## Regras — Config

1. Edita teto (3 modos) e slots (nome + hora, 2–6) à vontade.
2. Campo “treino hoje” kcal. Null = crédito 0. Some no rollover SP. Também pode ser informado pela Home (linha "Treino de hoje", [home-timeline](home-timeline.md) regra 11): mesmo campo `day.workoutKcal`, mesmo editor (campo 28 pt + linha de crédito ao vivo), mesma validação.
3. Política eat-back 0% / % / 100%.
4. Alvos P/C/G editáveis.
5. Mudou teto: diálogo “Reiniciar registros de hoje?”. Default sim. Confirmar → `wipeToday` (meal_log + skip + digest de hoje). Cancelar → teto não é salvo. Chat UI fica. Prompt do dia recomeça: `wipeToday` grava em `chat_message` um marcador `wiped` (nunca desenhado, nunca enviado) e o prompt só leva raw depois dele.
6. Mudou só nome/hora: relabela. Não apaga logs.
7. Back → Home.
8. Copy dos sheets ([A20](../../android/plans/pending_manual_validation/a20-polimento-geral.md)): "Treino de hoje" sem texto de apoio abaixo do título; "Horários das refeições": "Mudar nome ou horário não apaga o que você já registrou hoje."
9. Todo sheet termina no par `Salvar` / `Cancelar`: dois botões pill de largura total e 52 dp. Salvar vibra (confirmação), Cancelar vibra leve. A vibração segue a configuração de vibração ao toque do sistema.

## Regras — push

1. Exact alarm no horário de cada slot do perfil.
2. Só dispara se o slot ainda não tem log nem skip.
3. Copy: “{nome}. Ainda não registrou.”
4. Ações: Registrar → Chat. Pular → skip + cancela o alarm daquele slot.
5. Fds: os slots que o user cadastrou. Não cair para “só almoço + janta” se ele cadastrou mais.
6. Sem Firebase.

## Fronteiras e ownership

Comportamento: `produto`. Client: `android`.

## Decisões relacionadas

- [ADR-012](../adrs/ADR-012-chat-home-perfil.md)

## Planos relacionados

- [A3 (Concluído)](../../android/plans/completed/a3-config-wipe-treino.md)
- [A22 (Pendente aprovação manual)](../../android/plans/pending_manual_validation/a22-treino-home.md)
- [A6 (Concluído)](../../android/plans/completed/a6-foto.md)
- [A18 (Pendente aprovação manual)](../../android/plans/pending_manual_validation/a18-chat-registro-foto.md)
- [A7 (Concluído)](../../android/plans/completed/a7-push.md)
- [A8 (Concluído)](../../android/plans/completed/a8-memoria.md)

## Critérios de aceite funcionais

- Memória ≤ 1,3 k tok no prefixo (aprox. chars/4).
- Push não dispara se o slot já foi gravado ou pulado.
- Wipe do teto não apaga dias anteriores nem o arquivo de memória.
