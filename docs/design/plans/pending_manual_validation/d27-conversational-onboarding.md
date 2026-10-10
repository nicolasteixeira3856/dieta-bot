# Plan — D27 Release 2: Conversational onboarding

- Status: Pendente aprovação manual
- Date: 09/10/2026
- Owning context: `design`
- Affected code: none in `apps/` or `server/`. Figma `Design` → `Release 2` → section "Onboarding v2", new components in `Componentes`. Repository: `docs/qa/figma/{dark,light}/{ob0,ob1,ob1e,ob2,ob3,ob4,ob5,ob6}.png`, retirement of `o1`, `o1e`, `o2`, `o3`, `o3t`, `o3s`, `o4`, `o5` from the inventory and from `tools/export-figma.mjs` (the Config meal editor keeps `cfgS` and its own frames), the "Onboarding v2" node ids in `tools/export-figma.mjs`.
- Prerequisites: [D26](../completed/) `Concluído`; [ADR-057](../../../produto/adrs/ADR-057-conversational-onboarding.md) accepted by this approval.
- Figma MCP budget: ≤ 110 calls (at most 120 a day, ADR-031 § 6).

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/d27-conversational-onboarding.md. Implemente o plano aprovado.`

## Objective

Draw the guided onboarding of [ADR-057](../../../produto/adrs/ADR-057-conversational-onboarding.md) in Aero, in both themes, from the design system: one welcome screen, one chat template in its three states, the summary, and the three full-screen states, with the features the plan A71 will build and nothing more. Low-fidelity reference: the six frames the agent sketched for the owner on 09/10/2026 (welcome; chat with buttons and a "não entendi" retry; chat with free text, counter and prefilled composer; summary with correction entries; building; success and error stacked).

## Sources (feature parity, ADR-031 § 4)

| Gold | Reference (current gold or frame, layout only) | Behavior source |
|---|---|---|
| `ob0` | `splash` (logo), none for the rest | ADR-057 decision 1 (welcome, pitch, CTA `Vamos começar`) |
| `ob1` | `chat0` thread and composer, `chatQ` quick replies | ADR-057 decisions 2–3 (closed question with buttons, user bubble, progress `{n} de {total}`) |
| `ob1e` | `ob1` | ADR-057 decision 2 (`Desculpa, não entendi. Pode repetir?` with the buttons again) |
| `ob2` | `ob1`, `chatX` counter | ADR-057 decision 3 (free text, `{n}/2000` counter, the composer prefilled with `{ceiling} kcal · P {p} · C {c} · G {g}`) |
| `ob3` | `memL` list blocks | ADR-057 decision 4 (summary blocks with a correction entry each; IMC and TMB as information; CTA `Confirmar e montar o perfil`) |
| `ob4` | `splash` | ADR-057 decision 6 (`Estamos montando o seu perfil`, `Só mais alguns instantes…`, full screen, logo, animation placeholder) |
| `ob5` | none | ADR-057 decision 6 (success, green full screen, `Perfil pronto`, one CTA `Ir para a Home`) |
| `ob6` | none | ADR-057 decision 6 (error, red full screen, `Não deu certo`, `Tente de novo mais tarde. Suas respostas ficam salvas.`, `Tentar de novo` above `Voltar ao chat`) |

Code: `apps/android/app/src/main/java/app/fibrai/android/feature/onboarding/` (current screens, to be replaced by A71) and `feature/chat/` (bubbles, composer, quick replies to reuse).

## Scope

1. **Discovery (read only):**
   - per gold, list every visible element and classify it: `app` (exists in code or spec), `gold-only` (dropped) or `copy` (exact pt-BR text from ADR-057 or the code);
   - the table goes into Results before any write;
   - an element in the ADR but missing from the sketch is drawn from the ADR.
2. **Components** (in `Componentes`, readability rule):
   - `Onboarding/Progress`: a thin bar plus `{n} de {total}` and the section label (`Perfil`, `Refeições`, `Rotina`, `Avisos`);
   - `Onboarding/QuickReply`: the Chat quick-reply pill, with a `selected` variant;
   - `Onboarding/SummaryBlock`: title, lines, the pencil entry;
   - `Onboarding/FullScreenState`: `building` | `success` | `error` variants with the semantic surfaces (`status/good`, `status/bad`) and stacked buttons;
   - the welcome animation and the building animation are drawn as a static frame with a note for the client (the motion is the client's, within the tokens).

   Reuse `Chat/Bubble`, `Chat/Composer` and `Button/Primary`; extend with properties where a state needs it.
3. **Screens** (section "Onboarding v2" on `Release 2`):
   - Light row: `ob0`, `ob1`, `ob1e`, `ob2`, `ob3`, `ob4`, `ob5`, `ob6`, in product order, 390 px wide;
   - Dark row: clones with the `Dark` mode, no other change;
   - frame names `<id> · <screen title> · Light|Dark`; spacing per ADR-031 § 3;
   - the "Onboarding" section of Release 1 (`o1`–`o5`) is kept in the file as history and marked `Retired by D27` in its section name; it is removed from the export map and from the inventory.
4. **Owner review** (Figma review gate):
   - one screenshot per frame is sent to the owner;
   - the plan goes to `pending_manual_validation/` until the owner's OK in Figma;
   - fixes follow, with one screenshot per changed frame.
5. **Export** after the OK:
   - fill the "Onboarding v2" ids in `tools/export-figma.mjs` and drop the `o1`–`o5` ids;
   - `node tools/export-figma.mjs --only ob0 ob1 ob1e ob2 ob3 ob4 ob5 ob6`;
   - update the inventory of `docs/qa/README.md` (add the eight ids, remove the eight retired ids; the retired PNGs move to `docs/qa/_legacy/`);
   - `node tools/check-figma.mjs`.

## Out of scope

- Compose (A71). Behavior or copy changes beyond ADR-057. The Config editors (`cfg`, `cfgS`, `cfgT`) and their frames. The Home.

## Validation

1. The discovery table exists in Results before the first write.
2. A read-back confirms instances (not detached frames) for every DS component, no raw hex fills outside the variables, and no overlapping nodes.
3. Visual: one screenshot per frame; manual owner OK.
4. `check-figma` and `check-docs` pass.

## Results

### Discovery (09/10/2026, before any write)

Read only: Figma `Release 1` frames `splash`, `chatQ`, `chatX` (structure and layers), `Release 2` frame `memL`, the `Componentes` components `Chip/Log`, `Chat/Bubble`, `Chat/Composer`, `Chat/ActionBar`, `Button/Primary`, `Progress/Bar`, `Stepper/Progress`, `Option/Tone`, `Card/Note`, `Header/Page`, the Color variables (`docs/tokens.md`), [ADR-057](../../../produto/adrs/ADR-057-conversational-onboarding.md), the current onboarding copy (`OnboardingAeroScreens.kt`: tones `Seco` "Só os números. Sem opinião." and `Duro` "Cobra o que estourou e o que faltou. Sem rodeio.", the disclaimer "Estimativa, não consulta.").

Sample person (synthetic, ADR-033): female, 32 years, 165 cm, 66 kg. Mifflin-St Jeor TMB = 10·66 + 6,25·165 − 5·32 − 161 = 1.370 kcal (rounded to 10); IMC 66 / 1,65² = 24,2; macros 30/40/30 → P 103 g · C 137 g · G 46 g. Question count: 16 (1 sexo, 2 idade, 3 altura, 4 peso, 5 teto e macros proposed in the composer, 6 número de refeições, 7 nomes e horários, 8 compensação, 9 tom, 10 restrições, 11 medidas, 12 comidas habituais, 13 não gosta, 14 equipamentos, 15 meta de peso, 16 avisos); sections `Perfil` 1–5, `Refeições` 6–7, `Rotina` 8–15, `Avisos` 16.

| Gold | Element | Class | Source / copy |
|---|---|---|---|
| all chat states (`ob1`, `ob1e`, `ob2`) | Page gradient, glass bubbles, Tali label above bot messages | app | `chatQ` layout; ADR-035 |
| | Progress header: section label, `{n} de 16`, thin bar | app (new) | ADR-057 decision 3 |
| | Composer text only: no camera button, placeholder `Escreva sua resposta` | app (new copy) | ADR-057 decision 1 (no photo, no audio) |
| | Chat header with back, date chip, `Chat/MetaCard`, suggestion chips, `Forçar estimativa` | gold-only here (dropped) | Chat features outside the onboarding |
| `ob0` | Logo (oat seed) in place of the welcome animation, page gradient, bubbles | app | `splash`; animation is the client's (note in the section) |
| | `Bem-vindo ao Fibrai` (Title), pitch `Umas perguntas rápidas e a Tali monta o seu perfil.` (Body, muted), CTA `Vamos começar` | copy | ADR-057 decision 1 |
| `ob1` | Progress `ROTINA · 9 de 16`; Tali `Treino conta na meta do dia? Escolha quanto do gasto do treino volta para o teto.`; user `50 %`; Tali tone question with one example per tone; quick replies `Seco` (selected, preselected) and `Duro` | copy | ADR-057 decisions 2–3; tone copy from the code |
| `ob1e` | Same question; user `pode ser um meio termo`; Tali `Desculpa, não entendi. Pode repetir?`; the quick replies again | copy | ADR-057 decision 2 |
| `ob2` | Progress `PERFIL · 5 de 16`; user `66 kg`; Tali proposal with TMB and IMC as information; composer prefilled `1.370 kcal · P 103 · C 137 · G 46`; counter `{n}/2000` under the composer | copy | ADR-057 decisions 1, 3 |
| `ob3` | Title `Resumo do seu perfil`, subtitle `Confira antes de montar. Toque no lápis para corrigir.` | copy | ADR-057 decision 4 |
| | Blocks with a pencil each: Corpo (with TMB and IMC), Teto e macros, Refeições, Treino, Tom, Restrições, Medidas, Comidas, Equipamentos, Meta de peso, Avisos | app (new) | ADR-057 decision 4 (one block per question group) |
| | Caption `IMC e TMB são informação. Estimativa, não consulta.`; CTA `Confirmar e montar o perfil` | copy | ADR-057 decision 4; disclaimer of the retired O4 |
| `ob4` | Full screen, page gradient, logo, `Estamos montando o seu perfil`, `Só mais alguns instantes…`, `Loader/Aero` as the animation placeholder | copy | ADR-057 decision 6 |
| `ob5` | Full screen `status/good`, check icon, `Perfil pronto`, `A Tali já sabe o seu teto, as suas refeições e o que você come.`, one CTA `Ir para a Home` | copy | ADR-057 decision 6 |
| `ob6` | Full screen `status/bad`, alert icon, `Não deu certo`, `Tente de novo mais tarde. Suas respostas ficam salvas.`, `Tentar de novo` above `Voltar ao chat` (stacked) | copy | ADR-057 decision 6 |

Contrast on the status surfaces (no new token): text, icons and the inverse button fill use `accent/on` (white Light, `#03203a` Dark); the inverse button label uses the surface colour. Pairs: Light white on `#1e7733` and on `#b93b2c`; Dark `#03203a` on `#7ed99a` and on `#ff7a6b`. Measured values go to Validation below.

