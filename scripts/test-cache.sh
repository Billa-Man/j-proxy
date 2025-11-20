#!/bin/bash

echo "=========================================="
echo "Testing J-Proxy: Cache Functionality"
echo "=========================================="
echo ""
echo "Make sure server is running in PROXY mode"
echo "with CACHING ENABLED"
echo ""
echo "Press Enter to continue or Ctrl+C to cancel"
read

BASE_URL="http://localhost:8080"

echo ""
echo "Testing cache behavior..."
echo "-----------------------------------"

# Test endpoint 1
ENDPOINT="/posts/1"
echo ""
echo "Testing $ENDPOINT (should see MISS then HITs)"
for i in {1..5}; do
    echo -n "Request $i: "
    cache_status=$(curl -s -I "$BASE_URL$ENDPOINT" | grep -i "x-cache:" | awk '{print $2}' | tr -d '\r')
    echo "Cache: $cache_status"
    sleep 0.3
done

# Test endpoint 2
ENDPOINT="/users/1"
echo ""
echo "Testing $ENDPOINT (should see MISS then HITs)"
for i in {1..5}; do
    echo -n "Request $i: "
    cache_status=$(curl -s -I "$BASE_URL$ENDPOINT" | grep -i "x-cache:" | awk '{print $2}' | tr -d '\r')
    echo "Cache: $cache_status"
    sleep 0.3
done

# Test endpoint 3
ENDPOINT="/comments/1"
echo ""
echo "Testing $ENDPOINT (should see MISS then HITs)"
for i in {1..5}; do
    echo -n "Request $i: "
    cache_status=$(curl -s -I "$BASE_URL$ENDPOINT" | grep -i "x-cache:" | awk '{print $2}' | tr -d '\r')
    echo "Cache: $cache_status"
    sleep 0.3
done

echo ""
echo "Testing multiple different endpoints..."
echo "-----------------------------------"
for id in {1..10}; do
    echo -n "GET /posts/$id: "
    cache_status=$(curl -s -I "$BASE_URL/posts/$id" | grep -i "x-cache:" | awk '{print $2}' | tr -d '\r')
    echo "Cache: $cache_status"
    sleep 0.2
done

echo ""
echo "Re-testing same endpoints (all should be HIT)..."
echo "-----------------------------------"
for id in {1..10}; do
    echo -n "GET /posts/$id: "
    cache_status=$(curl -s -I "$BASE_URL/posts/$id" | grep -i "x-cache:" | awk '{print $2}' | tr -d '\r')
    echo "Cache: $cache_status"
    sleep 0.2
done

echo ""
echo "=========================================="
echo "Test complete! Check dashboard:"
echo "- Cache Hits: should be ~25"
echo "- Cache Misses: should be ~13"
echo "- Hit Rate: ~65%"
echo "- Cache Size: 10/100"
echo "=========================================="