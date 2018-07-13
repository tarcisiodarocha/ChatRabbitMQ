## Instalar RabbitMQ Server na AWS

(Obs: Instalação opcional na AWS com o Amazon Linux ao invés do Ubuntu Server https://dzone.com/articles/installing-rabbitmq-37-along-with-erlang-version-2)

Criar uma nova instância EC2 na AWS com o Ubuntu Server 16.04. 

Acessar a instância via ssh.

Instalar o RabbitMQ Server na instância conforme os passos a seguir:

### Passo 1 – Ataulizar Ubuntu e Instalar o Erlang

```
sudo apt-get update
sudo apt-get upgrade
```

```
wget http://packages.erlang-solutions.com/site/esl/esl-erlang/FLAVOUR_1_general/esl-erlang_20.1-1~ubuntu~xenial_amd64.deb
sudo dpkg -i esl-erlang_20.1-1\~ubuntu\~xenial_amd64.deb
```
Pode acontecer um erro de dependência do comando anterior. Instale as dependências com o comando:

```
sudo apt-get install -f
```

E reexecute o comando para instalar o Erlang:

```
sudo dpkg -i esl-erlang_20.1-1\~ubuntu\~xenial_amd64.deb
```

### Passo 2 – Instalar e Iniciar o RabbitMQ Server

```
echo "deb https://dl.bintray.com/rabbitmq/debian xenial main" | sudo tee /etc/apt/sources.list.d/bintray.rabbitmq.list
wget -O- https://www.rabbitmq.com/rabbitmq-release-signing-key.asc | sudo apt-key add -
```
```
sudo apt-get update
sudo apt-get install rabbitmq-server
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