### Build (09/10/2026)

- **Components** (`Componentes`, new section `Onboarding v2 · D27`, `208:268`, below `Opções · D25–D26`):
  - `Onboarding/Progress` (`208:271`): section label (Label/Section, `accent/default`), `{n} de {total}` (Caption, `text/muted`), 6 px bar (track `surface/2`, `Fill` `accent/default`, width n/total); TEXT properties `Section#208:0`, `Count#208:1`.
  - `Onboarding/QuickReply` (set `208:283`): `Selected=false` (`208:277`, glass pill) and `Selected=true` (`208:279`, `surface/selected`, `border/selected` 2 px, check); TEXT property `Label#208:2`; height 44.
  - `Onboarding/SummaryBlock` (`208:284`): glass card, title (Label/Section, `text/muted`), lines (Body, `text/primary`), pencil `Icon/pencil-simple`; TEXT properties `Title#208:5`, `Lines#208:6`.
  - `Onboarding/FullScreenState` (set `208:335`): `State=Building` (`208:290`, page gradient, logo as the animation placeholder, `Loader/Aero`), `State=Success` (`208:309`, `status/good`, check mark, inverse CTA), `State=Error` (`208:321`, `status/bad`, alert mark, inverse `Tentar de novo` above outline `Voltar ao chat`); TEXT properties `Title#208:7`, `Body#208:11`, `Primary#208:15`, `Secondary#208:19`.
  - Reused as instances: `Chat/Bubble`, `Chat/BotLabel`, `Chat/Composer` (camera hidden by override, placeholder `Escreva sua resposta`, left padding 20), `Button/Primary`, `Loader/Aero`, the `Logo / Mark` of `splash`. No component of Release 1 changed.
