# Plan — D27 Release 2: Conversational onboarding

- Status: Aguardando aprovação
- Date: 09/10/2026
- Owning context: `design`
- Affected code: none in `apps/` or `server/`. Figma `Design` → `Release 2` → section "Onboarding v2", new components in `Componentes`. Repository: `docs/qa/figma/{dark,light}/{ob0,ob1,ob1e,ob2,ob3,ob4,ob5,ob6}.png`, retirement of `o1`, `o1e`, `o2`, `o3`, `o3t`, `o3s`, `o4`, `o5` from the inventory and from `tools/export-figma.mjs` (the Config meal editor keeps `cfgS` and its own frames), the "Onboarding v2" node ids in `tools/export-figma.mjs`.
- Prerequisites: [D26](completed/) `Concluído`; [ADR-057](../../produto/adrs/ADR-057-conversational-onboarding.md) accepted by this approval.
- Figma MCP budget: ≤ 110 calls (at most 120 a day, ADR-031 § 6).

Authorization, lifecycle, cancellation and deferral: `docs/sdd/README.md`. Approval: `Aprovo o plano docs/design/plans/d27-conversational-onboarding.md. Implemente o plano aprovado.`

## Objective

Draw the guided onboarding of [ADR-057](../../produto/adrs/ADR-057-conversational-onboarding.md) in Aero, in both themes, from the design system: one welcome screen, one chat template in its three states, the summary, and the three full-screen states, with the features the plan A71 will build and nothing more. Low-fidelity reference: the six frames the agent sketched for the owner on 09/10/2026 (welcome; chat with buttons and a "não entendi" retry; chat with free text, counter and prefilled composer; summary with correction entries; building; success and error stacked).

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

<Filled at Completion: discovery table, MCP calls used, owner OK date, exported files.>
