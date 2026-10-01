# Aviso aos testers — Dieta Bot (teste fechado)

Versão 1 · 2026-10-01 · vale para o app de teste (`com.nutri.android.dev`) instalado pelo Firebase App Tester.

O Dieta Bot está em teste fechado com dois testers. Este aviso diz o que sai do seu celular, para onde vai e por quanto tempo fica. Não é Termos de Uso nem Política de Privacidade.

## O que o app envia

Cada mensagem do Chat vai para o servidor de teste do Dieta Bot (Google Cloud, EUA) e de lá para a OpenAI. Vão juntos:

- o texto que você digitou;
- a foto, quando você anexa uma;
- o histórico recente da conversa do dia;
- a memória do app (hábitos curtos que a IA guarda, como "café com leite de manhã");
- seu perfil de metas (teto de kcal, macros, horários das refeições) e o que já foi registrado no dia.

O servidor confere o convite do app antes de qualquer coisa. Texto e foto passam pela moderação da OpenAI antes e depois da resposta.

## Fotos

O servidor não grava a foto. Ela vai para a OpenAI na mesma requisição e é descartada quando a resposta volta. O registro de conversa guarda só se havia foto e o tamanho dela.

No celular, a foto enviada fica na área privada do app para aparecer no Chat. Sai quando você desinstala o app ou limpa os dados dele.

## Registro de conversa (debug)

O servidor de teste grava um registro de cada mensagem para depuração: texto enviado, histórico e memória que foram junto, resposta da IA, horário, versão do app e um ID da requisição. Serve para achar erros como uma resposta fora do assunto. Só o dono do app lê, pelo acesso administrativo ao servidor, e às vezes baixa uma cópia para o próprio computador para investigar.

Rotação de 30 dias: está planejada e ainda não está ativa. Hoje o arquivo gira por tamanho (cerca de 120 MB no total), então registros antigos podem ficar mais de 30 dias. Quando a rotação de 30 dias entrar, este aviso será atualizado.

Mensagens bloqueadas pela moderação (conteúdo sexual, ameaça e categorias graves parecidas) não são gravadas por inteiro. Fica só o ID da requisição, a rota e quais categorias marcaram, sem texto e sem foto. Mensagens fora do assunto e mensagens que recebem a resposta de apoio (por exemplo, sinais de transtorno alimentar) são gravadas como as demais.

## Identificação para abuso

- Endereço IP: o servidor usa o IP para limitar requisições por minuto. O IP aparece nos logs técnicos do servidor (acesso), não no registro de conversa.
- Pseudônimo da instalação: numa próxima versão, o app vai criar um código aleatório por instalação. O servidor transforma esse código num pseudônimo e manda para a OpenAI para separar uma instalação de outra em caso de abuso. Não é seu nome, e-mail nem ID do aparelho.

IP e pseudônimo não provam quem usou o app. Um IP pode ser compartilhado (Wi-Fi, operadora, VPN).

## OpenAI

As chamadas vão com `store=false`: a OpenAI não guarda a conversa para o app reutilizar. A OpenAI ainda pode reter dados por um tempo para monitorar abuso, conforme a política dela para a API. O Dieta Bot não controla essa retenção.

## Diagnóstico do app

O app de teste envia relatórios de falha e eventos de uso ao Firebase (Crashlytics e Analytics, projeto de teste). Esses eventos levam números e códigos (tela, rota, status, tempo de resposta, faixa de tamanho da mensagem, se havia foto, valores de kcal), nunca o texto que você digitou nem a foto.

## O que isto não garante

- A moderação erra para os dois lados: pode bloquear algo inofensivo e pode deixar passar algo que deveria bloquear.
- Não há retenção zero: os registros acima existem enquanto durarem as regras descritas.
- O app de teste não promete detecção completa de nenhum tipo de conteúdo.

## Estimativa, não orientação

Os números do Dieta Bot são estimativas. Não são orientação médica nem nutricional. Se algo da sua saúde depende disso, fale com um profissional.

## Uso fora do assunto

O app só trata de refeições e do orçamento do dia. Perguntas fora disso recebem uma recusa fixa. Isso não gera punição.

## Contato

Dúvida, pedido para apagar seus registros ou algo estranho no app: fale direto com o dono do app, pelo mesmo canal em que você recebeu o convite do teste.
