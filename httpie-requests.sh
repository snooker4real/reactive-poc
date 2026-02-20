#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"

echo "== FruitResource =="
# GET /api/fruits
http GET "$BASE_URL/api/fruits"

# POST /api/fruits (create Citron)
http POST "$BASE_URL/api/fruits" name=Citron

# GET /api/fruits/{id}
# Remplace 1 par un id existant
http GET "$BASE_URL/api/fruits/1"

# DELETE /api/fruits/{id}
# Remplace 1 par un id existant
http DELETE "$BASE_URL/api/fruits/1"

echo "== PriceResource =="
# POST /api/prices/{price}
http POST "$BASE_URL/api/prices/101.25" Content-Type:application/json

# GET /api/prices/last
http GET "$BASE_URL/api/prices/last"

# GET /api/prices/stream (SSE)
http --stream GET "$BASE_URL/api/prices/stream" intervalMs==5 count==50 Accept:text/event-stream
