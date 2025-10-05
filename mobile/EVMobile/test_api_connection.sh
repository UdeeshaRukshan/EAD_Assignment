#!/bin/bash

echo "🔍 Testing EV Charging API Connection for Android App"
echo "=================================================="

# Test direct localhost access (should work from host machine)
echo "1. Testing direct localhost access..."
curl -s -w "Status: %{http_code}, Time: %{time_total}s\n" http://localhost:5105/api/chargingstations | head -n 5

echo ""

# Test emulator access (10.0.2.2 maps to host localhost)
echo "2. Testing emulator API access (10.0.2.2:5105)..."
curl -s -w "Status: %{http_code}, Time: %{time_total}s\n" http://10.0.2.2:5105/api/chargingstations | head -n 5

echo ""

# Check if API is returning JSON
echo "3. Checking API response format..."
RESPONSE=$(curl -s http://localhost:5105/api/chargingstations)
if echo "$RESPONSE" | python3 -m json.tool > /dev/null 2>&1; then
    echo "✅ Valid JSON response"
    echo "Response contains $(echo "$RESPONSE" | jq '. | length') stations"
else
    echo "❌ Invalid JSON response"
    echo "Raw response: $RESPONSE"
fi

echo ""

# Test API endpoint variations
echo "4. Testing endpoint variations..."
echo "GET /api/chargingstations:"
curl -s -o /dev/null -w "Status: %{http_code}\n" http://localhost:5105/api/chargingstations

echo "GET /api/ChargingStations:"
curl -s -o /dev/null -w "Status: %{http_code}\n" http://localhost:5105/api/ChargingStations

echo ""
echo "🚀 Test completed! Make sure your API is running on port 5105"
echo "   Android app should connect to: http://localhost:5105/api/chargingstations"