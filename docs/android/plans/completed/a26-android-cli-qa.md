# Plano — A26 Android CLI no loop de QA visual

- Estado: Concluído
- Data: 29/09/2026
- Contexto proprietário: `android`
- Código afetado: nenhum código de produção. Skills (`.agents`, `.grok`, `.hermes`, `.claude/skills`), `docs/qa/README.md`, `docs/SETUP-WINDOWS.md`, `docs/android/README.md`, `AGENTS.md` (uma linha).
- Pré-requisitos: Nenhum. Android CLI `1.0.16457483` instalado pelo dono em `C:\Users\Nicolas\AppData\AndroidCLI\android.exe` (29/09/2026).

## Gate de autorização

Este plano é exclusivamente documental. A implementação só começa após aprovação explícita que identifique este arquivo:

> Aprovo o plano `docs/android/plans/a26-android-cli-qa.md`. Implemente o plano aprovado.

Se a implementação revelar decisão não coberta, pare, atualize os artefatos e peça nova aprovação.

## Objetivo

O agente usa o Android CLI (`android`) nas capturas avulsas e na lista de diffs do loop visual. A lista de diffs passa a ter números medidos (bounds e textos do `android layout`), não só a leitura da imagem. O gate de aprovação continua o mesmo: `tools/diff-gold.mjs` ≤ 2%.

## Descoberta (29/09/2026)

Conferido nesta sessão, com o emulador `emulator-5554` na Home (dark, 780×1688):

| Comando | Resultado |
|---|---|
| `android info` | SDK `C:\Users\Nicolas\AppData\Local\Android\Sdk` |
| `android emulator list` | `Medium_Phone`, `Small_Phone` |
| `android studio check` | Studio Quail 4 com `nutri-android` READY |
| `android screen capture -o <png>` | PNG 780×1688, igual ao `screencap` |
| `android screen capture --annotate -o <png>` | caixas numeradas por elemento (anel, macros, treino, timeline, FAB) |
| `android layout --flat -o <json>` | `resource-id` (= testTag), texto, `bounds`, `center` |
| `android skills list` | skills oficiais; as 4 pastas do projeto estão idênticas |

Limites encontrados:

- O PATH da sessão do agente pode não ter o `android` (instalado depois da sessão abrir). Fallback: caminho completo acima.
- O `layout` não traz cor, tamanho de fonte nem raio. Esses itens continuam medidos pela imagem vs gold.
- O JSON do `layout` é UTF-8. Lido como ANSI no PowerShell 5.1, vira `ConfiguraÃ§Ãµes`. Ler com `-Encoding utf8` ou pelo Bash/Node.
- `android studio render-compose-preview` exige `@Preview`, e o app não tem nenhum. Fica fora deste plano.
- Os `tools/capture-*.sh` já fazem `uiautomator dump` + `input tap` com asserções. O `android layout` faz o mesmo papel. Reescrever os 7 scripts não traz ganho e arrisca quebrar o QA que já passa. Eles ficam como estão.

## Fontes de verdade

- `AGENTS.md` § Visual QA e § Skills.
- [ADR-008](../../decisions/008-visual-qa.md) e [ADR-009](../../decisions/009-visual-match.md) (lei de pastas e match visual; não mudam).
- [docs/qa/README.md](../../qa/README.md) § Loop de Implementação Visual e § Gate.

## Escopo de implementação

### 1. Skills do projeto (as 4 pastas, conteúdo idêntico)

`dieta-bot-android-visual/SKILL.md` (English):

- Pré-check: `android info` ou `adb devices` com aparelho; geometria do gold (`wm size 780x1688`, `wm density 320`).
- Binário: `android`; se não estiver no PATH, `C:\Users\Nicolas\AppData\AndroidCLI\android.exe`.
- Captura avulsa: `android screen capture -o docs/qa/android/current/<theme>/<id>.png` no lugar do `cmd /c "adb exec-out screencap ..."`.
- Fluxos com interação: continuam os `tools/capture-*.sh` (lista em `docs/qa/README.md`).
- Lista de diffs com números: `android layout --flat -o <scratchpad>/<id>.json`. Conferir contra o gold, na mesma geometria (1 dp = 2 px):
  - posição e tamanho de cada `resource-id` da tela (ex.: `home-consumed`, `home-fab`, `home-summary-*`);
  - texto renderizado (pt-BR, registro consolidado `520 kcal · 28P · 52C · 22G`, chips zerados no dia 1);
  - elemento esperado que não aparece, ou `off-screen`.
