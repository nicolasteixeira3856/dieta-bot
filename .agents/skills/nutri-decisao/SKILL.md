---
name: nutri-decisao
description: Registra decisão de feature ou de negócio do Nutri. Use quando uma decisão de produto fechou, o dono confirmou um rumo, ou a skill nutri-feature entregou a decisão aceita. Não use para bugfix, refactor ou detalhe de implementação.
---

Não leia a pasta `decisoes/` inteira. Não cole o histórico no prompt.

1. Leia só `DECISOES.md`.
2. Data de hoje em `dd/mm/yyyy`. Arquivo do dia: `decisoes/dd-mm-yyyy.md` (barras viram hífens).
3. Se o arquivo do dia não existe, crie com o título `# dd/mm/yyyy`.
4. Acrescente uma seção:
   - `## título curto`
   - Decisão, em uma frase.
   - Ficou de fora, em uma frase.
5. No `DECISOES.md`, uma linha nova no topo da lista, depois do parágrafo de instrução:
   `- dd/mm/yyyy — [título curto](decisoes/dd-mm-yyyy.md)`
6. Pare. Não abra os outros dias.
