# ADR-012 — Chat é tela; Home é contador; perfil nomeado

- Estado: Proposto
- Data: 2026-09-25
- Contexto: `produto`
- Substitui: parcialmente `AGENTS.md` (Chat sheet, Home com composer, onboarding 2 telas, foto ≤1280 JPEG 70, telas só splash O1 O2 T0 T1 T2 T3, FAB proibido). Não substitui ADR 010 (Room) nem 011 (tap grava). ADR 011 continua para T2/T3 até o plano A5 remover essas rotas.

## Contexto

O app vivo registra em <15s via composer na Home + sheet T1 + T2 + T3. O dono travou outro produto: Home só mostra o dia; o registro e a conversa moram numa tela Chat aberta pelo FAB; o perfil leva TMB, teto editável, P/C/G e refeições nomeadas com horário; a IA recebe perfil + memória curta + snapshot do dia + até 12 msgs, com no máximo 2 digest no mesmo dia.

Constituição ainda descreve o app antigo. Sem este ADR, o agente reabre o debate a cada `/goal`.

## Decisão

1. **Home** é contador (kcal + P/C/G) + timeline do dia + atalho Config + FAB. Sem composer. Sem chip de janela no sentido antigo.
2. **Chat** é tela cheia, rota própria, aberta pelo FAB. Bolhas. Sem avatar, sem visto, sem status. Composer embaixo (texto + clipe + enviar). Back volta à Home.
3. **Config** é tela. Edita teto, refeições (nome + hora), política de eat-back e o kcal de treino **do dia**. Mudar o teto no meio do dia apaga só `meal_log` de hoje. Fio do chat de hoje fica na UI e sai do prompt. Memória e dias anteriores ficam.
4. **Onboarding** passa a: corpo/TMB (prefill do teto) + teto editável + eat-back + P/C/G derivados 30/40/30 editáveis + N refeições nomeadas com horário. Sugestões de nome por faixa de hora; nunca assumir o nome.
5. **Registro** só conta no dia depois do chip **Gravar refeição {slot}**. Estimate na bolha não grava. Sem tap, não entra no contador. Segundo Gravar no mesmo slot **soma**. Chip **Trocar** lista os slots do perfil. Chip **Pular {slot}** e tap na timeline também pulam. Pular nunca é automático.
6. **Prompt** do turno = perfil + memória ≤ ~1,3 k tok + snapshot Room do dia + até 2 digest + últimas ≤ 12 msgs raw. Digest substitui o bloco raw. Snapshot do dia não compacta. Server continua stateless.
7. **Foto** no Chat: câmera + picker. Client converte HEIC→JPEG qualidade 90 sem downscale. Cap 16 MB no JPEG. Timeout 60 s. Recusa >16 MB antes da Luna. Foto não persiste no server.
8. **Push** no horário de cada slot: `{slot}. Ainda não registrou.` Ações: Registrar (abre Chat) | Pular. “Já comi” neste corte = Pular.
9. **Fio** na UI: 60 dias, separador de data. Prompt: só o dia corrente.
10. T2 e T3 saem quando A5 estiver DONE. Até lá ADR 011 vale.

LLM continua só `gpt-6-luna`, `reasoning.effort=none`. Sem Gemini. Sem chave no APK. Sem TDEE como produto (TMB é prefill do teto, o número final é o que o user editou).

## Motivação

O job antigo (15 s na Home) foi trocado pelo dono por conversa + confirmação. Home sem composer é o doodle que ele enviou. Prompt curto + digest evita dump de 36 msgs raw. Chip Gravar preserva “Sim não grava sozinho” do ADR 011, com cara de WhatsApp.

## Consequências

### Positivas

- Um destino para prosa, foto, receita e dúvida.
- Perfil estável vai no prefixo barato.
- Room continua a fonte do dia.
- Server não guarda user.

### Negativas

- Registro deixa de ser 15 s na Home. Trade aceito.
- Constituição `AGENTS.md` fica defasada até um commit posterior alinhá-la a este ADR (fora deste Planning).
- Foto 16 MB + 60 s falha em 4G ruim. Aceito.
- FAB deixa de ser “showcase”: é o único caminho de registro.

## Alternativas consideradas

### Chat sheet na Home

Rejeitado pelo dono. FAB → tela.

### Composer na Home + FAB

Rejeitado. Home do doodle não tem composer.

### Dump das 36 msgs raw no prompt

Rejeitado. Custo e ruído. Digest substitui raw.

### Tool `log_meal` grava sozinha

Rejeitado. Tap do user grava.

### Cap 4 MB / timeout 20 s

Rejeitado. Dono: 16 MB / 60 s.

## Relações

- Especificações afetadas: `docs/produto/specifications/*` (a criar), `docs/android/specifications/room-v2.md`, `docs/server/specifications/v1-chat.md`.
- ADRs relacionados: [010](../../decisions/010-room.md), [011](../../decisions/011-t2-t3-actions.md).
- Contextos consumidores: `android`, `server`.

Depois de aceito, este ADR não se edita. Mudança posterior exige ADR novo.
