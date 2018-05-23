## Instalar RabbitMQ Server na AWS

Criar uma nova instância EC2 na AWS com o Ubuntu Server 16.04. 

Acessar a instância via ssh.

Instalar o RabbitMQ Server na instância conforme os passos a seguir:

### Passo 1 – Instalar o Erlang

```
wget https://packages.erlang-solutions.com/erlang-solutions_1.0_all.deb
sudo dpkg -i erlang-solutions_1.0_all.deb
```
```
sudo apt-get update
sudo apt-get install erlang erlang-nox
```

### Passo 2 – Instalar RabbitMQ Server

```
echo 'deb http://www.rabbitmq.com/debian/ testing main' | sudo tee /etc/apt/sources.list.d/rabbitmq.list
wget -O- https://www.rabbitmq.com/rabbitmq-release-signing-key.asc | sudo apt-key add -
```
```
sudo apt-get update
sudo apt-get install rabbitmq-server
```

### Passo 3 – Criar Admin User no RabbitMQ

Obs: Trocar "admin" por seu nome de usuário e "password" por uma senha.

```
sudo rabbitmqctl add_user admin password 
sudo rabbitmqctl set_user_tags admin administrator
sudo rabbitmqctl set_permissions -p / admin ".*" ".*" ".*"
```


### Passo 4 – Ativar RabbitMQ Web Management Console

```
sudo rabbitmq-plugins enable rabbitmq_management
```
