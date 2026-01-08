#!/bin/bash
set -e

echo "========================================================================================"
echo "⚠️  WARNING: NATIVE IMAGE LIMITATION  ⚠️"
echo "========================================================================================"
echo "This project relies on QuickFIX/J (Apache Mina), which has known compatibility issues"
echo "with GraalVM Native Image. The project WILL build, but runtime network operations"
echo "will likely FAIL."
echo ""
echo "It is highly recommended to use the JVM mode: ./run-project.sh"
echo "========================================================================================"
echo ""
read -p "Press [Enter] to continue explicitly (or Ctrl+C to abort)..."

COMPOSE_FILE="./docker-compose-native-distroless.yml"
BUILDER_IMAGE="qfj-builder"
BUILDER_CONTAINER="qfj-extractor"

echo "Building QuickFIX/J from source..."
docker build -t "$BUILDER_IMAGE" -f Dockerfile.quickfixj-builder .

echo "Extracting artifacts to local maven repositories..."
docker rm -f "$BUILDER_CONTAINER" 2>/dev/null || true
docker create --name "$BUILDER_CONTAINER" "$BUILDER_IMAGE"

mkdir -p broker-back-end/local-m2/org/quickfixj
mkdir -p exchange-back-end/local-m2/org/quickfixj

docker cp "$BUILDER_CONTAINER":/root/.m2/repository/org/quickfixj/. broker-back-end/local-m2/org/quickfixj/
docker cp "$BUILDER_CONTAINER":/root/.m2/repository/org/quickfixj/. exchange-back-end/local-m2/org/quickfixj/

docker rm -f "$BUILDER_CONTAINER"

trap 'echo "Stopping..."; docker-compose -f "$COMPOSE_FILE" down; rm -rf broker-back-end/local-m2 exchange-back-end/local-m2' EXIT

docker-compose -f "$COMPOSE_FILE" up --build --remove-orphans
