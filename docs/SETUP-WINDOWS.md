# SETUP Windows

Medido neste PC em 2026-09-24. Nenhum valor de `.env`.

## Saída bruta

Primeira passagem, antes do JDK e do PATH: `node -v` = `v24.16.0`, `npm -v` = `11.13.0`, `gh --version` = `gh version 2.101.0 (2026-09-15)`, `git --version` = `git version 2.55.0.windows.5`. `where.exe flutter` achou `C:\Users\Nicolas\fvm\default\bin\flutter` e `flutter.bat`. `echo` de `JAVA_HOME`, `ANDROID_HOME` e `ANDROID_SDK_ROOT` veio vazio. `where.exe java` e `where.exe adb` saíram com código 1: os dois comandos não estavam no PATH.

Depois do Temurin 21, das variáveis de usuário e dos atalhos em `%USERPROFILE%\.local\bin`, a mesma lista:

```
===== node -v =====
v24.16.0
EXIT:0

===== npm -v =====
11.13.0
EXIT:0

===== java -version =====
openjdk version "21.0.12.1" 2026-08-18 LTS
OpenJDK Runtime Environment Temurin-21.0.12.1+1 (build 21.0.12.1+1-LTS)
OpenJDK 64-Bit Server VM Temurin-21.0.12.1+1 (build 21.0.12.1+1-LTS, mixed mode, sharing)
EXIT:0

===== echo JAVA_HOME =====
JAVA_HOME=
EXIT:0

===== echo ANDROID_HOME =====
ANDROID_HOME=
EXIT:0

===== echo ANDROID_SDK_ROOT =====
ANDROID_SDK_ROOT=
EXIT:0

===== adb version =====
Android Debug Bridge version 1.0.41
Version 35.0.2-12147458
Installed as C:\Users\Nicolas\AppData\Local\Android\Sdk\platform-tools\adb.exe
Running on Windows 10.0.26200
EXIT:0

===== adb devices =====
List of devices attached
emulator-5554	device

EXIT:0

===== gh --version =====
gh version 2.101.0 (2026-09-15)
https://github.com/cli/cli/releases/tag/v2.101.0
EXIT:0

===== git --version =====
git version 2.55.0.windows.5
EXIT:0

===== where.exe java =====
C:\Users\Nicolas\.local\bin\java.cmd
EXIT:0

===== where.exe adb =====
C:\Users\Nicolas\.local\bin\adb.cmd
EXIT:0

===== where.exe flutter =====
C:\Users\Nicolas\fvm\default\bin\flutter
C:\Users\Nicolas\fvm\default\bin\flutter.bat
EXIT:0
```

O `echo` acima mostra o processo desta sessão, que não relê o registro. Os valores persistidos estão na tabela.

## O que foi gravado

`java` não existia no PATH (não era o JDK 8; estava ausente). Comando:

```
winget install --id EclipseAdoptium.Temurin.21.JDK -e --accept-package-agreements --accept-source-agreements --disable-interactivity
```

