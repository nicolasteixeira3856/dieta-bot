# Plano — A14 Marca: ícone do app e splash com o logo

- Estado: Concluído (28/09/2026). Ícone e splash confirmados pelo dono; gold e gate fechados pelo [A15](a15-ajuste-visual-golds-novos.md).
- Data: 28/09/2026
- Contexto proprietário: `android`
- Código afetado: `apps/android/app/src/main/res/` (ícone adaptativo, splash do sistema), `feature/splash/`, `core/designsystem/` (wordmark), golds `docs/qa/stitch/{dark,light}/splash.png`, `design/brand/` (fonte), docs.
- Pré-requisitos: [A13](a13-rename-dieta-bot.md) concluído (wordmark "Dieta Bot" já na UI e no gold).

## Gate de autorização

Este plano é exclusivamente documental. A implementação só começa após aprovação explícita que identifique este arquivo:

> Aprovo o plano `docs/android/plans/a14-marca-icone-splash.md`. Implemente o plano aprovado.

## Objetivo

O app ganha o logo do dono: ícone do launcher (adaptativo + temático do Android 13+), splash do sistema coerente e splash em Compose com o logo acima do wordmark "Dieta Bot".

## Fonte (entregue pelo dono em 28/09/2026, gerada no ChatGPT)

| Arquivo | Conteúdo | Checagem do agente |
|---|---|---|
| `design/brand/icon.png` | anel de progresso dourado + prato off-white + robô dourado, fundo transparente | 1254², RGBA, símbolo centralizado com 50% do quadro, sem halo |
| `design/brand/icon-mono.png` | mesma forma em branco, robô recortado | serve de máscara para o ícone temático |
| `design/brand/icon-dark-bg.png` | versão sobre `#0B0D10` | só referência visual; o fundo do ícone vem de cor |

Ressalvas registradas (decisão do dono de seguir assim): o robô ocupa cerca de 9% do quadro e está fora do centro, então some no tamanho do launcher. Não houve SVG. Os círculos da versão escura têm leve irregularidade.

## Escopo de implementação

### 1. Ícone do launcher (AGENTE)

- Foreground: `icon.png` redimensionado com alta qualidade para as densidades (`mipmap-mdpi` … `xxxhdpi`, 108 dp → 108/162/216/324/432 px), ocupando a área segura de 66 dp (o símbolo cresce de 50% para cerca de 61% do quadro).
- Background: cor `#0B0D10` (token `bg` dark), igual nos dois temas.
- Monochrome: `icon-mono.png` nas mesmas densidades (`<monochrome>`, Android 13+).
- `mipmap-anydpi-v26/ic_launcher.xml` + `ic_launcher_round.xml`; manifest `android:icon`/`roundIcon` → `@mipmap/ic_launcher`. O `drawable/ic_launcher.xml` e o `ic_mark` antigos saem se nada mais os usar.
- Legacy (<26): PNG composto (fundo + símbolo) por densidade.
- A notificação (`ic_notification`) não muda: ela precisa ser silhueta de 1 cor, e o robô pequeno não funciona em 24 dp.

### 2. Splash do sistema, Android 12+ (AGENTE)

- `windowSplashScreenAnimatedIcon` passa de `ic_splash_blank` para o foreground do logo, com o fundo `splash_bg` de cada tema.
- A splash em Compose continua sendo a tela da marca (AGENTS: não remover). A do sistema passa a mostrar o mesmo logo por alguns ms, em vez de um quadro vazio.

### 3. Splash em Compose + gold (AGENTE via MCP `stitch`)

1. Stitch, telas `splash` dark e light, com `edit_screens`: "adicione acima do wordmark 'Dieta Bot' um logo circular (anel dourado #E8B86D de 3/4 + disco off-white), 120 dp de diâmetro, centralizado; não altere mais nada". O Stitch não recebe a imagem, então o gold terá uma aproximação do logo.
2. Exportar os golds (`export-stitch.mjs`) e conferir que só a área do logo mudou.
3. `SplashScreen.kt`: `Image` do logo (densidades do item 1) no tamanho e posição do gold, com o wordmark vindo do A13.
4. Gate: `diff-gold` splash dark/light com uma **máscara na área do logo**, porque o gold tem uma aproximação e o app tem o arquivo real. Fora da máscara, o gate normal (máx. 2%). A lista de diffs é escrita no plano.

### 4. Docs (AGENTE)

- `docs/android/README.md` (seção Marca: fontes em `design/brand/`, como regenerar as densidades) e `docs/qa/README.md` (máscara do logo na splash).
- Script `tools/brand-icons.ps1`: gera as densidades a partir de `design/brand/*.png`, para refazer quando o logo mudar.

## Validação planejada

1. `aapt dump badging` do `assembleDevRelease`: `application-icon-*` aponta para `mipmap/ic_launcher`.
2. Emulador: launcher com o ícone no formato círculo e squircle; ícone temático ligado (Android 13+) mostra a silhueta; capturas em `docs/qa/android/current/{dark,light}/launcher.png`, só como evidência (não é tela ADR-012).
3. Cold start: splash do sistema com o logo → splash Compose → Home, ≤2 s (AGENTS).
4. `capture-onboarding.sh` dark/light + `diff-gold` splash (com a máscara), o1–o4 ✓.
5. `testDevDebugUnitTest` + `verifyRoborazziDevDebug`: baseline da splash regravada, registrando o motivo.
6. Manual (dono): ícone e splash no celular. ⏳