- Cor, tamanho de fonte (remaining 34pt), raio (sheet 22, card/chip 14) e CTA: pela imagem vs gold, como hoje.
- Elemento sem testTag ou não achado no `layout`: `android screen capture --annotate` e ler o PNG antes de concluir.
- Gate: `node tools/diff-gold.mjs <theme>/<id>` ≤ 2%. O `layout` não aprova tela sozinho.
- Ler o JSON como UTF-8.

`dieta-bot-android-qa/SKILL.md`:

- Troca "`adb devices` has an emulator" por "`android info` (or `adb devices`) lists the emulator".
- Item novo: a lista de diffs cita os bounds medidos pelo `android layout` das partes que mudaram.

`dieta-bot-android-ui/SKILL.md` § Visual Verification:

- Passo 2 passa a `android screen capture -o ...`.
- Linha nova: dúvida de API Android/M3 → `android docs search "<termos>"` antes de chutar.

Depois de editar em `.claude/skills`, copiar para `.agents`, `.grok`, `.hermes`. As skills oficiais (`android-cli` e demais) não são editadas.

### 2. `docs/qa/README.md`

- Passo 3, captura manual: `android screen capture -o docs/qa/android/current/{theme}/<id>.png`, com o `adb exec-out screencap` como alternativa.
- Passo 5: a lista de diffs usa `android layout --flat` para posição, tamanho e texto; cor, tipo e raio pela imagem.
- Gate e Regressão: sem mudança.

### 3. `docs/SETUP-WINDOWS.md`

Seção nova "Android CLI":

- instalação (comando oficial do Windows), caminho `%USERPROFILE%\AppData\AndroidCLI\android.exe`, `android init`;
- conferência: `android --version`, `android info`, `android emulator list`, `android studio check`;
- skills oficiais: `android skills list` / `android skills add <id>`, copiadas nas 4 pastas do projeto;
- nota: sessão de agente aberta antes da instalação não vê o PATH novo.

### 4. `AGENTS.md`

Uma linha em § Skills: `Official Google skills from npx stay.` → `Official Google skills (npx or \`android skills\`) stay.`

### 5. Índices

- `docs/android/README.md`: linha de Visual QA cita `android screen capture` / `android layout`; A26 no índice de planos.
- `docs/README.md`: A26 na matriz e na lista de planos aguardando aprovação.

## Arquivos e áreas afetadas

- `.{agents,grok,hermes,claude}/skills/dieta-bot-android-visual/SKILL.md`
- `.{agents,grok,hermes,claude}/skills/dieta-bot-android-qa/SKILL.md`
- `.{agents,grok,hermes,claude}/skills/dieta-bot-android-ui/SKILL.md`
- `docs/qa/README.md`
- `docs/SETUP-WINDOWS.md`
- `AGENTS.md`
- `docs/android/README.md`, `docs/README.md`
- este plano (ciclo de vida)

## Validação planejada

1. `git diff --no-index --stat .agents/skills .{grok,hermes,claude}/skills` vazio: as 4 pastas idênticas.
2. `grep -rn "adb exec-out screencap"` nas skills do projeto: zero ocorrências como caminho principal.
3. Prova no emulador, seguindo a skill nova, sem script: Home dark com dados → `android screen capture -o docs/qa/android/current/dark/home1.png` → `node tools/diff-gold.mjs dark/home1` com o mesmo resultado do screencap anterior (PNG 780×1688, gate igual).
4. `android layout --flat` na mesma tela, lido como UTF-8: textos com acento corretos; lista de diffs de exemplo com bounds de `home-consumed`, `home-workout`, `home-summary-1`, `home-fab` registrada neste plano.
5. Nenhum arquivo em `apps/android/`, `server/` ou `tools/` alterado (`git diff --stat`).

Sem validação manual do dono: o plano vai direto para `completed/` se 1–5 passarem.

## Fora de escopo

