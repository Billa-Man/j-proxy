#!/bin/bash

CONFIG="${1:-config/mocks.json}"
PORT="${2:-8080}"

echo "Starting J-Proxy in MOCK mode..."
echo "Config: $CONFIG"
echo "Port: $PORT"
echo ""

java -jar target/j-proxy-1.0.0.jar \
    --mode mock \
    --config "$CONFIG" \
    --port "$PORT"