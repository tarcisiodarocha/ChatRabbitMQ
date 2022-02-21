## Instalar RabbitMQ Server na AWS

Criar uma nova instância EC2 na AWS com o Ubuntu Server 20.04. 

Acessar a instância via ssh.

Instalar o RabbitMQ Server na instância conforme os passos a seguir:

### Passo 1 – Atualizar Ubuntu e Instalar o Erlang

```
sudo apt update
```

```
sudo apt upgrade
```

```
wget https://packages.erlang-solutions.com/erlang/debian/pool/esl-erlang_24.2.1-1~ubuntu~focal_amd64.deb
```

```
sudo dpkg -i esl-erlang_24.2.1-1~ubuntu~focal_amd64.deb
```

Pode acontecer um erro de dependência do comando anterior. Instale as dependências com o comando:

```
sudo apt install -f
```

E reexecute o comando para instalar o Erlang:

```
sudo dpkg -i esl-erlang_24.2.1-1~ubuntu~focal_amd64.deb 
```

### Passo 2 – Instalar e Iniciar o RabbitMQ Server

```
wget -O- https://github.com/rabbitmq/signing-keys/releases/download/2.0/rabbitmq-release-signing-key.asc | sudo apt-key add -
```

```
sudo apt update
```
```
sudo apt install rabbitmq-server --fix-missing
```

```
sudo systemctl start rabbitmq-server.service
sudo systemctl enable rabbitmq-server.service
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
