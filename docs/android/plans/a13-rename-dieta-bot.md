# Plano — A13 Rename visível "Nutri" → "Dieta Bot"

- Estado: Aguardando aprovação
- Data: 28/09/2026
- Contexto proprietário: `android` (também toca docs, skills e `AGENTS.md`)
- Código afetado: `apps/android/`, `tools/` (comentários e textos esperados), `AGENTS.md`, `SETUP.md`, `docs/` vivos, skills (`.agents`, `.grok`, `.hermes`), golds `docs/qa/stitch/`. `server/` **não** (é o [S7](../../server/plans/s7-rename-prompt.md)).
- Pré-requisitos: [A12](completed/a12-remover-legado-t123.md) concluído (menos arquivos para renomear). [ADR-016](../../produto/adrs/ADR-016-nome-dieta-bot.md) aceito junto com a aprovação.

## Gate de autorização

Este plano é exclusivamente documental. A implementação só começa após aprovação explícita que identifique este arquivo:

> Aprovo o plano `docs/android/plans/a13-rename-dieta-bot.md`. Implemente o plano aprovado.

A aprovação aceita o ADR-016. Se a implementação revelar decisão não coberta, pare, atualize os artefatos e peça nova aprovação.

## Objetivo

O app e a documentação viva passam a se chamar "Dieta Bot". Os IDs técnicos continuam `nutri` (ADR-016). Nenhum dado do usuário se perde, e o app atualiza por cima do instalado.

## Regra de corte (ADR-016)

| Muda | Fica |
|---|---|
| texto visível, docs vivas, skills, nomes de classe/arquivo/estilo Kotlin | `com.nutri.android(.dev)`, `nutri.db`, `nutri-bot-dev`, `nutri-api`, `/opt/nutri`, loggers `nutri`, projeto Stitch `Nutri` (ID), `nutri-release.jks`, extras/actions `com.nutri.*`, `nutri_tela`, prefs `nutri_*` |

## Divisão de trabalho

### Fase 0 — Golds do Stitch (AGENTE via MCP `stitch`, antes do código)

A regra de QA visual exige o gold antes da UI. O nome aparece em **12 telas × 2 temas**: `splash` (wordmark), `o1`–`o4` (cabeçalho "Nutri") e `chat0`, `chatL`, `chatE`, `chatT`, `chatP`, `chatF`, `chatG` ("Chat Nutri", "Nutri AI").

MCP `stitch` configurado em 28/09/2026 no `.mcp.json`: endpoint oficial `https://stitch.googleapis.com/mcp`, header `X-Goog-Api-Key: ${STITCH_API_KEY}` (a chave fica na variável de ambiente do dono, nunca no arquivo). Ferramentas: `list_screens`, `get_screen`, `edit_screens`, `generate_variants`…

1. **[AGENTE]** `list_screens` no projeto `6282733070135794645` e, para as 24 telas, `edit_screens` com um pedido fechado: "Troque somente o texto 'Nutri' por 'Dieta Bot' ('Chat Nutri' → 'Chat Dieta Bot', 'Nutri AI' → 'Dieta Bot AI'). Não altere layout, cores, tipografia, espaçamento nem outros textos."
   - A splash vai ser redesenhada com o logo novo em outro plano (marca). Aqui só troca o texto.
2. **[AGENTE]** `node tools/export-stitch.mjs` + `node tools/check-stitch.mjs`. Se o `edit_screens` tiver gerado telas novas (IDs diferentes), atualizar os mapas `DARK_SCREENS`/`LIGHT_SCREENS`. Pixel diff gold antigo × novo: **só** as áreas do nome podem mudar. Se o Stitch mexer em outra coisa, refazer a tela ou parar e pedir ao dono.
3. Gate automático (bloqueante): o pixel diff do passo 2 limpo em todas as telas. **[DONO]** Revisa as 24 telas depois, como validação manual. Não bloqueia a execução em sequência (decisão do dono, 28/09/2026).

### Fase 1 — Texto visível (AGENTE)

- `res/values/strings.xml`: `app_name` "Dieta Bot"; `src/dev/res/values/strings.xml`: "Dieta Bot Dev".
- `NutriTokens.WORDMARK` → "Dieta Bot"; `OnboardingChrome.kt` ("Nutri"); `ChatScreen.kt` ("Chat Nutri", "Nutri AI" ×2).
- Varredura final de `"[^"]*Nutri[^"]*"` em `src/main` (as strings visíveis zeram).

### Fase 2 — Nomes no Kotlin (AGENTE)

