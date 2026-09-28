# Plano — A8b Memória com gravação atômica (Concluído)

- Estado: Concluído
- Data: 27/09/2026
- Data de conclusão: 27/09/2026
- Contexto proprietário: `android`
- Código afetado: `apps/android/`, `tools/capture-chat.sh`
- Pré-requisitos: [A8 (Concluído)](a8-memoria.md) + [memoria-push.md](../../../produto/specifications/memoria-push.md)

## Gate de autorização

> Autorizado pelo dono no mesmo pedido que criou o plano:
> `/goal Escreva o A8b e já realize a implementação do plano`
> (escolha da opção B — cifra direta no Keystore + `AtomicFile` — após a análise do risco de gravação do A8).

## Objetivo

Tirar o risco de perder a memória se o app morrer no meio de uma gravação. O A8 grava com `EncryptedFile`, que não abre arquivo existente para escrita e usa o nome do arquivo como dado associado da cifra: a única forma possível era apagar e reescrever `memory.txt`. Um crash nessa janela perde a memória inteira.

## Fontes de verdade

- [memoria-push.md](../../../produto/specifications/memoria-push.md) regras de memória 1 e 5 (arquivo interno criptografado; sobrevive `wipeToday`, morre no uninstall).
- [A8](a8-memoria.md) (formato de linhas, 4000 chars, gatilhos de escrita — inalterados).

## Decisão

Substitui a decisão "EncryptedFile" do A8:

- **Cifra:** AES-256-GCM (`javax.crypto`) com chave não exportável no Android Keystore (alias próprio, `PURPOSE_ENCRYPT | PURPOSE_DECRYPT`, `BLOCK_MODE_GCM`, `ENCRYPTION_PADDING_NONE`, IV aleatório de 12 bytes gerado pelo Keystore, tag de 128 bits). Formato do arquivo: `NM` + versão (1 byte) + IV (12) + ciphertext+tag. A cifra não depende do nome do arquivo.
- **Gravação atômica:** `androidx.core.util.AtomicFile` sobre `filesDir/memory.bin`. `startWrite` → bytes cifrados → `finishWrite` (fsync + rename por cima, atômico no Linux). Exceção → `failWrite`: o arquivo anterior fica intacto. Arquivo `.new` órfão de crash é ignorado na leitura.
- **Por que não EncryptedFile + rename:** o nome é dado associado da cifra; `memory.tmp` renomeado para `memory.txt` não decifra.
- **Por que não arquivos A/B:** resolve, mas mantém uma API marcada `@Deprecated` (`EncryptedFile`/`MasterKey` na `security-crypto:1.1.0`, conferido no bytecode) e dobra a lógica de leitura.

## Escopo de implementação

### 1. Cifra e arquivo

- `MemoryCipher` (interface) + `KeystoreMemoryCipher` (produção).
- `AtomicMemoryFile` implementa `MemoryFile` (interface do A8, inalterada) sobre `AtomicFile` + `MemoryCipher`.
- Leitura que não decifra (chave perdida, arquivo adulterado): apaga e devolve vazio — mesmo comportamento do A8.
- `MemoryStore` inalterado.

### 2. Migração do A8

- Na primeira leitura, se `memory.bin` não existe e `memory.txt` (formato A8) existe: lê o texto uma vez pelo `EncryptedFile`, grava em `memory.bin` pela via atômica e só então apaga `memory.txt`. Se a leitura antiga falhar, apaga `memory.txt` (memória antiga perdida, como no A8).
- `EncryptedFile`/`MasterKey` ficam confinados a `LegacyMemoryReader`, usados só nessa migração. Remover a dependência `security-crypto` num corte futuro, quando nenhum aparelho tiver `memory.txt`.

### 3. QA

- `tools/capture-chat.sh`: checagens passam de `memory.txt` para `memory.bin` (existe, cru sem a linha, `memory.txt` ausente).

## Validação planejada

1. JVM (Robolectric, cifra fake AES-GCM com chave de software — o Robolectric não tem AndroidKeyStore):
   - ida e volta; arquivo cru sem o texto;
   - gravação interrompida (`startWrite` + bytes parciais, sem `finishWrite`) → leitura devolve o conteúdo anterior;
   - arquivo adulterado → leitura vazia e arquivo apagado;
   - migração: `memory.txt` legado vira `memory.bin`, legado apagado depois da gravação; falha na gravação nova mantém o legado; falha na leitura legada apaga o legado.
2. Emulador: `tools/capture-chat.sh light` com as checagens novas (Keystore real).
3. Emulador, migração real: APK do A8 grava `memory.txt` pelo fluxo do Chat → `adb install -r` do A8b (dados mantidos) → próximo POST leva a mesma memória, `memory.bin` existe, `memory.txt` sumiu.

