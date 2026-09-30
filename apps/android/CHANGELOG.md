# Changelog — Dieta Bot (dev)

Notas das versões distribuídas pelo Firebase App Tester, da mais nova para a mais antiga. Cada seção é gravada por `tools/distribute-dev.ps1 -Notes` no commit `chore(release)`. Não editar à mão.

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
