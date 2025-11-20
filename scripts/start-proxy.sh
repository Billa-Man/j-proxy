#!/bin/bash

TARGET="${1:-https://jsonplaceholder.typicode.com}"
CONFIG="${2:-config/proxy-config.json}"
PORT="${3:-8080}"

echo "Starting J-Proxy in PROXY mode..."
echo "Target: $TARGET"
echo "Config: $CONFIG"
echo "Port: $PORT"
echo ""

java -jar target/j-proxy-1.0.0.jar \
    --mode proxy \
    --target "$TARGET" \
    --config "$CONFIG" \
    --port "$PORT"