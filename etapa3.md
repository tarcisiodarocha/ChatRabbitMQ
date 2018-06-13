## Envio de arquivos

O chat deve disponibilizar o comando "upload" para permitir que um usuário envie arquivos (de qualquer tipo) para um usuário ou grupo corrente. 

Exemplo do envio do arquivo "aula1.pdf" para o usuário "marciocosta":

```
@marciocosta>> !upload /home/tarcisio/aula1.pdf
```
O envio de arquivos para um grupo deve ser semelhante:

```
#ufs>> !upload /home/tarcisio/aula1.pdf
```
Logo depois de chamado o comando "upload", deve ser exibida a mensagem (não bloqueante) ```Enviando "<nome-do-arquivo>" para <destinatário>```. Exemplo:

```
@marciocosta>> !upload /home/tarcisio/aula1.pdf
Enviando "/home/tarcisio/aula1.pdf" para @marciocosta.
@marciocosta>>
```
Observe também que no exemplo acima, logo depois de exibida a mensagem ```Enviando "/home/tarcisio/aula1.pdf" para @marciocosta``` o chat volta instantaneamente para o prompt (ex: "@marciocosta>> "), ou seja, o processo de envio de arquivos com o comando "upload" deve ser feita em background (sem bloquear o chat). Para que isso seja possível, é necessário criar uma thread no chat emissor para cada novo upload. Também pode ser necessário criar uma fila específica para o recebimento de arquivos para cada usuário. Com isso, cada usuário teria uma fila para o recebimento de mensagens de texto e outra para o recebimento de arquivos.

Depois que o arquivo for transferido do chat emissor para o servidor do RabbitMQ, deve ser exibida a mensagem ```Arquivo "<nome-do-arquivo>" foi enviado para @<id-do-receptor>``` Exemplo:

```
Arquivo "/home/tarcisio/aula1.pdf" foi enviado para @marciocosta !
```
O lado receptor do chat, deve receber o arquivo também em background sem bloqueios. É realizado automaticamente o download de arquivos a serem recebidos em uma pasta default (ex: /home/tarcisio/chat/downloads). Quando um download for completado, deve ser exibida a mensagem ```(<data> às <hora>) Arquivo <nome-do-arquivo> recebido de @<id-do-emissor>!``` no lado receptor. Exemplo: 

```
(21/09/2016 às 20:55) Arquivo "aula1.pdf" recebido de @tarcisio !
```

Obs: De acordo com o formato da mensagem (Protocol Buffers) descrita na etapa2, juntamente com o arquivo é enviado também o seu tipo MIME. Em java (versão 7 ou posterior) podem-se usar os seguintes comandos para descobrir em tempo de execução o tipo MIME de um arquivo:

```
String caminhoAoArquivo = "/home/tarcisio/aula1.pdf"; 
Path source = Paths.get(caminhoAoArquivo);
String tipoMime = Files.probeContentType(source);
System.out.println(tipoMime);
```