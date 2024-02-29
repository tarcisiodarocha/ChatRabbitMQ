## Replicação do servidor RabbitMQ visando alta disponibilidade

O servidor RabbitMQ possui um recurso de clusterização onde pode-se configurar diversas instâncias diferentes do RabbitMQ Server para trabalharem em conjunto oferecendo um serviço único. Para esse trabalho, devem ser criadas cinco instâncias do RabbitMQ configuradas como cluster. Deve-se configurar as instâncias de modo que elas repliquem as filas entre elas com uma taxa de replicação de três (cada fila terá mais duas réplicas em dois outros nós). Com isso, se uma instância cair, o serviço do RabbitMQ e as filas permanecerão disponíveis.

Para isso, deve-se pesquisar a documentação do RabbitMQ em:

* https://www.rabbitmq.com/clustering.html
* https://www.rabbitmq.com/ha.html

## Utilizar um balanceador de carga para os nós do cluster RabbitMQ

Utilizar o serviço de balanceamento de carga da AWS: https://aws.amazon.com/pt/elasticloadbalancing/

Criar um balanceador de carga com dois listeners diferentes, um para a interface web de gerenciamento (http) e outro para o protocolo AMQP.
