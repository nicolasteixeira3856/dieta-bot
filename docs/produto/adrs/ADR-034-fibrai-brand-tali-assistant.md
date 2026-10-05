# ADR-034 — Product brand "Fibrai"; AI assistant "Tali"

- Status: Accepted (2026-10-05, owner decision in chat)
- Date: 2026-10-05
- Context: `produto`
- Replaces: once the rename plans complete, the visible name "Dieta Bot" of [ADR-016](ADR-016-nome-dieta-bot.md). ADR-016 keeps the technical IDs `nutri` and stays in force until then.

## Context

[ADR-016](ADR-016-nome-dieta-bot.md) made "Dieta Bot" the visible name and called it temporary. The owner wants a final name that:

- works in pt-BR first and later in the US, Europe and Japan;
- does not lock the product to nutrition, because the long-term vision also covers training and a space where nutritionists and personal trainers deliver diets and workouts to their clients;
- gives the in-app assistant its own name.

The owner and the agent brainstormed on 2026-10-05. Rejected candidates and the reasons are under Alternatives.

## Decision

1. **Product brand: "Fibrai"** (one word, capital F). It is a coined word that reads as "fibra" (dietary and muscle fiber) plus "IA"/"AI".
2. **AI assistant name: "Tali"** (feminine). It is the name of the assistant inside the app: chat header, the FAB, and how the model presents itself. **Tali is never the store or product brand.**
3. **Technical IDs stay `nutri`**, as in ADR-016: package and `applicationId`, Firebase `nutri-bot-dev`, `nutri.db`, VM `nutri-api`, the Stitch project `Nutri`. The final package is still the ADR-016 "future ADR before prod".
4. This ADR records names only. It does not widen the product scope. AGENTS "One agent. Job: fit the next meal into today's remaining budget" and the ADR-012 screens stay. Training and a professionals' platform need their own product decisions.

### Not decided here

- Store title, and whether to use a "Fibrai Pro" name for professionals. A suggestion was "Fibrai: nutrição e treino" (24 characters, within the 30-character Google Play limit). Any store listing work is blocked by the [production gate](../../content-policy/production-gate.md).
- Pronunciation of "Fibrai" (fi-BRAI or FI-brai) and the wordmark/logo.

## Clearance done on 2026-10-05

| Check | Fibrai | Tali |
| --- | --- | --- |
| USPTO (tmsearch) | No result | TALI / TALI HEALTH (child cognitive health software) and TALI AI (clinical documentation software) exist in health software classes |
| Google Play / App Store | No app named Fibrai; a Play search for it shows internet providers named "Fibra" | Tali AI (clinical scribe); Tari App (nutrition and fitness tracker, written タリ in Japanese like Tali); Tally - AI Nutrition Companion (iOS) |
| Domains (RDAP) | `fibrai.app`, `fibrai.com.br`, `fibrai.ai`, `fibrai.fit`, `fibrai.health` free; `fibrai.com` registered, no site | — |
| INPI, EUIPO, JPO | Not checked (both sites refused the agent's connection) | Not checked |

The Tali results are the reason for decision 2. Because of them, Tali is used only as an in-app character name.

## Motivation

- "Fibra" covers nutrition (dietary fiber) and training (muscle fiber), and in pt-BR "ter fibra" also means grit. So the brand can grow with the vision without a *nutri-* prefix. The *nutri-* prefix is weak to register and has dozens of competing apps.
- A coined word is easier to register than the common word "fibra". "Fibra" alone is crowded in Brazil by internet providers and Banco Fibra, has `FIBRA` marks for supplements in the US, and could be read as descriptive under the doctrine of foreign equivalents.
- Keeping the assistant name apart from the brand means a conflict on "Tali" can be fixed by renaming the assistant, without touching the brand, store listing or icon.
- The owner picked Tali as the assistant name (also a nod to a Mass Effect character). It is short, has no bad meaning in pt-BR, English or Japanese, and is not a food word that the meal parser could confuse with an ingredient.

## Consequences

### Positive

- One brand for the user app, now and later, and a separate assistant name that can be renamed on its own.
- The technical IDs do not change. No reinstall, no data loss, no new Firebase app.

### Negative

- "Dieta Bot" stays visible in the app and the docs until the rename plans complete. AGENTS "Product name" changes only at that Completion.
- Visible copy changes: launcher name, splash wordmark, onboarding header, "Chat Dieta Bot", "Dieta Bot AI" and the model's self-introduction. This needs:
  - a Figma design plan for every gold showing the old name;
  - an android plan;
  - a server plan for the prompt.
- INPI, EUIPO and JPO clearance for "Fibrai" is still open. It should be done, preferably with an IP agent, before any money is spent on the brand beyond domains.
- Mass Effect is EA/BioWare IP. No art, icon or marketing may reference the character. Only the first name is used.
- "Fibrai" carries "fibra". A user who searches only "Fibra" in a Brazilian store finds internet providers. The icon and the store descriptor must make the health meaning clear.

## Alternatives considered

| Candidate | Role | Why rejected |
| --- | --- | --- |
| Tali as product brand ("Tali - Sua assistente de dieta e treino") | brand | US health software marks TALI / TALI AI; Tari App in the same category; title over 30 characters; "treino" overpromised the current scope |
| Tali + surname | brand | The dominant element "Tali" stays; it does not remove the conflict |
| Tessel | brand | Owner: too complex for a nutrition app |
| Saldo | brand | Owner: sounds odd in pt-BR; crowded by finance apps |
| Cota | brand | US `COTA` marks in medical software (Cota, Inc.) and occupational therapy; in Brazilian stores "cota" reads as price quotes; fits the daily budget only, not training or professionals |
| Nuttri, Nutrilify | brand | Saturated and weak *nutri-* prefix; "Nutrilify" is close to Amway's Nutrilite; "nut" is a food and "nutter" is British slang for crazy |
| Fibra | brand | Crowded by internet providers and Banco Fibra in Brazilian stores; most domains taken; possibly descriptive in the US |
| Fibri, Fibrit, Fybra | brand | Fibri suggests fibrillation (FibriCheck, medical marks); Fibrit searches return fibromyalgia apps; FYBRA is a live US class 9 mark |
| Forma, Ritmo, Mova, Equa, Soma, Plena, Kalibra, Resta, Taru | brand | Existing health, fitness or food apps with the same name |
| Zoe, Ada, Kaia, Aria, Mira | assistant | Existing health or AI products (ZOE nutrition, Ada Health, Kaia Health, Opera Aria, Mira fertility) |

## Relations

- Predecessor: [ADR-016](ADR-016-nome-dieta-bot.md) (visible name "Dieta Bot", technical IDs `nutri`).
- Related ADRs: [ADR-012](ADR-012-chat-home-perfil.md) (screens), [ADR-031](../../design/adrs/ADR-031-figma-source-of-truth.md) (Figma gate for the new name in golds).
- Production: [production gate](../../content-policy/production-gate.md) (store listing and title).
- Plans: none yet. The rename is a future design plan + android plan + server plan, each needing its own approval.

Once accepted, this ADR is not edited. A later change needs a new ADR that declares the replacement.
