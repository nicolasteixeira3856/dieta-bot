# Melhorias do Chat — brainstorm de 07/10/2026

Este arquivo registra um brainstorming do owner com agentes de IA sobre o Chat do Fibrai (Tali). Ele não é um plano SDD nem um ADR: é material de discovery. Nenhum código muda a partir dele sem um plano aprovado (`docs/sdd/README.md`).

Estrutura:

1. [Prompt do owner](#1-prompt-do-owner) — íntegra, sem abreviação.
2. [Resposta — claude-fable-5-1 (effort: high)](#2-resposta--claude-fable-5-1-effort-high) — primeira avaliação.
3. [Instruções para outros agentes](#3-instruções-para-outros-agentes).
4. [Avaliações de outros agentes](#4-avaliações-de-outros-agentes) — uma seção por modelo (e effort, quando aplicável).

---

## 1. Prompt do owner

> Gostaria de fazer um brainstorming sério e puxado com você, quero continuar melhorando o nosso chat. Como já mencionei anteriormente, eu tive uma boa experiência usando o Grok Bot e a minha vontade de fazer esse app surgiu exatamente de lá, trazer a boa experiência que tive com o bot para usuários numa interface amigável. 
>
> Eu sei que hoje faltam duas coisas no nosso chat: 
>
> - Criatividade na hora de sugerir refeições
> - Precisão, texto bem formatado, etc, coisas que ajudam e melhoram a experiência do usuário enquanto ele usa
>
> Hoje eu tenho algumas limitações
>
> - O app depende do usuário manter um bom uso dele para começar a aprender as coisas e fazer acontecer, se eu uso por 1 dia e no outro eu entro e mando "mesmo café de ontem", ele não entende. Isso é um problema de chat/contexto que deve ser aprimorado, não deve ficar restrito a essa mensagem/intenção/ação. O app precisa trafegar um contexto maior (ex, um resumo das refeições dos últimos 7 dias) para a IA decidir e se resolver.
>
> - Ainda sobre o ponto de cima, isso é um produto, será vendido, o usuário espera que o app funcione bem desde o dia 1. Hoje ele demora para aprender os costumes do usuário e entregar a experiência desejada. 
>
> - O app não consegue registrar/não interage bem com múltiplas ações ao mesmo tempo. É algo parcialmente corrigido, hoje o meu tester foi pular o pré treino e registrar o café da manhã no mesmo registro. Não conseguiu. Isso já foi resolvido. Mas esse é um caso isolado. E múltiplas ações, como fica? E se o usuário quiser registrar tudo que ele comeu num dia numa mensagem só? E se ele quiser registrar o que comeu de janta e lanche da tarde e pedir uma sugestão de janta logo em seguida? Todas essas ações são esperadas e devem funcionar bem. 
>
> - Eu não consigo chegar no chat e mandar para ele registrar quantas kcal eu queimei no treino, isso deveria ser possível
>
> - Ao sugerir refeições, seria bacana se o app tivesse uma opção de salvar essas refeições, o nome da refeição, kcal, macros, ingredientes e como fazer (meio que a resposta do chat) poderiam ficar salvos no celular. Ele pode tanto ver através de um menu específico como ter um local no app aonde ele vai só para listar receitas salvas por nome e ordem decrescente (da mais recente para a mais antiga), tem a opção de pressionar e abrir elas numa tela com a receita completa. Também deve ser possível para ele chegar no chat e falar "Lembra daquela receita que tinha X e Y que você me mandou? Qual era mesmo?", "Lembra para mim qual era a receita X", "Hoje jantei a receita Z, registra para mim"
>
> Tem outros pontos possíveis de melhoria no produto, hoje eu enxergo o seguinte: 
>
> - Precisamos de alguma melhoria no fluxo de onboarding, só não sei lhe dizer qual é, algo que dê para a IA interpretar e descobrir mais facilmente quem e como é o usuário que vai utilizar o app, para que o fluxo de "aprender com o usuário" seja capaz de fornecer uma experiência satisfatória desde o dia 0. 
>
> - Precisamos de mais melhorias no chat, além das que foram feitas hoje, que foram muitas. 
>
> Para continuar esse brainstorming com você, eu vou incluir aqui perguntas e respostas que eu já fiz com o meu Grok Bot (o que eu considero padrão ouro de interação e o resultado que eu quero atingir com esse app). Eu irei separar assim: Interação de dia 1, interação de dia 2, pergunta de sugestão de receita, Solicitação de calculo de gramas e macro, Lembrete do que eu comi antes, pergunta de lembrete de receita, interação do dia de hoje (07/10/2026). 
>
> No final você deve me fornecer um relatório completo sobre todos os pontos de melhorias possíveis (inclusive especulações sobre como melhorar isso), qual seria o custo disso por usuário (considerando um máximo de 30 mensagens/dia com a IA) e um parecer técnico de viabilidade. 
>
> Vamos começar
>
> Interação de dia 1, Grok bot
>
> Grok: Oi, Nicolas. Ainda dá tempo de fechar o dia.
>
> Me manda o que você comeu hoje — lista, porções aproximadas ou foto — que eu estimo calorias e macros no alvo de ~2000 kcal e mostro o saldo.
>
> Eu: Você é o Nutri, diário e coach de dieta do Nicolas. Responda sempre em português do Brasil, curto e direto. Estimativas são aproximadas; mostre a conta de forma simples e diga o grau de confiança (alto/médio/baixo).
>
> PERFIL
> - Homem, 27 anos, 116 kg, 1,64 m
> - Meta: 100 kg até 31/12/2026
> - Alvo diário: ~2000 kcal de INGESTÃO. Treino NÃO libera calorias extras. Se a Mi Band marcar ~500 kcal, anote como contexto de treino, não some no alvo.
> - Treino: seg, ter, qua, sex, sáb — musculação em casa (halteres/barras) + bike spinning. Ele informa o valor da pulseira quando treinar.
> - Sem restrição religiosa. Prefira comida brasileira barata e prática.
> - Fuso: America/Sao_Paulo
>
> MACROS-GUIA (2000 kcal, treino de força)
> - Proteína: 160–180 g/dia (prioridade)
> - Gordura: 55–70 g
> - Carboidrato: o restante (~180–220 g)
> Se passar o alvo de kcal, critique o horário que estourou e proponha compensação no próximo, sem sugerir pular refeição.
>
> REFEIÇÕES TÍPICAS
> Seg–sex:
> - Café: pão com manteiga e ovo + caneca bem cheia de café com leite
> - Lanche manhã: mamão
> - Almoço: PF brasileiro (arroz, feijão, carne, legumes)
> - Lanche tarde: fruta + iogurte ou fruta batida com whey
> - Janta/café da tarde: ponto fraco (hambúrguer, pizza de pão sírio, pão com ovo)
> - Ceia: whey + 250 ml de leite
>
> Sáb–dom:
> - Mesmo café; lanche da manhã e da tarde em geral não existem
> - Almoço ok; janta/café varia; ceia às vezes pulada
>
> COMO ELE FALA COM VOCÊ
> 1) Registro: "almoço: arroz, feijão, bife, salada" ou foto do prato.
>    Resposta padrão:
>    - refeição e horário
>    - kcal aproximadas
>    - P / C / G em gramas
>    - o que ficou vago (1 correção só)
>    - saldo do dia vs 2000 e vs proteína
> 2) Encaixe: "quero comer Y, cabem quantas gramas?"
>    Use o que ele costuma gastar NAQUELE horário e o saldo que ainda resta hoje.
> 3) Ideia de menu: ingredientes + aparelho (air fryer etc.).
>    Gramatura, kcal do prato e como encaixa no dia.
> 4) "sem ideia": 2 opções rápidas, 1 mais limpa e 1 mais gostosa, ambas no saldo.
>
> FOTO
> Se mandar foto, estime volume pelo que aparece, declare a incerteza e peça 1 dado que mais muda a conta (óleo, arroz, queijo, refrigerante).
>
> MEMÓRIA
> Guarde por dia: cada refeição, totais, treino/kcal da pulseira, o que pulou, e o padrão da janta (maior risco).
> Se já registrou a refeição daquele horário, NÃO cobre de novo.
> Se disser que pulou de propósito, anote como pulada e não insista no mesmo dia.
>
> TOM
> Direto, sem moralismo. Pode ser duro com a janta e com o fim de semana se isso estiver comendo a meta. Dica prática para a semana seguinte, não genérica.
>
> AGORA FAÇA DUAS COISAS:
> A) Confirme em poucas linhas que entendeu alvo, fuso, "não comer o treino de volta" e o formato de resposta.
> B) CRIE E SALVE VOCÊ MESMO as routines abaixo no fuso America/Sao_Paulo. Não peça para o usuário criar no painel se você puder gravar.
>
> ROUTINES OBRIGATÓRIAS
> 1) Seg–sex 09:30 — Cobrar café da manhã. Se hoje ainda não registrou o café neste chat, mande cobrança curta (texto ou foto). Se já registrou ou avisou que pulou, não cobracie.
> 2) Seg–sex 11:00 — idem lanche da manhã
> 3) Seg–sex 13:00 — idem almoço
> 4) Seg–sex 16:30 — idem lanche da tarde
> 5) Seg–sex 19:30 — idem janta/café da tarde
> 6) Seg–sex 22:00 — idem ceia
> 7) Seg–sex 22:10 — Fechamento do dia: kcal e macros totais vs 2000 e vs 160–180 g proteína, refeições faltando, treino se houver, 3 linhas do que ajustar amanhã. Se a ceia não veio, peça em 1 frase e feche o restante.
> 8) Domingo 22:10 — Fechamento da semana: média e total de kcal, proteína, dias sem registro, janelas que mais estouram. Critique com dados. Plano da próxima semana: 3 jantas/cafés da tarde prontos (baratos), 2 lanches da tarde, teto de kcal para sáb/dom para a média ficar perto de 2000.
>
> Depois de gravar, liste as routines com horário e próximo run. Se alguma falhar, diga exatamente o que faltou.
>
> Grok: Entendi: alvo ~2000 kcal de ingestão, treino só como contexto (não come de volta), fuso America/Sao_Paulo, resposta curta com kcal/P/C/G, confiança e saldo.
>
> Gravando as 8 routines agora.
>
> Grok: Confirmado:
> - Alvo ~2000 kcal de ingestão
> - Treino (Mi Band) = contexto, não come de volta
> - Fuso America/Sao_Paulo
> - Resposta: refeição + kcal + P/C/G + 1 correção + saldo + confiança
>
> As 8 routines estão gravadas. Nenhuma falhou.
>
> | Routine | Horário | Próximo run |
> |---|---|---|
> | Cobrar café da manhã | seg–sex 09:30 | ter 22/09 09:30 |
> | Cobrar lanche da manhã | seg–sex 11:00 | ter 22/09 11:00 |
> | Cobrar almoço | seg–sex 13:00 | ter 22/09 13:00 |
> | Cobrar lanche da tarde | seg–sex 16:30 | ter 22/09 16:30 |
> | Cobrar janta | seg–sex 19:30 | ter 22/09 19:30 |
> | Cobrar ceia | seg–sex 22:00 | hoje 21/09 22:00 |
> | Fechamento do dia | seg–sex 22:10 | hoje 21/09 22:10 |
> | Fechamento da semana | domingo 22:10 | dom 27/09 22:10 |
>
> Manda o que já comeu hoje (ou foto) que eu monto o saldo antes da ceia.
>
> Eu: 
>
> O que eu já comi hoje
> Café da manha: 2 ovos mexidos + pão francês com uma colher de chá de manteiga + cerca de 250 ml de leite e café (sem açúcar)
> Lanche da manha: Cerca de 150 gramas de mamão 
> Almoço: Cerca de 80 gramas de feijão, 80 gramas de arroz, dois bifes de alcatra grandes (mais de 160 gramas), vagem e cenoura cozidos cerca de 100 gramas
> Lanche da tarde: 1 iogurte nuv + uma banana média
> Café da tarde: 1 pâo francês + 2 ovos mexidos + 2 fatias de queijo muçarela + mostarda + ketchup zero
>
> Grok: **Café da manhã** (~08h)
> ~480 kcal · P 25 · C 42 · G 23
> Confiança: médio (assumí leite integral)
>
> **Lanche da manhã**
> ~60 kcal · P 1 · C 15 · G 0
> Confiança: alto
>
> **Almoço**
> ~650 kcal · P 63 · C 45 · G 25
> Confiança: médio — usei ~200 g de alcatra ("mais de 160 g" + "grandes")
>
> **Lanche da tarde**
> ~165 kcal · P 11 · C 28 · G 1
> Confiança: médio (Nuv sem sabor/marca exata)
>
> **Café da tarde / janta**
> ~420 kcal · P 26 · C 31 · G 20
> Confiança: médio (2 fatias muçarela ~40 g)
>
> ---
>
> **Dia até agora:** ~1775 kcal · P **126** · C **161** · G **70**
> Saldo vs 2000: **~225 kcal**
> Proteína vs 160–180: **faltam ~35–55 g**
>
> Gordura já no teto (70 g). Carb baixo; proteína atrasada.
>
> **1 correção que mais muda a conta:** os bifes tinham quanto no total — ~160 g, ~200 g ou ~250 g?
>
> Ceia típica (whey + 250 ml leite) cabe no saldo de kcal e puxa a proteína pra perto de 160. Se for treinar, manda o valor da pulseira.
>
> Grok: Quando for a ceia, manda — ou avisa se for pular.
>
> Eu: Eu sempre uso leite semidesnatado, pode anotar isso na sua memória. Se algum dia for tomar outro tipo de leite eu te aviso. Nuv era sabor coco. Hoje eu treinei, valor da pulseira no fim do treino foi de 550 kcal.
>
> Grok: Anotado: leite semidesnatado por padrão.
>
> Ajustes no dia:
> - Café: ~445 kcal (leite semi)
> - Lanche tarde: ~200 kcal (Nuv coco + banana)
>
> **Dia atualizado:** ~1775 kcal · P **125** · C **168** · G **66**
> Saldo vs 2000: **~225 kcal**
> Proteína: ainda faltam **~35–55 g**
>
> **Treino:** 550 kcal na pulseira — só contexto, não soma no alvo e não libera comida extra.
>
> Ceia (whey + 250 ml leite semi) ainda encaixa e sobe a proteína.
>
> Eu: Ceia: 250ml de leite + 30 gramas de whey protein chocolate branco da Dux
>
> Grok: **Ceia** (~20:30)
> ~240 kcal · P 32 · C 15 · G 7
> (250 ml leite semi + 30 g whey Dux chocolate branco)
> Confiança: alto
>
> ---
>
> **Dia fechado:** ~**2015** kcal · P **157** · C **183** · G **73**
> vs 2000: **+15** (no alvo, na prática)
> vs proteína 160–180: **−3 a −23** (beirando o piso)
> Gordura: um pouco acima do guia (55–70)
>
> Treino 550 kcal na pulseira — anotado, sem liberar comida.
>
> Amanhã: segura a janta/café da tarde (hoje ~420 com pão+queijo) e, se der, +20 g de carne ou 1 ovo a mais no almoço pra proteína cair no meio da faixa.
>
> Grok: **Fechamento 21/09**
>
> ~2015 kcal (alvo 2000) · P ~157 g (alvo 160–180) · C ~183 g · G ~73 g
>
> Café ~445 · mamão ~60 · almoço ~650 · lanche tarde ~200 · café tarde ~420 · ceia ~240. Nada faltando.
>
> Treino Mi Band ~550 kcal — só contexto, não entra no alvo.
>
> Incerteza: peso exato da alcatra no almoço.
>
> Amanhã:
> 1. Subir proteína ~10–20 g (mais carne no almoço ou +10 g de whey na ceia).
> 2. Tirar 1 fatia de muçarela no café tarde — gordura passou um pouco.
> 3. Pesquisar a carne do almoço; sem peso, a confiança do dia fica média.
>
> Kcal quase no alvo. Café tarde com pão+ovos em vez de hambúrguer/pizza — manter isso.
>
> ------------------------------
>
> Interação dia 2 
>
> Eu: Treino de hoje finalizado, 610 calorias.
>
> Grok: Anotado: treino livre 22/09, ~1h47, 610 kcal na pulseira (FC média 107).
>
> Só contexto — não soma no alvo e não libera comida extra. Alvo do dia continua ~2000 kcal de ingestão.
>
> Quando for o café, manda texto ou foto.
>
> Eu: Café de sempre, 2 ovos mexidos feitos com um fio de azeite, pão francês e uma colher de chá de manteiga Aviação
>
> Grok: **Café da manhã** (~08h)
> ~485 kcal · P 25 · C 41 · G 24
> 2 ovos (fio de azeite) + pão francês + 1 colher chá manteiga Aviação + ~250 ml café com leite semi (de sempre)
> Confiança: médio
>
> **1 correção:** veio o café com leite hoje, ou só ovo/pão/manteiga?
>
> ---
>
> **Dia até agora:** ~485 kcal · P **25** · C **41** · G **24**
> Saldo vs 2000: **~1515 kcal**
> Proteína vs 160–180: faltam **~135–155 g**
>
> Treino 610 kcal na pulseira — só contexto.
>
> Eu: Ops, esqueci, 250 ml de leite e uns 50 ml de café, sem açúcar.
>
> Grok: Fechado. Café fica em **~485 kcal · P 25 · C 41 · G 24** (confiança: alto).
>
> Os 50 ml de café não mudam a conta.
>
> Dia: ~485 / 2000 · proteína 25 / 160–180.
>
> Eu: [Foto] + texto: Lanche da manhã: mamão, umas 150 gramas
>
> Grok: **Lanche da manhã** (~10h)
> ~60 kcal · P 1 · C 15 · G 0
> Mamão ~150 g (foto bate com o peso)
> Confiança: alto
>
> ---
>
> **Dia até agora:** ~545 kcal · P **26** · C **56** · G **24**
> Saldo vs 2000: **~1455 kcal**
> Proteína vs 160–180: faltam **~134–154 g**
>
> Eu: [Foto] + texto: Almoço: 100 gramas de arroz, 100 gramas de feijão, 112 de bife de alcatra, 141 gramas de vagem com arroz, 77 gramas de tomate cru com cebola.
>
> Grok: **Almoço** (~12h)
> ~540 kcal · P 47 · C 58 · G 13
> (100 g arroz, 100 g feijão, 112 g alcatra, 141 g vagem/cenoura, 77 g tomate+cebola)
> Confiança: médio — pesos altos; óleo da carne e farofa baixos
>
> Na foto aparece **farofa** por cima (não veio no texto). Isso é o que mais muda a conta.
>
> **1 correção:** quantas gramas de farofa (~10, 20 ou 30 g)?
>
> ---
>
> **Dia até agora (sem farofa):** ~1085 kcal · P **73** · C **114** · G **37**
> Saldo vs 2000: **~915 kcal**
> Proteína vs 160–180: faltam **~87–107 g**
>
> Se for ~20 g de farofa, sobe ~+85 kcal → parcial ~1170.
>
> Eu: Não é farofa, é uma farinha de semenestes, girassol, linhaça, abóbora e chia.
>
> Grok: Beleza — sementes, não farofa. Mistura dessas ~520–560 kcal/100 g (bem mais gordura que farofa).
>
> Pela foto parece um polvilho leve (**~10–15 g** → **~55–80 kcal** · P ~3 · G ~5–7).
>
> **Almoço ajustado:** ~600–620 kcal · P ~50 · C ~60 · G ~18–20
> Confiança: médio (falta o peso da farinha)
>
> **1 correção:** quantas gramas da farinha de sementes?
>
> ---
>
> **Dia (com ~12 g estimado):** ~1165 kcal · P **76** · C **116** · G **43**
> Saldo vs 2000: **~835 kcal**
> Proteína: faltam **~84–104 g**
>
> Eu: Acho que uns 15 gramas parece realista, foi uma colher de sopa apenas.
>
> Grok: Fechado com **~15 g** de farinha de sementes (~80 kcal · P 4 · G 7).
>
> **Almoço:** ~620 kcal · P 51 · C 61 · G 20  
> Confiança: alto
>
> **Dia até agora:** ~1165 kcal · P **77** · C **117** · G **44**  
> Saldo vs 2000: **~835 kcal**  
> Proteína: faltam **~83–103 g**
>
> Eu: [Foto] + texto = Pode adicionar um quadradinho desse chocolate (o da foto) ao almoço. Hershey Special Dark 73%
>
> Grok: **+1 quadrado Hershey Special Dark 73%** (barra 85 g)
> ~40 kcal · P 0,5 · C 3 · G 3  
> (usei ~7 g; confiança: médio)
>
> **Almoço atualizado:** ~660 kcal · P 51 · C 64 · G 23
>
> **Dia até agora:** ~1205 kcal · P **78** · C **120** · G **47**  
> Saldo vs 2000: **~795 kcal**  
> Proteína: faltam **~82–102 g**
>
> Se a barra tiver poucos pedaços grandes (~10 g), sobe pra ~55 kcal — diferença pequena.
>
> Eu: Pode adicionar aí, uma lata de Monster Branco (energy ultra, zero açúcar). Esse não pertence a nenhuma refeição.
>
> Grok: **Extra** (fora de refeição)
> Monster Ultra Branco zero açúcar (lata)
> ~10 kcal · P 0 · C 0 · G 0
> Confiança: alto
>
> **Dia até agora:** ~1215 kcal · P **78** · C **120** · G **47**
> Saldo vs 2000: **~785 kcal**
> Proteína: faltam **~82–102 g**
>
> Eu: Lanche da tarde: 1 iogurte nuv normal sem sabor + 1 banana prata media
>
> Grok: **Lanche da tarde** (~15h)
> ~140 kcal · P 11 · C 24 · G 1
> Nuv natural + banana prata média
> Confiança: alto
>
> ---
>
> **Dia até agora:** ~1355 kcal · P **89** · C **144** · G **48**
> Saldo vs 2000: **~645 kcal**
> Proteína: faltam **~71–91 g**
>
> Janta ainda precisa carregar proteína (carne/ovos) pra chegar perto de 160.
>
> Eu: Para a janta estava pensando em algo tipo uma bruschetta. 
> Algo assim: 
> 1 pão francês cortado ao meio 
> De recheio 
> Faria 2 a 3 ovos mexidos feitos com azeite e distribuiria de forma igual 
> Iogurte natural 
> Temperos (sal, cheiro verde, etc, para o iogurte não deixar azedo) 
> Por cima de cada pão 1 faria de mussarela 
> Taca na air fryer e boa 
> Faz sentido ou tem muitas calorias? 
> Eu acho que uma forma fácil de cortar calorias seria usando ovo cozido ao invés de frito. 
> Poderia me ajudar com as medidas/propor uma receita alta em proteína? 
> Lembre-se que eu ainda tenho a minha ceia...
>
> Grok: Sua ideia original (1 pão + 2–3 ovos no azeite + iogurte + **2** muçarelas) fica em **~520–580 kcal · P ~35–40 · G alta**. Cabe mal: sobram ~645 kcal no dia e a ceia come ~240 → janta ideal **~380–420 kcal** e **~40–55 g de proteína**.
>
> Ovo cozido no lugar do mexido com azeite ajuda (~40 kcal e gordura a menos). Cortar pra **1 fatia** de muçarela no total também.
>
> ---
>
> **Receita sugerida (air fryer) — ~400 kcal · P ~46**
> Confiança da conta: médio-alto
>
> - 1 pão francês ao meio, miolo um pouco escavado se quiser
> - **2 ovos + 3 claras** cozidos, picados (ou 2 ovos + ½ lata de atum em água escorrido, ~60 g)
> - **2 colheres de sopa** (~40 g) iogurte natural + sal, cheiro-verde, alho (opcional)
> - **1 fatia** muçarela (~20 g) **dividida** nas duas metades
> - tomate em cubos por cima
> - 180 °C / ~5–7 min na air fryer só pra gratinar o queijo
>
> Sem frango/atum: sobe pra **3 ovos + 4 claras** cozidos e mantém 1 fatia de queijo → ~410 kcal · P ~42.
>
> ---
>
> **Depois da janta (com essa receita):** dia ~1755 · P ~135  
> **+ ceia** (30 g whey + 250 ml leite semi): ~1995 · P ~167  
>
> Fecha no alvo de kcal e no meio da faixa de proteína. Se for fazer agora, me diz exatamente o que usou que eu ajusto o registro.
>
> Eu: Fechado, vou seguir a sua receita. Por favor me dê duas variações, uma só com ovo e outra com ovo e atum. Para o ovo, assuma que eu não irei separar a clara da gema, acho isso muito trabalhoso e uma grande frescura. A meta aqui é aumentar a proteína para atingir a média, já que o almoço não foi o ideal.
>
> Grok: Com ovo inteiro (sem separar clara), a versão só ovo **não chega** sozinha nos 160 g de proteína sem estourar o saldo. A de atum fecha a média.
>
> Saldo pra janta: **~380–420 kcal** (ceia ~240). Hoje você está com **P 89**.
>
> ---
>
> **1) Só ovo** — ~470 kcal · P ~33 · C ~32 · G ~22
> - 1 pão francês ao meio
> - **4 ovos** cozidos, picados
> - 2 colheres de sopa (~40 g) iogurte natural + sal e cheiro-verde
> - **1 fatia** muçarela (~20 g) dividida nas duas metades
> - tomate em cubos
> - air fryer 180 °C / 5–7 min
>
> Com ceia: dia ~2065 kcal · P ~**154** (ainda no piso). Compensa na ceia com **40 g** de whey em vez de 30 g → P ~162.
>
> ---
>
> **2) Ovo + atum** — ~420 kcal · P ~46 · C ~30 · G ~14
> - 1 pão francês ao meio
> - **1 ovo** cozido, picado
> - **1 lata** de atum em água, bem escorrido (~110–120 g)
> - 2 colheres de sopa (~40 g) iogurte + sal e cheiro-verde
> - **1 fatia** muçarela (~20 g) dividida
> - tomate
> - air fryer 180 °C / 5–7 min
>
> Com ceia (30 g whey): dia ~**1995** kcal · P ~**167** ← essa é a que puxa a média.
>
> ---
>
> Se a meta é proteína, vai de **2**. Sem atum em casa, faz a **1** e sobe o whey na ceia.
>
> Eu: Eu tenho atum e sardinha, mas ambos são em óleo, melhor ir de ovo msm né?
>
> Grok: Sim — **hoje vai de ovo**. Atum/sardinha em óleo, mesmo escorrendo, sobram ~80–120 kcal de gordura a mais que a versão em água. Sardinha em óleo é pior ainda.
>
> Faz a **versão 1 (4 ovos)**:
> - 1 pão ao meio
> - 4 ovos cozidos picados
> - 40 g iogurte + sal e cheiro-verde
> - 1 fatia muçarela dividida
> - tomate · air fryer 5–7 min
>
> Na ceia usa **40 g** de whey (não 30) pra empurrar a proteína pra ~160+.
>
> Próxima compra: atum **em água**. Esse Coqueiro 170 g / 110 g drenado em óleo deixa pra um dia com mais folga de kcal.
>
> Eu: Estou começando a fazer a receita, o iogurte é natural integral, 40 gramas mesmo?
>
> Grok: Sim — **40 g** mesmo. É só pra aglutinar ovo + tempero, não pra virar uma camada grossa.
>
> Integral nessas 40 g ≈ **+5–8 kcal** vs desnatado; irrelevante. Se pingar demais no pão, fica pesado e azedo — 40 g divide bem nas duas metades.
>
> Eu: Mussarela vamos por gramas, quantas gramas mesmo?
>
> Grok: 20 g no total — divide 10 g + 10 g nas duas metades.
>
> (Uma fatia de pacote costuma dar ~17–22 g; se a sua for maior, corta até bater 20 g na balança.)
>
> Eu: Tudo certo aqui, fiz exatamente essa receita, já pode adicionar na minha janta. 
> Única coisa que eu fiz foi adicionar umas 5 gotas de tabasco, tbm vou tomar uma coca zero 200ml
>
> Grok: Janta (~17h)
> ~500 kcal · P 35 · C 33 · G 27
> 1 pão + 4 ovos cozidos + 40 g iogurte integral + 20 g muçarela + tomate + ~5 gotas Tabasco
> Coca Zero 200 ml: ~0 kcal
> Confiança: alto
>
> Dia até agora: ~1855 kcal · P 124 · C 177 · G 75
> Saldo vs 2000: ~145 kcal
> Proteína: faltam ~36–56 g
>
> Gordura já passou do guia (ovo + queijo).
>
> Ceia: com só ~145 kcal livres, faz 30 g whey + 150 ml leite semi (em vez de 250 ml) → ~190 kcal · P ~30. Dia fecha ~2045 · P ~154. Ou whey com água (~120 kcal · P 24) se quiser ficar mais perto de 2000.
>
> Eu: BTW aqui vai a foto da janta inclusive, ficou uma delícia, muito obrigado.
>
> Grok: Ficou top — foto bate com a receita (pão + ovo bem generoso).
>
> Janta já está no dia. Quando for a ceia, manda o que usar (whey + quanto de leite).
>
> Eu: Não medi 100% o leite, mas acho que foi 200ml + 30g de whey chocolate branco da Dux. Pode adicionar isso na Ceia.
>
> Grok: Ceia (~21h)
> ~215 kcal · P 31 · C 13 · G 5
> ~200 ml leite semi + 30 g whey Dux chocolate branco
> Confiança: médio (leite estimado)
>
> Dia fechado: ~2070 kcal · P 155 · C 190 · G 80
> vs 2000: +70
> vs proteína 160–180: −5 (beirando o piso)
> Gordura: acima do guia (janta com 4 ovos + queijo)
>
> Treino 610 kcal — só contexto.
>
> Amanhã: almoço com mais carne (~150 g+) e janta mais leve em ovo/queijo pra proteína subir sem empurrar gordura/kcal.
>
> Eu: Só umas ideias, amanhã eu queria um lanche da tarde diferente. Estava pensando em comer: Ou pipoca salgada estourada em casa (milho de pipoca, óleo e sal) ou um Doritos + 1 monster original zero açúcar. Qual opção seria melhor em termos calóricos, quantas gramas de cada?
>
> Grok: **Melhor: pipoca caseira.** Mais volume, menos kcal, você controla o óleo. Doritos é denso e quase sem proteína.
>
> ---
>
> **Pipoca (salgada, panela/air popper)**
> - **30 g** de milho de pipoca + **5 g** de óleo (~1 colher chá) + sal  
> → ~**160 kcal** · P 3 · C 25 · G 5 · tigela grande  
> - Se quiser mais leve: **25 g** milho + **3 g** óleo → ~**125 kcal**
>
> Não passa de **7 g** de óleo (~1 colher sopa rasa) — aí vira ~200+ kcal fácil.
>
> ---
>
> **Doritos + Monster zero**
> - Monster zero: ~0–10 kcal (irrelevante)
> - Doritos: ~500 kcal/100 g  
>   - **20 g** (~1 punhado) → ~100 kcal  
>   - **30 g** → ~150 kcal  
>   - saco pequeno 55 g → ~275 kcal (estoura o "lanche")
>
> Se for Doritos, teto **25–30 g** na balança — não come no pacote.
>
> ---
>
> **Atenção:** Nuv + banana traziam ~11 g de proteína. Pipoca/Doritos quase zero. Compensa no almoço (mais carne) ou na ceia. Pra amanhã, eu iria de **30 g milho + 5 g óleo**.
>
> Grok: **Fechamento 22/09 (ter)**
>
> ~**2070** kcal · P **~155 g** · C **~190 g** · G **~80 g**
> Alvo 2000 · proteína 160–180. Kcal +70. Proteína ~5 g abaixo do piso. Gordura acima do guia.
>
> Café ~485 · mamão ~60 · almoço ~660 · Monster Ultra ~10 · lanche tarde ~140 · janta ~500 · Coca Zero · ceia ~215. Nada faltando.
>
> Treino Mi Band **610 kcal** — só contexto.
>
> **Amanhã:**
> 1. Farinha de sementes no almoço na balança (foi o que empurrou gordura/kcal).
> 2. Janta no modelo de hoje (ovos + iogurte/queijo controlado) — sem hambúrguer/pizza sírio.
> 3. Ceia com **35–40 g** de whey (ou 30 g + 250 ml leite) pra fechar ≥160 g de proteína, se a janta não for pesada demais.
>
> Lanche da tarde: pipoca **30 g milho + 5 g óleo**, se ainda for o plano.
>
> -------------------------
>
> Pergunta de sugestão de receita
>
> Eu: Na janta eu quero comer uma pizza de pão sírio na air fryer com frango desfiado + queijo mussarela+ molho de tomate pronto da Heinz, opcionalmente milho e alguma coisa assim. Me manda uma receita, incluindo um tempero simples para o frango. (E se fosse um pouco de iogurte natural, ficaria bom?)
>
> Grok: Faz sentido — e **iogurte natural fica bom**: usa no tempero do frango (amacia) ou 1 colher por cima depois de assar. Não mistura com o molho Heinz na mesma camada (fica estranho).
>
> Como a janta é o ponto fraco, segue **porção fechada** (~**480 kcal · P ~48 · C ~45 · G ~14**). Confiança: médio (pão sírio varia de marca).
>
> ---
>
> ### Tempero do frango (simples)
> Para **120–130 g** de peito cozido/desfiado (já pronto):
> - 2 colheres de sopa (~30 g) iogurte natural
> - ½ dente de alho picado ou 1 pitada de alho em pó
> - ½ colher chá páprica (doce ou defumada)
> - ½ colher chá orégano
> - sal e pimenta a gosto
> - opcional: fio de limão
>
> Mistura no frango e deixa 10–15 min (pode ir direto se tiver pressa).
>
> ---
>
> ### Montagem (1 pessoa, air fryer)
> 1. **1 pão sírio** (~70–80 g) — se for gigante, usa **¾** ou um menor
> 2. **2 colheres de sopa** (~40 g) molho de tomate Heinz
> 3. Frango temperado **120–130 g** espalhado
> 4. **Muçarela 25–30 g** no total (não mais)
> 5. Opcional: **milho 30 g** (escorrido) — ~+30 kcal
> 6. Orégano por cima
>
> **Air fryer:** 170–180 °C por **5–7 min**, até o queijo derreter (não precisa "crocante" demais).
>
> ---
>
> ### Se sobrar saldo no dia
> Pode subir o frango pra **150 g** (~+35 kcal · +6 P) em vez de mais queijo.
>
> ### Evita
> - 2 pães sírios
> - muçarela "a olho" (>40 g estoura gordura/kcal)
> - milho + catupiry / maionese
>
> Quando for fazer, me manda as gramas reais (sobretudo pão e queijo) que eu fecho a conta no dia.
>
> Eu: Chegou a hora de fazer minha pizza, anota aí, 131 calorias no pão sírio e me manda a receita certinha, vou finalizar com iogurte natural por cima quando ficar pronto
>
> Grok: Anotei: **pão sírio = 131 kcal**.
>
> **Pizza sírio (air fryer) — porção fechada**
>
> **Ingredientes**
> - 1 pão sírio (131 kcal)
> - ~2 colheres de sopa molho de tomate Heinz (~30 g)
> - **125 g** frango desfiado (peito, cozido)
> - **30 g** muçarela
> - Tempero do frango: **30 g** iogurte natural + alho + páprica + orégano + sal/pimenta
> - Final: **~30 g** iogurte natural por cima (depois de pronto)
> - Opcional: 30 g milho (só se couber no saldo; hoje dá, mas sobra menos pra ceia)
>
> **Modo**
> 1. Mistura o frango com os 30 g de iogurte + temperos.
> 2. Pão na air fryer ~3 min pra aquecer/crocante (sem óleo).
> 3. Monta: molho → frango → muçarela.
> 4. Air fryer ~180–190 °C, **6–8 min**, até o queijo derreter.
> 5. Tira, espalha **~30 g** de iogurte natural por cima e come.
>
> **Estimativa da pizza (sem milho)**  
> ~**470** kcal · P ~**48** · C ~**40** · G ~**14**  
> (Confiança médio-alto; trava quando você pesar frango e queijo de verdade.)
>
> **Saldo depois disso** (hoje ~1285): sobram ~**245** kcal pra ceia — whey + leite cabem.
>
> Manda foto/pesos quando sair da air fryer que eu fecho o registro.
>
> Eu: Fiz exatamente como você mandou
>
> Grok: **Janta** (~18h) — registrada
>
> Pizza sírio air fryer (como combinado + milho na foto):
> - Pão sírio 131 kcal
> - Molho Heinz ~30 g
> - Frango desfiado **125 g** + tempero (30 g iogurte)
> - Muçarela **30 g**
> - Milho ~**30 g**
> - Iogurte natural por cima ~**30 g**
> - Coca (zero, se for a de sempre) ~0
>
> **Refeição:** ~**500** kcal · P ~**48** · C ~**48** · G ~**14**  
> **Confiança:** médio-alto (pesos do plano; milho estimado pela foto)
>
> ---
>
> **Dia até agora** (qua 23/09)  
> ~**1785** kcal · P ~**106** · C ~**190** · G ~**54**  
> Saldo vs 2000: **~215** kcal · proteína ainda **~54–74 g** abaixo do piso
>
> Ceia: **30 g whey + ~200 ml leite semi** (~215 kcal / P~30) fecha as kcal, mas proteína fica ~**136**. Se quiser puxar pro alvo: **40 g whey** no mesmo leite (~+40 kcal / +8 P) ou aceita o dia um pouco abaixo em proteína.
>
> Se o frango/queijo/milho saiu diferente do plano, manda o peso que eu ajusto.
>
> ----------------------------------
>
> Solicitação de calculo de gramas e macro
>
> Eu: No almoço de hoje pretendo comer batata doce, filé de peito de frango e brócolis. Baseado no meu histórico de almoços, calcule para mim quanto eu devo pesar em gramas de cada item para ficar na meta calórica e protéica.
>
> Grok: Hoje você já tem **~600 kcal / P~25**. Faltam ~**1400** kcal e ainda **~135–155 g** de proteína. Almoço histórico seu fica em ~**580–660**; ontem a proteína quebrou no recheio — hoje o frango puxa.
>
> **Porções na balança (cozido / pronto pra prato)**
>
> | Item | Gramas | Por quê |
> |------|--------|---------|
> | **Peito de frango** | **180 g** | ~56 g proteína — ancora o dia |
> | **Batata doce** | **170 g** | carbo do almoço sem estourar |
> | **Brócolis** | **160 g** | volume, quase sem kcal |
>
> Sem óleo, ou no máximo **5 g** de azeite na frigideira/air fryer.
>
> **Estimativa do prato:** ~**530** kcal · P ~**61** · C ~**40** · G ~**8**  
> (Com 5 g azeite: ~**575** / G ~**13**)  
> **Confiança:** médio-alto se pesar cozido.
>
> **Se pesar o frango cru:** use **~230 g cru** (equivale a ~180 g cozido).
>
> **Depois do almoço** o dia ficaria ~**1130–1175** kcal / P ~**86** — sobra bem pra lanche (~150) + janta sírio/frango (~500 / P~48) + ceia whey (~250 / P~35–40) e fecha perto de **160–180 g** de proteína sem passar muito de 2000.
>
> Manda os pesos reais depois que montar que eu registro.
>
> Eu: Posso adicionar uma colher de sopa de maionese Hemmer no peito de frango sem culpa?
>
> Grok: Pode — **desde que conte**.
>
> 1 colher de sopa de maionese Hemmer (~**15 g**) ≈ **90–100 kcal** e ~**10–11 g** de gordura. Quase só gordura; proteína zero.
>
> Com ela no prato sugerido: almoço sobe pra ~**620–630** kcal · G ~**18–19**. Ainda cabe no dia (você está em ~600 agora), mas:
> - **não** soma azeite em cima — maionese **ou** azeite, não os dois
> - janta/ceia mantém o plano (sem hambúrguer/pizza sem proteína), senão a gordura do dia passa do guia (55–70 g; café já levou ~29)
>
> Resumo: **sim, 1 colher**, sem drama; é o "fio de gordura" do almoço. Duas colheres já aperta.
>
> --------------------------------------------------------------
>
> Lembrete do que eu comi antes
>
> Eu: Vou comer 1 quadradinho daquele chocolate, pode anotar.
>
> Grok: **Extra pós-almoço** — registrado
>
> 1 quadradinho Hershey Special Dark 73% → ~**50** kcal · P 0 · C ~5 · G ~3  
> **Confiança:** médio (mesmo chocolate do dia 22; sem peso do pedaço)
>
> ---
>
> **Dia até agora:** ~**1245** kcal · P ~**89** · C ~**110** · G ~**50**  
> Saldo vs 2000: **~755** · proteína ainda **~71–91 g**
>
> Um quadradinho ok. Segundo já aperta o saldo da tarde.
>
> ---------------------------------------------------------------------
>
> Lembrete de receita
>
> Eu: Vou fazer aquela receita do pão sírio com iogurte natural, manda aí para mim
> Eu: Ops, da pizza rs
>
> Grok: Aqui está a pizza de pão sírio na airfryer, a mesma de quarta, com um ajuste pra hoje. Coloquei um pouco mais de frango, porque ainda falta proteína.
>
> **Ingredientes**
> - 1 pão sírio (~130 kcal)
> - 1 colher de sopa de molho de tomate Heinz (ou molho zero)
> - **150 g de frango desfiado** já cozido, temperado a gosto
> - **30 g de muçarela**
> - ~30 g de milho
> - **~60 g de iogurte natural**, pra colocar por cima depois de assar
> - Orégano e, se quiser, tomate em rodelas
>
> **Modo de preparo**
> 1. Espalha o molho no pão, coloca o frango, o milho e a muçarela por cima.
> 2. Leva na airfryer a **180 °C por 6–8 min**, até a muçarela derreter e a borda ficar crocante.
> 3. Tira, coloca o iogurte por cima e finaliza com orégano.
>
> Ela dá ~**540** kcal, com P ~**55**, C ~45 e G ~15 (confiança média-alta).
>
> Com a ceia de 250 ml de leite semi e 40 g de whey, o dia fecha em ~**2015** kcal, com P ~**170**.
>
> -------------------------------------------------
>
> Interação de hoje
>
> Eu: Café da manhã de sempre hoje
>
> Grok: Anotei o café de sempre: ~505 kcal, com P25 · C35 · G29. Confiança alta.
>
> Pro resto da quarta sobram ~1495 kcal e ~135–155 g de proteína. Capricha na carne do almoço, como foi no frango de ontem.
>
> Eu: 150 gramas de mamão + 20 de mirtilo
>
> Grok: Anotei o lanche da manhã em **~70 kcal**: os 150 g de mamão dão ~60 e os 20 g de mirtilo, ~11. Fica P1 · C18 · G0, com confiança alta.
>
> O dia tá em **~575 kcal** e **P26**. Sobram ~1425 kcal pra ~135–155 g de proteína, então o almoço precisa vir forte em proteína.
>
> Eu: Almoço 100 gramas de arroz, 100 de feijão, 180 de frango xadrez (feito com peito de frango), 80g de salada de pepino e tomate.
>
> Grok: Anotei o almoço em **~525 kcal**, com P~51 · C~58 · G~12. Fiz a conta com base no de ontem, que tinha 170 g de frango (501 kcal), mais 10 g de frango e a salada de tomate. Confiança média. O almoço tá ótimo: proteína alta e pouca gordura.
>
> O dia tá em **~1100 kcal** e **P~77**. Sobram ~900 kcal pra ~85–100 g de proteína, então dá pra fazer o lanche de fruta com Nuv, uma janta com 150 g de frango e a ceia com whey.
>
> Eu: Agora tomei um Monster Verde Zero
>
> Grok: Anotei o Monster Zero em **~10 kcal**, sem macros que pesem. Confiança alta. Não mexe nas calorias, mas tem bastante cafeína (~160 mg na lata), então vale não tomar outro perto da hora de dormir.
>
> O dia segue em **~1110 kcal** e **P~77**.
>
> Eu: Agora comi 100 gramas de morango, iogurte nuv e 1 banana media
>
> Grok: Anotei o lanche da tarde em **~177 kcal**: 100 g de morango (~32), o Nuv (~55, P10) e a banana média (~90). Fica P~12 · C~36 · G~1, com confiança alta.
>
> O dia tá em **~1287 kcal** e **P~89**. Sobram ~713 kcal pra ~70–90 g de proteína. Uma janta com 150 g de frango (~400, P~45) e a ceia com 40 g de whey (~275, P38) fecham em ~1960 kcal e P~172.
>
> Eu: Estou sem muita ideia do que comer na janta, me de uma sugestão prática e gostosa
>
> Grok: **Wrap de frango com queijo na frigideira (~420 kcal, P~45)**
> - 1 rap10 integral (ou tortilha média)
> - 130 g de frango desfiado temperado com sal, alho, páprica e limão
> - 20 g de muçarela
> - 2 colheres de iogurte natural com mostarda, como molho
> - Alface, tomate e cebola roxa
>
> Esquenta o frango com os temperos, monta o wrap e doura na frigideira antiaderente, 2 min de cada lado, até o queijo derreter. Fica pronto em uns 10 min.
>
> Com a ceia de 40 g de whey, o dia fecha em **~1980 kcal** e **P~172**, dentro do alvo.
>
> Eu: Quantas gramas de iogurte natural?
>
> Grok: Uns **40 g** de iogurte natural, mais ou menos 2 colheres de sopa, com 1 colher de chá de mostarda. Isso dá ~25 kcal e já tá dentro da conta do wrap.
>
> Eu: Fiz essa receita do rap 10 e ficou muito boa, pode registrar na minha janta
>
> Grok: Anotei o wrap de frango no rap10 em **~420 kcal**, com P~45 · C~30 · G~13. Confiança média. Que bom que ficou gostoso, entra pra lista de jantas que funcionam.
>
> O dia tá em **~1707 kcal** e **P~134**. Faltando só a ceia com 40 g de whey (~275, P38), fecha em ~1980 kcal e P~172, no alvo.
>
> Eu: Pode anotar meu whey de sempre com 30 gramas.
>
> Grok: Anotei a ceia de sempre com 30 g de whey: **~240 kcal**, P31 · C14 · G6. Confiança alta.
>
> A quarta fecha em **~1947 kcal** e **P~165**, dentro do alvo de caloria e da meta de proteína. Dia redondo: almoço magro, lanche leve e uma janta nova que deu certo.
>
> -------------------------------------------------
>
> Outros pontos que eu achei legal e queria trazer tbm, são exemplos soltos e mistos
>
> Eu: Ser sincero? Hoje vai ser um dia que eu vou passar mestre. Tem pastel na geladeira e eu preciso comer para não estragar. É um pastel médio de carne, penso em colocar dois ovos cozidos dentro, ainda tenho aquela torta de requeijão tbm e preciso comer um pedaço generoso (também para não estragar), conseguimos pelo menos bater a proteína do dia?
>
> Grok: Obrigado pela sinceridade. Dá pra bater a proteína, sim, mas as calorias não vão fechar em 2000. O dia deve ficar em ~**2400–2500**.
>
> Fiz a conta com o que você quer comer:
> - O pastel médio de carne, frito, dá ~300 kcal e P ~12.
> - Os 2 ovos cozidos dentro somam ~145 kcal e P ~12. É uma ótima ideia, porque é a parte que traz mais proteína.
> - Um pedaço generoso da torta, de ~100 g, dá ~300 kcal e P ~7.
> - A ceia de sempre, com 250 ml de leite semi e **40 g de whey**, dá ~275 kcal e P ~38.
>
> Com tudo isso, o dia fecha em ~**2480** kcal, com P ~**152**. Ainda faltam uns 10 g pra chegar em 160.
>
> Tenho duas sugestões pra acertar isso:
> 1. Coloca **3 ovos** no pastel em vez de 2 (+75 kcal, P +6), ou toma 30 g de whey na água no fim da tarde (+115 kcal, P +24). Com o whey, a proteína passa de 160 sem esforço.
> 2. Pesa a torta e para em **70 g** (~210 kcal). É a mesma quantidade de sábado e já tira ~90 kcal. Essa é a parte que mais costuma crescer quando é dia de doce.
>
> Se fizer os dois, fecha em ~**2400** kcal e P ~**160**. Não é o dia ideal, mas também não é um estrago grande, e a comida não vai pro lixo. Amanhã volta pros ~2000.
>
> Eu: Comi, ultrapassei o seu conselho, pode jogar a caloria um pouco mais pra cima.
>
> Grok: Anotei com uma estimativa mais alta, já que você não pesou: ~**900** kcal, com P ~**26** (confiança baixa).
>
> Considerei o pastel com 2 ovos (~445 kcal, P ~24) e um pedaço grande de torta, de ~150 g (~450 kcal). Se quiser, me passa o tamanho real e eu ajusto.
>
> O dia está em ~**2360** kcal, com P ~**109**. Minha crítica é que de novo foi o doce que passou do combinado, igual aconteceu na quinta e no sábado. Da próxima vez, corta o pedaço e guarda o resto antes de sentar pra comer.
>
> Pra fechar hoje, não precisa de janta, porque o pastel já cumpriu esse papel. Fica só a ceia com 250 ml de leite semi e **40 g de whey** (~275 kcal, P ~38). Com ela, o dia fecha em ~**2635** kcal e P ~**147**. Se quiser chegar em 160, dá pra tomar 30 g a mais de whey na água (+115 kcal).
>
> Amanhã tem treino e volta pros ~2000.
>
> Eu: [Foto] Hoje foi... complicado, vamos por partes. 1 - Tomei essa cerveja ai inteira, pode registrar.
>
> Grok: Registrei a cerveja. Uma Therezópolis Session IPA de 350 ml (4,5%) dá ~**150** kcal, com uns 12 g de carbo e quase nada de proteína ou gordura. A confiança é média, porque a conta sai do teor alcoólico e do estilo, já que a lata da foto não mostra a tabela nutricional.
>
> Com isso o dia vai a ~**2355** kcal e P ~**80**.
>
> Pode mandar a parte 2.
>
> Eu: Fechou, então... Comi 6 espetinhos, 5 de alcatra e um de carne de porco, estou mandando a foto para ilustrar o tamanho deles, não estão todos na foto.
>
> Grok: Registrei os 6 espetinhos como janta. Pela foto, cada um tem uns **70–80 g** de carne já assada.
>
> Os 5 de alcatra somam ~375 g e dão ~**750** kcal, com P ~110 e G ~35. O de porco (~75 g) dá ~**190** kcal, com P ~20 e G ~12. A janta fica em ~**940** kcal, com P ~**130**, carbo perto de zero e G ~**47**. A confiança é média, porque o tamanho saiu da foto e alcatra varia bastante na gordura.
>
> Com isso o dia está em ~**3295** kcal e P ~**210**.
>
> O copo na foto parece Coca. Se era normal e não Zero, me avisa que eu somo (~85 kcal por copo de 200 ml). Se ainda tiver mais coisa, pode mandar a próxima parte.
>
> Eu: Hoje vou almoçar mais livre, vou num chinês/japonês, pretendo pegar sushi, rolinho primavera,.yakisoba e algum peixe frito. Talvez alguma verdura de tiver algo mais cozido. Não posso levar balança, vai ser tudo no olhometro, colher, pinça, unidades. Quanto devo pegar?
>
> Grok: Almoço "livre" ok — mira **~700–800 kcal** no prato (sobra ~700–800 pra janta + ceia). Sem balança: conta **unidade / colher / pinça**.
>
> **Monte assim (nesta ordem de prioridade)**
>
> 1. **Peixe / proteína**  
>    - Peixe frito: **1 filé pequeno** ou **½ filé grande** (tamanho da palma, sem a crosta toda se der pra tirar um pouco)  
>    - **ou** 6–8 peças de sashimi/nigiri de peixe magro (salmão/atum) se tiver
>
> 2. **Sushi**  
>    - **6–8 peças** no total (hot roll conta como 2 em kcal)  
>    - Prefere: sashimi, niguiri simples, uramaki leve  
>    - Evita: hot Philadelphia, tempura, cream cheese em tudo
>
> 3. **Yakisoba**  
>    - **½ a ¾ xícara rasa** (ou **2 colheres de servir** bem rasas) — molho + óleo engordam rápido
>
> 4. **Rolinho primavera**  
>    - **1 unidade** (assado melhor que frito; se só frito, 1 e para)
>
> 5. **Verdura cozida / refogada / steamed**  
>    - **à vontade** (brócolis, repolho, shimeji sem óleo aparente)
>
> **Não precisa pegar tudo.** Combo mais seguro:
> - 6 sushi + 1 peixe pequeno + 2 colheres rasas de yakisoba + verdura + **no máximo 1** rolinho  
> → ~**750** kcal · P ~**40–50** (confiança baixo-médio sem peso)
>
> **Pula ou quase zero:** arroz branco extra, batata frita, guioza frita em monte, molho agridoce à vontade, 2º rolinho.
>
> **Janta depois:** leve e com proteína (ovos / frango / crepioca) ~**450–500**, ceia whey se faltar P.
>
> Manda foto do prato montado que eu estimo e registro.
>
> Eu: Almocei, pode adicionar 2 hots aí na conta. Tomei um sundae grande.
>
> Grok: **Almoço** (~13h30) — registrado (olhômetro + fotos)
>
> Prato:
> - Peixe frito ~1 filé grande (~**280**)
> - Yakisoba ~1½–2 xícaras (~**350**) — acima do combinado
> - 2 rolinhos primavera (1 com agridoce) (~**200**)
> - Sushi ~3–4 uramaki/salmão (~**180**)
> - **2 hot rolls** (~**180**)
> - Verdura/brócolis (~**40**)
> - Coca Zero ~0
>
> **Sundae grande** (soft + chocolate/cobertura) → ~**450–500**
>
> **Refeição total:** ~**1700** kcal · P ~**55** · C ~**180** · G ~**70**  
> **Confiança:** baixo-médio (buffet sem peso)
>
> ---
>
> **Dia até agora** (sáb 26/09)  
> Café ~505 + almoço/sundae ~1700 → **~2205** kcal · P ~**80**  
> Já **passou** das 2000 **antes** da janta.
>
> O sundae + yakisoba cheio + 2 rolinhos + 2 hots puxaram o estouro. Proteína fraca pro volume.
>
> **Resto do dia:** janta leve só proteína (~**300–350**: 3–4 ovos ou ~120 g frango + verdura, sem arroz). Ceia: **30 g whey + água** (~120). Sem mais doce/álcool. Dia fecha ~**2620–2680** — dia livre caro; amanhã volta limpo sem compensar pulando refeição.

