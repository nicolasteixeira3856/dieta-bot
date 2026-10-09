# Changelog — Dieta Bot (dev)

Notas das versões distribuídas pelo Firebase App Tester, da mais nova para a mais antiga. Cada seção é gravada por `tools/distribute-dev.ps1 -Notes` no commit `chore(release)`. Não editar à mão.

## 0.0.22 — 09/10/2026

### Novidades

- Treino pelo Chat: escreva, por exemplo, "treino de hoje 450 kcal" e a Tali grava o treino do dia com um recibo e o botão Desfazer. "Mais 200 kcal de treino" soma ao que já estava. A Home mostra o mesmo número e o crédito na meta.
- Várias coisas numa mensagem só: "café 2 ovos, almoço arroz e frango, pulei o lanche" vira um recibo para cada refeição e para o pulo. Desfazer em qualquer um desses recibos desfaz todos de uma vez. "Jantei frango, me sugere o lanche" registra o jantar e deixa a sugestão do lanche pronta para registrar ou reservar.
- Opções no plano: quando a Tali sugere duas opções, cada uma aparece num quadro próprio com os ingredientes, as calorias e os botões Registrar e Reservar. Também dá para responder "fiz a 2".
- Primeira conversa: com a memória vazia, um "oi" faz a Tali perguntar seu café, almoço, jantar e preferências de costume (dá para pular). O que você responder fica guardado.
- Receitas: embaixo de uma receita da Tali aparece "Salvar receita". As receitas salvas ficam em Configurações → Receitas, onde dá para abrir e excluir. Depois é só pedir "lembra a receita X?" ou dizer "jantei a X" para registrar com os números salvos.
- O que a Tali sabe: em Configurações há uma tela nova com tudo o que a Tali guardou sobre você (preferências, rotinas, lembretes temporários), com a origem de cada item. Dá para corrigir o texto ou apagar. Um item apagado não volta sozinho.

### Ajustes

- A Tali agora lembra que você tem air fryer, panela de pressão ou balança, e também os pratos de que você disse ter gostado.

## 0.0.21 — 08/10/2026

### Novidades

- Saldo do dia no recibo: depois de cada registro, o recibo mostra quanto você já comeu do teto e quanto falta de proteína (por exemplo, "1.240 de 2.000 kcal · faltam 62 g de proteína").
- Quando a Tali pergunta se pode registrar, aparece embaixo a projeção do dia com aquela refeição.
- "Anotado": quando você conta uma preferência fixa (por exemplo, "uso leite semidesnatado"), a Tali mostra embaixo da resposta o que guardou, do jeito que ficou salvo.

### Ajustes

- Se a resposta demora mais de 4 segundos, o balão de espera passa a dizer "Tali está pensando…".
- A Tali agora recebe os totais dos últimos 7 dias e os números das suas rotinas, o que ajuda em pedidos como "almocei o mesmo de ontem".

## 0.0.20 — 08/10/2026

### Ajustes

- Tema claro: os textos mais apagados ficaram mais escuros e fáceis de ler, como o horário das mensagens no Chat, o contador de refeições na tela inicial e os exemplos dentro dos campos do cadastro.
- Configurações → Distribuição das refeições: o modo escolhido ao lado de "Horários das refeições" (por exemplo, Seg–Sex · Sáb–Dom) agora usa a mesma cor dos outros textos de apoio.

## 0.0.19 — 07/10/2026

### Novidades

- Copiar mensagens, como no WhatsApp: segure uma mensagem (sua ou da Tali) para selecionar, toque em outras para juntar e use o botão Copiar no topo. O texto sai limpo, sem asteriscos, na ordem da conversa.

### Ajustes

- Os botões de uma resposta (Registrar, Registrar assim, Reservar, Pode passar · Ajustar para caber, Forçar estimativa) agora ficam logo abaixo da mensagem e rolam junto com a conversa, em vez de ficarem presos acima da caixa de texto.
- O onboarding mostra o passo certo em todas as telas: de 1/5 a 5/5, com cinco segmentos.
- Captura de tela com rolagem (Android 12 ou mais novo) conferida na Home, no Chat e nas Configurações. Se a opção de capturar mais não aparecer no seu celular, conte o modelo e a versão do Android.

## 0.0.18 — 07/10/2026

### Novidades

