# Hermes — outras IAs, e o Grok quando o assunto é o repo

Hermes Agent (Nous Research) é o lugar para trocar de modelo sem mudar o Nutri.
O app Flutter continua no grok-cli. Hermes não substitui o APK.

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

## Mesa

`/nutri-feature` na sessão Hermes carrega o mesmo procedimento do grok-cli.
Hermes faz a rodada de opinião (pesquisa, produto, tela) com o modelo que estiver ativo.

A memória dos papéis é a mesma pasta `.grok/memory/roles/`. No fim de um turno em que um papel errou e acertou, rode `python tools/role_memory.py add`. O script esquece o que pouco volta. Hermes não cria outra memória paralela.

Quando a decisão pedir patch no Nutri, Hermes chama o Grok no repo, sem subagentes e sem `.env`:

```
grok -p --prompt-file briefing.txt --deny "Read(.env)" --deny "Read(**/.env)" --no-subagents
```

`briefing.txt` leva a decisão, os arquivos permitidos e a prova esperada.
Não leva `OPENAI_API_KEY`, `TUNNEL_TOKEN` nem `INVITE_CODE`.

Outra IA entra do mesmo jeito: outro `hermes -p` com outro modelo, mesmo briefing, saída em markdown.
Hermes compara os textos. Não há sala compartilhada.

## Quando não chamar o Grok

Pergunta de produto que ainda não vai virar patch: a mesa Hermes basta.
Pergunta que mexe em `lib/` ou `server/`: o Grok, porque AGENTS.md e o código estão no repo dele.
