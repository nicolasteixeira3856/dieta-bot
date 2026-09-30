# ADR-023 — Chat v2 (intenção, texto da refeição, plano) e Memória v2 (permanente + dinâmica)

- Estado: Aceito (30/09/2026, pelo dono, depois do S11; parte server no ar no dev, parte client nos A27, A28 e A29)
- Data: 2026-09-30
- Contexto: `produto`
- Substitui: parcialmente a [spec de memória](../specifications/memoria-push.md) (regras 2–4 da memória, vigentes desde o [A8](../../android/plans/completed/a8-memoria.md)) e a regra 12 da [spec do Chat](../specifications/chat.md). Adiciona golds `chatR`, `chatM` e `chatS` à lista do `AGENTS.md` (não remove nenhum). Pode mudar o `reasoning.effort` do `AGENTS.md` (decisão 6). Estende o `POST /v1/chat` do [contrato](../../api-contract.md) de forma aditiva.

## Contexto

Feedback do dono depois de uma semana de uso (30/09/2026). O Chat é o núcleo do produto e está abaixo do esperado:

1. **Texto errado na Home.** Café "igual ao de ontem" → a IA pergunta o tipo de leite → resposta "Sempre uso leite semi desnatado" → Gravar. A Home mostrou "Sempre uso leite semi desnatado (435 kcal)". Causa: o texto gravado é a última mensagem do usuário antes da estimativa (`ChatViewModel.descriptionOf`). Depois de uma pergunta, essa mensagem é a resposta, não a refeição.
2. **Card de registro numa pergunta de receita.** "Vou fazer pizza de pão sírio na janta, quantas gramas de cada item?" → a IA respondeu as gramas e também mostrou o card com Gravar. Causa: o prompt manda estimar comida "eaten or about to be eaten", e toda estimativa gera card.
3. **Memória virou dump.** O `MemoryStore` acrescenta uma linha a cada Gravar e a cada pergunta respondida, e corta as mais velhas em 4000 caracteres. Não há extração, frequência nem esquecimento: a memória é um diário que repete o histórico. O dono quer uma IA "que cresce com o usuário": lembra hábitos que se repetem (café quase sempre igual, tipo de leite, marca de iogurte) e esquece o que some.
4. **Sem aviso de memória.** O usuário não sabe quando algo entrou na memória nem quando a IA usou a memória.

O `reasoning.effort` está em `none` desde o início (`AGENTS.md`). Não há conjunto de avaliação: toda mudança de prompt é validada à mão.

## Decisão

### 1. Intenção de cada mensagem

A IA classifica cada mensagem numa de três intenções:

| Intenção | Exemplos | Estimativa | O que o Chat mostra |
|---|---|---|---|
| `log` | "comi", "almocei", "café foi o mesmo de ontem", foto de prato comido | sim | card de estimativa com Gravar/Trocar/Pular (como hoje) |
| `plan` | "vou fazer pizza, quantas gramas?", "o que como na janta?", "cabe um açaí?" | sim | resposta de plano (decisão 3), sem Gravar/Trocar/Pular |
| `question` | "whey antes ou depois do treino?", "oi" | não | só a bolha |

Na dúvida entre `log` e `plan`, verbo no passado ou "comi/tomei" é `log`; futuro, condicional ou pedido de quantidade é `plan`.

### 2. Texto consolidado da refeição

Toda estimativa (`log` e `plan`) traz `meal_text`: a refeição inteira em pt-BR, com os alimentos e quantidades já corrigidos pela conversa, sem comentário, até 160 caracteres. Ex.: "2 ovos mexidos, 1 pão francês c/ manteiga, 200 ml leite semidesnatado, 100 ml café". É esse texto que vai para a Home ao gravar, em vez da mensagem do usuário. A foto também passa a usar `meal_text` (substitui a regra 12 do Chat).

### 3. Resposta de plano

- O texto da IA traz as gramas de cada item, o preparo em poucas linhas quando for receita, e o total do prato em kcal e P/C/G. A IA monta o plano em cima do que sobra no dia (`day.remaining_kcal`, calculado pelo app).
- Abaixo da bolha, o app mostra o **dia projetado**, calculado localmente e nunca pela IA: `Dia: {comido} → {comido + prato} de {teto efetivo} kcal` e uma linha de macros `P {x}/{alvo} · C {y}/{alvo} · G {z}/{alvo}` (valores depois do prato). Acima do teto efetivo, a linha de kcal fica em `bad`.
- Botão **Registrar assim**: grava a estimativa do plano no slot sugerido, pelo mesmo fluxo do Gravar (confirmação de Substituir do [ADR-017](ADR-017-registro-consolidado.md) incluída). Sem slot sugerido, abre o Trocar.
- Gold novo `chatR`.

