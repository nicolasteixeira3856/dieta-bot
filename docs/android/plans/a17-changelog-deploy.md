# Plano — A17 Changelog humano no deploy de teste

- Estado: Aguardando aprovação
- Data: 29/09/2026
- Contexto proprietário: `android`
- Código afetado: `tools/distribute-dev.ps1`, `apps/android/CHANGELOG.md` (novo)
- Pré-requisitos: Nenhum.

## Gate de autorização

Este plano é exclusivamente documental. A implementação só começa após aprovação explícita que identifique este arquivo:

> Aprovo o plano `docs/android/plans/a17-changelog-deploy.md`. Implemente o plano aprovado.

## Objetivo

As notas que o tester vê no Firebase App Tester passam a ser um changelog escrito para gente, em pt-BR, e não o `git log --oneline`.

## Decisão do dono (29/09/2026)

"Quando o deploy for feito você deve gerar um changelog para um humano ler e não mais um git inline."

## Escopo de implementação

### 1. Script (`tools/distribute-dev.ps1`)

- Parâmetro novo `-Notes <arquivo .md>`, **obrigatório** fora do `-DryRun`. Sem ele, o script falha antes do build e não queima número.
- O script valida: arquivo existe, não está vazio, não tem hash de commit (`\b[0-9a-f]{7,40}\b`) nem prefixo convencional (`feat:`, `fix(…)`, `chore`). Achou: falha com a linha culpada.
- As notas enviadas ao Firebase são `Dieta Bot 0.0.N` + linha em branco + o conteúdo do arquivo.
- O mesmo texto entra no topo de `apps/android/CHANGELOG.md` (`## 0.0.N — DD/MM/AAAA`), no commit `chore(release)`.
- Sai o bloco que monta as notas com `git log`.

### 2. Como o agente escreve as notas (regra no `AGENTS.md`, já registrada)

- Lê os commits e os planos concluídos desde a tag `dev-v0.0.*` anterior.
- Escreve para o tester: o que ele nota no app, e não o que mudou no código. Seções opcionais `Novidades`, `Correções`, `Ajustes`, com um item por linha, curto e em tom seco.
- Nada de nome de arquivo, id de plano, hash ou termo técnico ("Room", "shaping", "ViewModel").
- Exemplo:

  ```markdown
  ### Correções
  - O botão "Gravar" volta a aparecer depois da estimativa.
  - A IA não responde mais "não deu para estimar" quando entendeu a mensagem.
  ```

- Arquivo temporário no scratchpad do agente; o conteúdo final fica no `CHANGELOG.md`.

### 3. Docs

- `docs/android/README.md` § Distribuição: `-Notes` e o `CHANGELOG.md`.

## Validação planejada

1. `./tools/distribute-dev.ps1 -DryRun` continua funcionando sem `-Notes`.
2. Sem `-Notes` e sem `-DryRun`: falha antes do Gradle, `version.properties` intacto.
3. `-Notes` com um hash dentro: falha com a linha.
4. Deploy real na próxima entrega pedida pelo dono: notas no App Tester legíveis e `CHANGELOG.md` no commit de release.

## Fora de escopo

- Notas de versão em loja (Play Console).
- Mudança no esquema de versão 0.0.N.

## Riscos e controles

- **Esquecer as notas:** o script falha antes do build.

## Critérios de aceite

- O App Tester mostra texto humano, sem hash.
- `apps/android/CHANGELOG.md` com uma seção por versão distribuída a partir daqui.

## Encerramento

Registre resultados e aplique o ciclo de vida de `docs/sdd/README.md`, com a entrega git (§ 6).