## Fora de escopo

- Redesenhar o logo (o dono pode trocar os arquivos em `design/brand/` e rodar `tools/brand-icons.ps1`).
- Ícone de notificação, loja (Play), animação da splash.

## Riscos e controles

- **Logo ilegível pequeno:** registrado como ressalva; o dono decide se regenera.
- **Gold aproximado:** máscara só na área do logo; o resto da splash segue o gate.
- **Splash virar freeze:** o cold start é medido; a splash do sistema só dura até o primeiro frame.

## Critérios de aceite

- Ícone adaptativo e temático no launcher, splash do sistema e Compose com o logo, gate visual verde fora da máscara, testes verdes.

## Registro de execução

- 28/09/2026 — aprovado no goal único (A12 → A13 → S7 → A14).
- `tools/brand-icons.ps1` (System.Drawing, bicúbico de alta qualidade): recorte quadrado do símbolo, escala para 66/108 dp. Saídas: `mipmap-{mdpi…xxxhdpi}/ic_launcher_foreground.png` (108…432 px, símbolo 66…264 px), `ic_launcher_monochrome.png` e `drawable-nodpi/logo_mark.png` (480 px).
- `mipmap-anydpi-v26/ic_launcher.xml` + `ic_launcher_round.xml` (fundo `@color/ic_launcher_bg` `#0B0D10`, foreground, monochrome); manifest `icon`/`roundIcon` → `@mipmap/…`. Removidos `drawable/ic_launcher.xml`, `ic_mark.xml` e `ic_splash_blank.xml` (sem outro uso). **Sem PNG legado:** `minSdk` 26, todo aparelho suportado usa ícone adaptativo.
- Splash do sistema (`values-v31`, `values-night-v31`): `windowSplashScreenAnimatedIcon` → `@mipmap/ic_launcher_foreground`.
- Splash Compose: `Image(logo_mark)` de `SplashBoot.LOGO_DP` = 120 dp, 24 dp acima do wordmark; `testTag("splash-logo")`; decorativo (`contentDescription = null`).
- **Stitch (item 3): bloqueado pelo mesmo motivo do A13.** O `edit_screens` do MCP não persiste, e o MCP não recebe imagem. **[DONO]** No Stitch web, telas `splash` dark/light: além do rename do A13, colocar o logo (upload de `design/brand/icon.png`, ~120 dp, centralizado, 24 dp acima de "Dieta Bot"). **[AGENTE]** Depois: exportar, aplicar a máscara na área do logo no gate e reativar os testes de splash.

### Resultados da validação

1. `assembleDevRelease` → rótulo "Dieta Bot Dev", `application-icon-*` → recurso adaptativo (caminho encurtado pelo build de release). ✅
2. Emulador: gaveta de apps com o ícone novo ("Dieta Bot Dev": fundo escuro, anel dourado, prato, robô pequeno) — evidência em `docs/qa/android/current/light/launcher.png`. Ícone temático: camada `monochrome` presente; não foi ligado no launcher do emulador. ⏳ dono vê no celular.
3. Cold start (`am start -W`, primeiro frame): release **0,83–1,10 s** (4 medições, `speed-profile`); debug no emulador 2,8–3,4 s (5,5 s logo após instalar, verificação de dex). A duração da splash (`SplashBoot`) não mudou. ✅
4. `capture-onboarding` dark/light sem ✗. `diff-gold` o1–o4 ✓. **splash ✗** (dark 6,26% / tinta 3,46; light 4,60% / 2,16): logo + "Dieta Bot" × gold sem logo e com "Nutri". ⏳ Stitch.
5. `testDevDebugUnitTest` 171 (2 pulados do A13) / 0 falhas; Roborazzi: baselines `splash` regravadas (logo). ✅
6. Manual (dono): ícone e splash no celular. ⏳

### Atualização A15 (28/09/2026)

- Gold novo com o logo (upload do dono no Stitch web): caixa de 120 dp com o símbolo ≈ 60 dp. `SplashBoot.LOGO_BOX_DP` = 120, `LOGO_DP` = 60; espaços medidos no PNG do gold (logo 16 dp acima do wordmark, barra 10 dp abaixo).
- Máscara do logo no gate: **não foi preciso**, o logo real bate com o gold (`StitchGoldTest` splash 0,04% / 0,03%; `diff-gold` 0,42% / 0,41%). ✅
- Roborazzi `splash` regravado. ✅
- Falta: **[DONO]** ícone e splash no celular.

### Aprovação manual (28/09/2026)

Frase do dono: "Tudo certo, sobre a tinta do dark, mantem o token do agents. Pode completar todos os planos e dai realize um novo commit."
- Ícone e splash no celular: confirmados. ✅

## Encerramento

Depois da implementação, registre resultados reais e aplique o ciclo de vida em `docs/sdd/README.md`.

Só declaração explícita do dono cancelando este plano permite `Cancelado` e a pasta `plans/cancelled/`.
