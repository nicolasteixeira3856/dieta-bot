# Plano — A24 Refeições por dia da semana

- Estado: Aguardando aprovação
- Data: 29/09/2026
- Contexto proprietário: `android`
- Código afetado: `apps/android/` (`core/database` — Room v3 → v4, `domain`, `feature/onboarding`, `feature/config`, `feature/home`, `feature/chat/PromptBuilder.kt`, `core/push`)
- Pré-requisitos: **[ST4](../../stitch/plans/completed/st4-refeicoes-por-dia.md) em `stitch/plans/completed/`** (golds `o3`, `o3s`, `cfgS`). [A21](pending_manual_validation/a21-seletor-horario.md) concluído (O3 com o seletor novo). Aceita o [ADR-021](../../produto/adrs/ADR-021-refeicoes-por-dia.md). **Último plano do lote.**

## Gate de autorização

Este plano é exclusivamente documental. A implementação só começa após aprovação explícita que identifique este arquivo:

> Aprovo o plano `docs/android/plans/a24-refeicoes-por-dia.md`. Implemente o plano aprovado.

**Primeiro passo da implementação:** confirmar `docs/stitch/plans/completed/st4-refeicoes-por-dia.md` e os golds `o3s`/`cfgS`. Se não, parar e avisar o dono.

## Objetivo

Refeições diferentes por dia da semana, no mesmo esquema de modos do teto, sem quebrar nada para quem já usa o app.

## Escopo de implementação

### 1. Dados (Room v3 → v4)

- `meal_slot.days INTEGER NOT NULL DEFAULT 127`: bitmask seg=1 … dom=64.
- `profile.slotMode TEXT NOT NULL DEFAULT 'same'` (`same` | `split` | `each`).
- `Migration(3, 4)` com `ALTER TABLE … ADD COLUMN`. Todo slot existente fica com 127 e o perfil com `same`: comportamento idêntico ao de hoje.
- Um slot pertence a um grupo de dias. No modo `split`, os grupos são seg–sex (31) e sáb–dom (96); no `each`, um grupo por dia. `meal_log.slotId` continua apontando para o slot: histórico intacto.

### 2. Domínio

- `SlotsOfDay(date)`: slots cujo `days` contém o dia da semana de `date` em America/Sao_Paulo. Todo consumidor passa a usar isso: Home (timeline), `PromptBuilder` (perfil e snapshot), `SlotClock`/sugestão de slot, push (`SlotAlarmScheduler` agenda só os do dia e reagenda na virada), Trocar/Pular do Chat.
- Log de hoje cujo slot não é do dia → bloco "Outros" da timeline (regra existente).

### 3. Onboarding O3 (golds `o3`, `o3s`)

- Tags `Todos os dias` | `Seg–Sex · Sáb–Dom` | `Cada dia`, com o padrão `Todos os dias` (fluxo de hoje, uma etapa).
- Modo com grupos: etapas na mesma tela, com cabeçalho `{grupo} · Etapa {i} de {n}` e barra de n segmentos. O CTA avança de grupo e, na última etapa, vai para a O4. O Voltar recua um grupo; na 1ª etapa, volta para a O2.
- `Copiar de {grupo anterior}` em toda etapa depois da primeira.
- Trocar de modo no meio: as etapas já preenchidas são mantidas quando o grupo existe no modo novo; se não existir, pede confirmação `Descartar os horários de {grupo}?`.

### 4. Config (gold `cfgS`)

- Modo `same`: seção como hoje (gold `cfg`).
- Outros modos: uma linha por grupo (`Seg a Sex` · `4 refeições · 07:30 a 20:00`). Toque → editor de refeições em tela cheia com o mesmo passo a passo da O3 e o cabeçalho da Config.
- Salvar refeições não apaga logs (ADR-021 regra 7).

### 5. Migração segura para os testers

- Teste de migração do Room (`MigrationTestHelper`) com um banco v3 real: o dono exporta o `nutri.db` do app dev (`adb exec-out run-as com.nutri.android.dev cat databases/nutri.db`, por cabo) ou, sem cabo, um banco v3 montado pelos fixtures atuais. Depois da migração: mesmos slots, logs, skips, chat e memória.
- Build dev instalado **por cima** do 0.0.N anterior no emulador, com dados reais antes e depois (Home igual, push agendado igual).

### 6. Specs

- `perfil-onboarding.md` regra 3, `memoria-push.md` (Config e push regra 5), `home-timeline.md` regra 4, `chat.md` regra 8, `docs/android/specifications/room-v2.md` (v4).

## Arquivos e áreas afetadas

- `core/database/*` (entidades, DAO, `DietaBotDatabase` v4, migração, schema exportado).
- `domain/*` (slots do dia), `core/push/SlotAlarmScheduler.kt`.
- `feature/onboarding/*`, `feature/config/*`, `feature/home/*`, `feature/chat/PromptBuilder.kt`, `ChatViewModel.kt`.
- `tools/capture-onboarding.sh`, `tools/capture-config.sh` (estados `o3s`, `cfgS`).

## Validação planejada

1. `testDevDebugUnitTest`: `SlotsOfDay` por modo e dia (incluindo domingo → sáb–dom); prompt de sábado só com os slots de sáb–dom; troca de modo mantendo grupos.
2. Teste de migração 3→4 (seção 5).
3. `verifyRoborazziDevDebug`.
4. Capturas `o3`, `o3s`, `cfg`, `cfgS` vs gold, dark e light, com diff.
5. Emulador: data do sistema num sábado → Home, push e sugestão de slot com as refeições de sábado.
6. Manual (dono + 1 tester): atualizar pelo App Tester sem perder nada; depois ativar `Seg–Sex · Sáb–Dom`.

## Fora de escopo

- Teto por grupo de refeição (o modo do teto continua separado).
- Refeições em datas específicas (feriado).

## Riscos e controles

- **Quebrar o app dos testers na atualização:** migração aditiva (só `ADD COLUMN` com default), teste com banco v3 real, instalação por cima antes de distribuir.
- **Push de fim de semana errado:** reagendamento na virada do dia, testado com data do emulador.

## Critérios de aceite

- Usuário atual atualiza e não vê diferença nenhuma.
- Com `Seg–Sex · Sáb–Dom`, sábado mostra, avisa e sugere só as refeições de sábado.
- Golds `o3`, `o3s`, `cfgS` batendo.

## Encerramento

Registre resultados e aplique o ciclo de vida de `docs/sdd/README.md`, com a entrega git (§ 6).
