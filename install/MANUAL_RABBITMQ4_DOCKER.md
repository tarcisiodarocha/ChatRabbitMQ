# Instalação Completa do RabbitMQ 4 via Docker e Docker Compose no Ubuntu Server 24.04 (AWS EC2)

## 1. Pré-requisitos

- Instância AWS EC2 com Ubuntu Server 24.04 (t3.micro).
- Porta 22 aberta (SSH) e portas 5672 e 15672 liberadas no Security Group.

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

## 4. Criar diretórios e arquivos

```bash
mkdir -p ~/rabbitmq/data
mkdir -p ~/rabbitmq/log
cd ~/rabbitmq
```

Crie o arquivo `docker-compose.yml` conforme abaixo.

## 5. Conteúdo do docker-compose.yml

```yaml
version: "3.9"

services:
  rabbitmq:
    image: rabbitmq:4-management
    container_name: rabbitmq4
    restart: unless-stopped
    ports:
      - "5672:5672"   # Porta para os clientes acessarem via AMQP
      - "15672:15672" # Porta para acessar a interface de gerenciamento via browser
    environment:
      RABBITMQ_DEFAULT_USER: admin
      RABBITMQ_DEFAULT_PASS: password
    volumes:
      - ./data:/var/lib/rabbitmq
      - ./log:/var/log/rabbitmq
    networks:
      - rabbitmq_net

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
