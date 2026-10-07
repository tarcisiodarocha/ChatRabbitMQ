# Cluster RabbitMQ 4 com Alta Disponibilidade na AWS

## Visão Geral

| Componente | Descrição |
|---|---|
| 5 instâncias EC2 | Ubuntu 24.04 (t3.small recomendado), cada uma rodando 1 nó RabbitMQ via Docker |
| Cluster | Os 5 nós operam como um único serviço RabbitMQ |
| Quorum Queues | Filas replicadas em 3 nós (fator 3) — tolerância a até 2 falhas |
| Network Load Balancer | Distribui conexões AMQP (5672) e HTTP (15672) entre os 5 nós |

---

## 1. Criar o Grupo de Segurança

No Console da AWS, crie **um único grupo de segurança** (`rabbitmq-cluster-sg`) com as seguintes regras de entrada:

| Tipo | Protocolo | Porta | Origem | Descrição |
|---|---|---|---|---|
| SSH | TCP | 22 | Seu IP | Acesso administrativo |
| Custom TCP | TCP | 5672 | 0.0.0.0/0 | AMQP (clientes) |
| Custom TCP | TCP | 15672 | 0.0.0.0/0 | Management HTTP |
| Custom TCP | TCP | 4369 | `rabbitmq-cluster-sg` | EPMD (inter-nó) |
| Custom TCP | TCP | 25672 | `rabbitmq-cluster-sg` | Comunicação inter-nó |

> A origem `rabbitmq-cluster-sg` (o próprio grupo) permite que os nós se comuniquem entre si.

---

## 2. Criar as 5 Instâncias EC2

Crie **5 instâncias idênticas** com:

- **AMI:** Ubuntu Server 24.04 LTS
- **Tipo:** t3.small (mínimo)
- **Grupo de segurança:** `rabbitmq-cluster-sg`
- **Par de chaves:** o mesmo par para todas

Nomeie-as: `rabbitmq-node-1` até `rabbitmq-node-5`.

---

## 3. Instalar Docker em Cada Instância

Execute os comandos abaixo em **cada uma das 5 instâncias** via SSH:

```bash
sudo apt update && sudo apt upgrade -y

sudo apt install -y ca-certificates curl gnupg lsb-release
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | \
  sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

sudo usermod -aG docker $USER
newgrp docker
```

---

## 4. Copiar os Arquivos do Cluster

Em cada instância, crie o diretório e copie os arquivos:

```bash
mkdir -p ~/rabbitmq-cluster
```

Copie os arquivos `docker-compose-cluster.yml` e `rabbitmq.conf` para `~/rabbitmq-cluster/` em cada instância (via `scp` ou copiando o conteúdo manualmente).

---

## 5. Configurar o /etc/hosts em Cada Instância

Os nós precisam se resolver pelo hostname. Em **cada instância**, adicione ao `/etc/hosts` os IPs **privados** das demais:

```bash
sudo nano /etc/hosts
```

Acrescente (substituindo pelos IPs privados reais):

```
172.31.10.1  rabbitmq1
172.31.10.2  rabbitmq2
172.31.10.3  rabbitmq3
172.31.10.4  rabbitmq4
172.31.10.5  rabbitmq5
```

> Cada instância deve ter seu próprio hostname no `/etc/hosts` apontando para `127.0.0.1` ou seu IP privado. Confirme com `hostname`.

---

## 6. Subir Um Nó por Instância (docker-compose de nó único)

Em vez do `docker-compose-cluster.yml` (que sobe 5 contêineres em uma máquina), em **produção com 5 EC2 separadas** usa-se o `docker-compose.yml` do nó único (existente em `install/`) com as variáveis de ambiente ajustadas por instância:

**Na instância `rabbitmq-node-1`:**
```bash
cd ~/rabbitmq-cluster
RABBITMQ_NODENAME=rabbit@rabbitmq1 docker compose up -d
```

**Na instância `rabbitmq-node-2`:**
```bash
RABBITMQ_NODENAME=rabbit@rabbitmq2 docker compose up -d
```

E assim por diante para os nós 3, 4 e 5.

> **Alternativa para demonstração (1 EC2 grande):** Use o `docker-compose-cluster.yml` deste diretório para subir os 5 nós em uma única máquina (recomenda-se t3.xlarge ou maior).

---

## 7. Formar o Cluster

### Opção A — Usando o script automatizado (cluster Docker Compose)

```bash
bash form-cluster.sh
```

### Opção B — Manual (5 EC2 separadas)

Nos **nós 2 a 5**, execute:

