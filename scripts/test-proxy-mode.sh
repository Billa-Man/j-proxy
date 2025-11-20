#!/bin/bash

echo "=========================================="
echo "Testing J-Proxy: Proxy Mode"
echo "=========================================="
echo ""
echo "Make sure server is running in PROXY mode on port 8080"
echo "with config: config/proxy-config.json"
echo "Target: https://jsonplaceholder.typicode.com"
echo ""
echo "Press Enter to continue or Ctrl+C to cancel"
read

BASE_URL="http://localhost:8080"
DELAY=0.5

echo ""
echo "1. Testing basic GET requests..."
echo "-----------------------------------"

echo "GET /posts/1"
curl -s "$BASE_URL/posts/1" | jq -C '. | {id, title}'
sleep $DELAY

echo ""
echo "GET /users/1"
curl -s "$BASE_URL/users/1" | jq -C '. | {id, name, email}'
sleep $DELAY

echo ""
echo "GET /comments/1"
curl -s "$BASE_URL/comments/1" | jq -C '. | {id, name, email}'
sleep $DELAY

echo ""
echo "2. Testing POST request..."
echo "-----------------------------------"
echo "POST /posts"
curl -s -X POST "$BASE_URL/posts" \
  -H "Content-Type: application/json" \
  -d '{"title":"Test","body":"Test body","userId":1}' | jq -C '.'
sleep $DELAY

echo ""
echo "3. Testing various endpoints..."
echo "-----------------------------------"
for endpoint in "/todos/1" "/albums/1" "/photos/1"; do
    echo "GET $endpoint"
    curl -s "$BASE_URL$endpoint" | jq -C '. | {id, title}'
    sleep $DELAY
done

echo ""
echo "=========================================="
echo "Test complete! Check dashboard:"
echo "- Total Requests: ~7"
echo "- All should be MISS (first time)"
echo "=========================================="