### 4. Memória v2: permanente e dinâmica

A memória passa a ser uma lista de **fatos** estruturados, no aparelho, cifrada como hoje (AES-256-GCM, gravação atômica). O servidor continua sem estado: a IA só **propõe** mudanças (`memory_updates`) e o app aplica, com as regras abaixo.

Cada fato: `id` estável (`P3`, `D12`), `kind` (`permanent` | `dynamic`), `category` (`preference` | `portion` | `routine`), `key` curta (`leite`, `iogurte`, `cafe`), `text` em pt-BR (≤ 160 caracteres), `slot` (só rotina), dias em que foi visto e, na rotina, kcal/P/C/G do último registro.

| | Permanente | Dinâmica |
|---|---|---|
| Entra por | frase explícita ("sempre uso…", "lembra que…", "não uso mais…") ou promoção | observação: marca, tipo, porção ou rotina que aparece num registro |
| Limite | 30 fatos | 40 fatos |
| Sai por | só pedido explícito ("esquece o pão francês") | 21 dias sem aparecer, ou limite (sai o visto há mais tempo) |
| Contradição | "agora uso leite integral" substitui o fato de mesma `key` na hora, sem perguntar | idem |

- **Promoção:** fato dinâmico visto em **5 dias diferentes dentro de 21 dias** vira permanente. Com a permanente cheia, a promoção espera, em silêncio, até abrir vaga.
- **Permanente cheia e frase explícita nova:** a IA não grava e pergunta "Minha memória fixa está cheia. Esqueço {fato menos visto}?". A resposta do usuário gera `remove` + `add`.
- **Rotina:** `add`/`reinforce` de rotina só valem quando a refeição é **gravada** (Gravar, Registrar assim, Substituir, registro rápido). Estimativa não gravada não conta como hábito. Preferência e porção valem na hora da resposta.
- **Memória inicial vazia para todo mundo.** Na atualização, a memória de texto antiga é apagada, sem conversão.
- **Visibilidade:** só no editor dev (`Memória da IA (dev)`, [ADR-019](ADR-019-ferramentas-dev.md)), até o dono decidir o contrário.

### 5. Histórico recente no prompt

O "mesmo de ontem" deixa de depender da memória. Todo turno leva `recent`: as refeições gravadas nos **últimos 7 dias** (sem hoje), lidas do Room, com data, slot, `text`, kcal e P/C/G.

### 6. Avaliação e `reasoning.effort`

- Nasce um conjunto de avaliação do Chat no `server/evals/`, com casos escritos a partir do log de conversa do dev ([ADR-015](../../server/adrs/ADR-015-log-conversa-dev.md)). Cada feedback do dono vira caso novo.
- O prompt novo roda com `none` e com `low`, 3 repetições por caso. **`low` é adotado** se tiver pelo menos 10 pontos percentuais a mais de casos aprovados e latência p95 ≤ 20 s. Senão, fica `none`. O resultado é registrado no plano [S11](../../server/plans/pending_manual_validation/s11-chat-v2.md). Se `low` vencer, o S11 atualiza a linha LLM do `AGENTS.md` e a spec [v1-chat](../../server/specifications/v1-chat.md).

### 7. Avisos de memória no Chat

- **`Memória atualizada`:** selo abaixo da bolha da IA (ou do recibo, quando a atualização veio com o registro) sempre que o app aplicou pelo menos uma mudança de memória naquele turno.
- **Origem:** chip `Memória permanente` e/ou `Memória dinâmica` abaixo da bolha quando a resposta usou fatos da memória (`memory_used`, ids devolvidos pela IA e conferidos pelo app).
- Gold novo `chatM`.

### 8. Sugestão da rotina

- Ao abrir o Chat, se o slot da hora está vazio (sem registro nem pulo) e existe uma rotina forte para ele (permanente, ou dinâmica vista em pelo menos 3 dias), aparece um card no fim da conversa: `O de sempre no {slot}?`, o texto da rotina, `{kcal} kcal · {P}P · {C}C · {G}G` (do último registro), o chip de origem e dois botões:
  - **Registrar:** grava direto com os números da rotina, sem chamar a IA, com recibo e reforço da rotina.
  - **Quase igual:** coloca o texto da rotina no composer, foca e abre o teclado. O usuário edita e envia pelo fluxo normal.
