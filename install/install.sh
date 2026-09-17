#!/usr/bin/env bash
# ==============================================================================
# Script de Instalação Automática do RabbitMQ 4 via Docker Compose
# SO Alvo: Ubuntu Server 24.04 LTS (AWS EC2 / Virtual Machines)
# ==============================================================================

set -euo pipefail

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}======================================================================${NC}"
echo -e "${BLUE}   Instalação Automática: Docker + Docker Compose + RabbitMQ 4        ${NC}"
echo -e "${BLUE}======================================================================${NC}"

# 1. Atualizar o sistema
echo -e "\n${YELLOW}[1/6] Atualizando pacotes do sistema...${NC}"
sudo apt update && sudo apt upgrade -y

# 2. Instalar Docker e Docker Compose Plugin
echo -e "\n${YELLOW}[2/6] Instalando dependências e repositório do Docker...${NC}"
sudo apt install -y ca-certificates curl

sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Adicionar usuário atual ao grupo docker
if ! groups $USER | grep &>/dev/null '\bdocker\b'; then
    echo -e "${YELLOW}Adicionando usuário $USER ao grupo docker...${NC}"
    sudo usermod -aG docker $USER
fi

# 3. Criar estrutura de diretórios e ajustar permissões
echo -e "\n${YELLOW}[3/6] Criando diretórios e ajustando permissões de volume...${NC}"
RABBITMQ_DIR="$HOME/rabbitmq"
mkdir -p "$RABBITMQ_DIR/data" "$RABBITMQ_DIR/log"

# O RabbitMQ dentro do container roda sob o UID 999
sudo chown -R 999:999 "$RABBITMQ_DIR/data" "$RABBITMQ_DIR/log"

# 4. Criar docker-compose.yml
echo -e "\n${YELLOW}[4/6] Gerando o arquivo docker-compose.yml...${NC}"
cat << 'EOF' > "$RABBITMQ_DIR/docker-compose.yml"
services:
  rabbitmq:
    image: rabbitmq:4-management
    container_name: rabbitmq
    restart: unless-stopped
    hostname: rabbitmq1
    ports:
      - "5672:5672"     # AMQP
      - "8080:15672"   # Management Dashboard (Acesso via navegador)
      - "4369:4369"     # EPMD
      - "25672:25672"   # Inter-node CLI/Cluster
    environment:
      RABBITMQ_DEFAULT_USER: admin
      RABBITMQ_DEFAULT_PASS: password
      RABBITMQ_NODENAME: rabbit@rabbitmq1
      RABBITMQ_ERLANG_COOKIE: "MUDE_ESTE_TOKEN_SECRETO"
    volumes:
      - ./data:/var/lib/rabbitmq
      - ./log:/var/log/rabbitmq
    networks:
      - rabbitmq_net

networks:
  rabbitmq_net:
    driver: bridge
EOF

# 5. Iniciar o container
echo -e "\n${YELLOW}[5/6] Subindo o container do RabbitMQ 4...${NC}"
cd "$RABBITMQ_DIR"
sudo docker compose up -d

# 6. Status Final
echo -e "\n${YELLOW}[6/6] Verificando status do serviço...${NC}"
sudo docker compose ps

echo -e "\n${GREEN}======================================================================${NC}"
echo -e "${GREEN}   Instalação concluída com sucesso!                                  ${NC}"
echo -e "${GREEN}======================================================================${NC}"
echo -e " Painel Web:      http://<IP-DA-INSTANCIA>:8080"
echo -e " Usuário:         admin"
echo -e " Senha:           password"
echo -e " Porta AMQP:      5672"
echo -e " Diretorio:       $RABBITMQ_DIR"
echo -e "----------------------------------------------------------------------"
echo -e " OBS: Se for executar comandos 'docker' sem sudo futuramente, digite: "
echo -e " ${BLUE}newgrp docker${NC} ou faça logout e login novamente no terminal."
echo -e "======================================================================\n"
