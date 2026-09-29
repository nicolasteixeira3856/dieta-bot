# Plano — SV1 Verificação automática dos gates Stitch

- Estado: Concluído
- Data: 29/09/2026
- Contexto proprietário: `stitch`
- Código afetado: `tools/` (scripts Node do Stitch), `docs/stitch/`
- Pré-requisitos: Nenhum. Deve ficar pronto antes da verificação do [ST3](st3-seletor-horario.md), [ST4](../st4-refeicoes-por-dia.md) e [ST5](../st5-chat-texto-longo.md).

## Gate de autorização

Este plano é exclusivamente documental. A implementação só começa após aprovação explícita que identifique este arquivo:

> Aprovo o plano `docs/stitch/plans/sv1-verificacao-automatica.md`. Implemente o plano aprovado.

Se a implementação revelar decisão não coberta, pare, atualize os artefatos e peça nova aprovação.

## Objetivo

Verificar um gate em **uma rodada só**: um comando que exporta só as telas do gate, confere regras objetivas no HTML de cada tela e gera um relatório com o antes e depois, as falhas e o prompt de correção. Menos leitura de PNG, menos idas e vindas, menos custo.

Motivo: no ST1 e no ST2 (29/09/2026), a verificação levou muitas rodadas. Metade delas veio de defeitos do render do agente e de screenshots velhos do Stitch, não do Stitch em si. Detalhes em [dieta-bot-stitch](../../../../.claude/skills/dieta-bot-stitch/SKILL.md) § Known limits.

## Fontes de verdade

