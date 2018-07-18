## Replicação do servidor RabbitMQ visando alta disponibilidade

O servidor RabbitMQ possui um recurso de clusterização onde pode-se configurar diversas instâncias diferentes do RabbitMQ Server para trabalharem em conjunto oferecendo um serviço único. Para esse trabalho, devem ser criadas três instâncias do RabbitMQ configuradas como cluster. Deve-se configurar as instâncias de modo que elas repliquem todas as filas entre elas. Com isso, se uma instância cair, o serviço do RabbitMQ e as filas permanecerão disponíveis.

Para isso, deve-se pesquisar a documentação do RabbitMQ em:

* https://www.rabbitmq.com/clustering.html
* https://www.rabbitmq.com/ha.html
