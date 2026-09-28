# Plano — A7 push no horario do slot (Concluído)

- Estado: Concluído
- Data: 25/09/2026
- Data de conclusão: 27/09/2026
- Contexto proprietario: `android`
- Codigo afetado: `apps/android/`
- Pre-requisitos: A3 + A5 + [memoria-push.md](../../../produto/specifications/memoria-push.md)

## Gate de autorizacao

> Aprovado pelo usuário via comando:
> `/goal Aprovo o plano docs/android/plans/a7-push.md. Analise e implemente o plano aprovado.`

## Objetivo

Exact alarm por slot. So se empty. Registrar abre Chat. Pular grava skip.

## Fontes de verdade

- Visual Gold (Stitch): `docs/qa/stitch/dark/push.png` e `docs/qa/stitch/light/push.png`
- spec [memoria-push.md](../../../produto/specifications/memoria-push.md)

## Escopo de implementacao

- POST_NOTIFICATIONS + SCHEDULE_EXACT_ALARM. Android 14 exact: se negar, fallback inexact documentado.
- AlarmScheduler.resync(slots) hoje + 00:05.
- Receiver: se eaten ou skipped, return.
- Copy: {name}. Ainda nao registrou.
- Registrar → Chat. Pular → addSkip + cancela notify.
- Resync no boot, midnight SP e saveSlots.
- Sem Firebase. Sem 16:30 hardcode.

## Validacao planejada

- Unit resync 4 slots → 4 intents; skip existente nao agenda.
- Validacao visual Compose vs Stitch Gold: capturas emulador `docs/qa/android/current/{dark,light}/push.png` comparadas pixel a pixel contra `docs/qa/stitch/{dark,light}/push.png` com diff list aprovada.

## Fora de escopo

Fds especial so almoco+janta.

## Criterios de aceite

- Slots novos em Config re-agendam.
- Slot gravado nao notifica.

## Resultado da implementação

### Entregue

- **`domain/PushPlan`** (puro): alarmes do dia = slots do perfil ainda sem log e sem skip, no horário de cada um (America/Sao_Paulo), só os futuros; snapshot de outro dia conta como dia vazio; `nextResync` às 00:05 SP; `shouldNotify` (checagem no disparo); copy `{nome}. Ainda não registrou.`. Fds usa os slots cadastrados (nenhuma regra de "só almoço + janta").
- **`core/push/SlotAlarmScheduler`**: `resync()` lê um snapshot novo, cancela os alarmes agendados antes (ids em SharedPreferences `nutri_push`), agenda um `RTC_WAKEUP` por slot devido + o resync das 00:05. Exato (`setExactAndAllowWhileIdle`) quando `canScheduleExactAlarms()`; senão `setAndAllowWhileIdle` (inexato: pode chegar alguns minutos depois, nunca antes). Antes do onboarding, só o resync.
- **`core/push/PushHandler`**: no disparo, confere de novo log/skip (regra 2) e `POST_NOTIFICATIONS`; notificação no canal "Lembretes de refeição" (importância alta, categoria REMINDER, ícone monocromático garfo e faca, cor gold) com título `{nome}. Ainda não registrou.` e ações **Registrar** (abre o app no Chat) e **Pular** (`addSkip`, some a notificação, cancela o alarme do slot).
- **`core/push/PushReceiver`** (não exportado): alarme do slot, Pular, resync das 00:05, `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_SET`, `TIMEZONE_CHANGED`, `SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`.
- **`core/push/PushSync`** (iniciado no `NutriApplication`): com o app vivo, qualquer mudança de slots (Config/`saveSlots`, onboarding), logs ou skips reagenda; slot que ganhou log ou skip perde a notificação que estiver na tela.
- **`MainActivity`**: `nutri_open=chat` (Registrar) abre Splash → Home → Chat e apaga a notificação daquele slot; a Home pede `POST_NOTIFICATIONS` (Android 13+) uma vez, só o diálogo do sistema.
- **Manifest**: `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`, receiver. Sem Firebase. Sem horário fixo no código.
- **QA**: `tools/capture-push.sh` (alarmes reais no emulador); `push` em `GOLD_CONFLICTS` do `tools/diff-gold.mjs`.

