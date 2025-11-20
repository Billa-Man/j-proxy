#!/bin/bash

echo "Testing rate limiting..."
echo "Configured: 10 requests per 60 seconds"
echo ""

URL="http://localhost:8080/posts/1"

# Check if server is running
if ! curl -s -o /dev/null "$URL" 2>&1; then
    echo "Error: Server not running at $URL"
    echo "Start with: java -jar target/j-proxy-1.0.0.jar --mode proxy --config config/proxy-config.json"
    exit 1
fi

for i in {1..15}; do
    echo -n "Request $i: "
    
    # Make request and capture headers
    headers=$(curl -s -I "$URL" 2>&1)
    http_code=$(echo "$headers" | grep -i "^HTTP" | tail -n 1 | awk '{print $2}')
    
    if [ "$http_code" = "200" ]; then
        remaining=$(echo "$headers" | grep -i "x-ratelimit-remaining" | awk -F': ' '{print $2}' | tr -d '\r')
        echo "✓ Success (Remaining: $remaining)"
    elif [ "$http_code" = "429" ]; then
        retry_after=$(echo "$headers" | grep -i "retry-after" | awk -F': ' '{print $2}' | tr -d '\r')
        echo "✗ Rate Limited (Retry after: ${retry_after}s)"
    else
        echo "? Unknown status: $http_code"
    fi
    
    sleep 0.5
done

echo ""
echo "Check stats at: http://localhost:8080/__stats"