- [docs/sdd/README.md § Gate Stitch](../../../sdd/README.md#gate-stitch).
- [docs/stitch/README.md](../../README.md) (títulos ↔ golds).
- Skill [dieta-bot-stitch](../../../../.claude/skills/dieta-bot-stitch/SKILL.md).

## Escopo de implementação

### 1. Checagens por gate (`tools/verify-stitch.mjs`)

- Cada gate ganha um arquivo `docs/stitch/plans/<gate>.checks.json`, com uma lista de checagens por tela (gold id + tema). Tipos:
  - `text`: texto presente (`has`) ou ausente (`not`) no HTML renderizado. Exemplos: "PROTEÍNA", "IA ATIVA", o título do header.
  - `fits`: um elemento achado por texto não transborda o container a 390 px, com altura máxima opcional (a linha "Treino de hoje" em 56 px).
  - `gap`: distância mínima entre dois textos ("Treino de hoje" e "200 kcal" ≥ 12 px).
  - `visibleAbove`: um texto fica inteiro acima de um elemento fixo (o aviso acima do FAB "Chat").
  - `images`: toda `<img>` do HTML responde 200.
  - `uniqueTitles`: nenhum título do gate aparece em mais de uma tela do `list_screens` (vira aviso, não falha: a API mostra versões ocultas).
- Renderiza com o mesmo Playwright e a mesma viewport do exportador: 390 px e altura do frame. Uma função compartilhada, sem cópia.
- Saída: uma linha por checagem (`ok` / `FALHA` + valor medido). Código de saída 1 se alguma falhar.

### 2. Export seletivo

- `node tools/export-stitch.mjs --only home0,home1,homeX,homeW`: baixa e renderiza só essas telas, nos dois temas.
- Sem `--only`: comportamento de hoje.

### 3. Filtro de ruído

- Depois de exportar, compara cada PNG com a versão do git, pixel a pixel, usando o `pngjs` que já está em `tools/node_modules`.
- Diferença abaixo do limiar (0,05% dos pixels com Δ > 40): restaura o arquivo original. Assim só PNG com mudança visual real fica modificado.
- Mesmo tamanho de imagem ou não, o log mostra o percentual por arquivo.

### 4. Relatório único (`tools/verify-stitch.mjs --report`)

- Gera `stitch-report.html` no scratchpad (nunca em `docs/qa/`). Por tela: o gold antigo e o novo lado a lado, recortados na região que mudou, e o resultado das checagens.
- Para cada falha: o bloco de prompt de correção por tema, com o título exato da tela (da tabela do README) e a lista "keep unchanged".
- O agente manda esse arquivo ao dono numa mensagem só.

### 5. Coerência dark × light

- Para cada gold do gate, extrai os textos visíveis do HTML dark e do light e compara. Diferença vira falha, com os dois textos. Exemplo real: "Dieta Bot AI" × "Chat Dieta Bot"; rótulos dos chips só no light.
- Lista de exceções no `checks.json`, para textos que mudam de propósito entre os temas (hoje, nenhum).

### 6. Documentação

- `docs/sdd/templates/stitch-gate.md`: a seção Verificação passa a ser "rodar `node tools/verify-stitch.mjs <gate> --report`". O checklist em prosa continua, e cada item aponta para a checagem correspondente.
- `docs/stitch/README.md`: comando no Propósito.
- Skill `dieta-bot-stitch` (as 4 pastas): o Flow usa o comando novo, e os limites resolvidos saem da lista com data e modelo.
- `checks.json` escrito para ST3, ST4 e ST5 (gates ainda abertos). ST1 e ST2 ganham o seu como regressão.

## Arquivos e áreas afetadas

- `tools/verify-stitch.mjs` (novo), `tools/export-stitch.mjs` (`--only`, filtro de ruído, render compartilhado), `tools/check-stitch.mjs` (sem mudança de regra).
- `docs/stitch/plans/*.checks.json`, `docs/stitch/plans/completed/*.checks.json`.
- `docs/sdd/templates/stitch-gate.md`, `docs/stitch/README.md`, `docs/README.md` (índice).
- `.agents/skills`, `.claude/skills`, `.grok/skills`, `.hermes/skills` → `dieta-bot-stitch/SKILL.md`.

## Validação planejada

1. `node tools/verify-stitch.mjs st1` e `st2` no projeto atual: todas as checagens `ok`.
2. Regressão das falhas reais do ST1/ST2, com HTML salvo como fixture local:
   - HTML com "IA ATIVA" → `text not` falha;
   - linha de treino larga demais a 390 px → `fits` falha;
   - `<img>` com URL 403 → `images` falha;
   - dark sem "PROTEÍNA" e light com → coerência falha.
3. `node tools/export-stitch.mjs --only chat0`: só `chat0.png` nos dois temas é tocado; `git status` sem outro PNG.
4. Export completo sem mudança no Stitch: filtro de ruído deixa `git status` limpo.
5. `node tools/check-stitch.mjs` verde (40 golds).
6. `--report` gera o HTML com antes/depois e prompt de correção para uma falha de fixture.

## Fora de escopo

- Editar telas pelo MCP do Stitch (continua passo do dono; ver a skill).
- Mudar layout do app ou golds existentes.
- Rodar os gates ST3–ST5 (só escrever os `checks.json` deles).
- CI: os scripts precisam de `STITCH_API_KEY` e rede; seguem locais.

## Riscos e controles

- **Checagem por texto frágil** (o Stitch muda a marcação): as checagens buscam texto visível e geometria, não classes CSS.
- **Limiar do filtro esconder mudança real pequena:** o log sempre mostra o percentual; o limiar fica numa constante documentada.
- **Render ainda diferente do canvas do Stitch:** o verificador usa exatamente o render do exportador; divergência nova vai para a skill, com data e modelo.

## Critérios de aceite

- Um comando por gate responde "passou / não passou", com o valor medido de cada checagem.
- Export seletivo e filtro de ruído: nenhum PNG fora do gate aparece modificado.
- Falha gera o prompt de correção pronto, por tema, com títulos exatos.
- Falhas reais do ST1/ST2 reproduzidas como fixture são pegas.

## Encerramento

Depois da implementação, registre resultados reais e aplique o ciclo de vida em `docs/sdd/README.md`.

Só declaração explícita do dono cancelando este plano permite `Cancelado` e a pasta `plans/cancelled/`.

## Resultados (29/09/2026)

Implementado:

- `tools/verify-stitch.mjs`: checagens `text`, `fits`, `gap`, `visibleAbove` declaradas; `images`, coerência dark × light e títulos repetidos sempre rodam. Tela antiga pelo ID (confere o título), tela nova pelo título exato do README. `--report [--out]` exporta as telas do gate numa pasta temporária e gera o HTML com antes/depois recortado e o prompt de correção por tema. Referência dos tipos: [docs/stitch/README.md § Verificação automática](../../README.md#verificação-automática-sv1).
- `tools/export-stitch.mjs`: render compartilhado (`loadFrame`), `--only`, filtro de ruído (`NOISE_DELTA` 40, `NOISE_MAX_PCT` 0,05%) com o percentual por arquivo no log.
- `checks.json`: ST1 e ST2 (regressão, em `completed/`), ST3, ST4 e ST5.
- Regressão local: `node --test tools/verify-stitch.test.mjs`, com fixtures em `tools/fixtures/verify-stitch/` e um servidor local que devolve 403.
- Template do gate, README do contexto, índice e skill `dieta-bot-stitch` (4 pastas; `.agents` e `.hermes` são ignoradas pelo git e ficam sincronizadas só no disco).

Validação:

1. `verify-stitch st1` e `st2`: `PASSOU`. `aviso` em títulos repetidos do `home1` (as cópias sem uso já registradas no ST2) e nas divergências conhecidas abaixo.
2. Fixtures: "IA ATIVA" (`text not`), linha de treino 186 px além da borda (`fits`), imagem 403 (`images`), dark sem "PROTEÍNA" (coerência): as 4 falham como esperado; o par limpo passa.
3. `export-stitch --only chat0`: só `chat0` exportado nos dois temas, 0,000% de mudança, restaurado; `git status` sem PNG.
4. Export completo: os 40 PNGs voltaram ao git pelo filtro; `git status` sem PNG.
5. `check-stitch`: 40 golds verdes.
6. `--report` na fixture: prompt de correção com o título exato ("Estimate com botões de ação (V2 Expressive)", "Foto de refeição e estimativa no Chat (V2 Light)") e a lista "keep unchanged"; tela sem falha não ganha prompt. `--report` no ST2 real: 8 telas, sem mudança visual.
7. ST3–ST5: rodam e falham só por "no screen titled …" (telas novas ainda não criadas pelo dono); a `o3` atual passa.

Decisão tomada na implementação: a coerência dark × light achou divergências nos golds atuais, anteriores a esta ferramenta. Em vez de `exceptions` (diferença de propósito), elas entram em `coherence.known` com uma nota e aparecem como `aviso`, sem travar o gate:

- `homeX`: ícone `photo_camera` num nó da timeline só no light (já em `GOLD_CONFLICTS`).
- `chatF`: dark mostra "Dieta Bot AI" no lugar de "~780 kcal" (já registrado no ST1).
- `chatT`: rótulo do assistente "Dieta Bot AI" (dark) × "DIETA BOT INTELLIGENCE" (light). Novo.
- `chatP`: "DIETA BOT INTELLIGENCE" só no dark, "PRO" só no light. Novo.
- `chatG`: a bolha dark perde "A estimativa total é de:". Novo.

As três novas não têm origem conhecida (nada media isso antes). Corrigir no Stitch é passo do dono, num gate próprio, se ele quiser.
