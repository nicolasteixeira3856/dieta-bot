---
name: kotlin-clean
description: Código Kotlin limpo no client Dieta Bot. Use quando criar classe, refatorar, nomear, tratar erro de rede ou quando o arquivo misturar camada.
---

# kotlin-clean

## Arquivo

- Uma responsabilidade. ViewModel não chama Retrofit.
- Nome: verbo no método (`logMeal`), substantivo na propriedade (`uiState`).
- Data class imutável no estado. Sem `var` no UiState.

## Camada

```
View → ViewModel → use case / domain → repository → service
```

Fórmulas só em `domain/`. Teste no mesmo módulo.

## Coroutine

- `viewModelScope` no VM. Sem `GlobalScope`.
- `SharingStarted.WhileSubscribed(5_000)` no `stateIn`.
- Timeout de rede 20s. Falha → confiança baixa + pergunta “descreve em 1 linha”.

## Erro

- Result / sealed no boundary da API. Não swallow.
- Sem `!!` em resposta de rede.

## Não

- Não copiar padrão Flutter/Cubit.
- Não logar INVITE_CODE nem URL com query secreta.
- Não commitar `.env`.
