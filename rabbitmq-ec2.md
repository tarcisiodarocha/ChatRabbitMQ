## Instalar RabbitMQ Server na AWS

Criar uma nova instância EC2 na AWS com o Ubuntu Server 22.04. 

Instalar o RabbitMQ Server na instância conforme os passos a seguir:


### Passo 0 - Acessar a instância via ssh.

```
chmod 400 chave.pem
```

```
ssh -i chave.pem ubuntu@<ip_publico_da_instancia>
```

### Passo 1 – Atualizar Ubuntu 
```
sudo apt update
```

```
sudo apt upgrade
```


### Passo 2 – Instalar as dependências do RabbitMQ Server

```
sudo apt install socat logrotate init-system-helpers adduser -y
```

### Passo 3 – Baixar e instalar o RabbitMQ Server

```
wget https://github.com/rabbitmq/rabbitmq-server/releases/download/v3.10.7/rabbitmq-server_3.10.7-1_all.deb
```
```
sudo dpkg -i rabbitmq-server_3.10.7-1_all.deb
```
```
sudo apt --fix-broken install -y
```

### Passo 4 – Iniciar o RabbitMQ Server

```
sudo systemctl start rabbitmq-server.service
sudo systemctl enable rabbitmq-server.service
```


### Passo 5 – Criar Admin User no RabbitMQ

Obs: Trocar "password" por uma senha.

```
sudo rabbitmqctl add_user admin password 
sudo rabbitmqctl set_user_tags admin administrator
sudo rabbitmqctl set_permissions -p / admin ".*" ".*" ".*"
```


### Passo 6 – Ativar RabbitMQ Web Management Console

```
sudo rabbitmq-plugins enable rabbitmq_management
```
