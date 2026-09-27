# Plano — A2 Onboarding perfil

- Estado: Aguardando aprovacao
- Data: 25/09/2026
- Contexto proprietario: `android`
- Codigo afetado: `apps/android/`
- Pre-requisitos: [A1](completed/a1-room-v2.md) + [A0 Roborazzi](completed/a0-roborazzi-setup.md) + [perfil-onboarding.md](../../produto/specifications/perfil-onboarding.md)

## Gate de autorizacao

> Aprovo o plano `docs/android/plans/a2-onboarding-perfil.md`. Implemente o plano aprovado.

## Objetivo

O1-O4 no lugar de O1-O2. Perfil completo em Room. Splash alinhada ao Stitch gold. Primeira comparacao Roborazzi contra gold.

## Fontes de verdade

- Visual Gold (Stitch): `docs/qa/stitch/dark/{splash,o1,o2,o3,o4}.png` e `docs/qa/stitch/light/{splash,o1,o2,o3,o4}.png`
- spec perfil-onboarding (slots 2-6, chips por faixa, nunca assume)
- ADR-012

## Escopo de implementacao

### 0. Gold em resolucao real + splash

- `tools/export-stitch.mjs` exporta os PNGs gold em resolucao real (hoje 226x512). Re-exportar os 36.
- Splash segue o gold: layout e copy do Stitch ("Estimativa nutricional, nao substitui consulta medica ou nutricional."). Atualizar `SplashBoot.COPY` e `TokensTest`.
- Roborazzi compara `splash` contra `docs/qa/stitch/{dark,light}/splash.png`, tolerancia 1%.

### 1. O1 corpo + teto

- sexo H/M, idade, altura cm, peso kg, teto kcal prefill TMB se user ainda nao editou o teto nesta sessao, modo teto 3 chips.
- CTA: teto > 0 e sexo escolhido. Idade/altura/peso 0 → TMB some, teto livre.
- Copy: sugerido {n} kcal.

### 2. O2 eat-back

- 0 / % / 100. Sem cap.

### 3. O3 slots

- Stepper 2-6. Nome + TimePicker.
- Chips preenchem campo vazio: 05-10 Cafe da manha; 10-11 Lanche da manha; 11-15 Almoco; 15-18 Lanche da tarde; 18-22 Janta; 22-05 Ceia.
- CTA exige nome em todas as linhas.

### 4. O4 macros

- P C G prefill MacroSplit(teto dia 1). Editaveis.

### 5. Persistencia

- saveProfile + saveSlots no fim de O4. onboardingDone=1 so saindo de O4.
- Nav splash → O1 → O2 → O3 → O4 → Home.

## Validacao planejada

- test O1 27/116/180 male → 2072
- test O4 2000 → 150/200/67
- `node tools/check-stitch.mjs`: 36 gold presentes em resolucao real.
- Validacao visual Compose vs Stitch Gold: capturas `docs/qa/android/current/{dark,light}/{splash,o1,o2,o3,o4}.png` (Roborazzi + emulador) comparadas contra `docs/qa/stitch/{dark,light}/{splash,o1,o2,o3,o4}.png`, tolerancia 1%, com diff list aprovada.

## Fora de escopo

Config, Chat, Home nova, TMB como meta oculta.

## Criterios de aceite

- Kill apos O4: perfil e slots voltam.
- Sem slots vazios gravados.

## Encerramento

Ciclo SDD.