- **Frames** (`Release 2`, new section `Onboarding v2 · D27`, `209:992`, at y 9340 under `Opções · D25–D26`, 3840 × 3140): Light row at y 208, Dark row at y 1674 (clones in mode `Dark`, 7 text paints outside instances rebound with the Dark resolved value).

| Gold | Light | Dark | Size |
|---|---|---|---|
| `ob0` · Boas-vindas | `209:995` | `210:1190` | 390 × 844 |
| `ob1` · Onboarding — pergunta com botões | `209:1016` | `210:1210` | 390 × 844 |
| `ob1e` · Onboarding — resposta não entendida | `209:1068` | `210:1259` | 390 × 844 |
| `ob2` · Onboarding — resposta escrita com teto proposto | `209:1119` | `210:1308` | 390 × 844 |
| `ob3` · Onboarding — resumo do perfil | `209:1164` | `210:1351` | 390 × 1386 |
| `ob4` · Montando o perfil | `209:1244` | `210:1429` | 390 × 844 |
| `ob5` · Perfil pronto | `209:1272` | `210:1449` | 390 × 844 |
| `ob6` · Erro ao montar o perfil | `209:1286` | `210:1462` | 390 × 844 |

Review PNGs without MCP calls: `node tools/export-figma.mjs --only <node id> --dry-run --out <dir>`.

The Release 1 section `Splash e onboarding · D4` is renamed `… · o1–o5 Retired by D27` together with the export after the owner's OK (the retirement takes effect with the OK; `splash` stays in that section and in the inventory).

### Validation (before the owner review)

1. Discovery table above, written before the first write.
2. Read-back (`use_figma`) of the 16 frames: instances for every DS component (`ob0` 3, `ob1` 14, `ob1e` 14, `ob2` 11, `ob3` 23, `ob4` 3, `ob5` 2, `ob6` 2, the same in Dark); zero visible solid paints without a variable or style; no overlapping children in the section and no page node overlapping it; Light frames in mode Light, Dark frames in mode Dark.
3. Contrast of the status screens (WCAG, `accent/on` on the surface): Light white on `#1e7733` 5.61:1, on `#b93b2c` 5.64:1; Dark `#03203a` on `#7ed99a` 9.66:1, on `#ff7a6b` 6.49:1. The inverse button labels are the same pairs reversed. No new variable.
4. Visual: one review PNG per frame (16) from the dry-run export, sent to the owner on 09/10/2026.

Figma MCP budget: 12 of 110 calls (whoami, 3 skill reads, 5 reads, 3 writes with read-back). Owner review pending.
