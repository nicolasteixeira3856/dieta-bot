# Especificacao — Perfil e onboarding

## Estado

Vigente desde o [A2](../../android/plans/completed/a2-onboarding-perfil.md): splash + O1-O4 conforme Stitch gold, perfil completo em Room v2.

Mudanças planejadas (29/09/2026, aguardando aprovação): horário em diálogo com rodas ([A21](../../android/plans/a21-seletor-horario.md), gate [ST3](../../stitch/plans/completed/st3-seletor-horario.md)); refeições por dia da semana na O3 em etapas ([ADR-021](../adrs/ADR-021-refeicoes-por-dia.md), [A24](../../android/plans/a24-refeicoes-por-dia.md), gate [ST4](../../stitch/plans/completed/st4-refeicoes-por-dia.md)).

## Contexto e objetivo

Perfil persistente que a IA recebe em todo turno. Onboarding coleta TMB+teto+slots+macros.

## Escopo

O1-O4. Edicao posterior = Config. TMB so sugere.

## Fora de escopo

TDEE de manutencao como meta oculta. Nutricionista. Health/Xiaomi. 2 g/kg.

## Regras funcionais

1. O1: sexo, idade, altura cm, peso kg. TMB Mifflin-St Jeor (homem 10w+6.25h-5a+5; mulher 10w+6.25h-5a-161). Prefill teto = TMB arredondado 10 kcal, em todos os campos de teto ate o user editar um. Legenda: "Sugerido {n} kcal com base no seu perfil. Voce pode alterar quando quiser." User edita livre. Modo teto: mesmo / util-fds / 7 dias.
2. O2: eat-back 0% | % digitavel default 50 | 100%. Sem cap.
3. O3: 2-6 refeicoes, padrao 4 (07:30, 12:30, 16:00, 20:00). Cada uma: nome + horario. Nome comeca vazio. Chips de sugestao pela hora do slot (Stitch): 05-10 Cafe / Desjejum; 10-11 Lanche da manha / Lanche; 11-15 Almoco / Prato feito; 15-18 Lanche / Cafe da tarde; 18-22 Jantar / Ceia; 22-05 Ceia / Lanche da noite. Tap no chip preenche o campo. Nunca grava nome sem o user confirmar. Mudar a quantidade preserva linhas editadas; linhas intocadas assumem os horarios padrao da nova quantidade. Timezone America/Sao_Paulo.
4. O4: P/C/G derivados 30/40/30 sobre o teto do dia 1 (4/4/9 kcal/g). Campos editaveis; editado nao e recalculado. Barra de proporcao reflete os gramas. Rodape: "{teto do dia 1} KCAL TOTAL ESTIMADA".
5. Perfil salvo em Room. onboardingDone=1 so no fim de O4.
6. Prefix do chat: teto vigente, eat-back, alvos P/C/G, lista nome+hora dos slots.
7. Disclaimer na splash com a copy do Stitch ("Estimativa nutricional, nao substitui consulta medica ou nutricional.") e na Home. As telas O1-O4 do gold nao tem disclaimer.

## Estados e falhas

- Campo vazio em O1/O3/O4: CTA desliga.
- Teto < 800: aceita se o user digitou.
- Reinstalacao: Room some. Onboarding de novo.

## Fronteiras e ownership

Dono: produto. Implementacao: android A2.

## Decisoes relacionadas

- [ADR-012](../adrs/ADR-012-chat-home-perfil.md)

## Planos relacionados

- [A2 (Concluido)](../../android/plans/completed/a2-onboarding-perfil.md)

## Criterios de aceite funcionais

- 4 telas nesta ordem.
- Teto editavel depois do prefill.
- Slot sem nome nao persiste.
- Perfil lido de volta apos kill do processo.
