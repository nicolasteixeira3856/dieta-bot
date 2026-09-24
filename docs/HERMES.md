# Hermes — outras IAs, e o Grok quando o assunto é o repo

Hermes Agent (Nous Research) é o lugar para trocar de modelo sem mudar o Nutri.

O time de chat foi aposentado. grok-cli é 1 agente. Hermes não despacha persona no tree.

Site: https://hermes-agent.nousresearch.com/docs

## O que copiar

As skills compartilhadas já estão em `.agents/skills/`.
Grok e Hermes leem essa pasta.

No repo do Nutri, uma vez:

```
hermes skills trust
```

Modelo da cadeira, qualquer um:

```
hermes model
```

Isso cobre Claude, Gemini, OpenAI, OpenRouter, Nous Portal, ou um endpoint compatível.
Não cole chave de provedor neste repo. A chave do Hermes fica em `~/.hermes/.env`.

## Patch no repo

Quando a decisão pedir patch no Nutri, Hermes chama o Grok no repo, sem outro agente e sem `.env`:

```
grok -p --prompt-file briefing.txt --deny "Read(.env)" --deny "Read(**/.env)" --no-subagents
```

`briefing.txt` leva a decisão, os arquivos permitidos e a prova esperada.
Não leva `OPENAI_API_KEY`, `TUNNEL_TOKEN` nem `INVITE_CODE`.

Outra IA entra do mesmo jeito: outro `hermes -p` com outro modelo, mesmo briefing, saída em markdown.
Não há sala compartilhada.

## Quando não chamar o Grok

Pergunta que ainda não vai virar patch: responde na sessão Hermes, um agente.
Pergunta que mexe no repo: o Grok, porque AGENTS.md e o código estão no repo dele.