## Fora de escopo

- Formato das linhas, limite de 4000 chars, gatilhos (A8).
- Remover a dependência `security-crypto` (fica para depois da migração).
- Sync com server, cifrar `chat_message`.

## Riscos e controles

- **Keystore indisponível ou chave invalidada:** leitura/escrita falham → memória vazia, chat segue (igual ao A8).
- **Crash entre gravar `memory.bin` e apagar `memory.txt`:** na próxima leitura `memory.bin` existe e é a fonte; `memory.txt` restante é apagado.

## Critérios de aceite

- Crash no meio de uma gravação não perde a memória anterior.
- Memória do A8 migrada sem perda.
- Arquivo em disco nunca em texto claro.

## Resultado da implementação

### Entregue

- `core/memory/MemoryCipher.kt`: `MemoryCipher`, `AesGcmMemoryCipher` (envelope `NM` + v1 + IV 12 + ciphertext/tag 128) e `KeystoreMemoryCipher` (chave AES-256 `nutri_memory_v2` no AndroidKeyStore, GCM, sem padding, IV gerado pelo provider).
- `core/memory/AtomicMemoryFile.kt`: `memory.bin`; escrita em `memory.bin.new` → `fsync` → `Files.move(ATOMIC_MOVE, REPLACE_EXISTING)`; falha apaga o `.new` e mantém o anterior; `.new` órfão nunca é lido; leitura que não decifra apaga e devolve vazio. Migração do A8 na primeira leitura. `MemoryModule` liga `MemoryFile`, `MemoryCipher`, `LegacyMemory`.
- `core/memory/LegacyMemory.kt`: `EncryptedFileLegacyMemory`, único uso de `EncryptedFile`/`MasterKey` (com `@Suppress("DEPRECATION")`), só para ler `memory.txt` uma vez. `EncryptedMemoryFile` (A8) removido.
- `MemoryStore`, formato das linhas, limite e gatilhos: inalterados.
- `tools/capture-chat.sh`: checa `memory.bin` (cabeçalho `NM`, sem texto claro), ausência de `memory.txt` e deixa um `memory.bin.new` corrompido antes do resto do fluxo.

### Decisões tomadas na implementação

- **Move atômico próprio em vez de `androidx.core.util.AtomicFile`.** Mesmo roteiro (arquivo novo, fsync, rename por cima), mas o `AtomicFile.finishWrite` usa `File.renameTo`, que no JVM do Windows (onde rodam os testes unitários) não substitui arquivo existente: os testes não exercitariam o caminho real. `Files.move(ATOMIC_MOVE)` é `rename(2)` no Android (minSdk 26) e `MoveFileEx` com substituição no Windows.
- **Obsolescência confirmada:** `EncryptedFile` e `MasterKey` têm o atributo `Deprecated` no bytecode da `security-crypto:1.1.0` (javap) e o Kotlin avisa no uso.

## Validação executada

1. `./gradlew.bat :app:testDebugUnitTest`: 153/153. Novo `AtomicMemoryFileTest` (9): ida e volta, disco sem texto claro e IV aleatório; ausente → null; crash com `.new` parcial → memória anterior; cifra falhando na escrita → anterior intacto; byte adulterado / outra chave → vazio e arquivo apagado; migração move e apaga o legado; escrita nova falhando mantém o legado; legado ilegível é descartado; legado que sobrou depois de migrar perde para `memory.bin`; `MemoryStore` de ponta a ponta (60 appends ≤ 4000).
2. Migração real no emulador (`Medium_Phone`, API 36): APK do A8 gravou `memory.txt` (108 bytes) pelo fluxo do Chat → `installDebug` do A8b por cima (dados mantidos) → primeira mensagem: `memory.txt` sumiu, `memory.bin` com 83 bytes e cabeçalho `4e 4d 01`, sem texto claro, POST com `memory` = "Café da manhã: Café com 2 ovos mexidos (380 kcal)".
3. `tools/capture-chat.sh light` (app limpo, A8b): todas as checagens do A5/A5b + `memory.bin` com cabeçalho `NM`, sem texto claro, sem `memory.txt`; com um `memory.bin.new` corrompido deixado no aparelho e vários force-stops, o POST seguinte ainda levou a linha do Gravar.
4. Gate visual inalterado (UI não mudou): light chatE 1,33%, chatT 1,81%, chatP 0,63%, splash e O1–O4 0,70–1,33%.

## Pendências

- Remover `LegacyMemory` e a dependência `security-crypto` num corte futuro (quando nenhum aparelho tiver `memory.txt`).

## Encerramento

Ciclo SDD.
