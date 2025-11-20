#!/bin/bash

echo "=========================================="
echo "Testing J-Proxy: Rate Limiting"
echo "=========================================="
echo ""
echo "Make sure server is running in PROXY mode"
echo "with RATE LIMITING ENABLED"
echo "Example config: 10 requests per 60 seconds"
echo ""
echo "Press Enter to continue or Ctrl+C to cancel"
read

BASE_URL="http://localhost:8080"
ENDPOINT="/posts/1"

echo ""
echo "Sending rapid requests to trigger rate limit..."
echo "-----------------------------------"

for i in {1..20}; do
    response=$(curl -s -w "\nHTTP_CODE:%{http_code}" "$BASE_URL$ENDPOINT")
    http_code=$(echo "$response" | grep "HTTP_CODE" | cut -d: -f2)
    
    echo -n "Request $i: "
    
    if [ "$http_code" == "200" ]; then
        remaining=$(curl -s -I "$BASE_URL$ENDPOINT" 2>&1 | grep -i "x-ratelimit-remaining" | awk -F': ' '{print $2}' | tr -d '\r')
        echo "✓ Success (Remaining: $remaining)"
    elif [ "$http_code" == "429" ]; then
        retry_after=$(curl -s -I "$BASE_URL$ENDPOINT" 2>&1 | grep -i "retry-after" | awk -F': ' '{print $2}' | tr -d '\r')
        echo "✗ Rate Limited (Retry after: ${retry_after}s)"
    else
        echo "? Unknown status: $http_code"
    fi
    
    sleep 0.3
done

echo ""
echo "=========================================="
echo "Test complete! You should see:"
echo "- First 10 requests: Success"
echo "- Remaining requests: Rate Limited"
echo "- Wait 60 seconds for reset"
echo "=========================================="