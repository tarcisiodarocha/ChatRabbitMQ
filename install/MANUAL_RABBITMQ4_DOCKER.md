# Instalação Completa do RabbitMQ 4 via Docker e Docker Compose no Ubuntu Server 24.04 (AWS EC2)

## 1. Pré-requisitos

- Criar **Security Group** na AWS e liberar as portas de entrada necessárias:
  - `22` (SSH)
  - `5672` (AMQP — acesso de aplicações)
  - `8080` (Painel de Gerenciamento Web)
  - *(Opcional - apenas se for formar cluster entre instâncias na mesma VPC)*: `4369`, `25672`
- Criar instância AWS EC2 com **Ubuntu Server 24.04 LTS** (ex: `t3.micro`).
- Acessar a instância via SSH:

```bash
chmod 400 <nome-da-chave-ssh>
ssh -i <nome-da-chave-ssh> ubuntu@<ipv4-publico-da-instancia>
```

---

## 2. Atualizar o Sistema

```bash
sudo apt update && sudo apt upgrade -y
```

---

## 3. Instalar Docker e Docker Compose

Adicionar o repositório oficial do Docker e instalar os pacotes necessários:

```bash
sudo apt update
sudo apt install -y ca-certificates curl

# Criar repositório e importar chave GPG oficial do Docker
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

# Configurar o repositório APT do Docker
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Instalar Docker Engine, CLI, Containerd e o plugin do Docker Compose
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

Adicionar o usuário atual ao grupo `docker` (para executar comandos do Docker sem `sudo`):

```bash
sudo usermod -aG docker $USER
newgrp docker
```

Verificar se a instalação foi bem-sucedida:

```bash
docker --version
docker compose version
```

---

## 4. Criar Diretórios e Ajustar Permissões

*(Nota: O RabbitMQ executa internamente no container com o UID `999`. Ajustamos o dono das pastas locais para evitar erros de leitura e gravação no volume).*

```bash
mkdir -p ~/rabbitmq/data ~/rabbitmq/log
sudo chown -R 999:999 ~/rabbitmq/data ~/rabbitmq/log
cd ~/rabbitmq
```

---

## 5. Conteúdo do `docker-compose.yml`

Crie e abra o arquivo `docker-compose.yml`:

```bash
nano docker-compose.yml
```

Cole o conteúdo a seguir e salve o arquivo (`Ctrl+O`, `Enter`, `Ctrl+X`):

```yaml
# ==============================
# RabbitMQ 4 - Docker Compose
# ==============================

services:
  rabbitmq:
    image: rabbitmq:4-management         # Imagem oficial do RabbitMQ 4 com o plugin de gerenciamento
    container_name: rabbitmq             # Nome do container
    restart: unless-stopped              # Reinicia automaticamente caso falhe ou a instância reinicie
    hostname: rabbitmq1                  # Nome do host interno usado pelo nó

    ports:
      - "5672:5672"     # Porta padrão AMQP 0-9-1 (utilizada pelas aplicações)
      - "8080:15672"   # Interface web de gerenciamento (mapeada para a porta 8080 externa)
      - "4369:4369"     # EPMD (Erlang Port Mapper Daemon - comunicação de cluster)
      - "25672:25672"   # Comunicação interna entre nós e CLI

    environment:
      # Usuário e senha padrão para acesso inicial
      RABBITMQ_DEFAULT_USER: admin
      RABBITMQ_DEFAULT_PASS: password

      RABBITMQ_NODENAME: rabbit@rabbitmq1

      # Cookie Erlang de autenticação entre nós (Altere para uma chave secreta e forte)
      RABBITMQ_ERLANG_COOKIE: "MUDE_ESTE_TOKEN_SECRETO"

    volumes:
      # Persistência de dados (filas, trocas, mensagens)
      - ./data:/var/lib/rabbitmq
      # Logs do sistema
      - ./log:/var/log/rabbitmq

    networks:
      - rabbitmq_net

# ==============================
# Rede
# ==============================
networks:
  rabbitmq_net:
    driver: bridge
```

---

## 6. Subir o Container

Inicie o serviço em segundo plano:

```bash
docker compose up -d
```

Verifique o status do container:

```bash
docker compose ps
```

---

## 7. Acessar o RabbitMQ

- **Painel de Gerenciamento Web:** `http://<IP-PUBLICO-DA-INSTANCIA>:8080`
  - **Usuário:** `admin`
  - **Senha:** `password`
- **Porta AMQP (Conexão de Aplicações):** `5672`

---

## 8. Comandos Úteis

```bash
# Ver logs em tempo real
docker compose logs -f

# Parar o serviço
docker compose down

# Reiniciar o serviço
docker compose restart

# Atualizar a imagem e reiniciar o container
docker compose pull && docker compose up -d
```

---

## 9. Remoção Completa

Caso deseje remover o serviço e apagar todos os dados persistidos:

```bash
docker compose down -v
cd ~ && rm -rf ~/rabbitmq
```

---
**Compatível com:** Ubuntu Server 24.04 LTS, Docker Engine 24+, Docker Compose v2+, RabbitMQ 4.x
