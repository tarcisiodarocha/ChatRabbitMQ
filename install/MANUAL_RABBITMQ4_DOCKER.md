# Instalação Completa do RabbitMQ 4 via Docker e Docker Compose no Ubuntu Server 24.04 (AWS EC2)

## 1. Pré-requisitos

- Instância AWS EC2 com Ubuntu Server 24.04 (t3.micro).
- Configurar o Grupo de Segurança da instância EC2 para liberar as portas: 5672, 15672, 4369:4369"     # EPMD (Erlang Port Mapper Daemon) — usado para descoberta entre nós
      - "25672:25672"   # Comunicação interna entre nós do cluster
      - "35197:35197"   # Porta usada dinamicamente para distribuição de dados (inter-node)
  
## 2. Atualizar o sistema

```bash
sudo apt update && sudo apt upgrade -y
```

## 3. Instalar Docker e Docker Compose

```bash
sudo apt install -y ca-certificates curl gnupg lsb-release
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
echo   "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg]   https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" |   sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
```

Verifique a instalação:
```bash
docker --version
docker compose version
```

Permitir uso do Docker sem sudo:
```bash
sudo usermod -aG docker $USER
newgrp docker
```

## 4. Criar diretórios e arquivos

```bash
mkdir -p ~/rabbitmq/data
mkdir -p ~/rabbitmq/log
cd ~/rabbitmq
```



## 5. Conteúdo do docker-compose.yml

Com o nano, crie o arquivo docker-compose.yml

```bash
nano docker-compose.yml
```

Cole e salve o conteúdo a seguir.

```yaml
# ==============================
# RabbitMQ 4 Cluster - Docker Compose
# ==============================
# Este arquivo define um container RabbitMQ pronto para clusterização.
# Ele inclui todas as portas necessárias para comunicação entre nós,
# gerenciamento e acesso de clientes AMQP.

services:
  rabbitmq:
    image: rabbitmq:4-management          # Imagem oficial com plugin de gerenciamento habilitado
    container_name: rabbitmq4             # Nome fixo do container (opcional alterar por nó)
    restart: unless-stopped               # Reinicia automaticamente, exceto se parado manualmente
    hostname: rabbitmq4                   # Nome do host usado pelo RabbitMQ internamente no cluster
    ports:
      - "5672:5672"     # Porta padrão AMQP — usada pelos clientes (aplicações)
      - "15672:15672"   # Painel de gerenciamento (interface web)
      - "4369:4369"     # EPMD (Erlang Port Mapper Daemon) — usado para descoberta entre nós
      - "25672:25672"   # Comunicação interna entre nós do cluster
      - "35197:35197"   # Porta usada dinamicamente para distribuição de dados (inter-node)

    environment:
      # Usuário e senha padrão do painel de administração e acesso inicial
      RABBITMQ_DEFAULT_USER: admin
      RABBITMQ_DEFAULT_PASS: password

      # Cookie Erlang: deve ser idêntico em todos os nós do cluster.
      # É o token de autenticação entre as instâncias RabbitMQ.
      RABBITMQ_ERLANG_COOKIE: "CLUSTER_SECRET_TOKEN"

    volumes:
      # Diretório local persistente para armazenar dados (filas, mensagens, etc.)
      - ./data:/var/lib/rabbitmq
      # Diretório local para logs
      - ./log:/var/log/rabbitmq

    networks:
      - rabbitmq_net

# ==============================
# Rede
# ==============================
# A rede bridge conecta o container ao ambiente local.
networks:
  rabbitmq_net:
    driver: bridge
```

## 6. Subir o container

```bash
docker compose up -d
```

Verifique o status:
```bash
docker compose ps
```

## 7. Acessar o RabbitMQ

- Painel de gerenciamento: [http://<IP-da-instância>:15672](http://<IP-da-instância>:15672)  
  Usuário: `admin`  
  Senha: `password`

- Porta AMQP: `5672`

## 8. Logs e persistência

Os dados são salvos em `~/rabbitmq/data` e os logs em `~/rabbitmq/log`.  
Mesmo que o container seja removido, os dados permanecerão.

## 9. Comandos úteis

```bash
# Parar
docker compose down

# Reiniciar
docker compose restart

# Atualizar imagem
docker compose pull && docker compose up -d

# Ver logs em tempo real
docker logs -f rabbitmq4
```

## 10. Remoção completa

```bash
docker compose down -v
rm -rf ~/rabbitmq
```

---
**Compatível com:** Ubuntu Server 24.04, Docker Compose v2+, RabbitMQ 4.x
