## Replicação do servidor RabbitMQ visando alta disponibilidade

O servidor RabbitMQ possui um recurso de clusterização onde pode-se configurar diversas instâncias diferentes do RabbitMQ Server para trabalharem em conjunto oferecendo um serviço único. Para esse trabalho, devem ser criadas três instâncias do RabbitMQ configuradas como cluster. Deve-se configurar as instâncias de modo que elas repliquem todas as filas entre elas. Com isso, se uma instância cair, o serviço do RabbitMQ e as filas permanecerão disponíveis.

Para isso, deve-se pesquisar a documentação do RabbitMQ em:

* https://www.rabbitmq.com/clustering.html
* https://www.rabbitmq.com/ha.html

## Utilizar um balanceador de carga para os nós do cluster RabbitMQ

Utilizar o serviço de balanceamento de carga da AWS: https://aws.amazon.com/pt/elasticloadbalancing/

Criar dois balanceadores de carga diferentes para o chat, um para a interface web de gerenciamento (http) e outro para o protocolo AMQP.

Passos gerais para criar um balanceador de carga na AWS:

* Criar um Elastic IP para o balanceador
* Criar um novo balanceador tipo "network"
  - Dar um nome ao balanceador  
  - Escolher o tipo internet-fancing para que o balanceamento seja feito a partir de requisições vindas da Internet
  - Escolher a porta (listener) que o balanceador vai escutar (ex: 80)
  - Escolher a zona de disponibilidade onde as instâncias que serão balanceadas estão
  - Escolher o Elastic IP criado anteriormente
  - Escolher a porta alvo das instâncias (target) para onde o balanceador redirecionar as requisições (ex: 15672)
  - Selecionar e adicionar as instâncias target que serão balanceadas
  
Depois de criado o balanceador, será disponibilizado um domínio para ele. 