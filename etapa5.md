## Listar todos os usuários em um dado grupo 

O chat deve disponibilizar operação para listar todos os usuários de um dado grupo do chat. Ex:

```
@marcio<< !listUsers ufs
tarcisio, marcio, favio, monica
@marcio<< 
```

## Listar todos os grupos

O chat deve disponibilizar operação para listar todos os grupos dos quais o usuário corrente faz parte. Ex:

```
@marcio<< !listGroups
ufs, amigos, familia
@marcio<< 
```

Para implementar essas operações, deve-se usar a API HTTP de Gerenciamento do RabbitMQ: https://rawcdn.githack.com/rabbitmq/rabbitmq-management/v3.7.7/priv/www/api/index.html