- Tom da Tali: escolha como ela fala com você. **Seco** mostra só os números; **Duro** cobra o que estourou e o que faltou, sem rodeio. A escolha aparece no fim do cadastro e pode ser trocada em Configurações → Tom da Tali.
- Fechamento do dia às 22h: uma notificação e um cartão na tela inicial com as calorias, os macros, as refeições que faltaram e um texto curto no tom escolhido. No domingo vem também o fechamento da semana. Sem internet, o cartão mostra só os números e o texto chega na próxima vez que você abrir o app.
- Respostas mais fáceis de ler: números importantes em negrito, opções em lista, passos numerados e uma tabela de porções nas receitas.
- Reservar um plano: quando a Tali sugere um prato para uma refeição, o botão **Reservar** guarda esse plano. A refeição aparece como planejada na linha do tempo e, quando você registra o que comeu, o recibo mostra a diferença para o plano.
- Quando um plano passa do que sobra no dia, você escolhe: **Pode passar** ou **Ajustar para caber**.
- Pular refeição junto com outra coisa: dá para dizer numa mesma mensagem "pulei o almoço" e o que comeu em outra refeição; o Chat registra uma e pula a outra, cada uma com o seu recibo e Desfazer.
- Pular uma refeição que já tinha registro agora pergunta antes: **Excluir e pular** ou **Manter registro**.

### Ajustes

- O cadastro agora tem 5 telas; o botão da tela de macros passou a ser **Continuar**.

## 0.0.17 — 06/10/2026

### Correções

- Chat: depois de excluir uma refeição que o Chat registrou, a conversa não trata mais essa refeição como registrada. Antes, isso podia fazer as próximas mensagens responderem "não deu pra estimar".
- Chat: quando uma refeição não pode ser registrada sozinha, a resposta agora mostra "Não registrado", em vez de ficar sem aviso.

## 0.0.16 — 06/10/2026

### Novidades

- Configurações: nova opção "Resetar app", no fim da tela. Ela apaga tudo o que o app guarda no aparelho (perfil, metas, refeições, registros, conversa, memória e lembretes) e volta para a configuração inicial. Antes, pede confirmação; não dá para desfazer.
- Tela inicial: tocar num card de refeição sem registro agora abre o Chat para registrar.

### Ajustes

- Para pular uma refeição, segure o card (toque longo). O celular vibra e pede a mesma confirmação "Pular {refeição}?" de antes. O texto do card agora diz "Toque para registrar, segura para pular".

## 0.0.15 — 06/10/2026

### Novidades

- Logo nova: uma semente de aveia estilizada, com um rostinho discreto. Ela aparece no ícone do app, no ícone temático do Android 13 e na abertura.

### Ajustes

- A abertura agora mostra só o logo, centralizado e maior. O nome e o aviso de estimativa saíram da abertura; o aviso continua na tela inicial.

## 0.0.14 — 05/10/2026

### Novidades

- O app agora se chama Fibrai. O nome aparece no ícone, na abertura e na configuração inicial.
- A assistente do Chat agora é a Tali, com foto no topo do Chat e ao lado de cada resposta.

### Ajustes

- Esta versão atualiza o app novo instalado na versão anterior. Se o "Dieta Bot Dev" antigo ainda estiver no celular, pode desinstalar: ele não recebe mais atualizações.

## 0.0.13 — 05/10/2026

### Importante: app novo

- Esta versão é um app novo. Instale pelo Firebase App Tester e depois desinstale o "Dieta Bot Dev" antigo (os dois aparecem com o mesmo nome até a próxima versão).
- O app novo começa do zero: você refaz a configuração inicial. Refeições, memória e conversa do app antigo não passam para o novo.

### Novidades

- Acrescentar um alimento a uma refeição já registrada agora mostra o item novo separado do total: a estimativa traz só o que você acrescentou (com "+"), e embaixo aparecem o que já estava registrado e o total da refeição. Nada muda até você tocar em Adicionar.
- Em Escolher outra refeição, só o acréscimo vai para a refeição escolhida; a refeição original fica como estava. Se a escolhida já tiver registro, o Chat pergunta de novo com os números dela.
- Correções de uma refeição perguntam "Atualizar {refeição}?" com o valor de antes e o novo total. Cancelar não muda nada.

### Correções

- Um acréscimo não copia mais a refeição inteira para outra refeição nem soma o mesmo alimento duas vezes.
- Uma proposta que ficou velha (outra mensagem, virada do dia, mudança na refeição) aparece como "Não registrado" e não grava nada.

## 0.0.12 — 04/10/2026

### Correções

