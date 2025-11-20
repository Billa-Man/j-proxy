#!/bin/bash

echo "=========================================="
echo "J-Proxy Stress Test"
echo "=========================================="
echo ""
echo "This will send 100 requests rapidly"
echo "Make sure server is running on port 8080"
echo ""
echo "Press Enter to continue or Ctrl+C to cancel"
read

BASE_URL="http://localhost:8080"
MODE=$1

if [ -z "$MODE" ]; then
    echo "Usage: $0 [mock|proxy]"
    exit 1
fi

if [ "$MODE" == "mock" ]; then
    ENDPOINTS=("/users/123" "/users" "/health" "/error")
else
    ENDPOINTS=("/posts/1" "/users/1" "/comments/1" "/todos/1")
fi

echo ""
echo "Starting stress test..."
echo "Mode: $MODE"
echo "Endpoints: ${#ENDPOINTS[@]}"
echo "Total requests: 100"
echo "-----------------------------------"

SUCCESS=0
FAILED=0
START_TIME=$(date +%s)

for i in {1..100}; do
    # Pick random endpoint
    ENDPOINT=${ENDPOINTS[$RANDOM % ${#ENDPOINTS[@]}]}
    
    # Make request
    status=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL$ENDPOINT")
    
    if [ "$status" -ge 200 ] && [ "$status" -lt 400 ]; then
        ((SUCCESS++))
        echo -n "."
    else
        ((FAILED++))
        echo -n "x"
    fi
    
    # Progress indicator
    if [ $((i % 50)) -eq 0 ]; then
        echo " $i"
    fi
    
    sleep 0.05
done

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo ""
echo ""
echo "=========================================="
echo "Stress Test Results"
echo "=========================================="
echo "Total Requests: 100"
echo "Successful: $SUCCESS"
echo "Failed: $FAILED"
echo "Success Rate: $((SUCCESS * 100 / 100))%"
echo "Duration: ${DURATION}s"
echo "Avg: $((100 / DURATION)) req/s"
echo "=========================================="
echo ""
echo "Check dashboard for detailed metrics!"