# Plano — S7 Nome "Dieta Bot" no prompt do server

- Estado: Aguardando aprovação
- Data: 28/09/2026
- Contexto proprietário: `server`
- Código afetado: `server/llm.py` (texto da instrução do Chat), `docs/server/README.md`. Contrato HTTP inalterado.
- Pré-requisitos: [ADR-016](../../produto/adrs/ADR-016-nome-dieta-bot.md) aceito (pelo A13 ou por este plano, o que for aprovado primeiro). Independente do A12/A13 no código.

## Gate de autorização

Este plano é exclusivamente documental. A implementação só começa após aprovação explícita que identifique este arquivo:

> Aprovo o plano `docs/server/plans/s7-rename-prompt.md`. Implemente o plano aprovado.

## Objetivo

O modelo se apresenta como "Dieta Bot", igual ao app. Hoje a instrução diz "You are Nutri chat assistant."

## Escopo de implementação

1. `server/llm.py`, `_CHAT_INSTRUCTIONS`: "You are Nutri chat assistant." → "You are Dieta Bot, a meal-tracking chat assistant." Nenhuma outra frase muda.
2. **Fica** (IDs técnicos, ADR-016): loggers `nutri`, `/opt/nutri`, VM `nutri-api`, scripts de deploy.
3. Teste: um caso em `server/tests/test_chat.py` confirma que a instrução enviada ao modelo contém "Dieta Bot" e não contém "Nutri".
4. `./tools/deploy-gcp.ps1` + smoke: `/v1/chat` real com `X-Request-Id: s7-smoke`, e `pull-conversations.ps1 -RequestId s7-smoke` mostra a resposta.

## Validação planejada

1. `pytest -q` verde (54 = 53 + 1).
2. Deploy + smoke do passo 4.
3. Pergunta real "quem é você?" → a resposta usa "Dieta Bot" (registrada aqui).

## Fora de escopo

- Qualquer outra mudança de prompt ou shaping (a análise do "nao deu pra estimar" do S6 pode gerar outro plano).
- Renomear loggers, pastas ou a VM.

## Riscos e controles

- **Mudar o comportamento do modelo:** troca só a frase de identidade. O smoke e o log do S6 mostram a resposta real.

## Critérios de aceite

- Prompt com "Dieta Bot", testes verdes, smoke registrado.

## Encerramento

Depois da implementação, registre resultados reais e aplique o ciclo de vida em `docs/sdd/README.md`.

Só declaração explícita do dono cancelando este plano permite `Cancelado` e a pasta `plans/cancelled/`.
