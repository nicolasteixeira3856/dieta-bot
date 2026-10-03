# Especificacao — Perfil e onboarding

## Contexto e objetivo

Perfil persistente que a IA recebe em todo turno. Onboarding coleta TMB+teto+slots+macros.

## Escopo

O1-O4. Edicao posterior = Config. TMB so sugere.

## Fora de escopo

TDEE de manutencao como meta oculta. Nutricionista. Health/Xiaomi. 2 g/kg.

## Regras funcionais

1. O1: sexo, idade, altura cm, peso kg. TMB Mifflin-St Jeor (homem 10w+6.25h-5a+5; mulher 10w+6.25h-5a-161). Prefill teto = TMB arredondado 10 kcal, em todos os campos de teto ate o user editar um. Legenda: "Sugerido {n} kcal com base no seu perfil. Voce pode alterar quando quiser." User edita livre. Modo teto: mesmo / util-fds / 7 dias. Idade, altura e peso são obrigatórios (> 0, sem faixas de plausibilidade): sem os três, "Modo do teto" e os campos de teto ficam desabilitados (38 %, sem toque, sem foco; campo vazio mostra `—`), a legenda "Sugerido" some e aparece, logo abaixo dos campos do corpo, "Preencha idade, altura e peso para ver a meta sugerida." Os campos de teto começam vazios e recebem o prefill assim que o perfil fica válido. Quem não quer informar o perfil não define o teto à mão. Apagar um campo do corpo depois de editar o teto desabilita os controles de novo e mantém os valores digitados. Teclado: idade → Próximo → altura → Próximo → peso → Concluído (fecha o teclado). Teto: campo único Concluído; dias úteis Próximo → fim de semana Concluído; Seg → … → Dom com Próximo, Dom Concluído.
2. O2: eat-back 0% | % digitavel default 50 | 100%. Sem cap.
3. O3: modo independente do teto: Todos os dias (same, padrão), Seg–Sex · Sáb–Dom (split) ou Cada dia (each). Uma etapa por grupo na mesma tela; cabeçalho de grupo, índice e barra de segmentos. Continuar avança e, na última etapa, abre O4; Voltar recua e, no primeiro grupo, abre O2. "Copiar de {grupo anterior}" copia nomes/horas, preservando IDs de destino. Mudar modo preserva os grupos com a mesma máscara; pede "Descartar os horários de {grupo}?" antes de perder grupos preenchidos. Cada grupo: 2-6 refeicoes, padrao 4 (07:30, 12:30, 16:00, 20:00). Cada uma: nome + horario. Horário escolhido em diálogo com rodas (hora e minuto, 24 h), tanto na O3 quanto na Config. Rodas em loop, passo de um minuto; Cancelar preserva o horário, OK confirma. Nome comeca vazio. Chips de sugestao pela hora do slot (Stitch): 05-10 Cafe / Desjejum; 10-11 Lanche da manha / Lanche; 11-15 Almoco / Prato feito; 15-18 Lanche / Cafe da tarde; 18-22 Jantar / Ceia; 22-05 Ceia / Lanche da noite. Tap no chip preenche o campo. Nunca grava nome sem o user confirmar. Mudar a quantidade preserva linhas editadas; linhas intocadas assumem os horarios padrao da nova quantidade. Timezone America/Sao_Paulo.
4. O4: P/C/G derivados 30/40/30 sobre o teto do dia 1 (4/4/9 kcal/g). Campos editaveis; editado nao e recalculado. Barra de proporcao reflete os gramas. Rodape: "{teto do dia 1} KCAL TOTAL ESTIMADA".
5. Perfil salvo em Room. onboardingDone=1 so no fim de O4.
6. Prefix do chat: teto vigente, eat-back, alvos P/C/G, lista nome+hora só dos slots do dia em America/Sao_Paulo (ADR-021).
7. Disclaimer na splash com a copy do Stitch ("Estimativa nutricional, nao substitui consulta medica ou nutricional.") e na Home. As telas O1-O4 do gold nao tem disclaimer.

## Estados e falhas

- Campo vazio em O1/O3/O4: CTA desliga.
- O1 sem idade, altura ou peso (`o1e`): modo e teto desabilitados, dica do perfil visível, Continuar desligado. Perfil gravado lido de volta é válido e abre em `o1`.
- Teto < 800: aceita se o user digitou.
- Reinstalacao: Room some. Onboarding de novo.

## Fronteiras e ownership

Dono: produto. Implementação: `android`.

## Decisoes relacionadas

- [ADR-012](../adrs/ADR-012-chat-home-perfil.md)
- [ADR-021](../adrs/ADR-021-refeicoes-por-dia.md)

## Criterios de aceite funcionais

- 4 telas nesta ordem.
- Teto editavel depois do prefill.
- Continuar só com sexo, idade, altura, peso e teto preenchidos.
- Slot sem nome nao persiste.
- Perfil lido de volta apos kill do processo.

## Proveniência

- [A2](../../android/plans/completed/a2-onboarding-perfil.md) — Onboarding perfil
- [A21](../../android/plans/completed/a21-seletor-horario.md) — Seletor de horário em rodas
- [ST3](../../stitch/plans/completed/st3-seletor-horario.md) — Seletor de horário em rodas
- [A24](../../android/plans/completed/a24-refeicoes-por-dia.md) — Refeições por dia da semana
- [ST4](../../stitch/plans/completed/st4-refeicoes-por-dia.md) — Refeições por dia da semana
- [A31](../../android/plans/completed/a31-o1-perfil-obrigatorio-teclado.md) — O1: required profile and keyboard flow
- [ST8](../../stitch/plans/completed/st8-teto-sem-perfil.md) — Ceiling screen before the profile (`o1e`)
