---
name: nutri-lembrar
description: Grava uma lição curta na memória do papel quando o usuário corrige, um erro recorrente aparece, ou um teste falha e depois passa. Dispara no fim do turno, sem pedido de "lembra". Não use para segredo, .env, ou regra que já está no AGENTS.md.
---

Papéis: harper, iris, benjamin, lucas, pesquisador, qa, revisor.
Arquivo e teto: `tools/role_memory.py`.

Antes de spawnar um papel, cole no briefing a saída de:

```
python tools/role_memory.py show --role NOME
```

Se nessa conversa o papel errou e o caminho certo ficou claro, grave uma frase reutilizável:

```
python tools/role_memory.py add --role NOME --text "frase"
```

Se a mesma lição já existir, o script soma um uso. Se passar do teto, ele apaga as menos usadas e as mais frias.
Não grave stack trace, diff, data solta, nem valor de variável.
No máximo uma lição nova por papel por turno.
Se o filho devolver uma linha `LEMBRAR: ...`, use esse texto no `--text`.
