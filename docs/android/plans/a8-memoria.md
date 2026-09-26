# Plano — A8 memoria criptografada

- Estado: Aguardando aprovacao
- Data: 25/09/2026
- Contexto proprietario: `android`
- Codigo afetado: `apps/android/`
- Pre-requisitos: A5 + [memoria-push.md](../../produto/specifications/memoria-push.md)

## Gate de autorizacao

> Aprovo o plano `docs/android/plans/a8-memoria.md`. Implemente o plano aprovado.

## Objetivo

Arquivo de memoria <= ~1.3k tok junto do perfil. Prefixo do chat. EncryptedFile.

## Escopo de implementacao

- EncryptedFile filesDir/memory.txt AES256_GCM MasterKey.
- MemoryStore.read/write.
- Atualiza so apos addLog ou resposta de assuncao. Append 1 linha. Se chars > 4000, dropa linhas velhas.
- Perfil nao mora neste arquivo.
- PromptBuilder.memory = MemoryStore.read().
- wipeToday NAO apaga. Uninstall apaga.

## Validacao planejada

- Unit 50 appends → <=4000 chars.
- wipeToday nao chama clear.
- Arquivo nao e plaintext.

## Fora de escopo

Hermes USER.md. Sync server. Criptografar chat_message.

## Criterios de aceite

- POST /v1/chat IN.memory nao-vazio depois de 1 Gravar.
- memory.txt cru nao mostra a linha.

## Encerramento

Ciclo SDD.
