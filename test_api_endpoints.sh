#!/bin/bash
# API Endpoint Testing Script for WD Customer API
# Tests error handling and correlation IDs

set -e

# Configuration
API_BASE_URL="${API_BASE_URL:-http://localhost:8080}"
TOKEN="${JWT_TOKEN:-}"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test counter
TESTS_PASSED=0
TESTS_FAILED=0

# Helper function to make authenticated requests
api_call() {
    local method=$1
    local endpoint=$2
    local data=$3
    
    if [ -z "$TOKEN" ]; then
        echo -e "${RED}ERROR: JWT_TOKEN environment variable not set${NC}"
        exit 1
    fi
    
    if [ -z "$data" ]; then
        curl -s -X "$method" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            -w "\nHTTP_STATUS:%{http_code}" \
            "${API_BASE_URL}${endpoint}"
    else
        curl -s -X "$method" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            -d "$data" \
            -w "\nHTTP_STATUS:%{http_code}" \
            "${API_BASE_URL}${endpoint}"
    fi
}

# Test function
test_endpoint() {
    local test_name=$1
    local method=$2
    local endpoint=$3
    local expected_status=$4
    local data=$5
    
    echo -e "\n${YELLOW}Testing:${NC} $test_name"
    echo "  Endpoint: $method $endpoint"
    echo "  Expected: HTTP $expected_status"
    
    response=$(api_call "$method" "$endpoint" "$data")
    actual_status=$(echo "$response" | grep "HTTP_STATUS" | cut -d':' -f2)
    
    if [ "$actual_status" = "$expected_status" ]; then
        echo -e "  ${GREEN}✓ PASS${NC} (HTTP $actual_status)"
        ((TESTS_PASSED++))
    else
        echo -e "  ${RED}✗ FAIL${NC} (Expected $expected_status, got $actual_status)"
        echo "  Response: $response"
        ((TESTS_FAILED++))
    fi
}

echo "========================================="
echo "  WD Customer API Endpoint Tests"
echo "========================================="
echo "Base URL: $API_BASE_URL"
echo ""

# =============================================================================
# 1. SITE REPORTS ENDPOINTS (Error Handling Tests)
# =============================================================================
echo -e "\n${GREEN}=== Site Reports Error Handling ===${NC}"

test_endpoint \
    "Get non-existent site report (404)" \
    "GET" \
    "/api/customer/site-reports/999999" \
    "404"

test_endpoint \
    "Get site reports list" \
    "GET" \
    "/api/customer/site-reports?page=0&size=10" \
    "200"

# =============================================================================
# 2. DASHBOARD ENDPOINTS
# =============================================================================
echo -e "\n${GREEN}=== Dashboard Endpoints ===${NC}"

test_endpoint \
    "Get dashboard data" \
    "GET" \
    "/api/dashboard" \
    "200"

# =============================================================================
# 3. CORRELATION ID TESTS
# =============================================================================
echo -e "\n${GREEN}=== Correlation ID Tests ===${NC}"

echo "Testing correlation ID in error responses..."
response=$(api_call "GET" "/api/customer/site-reports/999999")
if echo "$response" | grep -q "correlationId"; then
    echo -e "  ${GREEN}✓ PASS${NC} Correlation ID present in error response"
    ((TESTS_PASSED++))
else
    echo -e "  ${RED}✗ FAIL${NC} Correlation ID missing from error response"
    ((TESTS_FAILED++))
fi

# =============================================================================
# 4. AUTHORIZATION TESTS
# =============================================================================
echo -e "\n${GREEN}=== Authorization Tests ===${NC}"

echo "Testing 403 Forbidden for unauthorized access..."
# This would need to test accessing a project the user doesn't have access to
# For now, we'll skip this as it requires specific test data setup

# =============================================================================
# SUMMARY
# =============================================================================
echo ""
echo "========================================="
echo "  Test Summary"
echo "========================================="
echo -e "Tests Passed: ${GREEN}$TESTS_PASSED${NC}"
echo -e "Tests Failed: ${RED}$TESTS_FAILED${NC}"
echo "Total Tests: $((TESTS_PASSED + TESTS_FAILED))"
echo ""

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}All tests passed! ✓${NC}"
    exit 0
else
    echo -e "${RED}Some tests failed! ✗${NC}"
    exit 1
fi
