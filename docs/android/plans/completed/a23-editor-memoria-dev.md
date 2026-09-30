# Plano — A23 Editor de memória e perfil da IA (só dev)

- Estado: Concluído (29/09/2026)
- Data: 29/09/2026
- Contexto proprietário: `android`
- Código afetado: `apps/android/` (`app/src/dev/` ou atrás de `BuildConfig.ENV == "dev"`, `core/memory`, `feature/config`)
- Pré-requisitos: Nenhum. Aceita o [ADR-019](../../../produto/adrs/ADR-019-ferramentas-dev.md).

## Gate de autorização

Este plano é exclusivamente documental. A implementação só começa após aprovação explícita que identifique este arquivo:

> Aprovo o plano `docs/android/plans/a23-editor-memoria-dev.md`. Implemente o plano aprovado.

Aprovado pelo dono em 29/09/2026 com essa frase. O ADR-019 passa a `Aceito`.

## Objetivo

No flavor dev, o dono vê exatamente o perfil e a memória que vão para a IA, edita e salva. Sem apagar nada.

## Escopo de implementação

### 1. Entrada

- Config (só dev): última linha, depois do card de aviso, `Memória da IA (dev)` com chevron. No `prod` não existe classe, rota nem string.
- Para a captura `cfg` bater com o gold, a linha some quando a propriedade `debug.nutri.hide_dev_tools` = `1` (`adb shell setprop`), ligada pelo `tools/capture-config.sh` (ADR-019, consequência negativa).

### 2. Tela (ferramenta, sem gold)

- Tela cheia, tema e tokens do app, sem scroll horizontal (texto com quebra, `fillMaxWidth`).
- Topo: voltar + título `Memória da IA` + `Salvar`.
- Duas áreas empilhadas, cada uma um `TextField` multilinha grande, fonte monoespaçada 13 sp:
  1. **Perfil** — o bloco `PROFILE` exatamente como o `PromptBuilder` monta, em linhas `chave=valor`:
     ```text
     teto_kcal=2230
     proteina_g=167
     carbo_g=223
     gordura_g=74
     compensacao=zero | partial 50% | full
     refeicao.1=Café 07:30
     refeicao.2=Lanche 11:00
     ```
  2. **Memória** — o texto do `MemoryStore`, uma linha por fato, e o contador `{n}/4000`.
- Um rodapé somente leitura com o `DAY` do momento (o que iria no próximo turno), para conferência.

### 3. Salvar

- Memória: `MemoryStore.replace(text)` novo, com a mesma cifra (AES-GCM) e gravação atômica do A8b e o corte em 4000 chars. Texto vazio é recusado ("sem delete"): `Memória vazia não é salva.`
- Perfil: parse `chave=valor`, com as mesmas validações da Config. Pode mudar valores, nome e horário de refeição existente. **Não** pode adicionar nem remover refeição, nem remover chave (isso seria delete): erro com o número da linha, e nada é salvo.
- Mudou o teto → o mesmo diálogo de wipe da Config (regra 5 da memoria-push). Cancelar lá = nada salvo.
- Sucesso: `Salvo.` e volta para a Config.

### 4. Telemetria

- Evento `dev_memory_saved` com `memory_len_bucket` e `profile_changed: bool`. Nunca o texto.

## Arquivos e áreas afetadas

- `feature/devtools/` (novo, só compilado/roteado no dev), `core/memory/MemoryStore.kt` (`replace`), `feature/config/ConfigScreen.kt` (linha dev), navegação.
- `tools/capture-config.sh` (`setprop`).

## Validação planejada

1. `testDevDebugUnitTest`: parse/serialize do perfil (ida e volta sem perda); remover chave → erro; mudar teto → pede wipe; memória vazia recusada; `replace` mantém cifra.
2. `assembleProdRelease` com `prod.*` do dono ou inspeção do `apkanalyzer`: nenhuma classe `devtools` no prod (se não houver `prod.*` local, registrar como pendente).
3. Emulador: editar uma linha de memória, salvar, mandar mensagem no Chat e ver a linha nova no log do server (`pull-conversations.ps1`).
4. `capture-config.sh` continua batendo com `cfg`.

## Fora de escopo

- Gold/Stitch (ADR-019). Apagar memória ou refeição.

## Riscos e controles

- **Perfil inválido quebrar o app:** parse com validação completa antes de gravar; nada parcial.
- **Vazar para prod:** código no source set `dev` + checagem do APK.

## Critérios de aceite

- A memória na tela é byte a byte o `memory` do próximo POST; o perfil tem os mesmos valores do `profile` do próximo POST (formato legível, não o JSON).
- Editar e salvar funciona; apagar não.

## Encerramento

Registre resultados e aplique o ciclo de vida de `docs/sdd/README.md`, com a entrega git (§ 6).

## Resultado (29/09/2026)

### Implementação

