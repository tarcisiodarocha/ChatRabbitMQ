## Instalar RabbitMQ Server na AWS

Criar uma nova instância EC2 na AWS com o Ubuntu Server 20.04. 

Acessar a instância via ssh.

Instalar o RabbitMQ Server na instância conforme os passos a seguir:

### Passo 0 – Acessar shell como super usuário

```
sudo su
```

### Passo 1 – Atualizar Ubuntu e Instalar o Erlang

```
apt update
```

```
apt upgrade
```

```
wget https://packages.erlang-solutions.com/erlang/debian/pool/esl-erlang_23.3.1-1~ubuntu~focal_amd64.deb
```

```
dpkg -i esl-erlang_23.3.1-1~ubuntu~focal_amd64.deb
```

Pode acontecer um erro de dependência do comando anterior. Instale as dependências com o comando:

```
apt install -f
```

E reexecute o comando para instalar o Erlang:

```
dpkg -i esl-erlang_23.3.1-1~ubuntu~focal_amd64.deb
```

### Passo 2 – Instalar e Iniciar o RabbitMQ Server

```
wget -O- https://dl.bintray.com/rabbitmq/Keys/rabbitmq-release-signing-key.asc | sudo apt-key add - 
```
```
wget -O- https://www.rabbitmq.com/rabbitmq-release-signing-key.asc | sudo apt-key add -             
```
```
apt update
```
```
apt install rabbitmq-server
```

```
systemctl start rabbitmq-server.service
systemctl enable rabbitmq-server.service
```


### Passo 3 – Criar Admin User no RabbitMQ

Obs: Trocar "admin" por seu nome de usuário e "password" por uma senha.

```
rabbitmqctl add_user admin password 
rabbitmqctl set_user_tags admin administrator
rabbitmqctl set_permissions -p / admin ".*" ".*" ".*"
```


### Passo 4 – Ativar RabbitMQ Web Management Console

```
rabbitmq-plugins enable rabbitmq_management
```
