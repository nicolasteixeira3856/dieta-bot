# Plano — A21 Seletor de horário em rodas

- Estado: Pendente aprovação manual
- Aprovado: 29/09/2026 (owner approval naming this plan).
- Data: 29/09/2026
- Contexto proprietário: `android`
- Código afetado: `apps/android/` (`core/designsystem`, `feature/onboarding`, `feature/config`)
- Pré-requisitos: **[ST3](../../../stitch/plans/completed/st3-seletor-horario.md) em `stitch/plans/completed/`** (gold `o3t`). [A20](a20-polimento-geral.md) concluído (botões e toque do design system).

## Gate de autorização

Este plano é exclusivamente documental. A implementação só começa após aprovação explícita que identifique este arquivo:

> Aprovo o plano `docs/android/plans/a21-seletor-horario.md`. Implemente o plano aprovado.

**Primeiro passo da implementação:** confirmar `docs/stitch/plans/completed/st3-seletor-horario.md` e `o3t.png` em `docs/qa/stitch/{dark,light}/`. Se não, parar e avisar o dono.

## Objetivo

Trocar o `TimePicker` de relógio do Material por rodas de hora e minuto, no estilo do alarme da Samsung, dentro de um diálogo. Vale na O3 e na Config (horário das refeições).

## Fontes de verdade

- Gold `o3t` (dark + light). [ADR-020](../../../produto/adrs/ADR-020-estados-novos-chat-home-horario.md).

## Escopo de implementação

### 1. Componente `TimeWheelDialog` (`core/designsystem`)

- Duas `LazyColumn` com `rememberSnapFlingBehavior`: horas 00–23 e minutos 00–59, **em loop** (lista virtual grande, começando no meio), passo de 1 minuto.
- Faixa de seleção única atravessando as duas rodas; o valor central grande e os vizinhos com tamanho e opacidade do gold.
- Vibração leve (`TextHandleMove`/`SegmentTick`) a cada valor que passa pelo centro, com a regra de vibração do A20.
- Acessibilidade: cada roda é um controle ajustável (`semantics { progressBarRangeInfo / setProgress }`), e o TalkBack lê "07 horas", "30 minutos"; volume/setas ajustam.
- `Cancelar` | `OK` com o mesmo tamanho (gold). OK devolve minutos desde a meia-noite.
- Sem teclado nem modo de digitação (gold não tem).

### 2. Uso

- O3 (`OnboardingScreens.kt`): substitui o `TimePicker` + `rememberTimePickerState` atuais. Título = nome da refeição (ou "Refeição {n}" se vazio).
- Config → editor de horários: o mesmo diálogo.

### 3. QA

- `tools/capture-onboarding.sh`: novo estado `o3t` (toque no horário do 1º card).
- Loop visual `o3t` dark e light.

### 4. Spec

- `perfil-onboarding.md` regra 3: "Horário escolhido em diálogo com rodas (hora e minuto, 24 h)".

## Arquivos e áreas afetadas

- `core/designsystem/TimeWheelDialog.kt` (novo).
- `feature/onboarding/OnboardingScreens.kt`, `feature/config/ConfigScreen.kt`.
- `tools/capture-onboarding.sh`, Roborazzi.

## Validação planejada

1. Teste de unidade da matemática do loop (índice ↔ valor, início no valor dado, 23→00 e 59→00).
2. Teste de UI (Compose): rolar a roda de minutos 3 itens e OK → valor + 3.
3. `verifyRoborazziDevDebug` com o diálogo.
4. Captura `o3t` vs gold, dark e light, com diff.
5. Manual (dono/tester que reclamou): escolher 07:30 e 21:45 sem esforço.

## Fora de escopo

- Refeições por dia: [A24](a24-refeicoes-por-dia.md).
- Formato 12 h.

## Riscos e controles

- **Snap impreciso em fling rápido:** snap por item + teste de UI.

## Critérios de aceite

- `o3t` bate com o gold; nenhum `TimePicker` de relógio sobra no app.

## Encerramento

Registre resultados e aplique o ciclo de vida de `docs/sdd/README.md`, com a entrega git (§ 6).


## Validation

Implementation and automated evidence: [A21 validation](../../validation/a21-seletor-horario.md). Owner validation remains pending: choose 07:30 and 21:45 comfortably on the physical device.