- Reescrever `tools/capture-*.sh` para usar `android layout` no lugar de `uiautomator dump`.
- Adicionar `@Preview` no app ou usar `android studio render-compose-preview`.
- Mudar o gate (`diff-gold.mjs`, 2%), o `StitchGoldTest` ou o Roborazzi.
- Journeys (`android` journey tests) e `android run`/`install` no lugar do Gradle.
- `docs/SETUP-WINDOWS.md` linha 162 (`docs/qa/screencap-check.png` na raiz): registro histórico, não mexe.
- Editar as skills oficiais do Google.

## Riscos e controles

- **`android` fora do PATH do agente:** a skill traz o caminho completo como fallback.
- **Acentos quebrados no JSON:** a skill manda ler como UTF-8; a validação 4 confere.
- **Bounds viram gate informal:** a skill diz que o `layout` só alimenta a lista de diffs; aprovação é o `diff-gold.mjs`.
- **CLI muda de versão e quebra flag:** usar só `screen capture`, `--annotate`, `layout --flat -o`, `info`, `docs search`. Todas existem na `1.0.16457483`.

## Critérios de aceite

- As 3 skills do projeto usam `android screen capture` e `android layout` como descrito, iguais nas 4 pastas.
- `docs/qa/README.md` e `docs/SETUP-WINDOWS.md` documentam o CLI.
- A captura de `home1` dark feita com o CLI passa no `diff-gold.mjs` como a do screencap.
- Nenhum código de produção alterado.

## Resultado (29/09/2026)

Implementado e validado no mesmo dia. Sem validação manual do dono: `Concluído`.

Ajustes durante a implementação, dentro do escopo:

- Caminho do binário nas skills: `$env:USERPROFILE\AppData\AndroidCLI\android.exe` / `$USERPROFILE/...`, não `C:\Users\Nicolas\...`. Assim a skill não fica presa a esta máquina (alinhado ao plano SD1 de alinhamento de skills, aberto em paralelo por outro chat e ainda aguardando aprovação).
- O emulador estava no tema claro. A prova foi feita em `light/home1`, com o mesmo gate.
- `.agents/` e `.hermes/` estão no `.gitignore`. As 4 pastas foram sincronizadas no disco, mas só `.claude` e `.grok` entram no commit.

Validação executada:

1. `git diff --no-index --stat .agents/skills <.grok|.hermes|.claude>/skills`: vazio nas três. As 4 pastas estão idênticas byte a byte (LF).
2. `grep -rn "adb exec-out screencap"` nas skills do projeto: sobra só como fallback declarado em `dieta-bot-android-visual`. Caminho principal: `android screen capture`.
3. Emulador `emulator-5554` (780×1688, density 320, tema claro, Home com dados). `android screen capture` e `adb exec-out screencap` da mesma tela: 780×1688, 0 byte de pixel diferente (pngjs). `node tools/diff-gold.mjs light/home1`: `✓ 1.19% ink 0.97 (content -16 dp)` com o `home1.png` versionado e o mesmo `✓ 1.19%` com a captura do CLI no lugar. O arquivo versionado foi restaurado (`git checkout`); nenhuma captura mudou.
4. `android layout --flat` lido como UTF-8: `Configurações`, `P Proteína`, `350 kcal · +175 na meta` corretos. Lista de diffs de exemplo, `home1` claro (gold 780×2724, mesma grade de 780 px; offset de conteúdo −16 dp = −32 px medido pelo gate):

   | resource-id | bounds no app | no gold | leitura |
   |---|---|---|---|
   | `home-consumed` | `[305,321][476,413]` | ≈ y 364–430 | igual, com o offset de −32 px |
   | `home-workout` | `[40,1040][740,1152]` (112 px) | ≈ `[44,1074][736,1183]` (109 px) | igual (±4 px) |
   | `home-summary-1` | `[176,1516][453,1548]`, `520 kcal · 28P · 52C · 22G` | ≈ y 1557, mesmo texto | igual |
   | `home-fab` | `[608,1456][736,1584]` | rodapé do gold de página inteira | fixo no viewport; comparar pela captura, não pelo y do gold |

5. `git diff --stat`: nada em `apps/android/`, `server/` ou `tools/`.

## Encerramento

Ciclo de vida aplicado: `Concluído` em `plans/completed/`.

Só declaração explícita do dono cancelando este plano permite `Cancelado` e a pasta `plans/cancelled/`.
