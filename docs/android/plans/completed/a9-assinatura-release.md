# Plano — A9 Chave de assinatura própria para o release

- Estado: Concluído (28/09/2026). Backup da chave confirmado pelo dono.
- Data: 28/09/2026
- Contexto proprietário: `android`
- Código afetado: `apps/android/app/build.gradle.kts`, `.gitignore`, docs. Arquivos locais (não versionados): `nutri-release.jks`, `key.properties` na raiz do repo.
- Pré-requisitos: Nenhum.

## Gate de autorização

Este plano é exclusivamente documental. A implementação só começa após aprovação explícita que identifique este arquivo:

> Aprovo o plano `docs/android/plans/a9-assinatura-release.md`. Implemente o plano aprovado.

Se a implementação revelar decisão não coberta, pare, atualize os artefatos e peça nova aprovação.

## Objetivo

`assembleRelease` gera um APK assinado com uma chave própria do Nutri, e não com a chave de debug do PC. A chave e as senhas ficam na raiz do repo, fora do git, para o dono fazer a cópia segura.

## Fontes de verdade

- `AGENTS.md` (nada de segredo no git).
- `apps/android/app/build.gradle.kts` (hoje o `release` não tem `signingConfig`).

## Decisões fixas

| Item | Valor |
|---|---|
| Keystore | `nutri-release.jks` (PKCS12) na raiz do repo |
| Propriedades | `key.properties` na raiz: `storeFile`, `storePassword`, `keyAlias`, `keyPassword` |
| Alias | `nutri` |
| Algoritmo | RSA 4096, validade 10 000 dias (~27 anos) |
| DN | `CN=Nutri, O=Nicolas Teixeira, C=BR` |
| Senha | 32 chars aleatórios (CSPRNG), mesma para store e key (exigência do PKCS12). Gerada por script e nunca impressa |
| Sem `key.properties` | o release sai **sem assinatura** (`app-release-unsigned.apk`), com um aviso no build. Nunca cai na chave de debug |

## Escopo de implementação

### 1. Gerar a chave (AGENTE)

1. Script PowerShell único: gera a senha, roda o `keytool -genkeypair` com os valores acima e escreve `key.properties` com `storeFile=../../nutri-release.jks` (relativo a `apps/android/`). Nada é ecoado.
2. Se `nutri-release.jks` ou `key.properties` já existirem, **parar** sem sobrescrever.
3. Registrar neste plano só o SHA-256 do certificado (`keytool -list -v`), que é público e serve depois para o Firebase.

### 2. `.gitignore` (AGENTE)

- Adicionar `key.properties` e `*.keystore`. O `*.jks` já existe.
- Confirmar com `git check-ignore -v nutri-release.jks key.properties`.

### 3. Gradle (AGENTE)

- Em `apps/android/app/build.gradle.kts`: ler `key.properties` de `rootProject.file("../../key.properties")`.
- Se existir: `signingConfigs.create("release")` com os quatro valores, e `buildTypes.release.signingConfig` = ele.
- Se não existir: `logger.warn` explicando o release não assinado.
- `isMinifyEnabled` continua `false` (R8 fora de escopo).

### 4. Docs (AGENTE)

- `docs/android/README.md`: seção curta "Release" com o comando, onde ficam a chave e o aviso de backup.
- `SETUP.md`: 3 linhas sobre `key.properties`.

### 5. Backup e instalação (DONO)

1. Copiar `nutri-release.jks` **e** `key.properties` para um lugar seguro fora do PC, como um gerenciador de senhas com anexo ou um drive pessoal cifrado. Sem essa chave, nenhuma atualização futura instala por cima.
2. Não instalar o APK do A9 no celular. A troca de assinatura exige desinstalar o app atual e apaga os dados locais, e o A10 vai trocar o app de novo (ID `.dev`). Melhor desinstalar uma vez só, no A10.

## Arquivos e áreas afetadas

- `apps/android/app/build.gradle.kts`, `.gitignore`, `docs/android/README.md`, `SETUP.md`.
- Locais: `nutri-release.jks`, `key.properties`.

## Validação planejada

