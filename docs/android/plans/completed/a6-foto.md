# Plano — A6 foto camera + picker (Concluído)

- Estado: Concluído
- Data: 25/09/2026
- Data de conclusão: 27/09/2026
- Contexto proprietario: `android`
- Codigo afetado: `apps/android/`
- Pre-requisitos: A5 + [memoria-push.md](../../../produto/specifications/memoria-push.md)

## Gate de autorizacao

> Aprovado pelo usuário via comando:
> `/goal Aprovo o plano docs/android/plans/a6-foto.md. Analise e implemente o plano aprovado.`

## Objetivo

Clip do Chat abre camera ou picker. Upload JPEG <=16MB. Preview subsample.

## Fontes de verdade

- Visual Gold (Stitch): `docs/qa/stitch/dark/chatF.png` e `docs/qa/stitch/light/chatF.png`
- spec [memoria-push.md](../../../produto/specifications/memoria-push.md)

## Escopo de implementacao

- TakePicture FileProvider + PickVisualMedia.
- HEIC/WebP → JPEG q90. Sem downscale.
- JPEG > 16MB: snackbar foto grande demais. Nao POST.
- Preview inSampleSize lado <=720. Upload = bytes do arquivo.
- image_b64 no ChatIn. photoPath no chat_message.
- CAMERA no Manifest. Runtime dialog.
- PhotoScale 1280 do T1 nao e usado pelo Chat.

## Validacao planejada

- Unit rejeita File.length()>16MB.
- Validacao visual Compose vs Stitch Gold: capturas emulador `docs/qa/android/current/{dark,light}/chatF.png` comparadas pixel a pixel contra `docs/qa/stitch/{dark,light}/chatF.png` com diff list aprovada.

## Fora de escopo

Downscale. Multipart. Compactacao server.

## Criterios de aceite

- Foto <=16MB sobe. >16MB nao chama API.
- HEIC da galeria vira JPEG antes do POST.

## Resultado da implementação

### Entregue

- **`domain/PhotoGate`** (puro): `MAX_BYTES` 16 MB, `fits`, `sampleSize` (potência de 2, lado ≤ 720), `isJpeg` (FF D8 FF), JPEG q90.
- **`core/photo/PhotoStore`** (`PhotoFiles`): fotos em `filesDir/photos`. Galeria: JPEG copiado byte a byte; qualquer outro formato (HEIC, WebP, PNG) decodificado em tamanho cheio (`ImageDecoder` na API 28+, que aplica a rotação; `BitmapFactory` antes) e regravado em JPEG q90. Câmera: `TakePicture` direto em `filesDir/photos` via `FileProvider` (`${applicationId}.photos`). Acima de 16 MB → `TooLarge`, arquivo apagado. Upload = bytes do arquivo em base64. `preview()` = `inSampleSize` (lado ≤ 720) + rotação EXIF.
- **Manifest**: `CAMERA` + `uses-feature camera required=false` + `FileProvider`. Permissão pedida em runtime no primeiro uso da câmera; negada → aviso "Sem permissão da câmera.".
- **`ChatViewModel`**: a câmera do composer abre o sheet "Enviar foto do prato" (Tirar foto / Escolher da galeria); o chip "Tirar foto do prato" (dia vazio) abre a câmera. A foto sai na hora com o texto digitado como legenda (`image_b64` só no turno, nunca no compact). Sucesso → `chat_message.photoPath`. Falha → bolha pendente com a foto; o retry reenvia. Acima de 16 MB → aviso "Foto grande demais.", sem POST. Foto que nunca teve resposta é apagada no `onCleared`.
- **`PromptBuilder`**: mensagem com foto entra no histórico como `[foto] {legenda}`; a imagem vai uma vez só.
- **UI (chatF)**: bolha do user com a foto (242×135 dp, raio 14, `ContentScale.Crop`), selo "Visão Computacional", legenda, hora e ✓✓. Prévia decodificada fora da main thread, com cache (`PhotoPreviews`).
- **`PhotoScale`/`PhotoCompressor` (1280, q70)** ficam só no fluxo T1 antigo; o Chat não usa.
- **QA**: `tools/capture-photo.sh` (fluxo real: galeria, câmera, 16 MB, WebP + captura chatF); `fake-chat-server.mjs` informa bytes/JPEG da última foto; gate por região em `StitchGoldTest` e `tools/diff-gold.mjs`.

