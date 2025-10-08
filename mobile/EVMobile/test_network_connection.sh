#!/bin/bash

echo "=== Testing Network Connection for Android Emulator ==="
echo ""

echo "1. Testing localhost API accessibility..."
curl -X GET "http://localhost:5105/api/chargingstations" \
  -H "accept: application/json" \
  --connect-timeout 10 \
  --max-time 15 \
  -w "\nStatus: %{http_code}\nTime: %{time_total}s\n" \
  -s -o /dev/null

echo ""
echo "2. Testing emulator API endpoint (10.0.2.2)..."
curl -X GET "http://10.0.2.2:5105/api/chargingstations" \
  -H "accept: application/json" \
  --connect-timeout 10 \
  --max-time 15 \
  -w "\nStatus: %{http_code}\nTime: %{time_total}s\n" \
  -s -o /dev/null

echo ""
echo "3. Testing registration endpoint..."
curl -X POST "http://10.0.2.2:5105/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Test",
    "lastName": "User", 
    "email": "testnetwork@example.com",
    "phoneNumber": "+1234567890",
    "password": "TestPassword123!",
    "role": 3
  }' \
  --connect-timeout 10 \
  --max-time 15 \
  -w "\nStatus: %{http_code}\nTime: %{time_total}s\n" \
  -s -o /dev/null

echo ""
echo "=== Network Test Complete ==="