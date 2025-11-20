#!/bin/bash

echo "=========================================="
echo "Testing J-Proxy: Mock Mode"
echo "=========================================="
echo ""
echo "Make sure server is running in MOCK mode on port 8080"
echo "Press Enter to continue or Ctrl+C to cancel"
read

BASE_URL="http://localhost:8080"
DELAY=0.3

echo ""
echo "1. Testing successful endpoints..."
echo "-----------------------------------"

echo "GET /users/123"
curl -s "$BASE_URL/users/123" | jq -C '.'
sleep $DELAY

echo ""
echo "GET /users"
curl -s "$BASE_URL/users" | jq -C '.'
sleep $DELAY

echo ""
echo "POST /users"
curl -s -X POST "$BASE_URL/users" -d '{"name":"Test User"}' | jq -C '.'
sleep $DELAY

echo ""
echo "GET /health"
curl -s "$BASE_URL/health" | jq -C '.'
sleep $DELAY

echo ""
echo "2. Testing error endpoint..."
echo "-----------------------------------"
echo "GET /error"
curl -s "$BASE_URL/error" | jq -C '.'
sleep $DELAY

echo ""
echo "3. Testing slow endpoint (2s delay)..."
echo "-----------------------------------"
echo "GET /slow"
curl -s "$BASE_URL/slow" | jq -C '.'
sleep $DELAY

echo ""
echo "4. Testing 404 (non-existent endpoint)..."
echo "-----------------------------------"
echo "GET /notfound"
curl -s "$BASE_URL/notfound" | jq -C '.'
sleep $DELAY

echo ""
echo "5. Rapid fire test (20 requests)..."
echo "-----------------------------------"
for i in {1..20}; do
    echo -n "Request $i... "
    status=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/users/123")
    echo "Status: $status"
    sleep 0.1
done

echo ""
echo "=========================================="
echo "Test complete! Check dashboard metrics:"
echo "- Total Requests should be ~25"
echo "- Success Rate should be ~96%"
echo "- Request table should show all requests"
echo "=========================================="