### Decisões tomadas na implementação

- **Gold `push` é tela do sistema.** O gold desenha uma tela de bloqueio (relógio 20:00, pílula "RITMO CIRCADIANO ATIVO", cartão com botões em pílula). O app só publica a notificação: tela de bloqueio, cartão, botões e cores são do SystemUI, que o app não controla. Captura = tela de bloqueio real com a notificação; diff reportado, sem gate (78% light, 23,75% dark; conteúdo diferente por construção).
- **Copy da spec, não do gold.** Gold: "Lembrete do Jantar / Você ainda não registrou sua refeição das 20:00. Deseja registrar agora ou pular?" + "Registrar agora" / "Pular refeição". Spec memoria-push (push, regras 3 e 4) e este plano: `{nome}. Ainda não registrou.` + Registrar / Pular — seguidos (texto explícito da spec; tom seco do AGENTS). **Pendente de decisão do dono** se quiser a copy do gold (atualizar a spec).
- **Permissão de alarme exato**: não há tela para levar o user às configurações (ADR-012). Sem ela, fallback inexato documentado acima; ao ser concedida, o receiver reagenda.
- **Registrar apaga a notificação** (as ações não usam `autoCancel`): achado no emulador, corrigido com `EXTRA_SLOT` + `PushHandler.clear`.
- **Log/skip no app apaga a notificação do slot** (via `PushSync`): evita lembrete de refeição já registrada.

## Validação executada

1. `./gradlew.bat :app:testDebugUnitTest`: 186/186. Novos: `PushPlanTest` (4 slots vazios → 4 alarmes no horário SP; log/skip/passado fora; snapshot velho após meia-noite = dia vazio; fds com os slots cadastrados; resync 00:05; copy e checagem no disparo), `SlotAlarmSchedulerTest` (4 slots → 4 alarmes + resync; skip e log existentes não agendam e o resync cancela; Config com slots novos reagenda e cancela o removido; exato negado ainda agenda os 4; antes do onboarding só o resync; fim do dia só os futuros), `PushHandlerTest` (slot vazio notifica com a copy e as ações Registrar/Pular, Registrar abre `MainActivity` com `nutri_open=chat` e o slot; slot com log ou skip não notifica; Pular grava skip sem log e some a notificação; sem permissão nada é publicado; log no app apaga a notificação do slot; slot desconhecido ignorado).
2. Emulador (`Medium_Phone`, API 36), `tools/capture-push.sh`, slots semeados relativos à hora atual — A +2 min vazio, B +3 min com log, C +4 min vazio, D +10 min pulado:
   - light: Home pede a permissão de notificação; sem `SCHEDULE_EXACT_ALARM`, A e C agendados inexatos (B e D sem alarme); com ela, A e C exatos (`window=0 exactAllowReason=permission`); app em segundo plano, alarmes reais: A notificou com a copy, B não, C notificou, D não; captura da tela de bloqueio; Pular em A gravou o skip, nenhum log, notificação sumiu; Registrar em C abriu o Chat e a notificação sumiu. Exit 0.
   - dark: mesmo fluxo, 16 checagens ✓, 0 ✗, exit 0; captura da tela de bloqueio no tema escuro do sistema.
3. Gate visual: `push` reportado como conflito (tela do sistema): 78,03% light, 23,75% dark; nenhuma tela do app mudou. JVM sem render de push (não é uma tela Compose).

## Lista de diffs (gold × app)

- Tudo fora da notificação é SystemUI do emulador: relógio, data, papel de parede, ícone de desbloqueio; a pílula "RITMO CIRCADIANO ATIVO" não existe no Android.
- Cartão: estilo do sistema (título na linha, ações em texto) × cartão escuro com botões em pílula do gold; agrupado com as outras notificações do Nutri quando há mais de uma.
- Copy: spec × gold (ver decisões).

## Encerramento

Ciclo SDD.
