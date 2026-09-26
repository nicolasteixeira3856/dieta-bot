# Especificação — Memória, foto, push, Config

## Estado

Sem memória. Foto T1 ≤1280 JPEG 70. Sem push. Sem Config. `DayEntity.workoutKcal` existe sem UI.

## Contexto e objetivo

Perfil editável depois do onboarding. Foto no Chat. Lembrete no horário do slot. Memória curta cifrada.

## Escopo

Memória local, foto no Chat, tela Config, push exact por slot, treino do dia.

## Fora de escopo

Firebase, Health/Xiaomi, TDEE, multipart, stream.

## Regras — memória

1. Arquivo interno criptografado (`EncryptedFile` / equivalente). Não vai ao server além do campo `memory` do POST.
2. Conteúdo: gostos, marcas, assunções confirmadas, o que costuma pular. Sem transcrição.
3. Teto ~1,3 k tok junto com o perfil. O que não ocorre cede lugar ao que ocorre.
4. Atualiza quando o user confirma Gravar ou quando responde assunção. Não a cada prosa.
5. Sobrevive `wipeToday`. Morre no uninstall.

## Regras — foto

1. Chat: `TakePicture` + `PickVisualMedia`.
2. HEIC → JPEG qualidade 90. Sem downscale do lado. Preview na bolha usa subsample.
3. Cap 16 MB no JPEG. Acima: copy “foto grande demais”.
4. Timeout 60 s. Foto não persiste no server.

## Regras — Config

1. Edita teto (3 modos) e slots (nome + hora, 2–6) à vontade.
2. Campo “treino hoje” kcal. Null = crédito 0. Some no rollover SP.
3. Política eat-back 0% / % / 100%.
4. Alvos P/C/G editáveis.
5. Mudou teto: diálogo “apaga os logs de hoje?”. Default sim. Confirmar → `wipeToday` (meal_log + skip + digest de hoje). Chat UI fica. Prompt do dia recomeça.
6. Mudou só nome/hora: relabela. Não apaga logs.
7. Back → Home.

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

- `docs/android/plans/a3-config-wipe-treino.md`
- `docs/android/plans/a6-foto.md`
- `docs/android/plans/a7-push.md`
- `docs/android/plans/a8-memoria.md`

## Critérios de aceite funcionais

- Memória ≤ 1,3 k tok no prefixo (aprox. chars/4).
- Push não dispara se o slot já foi gravado ou pulado.
- Wipe do teto não apaga dias anteriores nem o arquivo de memória.