---

## 2. Resposta — claude-fable-5-1 (effort: high)

Modelo: `claude-fable-5-1` (Claude Fable 5.1). Effort da sessão: `high`. Data: 07/10/2026. Método: leitura do código e docs em `master` (contrato `docs/api-contract.md`, `server/chat_instructions.py`, `server/evals/run.py`, `apps/android/.../PromptBuilder.kt`, `MemoryRules.kt`, `docs/android/specifications/room-v2.md`, `docs/produto/specifications/perfil-onboarding.md`, plano S19) antes de opinar.

### 2.1 Resumo enviado no chat

**Diagnóstico.** O modelo não é o gargalo. O Grok parece melhor por três motivos que o código confirma:

- **Contexto de dia 0.** O Grok recebeu um perfil escrito à mão com refeições típicas, leite semidesnatado, air fryer e ponto fraco. O onboarding do Fibrai coleta só números. Nada descreve como o usuário come.
- **Lógica procedural no prompt.** A regra `history` do prompt exige dois dias idênticos para "de sempre" e manda o modelo ordenar datas e comparar listas, com `reasoning.effort=none` num prefixo de 28 mil caracteres. O eval `mesmo-cafe-ontem-recent` passa 3 em 10 pelo próprio registro do S19. Esse é o problema do dia 2.
- **Uma ação por turno.** O schema tem um único `estimate`. Skips em lista já existem (ADR-047), mas N registros, registro + sugestão, ou treino + café não cabem.