- Hooks de flavor em `app/src/{dev,prod}/java/com/nutri/android/flavor/FlavorHooks.kt`: `flavorDestinations(nav)` e `FlavorConfigRows(nav)`. No `prod` os dois são vazios; o `main` só conhece o slot `extra` do `ConfigScreen` e a chamada dos hooks.
- `app/src/dev/java/com/nutri/android/feature/devtools/`: `ProfileText` (formato/parse `chave=valor`), `DevMemoryViewModel`, `DevMemoryScreen` (rota `RouteDevMemory`, linha `Memória da IA (dev)`).
- Perfil e `DAY` vêm do próprio `PromptBuilder.build(...)`: a tela mostra o que o próximo turno manda. O `DAY` usa a mesma regra de serialização do POST (campos com valor padrão não aparecem).
- `teto_kcal` é o teto efetivo do POST (base + crédito do treino). Mudou → diálogo de wipe da Config (`WipeDialog`, agora `internal`); confirmado, grava a base = teto − crédito (com a compensação editada) só no teto de hoje (`same`, dia útil/fim de semana ou o dia da semana). Teto ≤ crédito é recusado. Teto igual não mexe na base.
- `MemoryStore.replace(text)`: mesmas regras de linha do `append` (espaços colapsados, ≤ 240 chars por linha), corte em 4000 (saem as mais velhas), mesma cifra e gravação atômica. Sem linha → `false`, nada gravado.
- Erros: `Linha {n}: …` para linha inválida, chave repetida/desconhecida ou refeição nova; `Falta {chave}: remover não é permitido.` para chave ou refeição removida.
- Telemetria `dev_memory_saved` (constante só no dev): `memory_len_bucket` (`<1000`, `1000-2999`, `>=3000`) e `profile_changed`. Sem texto.
- Escolha da consequência negativa do ADR-019: flag de QA. A linha some com `debug.nutri.hide_dev_tools=1`, ligada no início do `tools/capture-config.sh` e desligada no `EXIT`; o script confere que a linha não aparece antes da captura `cfg`.

### Validação executada

1. `testDevDebugUnitTest`: 244 testes, 242 passam. Novos, todos verdes: `ProfileTextTest` (6: formato, ida e volta, edição com ids, chave removida, refeição adicionada, erros com linha), `DevMemoryViewModelTest` (7: igual ao POST, memória vazia recusada, chave removida recusada, edição sem wipe, teto → wipe e Cancelar não grava, Confirmar grava base e limpa hoje, dia útil/fim de semana), `MemoryStoreTest` (+2) e `AtomicMemoryFileTest.storeReplace_keepsCipherAndAtomicFile`. As 2 falhas são `StitchGoldTest.o3_dark`/`o3_light`, que falham igual na `origin/master` sem este plano (vêm do A21); fora do escopo.
2. `verifyRoborazziDevDebug`: mesmo resultado (só o `o3` da master). `assembleDevRelease` ok. Sem `prod.*` no `local.properties` do dono: `assembleProdRelease` rodou com valores provisórios só no worktree (removidos depois; APK apagado, nunca distribuído). Varredura dos `classes*.dex` do prod: `devtools`, `DevMemory`, `RouteDevMemory`, `Memória da IA`, `dev_memory_saved`, `hide_dev_tools` = 0 ocorrências (no dev: presentes). No prod fica só o `FlavorHooks` vazio.
3. Emulador próprio (`Small_Phone`, `emulator-5580`, `-read-only`; o `emulator-5554` do outro agente não foi usado): Config mostra a linha no fim; `Salvar` com memória vazia mostra `Memória vazia não é salva.`; digitar `Nao come gluten a23-teste-editor` e salvar → `Salvo.` e volta à Config; reabrir mostra a linha. Mensagem no Chat → `tools/pull-conversations.ps1 -RequestId 99ec8bc6-c1b3-4dc3-be7c-d3638b9f974b` mostra `MEMORY: Nao come gluten a23-teste-editor` no prompt do server de dev; a resposta cita o glúten. Tela conferida nos temas escuro e claro (ferramenta, sem gold).
4. `ADB=<emulator-5580> tools/capture-config.sh dark`: `✓ dev row hidden`, `cfg` capturado e `node tools/diff-gold.mjs dark/cfg` = 0,40 % (limite 2 %). Duas checagens do script falham (`empty workout: Nenhum informado`, `workout null`) e falham igual com o APK da `origin/master`: não é do A23. As capturas não foram versionadas neste plano (a tela nova não tem gold).

### Merge com o A24 (29/09/2026, autorizado pelo dono)

- Conflitos de texto resolvidos em `tools/capture-config.sh` (mantidos o `setprop` do A23 e o `|| exit 1` do A24), `docs/android/README.md` e `memoria-push.md` (estados dos dois planos). Matriz de `docs/README.md` corrigida: A24 pendente de validação manual.
- Ajuste ao A24: o perfil mostra só as refeições de hoje (`slotsOn`), mas `saveSlots` substitui a semana inteira. O editor agora aplica nome e horário editados sobre todas as refeições gravadas, mantendo `days` e o `slotMode`; nenhuma refeição de outro dia é apagada. Teste `slotsByWeekday_editTodays_keepTheRestOfTheWeek`.
- A `origin/master` do A24 (a0b1ba1) não compilava: `ConfigScreen.kt` usa `remember` sem o import (conferido num worktree limpo). O import volta nesta entrega.
- Depois do merge: `testDevDebugUnitTest`, `verifyRoborazziDevDebug` e `assembleDevRelease` verdes, sem falhas (o `o3` também passa). Varredura do prod repetida: zero ocorrências de `devtools`, `DevMemory`, `Memória da IA`, `dev_memory_saved`, `hide_dev_tools`.

### Pendências fora do escopo

- `capture-config.sh`: apagar o treino no sheet grava 0 em vez de nulo (editor do A22; falha igual na master antes do A23).