```bash
docker exec rabbitmq rabbitmqctl stop_app
docker exec rabbitmq rabbitmqctl reset
docker exec rabbitmq rabbitmqctl join_cluster rabbit@rabbitmq1
docker exec rabbitmq rabbitmqctl start_app
```

Verificar no nó 1:

```bash
docker exec rabbitmq1 rabbitmqctl cluster_status
```

---

## 8. Configurar a Política de Quorum Queues (Replicação Fator 3)

Execute **uma única vez** em qualquer nó do cluster:

```bash
docker exec rabbitmq1 rabbitmqctl set_policy \
  quorum-replication ".*" \
  '{"x-queue-type":"quorum","x-quorum-initial-group-size":3}' \
  --priority 0 \
  --apply-to queues
```

Isso garante que cada fila seja replicada em **3 nós**. Com 5 nós e fator 3, o cluster tolera a falha de até **2 nós simultâneos** sem perda de serviço.

Verificar política aplicada:

```bash
docker exec rabbitmq1 rabbitmqctl list_policies
```

---

## 9. Criar o Network Load Balancer na AWS

O **Network Load Balancer (NLB)** opera na camada TCP, o que é necessário para o protocolo AMQP.

### 9.1 Criar o NLB

No Console AWS → EC2 → Load Balancers → **Create Load Balancer** → **Network Load Balancer**:

- **Nome:** `rabbitmq-nlb`
- **Scheme:** Internet-facing
- **IP address type:** IPv4
- **Mapeamento de rede:** selecione as AZs das 5 instâncias

### 9.2 Criar os Target Groups

Crie **dois Target Groups** (EC2 → Target Groups → Create):

**Target Group 1 — AMQP:**
| Campo | Valor |
|---|---|
| Nome | `rabbitmq-amqp-tg` |
| Protocol | TCP |
| Port | 5672 |
| Health check protocol | TCP |
| Health check port | 5672 |

Registre as 5 instâncias EC2 como targets na porta 5672.

**Target Group 2 — Management HTTP:**
| Campo | Valor |
|---|---|
| Nome | `rabbitmq-mgmt-tg` |
| Protocol | TCP |
| Port | 15672 |
| Health check protocol | HTTP |
| Health check path | `/` |
| Health check port | 15672 |

Registre as 5 instâncias EC2 como targets na porta 15672.

### 9.3 Adicionar os Listeners ao NLB

Após criar o NLB, adicione dois listeners:

| Listener | Protocol | Port | Target Group |
|---|---|---|---|
| AMQP | TCP | 5672 | `rabbitmq-amqp-tg` |
| Management | TCP | 15672 | `rabbitmq-mgmt-tg` |

### 9.4 Anotar o DNS do NLB

Após a criação, o NLB terá um DNS no formato:
```
rabbitmq-nlb-xxxxxxxxxxxx.elb.us-east-1.amazonaws.com
```

Use esse endereço na aplicação Java (ver seção 10).

---

## 10. Conectar o Cliente Java ao Cluster via LB

Execute o chat passando o DNS do NLB como argumento:

```bash
java -jar target/ChatRabbitMQ-1.0-SNAPSHOT-jar-with-dependencies.jar \
  rabbitmq-nlb-xxxxxxxxxxxx.elb.us-east-1.amazonaws.com
```

Se nenhum argumento for passado, o cliente usa o host padrão configurado no código.

---

## 11. Verificar Alta Disponibilidade

Para testar a resiliência, derrube um nó e verifique que o chat continua funcionando:

```bash
# Derrubar nó 2
docker stop rabbitmq2

# O chat deve continuar funcionando normalmente.
# Verificar o estado do cluster no nó 1:
docker exec rabbitmq1 rabbitmqctl cluster_status
```

Com fator de replicação 3 e 5 nós, até **2 nós podem cair** sem interrupção do serviço.

---

## Resumo da Arquitetura

```
Clientes Java
     │
     ▼
┌────────────────────────┐
│  Network Load Balancer │  DNS: rabbitmq-nlb-xxx.elb.amazonaws.com
│  TCP 5672  (AMQP)      │
│  TCP 15672 (Management)│
└────────┬───────────────┘
         │  distribui entre os 5 nós
    ┌────┴────────────────────────────────┐
    │         Cluster RabbitMQ 4          │
    │  ┌──────┐ ┌──────┐ ┌──────┐        │
    │  │ nó 1 │ │ nó 2 │ │ nó 3 │ ...   │
    │  └──────┘ └──────┘ └──────┘        │
    │     Quorum Queues — fator 3         │
    └─────────────────────────────────────┘
```
