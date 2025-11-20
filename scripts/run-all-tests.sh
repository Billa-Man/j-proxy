#!/bin/bash

echo "=========================================="
echo "J-Proxy Complete Test Suite"
echo "=========================================="
echo ""
echo "This will run all test scripts sequentially"
echo ""
echo "Make sure:"
echo "1. Server is NOT running yet"
echo "2. You have config files ready"
echo ""
echo "Press Enter to continue or Ctrl+C to cancel"
read

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo ""
echo "=========================================="
echo "Test 1: Mock Mode"
echo "=========================================="
echo "Please start server in MOCK mode with config file 'config/mocks.json' and press Enter"
read
bash "$SCRIPT_DIR/test-mock-mode.sh"

echo ""
echo "Press Enter when ready for next test..."
read

echo ""
echo "=========================================="
echo "Test 2: Proxy Mode"
echo "=========================================="
echo "Please RESTART server in PROXY mode with config file 'config/proxy-config.json' and press Enter"
read
bash "$SCRIPT_DIR/test-proxy-mode.sh"

echo ""
echo "Press Enter when ready for next test..."
read

echo ""
echo "=========================================="
echo "Test 3: Cache Testing"
echo "=========================================="
bash "$SCRIPT_DIR/test-cache.sh"

echo ""
echo "Press Enter when ready for next test..."
read

echo ""
echo "=========================================="
echo "Test 4: Stress Test"
echo "=========================================="
bash "$SCRIPT_DIR/stress-test.sh" proxy

echo ""
echo "=========================================="
echo "All Tests Complete!"
echo "=========================================="
echo ""
echo "Summary:"
echo "✓ Mock Mode Tests"
echo "✓ Proxy Mode Tests"
echo "✓ Cache Tests"
echo "✓ Stress Tests"
echo ""
echo "Check the dashboard for comprehensive metrics!"