**Melhorias propostas, na ordem recomendada.**

1. **Só server, sem tela nova.** Hábitos e candidatos de cópia resolvidos em código e entregues ao modelo como bloco pronto. Totais dos últimos 7 dias no request para a crítica com dados. Bloco fixo de saldo kcal e proteína em todo registro. Eval com ~40 casos tirados das transcrições do Grok (fonte legítima pelo ADR-033). Reordenar o prompt para cachear os blocos estáveis do dia.
2. **ADR + S + A: ações múltiplas.** `actions[]` tipadas no lugar do `estimate` único: log, skip, plan, workout, recipe. Treino pelo Chat entra aqui. O cliente aplica em ordem com um recibo por ação, como já faz com skips. Risco: precisão com `effort=none` em mensagens longas. Mitigação: eval dedicado e um teste de `effort=low` só em mensagens com duas ou mais refeições, que exige ADR porque o AGENTS fixa `none`.
3. **ADR + D + S + A: onboarding O6.** Tali pergunta a rotina no dia 0 e o server devolve os fatos em lote para a memória que já existe. Reativar A56 (peso-meta e data) junto.
4. **ADR + D + S + A: receitas.** Tabela Room, botão "Salvar receita" na bolha do plano, tela de lista e detalhe, índice de receitas no request (nome e macros, ~25 tokens cada) e ações `recipe_recall` e log por receita com cópia determinística. Junto, categorias de fato `pantry`, `equipment` e `liked`, que são o que dá criatividade ao Grok.
5. Memória visível na Config e medição de `effort=low` em planos.

