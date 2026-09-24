---
name: nutri-visual
description: Valida o visual do Nutri no emulador Android. Use quando uma tela mudou ou antes de dar a UI por pronta. Não use o celular físico.
---

A prova visual é o emulador. O celular do dono fica fora disso.

1. `flutter devices` e escolha o emulador Android, nunca o serial `RQGL2047C9L`.
2. Se ele estiver desligado, `flutter emulators --launch` no AVD que já existe. Não crie outro.
3. `flutter run -d <emulador>` com o app. Percorra a tela que mudou.
4. `adb -s emulator-5554 exec-out screencap -p` (troque o serial se o emulador usar outro).
5. Abra esse PNG e diga o que está visível: título, botão, corte, overflow, texto em cima de texto.
6. Compare com `wires/nutri-wires.html` só nessa tela.

`find.text` passando não fecha a entrega se a imagem mostrar a tela quebrada.
A Home do dia 1 não tem chip. O chat é sheet, não uma rota nova.
