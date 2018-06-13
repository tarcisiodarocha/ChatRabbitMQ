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
Logo depois de chamado o comando "upload", deve ser exibida a mensagem (não bloqueante) 'Uploading file "<nome-do-arquivo>"...'. Exemplo:

```
@marciocosta>> !upload /home/tarcisio/aula1.pdf
Uploading file "/home/tarcisio/aula1.pdf" to @marciocosta...
@marciocosta>>
```
Observe também que no exemplo acima, logo depois de exibida a mensagem 'Uploading file "/home/tarcisio/aula1.pdf"...' o chat volta instantaneamente para o prompt (ex: "@marciocosta>> "), ou seja, o processo de envio de arquivos com o comando "upload" deve ser feita em background (sem bloquear o chat). Para que isso seja possível, é necessário criar uma thread no chat emissor para cada novo upload. Também pode ser necessário criar uma fila específica para o recebimento de arquivos para cada usuário. Com isso, cada usuário teria uma fila para o recebimento de mensagens de texto e outra para o recebimento de arquivos.

Depois que o arquivo for transferido do chat emissor para o servidor do RabbitMQ, deve ser exibida a mensagem 'File "<nome-do-arquivo>" is now available to @<id-do-receptor>" Exemplo:

```
File "/home/tarcisio/aula1.pdf" is available to @marciocosta !
```
O lado receptor do chat, deve receber o arquivo também em background sem bloqueios. É realizado automaticamente o download de arquivos a serem recebidos em uma pasta default (ex: /home/tarcisio/chat/downloads). Quando um download for completado, deve ser exibida a mensagem 'File <nome-do-arquivo> from @<id-do-emissor> downloaded!' no lado receptor. Exemplo: 

```
File "aula1.pdf" from @tarcisio downloaded!
```