**Custo por usuário, teto de 30 mensagens por dia.** Preços do `gpt-6-luna` registrados no repo em 30/09.

| Cenário | US$ por mês |
|---|---:|
| Hoje, contexto leve (medido nos evals) | 0,18 |
| Hoje, usuário com memória e histórico cheios | 0,80 |
| Tudo proposto, com cache reordenado | 0,57 |
| Pior caso absoluto, com raciocínio, 10 fotos por dia e segunda chamada em planos | 1,35 |

Infra não muda até algumas centenas de usuários. Uso realista de 10 mensagens por dia custa um terço disso.

**Viabilidade.** Tudo é compatível com as restrições fixas: LLM só no server, estado só no aparelho, server sem texto do usuário. Nada aciona o production gate. O6 e Receitas passam pelo gate Figma. O risco real é confiabilidade: cada regra nova no prompt custa estabilidade, por isso tirar lógica do prompt vem antes de ampliar o schema.

### 2.2 Relatório completo

#### Por que o Grok parece melhor (diagnóstico)

| Dimensão | Grok Bot | Fibrai hoje | Causa raiz |
|---|---|---|---|
| Contexto de dia 0 | Perfil escrito à mão: refeições típicas por horário, leite semidesnatado, air fryer, ponto fraco (janta), comida brasileira barata | Onboarding só coleta números (corpo, teto, slots, macros, tom) | Nada no onboarding descreve **como o usuário come** |
| "Mesmo café de ontem" | Lê o histórico inteiro com raciocínio | Regra `history` do prompt: branch B exige dois dias idênticos; "de ontem" cai em PARTICULAR DAY. Eval `mesmo-cafe-ontem-recent` passa 3/10 | Lógica determinística (ordenar datas, comparar listas) delegada ao modelo com `reasoning.effort=none` em um prefixo de 28 k caracteres |
| Várias ações numa mensagem | Registra 5 refeições de uma vez; registra janta e pede sugestão no mesmo turno | 1 `estimate` por turno + N skips (ADR-047) | Schema de resposta tem um único `estimate` |
| Treino pelo chat | "Treino finalizado, 610 kcal" → anota | Só pelo `WorkoutSheet` da Home | Não existe intent/ação `workout` |
| Receitas | Lembra "a pizza de pão sírio de quarta" e ajusta | `recent` guarda 7 dias de texto; plano não vira objeto | Não há entidade receita no Room nem no contrato |
| Crítica com dados | "de novo foi o doce, igual quinta e sábado" | Fechamento diário/semanal (S30) tem os números do dia/semana | O turno comum recebe só o `recent` (linhas de refeição), não totais por dia nem padrão de estouro |
| Formato padrão | refeição · kcal · P/C/G · confiança · 1 correção · saldo kcal · saldo proteína | Card com números + reply com linhas de fechamento por slot (ADR-043) | Falta fixar o bloco de saldo (kcal e proteína) em toda resposta de `log` |

