## Instalar RabbitMQ Server na AWS

Criar uma nova instância EC2 na AWS com o Ubuntu Server 20.04. 

Acessar a instância via ssh.

Instalar o RabbitMQ Server na instância conforme os passos a seguir:

### Passo 1 – Atualizar Ubuntu 
```
sudo apt update
```

```
sudo apt upgrade
```


### Passo 2 – Instalar e Iniciar o RabbitMQ Server

```
wget -O- https://www.rabbitmq.com/rabbitmq-release-signing-key.asc | sudo apt-key add -
```
```
echo "deb https://dl.bintray.com/rabbitmq-erlang/debian focal erlang-22.x" | sudo tee /etc/apt/sources.list.d/rabbitmq.list
```
```
sudo apt install rabbitmq-server --fix-missing
```

```
sudo systemctl start rabbitmq-server.service
sudo systemctl enable rabbitmq-server.service
```


### Passo 3 – Criar Admin User no RabbitMQ

Obs: Trocar "password" por uma senha.

```
sudo rabbitmqctl add_user admin password 
sudo rabbitmqctl set_user_tags admin administrator
sudo rabbitmqctl set_permissions -p / admin ".*" ".*" ".*"
```


### Passo 4 – Ativar RabbitMQ Web Management Console

```
sudo rabbitmq-plugins enable rabbitmq_management
```
