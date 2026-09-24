---
name: nutri-ui
description: Faz tela Flutter do Nutri com a cara do wire e movimento curto. Use ao criar ou refazer layout, visual, animação ou copy visível. Não use para regra de negócio sem tela.
---

A medida visual é `wires/nutri-wires.html`, o bloco `:root` e o telefone da tela correspondente. Não invente outra paleta.

Cores desse bloco, não o verde padrão do Material: fundo `#0b0d10`, superfície `#171b20`, linha `#2a3139`, texto `#f3f5f7`, mudo `#8b939c`, ouro `#e8b86d`, ok `#7dda9a`, erro `#e07a6a`. Cantos de card 18, campo 16, botão cheio em pílula. Número grande com tracking apertado. Texto secundário menor e mudo.

Movimento só com o que o Flutter já traz. 180–240 ms, curva `easeOutCubic`. Nunca `ease-in`. Troca de tela e de chip com `AnimatedSwitcher` ou `AnimatedContainer`. Sheet sobe; não vira rota. Saldo que muda conta no lugar, sem pulo, com `FontFeature.tabularFigures()`. Botão ao toque vai a escala 0,97 e não entra a partir de escala 0. Sem Lottie, sem Rive, sem pacote novo de animação.

Overflow e tela estreita: `flutter-fix-layout-issues` e `flutter-build-responsive-layout`. A Home continua o telefone do wire. Não troque por sidebar aos 600 px. Revisão de detalhe: `web-design-guidelines`, sem mudar a paleta.

Uma tela por vez, a do wire. Home do dia 1 sem chip. Antes de dar por pronta, siga `nutri-visual` no emulador e compare com o mesmo frame do wire.