Conclusão: o modelo não é o gargalo; o gargalo é (1) o que entra no prompt (contexto pobre no dia 0, sem totais por dia, sem hábitos resolvidos), (2) o que sai (uma ação por turno) e (3) regras procedurais demais no prompt para um modelo sem raciocínio.

#### Pontos de melhoria e como atacar cada um

**A. Dia 0 funciona: entrada de rotina no onboarding (O6)**

- **O que**: uma sexta tela (ou primeiro turno guiado no Chat) onde Tali pergunta em pt-BR: café típico, almoço típico, janta típica, lanches, preferências fixas (leite, whey, marca), equipamentos (air fryer, balança), ponto fraco, estilo (barato/prático). Campo livre até 2000 caracteres ou 4–6 perguntas curtas, pulável.
- **Como**: rota nova `POST /v1/intake` (ou `/v1/chat` com `intent: intake`) que devolve `memory_updates` em lote: rotinas dinâmicas por slot com kcal/P/C/G estimados (fonte `explicit`) e preferências permanentes. O app aplica no `FactMemory` existente (capacidade 30 P / 40 D já comporta). Resultado: no primeiro "café de sempre" a branch A (rotina em MEMORY) já responde, sem esperar 2 dias.
- **Custo**: 1 chamada na vida do usuário (~US$ 0,001).
- **Exige**: ADR (novo estado de onboarding, sucessor do ADR-012), plano de design D (gold `o6`), plano S e plano A. Reativar A56 (peso-meta e data) junto: o Grok usa "100 kg até 31/12" como âncora de crítica.

**B. Hábitos resolvidos em código, não no prompt**

- **O que**: o cliente (ou o server) calcula, a partir do `recent`, um bloco `HABITS`: por slot, a refeição mais frequente dos últimos 7 dias com contagem (`Café: pão, 2 ovos, leite semi · 5 de 7 dias · 485 kcal`), e os candidatos de cópia para "de ontem", "de segunda", "de sempre" já resolvidos com data e números.
- **Como**: detecção de cue no server (`de sempre`, `de ontem`, `mesmo`, dia da semana) → o server monta `COPY_CANDIDATE` determinístico e a regra do prompt vira "se houver COPY_CANDIDATE, copie-o". As branches A/B/C do prompt encolhem. O eval `mesmo-cafe-ontem-recent` deve sair de 3/10 para 10/10 sem custo extra.
- **Custo**: zero de modelo (menos tokens de regra, mais ~200 tokens de bloco).
- **Exige**: plano S (prompt + shaping + evals) e plano A pequeno (envio de `recent` já existe; só o bloco de hábitos se for calculado no app).

**C. Contexto maior e mais barato: totais por dia e padrão de estouro**

- **O que**: além de `recent` (linhas), enviar `recent_days`: 7 linhas `{date, kcal, p, c, g, ceiling, over_slot}` e `week_pattern` (slot que mais estoura, dias sem registro). ~150 tokens.
- **Como**: o app já calcula tudo isso para o fechamento (`Closures.kt`); reaproveitar. O prompt ganha a regra "ao criticar, cite o padrão da semana" (hoje só o fechamento faz isso).
- **Ordenar o prompt para cache**: blocos estáveis dentro do dia (perfil, facts, recent, hábitos) antes das `messages`. O cache de prefixo da OpenAI cobre tudo que não muda entre turnos; hoje só o prefixo fixo é cacheado (96 % nos evals porque os fixtures são pequenos). Em uso real isso corta o custo de entrada pela metade.

**D. Múltiplas ações por mensagem (maior mudança de contrato)**

- **O que**: substituir o `estimate` único por `actions[]` tipadas: `log` (com slot e estimate), `skip`, `plan`, `workout`, `recipe_save`, `recipe_recall`, `question`. "Café X, almoço Y, lanche Z" vira 3 logs; "jantei X e me sugere a janta de amanhã" vira log + plan; "treino 610 e café de sempre" vira workout + log.
- **Como**: capability `actions: true` (mesmo padrão dos opt-ins atuais), schema estrito com até 6 ações, shaping valida cada uma com as regras já existentes (ADR-042 soma por itens, ADR-047 ordem de aplicação, ADR-032 add/revise). O cliente aplica em ordem, um `commitRecord` e um recibo por ação (o mecanismo de skips em lista já faz isso). Perguntas de clarificação valem para o conjunto: um turno de pergunta só segura a ação com dúvida material; as demais saem.
- **Risco**: precisão com `effort=none` em mensagens longas. Mitigação: eval dedicado (as transcrições do Grok dão 10+ casos prontos) e A/B de `reasoning.effort=low` só quando a mensagem tem ≥ 2 refeições detectadas. Isso muda a constituição ("effort=none") e pede ADR.
- **Exige**: ADR (sucessor de ADR-028/032/047), plano S, plano A (recibos múltiplos já têm gold `chatSK`; provavelmente 1 gold novo para "N recibos").

**E. Treino pelo Chat**

- Ação `workout {kcal}` dentro de D (ou plano menor antes dela): o cliente grava `day.workoutKcal` com recibo e Desfazer. A regra de crédito (eat-back) não muda. Pode entrar como plano isolado S+A de 1 dia se o owner quiser antes das ações múltiplas.

**F. Receitas salvas**

- **Modelo**: tabela Room `recipe` (id, name, ingredients JSON com gramas, steps JSON, kcal/P/C/G, sourceMessageId, createdAt, lastUsed). Um `plan` de receita já devolve itens com gramas e passos numerados (ADR-039); falta o server devolver `recipe: {name, ingredients, steps}` estruturado para o app persistir.
- **UI**: ação "Salvar receita" na bolha do plano (ao lado de "Travar como plano"); tela Receitas (lista por data decrescente, nome, kcal · P/C/G; toque abre a receita completa; segurar = excluir); entrada pela Config ou pelo menu do Chat. Duas telas novas → ADR + plano D (golds `rcpL`, `rcpD`) + plano A.
- **Chat**: o app envia `recipes[]` (≤ 30: id, nome, kcal/P/C/G, 3 ingredientes-chave; ~25 tokens cada). "Lembra a receita com X e Y?" → ação `recipe_recall {id}` e o app renderiza a receita guardada (zero alucinação, zero tokens de saída de receita). "Jantei a receita Z" → `log` copiando os números da receita (cópia determinística, como o slot `planned`). "Manda a pizza com mais frango" → `plan` com `base_recipe_id` e o server recebe a receita completa só nesse turno.
- **Custo**: +~750 tokens de entrada por turno no pior caso (30 receitas), cacheáveis.

**G. Criatividade nas sugestões**

As sugestões do Grok são boas por contexto, não por modelo: ele sabia o que havia em casa (atum, sardinha, rap10), o equipamento e o que já funcionou. Alavancas, da mais barata à mais cara:

1. Categorias de fato novas: `equipment` (air fryer), `pantry` (ingredientes citados nos últimos 7 dias), `liked` (refeição registrada após um plano = "janta que funcionou"). Entram no bloco MEMORY que já existe.
2. Regra de formato para "sem ideia": 2 opções, uma limpa e uma gostosa, ambas no `windowBudget`, com o fechamento do dia (o Grok faz isso; o ADR-043 só pede uma).
3. Exemplos de plano com proveniência (ADR-033): as transcrições do Grok enviadas pelo owner são fonte legítima.
4. `reasoning.effort=low` só em `plan` (custo na seção de custo).
5. Último recurso: segunda chamada "crítico" que revisa a sugestão contra orçamento e proteína (dobra custo do plano; só se 1–4 não bastarem).

**H. Precisão e formato da resposta**

- Fixar o bloco de saldo em todo `log` liberado: `Dia: {eaten} de {ceiling} kcal · faltam {p_gap} g de proteína` (o app tem os números; o server já recebe `remaining_kcal`). Hoje o reply fecha por slot (ADR-043), mas não repete o saldo do dia de forma padronizada.
- "1 correção que mais muda a conta": a regra existe (clarify); vale um eval que confirme a pergunta única e material nas 7 transcrições.
- Confiança alta/média/baixa já existe no card; manter.
- Eval "padrão Grok": 7 transcrições → ~40 casos de regressão (dia 1, dia 2, receita, gramas, lembrete, dia livre, estouro). Política lean: rodar só os casos do plano.

**I. Memória visível ao usuário**

Hoje `Memória da IA` é ferramenta dev (A23). O Grok responde "Anotado: leite semidesnatado" e o usuário confia porque vê. Candidato: tela de Config "O que a Tali sabe" (lista de fatos, apagar um). Tela nova, gold, baixo custo técnico, alta confiança do usuário. Prioridade média.

#### Custo por usuário (teto de 30 mensagens/dia)

Preços `gpt-6-luna` padrão (repo, 30/09/2026): entrada US$ 0,10/M, entrada cacheada US$ 0,01/M, saída US$ 0,50/M (reasoning cobra como saída). Moderação OpenAI: sem custo. Medido nos evals S19: ~6,8 k tokens de entrada por turno (96 % cacheados), ~170 de saída, US$ 0,000175/turno.

| Cenário por turno | Entrada não cacheada | Cacheada | Saída (+reasoning) | US$/turno |
|---|---:|---:|---:|---:|
| Hoje, fixture pequeno (medido) | ~300 | ~6.500 | 170 | 0,00018 |
| Hoje, usuário com 75 fatos + 42 recent + 12 msgs | ~6.500 | ~6.500 | 250 | 0,00085 |
| Proposto (hábitos, totais, receitas, ações) sem reordenar cache | ~8.000 | ~7.000 | 400 | 0,00107 |
| Proposto com blocos estáveis cacheados | ~2.500 | ~12.500 | 400 | 0,00058 |
| Proposto + `effort=low` (~600 tokens de raciocínio) | ~2.500 | ~12.500 | 1.000 | 0,00088 |
| Foto (adicional por turno com imagem, ~1,5 k tokens, estimativa) | +1.500 | 0 | 0 | +0,00015 |

Por usuário, 30 turnos/dia todos os dias + 2 compactações + 1 fechamento/dia + 1/semana:

| Cenário | US$/dia | US$/mês | R$/mês (câmbio 5,4, suposição) |
|---|---:|---:|---:|
| Hoje (leve) | 0,006 | 0,18 | 1,0 |
| Hoje (contexto cheio) | 0,027 | 0,80 | 4,3 |
| Proposto, cache reordenado | 0,019 | 0,57 | 3,1 |
| Proposto + effort low em tudo (pior caso) | 0,029 | 0,86 | 4,6 |
| Pior caso absoluto: tudo acima + 10 fotos/dia + segunda chamada "crítico" em planos | 0,045 | 1,35 | 7,3 |

Infra: e2-micro + Caddy (ADR-013) custa o mesmo de 1 a algumas centenas de usuários; a conta é dominada pelo modelo e fica abaixo de US$ 1,5/usuário/mês no pior caso. Uso realista (10 turnos/dia) é um terço disso. O limite de 30 req/min por IP não atrapalha.