- O card some quando o usuário envia qualquer mensagem ou quando o slot é gravado ou pulado. Não é gravado na conversa nem enviado à IA.
- Gold novo `chatS`.

### 9. Gate

Um único gate Stitch, [ST6](../../stitch/plans/st6-chat-v2.md), cria `chatR`, `chatM` e `chatS` nos dois temas.

## Motivação

- O texto da Home e a separação entre plano e registro são defeitos de montagem, não da IA. Resolvê-los no contrato (`meal_text`, `intent`) é barato e resolve o problema na raiz.
- Separar histórico (dado exato, já no Room) de memória (hábitos) deixa a memória curta e útil. A memória para de repetir o que o prompt já leva.
- Permanente e dinâmica seguem o pedido do dono: o que ele disse explicitamente não some; o que só apareceu uma vez some sozinho.
- O servidor continua sem estado ([ADR-013](../../server/adrs/ADR-013-gcp-host.md), e2-micro): a memória fica no aparelho, cifrada.
- Conta do dia projetado no app: numa conta, a IA pode errar; o app não, e o número bate com a Home.
- A avaliação troca "achismo" por número, e cada feedback vira teste de regressão.

## Consequências

### Positivas

- A Home mostra a refeição, não a resposta a uma pergunta.
- Perguntar uma receita não abre registro. Registrar um plano é um toque.
- A memória cresce e encolhe com o uso, e o usuário vê quando ela muda e quando foi usada.
- Café de todo dia em um toque.

### Negativas

- Prompt maior: `recent` (até ~7 k caracteres) e fatos (até 70 × 160). Aceito pelo dono.
- Três golds novos por tema: 24 → 27.
- Room v4 → v5 (colunas novas em `chat_message`) e formato novo do arquivo de memória. Os testers perdem a memória antiga. O `recent` cobre o "mesmo de ontem" desde a primeira versão.
- APKs antigos (até 0.0.3) continuam funcionando: todos os campos novos são opcionais. Para eles, o servidor devolve `estimate: null` em `plan` (sem card; as gramas e o total vêm no texto).
- A IA pode classificar errado, ou propor memória errada. O conjunto de avaliação mede isso, e o editor dev corrige.

## Alternativas consideradas

- **Só melhorar o prompt, sem campos novos:** não resolve o texto da Home (o app não sabe qual mensagem é a refeição) nem o card do plano (o app não sabe a intenção).
- **Memória reescrita inteira pela IA a cada turno:** a IA vira dona do estado, pode apagar o que não devia, e cada turno fica mais caro. Rejeitada: a IA propõe, o app aplica com regras fixas.
- **Consolidação diária por chamada extra à IA:** o reforço por turno, com ids, cobre as rotinas sem uma chamada a mais. Pode voltar se o reforço falhar na avaliação.
- **Memória no servidor:** fere o servidor sem estado e o e2-micro.
- **Baixar o reasoning:** já está no mínimo (`none`). Só dá para testar para cima.
- **Dia projetado calculado pela IA no texto:** risco de conta errada. Rejeitado pelo dono em favor do app.

## Relações

- Especificações afetadas: [chat](../specifications/chat.md), [memoria-push](../specifications/memoria-push.md), [v1-chat](../../server/specifications/v1-chat.md), [api-contract](../../api-contract.md).
- ADRs relacionados: [ADR-012](ADR-012-chat-home-perfil.md), [ADR-015](../../server/adrs/ADR-015-log-conversa-dev.md), [ADR-017](ADR-017-registro-consolidado.md), [ADR-019](ADR-019-ferramentas-dev.md).
- Planos: [S10](../../server/plans/completed/s10-avaliacao-chat.md), [S11](../../server/plans/pending_manual_validation/s11-chat-v2.md), [A27](../../android/plans/a27-chat-v2-texto-intencao.md), [A28](../../android/plans/a28-memoria-v2.md), [ST6](../../stitch/plans/st6-chat-v2.md), [A29](../../android/plans/a29-chat-v2-interface.md).

Depois de aceito, este ADR não se edita. Mudança posterior exige ADR novo que declare a substituição.
