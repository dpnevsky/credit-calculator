#!/bin/bash
# Kafka topic initialization script for credit-calculator
# Run this after Kafka broker is ready

KAFKA_BROKER="${KAFKA_BROKER:-localhost:9092}"

echo "Creating Kafka topics on broker: $KAFKA_BROKER"

# Document flow topics
kafka-topics.sh --bootstrap-server "$KAFKA_BROKER" --create --if-not-exists \
  --topic document-generation-requested \
  --partitions 3 \
  --replication-factor 1 \
  --config retention.ms=604800000

kafka-topics.sh --bootstrap-server "$KAFKA_BROKER" --create --if-not-exists \
  --topic document-generated \
  --partitions 3 \
  --replication-factor 1 \
  --config retention.ms=604800000

# Application lifecycle events
kafka-topics.sh --bootstrap-server "$KAFKA_BROKER" --create --if-not-exists \
  --topic application-status-changed \
  --partitions 3 \
  --replication-factor 1 \
  --config retention.ms=604800000

# Dead letter queue
kafka-topics.sh --bootstrap-server "$KAFKA_BROKER" --create --if-not-exists \
  --topic dead-letter-queue \
  --partitions 1 \
  --replication-factor 1 \
  --config retention.ms=2592000000

echo "Kafka topics created successfully."

kafka-topics.sh --bootstrap-server "$KAFKA_BROKER" --list
