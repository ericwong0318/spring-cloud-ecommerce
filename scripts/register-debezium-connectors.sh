#!/bin/bash
# Debezium Connector Registration Script
# Registers PostgreSQL CDC connectors for each service's outbox table

set -e

DEBEZIUM_HOST="localhost"
DEBEZIUM_PORT="8084"
KAFKA_CONNECT_HOST="localhost"
KAFKA_CONNECT_PORT="8083"

echo "Waiting for Debezium Connect to be ready..."
until curl -s -f "http://${DEBEZIUM_HOST}:${DEBEZIUM_PORT}/connectors" > /dev/null; do
    echo "Waiting for Debezium Connect..."
    sleep 5
done

echo "Debezium Connect is ready. Registering connectors..."

# Function to register a connector
register_connector() {
    local connector_name=$1
    local database_host=$2
    local database_name=$3
    local table_name=$4
    local topic_prefix=$5

    echo "Registering connector: ${connector_name}"

    curl -X POST -H "Content-Type: application/json" \
        --data "{
            \"name\": \"${connector_name}\",
            \"config\": {
                \"connector.class\": \"io.debezium.connector.postgresql.PostgresConnector\",
                \"database.hostname\": \"${database_host}\",
                \"database.port\": \"5432\",
                \"database.user\": \"postgres\",
                \"database.password\": \"postgres\",
                \"database.dbname\": \"${database_name}\",
                \"database.server.name\": \"${topic_prefix}\",
                \"table.include.list\": \"public.event_outbox\",
                \"plugin.name\": \"pgoutput\",
                \"publication.autocreate.mode\": \"filtered\",
                \"slot.name\": \"${connector_name}_slot\",
                \"publication.name\": \"${connector_name}_pub\",
                \"transforms\": \"unwrap,addTopicPrefix\",
                \"transforms.unwrap.type\": \"io.debezium.transforms.ExtractNewRecordState\",
                \"transforms.unwrap.drop.tombstones\": \"true\",
                \"transforms.unwrap.delete.handling.mode\": \"rewrite\",
                \"transforms.addTopicPrefix.type\": \"org.apache.kafka.connect.transforms.RegexRouter\",
                \"transforms.addTopicPrefix.regex\": \"([^.]+)\\.([^.]+)\\.([^.]+)\",
                \"transforms.addTopicPrefix.replacement\": \"\${3}.events\",
                \"key.converter\": \"org.apache.kafka.connect.json.JsonConverter\",
                \"value.converter\": \"org.apache.kafka.connect.json.JsonConverter\",
                \"key.converter.schemas.enable\": \"false\",
                \"value.converter.schemas.enable\": \"false\",
                \"snapshot.mode\": \"never\"
            }
        }" \
        "http://${DEBEZIUM_HOST}:${DEBEZIUM_PORT}/connectors" \
        -w "\n%{http_code}\n" -s | tail -1
}

# Register connectors for each service
echo "=== Registering Debezium connectors ==="

register_connector "product-outbox-connector" "postgres-product" "product_db" "event_outbox" "product"
register_connector "category-outbox-connector" "postgres-category" "category_db" "event_outbox" "category"
register_connector "order-outbox-connector" "postgres-order" "order_db" "event_outbox" "order"
register_connector "inventory-outbox-connector" "postgres-inventory" "inventory_db" "event_outbox" "inventory"
register_connector "notification-outbox-connector" "postgres-notification" "notification_db" "event_outbox" "notification"
register_connector "payment-outbox-connector" "postgres-payment" "payment_db" "event_outbox" "payment"

echo "=== All connectors registered ==="

# List registered connectors
echo "Registered connectors:"
curl -s "http://${DEBEZIUM_HOST}:${DEBEZIUM_PORT}/connectors" | jq .