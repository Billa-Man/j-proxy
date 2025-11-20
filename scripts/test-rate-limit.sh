#!/bin/bash

echo "Testing rate limiting..."
echo "Configured: 10 requests per 60 seconds"
echo ""

URL="http://localhost:8080/posts/1"

for i in {1..15}; do
    echo -n "Request $i: "
    response=$(curl -s -w "\nHTTP_CODE:%{http_code}" "$URL")
    http_code=$(echo "$response" | grep "HTTP_CODE" | cut -d: -f2)
    
    if [ "$http_code" == "200" ]; then
        remaining=$(curl -s -I "$URL" 2>&1 | grep -i "x-ratelimit-remaining" | cut -d: -f2 | tr -d ' \r')
        echo "✓ Success (Remaining: $remaining)"
    elif [ "$http_code" == "429" ]; then
        retry_after=$(curl -s -I "$URL" 2>&1 | grep -i "retry-after" | cut -d: -f2 | tr -d ' \r')
        echo "✗ Rate Limited (Retry after: ${retry_after}s)"
    else
        echo "? Unknown status: $http_code"
    fi
    
    sleep 0.5
done

echo ""
echo "Check stats at: http://localhost:8080/__stats"