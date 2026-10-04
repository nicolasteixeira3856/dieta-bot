# Changelog — Dieta Bot (dev)

Notas das versões distribuídas pelo Firebase App Tester, da mais nova para a mais antiga. Cada seção é gravada por `tools/distribute-dev.ps1 -Notes` no commit `chore(release)`. Não editar à mão.

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
