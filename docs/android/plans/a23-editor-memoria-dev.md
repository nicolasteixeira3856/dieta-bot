# Plano — A23 Editor de memória e perfil da IA (só dev)

- Estado: Aguardando aprovação
- Data: 29/09/2026
- Contexto proprietário: `android`
- Código afetado: `apps/android/` (`app/src/dev/` ou atrás de `BuildConfig.ENV == "dev"`, `core/memory`, `feature/config`)
- Pré-requisitos: Nenhum. Aceita o [ADR-019](../../produto/adrs/ADR-019-ferramentas-dev.md).

## Gate de autorização

Este plano é exclusivamente documental. A implementação só começa após aprovação explícita que identifique este arquivo:

> Aprovo o plano `docs/android/plans/a23-editor-memoria-dev.md`. Implemente o plano aprovado.

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
