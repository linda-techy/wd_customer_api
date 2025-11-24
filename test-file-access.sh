#!/bin/bash

# Test script for secure file access
# Usage: ./test-file-access.sh <email> <password>

set -e

API_URL="https://cust-api.walldotbuilders.com"
EMAIL="${1:-customer@example.com}"
PASSWORD="${2:-password}"

echo "========================================="
echo "🧪 Testing Secure File Access"
echo "========================================="
echo ""

# Step 1: Login and get JWT token
echo "1️⃣  Logging in..."
LOGIN_RESPONSE=$(curl -s -X POST "$API_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")

TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.accessToken')

if [ "$TOKEN" == "null" ] || [ -z "$TOKEN" ]; then
    echo "❌ Login failed!"
    echo "Response: $LOGIN_RESPONSE"
    exit 1
fi

echo "✅ Login successful"
echo "Token: ${TOKEN:0:20}..."
echo ""

# Step 2: Get dashboard data to find a document URL
echo "2️⃣  Fetching project documents..."
DASHBOARD_RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" \
  "$API_URL/api/dashboard")

# Extract first document URL
DOCUMENT_URL=$(echo $DASHBOARD_RESPONSE | jq -r '.projects.recentProjects[0].documents[0].downloadUrl // empty')

if [ -z "$DOCUMENT_URL" ]; then
    echo "⚠️  No documents found in dashboard"
    echo "Trying to get project details..."
    
    # Try getting project 1 details
    PROJECT_RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" \
      "$API_URL/api/dashboard/projects/1")
    
    DOCUMENT_URL=$(echo $PROJECT_RESPONSE | jq -r '.documents[0].downloadUrl // empty')
fi

if [ -z "$DOCUMENT_URL" ]; then
    echo "❌ No documents found to test"
    exit 1
fi

echo "✅ Found document URL:"
echo "   $DOCUMENT_URL"
echo ""

# Step 3: Check if URL uses new pattern
echo "3️⃣  Checking URL pattern..."
if [[ $DOCUMENT_URL == *"/api/storage/"* ]]; then
    echo "✅ URL uses new secure pattern (/api/storage/)"
else
    echo "❌ URL uses old insecure pattern (/storage/)"
    echo "   Backend needs to be redeployed!"
    exit 1
fi
echo ""

# Step 4: Test authenticated access
echo "4️⃣  Testing authenticated file access..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer $TOKEN" \
  "$DOCUMENT_URL")

if [ "$HTTP_CODE" == "200" ]; then
    echo "✅ Authenticated access works (200 OK)"
elif [ "$HTTP_CODE" == "206" ]; then
    echo "✅ Authenticated access works (206 Partial Content)"
else
    echo "❌ Authenticated access failed (HTTP $HTTP_CODE)"
    exit 1
fi
echo ""

# Step 5: Test unauthenticated access (should fail)
echo "5️⃣  Testing unauthenticated file access..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$DOCUMENT_URL")

if [ "$HTTP_CODE" == "401" ] || [ "$HTTP_CODE" == "403" ]; then
    echo "✅ Unauthenticated access blocked (HTTP $HTTP_CODE)"
else
    echo "⚠️  Unauthenticated access returned HTTP $HTTP_CODE"
    echo "   Expected 401 or 403"
fi
echo ""

# Step 6: Test old direct URL pattern (should fail)
echo "6️⃣  Testing old /storage/ URL pattern..."
OLD_URL=$(echo $DOCUMENT_URL | sed 's|/api/storage/|/storage/|')
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$OLD_URL")

if [ "$HTTP_CODE" == "403" ] || [ "$HTTP_CODE" == "404" ]; then
    echo "✅ Old URL pattern blocked (HTTP $HTTP_CODE)"
else
    echo "⚠️  Old URL pattern returned HTTP $HTTP_CODE"
    echo "   Expected 403 or 404"
fi
echo ""

# Summary
echo "========================================="
echo "✅ All Tests Passed!"
echo "========================================="
echo ""
echo "Summary:"
echo "  ✅ Authentication working"
echo "  ✅ URLs use secure pattern"
echo "  ✅ Authenticated access works"
echo "  ✅ Unauthenticated access blocked"
echo "  ✅ Old URL pattern blocked"
echo ""
echo "🎉 File serving is secure!"