| De | Para |
|---|---|
| `NutriApplication` (+ manifest `.NutriApplication`) | `DietaBotApplication` |
| `NutriTheme`, `NutriType`, `NutriMeasure`, `NutriShapes`, `NutriHex` e os arquivos `NutriTheme.kt`, `NutriType.kt`, `NutriTokens.kt` | `DietaBot*` |
| `NutriCta`, `NutriCtaGhost`, `NutriGroup`, `NutriComponents.kt` | `DietaBot*` |
| `NutriDatabase`, `NutriConverters` | `DietaBotDatabase`, `DietaBotConverters` |
| `NutriApi` | `DietaBotApi` |
| estilo `Theme.Nutri` (`themes.xml` ×4 qualifiers + manifest) | `Theme.DietaBot` |

- **Room:** o nome da classe entra no caminho do schema exportado. Mover `app/schemas/com.nutri.android.core.database.NutriDatabase/` → `…DietaBotDatabase/` (o conteúdo é o mesmo) e ajustar `SCHEMA_V1/V2` em `MigrationV1V2Test`. O arquivo `nutri.db`, a versão e o identity hash não mudam, então os dados no aparelho ficam.
- Rename por símbolo (classe + arquivo + usos + testes). O pacote `com.nutri.android` não muda.

### Fase 3 — Docs, skills e scripts (AGENTE)

- `AGENTS.md`: título "Dieta Bot — repo constitution"; onde fala do produto, "Dieta Bot". Uma linha nova: "Technical IDs stay `nutri` (ADR-016)".
- Docs vivas com "Nutri" (hoje: `docs/README.md`, `docs/HERMES.md`, `docs/sdd/README.md`, `docs/produto/README.md`, `docs/android/README.md`, `docs/server/README.md`, `docs/qa/README.md`, `SETUP.md` e specs em `docs/produto/specifications/`) → "Dieta Bot". Referências ao **projeto Stitch `Nutri`** ficam, porque são o nome/ID do projeto.
- **Não editar:** `docs/decisions/*`, ADRs aceitos, planos em `completed/` e `pending_manual_validation/` (histórico).
- Skills com "Nutri" (9 por pasta) → "Dieta Bot", nas três pastas, que continuam idênticas (`diff -r`).
- `tools/`: comentários e textos esperados pelos scripts de captura (se algum `expect` procura "Nutri"/"Chat Nutri").

### Fase 4 — Validação (AGENTE) e celular (DONO)

1. `git grep -w Nutri` fora das exceções da regra de corte, do histórico e do Stitch → zero. Registrar o que sobrou e por quê.
2. `:app:testDevDebugUnitTest` verde (inclui a `MigrationV1V2Test` com o schema movido); `verifyRoborazziDevDebug`: baseline da splash regravada se o wordmark mudou, registrando o motivo.
3. Emulador + fake: `capture-onboarding.sh` e `capture-chat.sh` (light e dark) sem ✗; `diff-gold` splash/o1–o4 contra os **golds novos** ✓; chat comparado com os golds novos, com a lista de diffs escrita.
4. **Upgrade sem perda:** instalar o APK atual, registrar uma refeição, instalar o APK do A13 por cima (`adb install -r`) e ver a refeição ainda lá.
5. `assembleDevRelease` → **[DONO]** instala por cima no celular: nome "Dieta Bot Dev" no launcher, dados mantidos.

## Fora de escopo

- `applicationId`, pacote Kotlin, Firebase, banco, VM, projeto Stitch (ADR-016).
- Logo, ícone e splash redesenhada (plano de marca separado, depois das imagens do dono).
- Prompt do server ([S7](../../server/plans/s7-rename-prompt.md)).

## Riscos e controles

- **Room perder o schema:** pasta movida + teste de migração no gate.
- **Gold e UI fora de sincronia:** a Fase 0 é bloqueante; sem golds novos, a Fase 1 não começa.
- **IA do Stitch alterar mais que o texto:** pixel diff por tela + revisão do dono; tela com diff fora da área do nome é refeita.
- **Rename pegar ID técnico:** a regra de corte está na tabela; a validação 1 lista as exceções.
- **Skills fora de sincronia:** `diff -r` nas três pastas.

## Critérios de aceite

- O usuário vê "Dieta Bot" em todo lugar; os IDs técnicos seguem `nutri`.
- Testes, migração, Roborazzi e gate visual verdes contra os golds novos.
- O app atualiza por cima sem perder dados.

## Encerramento

Depois da implementação, registre resultados reais e aplique o ciclo de vida em `docs/sdd/README.md`.

Só declaração explícita do dono cancelando este plano permite `Cancelado` e a pasta `plans/cancelled/`.