O instalador concluiu o Eclipse Temurin JDK 21.0.12.101 e gravou `JAVA_HOME` de máquina em `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\` (com barra no fim). O binário também entrou no PATH de máquina.

Em seguida, no ambiente do usuário:

```powershell
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot", "User")
[Environment]::SetEnvironmentVariable("ANDROID_HOME", "C:\Users\Nicolas\AppData\Local\Android\Sdk", "User")
[Environment]::SetEnvironmentVariable("ANDROID_SDK_ROOT", "C:\Users\Nicolas\AppData\Local\Android\Sdk", "User")
```

O PATH do usuário ganhou, no fim:

```
C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin
C:\Users\Nicolas\AppData\Local\Android\Sdk\platform-tools
```

O SDK em `%LOCALAPPDATA%\Android\Sdk` já existia. Não foi criado outro.

Esta sessão não herda variável gravada depois que ela abriu. Para `java` e `adb` resolverem aqui, ficaram dois atalhos que chamam o executável real:

- `C:\Users\Nicolas\.local\bin\java.cmd`
- `C:\Users\Nicolas\.local\bin\adb.cmd`

Um terminal novo vê `JAVA_HOME`, `ANDROID_HOME`, `ANDROID_SDK_ROOT` e o `java.exe` do Temurin pelo PATH de máquina.

O Android Studio tem um JBR 21.0.6 em `C:\Program Files\Android\Android Studio\jbr`. Ele não estava no PATH. O `JAVA_HOME` do usuário aponta para o Temurin 21, não para o JBR.

## Tabela

| ferramenta | versão | path | ok\|falta |
| --- | --- | --- | --- |
| node | v24.16.0 | C:\Program Files\nodejs\node.exe | ok |
| npm | 11.13.0 | C:\Program Files\nodejs\npm.cmd | ok |
| java | 21.0.12.1 LTS Temurin | C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot | ok |
| JAVA_HOME | 21 | User: C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot | ok |
| ANDROID_HOME | SDK presente | C:\Users\Nicolas\AppData\Local\Android\Sdk | ok |
| ANDROID_SDK_ROOT | igual ao ANDROID_HOME | C:\Users\Nicolas\AppData\Local\Android\Sdk | ok |
| adb | 1.0.41 / 35.0.2-12147458 | C:\Users\Nicolas\AppData\Local\Android\Sdk\platform-tools\adb.exe | ok |
| device | emulator-5554 | state device | ok |
| gh | 2.101.0 (2026-09-15) | C:\Program Files\GitHub CLI\gh.exe | ok |
| git | 2.55.0.windows.5 | C:\Users\Nicolas\AppData\Local\grok\git\2.55.0.windows.5\cmd\git.exe | ok |
| flutter | legado, já no PATH | C:\Users\Nicolas\fvm\default\bin\flutter.bat | ok |
| platform-tools | 35.0.2 | platform-tools | ok |
| platforms;android-35 | 2 | platforms\android-35 | ok |
| platforms;android-36 | 2 | platforms\android-36 | ok |
| build-tools | 34.0.0, 35.0.1, 36.0.0 | build-tools\34.0.0, build-tools\35.0.1, build-tools\36.0.0 | ok |

## SDK já instalado

`sdkmanager` está em `C:\Users\Nicolas\AppData\Local\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat`. Nada novo foi baixado. Nenhuma system-image nova.

O comando `--list_installed` avisou que o sdkmanager está depreciado. Pacotes que importam para o client:

```
  build-tools;34.0.0                                    | 34.0.0        | Android SDK Build-Tools 34                     | build-tools\34.0.0
  build-tools;35.0.1                                    | 35.0.1        | Android SDK Build-Tools 35.0.1                 | build-tools\35.0.1
  build-tools;36.0.0                                    | 36.0.0        | Android SDK Build-Tools 36                     | build-tools\36.0.0
  platform-tools                                        | 35.0.2        | Android SDK Platform-Tools                     | platform-tools
  platforms;android-35                                  | 2             | Android SDK Platform 35                        | platforms\android-35
  platforms;android-36                                  | 2             | Android SDK Platform 36                        | platforms\android-36
```

Também já estavam instalados, e foram deixados como estão: platforms android-31, android-33 e android-34, emulator 35.4.9, e a system-image `system-images;android-36;google_apis_playstore;x86_64`.

## Comandos que o dono ainda precisa rodar

Nenhum.

`adb devices` lista `emulator-5554` com state `device`. Não foi criado emulador por script.

## Como chamar o emulador

```
adb exec-out screencap -p > docs/qa/<tela>.png
```

No PowerShell, `>` grava o PNG como texto UTF-16. Para o arquivo sair binário:

```
cmd /c "adb exec-out screencap -p > docs\qa\<tela>.png"
```

## Clients

Os clients leem só `API_PUBLIC_URL` + `INVITE_CODE`. Nunca `OPENAI_API_KEY`.

## O que não foi instalado

Expo CLI, EAS CLI, Kotlin CLI avulso, CocoaPods, Xcode, Firebase, Genymotion, stack extra de Chocolatey, JDK 8 e JDK 24. Node e npm não foram rebaixados. A auth do `gh` não foi reconfigurada. Flutter não foi instalado nem acrescentado ao PATH.
