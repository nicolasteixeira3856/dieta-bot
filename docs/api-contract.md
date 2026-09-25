# HTTP contract — /v1/estimate and /v1/fit

Auth: header `X-Invite: $INVITE_CODE`
Content-Type: application/json

The server does not compute the ceiling. The app sends the budget on /fit.

## GET /health
`{ "ok": true, "model": "gpt-6-luna" }`

## POST /v1/estimate
IN
```json
{
  "local_time": "2026-09-23T12:35:00-03:00",
  "window": "lunch",
  "text": "4 colheres de arroz, 1 concha de feijão, 1 bife, salada sem azeite",
  "image_b64": null,
  "assumptions": [{"food":"leite","modifier":"semidesnatado"}]
}
```
OUT
```json
{
  "kcal": 610, "p": 42, "c": 68, "g": 16,
  "confidence": "medium",
  "question": "O bife era ~100 g ou ~150 g?",
  "items": [{"name":"arroz","g":80,"kcal":100}],
  "model": "gpt-6-luna"
}
```
`question` exists only if confidence != high.

## POST /v1/fit
IN
```json
{
  "mode": "want",
  "text": "quero hambúrguer caseiro",
  "available_items": [],
  "budget": {"kcal": 455, "p": 49},
  "image_b64": null
}
```
OUT: dish with portions, fits true/false, 1 question, 2 options in surprise mode.
Never offer a dish that blows the ceiling.

Timeout 20s. Photo is not persisted.
