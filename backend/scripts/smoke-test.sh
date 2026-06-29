#!/usr/bin/env bash
set -euo pipefail

BASE_URL=${VITE_API_BASE_URL:-http://localhost:8080/api}

parse_http_code() {
  echo "$1" | sed -n 's/.*HTTP_CODE:\([0-9]*\)/\1/p'
}

echo "=== HEALTH ==="
if ! curl -sS "$BASE_URL/health" >/dev/null; then
  echo "Health check failed"; exit 1
fi

echo -e "\n=== PRODUCTS ==="
prod=$(curl -sS "$BASE_URL/products") || { echo "Products request failed"; exit 1; }
echo "$prod" | jq . || true

# Register a demo user (ignore if already exists)
echo -e "\n=== REGISTER (demo@example.com) ==="
reg_out=$(curl -sS -w "\nHTTP_CODE:%{http_code}\n" -X POST "$BASE_URL/auth/register" -H "Content-Type: application/json" -d '{"name":"Demo User","email":"demo@example.com","password":"password"}' ) || true
reg_code=$(parse_http_code "$reg_out")
echo "$reg_out"

# Login demo user and capture token (if returned)
echo -e "\n=== LOGIN (demo@example.com) ==="
login_out=$(curl -sS -i -w "\nHTTP_CODE:%{http_code}\n" -X POST "$BASE_URL/auth/login" -H "Content-Type: application/json" -d '{"email":"demo@example.com","password":"password"}' ) || true
login_code=$(parse_http_code "$login_out")
echo "$login_out"

# Try admin login (seeded by Flyway)
echo -e "\n=== LOGIN (admin@example.com) ==="
admin_out=$(curl -sS -i -w "\nHTTP_CODE:%{http_code}\n" -X POST "$BASE_URL/auth/login" -H "Content-Type: application/json" -d '{"email":"admin@example.com","password":"admin123"}' ) || true
admin_code=$(parse_http_code "$admin_out")
echo "$admin_out"

extract_token() {
  echo "$1" | sed -n 's/.*{"token":"\([^"]*\)".*/\1/p'
}

# Create a simple address for the demo user (idempotent)
if [ -n "$(echo "$login_out" | sed -n 's/.*{"token":"\([^"]*\)".*/\1/p')" ]; then
  DEMO_TOKEN=$(echo "$login_out" | sed -n 's/.*{"token":"\([^"]*\)".*/\1/p')
  echo -e "\n=== CREATE ADDRESS (demo) ==="
  addr_out=$(curl -sS -w "\nHTTP_CODE:%{http_code}\n" -X POST "$BASE_URL/addresses" -H "Content-Type: application/json" -H "Authorization: Bearer $DEMO_TOKEN" -d '{"street":"123 Demo St","city":"Testville","country":"Testland","zipCode":"12345"}' ) || true
  addr_code=$(parse_http_code "$addr_out")
  echo "$addr_out"
fi

# Add a product to cart (demo user)
PRODUCT_ID=$(echo "$prod" | jq -r '.[0].id')
if [ -n "$DEMO_TOKEN" ]; then
  echo -e "\n=== ADD TO CART ==="
  add_out=$(curl -sS -w "\nHTTP_CODE:%{http_code}\n" -X POST "$BASE_URL/cart" -H "Content-Type: application/json" -H "Authorization: Bearer $DEMO_TOKEN" -d "{\"productId\":$PRODUCT_ID,\"quantity\":1}" ) || true
  add_code=$(parse_http_code "$add_out")
  echo "$add_out"
fi

# Checkout (demo user)
if [ -n "$DEMO_TOKEN" ]; then
  echo -e "\n=== CHECKOUT ==="
  # Find a shipping address id (try the first address)
  addr_id=$(curl -sS -H "Authorization: Bearer $DEMO_TOKEN" "$BASE_URL/addresses" | jq -r '.[0].id' || echo null)
  if [ "$addr_id" = "null" ] || [ -z "$addr_id" ]; then
    echo "No address found for demo user; skipping checkout"
  else
    co_out=$(curl -sS -w "\nHTTP_CODE:%{http_code}\n" -X POST "$BASE_URL/orders/checkout" -H "Content-Type: application/json" -H "Authorization: Bearer $DEMO_TOKEN" -d "{\"shippingAddressId\":$addr_id}" ) || true
    co_code=$(parse_http_code "$co_out")
    echo "$co_out" | jq . || echo "$co_out"
  fi
fi

# Verify orders appear
if [ -n "$DEMO_TOKEN" ]; then
  echo -e "\n=== VERIFY ORDERS ==="
  curl -sS -H "Authorization: Bearer $DEMO_TOKEN" "$BASE_URL/orders" | jq . || true
fi

# Admin orders (requires admin)
ADMIN_TOKEN=$(echo "$admin_out" | sed -n 's/.*{"token":"\([^"]*\)".*/\1/p')
if [ -n "$ADMIN_TOKEN" ]; then
  echo -e "\n=== ADMIN ORDERS (requires admin) ==="
  curl -sS -H "Authorization: Bearer $ADMIN_TOKEN" "$BASE_URL/orders/all" | jq . || true
fi

echo -e "\nFull smoke tests completed."