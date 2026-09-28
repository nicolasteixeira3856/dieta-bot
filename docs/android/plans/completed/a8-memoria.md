# Plano — A8 memoria criptografada (Concluído)

- Estado: Concluído
- Data: 25/09/2026
- Data de conclusão: 27/09/2026
- Contexto proprietario: `android`
- Codigo afetado: `apps/android/`
- Pre-requisitos: A5 + [memoria-push.md](../../../produto/specifications/memoria-push.md)

## Gate de autorizacao

> Aprovado pelo usuário via comando:
> `/goal Aprovo o plano docs/android/plans/a8-memoria.md. Analise e implemente o plano aprovado.`

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

## Resultado da implementação

### Entregue

- **`core/memory/EncryptedMemoryFile`** (substituído no [A8b](a8b-memoria-gravacao-atomica.md): cifra direta no Keystore + gravação atômica): `filesDir/memory.txt` via Jetpack Security `EncryptedFile` (`AES256_GCM_HKDF_4KB`), chave `MasterKey` `AES256_GCM` no Android Keystore. Dependência nova: `androidx.security:security-crypto:1.1.0`. Arquivo ilegível (chave perdida, dano) → apagado, leitura vazia, o chat segue.
- **`core/memory/MemoryStore`** (`@Singleton`, mutex): `read()` / `append(line)`. Uma linha por append (espaços e quebras colapsados, ≤ 240 chars). Acima de 4000 chars, as linhas mais velhas saem. Não existe `clear`.
- **`ChatViewModel`**: `PromptBuilder.build(..., memory = MemoryStore.read())` em todo POST (turno e compact reconstruído). Appends:
  - Gravar/Trocar → `{slot}: {descrição} ({kcal} kcal)`, depois do `addLog` + recibo;
  - resposta a assunção (a última estimativa do dia tinha `question` e nada a fechou) → `Respondeu "{pergunta}": {resposta}`, só depois de o server responder.
- **`PromptBuilder.build`** ganhou o parâmetro `memory` (antes fixo em `""`). Perfil continua no bloco próprio.
- **`wipeToday`** não conhece a memória. Uninstall/`pm clear` apagam `filesDir`; `allowBackup="false"` impede restaurar o arquivo sem a chave.
- **QA**: `fake-chat-server.mjs` devolve a `memory` do último request em `/__calls`; `capture-chat.sh` checa arquivo, cifra e `memory` no POST.

### Decisões tomadas na implementação

- **Cifra atrás da interface `MemoryFile`.** O Robolectric não tem AndroidKeyStore, então a regra (append, corte, sem clear) é testada na JVM com `FakeMemoryFile`, e a cifra real é verificada no emulador.
- **Escrita = apagar + gravar.** O `EncryptedFile` não abre arquivo existente para escrita e usa o nome do arquivo como dado associado (sem gravar em temporário e renomear). Um crash no meio da escrita perde a memória, não o app.
- **Só o Gravar do Chat escreve.** Os outros `addLog` (HomeViewModel, T2, DayViewModel) são do fluxo antigo T1/T2, fora do caminho vivo (ADR-012).
- **"Assunção respondida"** = mensagem do user enquanto a última estimativa do dia tem `question` aberta (sem recibo nem wipe depois).

## Validação executada

1. `./gradlew.bat :app:testDebugUnitTest`: 144/144.
   - `MemoryStoreTest`: 50 appends ≤ 4000 chars, mais velhas saem, ordem mantida; 1 linha por append, colapsada e cortada; vazio ignorado.
   - `ChatViewModelTest`: envio sem Gravar não escreve; Gravar escreve 1 linha e o próximo POST leva `memory` com ela; resposta a pergunta de confiança média escreve `Respondeu "...": francês`; `wipeToday` mantém a memória e ela vai no POST seguinte.
   - `ChatViewModelTest` e `ChatCompactTest` passam a injetar `MemoryStore(FakeMemoryFile())`.
2. Emulador (`Medium_Phone`, API 36, 780x1688 @ 320) contra o fake, `tools/capture-chat.sh light`: todas as checagens do A5/A5b + `memory.txt` escrito (108 bytes), cru sem "380 kcal"/"pães"/"Caf" (cifrado); POST seguinte com `memory` = "Café da manhã: Café com 2 ovos mexidos (380 kcal)", depois de force-stops e de apagar `chat_message` (a chave do Keystore decifra entre processos; a memória independe do chat).
3. Gate visual (a UI não mudou): light chatE 1,33%, chatT 1,90%, chatP 0,63%, splash e O1–O4 0,70–1,33%. Dark não recapturado (sem mudança de UI).
4. O emulador caiu na primeira rodada; as capturas vazias dela foram descartadas (`git checkout`) antes da rodada válida.

## Encerramento

Ciclo SDD.
