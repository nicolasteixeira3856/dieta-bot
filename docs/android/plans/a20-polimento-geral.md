# Plano — A20 Polimento: feedback de toque, botões dos sheets, respiro de scroll, Config

- Estado: Aguardando aprovação
- Data: 29/09/2026
- Contexto proprietário: `android`
- Código afetado: `apps/android/` (`core/designsystem`, `feature/config`, `feature/home`, `feature/onboarding`, `feature/chat` só no toque)
- Pré-requisitos: Nenhum. Nenhum gold muda: os ajustes aproximam o app dos golds atuais (`cfg`, `home1`, `chatP`).

## Gate de autorização

Este plano é exclusivamente documental. A implementação só começa após aprovação explícita que identifique este arquivo:

> Aprovo o plano `docs/android/plans/a20-polimento-geral.md`. Implemente o plano aprovado.

## Objetivo

Os ajustes de layout do feedback que não precisam de Stitch.

## Escopo de implementação

### 1. Feedback de toque e vibração (`core/designsystem`)

- Todo controle tocável ganha indicação visual (ripple M3 / estado pressionado). Hoje há 34 `clickable(...)` e alguns com `indication = null` em controle real. `indication = null` fica só em scrim e fundo de sheet (que bloqueiam toque).
- Um `Modifier.dietaClick(haptic = …)` no design system centraliza a regra. Vibração (`LocalHapticFeedback`), sempre respeitando a configuração de vibração ao toque do sistema:
  - `Confirm` em Gravar, Substituir, Salvar, Continuar e Pular refeição.
  - `ContextClick` (leve) em chips, linhas da Config, abas, Trocar, Cancelar e FAB.
  - Nada em rolagem nem em digitação.
- Botões M3 que já têm ripple ganham só a vibração.

### 2. Botões dos sheets (Config e onde houver o par Salvar/Cancelar)

- `Cancelar` passa a ser um botão de verdade, do mesmo tamanho do `Salvar` (largura total, 52 dp, pill, 1 dp de borda, texto 15 sp W700 na cor primária). É o par `Pular refeição`/`Cancelar` do gold `chatP`. Hoje é um `Text` `labelMd` muted com área de toque pequena.
- Um componente só (`SheetActions(primary, secondary)`), usado nos sheets da Config e no diálogo de pular da Home, que hoje usa `TextButton`.

### 3. Respiro no fim das telas com scroll

- Home: o disclaimer "Estimativa nutricional…" fica acima do FAB. Padding inferior do conteúdo = altura do FAB + margem + inset de navegação, medido no gold `home1`.
- Config, O1–O4 e sheets com scroll: o último item termina acima do CTA/barra, com ≥ 24 dp de respiro + inset de navegação (`WindowInsets.navigationBars`/`safeDrawing`).
- Chat fica como está (dono: "está bom").

### 4. Config: tamanho dos títulos

- No gold `cfg`, "Compensação de treinos", "Macronutrientes (P · C · G)" e "Gasto calórico do treino" têm o mesmo tamanho de "Meta de calorias". O código usa o mesmo estilo (16 sp W500), então a diferença nasce no layout (quebra de linha, peso da coluna do valor, detalhe). Diagnóstico na captura vs gold, e ajuste até bater.

### 5. Copy dos sheets da Config (`ConfigScreen.kt`)

| Sheet | Hoje | Depois |
|---|---|---|
| Treino de hoje | "Kcal do treino. Vazio: crédito 0. Zera à meia-noite." | (sem texto) |
| Horários das refeições | "Nome e horário. Os registros de hoje ficam." | "Mudar nome ou horário não apaga o que você já registrou hoje." |

- `produto/specifications/memoria-push.md` § Config: registrar a copy.

## Arquivos e áreas afetadas

- `core/designsystem/*` (modificador + `SheetActions`).
- `feature/config/ConfigScreen.kt`, `feature/home/HomePanelScreen.kt`, `feature/onboarding/*`, `feature/chat/ChatScreen.kt` (só `clickable` → modificador).
- Roborazzi e scripts de captura, se algum testTag mudar.

## Validação planejada

1. `testDevDebugUnitTest` + `verifyRoborazziDevDebug` (renders novos dos sheets).
2. Emulador: `cfg`, `home1` (disclaimer inteiro acima do FAB), O1–O4 com scroll até o fim; captura vs gold com diff escrito.
3. Área de toque: `Cancelar` ≥ 48 dp de altura (Accessibility Scanner ou `UiAutomator` bounds).
4. Manual (dono): sentir a vibração em Gravar/Salvar; desligar a vibração ao toque no sistema e confirmar que o app para de vibrar.

## Fora de escopo

- Qualquer mudança de gold ou de tela nova.
- Seletor de horário: [A21](a21-seletor-horario.md).

## Riscos e controles

- **Ripple em lugar que o gold não mostra:** ripple só aparece durante o toque; a captura estática não muda.
- **Vibração demais:** só as duas intensidades acima.

## Critérios de aceite

- Todo botão responde ao toque visualmente; Gravar/Salvar vibram.
- Cancelar e Salvar do mesmo tamanho.
- Nada cortado sob FAB/CTA no fim de nenhuma tela com scroll.
- Títulos da Config iguais ao gold.

## Encerramento

Registre resultados e aplique o ciclo de vida de `docs/sdd/README.md`, com a entrega git (§ 6).
