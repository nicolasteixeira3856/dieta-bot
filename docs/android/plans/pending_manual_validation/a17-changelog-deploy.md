# Plano — A17 Changelog humano no deploy de teste

- Estado: Pendente aprovação manual
- Aprovado: 29/09/2026 ("Aprovo o plano `docs/android/plans/a17-changelog-deploy.md`. Implemente o plano aprovado.")
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

## Resultados (29/09/2026)

- `tools/distribute-dev.ps1`: `-Notes` novo, validado no passo 0, antes da árvore limpa, do bump e do Gradle. Recusa arquivo ausente, vazio, hash (`[0-9a-f]{7,40}`, case-sensitive), prefixo convencional (`feat|fix|chore|docs|refactor|test|build|ci|perf|style`, com escopo opcional e `:`) e título `#`/`##` (o `##` é da seção de versão no `CHANGELOG.md`). Bloco do `git log` removido. Script continua ASCII (o PowerShell 5.1 lê `.ps1` sem BOM como ANSI); o travessão do título vem de `[char]0x2014`. Data em America/Sao_Paulo.
- `apps/android/CHANGELOG.md` criado só com o cabeçalho; as seções nascem a partir do próximo deploy.
- `AGENTS.md` e `docs/android/README.md` § Distribuição atualizados.

Validação:

1. `-DryRun` sem `-Notes`: testes + `assembleDevRelease` OK, `app-dev-release.apk: CN=Nutri, versionName 0.0.3-dev, versionCode 3`, exit 0, árvore limpa. OK. Com `-Notes` válido, mostra `Dieta Bot 0.0.3` + as notas (acentos preservados) e sai limpo.
2. Sem `-Notes` e sem `-DryRun`: `x -Notes <file.md> is required…`, exit 1, sem Gradle, `version.properties` com o mesmo hash. OK.
3. `-Notes` com `8bc25a5`: `x notes line 2 has a commit hash…` com a linha. OK. Também recusados: `- fix(server): …` (linha 1), arquivo vazio, arquivo ausente.
4. Pendente (manual): o próximo deploy pedido pelo dono. Conferir as notas no App Tester e a seção no `CHANGELOG.md` do commit `chore(release)`. O prepend foi exercitado isolado, numa cópia do `CHANGELOG.md`, para duas versões: a mais nova fica no topo.

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