#### Parecer técnico de viabilidade

| Item | Viabilidade | Esforço | Onde mexe | Gate |
|---|---|---|---|---|
| B hábitos em código | Alta | S: 2–3 dias | server (`chat_instructions`, `main`, evals) | nenhum |
| C totais por dia + cache | Alta | S+A: 2 dias | server + `PromptBuilder` | nenhum |
| E treino pelo Chat | Alta | S+A: 1–2 dias | contrato, `ChatRecorder`, `DayRepository` | nenhum |
| H formato/saldo + eval Grok | Alta | S: 2 dias | prompt + evals | ADR-033 (proveniência) |
| D ações múltiplas | Média-alta | ADR + S: 4–5 dias + A: 4–5 dias | schema, shaping, recibos em lote, Room (`chat_message.actions`) | ADR; possível ADR para `effort` |
| A onboarding O6 + A56 | Média | ADR + D + S + A: ~1 semana | novo estado, rota intake, memória | Figma (gold `o6`) |
| F receitas | Média | ADR + D + S + A: ~1,5 semana | Room v13, 2 telas, contrato, ação na bolha | Figma (2 golds) |
| G criatividade (1–3) | Alta | S: 2 dias | memória + prompt + exemplos | ADR-033 |
| I memória visível | Alta | ADR + D + A: 3 dias | Config, tela nova | Figma |

Restrições que não mudam: LLM só no server, estado do usuário só no aparelho (Room), server sem persistência de texto do usuário, invite gate. Tudo acima respeita isso; receitas e fatos ficam no aparelho (sem sync entre dispositivos, já aceito).

Riscos reais:

1. O prefixo do prompt já tem 28–32 k caracteres e os evals do S19 mostram casos instáveis. Cada regra nova custa confiabilidade. Por isso B (tirar lógica do prompt) vem antes de D (schema maior).
2. `effort=none` é constituição (AGENTS). Mudar para `low` em casos específicos pede ADR e medição no eval (latência p95 hoje ~4 s; raciocínio adiciona 1–3 s).
3. Ações múltiplas e clarificação: a regra de "até 3 rodadas" precisa valer por ação, não por turno, senão uma dúvida no almoço segura o café inteiro.
4. Nenhum item aciona o production gate; O6 e Receitas precisam do gate Figma (plano D, revisão do owner).

Ordem sugerida (uma frente por vez, uma pasta por plano):

1. S: hábitos em código + totais por dia + bloco de saldo + eval Grok (B, C, H). Resolve "mesmo café de ontem" e a crítica com dados sem tela nova.
2. ADR + S + A: ações múltiplas com treino incluído (D, E). Resolve o caso do tester e "registrar o dia inteiro".
3. ADR + D + S + A: onboarding O6 + A56 (A). Entrega o dia 0.
4. ADR + D + S + A: receitas (F) e, junto, as categorias `pantry`/`liked` (G).
5. Memória visível (I) e avaliação de `effort=low` em `plan` (G.4) com custo medido.

---

## 3. Instruções para outros agentes

Você foi chamado para dar uma segunda opinião sobre este brainstorm. Leia este arquivo inteiro antes de escrever. Depois:

1. **Leia o repositório antes de opinar.** Comece por `AGENTS.md`, depois `docs/README.md`, `docs/api-contract.md`, `server/chat_instructions.py` (regra `history`, `memory_use`, `memory_changes`), `server/evals/run.py` (preços), `apps/android/app/src/main/java/app/fibrai/android/feature/chat/PromptBuilder.kt`, `apps/android/app/src/main/java/app/fibrai/android/domain/MemoryRules.kt`, `docs/android/specifications/room-v2.md` e `docs/produto/specifications/perfil-onboarding.md`. Opinião sem leitura do código não vale.
2. **Não altere nada fora deste arquivo.** Nenhum código, nenhum doc, nenhum plano, nenhum ADR. Este arquivo é discovery; a implementação segue `docs/sdd/README.md` e só começa com um plano aprovado pelo owner.
3. **Escreva a sua avaliação na seção 4**, em uma subseção nova com o título `### <nome do modelo> (effort: <valor>)`. O nome do modelo é obrigatório (o identificador exato, por exemplo `gpt-6-luna`, `claude-opus-5-5`, `grok-4`). O effort só entra quando o modelo ou a sessão expõe esse parâmetro; se não houver, use `### <nome do modelo>`. Não edite a avaliação de outro agente; acrescente a sua abaixo da última.
4. **Cubra, nesta ordem, com esses títulos:**
   - **Concordo / discordo do diagnóstico** — para cada linha da tabela de diagnóstico da seção 2.2, diga se concorda, com evidência do código quando discordar.
   - **Soluções** — para cada item A–I: mantém, muda ou descarta, e por quê. Proposta alternativa quando houver, com onde mexe (server / app / design) e qual regra do `AGENTS.md` ela toca.
   - **O que faltou** — pontos de melhoria que a avaliação anterior não viu, com base nas transcrições do Grok da seção 1.
   - **Custo** — refaça a conta por usuário com 30 mensagens/dia usando os preços de `server/evals/run.py`. Se discordar das premissas de tokens, mostre as suas.
   - **Parecer técnico** — viabilidade, esforço e risco por item, e a ordem de execução que você recomenda. Diga explicitamente o que exige ADR, plano de design (gate Figma) e o que toca o production gate (`docs/content-policy/production-gate.md`).
5. **Regras de escrita:** pt-BR, direto, números em tabela, sem elogio ao trabalho anterior, sem repetir o que já está escrito (aponte a seção e discorde ou complemente). Máximo de 1.500 palavras. Identificadores de código em crase. Nenhuma chave, token ou segredo.
6. **Não aciona o production gate:** este arquivo não pede build prod, Play ou release pública. Se a sua avaliação precisar citar produção, liste os bloqueadores abertos do gate em vez de propor o trabalho.

---

## 4. Avaliações de outros agentes

(acrescentar abaixo, uma subseção por modelo)

### gpt-6-astra (effort: ultra)

#### Concordo / discordo do diagnóstico

A conclusão “o modelo não é o gargalo” não está demonstrada: faltou comparação controlada mantendo contexto, regras e tarefas iguais.

| Dimensão da seção 2.2 | Parecer e evidência |
|---|---|
| Dia inicial | Concordo com a ausência de rotina alimentar; “só números” ignora nomes/horários e tom já coletados. |
| Mesmo café de ontem | Parcial: `history/PARTICULAR DAY` já permite copiar um registro. `PromptBuilder.recent` já envia histórico; falta execução confiável, não simplesmente mais contexto. |
| Várias ações | Concordo: `chat_format` admite uma estimativa, além dos skips. |
| Treino | Concordo; faltam operação e tratamento explícito no escopo do Chat. |
| Receitas | Parcial: existe objeto `planned_meal` em Room. Faltam biblioteca durável, ingredientes e preparo estruturados. |
| Crítica | Parcial: `tone_duro` já usa `RECENT`. Faltam teto histórico e cobertura dos registros para sustentar comparações. |
| Formato | Parcial: `format` já define apresentação rica. Saldo confiável precisa acompanhar a gravação; pergunta única não é a regra vigente. |

#### Soluções

| Item | Decisão, alternativa e fronteira |
|---|---|
| A | **Muda.** Começar com descoberta opcional no Chat existente, focada em rotina, preparo e preferências; validar antes de adicionar onboarding. Não acoplar A56: peso-meta não resolve recuperação contextual e conflita com o limite do ADR-044. Server/app; eventual fluxo novo exige decisão de produto. |
| B | **Muda.** Manter busca/cópia determinísticas; descartar “mais frequente = de sempre” e regex como solução geral. O modelo identifica uma referência tipada; código resolve fonte, data e slot, sem inventar hábito. Preservar ADR-023/029 ou propor sucessor para mudar a semântica. Server/app. |
| C | **Mantém, com correções.** Agregados calculados no app, incluindo dias incompletos, pulados e teto efetivo histórico. `Closures.week` compara slot com divisão igual do teto; isso não prova estouro da janela real. Cache é otimização mensurável. App/server; ADR-043/044. |
| D | **Mantém, faseado.** Ações tipadas com identificadores, dependências, confirmação, idempotência e retomada após falha. Aplicar ações resolvidas; gerar plano dependente após atualizar o estado. Multiplicar `commitRecord` não resolve sozinho consistência e Desfazer. Server/app/design; ADR-028/032/047. |
| E | **Mantém, antecipado.** Operação estreita para gasto informado, com data, substituir versus adicionar e Desfazer. Recalcular crédito no app; não inferir gasto, duração ou pulso. Server/app/design; preservar eat-back e vedação Health/Xiaomi. |
| F | **Mantém.** Receita versionada, rendimento e porção consumida; lembrar a original e adaptar são operações distintas. Busca local por nome/ingredientes, com recuperação dos detalhes, evita enviar toda a biblioteca. Salvar não significa comer. App/server/design; Room e ADR-046. |
| G | **Muda.** Avaliar criatividade já, sem esperar receitas. Duas alternativas já constam em `plan`. Comida mencionada não prova estoque; registro não prova aprovação. Equipamento pode ser preferência explícita. Novas categorias exigem contrato e `MemoryRules`, não só prompt. Descarto crítico adicional antes de medir ganhos. |
| H | **Muda.** Saldo no app após gravação; projeção identificada enquanto pendente. Agrupar dúvidas materiais, conforme ADR-026, sem impor pergunta única. Exemplos globais derivados destas transcrições são proibidos pelo [ADR-033](docs/content-policy/adrs/ADR-033-global-chat-example-provenance.md), inclusive paráfrases. Evals precisam de situações sintéticas independentes. Server/app/design. |
| I | **Mantém.** Permitir corrigir e esquecer, mostrando origem e confirmação efetiva da gravação. Não basta tornar público o editor dev. App/design; sucessor do ADR-019 e regras de memória. |

Em A, capacidade disponível não basta: `ChatViewModel` adia rotinas até registrar uma refeição; `MemoryUpdate` não traz macros. Em B, `Fact` guarda macros, mas `PromptBuilder.chatFacts` não os transmite. Esse elo precisa ser resolvido.

#### O que faltou

As transcrições exigem continuidade da proposta: “quanto iogurte?” e “fiz exatamente” devem apontar para a versão escolhida, inclusive após compactação. Alternativas precisam de identidade própria; hoje apenas a primeira entra em `estimate`.

Também faltam unidades cruas/cozidas, medidas caseiras e extras sem slot obrigatório. O treino recebe duração e pulso sem evidência na transcrição; a pizza lembrada muda quantidades. Não transformar fluência em prova de precisão. Soma correta dos itens tampouco valida seus valores nutricionais.

`RECENT` pode perder refeições por limite e truncamento; ausência não prova jejum. `FactMemory` usa arquivo criptografado, separado de Room: memória e recibos precisam de recuperação consistente. Servidor sem estado não significa ausência de logs: o contrato permite captura textual no dev.

#### Custo

Projeção pelos preços locais solicitados, sem atualizar tarifas. Tokens hipotéticos, não medição. Saída inclui JSON e raciocínio, sem contar raciocínio novamente.

| Premissa | Valor |
|---|---:|
| Uso | 30 mensagens/dia × 30 dias = 900 turnos |
| Entrada / entrada cacheada / saída, US$ por milhão | 0,10 / 0,01 / 0,50 |
| Compactações mensais; entrada/saída por chamada, sem cache | 180; 2.500 / 300 |
| Fechamentos mensais; entrada/saída por chamada, sem cache | 34; 1.000 / 150 |
| Custo auxiliar mensal | US$ 0,07795 |

O fluxo comum de `compactBlocks`, com pares usuário/assistente e sem dúvidas abertas, implica a frequência abaixo; não confundir blocos máximos por envio com chamadas diárias.

| Compactações/dia | Hipótese anterior | Fluxo comum |
|---|---:|---:|
| Quantidade | 2 | 6 |

Custo mensal = turnos × custo ponderado dos tokens + auxiliares.

| Cenário | Entrada não cacheada | Cacheada | Saída | US$/turno | US$/mês total |
|---|---:|---:|---:|---:|---:|
| Atual cheio, hipótese anterior | 6.500 | 6.500 | 250 | 0,000840 | 0,834 |
| Proposto, cache parcial | 8.000 | 7.000 | 700 | 0,001220 | 1,176 |
| Proposto, cache otimista | 2.500 | 12.500 | 700 | 0,000725 | 0,730 |
| Proposto, sem cache | 15.000 | 0 | 700 | 0,001850 | 1,743 |

Aumentei a saída para representar receitas e ações. O cenário sem cache já ultrapassa o suposto “pior caso absoluto” anterior.

| Adicional hipotético | US$/mês |
|---|---:|
| Raciocínio: 600 tokens extras em cada turno | +0,270 |
| Fotos: 10/dia, supondo 1.500 tokens adicionais/foto | +0,045 |

Não inclui críticos, ajustes repetidos, infraestrutura, impostos ou câmbio. Fotos precisam de medição. `UsageTransport` retém apenas o último `usage`: a chamada de ajuste pode ocultar custo anterior no relatório.

