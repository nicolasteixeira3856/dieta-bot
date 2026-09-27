# Plano — A6 foto camera + picker

- Estado: Aguardando aprovacao
- Data: 25/09/2026
- Contexto proprietario: `android`
- Codigo afetado: `apps/android/`
- Pre-requisitos: A5 + [memoria-push.md](../../produto/specifications/memoria-push.md)

## Gate de autorizacao

> Aprovo o plano `docs/android/plans/a6-foto.md`. Implemente o plano aprovado.

## Objetivo

Clip do Chat abre camera ou picker. Upload JPEG <=16MB. Preview subsample.

## Fontes de verdade

- Visual Gold (Stitch): `docs/qa/stitch/dark/chatF.png` e `docs/qa/stitch/light/chatF.png`
- spec [memoria-push.md](../../produto/specifications/memoria-push.md)

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

## Encerramento

Ciclo SDD.