1. `git check-ignore` confirma os dois arquivos; `git status` não os lista.
2. `./gradlew.bat :app:assembleRelease` gera `app-release.apk`.
3. `apksigner verify --print-certs app-release.apk` mostra `CN=Nutri, O=Nicolas Teixeira, C=BR` e o mesmo SHA-256 registrado no passo 1.3.
4. Com `key.properties` renomeado temporariamente: o build gera `app-release-unsigned.apk` e mostra o aviso. Depois o nome volta ao original.
5. `./gradlew.bat :app:testDebugUnitTest` verde (nada de lógica mudou).

## Fora de escopo

- Flavors ([A10](../completed/a10-flavors-dev-prod.md)), Firebase ([A11](../pending_manual_validation/a11-firebase-dev.md)).
- R8/minify, AAB, Play Console, Play App Signing.
- Incremento de `versionCode`.

## Riscos e controles

- **Perder a chave:** o backup do passo 5.1 é obrigatório e fica registrado como validação manual.
- **Senha vazar em log:** o script não imprime a senha, e o Gradle não loga os valores.
- **Sobrescrever uma chave existente:** o script para se algum dos arquivos já existir.

## Critérios de aceite

- APK release assinado com `CN=Nutri`, SHA-256 registrado.
- Chave e propriedades na raiz, ignoradas pelo git.
- Dono confirma o backup.

## Registro de execução

- 28/09/2026 — `nutri-release.jks` (PKCS12, RSA 4096, SHA384withRSA, válido até 13/02/2054) e `key.properties` gerados na raiz. Senha de 32 chars de um alfabeto sem caracteres ambíguos (CSPRNG), passada ao `keytool` por variável de ambiente e nunca impressa.
  - Primeira tentativa abortada antes de criar qualquer arquivo (`ErrorActionPreference=Stop` tratou o stderr do keytool como erro). Conferido que nada ficou em disco antes de refazer.
- Certificado: `CN=Nutri, O=Nicolas Teixeira, C=BR`.
  - SHA-1 `5A:C7:78:1F:D3:21:63:53:63:E6:7F:5F:D9:3F:56:45:7C:B6:53:2F`
  - SHA-256 `C9:97:8F:23:53:3C:F1:AD:91:C3:A7:EA:AE:57:A7:D7:88:E3:92:B9:50:D0:0F:9B:D8:A0:85:3F:7B:2B:8A:1D`
- `.gitignore`: `*.keystore`, `key.properties` (o `*.jks` já existia).
- `build.gradle.kts`: `signingConfigs.release` só se `key.properties` existir; `release.signingConfig = findByName("release")`.
- **Desvio necessário:** o release nunca tinha compilado. Os `@Preview` de `core/designsystem/ExpressiveButtonGroup.kt` dependiam de `ui-tooling-preview`, que só chegava ao debug via `debugImplementation(ui-tooling)`. Adicionado `implementation(libs.compose.ui.tooling.preview)` (entrada nova no `libs.versions.toml`, versão pelo BOM). É só uma anotação, e nenhum código mudou.

### Resultados da validação

1. `git check-ignore` → `nutri-release.jks` (`*.jks`) e `key.properties`; `git status` não lista nenhum dos dois. ✅
2. `assembleRelease` → `app-release.apk`. ✅
3. `apksigner verify --print-certs` (build-tools 36.0.0): `Verifies`, esquema v2, DN `CN=Nutri, O=Nicolas Teixeira, C=BR`, SHA-256 `c9978f23…2b8a1d`, igual ao do keytool. ✅
4. Sem `key.properties`: aviso `release APK will be unsigned` e `app-release-unsigned.apk`. Arquivo restaurado, APK assinado regerado. ✅
5. `testDebugUnitTest` → 186 testes, 0 falhas. ✅
6. Manual: backup da chave pelo dono. ✅ (confirmado em 28/09/2026)

### Aprovação manual (28/09/2026)

Frase do dono: "Pode completar tambem o A9 tbm." Backup de `nutri-release.jks` + `key.properties` dado como feito.

## Encerramento

Depois da implementação, registre resultados reais e aplique o ciclo de vida em `docs/sdd/README.md`. Fica em `pending_manual_validation/` até o dono confirmar o backup.

Só declaração explícita do dono cancelando este plano permite `Cancelado` e a pasta `plans/cancelled/`.
