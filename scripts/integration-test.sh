#!/bin/bash

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

BASE_URL="http://localhost:8080"
PASSED=0
FAILED=0

# Helper function
test_endpoint() {
    local name=$1
    local method=$2
    local path=$3
    local expected_status=$4
    local extra_check=$5
    
    echo -n "Testing $name... "
    
    if [ "$method" = "GET" ]; then
        response=$(curl -s -w "\n%{http_code}" "$BASE_URL$path")
    else
        response=$(curl -s -X "$method" -w "\n%{http_code}" "$BASE_URL$path")
    fi
    
    # Get status code (last line)
    status=$(echo "$response" | tail -n 1)
    # Get body (everything except last line)
    body=$(echo "$response" | sed '$d')
    
    if [ "$status" = "$expected_status" ]; then
        if [ -z "$extra_check" ] || echo "$body" | grep -q "$extra_check"; then
            echo -e "${GREEN}✓ PASSED${NC}"
            ((PASSED++))
        else
            echo -e "${RED}✗ FAILED (body check failed)${NC}"
            echo "  Expected to find: '$extra_check'"
            echo "  Body: $body"
            ((FAILED++))
        fi
    else
        echo -e "${RED}✗ FAILED (expected $expected_status, got $status)${NC}"
        echo "  Body: $body"
        ((FAILED++))
    fi
}

echo "========================================"
echo "J-Proxy Integration Tests"
echo "========================================"
echo ""

# Check if server is running
echo -n "Checking if server is running... "
if curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/" > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC}"
else
    echo -e "${RED}✗${NC}"
    echo "Error: Server not running at $BASE_URL"
    echo "Start server with: java -jar target/j-proxy-1.0.0.jar --mode mock --config config/mocks.json"
    exit 1
fi

echo ""
echo "Starting mock mode tests..."
echo ""

test_endpoint "Health endpoint" "GET" "/health" "200" "healthy"
test_endpoint "User endpoint" "GET" "/users/123" "200" "John Doe"
test_endpoint "Users list" "GET" "/users" "200" "Alice"
test_endpoint "Create user" "POST" "/users" "201" "created"
test_endpoint "Not found" "GET" "/notfound" "404" "No mock configured"

echo ""
echo "========================================"
echo -e "Results: ${GREEN}$PASSED passed${NC}, ${RED}$FAILED failed${NC}"
echo "========================================"

if [ $FAILED -eq 0 ]; then
    exit 0
else
    exit 1
fi