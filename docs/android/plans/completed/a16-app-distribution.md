# Plano — A16 Firebase App Distribution (APK assinado, versão 0.0.N)

- Estado: Concluído (28/09/2026). Validação manual do dono confirmada (convite, App Tester, atualização no celular).
- Aprovação: 28/09/2026, `/goal` do dono ("Aprovo os planos … a15 … e … a16 …").
- Data: 28/09/2026
- Contexto proprietário: `android`
- Código afetado: `apps/android/app/build.gradle.kts` (versão), `apps/android/version.properties` (novo), `tools/distribute-dev.ps1` (novo), `AGENTS.md`, docs. Fora do repo: App Distribution no projeto Firebase `nutri-bot-dev`.
- Pré-requisitos: A9 (chave de release), A10 (flavors), A11 (Firebase `nutri-bot-dev`, app `1:823717355877:android:d01b29a0b20b0674bd818c`).

## Gate de autorização

Este plano é exclusivamente documental. A implementação só começa após aprovação explícita que identifique este arquivo:

> Aprovo o plano `docs/android/plans/a16-app-distribution.md`. Implemente o plano aprovado.

## Objetivo

Um comando gera o **APK dev release assinado** (chave do A9), sobe a versão sozinho e distribui pelo Firebase App Distribution para o dono (`nicolasteixeira3856@gmail.com`), que instala e atualiza pelo app **Firebase App Tester**, sem cabo.

## Decisões fixas

| Item | Valor |
|---|---|
| Artefato | APK `assembleDevRelease` (flavor dev, `com.nutri.android.dev`, assinado com `nutri-release.jks`) |
| Canal | Firebase App Distribution, projeto `nutri-bot-dev`, via `npx firebase-tools appdistribution:distribute` (login já feito) |
| Tester | `nicolasteixeira3856@gmail.com` (grupo `owner`) |
| Versão | `versionName` = `0.0.N` (+ sufixo `-dev` do flavor → `0.0.N-dev`), `versionCode` = `N` |
| Início | `0.0.1` (`versionCode` 1) |
| Fonte da versão | `apps/android/version.properties` (`VERSION_PATCH=N`), lido pelo Gradle; `1.0` fixo sai do `defaultConfig` |
| Bump | **automático** a cada distribuição: N → N+1, sem pedir confirmação. Nunca repete nem desce |
| Até | release 1 (`1.0.0`): a virada para `1.0.0` e a regra depois dela são um plano/ADR próprio |
| Registro | commit `chore(release): 0.0.N-dev` com o `version.properties` + tag git `dev-v0.0.N`; release notes = `git log --oneline` desde a tag anterior |

## Escopo de implementação

### 1. Versão no Gradle

- `version.properties` na pasta `apps/android/` com `VERSION_PATCH=1`.
- `build.gradle.kts`: `versionCode = VERSION_PATCH`, `versionName = "0.0.$VERSION_PATCH"`; falha clara se o arquivo faltar ou não for inteiro.

### 2. `tools/distribute-dev.ps1`

1. Recusa rodar com a árvore git suja (a distribuição tem que corresponder a um commit) ou sem `key.properties`/`google-services.json`.
2. Lê N, grava N+1 (na primeira execução, publica a `0.0.1` já gravada).
3. `testDevDebugUnitTest` (falhou → para, sem subir versão).
4. `assembleDevRelease`; confere com `apksigner` o certificado `CN=Nutri` (A9) e com `aapt` o `versionName`/`versionCode`.
5. `npx -y firebase-tools@latest appdistribution:distribute <apk> --app 1:823717355877:android:d01b29a0b20b0674bd818c --groups owner --release-notes "<log>"`.
6. Commit `chore(release): 0.0.N-dev` + tag `dev-v0.0.N` + push (commit e tag).
7. Imprime a versão distribuída e o link do console.
- Opção `-DryRun`: faz tudo menos distribuir, commitar e pushar (para validar o script).

### 3. Firebase (uma vez)

- **[AGENTE]** Criar o grupo `owner` com `nicolasteixeira3856@gmail.com` (`appdistribution:group:create` + `appdistribution:testers:add`).
- **[DONO]** Se o console pedir, ativar o App Distribution no projeto `nutri-bot-dev` (Release & Monitor → App Distribution → Get started).
- **[DONO]** Aceitar o convite que chega no e-mail e instalar o **Firebase App Tester** no celular.

### 4. Regra permanente (`AGENTS.md` e skill de QA)

- "Quando o dono pedir um build/deploy de teste: rodar `tools/distribute-dev.ps1`. A versão sobe sozinha (0.0.N → 0.0.N+1); nunca editar `version.properties` à mão; nunca reutilizar um número."
- Docs: `docs/android/README.md` (seção Distribuição), `SETUP.md` (App Tester).

## Validação planejada