- Ao editar um número ou o nome de uma refeição (configuração inicial e Configurações), o cursor agora vai para o fim do valor. Antes, tocar para mudar "200" de carboidrato e digitar 5 virava "5200".
- O campo que você está digitando sempre aparece acima do teclado. Antes, na meta de fim de semana (Metas separadas) e na meta por dia, o teclado cobria o campo.
- Na meta de calorias das Configurações, com uma meta por dia, a janela de edição não passa mais do topo da tela quando o teclado abre: o título e o Salvar continuam visíveis.

### Ajustes

- Com o teclado aberto, o botão de baixo (Continuar / Salvar) fica atrás do teclado e volta quando ele fecha.

## 0.0.11 — 04/10/2026

### Novidades

- Visual novo em todas as telas: abertura, configuração inicial, painel do dia, Chat, Configurações, diálogo de reinício do dia e lembrete. Cartões de vidro translúcido sobre um fundo em degradê azul, nova fonte, ícones novos e números mais legíveis, nos temas claro e escuro (o app segue o tema do sistema).

### Ajustes

- Nada muda no funcionamento: as mesmas telas, botões, textos, números e regras de antes.
- Em celulares com Android 11 ou anterior, o vidro aparece mais opaco, sem o desfoque do fundo.
- O app ficou cerca de 7 MB menor.

## 0.0.10 — 03/10/2026

### Novidades

- Mandou o rótulo de um produto (foto ou digitado) que vai comer depois? A IA guarda os números por 3 dias e usa nas estimativas desses dias, mesmo que você coma em várias vezes. Registrar, Excluir, Trocar refeição, Editar ou Desfazer não apagam esse rótulo. "Esquece a lasanha" tira na hora.
- Depois de uma estimativa ou de uma pergunta da IA, "registra na refeição de hoje" grava na refeição que ela sugeriu, sem perguntar qual.

### Correções

- Em conversas longas, a resposta a uma pergunta da IA não se perde mais: a pergunta e a refeição dela continuam na conversa.

### Ajustes

- Depois de instalar esta versão, não volte para uma versão anterior: a memória da IA pode não abrir nela.

## 0.0.9 — 02/10/2026

### Ajustes

- No Chat, tocar em enviar fecha o teclado. A resposta e o recibo aparecem inteiros, sem o teclado por cima.

## 0.0.8 — 02/10/2026

### Novidades

- Refeição de hoje mandada no Chat é registrada sozinha quando a IA tem certeza. Logo abaixo aparece um recibo com a refeição, o horário e as kcal.
- O recibo mais recente de cada refeição tem ações: Desfazer, Excluir, Trocar refeição e Editar. Editar devolve o texto para a caixa de mensagem para você corrigir e mandar de novo. Refeição registrada por foto não tem Editar.
- Desfazer volta a refeição como estava antes, inclusive o que a IA tinha aprendido com ela.
- Quando a IA não tem certeza, aparece só um botão "Registrar". Ele some quando você manda a próxima mensagem.
- Se a refeição já tinha registro, o Chat pergunta antes de trocar: "Substituir {refeição}?", com o valor antigo e o novo. "Outra refeição" deixa escolher onde gravar.
- "Pulei o café" ou "hoje não vou jantar" marcam a refeição como pulada, com Desfazer no recibo. "Acho que não vou jantar" ou "ainda não almocei" não marcam nada.

### Ajustes

- A barra Gravar | Trocar | Pular saiu do Chat. Estimativas antigas ficam sem botões.
- Foto de prato com uma pergunta, como "isso tem muita caloria?", só responde e oferece "Registrar". Não grava sozinha.

## 0.0.7 — 01/10/2026

### Novidades

- Na primeira tela do cadastro, idade, altura e peso agora são obrigatórios. Enquanto faltar algum, o modo do teto e a meta diária ficam apagados e aparece o aviso "Preencha idade, altura e peso para ver a meta sugerida." Ao completar os três, a meta sugerida já vem preenchida.
- O teclado segue o formulário: "Próximo" vai de idade para altura e de altura para peso, e "Concluído" no peso fecha o teclado. Nas metas por dia, "Próximo" passa de segunda a domingo.

### Ajustes

- O Chat abre direto na última mensagem, sem rolar a conversa toda.
- Conversas longas carregam 20 mensagens por vez ao rolar para cima, até 60 dias atrás.
- Tocar na câmera ou em "Tirar foto do prato" fecha o teclado antes de abrir a escolha da foto.

## 0.0.6 — 01/10/2026

### Novidades

