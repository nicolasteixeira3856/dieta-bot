# ADR-021 — Refeições por dia da semana

- Estado: Aceito (ST4 concluído; A24 aprovado explicitamente pelo dono em 29/09/2026); parcialmente substituído pelo [ADR-058](ADR-058-extras-and-history.md) (regra 7: um registro feito como extra tem tipo próprio e não cai em "Outros")
- Data: 2026-09-29
- Contexto: `produto`
- Substitui: parcialmente o [ADR-012](ADR-012-chat-home-perfil.md), regra 4 ("N refeições nomeadas com horário", uma lista para a semana toda), e a regra 3 da [perfil-onboarding](../specifications/perfil-onboarding.md).

## Contexto

O teto já pode variar por dia (modos "mesmo valor", "útil / fim de semana", "7 dias"). As refeições não: a mesma lista vale para segunda e domingo. No fim de semana a rotina muda (acorda mais tarde, junta café e almoço), e o push e a sugestão de slot erram.

## Decisão

1. As refeições seguem **os mesmos três modos do teto**, e o usuário escolhe o modo por tags no topo da O3:
   - `Todos os dias` (padrão: comportamento de hoje)
   - `Seg–Sex · Sáb–Dom`
   - `Cada dia`
2. A O3 vira um passo a passo **dentro da mesma tela**: uma etapa por grupo de dias ("Seg a Sex", depois "Sáb e Dom"; ou "Seg", "Ter"…). Cada etapa é a lista de hoje (2–6 refeições, nome + horário + sugestões). O CTA avança de grupo; na última etapa, vai para a O4. O `Voltar` recua um grupo. Botão "Copiar do grupo anterior" em toda etapa depois da primeira.
3. O modo das refeições é independente do modo do teto.
4. O "dia" de uma refeição segue America/Sao_Paulo. Home, push, Chat (perfil e snapshot) e sugestão de slot usam só as refeições do dia corrente.
5. **Usuário existente:** a migração marca todas as refeições atuais como "todos os dias". Nada muda para ele até escolher outro modo na Config.
6. Config → "Horários das refeições": mostra o modo e os grupos, e edita com o mesmo passo a passo.
7. Trocar o modo ou as refeições de um dia **não apaga** registros de hoje. Registro de uma refeição que deixou de existir hoje vai para o bloco "Outros" da timeline (regra que já existe).

## Motivação

Mesma mecânica que o usuário já conhece do teto. O caso comum (Seg–Sex / Sáb–Dom) sai em duas etapas.

## Consequências

### Positivas

- Push e sugestão de slot certos no fim de semana.

### Negativas

- Migração do Room (coluna de dias por refeição). Risco para os testers atuais; o plano [A24](../../android/plans/completed/a24-refeicoes-por-dia.md) testa a migração com o banco real de um tester.
- Golds ([ST4](../../stitch/plans/completed/st4-refeicoes-por-dia.md)): `o3` alterado (tags de modo), `o3s` novo (etapa de um grupo) e `cfgS` novo (Config com grupos). A edição pela Config reusa a tela da O3 com o cabeçalho da Config, sem gold próprio.

## Alternativas consideradas

- **Lista única com "dias ativos" por refeição:** mais flexível, mas vira uma grade confusa no celular.
- **Só Seg–Sex / Sáb–Dom:** não atende quem treina em dias alternados; "Cada dia" cobre esse caso.

## Relações

- Gate: [ST4](../../stitch/plans/completed/st4-refeicoes-por-dia.md). Implementação: [A24](../../android/plans/completed/a24-refeicoes-por-dia.md).

Depois de aceito, este ADR não se edita.
