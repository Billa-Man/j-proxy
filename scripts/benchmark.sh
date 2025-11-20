#!/bin/bash

echo "=========================================="
echo "J-Proxy Performance Benchmark"
echo "=========================================="
echo ""

BASE_URL="http://localhost:8080"
ENDPOINT="/users/123"
REQUESTS=50

if [ ! -z "$1" ]; then
    REQUESTS=$1
fi

echo "Endpoint: $ENDPOINT"
echo "Total requests: $REQUESTS"
echo ""
echo "Press Enter to start or Ctrl+C to cancel"
read

echo ""
echo "Running benchmark..."
echo "-----------------------------------"

TIMES=()
SUCCESS=0
FAILED=0

for i in $(seq 1 $REQUESTS); do
    # Use Python for millisecond precision (works on both macOS and Linux)
    START=$(python3 -c 'import time; print(int(time.time() * 1000))')
    status=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL$ENDPOINT")
    END=$(python3 -c 'import time; print(int(time.time() * 1000))')
    
    DURATION=$((END - START))
    TIMES+=($DURATION)
    
    if [ "$status" -ge 200 ] && [ "$status" -lt 400 ]; then
        ((SUCCESS++))
    else
        ((FAILED++))
    fi
    
    echo "Request $i: ${DURATION}ms (Status: $status)"
    sleep 0.1
done

# Calculate statistics
TOTAL=0
MIN=999999
MAX=0

for time in "${TIMES[@]}"; do
    TOTAL=$((TOTAL + time))
    if [ $time -lt $MIN ]; then MIN=$time; fi
    if [ $time -gt $MAX ]; then MAX=$time; fi
done

AVG=$((TOTAL / REQUESTS))

# Sort array for median
IFS=$'\n' SORTED=($(sort -n <<<"${TIMES[*]}"))
MEDIAN=${SORTED[$((REQUESTS / 2))]}

# Calculate percentiles
P95=${SORTED[$((REQUESTS * 95 / 100))]}
P99=${SORTED[$((REQUESTS * 99 / 100))]}

echo ""
echo "=========================================="
echo "Benchmark Results"
echo "=========================================="
echo "Total Requests: $REQUESTS"
echo "Successful: $SUCCESS"
echo "Failed: $FAILED"
echo ""
echo "Response Times:"
echo "  Min:    ${MIN}ms"
echo "  Max:    ${MAX}ms"
echo "  Avg:    ${AVG}ms"
echo "  Median: ${MEDIAN}ms"
echo "  P95:    ${P95}ms"
echo "  P99:    ${P99}ms"
echo "=========================================="