### Decisões tomadas na implementação

- **Conflito com `AGENTS.md`.** AGENTS diz "Client: ≤1280 JPEG 70"; este plano e a spec memoria-push (foto, regra 2) dizem "Sem downscale", JPEG q90, cap 16 MB, e o S1 já pôs o cap de 16 MB no server. Segui o plano, aprovado agora pelo dono (precedência 1 do SDD). **Pendente: o dono atualizar a linha "Photo on Chat" do `AGENTS.md`.**
- **A foto sai ao ser escolhida**, legendada pelo texto do composer (o gold não mostra estado de anexo pendente).
- **chatF inteiro vai para `GOLD_CONFLICTS`**: é a geração chatF/chatG (cabeçalho "IA Ativa", "Nutri AI" dentro da bolha, composer com microfone), já registrada no A5. Só a bolha de foto (caixa `214,368–746,734` px do gold) tem gate, com o melhor deslocamento vertical.
- **HEIC**: não há gerador de HEIC no ambiente. O ramo é o mesmo de WebP/PNG (tudo que não começa com FF D8 FF), validado com WebP no emulador e com PNG/WebP na JVM.
- **Selo "Visão Computacional"** mantido como no gold (a foto vai mesmo ao modelo de visão).

## Validação executada

1. `./gradlew.bat :app:testDebugUnitTest`: 167/167.
   - `PhotoGateTest`: 16 MB aceita; 16 MB + 1 e 0 recusam; `sampleSize` deixa o lado ≤ 720; magia JPEG.
   - `PhotoStoreTest` (Robolectric): JPEG da galeria copiado byte a byte, sem `.import` sobrando; PNG e WebP → JPEG 1600×1200 (sem downscale); JPEG de 16 MB + 1 → `TooLarge`, apagado, `base64` nulo; captura vazia → `Failed`; prévia de 4000×3000 → 500 px.
   - `ChatViewModelTest`: foto vai com legenda e `image_b64`, `photoPath` gravado, próximo turno sem imagem e com `[foto] Almoço de hoje`; acima de 16 MB → aviso, sem request, Room vazio; falha mantém a foto para o retry (mesma imagem nas duas tentativas); câmera/galeria canceladas não fazem nada e a captura vazia é apagada.
2. Emulador (`Medium_Phone`, API 36) contra o fake, `tools/capture-photo.sh light` e `dark`: JPEG da galeria chegou byte a byte (353.224 bytes); permissão de câmera pedida em runtime, `com.android.camera2` fotografou e o JPEG chegou; 17 MB → "Foto grande demais.", API não chamada, nada guardado; WebP → JPEG 2000×1116 no aparelho e no POST.
3. Gate visual chatF (bolha de foto): emulador 1,87% dark / 1,68% light; JVM 1,77% / 1,61%. Tela inteira reportada como conflito (10,4% / 11,0% no emulador). chatE/T/P na JVM inalterados; splash e O1–O4 no gate (0,69–1,80%).

## Lista de diffs (gold × app)

- **Bolha de foto**: igual em posição, largura e imagem; resta o raster do texto do selo e da legenda.
- **Resto do chatF (geração conflitante; segue o chatE)**: cabeçalho à esquerda com "IA Ativa" × centralizado; pílula de data em caixa alta; "Nutri AI" dentro da bolha × rótulo acima; "ENERGIA ESTIMADA" × "ENERGIA TOTAL"; composer com microfone (omitido desde o A5).

## Encerramento

Ciclo SDD.
