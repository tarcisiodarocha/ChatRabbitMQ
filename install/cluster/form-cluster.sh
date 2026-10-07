#!/bin/bash
# ============================================================
# form-cluster.sh — Forma o cluster e aplica a política de
# Quorum Queues com fator de replicação 3.
#
# Execute APÓS os 5 contêineres estarem saudáveis:
#   docker compose -f docker-compose-cluster.yml up -d
#   bash form-cluster.sh
# ============================================================

set -e

NODES=(rabbitmq2 rabbitmq3 rabbitmq4 rabbitmq5)

echo "==> Aguardando rabbitmq1 ficar pronto..."
until docker exec rabbitmq1 rabbitmqctl status > /dev/null 2>&1; do
  sleep 2
done
echo "    rabbitmq1 OK"

# Une os nós 2-5 ao nó principal (rabbitmq1)
for NODE in "${NODES[@]}"; do
  echo "==> Unindo $NODE ao cluster..."
  until docker exec "$NODE" rabbitmqctl status > /dev/null 2>&1; do
    sleep 2
  done

  docker exec "$NODE" rabbitmqctl stop_app
  docker exec "$NODE" rabbitmqctl reset
  docker exec "$NODE" rabbitmqctl join_cluster rabbit@rabbitmq1
  docker exec "$NODE" rabbitmqctl start_app
  echo "    $NODE unido com sucesso."
done

echo ""
echo "==> Verificando membros do cluster:"
docker exec rabbitmq1 rabbitmqctl cluster_status | grep -A 20 "Disk Nodes"

# Aplica política global de Quorum Queues com initial-group-size = 3.
# Isso garante que toda fila criada com x-queue-type=quorum seja
# replicada em 3 nós por padrão.
echo ""
echo "==> Aplicando política de Quorum Queue (fator de replicação 3)..."
docker exec rabbitmq1 rabbitmqctl set_policy \
  quorum-replication ".*" \
  '{"x-queue-type":"quorum","x-quorum-initial-group-size":3}' \
  --priority 0 \
  --apply-to queues

echo ""
echo "==> Cluster formado e política aplicada com sucesso!"
echo "    Painel de gerenciamento: http://localhost:15672  (admin/password)"