1. `-DryRun`: APK gerado com `versionName 0.0.1-dev`, `versionCode 1`, assinado `CN=Nutri`; árvore limpa depois.
2. Primeira distribuição real: `0.0.1-dev` aparece no console e no App Tester do dono; commit + tag `dev-v0.0.1` no remoto.
3. Segunda distribuição: `0.0.2-dev`, `versionCode` 2, instala por cima da 0.0.1 **sem perder dados**.
4. Manual (dono): convite aceito, app instalado pelo App Tester.

## Fora de escopo

- Flavor prod, Play Store, AAB, CI (GitHub Actions), grupos além do `owner`.
- A versão `1.0.0` e a política de versões depois dela.

## Riscos e controles

- **Downgrade de `versionCode`:** o app instalado hoje tem `versionCode 1`; a `0.0.1` também tem 1 (instala por cima, igual é permitido) e daí só sobe.
- **Distribuir código não commitado:** o script recusa a árvore suja.
- **Número de versão queimado:** se a distribuição falhar depois do bump, o script desfaz o bump local (sem commit/tag), e o número não é usado.

## Critérios de aceite

- Um comando distribui o APK assinado com versão nova, e o dono recebe no App Tester.
- Versões 0.0.1, 0.0.2… registradas em commits e tags.

## Resultados (28/09/2026)

### Implementado

- `apps/android/version.properties` (`VERSION_PATCH`) → `versionCode` N, `versionName` `0.0.N` (`-dev` do flavor). O Gradle falha com mensagem clara sem o arquivo ou com valor não inteiro (testado com `abc` e com o arquivo ausente). ✅
- `tools/distribute-dev.ps1` como no escopo. Detalhe do bump: se a tag `dev-v0.0.N` já existe, grava N+1; se não existe, publica N (é assim que a primeira execução sai `0.0.1`). Falha antes do commit restaura `version.properties`. Na primeira execução não há mudança de arquivo, então o commit `chore(release): 0.0.1-dev` é vazio (`--allow-empty`). Release notes vão por `--release-notes-file` (UTF-8 sem BOM).
- Firebase: grupo `owner` ("Owner") criado em `nutri-bot-dev` com `nicolasteixeira3856@gmail.com` (`appdistribution:group:create` + `appdistribution:testers:add`). O App Distribution já respondia pela API: não foi preciso ativar no console. ✅
- Regra permanente em `AGENTS.md` (How to work + "Do not" citando o A16), skill `dieta-bot-android-qa` nas 3 pastas (sincronizadas), `docs/android/README.md` (Distribuição) e `SETUP.md` (App Tester).
- Fora do previsto, necessário para o script: `StitchGoldTest` e `RoborazziSmokeTest` passaram a usar `@Config(application = Application::class)`, como os outros testes Robolectric. Com a `DietaBotApplication` real, o `PushSync` observava o Room entre testes e vazava `Illegal connection pointer` para o teste seguinte (`chatE_dark` falhou uma vez no primeiro `-DryRun`). Depois: 3 execuções seguidas 171 / 0 falhas / 0 pulados, números de gold idênticos.

### Validação

1. `-DryRun` com a árvore limpa: `app-dev-release.apk`, `CN=Nutri`, `versionName 0.0.1-dev`, `versionCode 1`; árvore limpa depois. Com uma tag local temporária `dev-v0.0.1`: `0.0.2-dev` / 2 e `version.properties` de volta a 1 (tag removida). Com o teste intermitente acima: parou em "testDevDebugUnitTest failed", sem build e sem bump. ✅
2. Primeira distribuição real: `0.0.1-dev (1)` enviada e distribuída ao grupo `owner`; commit `a304fa3 chore(release): 0.0.1-dev` e tag `dev-v0.0.1` no remoto. ✅
3. Segunda distribuição: `0.0.2-dev (2)`; `version.properties` 1 → 2 no commit `262bd0c chore(release): 0.0.2-dev`, tag `dev-v0.0.2` no remoto. ✅ Emulador: `0.0.1-dev` assinada instalada, onboarding feito, `adb install -r` da `0.0.2-dev` → `versionCode 2`, abre na Home com a meta e as refeições do onboarding (dados mantidos). ✅
4. Manual (dono): aceitar o convite do e-mail, instalar o **Firebase App Tester**, instalar a `0.0.2-dev` (ou a 0.0.1 e depois atualizar) e confirmar que o app sobe sem perder dados. ⏳ Obs.: o celular hoje tem o APK antigo `1.0` / `versionCode 1` com a mesma chave; a 0.0.2 (`versionCode 2`) instala por cima.

### Aprovação manual (28/09/2026)

Frase do dono: "Tudo certo, sobre a tinta do dark, mantem o token do agents. Pode completar todos os planos e dai realize um novo commit."
- Convite aceito, Firebase App Tester instalado e atualização no celular: confirmados. ✅

## Encerramento

Depois da implementação, registre resultados reais e aplique o ciclo de vida em `docs/sdd/README.md`.