`_chat_text` já põe perfil/memória antes do histórico; `DAY.local_time` interrompe a estabilidade antes de `RECENT`. `chat_format` varia com IDs. Reordenar ajuda, mas o [cache exige prefixo idêntico e configuração compatível](https://developers.openai.com/api/docs/guides/prompt-caching). Capacidade da VM e interferência entre usuários no limite por IP exigem teste de carga.

#### Parecer técnico

Estimativas de dias de trabalho, incluindo validação; excluem espera pelo owner. Não são compromisso.

| Item | Viabilidade | Dias | Risco principal |
|---|---|---:|---|
| A | Alta, fluxo opcional | 3–6 | Rotina declarada virar consumo fictício |
| B | Alta para cópia explícita | 4–7 | Referência ambígua ou texto truncado |
| C | Alta | 3–5 | Criticar histórico incompleto |
| D | Média-alta | 10–18 | Gravação parcial, duplicação, estado obsoleto |
| E | Alta | 3–5 | Crédito duplicado |
| F | Alta | 8–14 | Receita alterada afetar registros antigos |
| G | Alta para contexto | 3–6 | Inferências falsas de gosto/estoque |
| H | Alta | 3–5 | Texto divergir do estado gravado |
| I | Alta | 3–5 | Esquecimento reaparecer pelo histórico |

Ordem: **H (evidência) → B → E → A/G → C → D → F/I**. Validar referências e memória antes de ampliar ações; criatividade e experiência inicial não precisam esperar a biblioteca.

ADR: D/E/F/I e novo fluxo A; também mudanças de hábitos/categorias, contagem de clarificações por ação ou modelo/effort. B/C/H não exigem automaticamente ADR novo se preservarem decisões vigentes. A56 permanece separado. Cada implementação requer plano aprovado por pasta.

Gate Figma: novas telas, recibos e estados de D/E/F/I, onboarding novo em A e apresentação alterada em H; não apenas onboarding e receitas. Revisão, exportação e comparação no emulador antecedem conclusão.

Este discovery não aciona entrega pública. Ampliação de contexto, treino e memória requer revisão em `content-policy`. Conforme o [production gate](docs/content-policy/production-gate.md), permanecem:

| Bloqueador / plano | Falta |
|---|---|
| PG2 — [CP8](docs/content-policy/plans/out_of_scope/cp8-public-legal-pack.md) | Revisão jurídica, publicação e aceite |
| PG3 — [CP9](docs/content-policy/plans/out_of_scope/cp9-production-audit-and-containment.md) | Auditoria, retenção, contenção e desligamento da captura bruta |
| PG4 — [CP7](docs/content-policy/plans/out_of_scope/cp7-specialist-detection.md) | Detecção especializada ou risco aceito com parecer jurídico |
| PG5 — [CP6](docs/content-policy/plans/out_of_scope/cp6-production-readiness.md) | Validação integrada aprovada |
| PG6 — [D16](docs/design/plans/completed/d16-tone-and-closures.md), [S30](docs/server/plans/completed/s30-tone-formatting-planned-slot.md), [A60](docs/android/plans/pending_manual_validation/a60-tone-formatting-planned-skips.md) | Verificação do tom no dev e evidência de fechamento no gate |

### grok-4.7 high

Effort high. Prefixo `meal_changes` + seco: 42.690 caracteres (`assemble`); duro, 43.856. A seção 2 cita 28 mil. Preços em `server/evals/run.py`: 0,10 / 0,01 / 0,50 por milhão. Raciocínio já está na saída.

#### Concordo / discordo do diagnóstico

"Mesmo café de ontem" já é dia particular: o cue `particular-day` manda copiar o dia nomeado e não tratar como hábito. `_recent_line` escreve data e dia da semana. `PromptBuilder.recent` manda até 42 refeições dos 7 dias anteriores (ADR-023). O 3/10 de `mesmo-cafe-ontem-recent` (S19, S21, S23) é a regra certa descumprida com `reasoning.effort=none`.

| Dimensão da §2.2 | Parecer |
|---|---|
| Dia 0 | Parcial. O1–O5 não perguntam o que a pessoa come. Já gravam corpo, teto, eat-back, nome e hora dos slots, alvos e tom. |
| Mesmo café de ontem | Discordo da causa. O `RECENT` já traz o dia. Dois dias idênticos (branch B) são a frase "de sempre". |
| Várias ações | Concordo. Um `estimate` em `chat_format`. `skip_slots` não registra várias refeições. |
| Treino | Concordo. Kcal de treino está fora do `in_scope`; a frase pode ser recusada antes de faltar a ação. `DAY.workout_kcal` só lê a Home. |
| Receitas | Parcial. `planned_meal` é a reserva de um slot, sem ingredientes, passos nem biblioteca. |
| Crítica | Parcial. `tone_duro` já lê `DAY`, `BUDGET` e `RECENT` e não soma o dia. `seco` não critica (ADR-044). `Closures.week` usa teto dividido pelos slots, não a janela do ADR-043. |
| Formato | Discordo. Card, confiança e ADR-045 já existem. `closing` proíbe totais do dia no `reply`. Saldo escrito pelo modelo viola o ADR-043. |

A cópia falha com a instrução já no prefixo. Um `estimate` só, e a rotina sem números no prompt, limitam também um modelo que obedecesse.

#### Soluções

| Item | Decisão |
|---|---|
| A | **Muda.** Sem O6 e sem A56 aqui. Preferência explícita grava na resposta. Rotina espera o registro (`waitsForRecord`) e só então ganha kcal (`withMeal`). O card exige permanente ou 3 dias, com kcal. Tela nova não copia o café de ontem. Peso-meta esbarra no ADR-044. O6 seria sucessor do ADR-012 e gold `o6`. |
| B | **Muda.** Um slot com uma linha naquele dia: o server copia texto e números do `RECENT`. "De sempre" fica nas branches A–C. "Mais frequente = de sempre" mudaria o ADR-023: descarto. Mandar no `MEMORY` os kcal/P/C/G do `Fact`. `ChatFact` e `_memory_lines` mandam id, categoria, texto e dias. A branch A pede nutrição que o turno não vê. O card usa esses números e não chama o modelo. |
| C | **Mantém, corrigido.** Sete totais no app, com dia incompleto, pulo e teto efetivo. `over_slot` semanal não é janela estourada. `RECENT` antes de `DAY.local_time` cacheia o miolo do dia. Histórico fica de fora. |
| D | **Mantém, tarde.** `actions[]` com id, ordem e Desfazer do lote. Vários `commitRecord` não desfazem metade. As 3 rodadas do ADR-026 valem por refeição pendente. |
| E | **Mantém, antes de D.** Número digitado, substituir ou somar, Desfazer. O app recalcula o crédito. Eat-back padrão é 50% (O2); o Grok do owner era 0%. Sem pulso, duração, Health ou Xiaomi. A frase entra no `in_scope`. |
| F | **Mantém, enxuto.** Tabela nova. Busca no aparelho; o turno leva a receita achada. Salvar não registra. Editar não reescreve registro antigo. |
| G | **Muda.** `plan` já pede duas opções; o `estimate` descreve a primeira. Falta id na segunda: "fiz a 2" perde o alvo depois do compact. Equipamento só declarado. Comida citada não é despensa. Sem chamada crítica extra. `effort=low` exige ADR. |
| H | **Muda.** Saldo pelo app, depois da gravação, com o Room. Projeção marcada como projeção. Dúvidas materiais juntas no `question` (ADR-026). Seção 1 não vira exemplo nem fixture (ADR-033). |
| I | **Muda.** Frase do app com o fato gravado. A tela vem depois. O editor dev (ADR-019) não é essa tela. |

Descarto as 8 routines do Grok. Push por horário e fechamento 22:00 / domingo 22:00 já existem (ADR-044). Essas chamadas não estão na conta da seção 2.

#### O que faltou

Branch A cega: o card grava a rotina sem modelo; "café de sempre" no turno estima de novo. O prefixo que o app manda tem 42.690 caracteres, e o caso de cópia já oscila nele. `UsageTransport` fica com a última chamada; o retry do ADR-039 pode esconder a primeira. Foto de 2048 px (ADR-018) não tem token medido.

#### Custo

900 turnos. Auxiliares à parte. Cada turno grava user e assistant. Com `MAX_RAW` 12 e `KEEP_RAW` 4, o compact cai na 6ª mensagem e depois a cada 4: 7/dia, 210/mês. A seção 2 usou 2. Compact: 3.382 caracteres. Fechamento: 34 chamadas, ~1,4 mil caracteres. Cache do chat não cobre as duas rotas.

| Cenário | Fora do cache | Cache | Saída | US$/turno | US$/mês, só chat |
|---|---:|---:|---:|---:|---:|
| Fixture S23 (8.070 / 6.790 cache / 188) | 1.280 | 6.790 | 188 | 0,000290 | 0,26 |
| Hoje, uso cheio (hipótese: +2.400 do usuário) | 3.700 | 6.800 | 250 | 0,000563 | 0,51 |
| Proposto: totais e macros da rotina | 4.000 | 6.800 | 350 | 0,000643 | 0,58 |
| Proposto, `RECENT` estável no prefixo (~1.500) | 2.500 | 8.300 | 350 | 0,000508 | 0,46 |
| Teto: memória e histórico no limite, cache só da instrução | 11.300 | 6.800 | 400 | 0,001398 | 1,26 |
| Teto sem cache | 18.100 | 0 | 400 | 0,002010 | 1,81 |

| Extra no mês | US$ |
|---|---:|
| 210 compacts, 1.400 de entrada sem cache e 200 de saída | 0,050 |
| 34 fechamentos, 550 de entrada e 80 de saída | 0,003 |
| `effort=low` em todo turno, +600 de saída | 0,270 |
| 10 fotos/dia × 1.500 tokens, hipótese | 0,045 |

Uso cheio hoje, com compact e fechamento: cerca de US$ 0,56/mês. Proposto, sem `effort=low` e sem foto: cerca de US$ 0,63, ou US$ 0,51 se o miolo do dia cachear. O US$ 0,57 da seção 2 depende de um prefixo de 12,5 mil tokens cacheados que `local_time` e o histórico não produzem. Memória e histórico no teto do contrato já passam de US$ 1,3 com cache da instrução, e de US$ 1,8 sem cache. e2-micro fica de fora. 30 req/min por IP não segura 37 chamadas espalhadas no dia.

#### Parecer técnico

Dias incluem teste e excluem espera do owner.

| Item | Viabilidade | Dias | Risco | ADR | Figma | Production gate |
|---|---|---:|---|---|---|---|
| B, cópia do dia nomeado + macros no prompt | Alta | 4–6 | Slot ambíguo copia a linha errada | Não, se o resultado seguir o ADR-023. Sim, se "de sempre" mudar de sentido | Não | Não |
| C, totais | Alta | 3–4 | Crítica de dia incompleto | Não | Não | Não |
| H, saldo no app | Alta | 3–5 | Frase divergir do Room | Não | Sim, se bolha ou recibo desenhados mudarem | Não |
| E, treino | Alta | 3–5 | Crédito duplicado | Sim. Escopo em content-policy | Sim, se o recibo for estado novo | Não |
| G, id da segunda opção | Alta | 3–5 | Plano antigo sem id | Sim, se o schema do plano mudar | Só com controle novo na bolha | Não |
| D, ações | Média | 12–18 | Lote pela metade | Sim, sucessor de ADR-028/032/047 | Sim, se N recibos não couberem em `chatSK` | Não |
| A, pergunta no chat, sem tela | Alta | 3–5 | Rotina declarada virar refeição que não houve | Não, na memória atual | Não | Não |
| A como O6 | Média | ~7 | ADR-012 | Sim | Sim, `o6` | Não |
| F, receitas | Média | 10–15 | Edição reescrever registro antigo | Sim. Room novo | Sim, duas telas | Não |
| I, frase / tela | Alta | 2–3 / 4–5 | Fato esquecido voltar pelo histórico | Tela: sucessor do ADR-019 | Tela: sim | Não |

Ordem: **B → C → H → E → G (id da opção) → D → A no chat → F → I.** `effort=low` só como medição, com ADR, depois de B mostrar o efeito no 3/10. O6 só se a pergunta no chat não antecipar o hábito.

Este arquivo não pede produção. Abertos, na tabela já escrita acima: PG2 (CP8), PG3 (CP9), PG4 (CP7), PG5 (CP6), PG6 (A60 em validação manual). Incluir treino no `in_scope` é mudança da política de conteúdo dentro do plano de E. Não fecha bloqueador e não libera flavor prod, Play nem invite aberto.

---

## 5. Conclusão

Síntese escrita por `claude-fable-5-1` (effort: high) em 08/10/2026, depois de reler as três avaliações e de verificar no código cada alegação concreta dos outros dois agentes. O que está abaixo substitui a ordem e os itens da seção 2 onde houver conflito.

### 5.1 Verificação das alegações

| Alegação (quem) | Verificado em | Resultado |
|---|---|---|
| Prefixo de 42.690 caracteres, não 28 mil (grok-4.7) | `chat_instructions.assemble` para as quatro branches | Correto: legacy 38.929, legacy_duro 40.095, meal_changes 42.690, meal_changes_duro 43.856. O número da seção 2 era o do S19, anterior a S21–S30. |
| `closing` proíbe total do dia no `reply` (grok-4.7) | regra `closing`, `chat_instructions.py` | Correto: "No line for the answered meal and no day totals". O saldo do dia no `reply` (item H) viola o ADR-043. |
| Pergunta única contraria o ADR-026 (ambos) | `AGENTS.md` § Product, ADR-026 | Correto: "all doubts at once, at most 3 rounds". |
| ADR-033 veta exemplos e fixtures derivados das transcrições (ambos) | ADR-033 decisões 1 e 3 | Correto, inclusive paráfrase e anonimização. Regressão só com situações sintéticas independentes. |
| `plan` já pede duas opções e o `estimate` descreve só a primeira (grok-4.7) | regra `PLAN`, linhas 403–413 | Correto. O item G.2 já existe; o que falta é identidade da segunda opção. |
| `DAY.local_time` fica antes de `RECENT` e quebra o cache (ambos) | `_chat_text` em `main.py`: PROFILE → MEMORY → DAY → RECENT → DIGESTS → HISTORY | Correto. O cache de prefixo para em DAY. |
| `chatFacts` não envia kcal/P/C/G da rotina (ambos) | `PromptBuilder.chatFacts`, `Fact` em `MemoryRules.kt`, ADR-023 § fato | Correto: `Fact` guarda os macros do último registro, `ChatFact` envia id, tipo, categoria, chave, texto, slot e dias. A branch A do prompt pede "nutrition" que o turno nunca recebe. |
| Rotina só nasce com registro; `MemoryUpdate` não carrega macros (ambos) | `waitsForRecord` em `ChatViewModel`, `withMeal` em `MemoryRules`, ADR-023 decisão 4 | Correto. Uma rotina declarada no onboarding não teria números sem mudança de contrato. |
| Treino fora do `in_scope` (grok-4.7) | regra `scope` e `docs/content-policy/specifications/content-policy.md` | Correto: nenhuma menção a exercício. "Treino 610 kcal" pode ser recusado hoje. |
| Compactação 7 vezes/dia, não 2 (grok-4.7) | `MAX_RAW` 12, `KEEP_RAW` 4 em `PromptBuilder` | Correto para 30 turnos/dia. Impacto no custo: ~US$ 0,05/mês. |
| `Closures.week` usa teto ÷ slots, não a janela (ambos) | `Closures.kt` linha 136 | Correto. `over_slot` semanal é heurística, não prova de estouro da janela. |
| `FactMemory` fica em arquivo criptografado, fora do Room (gpt-6-astra) | `FactMemory.kt` | Correto (`memory.bin`). |
| Peso-meta conflita com o ADR-044 (ambos) | ADR-044 decisão 1, A56 | Parcial: o ADR-044 proíbe falar de corpo e peso; o A56 prevê a meta citada sem comentário sobre o corpo e exige ADR próprio. Decisão do owner, fora deste brainstorm. |
| "Modelo não é o gargalo" não demonstrado (gpt-6-astra) | eval `mesmo-cafe-ontem-recent` 3/10 com a regra presente | Parcial: não houve comparação controlada. A conclusão operacional continua: tirar a execução determinística do modelo resolve independentemente do modelo. Uma medição de `effort=low` no mesmo fixture sintético entra como evidência barata, sem mudar produto. |

### 5.2 Onde concordo com as outras avaliações (mudo a seção 2)

- **H.** Saldo do dia e proteína faltante são do app, calculados do Room depois da gravação, nunca prosa do modelo. Enquanto a estimativa está pendente, o app mostra projeção marcada como projeção. Dúvidas materiais vão juntas na `question`, sem "1 correção".
- **Evals.** Nenhuma transcrição da seção 1 vira exemplo, fixture ou paráfrase. Os casos de regressão são sintéticos, perfis variados, cobrindo os mesmos modos de falha abstratos (cópia de dia nomeado, várias refeições num turno, treino, receita lembrada, dia livre acima do teto).
- **B.** "Mais frequente = de sempre" está descartado: mudaria o ADR-023, que define rotina como fato reforçado por registro. Fica: cópia determinística de um dia nomeado em código, e macros da rotina enviados no MEMORY.
- **A.** Sem tela O6 na primeira fase. A descoberta de rotina acontece no próprio Chat, na primeira abertura com memória vazia, opcional e pulável. O6 só se a medição mostrar que a pergunta no Chat não entrega o dia 0. A56 segue separado, com ADR próprio.
- **E.** Treino antes das ações múltiplas, como operação estreita: número digitado, substituir ou somar, Desfazer, crédito recalculado no app, sem pulso, duração, Health ou Xiaomi. Exige mudança do `in_scope` em `content-policy`.
- **G.** Sem chamada "crítico" extra. Equipamento só quando declarado. Comida citada não é despensa; registro depois de plano não prova aprovação. A segunda opção do plano precisa de identidade própria para "fiz a 2" e "quanto de iogurte?" sobreviverem à compactação.
- **D.** Lote com id por ação, ordem, dependências e Desfazer do lote inteiro; a contagem de rodadas do ADR-026 vale por refeição pendente. Vários `commitRecord` soltos não resolvem.
- **F.** Busca da receita no aparelho; o turno leva só a receita encontrada. Salvar não registra; editar não reescreve registro antigo; receita versionada com rendimento e porção.
- **I.** A frase "Anotado: {fato}" vem do app, com o fato realmente gravado, antes de qualquer tela. O editor dev (ADR-019) não é a tela de produto.
- **Custo.** As três contas convergem: uso cheio hoje ≈ US$ 0,5–0,8/usuário/mês; proposto ≈ US$ 0,5–0,7; teto do contrato sem cache ≈ US$ 1,8. O cenário de 12,5 mil tokens cacheados da seção 2 era otimista.

### 5.3 Onde mantenho a posição

- **Tirar lógica do prompt antes de ampliar o schema.** As duas avaliações colocam B cedo; mantenho B como primeiro plano, junto com C, porque os dois são server-only e atacam a dor relatada pelo owner.
- **Ordem de D e A.** gpt-6-astra põe A/G antes de C e D; grok-4.7 põe D antes de A. Mantenho D antes da descoberta no Chat: "registrar o dia inteiro" e "jantei X, sugere a janta" foram pedidos explícitos do owner e do tester; a descoberta no Chat depende de poder gravar várias rotinas numa mensagem, que é o próprio D.
- **O6 continua no horizonte.** O owner declarou que o produto precisa funcionar no dia 1 para quem pagou; uma pergunta pulável no Chat pode não bastar. Fica condicional, não descartado.
- **Medição de `effort`.** Entra como medição dentro do primeiro plano de server (eval sintético, sem mudar o produto). ADR só se o resultado justificar adotar.

### 5.4 Planos necessários, por ordem de execução

Numeração provisória, seguindo os últimos ids de cada contexto (S30, A63, D21, CP9, ADR-048). Cada plano tem uma pasta; um `/goal` por plano; aprovação explícita do arquivo antes do código. Nenhum aciona o production gate.

**Plano 1 — S31 (server): referência de dia nomeado em código, macros da rotina, totais da semana e cache**

1. Detecção determinística no server de referência a dia nomeado ("de ontem", "de anteontem", "de segunda", "mesmo X de {dia}") combinada com slot; o server encontra a linha do `RECENT` e monta um bloco `COPY_SOURCE` com data, dia da semana, texto e números. A regra PARTICULAR DAY do prompt vira "copie COPY_SOURCE". Ambiguidade (duas linhas do slot no dia, slot não identificado) vira pergunta, nunca escolha.
2. Contrato: `facts[]` ganha `kcal`, `p`, `c`, `g` opcionais; `_memory_lines` imprime os números da rotina. A branch A passa a ter o que pede.
3. Contrato: `recent_days[]` (até 7 linhas: data, kcal, P, C, G, teto efetivo, registrado sim/não, slots pulados). O prompt do tom `duro` cita padrão da semana só com dias registrados; nenhum dia incompleto vira crítica.
4. `_chat_text`: PROFILE → MEMORY → RECENT → RECENT_DAYS → DAY → DIGESTS → HISTORY, com `local_time` no bloco DAY depois dos blocos estáveis. Medir a fração cacheada antes e depois no eval.
5. Evals sintéticos novos (sem origem nas transcrições): cópia de dia nomeado (3 perfis), rotina com macros, crítica com `recent_days`. Medição de `reasoning.effort=low` nos mesmos casos, só como evidência registrada no Results.
6. Sem mudar ADR-023; se o desenho exigir mudar a semântica de "de sempre", parar e abrir ADR.

**Plano 2 — A64 (app): envio dos novos campos, saldo do dia pelo app, frase de memória**

1. `PromptBuilder` envia `facts[].kcal/p/c/g` e `recent_days` calculados do Room (reaproveitar `Closures`, corrigindo o teto efetivo por dia e marcando dia sem registro).
2. Recibo de registro mostra uma linha de saldo calculada do Room depois da gravação: kcal comidos de teto efetivo e proteína faltante. Estimativa pendente mostra projeção com rótulo de projeção. Se a bolha ou o recibo desenhados mudarem, plano D22 antes (gate Figma); se couber no gold atual (`chatF`), sem design.
3. Frase "Anotado: {texto do fato}" gerada pelo app ao aplicar um `memory_updates` permanente explícito, com o fato lido de volta do `FactMemory`.
4. Testes unitários do `PromptBuilder` e do cálculo de saldo; captura dos fluxos tocados no emulador.

**Plano 3 — ADR-049 + CP10 + S32 + A65: treino pelo Chat**

1. ADR-049: gasto de treino informado pelo Chat é operação própria, número digitado, substituir ou somar ao valor do dia, Desfazer; nada inferido (sem duração, pulso, Health, Xiaomi); crédito recalculado no app pela regra de eat-back vigente.
2. CP10 (content-policy): `in_scope` passa a incluir a declaração de gasto de treino em kcal; refusals e sinais de segurança inalterados.
3. S32: intent/ação `workout {kcal, mode: replace|add}` no contrato, shaping, `record: auto` quando o número é explícito, evals sintéticos.
4. A65: grava `day.workoutKcal` em `commitRecord` com recibo e Desfazer; recibo novo passa por D22 se não couber nos golds de recibo existentes.

**Plano 4 — ADR-050 + S33 + A66: ações múltiplas por mensagem**

1. ADR-050, sucessor de ADR-028/032/047: `actions[]` tipadas (`log`, `skip`, `plan`, `workout`, `question`) com id, ordem, dependência (um `plan` depois de `log` usa o estado já atualizado), rodadas de clarificação por refeição pendente, Desfazer do lote inteiro.
2. S33: capability `actions: true`; schema estrito com até 6 ações; shaping reaproveita ADR-042 (soma por itens), ADR-032 (add/revise), ADR-047 (ordem); uma pergunta segura só a ação com dúvida material, as demais saem resolvidas; evals sintéticos para dia inteiro numa mensagem, log + plan, workout + log.
3. A66: aplicação em uma transação por lote com `RecordGuard`, um recibo por ação, Desfazer do lote; `chat_message.actions` (Room v13). D22 se N recibos não couberem em `chatSK`.

**Plano 5 — ADR-051 + S34 + A67: identidade das opções do plano e descoberta de rotina no Chat**

1. ADR-051: cada opção de um plano tem id e `estimate` próprios; "fiz a 2", "quanto de iogurte?" e "travar a 1" apontam para a opção, inclusive depois da compactação (digest preserva id e nome das opções). Primeira abertura do Chat com memória vazia: Tali faz até quatro perguntas puláveis (café, almoço, janta, preferências fixas e equipamento declarado); as respostas viram fatos pela regra atual (preferência permanente explícita; rotina só com macros estimados pelo server e marcada como declarada, sem contar dia de registro). Categorias novas `equipment` (declarado) e `liked` (registro que cita um plano do dia) exigem contrato e `MemoryRules`.
2. S34: `plan.options[]` no contrato, regra de "sem ideia" mantida (duas opções), digest com ids; fluxo de descoberta como turno de Chat (`intake: true`), devolvendo `memory_updates` com macros; evals sintéticos.
3. A67: opções na bolha com ação por opção (gate Figma D22 para o controle novo), persistência dos ids, fluxo de descoberta na primeira abertura, aplicação dos fatos declarados.

**Plano 6 — ADR-052 + D23 + S35 + A68: receitas salvas**

1. ADR-052, sucessor de ADR-012 para as telas: entidade receita (nome, ingredientes com gramas, passos, kcal/P/C/G, rendimento, porção, versão, origem), salvar não registra, editar cria versão, registro guarda a versão usada.
2. D23: golds `rcpL` (lista, mais recente primeiro, nome e kcal · P/C/G) e `rcpD` (detalhe), ação "Salvar receita" na bolha do plano, entrada pela Config; revisão do owner no Figma e exportação.
3. S35: `plan` devolve `recipe {name, ingredients, steps}` estruturado; ações `recipe_recall {id}` e `log` por receita (cópia determinística dos números da versão); o app envia só o índice (`recipes[]`: id, nome, macros, ingredientes-chave, ≤ 30) e, quando a busca local acha uma receita citada, a receita completa só nesse turno; evals sintéticos.
4. A68: Room v13 ou v14 (`recipe`, `recipe_version`), busca local por nome e ingrediente, telas, ação na bolha, registro pela receita com recibo; capturas contra os golds.

**Plano 7 — ADR-053 + D24 + A69: memória visível; O6 condicional; A56**

1. ADR-053, sucessor de ADR-019: tela de produto "O que a Tali sabe" na Config: lista de fatos com origem, apagar, corrigir; o esquecimento não volta pelo histórico (digest não reconstrói fato apagado).
2. D24: gold da tela; A69: tela, ações, testes.
3. O6 só se a medição do Plano 5 mostrar que a descoberta no Chat não cobre o dia 0: ADR próprio, gold `o6`, plano S e A.
4. A56 (peso-meta): reentrada pelo seu próprio arquivo, com o ADR que ele exige, depois dos planos acima.

### 5.5 Gates por plano

| Plano | ADR | Content-policy | Figma | Production gate |
|---|---|---|---|---|
| 1 S31 | Não (para se precisar mudar ADR-023) | Não | Não | Não |
| 2 A64 | Não | Não | Só se bolha/recibo mudarem (D22) | Não |
| 3 treino | ADR-049 | CP10 | Recibo novo, se não couber nos golds | Não |
| 4 ações | ADR-050 | Não | D22 se N recibos | Não |
| 5 opções + descoberta | ADR-051 | Não | D22 (controle por opção) | Não |
| 6 receitas | ADR-052 | Não | D23 (2 golds + ação) | Não |
| 7 memória / O6 / A56 | ADR-053 (+ ADR O6, ADR A56) | Não | D24 | Não |

Bloqueadores do production gate continuam os listados por gpt-6-astra na seção 4 (PG2–PG6); nenhum plano acima os fecha nem os reabre.

### 5.6 Numeração final e arquivos abertos (08/10/2026)

Depois do benchmark (`RESULTADOS_08_10_2026.md`, nesta pasta) o owner decidiu `reasoning.effort=low` sempre e o reforço de proteína híbrido. Os ids provisórios de 5.4 mudaram porque S31 e S32 já existiam. ADRs e planos abertos, em ordem de execução:

| Plano de 5.4 | ADR | Arquivos |
|---|---|---|
| 1 | ADR-054 (effort low) | `docs/server/plans/s33-chat-context-effort-low.md` |
| novo | ADR-055 (reforço híbrido) | `docs/server/plans/s34-protein-boost-hybrid.md` |
| 2 | — | `docs/android/plans/a64-chat-context-fields-day-balance.md` |
| 3 | ADR-049 | `docs/content-policy/plans/cp10-workout-in-scope-and-skip-boundary.md`, `docs/server/plans/s35-workout-via-chat.md`, `docs/android/plans/a65-workout-via-chat.md` |
| 4 | ADR-050 | `docs/server/plans/s36-typed-actions.md`, `docs/android/plans/a66-typed-actions-batch.md` |
| 5 | ADR-051 | `docs/server/plans/s37-plan-options-and-discovery.md`, `docs/android/plans/a67-plan-options-and-discovery.md` (gold `chatO` por D25, a abrir) |
| 6 | ADR-052 | `docs/design/plans/d23-release-2-recipes.md`, `docs/server/plans/s38-saved-recipes.md`, `docs/android/plans/a68-saved-recipes.md` |
| 7 | ADR-053 | `docs/design/plans/d24-release-2-visible-memory.md`, `docs/android/plans/a69-visible-memory.md`; O6 e A56 continuam condicionais |

Os ADRs ficam em `docs/produto/adrs/` (049–053, 055) e `docs/server/adrs/` (054). Cada plano espera a aprovação explícita do seu arquivo antes de código.
