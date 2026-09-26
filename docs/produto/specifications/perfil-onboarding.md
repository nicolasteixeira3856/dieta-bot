# Especificacao — Perfil e onboarding

## Estado

Hoje O1 teto + O2 eat-back. Sem corpo, sem TMB, sem slots nomeados, sem alvos C/G.

## Contexto e objetivo

Perfil persistente que a IA recebe em todo turno. Onboarding coleta TMB+teto+slots+macros.

## Escopo

O1-O4. Edicao posterior = Config. TMB so sugere.

## Fora de escopo

TDEE de manutencao como meta oculta. Nutricionista. Health/Xiaomi. 2 g/kg.

## Regras funcionais

1. O1: sexo, idade, altura cm, peso kg. TMB Mifflin-St Jeor (homem 10w+6.25h-5a+5; mulher 10w+6.25h-5a-161). Prefill teto = TMB arredondado 10 kcal. User edita livre. Modo teto: mesmo / util-fds / 7 dias.
2. O2: eat-back 0% | % digitavel default 50 | 100%. Sem cap.
3. O3: 2-6 refeicoes. Cada uma: nome + horario. Chips de sugestao por faixa (05-10 Cafe da manha; 11-15 Almoco; 18-22 Janta). Chip preenche o campo. Nunca grava nome sem o user confirmar. Timezone America/Sao_Paulo.
4. O4: P/C/G derivados 30/40/30 sobre o teto do dia 1. Campos editaveis.
5. Perfil salvo em Room. onboardingDone=1 so no fim de O4.
6. Prefix do chat: teto vigente, eat-back, alvos P/C/G, lista nome+hora dos slots.
7. Disclaimer "estimativa, nao consulta" no onboarding e na Home.

## Estados e falhas

- Campo vazio em O1/O3/O4: CTA desliga.
- Teto < 800: aceita se o user digitou.
- Reinstalacao: Room some. Onboarding de novo.

## Fronteiras e ownership

Dono: produto. Implementacao: android A2.

## Decisoes relacionadas

- [ADR-012](../adrs/ADR-012-chat-home-perfil.md)

## Planos relacionados

- [A2](../../android/plans/a2-onboarding-perfil.md)

## Criterios de aceite funcionais

- 4 telas nesta ordem.
- Teto editavel depois do prefill.
- Slot sem nome nao persiste.
- Perfil lido de volta apos kill do processo.
