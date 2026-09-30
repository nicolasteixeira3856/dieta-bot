# ADR-019 — Telas de ferramenta só no flavor dev

- Estado: Aceito (29/09/2026, com a aprovação do A23)
- Data: 2026-09-29
- Contexto: `produto`
- Substitui: parcialmente o `AGENTS.md` ("Screens: ADR-012 … Nothing else", "No screenshot, UI is not done"), só para telas que existem apenas no flavor `dev`.

## Contexto

O dono quer ver e editar, pelo celular, a memória e o perfil que o app manda para a IA. É uma ferramenta de diagnóstico para os testes, não produto. O ADR-012 fecha a lista de telas e o `AGENTS.md` exige gold do Stitch para toda UI.

## Decisão

1. Uma tela que só existe no flavor `dev` (código em `app/src/dev/` ou atrás de `BuildConfig.ENV == "dev"`) é **ferramenta**, não tela de produto.
2. Ferramenta não entra na lista do ADR-012, não tem gold no Stitch e não passa pelo loop de captura vs gold.
3. Ferramenta usa o tema e os tokens do app (`AGENTS.md` § Tokens), sem scroll horizontal. O flavor `prod` não pode ter nenhuma classe, rota ou texto dela.
4. Ferramenta nunca apaga dados. Editar e salvar pode.
5. A primeira ferramenta é o editor de memória e perfil ([A23](../../android/plans/completed/a23-editor-memoria-dev.md)), aberto por uma linha extra na Config do dev.

## Motivação

Diagnóstico rápido nos testes sem abrir exceção no produto nem gastar o fluxo do Stitch.

## Consequências

### Positivas

- O dono corrige a memória que confundiu a IA sem reinstalar o app.

### Negativas

- A Config do dev fica diferente do gold `cfg` por uma linha. A captura de `cfg` para o gate visual continua sendo do flavor dev: o A23 esconde a linha quando o `capture-config.sh` roda (flag de QA), ou a comparação ignora aquela linha. O A23 escolhe e registra.

## Alternativas consideradas

- **Exigir gold no Stitch:** rejeitada pelo dono.

## Relações

- [ADR-012](ADR-012-chat-home-perfil.md), [ADR-014](../../android/adrs/ADR-014-flavors-firebase-dev.md).

Depois de aceito, este ADR não se edita.
