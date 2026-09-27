# Design System — Nutri (Google Stitch)

Design System oficial e fonte da verdade visual para o app Nutri (Android Jetpack Compose, Mobile 390×844).

- **Projeto Oficial no Google Stitch:** `Nutri`
- **Project ID:** `6282733070135794645`
- **Gold Visual (Exportado):** `docs/qa/stitch/dark/` e `docs/qa/stitch/light/` (18 telas por tema, total de 36 telas).

> [!IMPORTANT]
> **Padrão Ouro de Implementação:**
> Toda e qualquer implementação de UI no client Android deve ser baseada estritamente nos PNGs do Stitch (`docs/qa/stitch/{dark,light}/`). O agente/desenvolvedor deve iterar o código Compose até que a captura do emulador em `docs/qa/android/current/{dark,light}/` esteja visualmente equivalente ao Gold do Stitch. Wireframes são permitidos exclusivamente como referência preliminar de criação de layout (preferindo sempre Stitch via MCP, com wireframe como fallback).

---

## 1. Princípios e Job

- **Job central:** Encaixar a próxima refeição no saldo do dia, sobretudo o jantar.
- **Registro de Alimentos:** Linguagem natural e/ou foto exclusivamente pelo Chat (aberto pelo FAB da Home).
- **Home:** Painel / relógio do dia. Não é diário de busca. Não calcula TDEE.
- **Linha do Tempo Contínua:** Refeições organizadas verticalmente com nós conectados por guia temporal.
- **Registro Único Consolidado:** Cada refeição confirmada no Chat vira uma única entrada consolidada na Timeline (ex.: `2 pães franceses, 2 ovos mexidos e café com leite` - `520 kcal · 28P · 52C · 22G`), sem fragmentação artificial por ingrediente.
- **Copy Humana e Direta:** Sem frases de efeito robóticas ("Número é a verdade", "A conta só sugere"). Textos naturais em pt-BR.
- **Disclaimer obrigatório:** "Estimativa nutricional, não substitui consulta médica ou nutricional."

---

## 2. Paleta de Cores e Tokens Literais (Sem Dynamic Color)

### Tema Dark
- `bg`: `#0b0d10` (fundo externo do sistema / canvas)
- `phone`: `#0e1114` (fundo do canvas mobile 390×844)
- `panel`: `#12151a`
- `surf`: `#171b20` (cards, timeline cards, bolha do assistente, container do composer)
- `surf2`: `#1e242b` (bolha do usuário, chips de seleção, botões secundários)
- `line`: `#2a3139` (bordas e divisores)
- `text`: `#f3f5f7` (texto primário de alto contraste)
- `muted`: `#8b939c` (texto secundário / metadados 12–14pt)
- `dim`: `#5c6570` (kickers, labels auxiliares, horários)
- `gold`: `#e8b86d` (acento da marca: anel de progresso, FAB, destaques principais)
- `CTA`: `#f3f5f7` sobre fundo `#111111`
- `destructive CTA`: texto `#111111` sobre `#e07a6a` (`bad`)

### Tema Light
- `bg`: `#f4f3f0`
- `phone`: `#f7f6f3`
- `panel`: `#eceae6`
- `surf`: `#ffffff`
- `surf2`: `#e8e6e2`
- `line`: `#d5d2cc`
- `text`: `#14161a`
- `muted`: `#5c636b`
- `dim`: `#8b939c`
- `gold`: `#b8873d`
- `CTA`: `#111111` sobre fundo `#f3f5f7`
- `destructive CTA`: texto `#ffffff` sobre `#c14d40` (`bad`)

### Macronutrientes Semânticos (P · C · G)
Cores tonais com semiótica nutricional clara:
1. **Proteína (P):** Verde Menta / Esmeralda Profundo
   - Dark: `#4ec994` (glow sutil)
   - Light: `#1b7a4b`
   - *Conceito:* Construção e regeneração celular, o pilar seguro da dieta.
2. **Carboidrato (C):** Âmbar Alaranjado Queimado
   - Dark: `#e58e42`
   - Light: `#c2651e`
   - *Conceito:* Combustível e energia rápida; atenção sem alarme de erro.
3. **Gordura (G):** Ouro da Marca
   - Dark: `#e8b86d`
   - Light: `#b8873d`
   - *Conceito:* Lipídios essenciais, azeites e óleos nobres.
4. **Estouro de Meta / Alerta:** Vermelho Coral (`bad`)
   - Dark: `#e07a6a`
   - Light: `#c14d40`

---

## 3. Tipografia e Ritmo Visual (Material 3 Expressive)

- **Família Display & Títulos:** *Plus Jakarta Sans* (w600/700/800)
- **Família Corpo e Dados:** *Inter* (w400/500/600)
- **Herói de Calorias:** 36pt font-bold (leading-none), anel de progresso circular 204dp com stroke 16dp e ambient glow.
- **Campos de Entrada (Input):** 28pt tabular numbers para peso/calorias.
- **Área de Toque Mínima:** 56dp nos botões principais e itens de formulário.
- **Raios de Curvatura (Shapes):**
  - Bottom Sheet: 22dp no topo.
  - Cards e Timeline Milestones: 16–20dp squircle com borda sutil de 1px.
  - Chips e Micro-tags: 8dp.
  - FAB: 64×64dp circular com elevação Expressive.

