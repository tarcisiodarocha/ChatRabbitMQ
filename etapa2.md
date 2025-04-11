## Formato de Mensagem

Juntamente com o conteúdo da mensagem, são enviados a data e hora de envio e o nome do emissor para que sejam exibidos no console do receptor. Em suma, cada mensagem enviada deve incluir: nome do emissor, data de envio, hora de envio e o conteúdo da mensagem (texto que o emissor esta enviando ao receptor e/ou outros conteúdos como arquivos e imagens). O marshalling dos dados que compõem uma mensagem deve ser feito através do uso de Protocol Buffers (https://developers.google.com/protocol-buffers/). Segue o modelo de mensagem sugerido:

```
syntax = "proto3";
message Mensagem{
     string emissor = 1; // Nome do emissor
     string data = 2; // Data de envio
     string hora = 3; // Hora de envio
     string grupo = 4; // Informa o nome do grupo, se a mensagem for para um grupo
     Conteudo conteudo = 5;
}

message Conteudo{
     string tipo = 1; // Tipo do conteúdo no padrão de tipos MIME. Exemplos: "text/plain", "image/png" 
     bytes corpo = 2; // Sequência de bytes que compõe o corpo da mensagem
     string nome = 3; // Nome do conteúdo, se existente. Exemplos: "logo_ufs.png", "index.html"
}
```

## Gerenciamento de Grupos

O chat tambem deve disponibilizar comandos de criação de grupos e de adição de membros a um grupo.


### Criação de Grupos

Para criar um novo grupo, o usuario pode digitar na linha de comando do chat o simbolo `!` seguido do nome do comando `addGroup` seguido do nome do grupo que se deseja criar. Exemplo de criacao de um grupo chamado "amigos":

```
@marcio<< !addGroup amigos
@marcio<<
```

Apesar, de nesse exemplo anterior, o usuário estar em uma seção de envio de mensagens para "marcio", o chat será capaz de identificar que a entrada `!addGroup amigos` não se trata de uma mensagem a ser enviada ao usuário "marcio" e sim um comando ao chat, pelo fato de se iniciar com o simbolo `!`. Toda entrada iniciada com `!` deve ser tratada pelo chat como um comando. 

### Inclusão de usuários em um grupo

Para incluir um usuário em um grupo deve-se usar o comando `addUser` seguido dos parametros nome do usuario e nome do grupo. Exemplo onde se adiciona os usuários "marcio" e "joao" ao grupo "amigos":

```
@marcio<< !addUser joao amigos
@marcio<< !addUser marcio amigos
@marcio<<
```
Assuma também que o usuário que pede para ciar um grupo é adicionado automaticamente ao mesmo grupo. Por exemplo, se considerarmos que foi o usuário "tarcisio" que criou o grupo "amigos", "tarcisio" é adicionado  automaticamente ao grupo amigos (com isso, se tarcisio criou o grupo amigos e adicionou marcio e joao, esse grupo fica com tres membros: tarcisio, marcio e joao).

### Envio de mensagem para um grupo

No prompt, se o usuário (tarcisio) quer enviar mensagem para um grupo, ele deve digitar "#" seguido do nome do grupo para o qual ele quer enviar mensagens. Depois que o usuário pressionar a tecla <ENTER>, o prompt é alterado para exibir o nome do grupo correspondente e a indicação entre parêntesis de que se trata de um grupo. Exemplo:

```
@marcio<< #amigos
#amigos<<  
```
A partir disso, o usuário poderá digitar as mensagens para o respectivo grupo:

```
#amigos<< Olá, pessoal!
#amigos<< Alguém vai ao show de Djavan?
#amigos<<
```

No RabbitMQ o envio de uma mensagem para um grupo deve corresponder ao envio de uma única mensagem ao exchange correspondente que, por sua vez, será o responsável por replicar a mensagem nas diversas filas dos integrantes do respectivo grupo.

### Recebimento de mensagens de grupo

Mensagens recebidas dentro do contexto de um grupo são exibidas de forma semelhante àquelas recebidas de um usuário individualmente com exceção de que se acrescenta do nome do grupo logo após ao nome do usuário que a postou. Exemplo:

```
(21/09/2024 às 21:50) joao#amigos diz: Olá, amigos!!!
```

### Exclusão usuário de um grupo

Devem ser incluidos também comandos para excluir um grupo e remover um usuário do grupo.Para remover um usuário de um determinado grupo, deve-se diponibilizar o comando "removeUser" seguido do <nome do usuário> e do <nome do grupo>. Exemplo:

```
@marcio<< !removeUser joao amigos
@marcio<<
```
Neste último exemplo, joao é removido do grupo amigos.

Para excluir um grupo, deve-se adotar o comando "removeGroup" seguido do <nome do grupo> a ser removido. Exemplo:

```
@marcio<< !removeGroup amigos
@marcio<<
```
