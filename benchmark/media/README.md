# Fotos do benchmark

Até 10 casos usam foto (`cases/*.json` com o campo `image`). O arquivo deve estar nesta pasta com o nome
exato listado abaixo, JPEG, lado maior ≤ 2048 px (o runner redimensiona e remove EXIF antes de enviar,
como o app faz, ADR-018). Casos cuja foto não existir são pulados pelo runner e contados como `skipped`
no relatório, sem consumir disparos.

| Arquivo | Caso | O que a foto deve mostrar |
|---|---|---|
| `prato-pf.jpg` | `photo-pf-sem-texto` | Prato feito: arroz, feijão, carne, legumes. Sem texto na mensagem. |
| `prato-pf-farinha.jpg` | `photo-pf-farinha-sementes` | Prato com uma colher de farinha de sementes por cima (o que parece farofa). |
| `mamao.jpg` | `photo-mamao-150g` | Mamão em pedaços, ~150 g. |
| `chocolate-hershey.jpg` | `photo-chocolate-quadrado` | Barra Hershey Special Dark 73 % com um quadrado separado. |
| `cerveja-lata.jpg` | `photo-cerveja-lata` | Lata de cerveja 350 ml (sem tabela nutricional visível). |
| `espetinhos.jpg` | `photo-espetinhos` | Espetinhos de carne assados (alguns no prato). |
| `rotulo-lasanha.jpg` | `photo-rotulo-lasanha` | Tabela nutricional de uma lasanha congelada (kcal e proteína por 100 g legíveis). |
| `pizza-pao-sirio.jpg` | `photo-pizza-pronta` | Pizza de pão sírio pronta na air fryer. |
| `monster-lata.jpg` | `photo-monster-zero` | Lata de energético zero açúcar. |
| `sem-comida.jpg` | `photo-sem-comida` | Qualquer foto sem comida (deve ser recusada como out_of_scope). |

Nenhuma foto é versionada no repositório: esta pasta tem um `.gitignore` que ignora `*.jpg`.
