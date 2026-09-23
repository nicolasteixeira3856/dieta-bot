# Contrato HTTP — /v1/estimate e /v1/fit

Auth: header `X-Invite: $INVITE_CODE`
Content-Type: application/json

O servidor NÃO calcula teto. O app manda orçamento quando for /fit.

## GET /health
`{ "ok": true, "model": "gpt-6-luna" }`

## POST /v1/estimate
IN
```json
{
  "local_time": "2026-09-23T12:35:00-03:00",
  "janela": "almoco",
  "text": "4 colheres de arroz, 1 concha de feijão, 1 bife, salada sem azeite",
  "image_b64": null,
  "assumptions": [{"alimento":"leite","modificador":"semidesnatado"}]
}
```
OUT
```json
{
  "kcal": 610, "p": 42, "c": 68, "g": 16,
  "confianca": "medio",
  "pergunta": "O bife era ~100 g ou ~150 g?",
  "itens": [{"nome":"arroz","g":80,"kcal":100}],
  "model": "gpt-6-luna"
}
```
`pergunta` só existe se confianca != alto.

## POST /v1/fit
IN
```json
{
  "mode": "want",
  "text": "quero hambúrguer caseiro",
  "itens_disponiveis": [],
  "orcamento": {"kcal": 455, "p": 49},
  "image_b64": null
}
```
OUT: prato com porções, cabe true/false, 1 pergunta, 2 opções no mode surprise.
Nunca oferecer o prato que explode o teto.

Timeout 20s. Foto não persiste.