---

## 4. Estrutura da Linha do Tempo (Home)

A Home adota uma Timeline vertical contínua com nós visuais:
1. **Refeição Registrada:** Nó sólido com ícone de conclusão (ou ícone de câmera se registrada por foto), texto descritivo consolidado e micro-resumo nutricional (`520 kcal · 28P · 52C · 22G`).
2. **Refeição Pulada:** Nó vazado pontilhado discreto, texto indicando *"Refeição pulada"*.
3. **Refeição Atual / Próxima:** Nó com anel ativo sutil. Toque na área do slot abre diálogo de confirmação para pular. O texto padrão de slot vazio é estritamente: `"Nenhum registro · Toque para pular"`. O único ponto de entrada para novo registro e Chat é o **FAB**.

---

## 5. Inventário Oficial de Telas no Google Stitch (Project: `Nutri` / `6282733070135794645`)

| Wire ID | Grupo | Screen ID Stitch (Dark) | Screen ID Stitch (Light) | Título no Stitch |
|---|---|---|---|---|
| `splash` | Entrada | `5a803993e6834bdeb8e8b8e098a1226b` | `7a34dd2615cd4e2d850db7cac3803b5d` | Nutri Splash Screen |
| `o1` | Onboarding | `268a0d329a034b60b78545a722394dc7` | `9f5820b4990f4c13b4b84a8860209bee` | Onboarding 1/4 - Teto do dia |
| `o2` | Onboarding | `c97ade760dcb4c3b8c55a80faab7510f` | `8ab9ffbf98294b9c834d5c7c480f0419` | Onboarding 2/4 - Compensação de treinos |
| `o3` | Onboarding | `14440390e2d94582af1efa4e8aa4573b` | `87f1e6f1b9334faeae8f40f6cebf56e5` | Onboarding 3/4 - Distribuição das refeições |
| `o4` | Onboarding | `9b9a9ab5dd9e471f9ff600ef6273cc6a` | `9e763f0f8a6b4245894f38044e6d48f4` | Onboarding 4/4 - Alvos de macronutrientes |
| `home0` | Home | `9e798987e4794692a03d42e2fb7cb249` | `9f2849ad1ae9413f8ef23ae7777051a0` | Home vazia - Day 1 (Timeline) |
| `home1` | Home | `fad15337390b4638b5d51fdac191a010` | `83adedc45ff1441ea398414b7f41dede` | Home no dia - 1300 kcal (Timeline) |
| `homeX` | Home | `1df72fbb44824b6a861d334672f31c6c` | `74299a8d5cff46f790fab34d5626b5a7` | Home meta excedida - 2280 kcal (Timeline) |
| `chat0` | Chat | `be092fe5db2f40b4bca8da18ae9ce580` | `84f166d46b6f4a6d847a3c64977e07d3` | Chat vazio |
| `chatL` | Chat | `e021044511b44e25b8a8de6433b8dbad` | `611e2752cdd04d2fb103c251b6d21c7b` | Chat loading - Estimando |
| `chatE` | Chat | `a91c63f2a624456f9d5e30ef14422578` | `2811a76b200a447aad6d5233cbb5cce4` | Estimate com botões de ação |
| `chatT` | Chat | `8537f8a0e82a4529b3fb0efb9ea89c38` | `9feb6e7d276a41d6956df9fc001f445a` | Selecionar refeição - Bottom Sheet |
| `chatP` | Chat | `e64a7a52e1a54259b2e488e7dfdf7413` | `66c301014a5f4fdc8b002756faa87396` | Diálogo de confirmação para pular |
| `chatF` | Chat | `ac502aadad004c18bc271e0c74355960` | `35a756967193481fa84c48e05e33e0be` | Foto de refeição e estimativa no Chat |
| `chatG` | Chat | `5e95d8451bf447989bd62a2e9dd38909` | `c226508d6c9b4830952a42005f1cf2f4` | Confirmação pós-gravação (Duplo-check) |
| `cfg` | Config | `ffb8e640dff34e8b9015e4936f36ebe5` | `d582887ce63242a9ab48b882647ddbb9` | Configurações do perfil e dia |
| `wipe` | Config | `f2797b013a714413ac252918753dffa9` | `bacd8c4d917040d687ef6a4e9ddedad2` | Reiniciar registros de hoje - Diálogo Wipe |
| `push` | Sistema | `f9c469937daa43849a07deb36ac0610d` | `038a997a4f2247c9a12da72aea01f73c` | Notificação do sistema - Lembrete |

Total: **36 telas oficiais no Stitch** (18 Dark + 18 Light).

---

## 6. Ferramentas e Exportação

- **Exportação das PNGs do Stitch:**
  ```bash
  node tools/export-stitch.mjs
  ```
- **Validação de Integridade do Gold:**
  ```bash
  node tools/check-stitch.mjs
  ```