- O app passa a mandar ao servidor um identificador anônimo desta instalação, como descrito no aviso do teste fechado. Ele serve só para ligar pedidos abusivos a uma instalação, sem nome, e-mail ou telefone. Fica no aparelho e some ao desinstalar ou limpar os dados do app.

### Ajustes

- Nenhuma tela mudou. Registrar refeição, foto e perguntas funcionam como antes.

## 0.0.5 — 30/09/2026

### Novidades

- Quando a IA tem dúvida sobre uma refeição que você comeu, ela pergunta antes de estimar: aparece só a pergunta, com todas as dúvidas juntas, sem número nem botões de gravar. A estimativa vem uma vez só, depois das respostas.
- No máximo 3 rodadas de perguntas: na quarta mensagem a estimativa sempre chega.
- A partir da segunda pergunta aparece o botão "Forçar estimativa": um toque e a IA estima com o que já sabe.

### Correções

- Não aparece mais uma estimativa provisória que depois era trocada por outra para a mesma refeição.
- A IA passa a ver as próprias perguntas no histórico da conversa, então não repete pergunta já feita.

### Ajustes

- A pergunta da IA ficou com letra um pouco maior, no balão com a barra dourada.

## 0.0.4 — 30/09/2026

### Novidades

- Pergunte antes de comer: "vou fazer uma pizza de pão sírio, quantas gramas?" traz as quantidades e mostra como o dia fica com esse prato (Dia: comido → depois do prato, de quanto é a meta, e P/C/G). Se for comer assim, toque em "Registrar assim".
- A IA agora tem memória de hábitos: o que você diz que sempre faz ("sempre uso leite semidesnatado") fica guardado, e o que se repete nos registros (o café de todo dia) ela aprende sozinha. O que some por 3 semanas é esquecido.
- Avisos de memória no Chat: "Memória atualizada" quando a IA guardou algo novo, e "Memória permanente" / "Memória dinâmica" quando a resposta usou o que ela lembra.
- "O de sempre": depois de 3 dias registrando o mesmo café, ao abrir o Chat no horário do café aparece um cartão com ele. "Registrar" grava direto; "Quase igual" coloca o texto no campo para você ajustar.
- Perguntas como "quantas calorias tem uma banana?" só respondem, sem abrir registro.

### Correções

- O texto que vai para a Home é a refeição inteira, já corrigida pela conversa, e não mais a sua resposta a uma pergunta da IA.
- "Igual ao de ontem" funciona: a IA vê os registros dos últimos 7 dias.
- Nome da refeição na mensagem ("no almoço…") vale mais do que a semelhança com outra refeição na hora de sugerir onde gravar.

### Ajustes

- A memória antiga da IA foi apagada nesta versão; ela recomeça vazia e aprende de novo com o uso.

## 0.0.3 — 29/09/2026

### Novidades

- Refeições por dia da semana: em Configurações → Horários das refeições, escolha "Todos os dias", "Seg–Sex · Sáb–Dom" ou "Cada dia". A Home, o Chat e os lembretes seguem as refeições do dia. Quem já usa o app continua com as mesmas refeições de antes.
- Treino direto da Home: toque em "Treino de hoje", informe o kcal e veja na hora quanto ele soma na meta.
- Horário das refeições em rodas de hora e minuto (estilo alarme), que giram sem parar, no cadastro e nas Configurações.
- Foto no Chat vira anexo: aparece uma prévia no campo de mensagem, dá para escrever junto e ela só vai quando você toca em enviar.
- Mensagens de até 2000 caracteres no Chat. Acima disso, o campo fica vermelho com "Texto muito longo" e não envia.
- Ferramenta de teste: Configurações → "Memória da IA (dev)" mostra e permite editar o que o app manda para a IA (perfil e memória). Não apaga nada.

### Correções

- Uma refeição tem um registro só: gravar de novo numa refeição já registrada pede confirmação e substitui, em vez de somar em duplicidade.
- A IA agora estima a refeição inteira e sugere em qual refeição gravar.
- Enter do teclado pula linha no Chat, em vez de enviar sem querer.
- Fotos grandes não dão mais "Foto grande demais.": o app reduz a foto antes de enviar.
- Textos longos não são mais cortados em silêncio.

### Ajustes

- Pergunta da IA aparece numa bolha própria, com destaque; todas as bolhas têm o mesmo formato; saiu o selo "IA ATIVA".
- Toques com efeito visual e vibração leve (segue a vibração ao toque do sistema).
- Botões Salvar e Cancelar do mesmo tamanho nas janelas de edição, com mais respiro no fim das listas.
