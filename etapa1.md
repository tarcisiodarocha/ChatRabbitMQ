## Implementação da Interface do Chat em Linha de Comando

Ao ser executado, o chat perguntaria o nome do usuário do mesmo. Exemplo:
```
User:
```

Com isso, o usuário digita o seu nome de usuário. Exemplo:
```
User: tarcisio
```

Com o nome do usuário, o chat cria a fila do usuário no RabbitMQ e exibe um prompt para que o usuário inicie a comunicação. Exemplo de prompt:
```
<<  
```
### Envio de mensagens 

No prompt, se o usuário (tarcisio) quer enviar mensagem para um outro usuário do chat, ele deve digitar "@" seguido do nome do usuário com o qual ele quer conversar. Exemplo:
```
<< @joao
```
Com isso, o prompt é alterado automaticamente para exibir o nome do outro usuário para o qual se quer enviar mensagem. Exemplo:
```
@joao<< 
```

Nesse exemplo, toda nova mensagem digitada no prompt é enviada para "joao" até que o usuário mude para para um novo destinatário. Exemplo:
```
@joao<< Olá, João!!!
@joao<< Vamos adiantar o trabalho de SD?
```
Por exemplo, se o usuário quiser enviar mensagens para outro usuário diferente de "joao", ele deve informar o nome do outro usuário para o qual ele quer enviar mensagem:
```
@joao<< @marcio
```
O comando acima faria o prompt ser "chaveado" para "marcio". Com isso, as próximas mensagens seriam enviadas para o usuário "marcio":
```
@marcio<< Oi, Marcio!!
@marcio<< Vamos sair hoje?
@marcio<< Já estou em casa!
@marcio<<
```

### Recebimento de Mensagens

A qualquer momento, o usuário (exemplo: tarcisio) pode receber mensagem de qualquer outro usuário (marcio, joao...). Nesse caso, a mensagem seria impressa na tela da seguinte forma:
```
(21/09/2016 às 20:53) @marcio diz: E aí, Tarcisio! Vamos sim!
```
Depois de impressa a mensagem, o prompt volta para o estado anterior:
```
@marcio<< 
```
Agora segue um exemplo de três mensagens recebidas de joaosantos:
```
(21/09/2016 às 20:55) @joao diz: Opa!
@marcio<< 
(21/09/2016 às 20:55) @joao diz: vamos!!!
@marcio<< 
(21/09/2016 às 20:56) @joao diz: estou indo para a sua casa
@marcio<< 
```

