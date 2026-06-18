#!/usr/bin/env bash
set -Eeuo pipefail

# Black-box API regression test for the Stella API.
# Drives a live instance (local or remote) over HTTP only — no backend classes imported.
# Configure via environment variables (see docs/testing.md).

BASE_URL="${STELLA_API_BASE_URL:-http://localhost:8080}"
API_PREFIX="${STELLA_API_PREFIX:-/api/v0}"
USERNAME="${STELLA_API_USERNAME:-}"
PASSWORD="${STELLA_API_PASSWORD:-}"
TOKEN="${STELLA_API_TOKEN:-}"
RUN_SEMANTIC_SEARCH="${STELLA_RUN_SEMANTIC_SEARCH:-true}"
RUN_REINDEX="${STELLA_RUN_REINDEX:-false}"

ITEM_ID=""
CATEGORY_ID=""
LOCATION_ID=""
INSTANCE_ID=""
PERSON_ID=""
TEST_SUFFIX="$(date +%Y%m%d%H%M%S)-$$"
ITEM_NAME="Stella black-box test ${TEST_SUFFIX}"
ITEM_DESCRIPTION="Temporary item created by scripts/api-blackbox-test.sh"
CATEGORY_NAME="Test Category ${TEST_SUFFIX}"
LOCATION_NAME="Test Location ${TEST_SUFFIX}"
PERSON_CPF="$(python3 -c 'import random; d=random.sample(range(10),9); rest=lambda s,w: (lambda r: 0 if r<2 else 11-r)(sum(a*b for a,b in zip(s,w))%11); v1=rest(d,[10,9,8,7,6,5,4,3,2]); v2=rest(d+[v1],[11,10,9,8,7,6,5,4,3,2]); print("".join(map(str,d+[v1,v2])))')"

TOTAL=0
PASSED=0

log() {
  printf '[stella-api-blackbox] %s\n' "$*"
}

fail() {
  printf '[stella-api-blackbox] ERROR: %s\n' "$*" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "required command not found: $1"
}

join_url() {
  local base="${1%/}"
  local path="/${2#/}"
  printf '%s%s' "$base" "$path"
}

urlencode() {
  python3 -c 'import sys, urllib.parse; print(urllib.parse.quote(sys.argv[1], safe=""))' "$1"
}

json_object() {
  python3 -c 'import json, sys; print(json.dumps(dict(zip(sys.argv[1::2], sys.argv[2::2]))))' "$@"
}

json_get_field() {
  python3 -c 'import json, sys; value = json.load(sys.stdin).get(sys.argv[1]); sys.exit(1) if value is None else print(value)' "$1"
}

json_assert_field() {
  local json="$1" field="$2" expected="$3"
  python3 -c 'import json, sys; data = json.load(sys.stdin); actual = str(data.get(sys.argv[1], "")); sys.exit(0 if actual == sys.argv[2] else 1)' "$field" "$expected" <<<"$json"
}

assert_json_array_contains_id() {
  local json="$1"
  local id="$2"
  python3 -c 'import json, sys; data = json.load(sys.stdin); expected = sys.argv[1]; sys.exit(0 if any(item.get("id") == expected or item.get("mainItemId") == expected for item in data) else 1)' "$id" <<<"$json"
}

assert_json_array_not_empty() {
  python3 -c 'import json, sys; data = json.load(sys.stdin); sys.exit(0 if isinstance(data, list) and len(data) >= 0 else 1)' <<<"$1"
}

scenario() {
  TOTAL=$((TOTAL + 1))
  log "[$TOTAL] $*"
}

ok() {
  PASSED=$((PASSED + 1))
}

request() {
  local method="$1"
  local path="$2"
  local expected_status="$3"
  local body="${4:-}"
  local url
  local response_file
  local status
  response_file="$(mktemp)"
  url="$(join_url "$BASE_URL" "$path")"

  local curl_args=(-sS -X "$method" -H "Accept: application/json" -o "$response_file" -w "%{http_code}")
  if [[ -n "$TOKEN" ]]; then
    curl_args+=(-H "Authorization: Bearer $TOKEN")
  fi
  if [[ -n "$body" ]]; then
    curl_args+=(-H "Content-Type: application/json" --data "$body")
  fi
  curl_args+=("$url")

  status="$(curl "${curl_args[@]}")" || {
    rm -f "$response_file"
    fail "connection failure on ${method} ${url}"
  }

  if [[ "$status" != "$expected_status" ]]; then
    printf '[stella-api-blackbox] unexpected response on %s %s: HTTP %s, expected %s\n' "$method" "$url" "$status" "$expected_status" >&2
    printf '[stella-api-blackbox] response body:\n' >&2
    cat "$response_file" >&2
    printf '\n' >&2
    rm -f "$response_file"
    exit 1
  fi

  cat "$response_file"
  rm -f "$response_file"
}

cleanup() {
  log "cleaning up resources created by the test..."
  local base_url token_header
  base_url="${BASE_URL%/}"
  token_header="Authorization: Bearer $TOKEN"
  local delete_status

  for resource_path in \
    "${API_PREFIX}/instances-item/${INSTANCE_ID}" \
    "${API_PREFIX}/main-items/${ITEM_ID}" \
    "${API_PREFIX}/categories/${CATEGORY_ID}" \
    "${API_PREFIX}/locations/${LOCATION_ID}" \
    "${API_PREFIX}/people/${PERSON_ID}"; do
    local id="${resource_path##*/}"
    if [[ -n "$id" ]]; then
      delete_status="$(curl -sS -X DELETE -H "$token_header" -o /dev/null -w "%{http_code}" "${base_url}${resource_path}" || true)"
      if [[ "$delete_status" != "204" && "$delete_status" != "404" && "$delete_status" != "409" ]]; then
        printf '[stella-api-blackbox] WARNING: could not remove %s (HTTP %s)\n' "$resource_path" "$delete_status" >&2
      fi
    fi
  done
}

authenticate() {
  if [[ -n "$TOKEN" ]]; then
    log "using token provided via STELLA_API_TOKEN"
    return
  fi

  if [[ -z "$USERNAME" || -z "$PASSWORD" ]]; then
    fail "provide STELLA_API_TOKEN or STELLA_API_USERNAME/STELLA_API_PASSWORD for authenticated endpoints"
  fi

  local payload response
  payload="$(json_object username "$USERNAME" password "$PASSWORD")"
  response="$(request "POST" "/api/public/login" "200" "$payload")"
  TOKEN="$(json_get_field accessToken <<<"$response")" || fail "login did not return accessToken"
}

main() {
  require_command curl
  require_command python3

  BASE_URL="${BASE_URL%/}"
  API_PREFIX="/${API_PREFIX#/}"
  API_PREFIX="${API_PREFIX%/}"

  trap cleanup EXIT

  # ── Infrastructure ────────────────────────────────────────────────────────────

  scenario "health check returns UP"
  request "GET" "/actuator/health" "200" >/dev/null
  ok

  # ── Authentication ─────────────────────────────────────────────────────────────

  scenario "login with valid credentials returns accessToken"
  authenticate
  ok

  scenario "protected endpoint without token returns 401"
  local saved_token="$TOKEN"
  TOKEN=""
  request "GET" "${API_PREFIX}/main-items" "401" >/dev/null
  TOKEN="$saved_token"
  ok

  scenario "login with wrong password returns 401"
  local bad_payload
  bad_payload="$(json_object username "$USERNAME" password "wrong-password-blackbox")"
  request "POST" "/api/public/login" "401" "$bad_payload" >/dev/null
  ok

  scenario "GET /api/v0/users/me returns the authenticated user"
  local me
  me="$(request "GET" "${API_PREFIX}/users/me" "200")"
  json_get_field username <<<"$me" >/dev/null || fail "me did not return username"
  ok

  # ── Main item ────────────────────────────────────────────────────────────────

  scenario "main item creation returns 201 with id"
  local create_payload created
  create_payload="$(python3 -c 'import json, sys; print(json.dumps({"name": sys.argv[1], "description": sys.argv[2], "notes": "Created by black-box test", "registrationOrigin": "BLACK_BOX_TEST", "active": True}))' "$ITEM_NAME" "$ITEM_DESCRIPTION")"
  created="$(request "POST" "${API_PREFIX}/main-items" "201" "$create_payload")"
  ITEM_ID="$(json_get_field id <<<"$created")" || fail "main item creation did not return id"
  ok

  scenario "main item lookup by id returns correct data"
  local by_id
  by_id="$(request "GET" "${API_PREFIX}/main-items/${ITEM_ID}" "200")"
  python3 -c 'import json, sys; d = json.load(sys.stdin); sys.exit(0 if d.get("id") == sys.argv[1] and d.get("name") == sys.argv[2] else 1)' "$ITEM_ID" "$ITEM_NAME" <<<"$by_id" || fail "lookup by id did not return the created item"
  ok

  scenario "main item update returns updated data"
  local update_payload updated_name updated
  updated_name="${ITEM_NAME} updated"
  update_payload="$(python3 -c 'import json, sys; print(json.dumps({"name": sys.argv[1], "active": True}))' "$updated_name")"
  updated="$(request "PUT" "${API_PREFIX}/main-items/${ITEM_ID}" "200" "$update_payload")"
  json_assert_field "$updated" name "$updated_name" || fail "update did not reflect the new name"
  ITEM_NAME="$updated_name"
  ok

  scenario "main item listing contains the created item"
  local list
  list="$(request "GET" "${API_PREFIX}/main-items" "200")"
  assert_json_array_contains_id "$list" "$ITEM_ID" || fail "listing did not return the created item"
  ok

  scenario "main item search by name returns the created item"
  local encoded_name search_by_name
  encoded_name="$(urlencode "$ITEM_NAME")"
  search_by_name="$(request "GET" "${API_PREFIX}/main-items/search?name=${encoded_name}" "200")"
  assert_json_array_contains_id "$search_by_name" "$ITEM_ID" || fail "search by name did not return the created item"
  ok

  scenario "main item revision history returns a list"
  local revisions
  revisions="$(request "GET" "${API_PREFIX}/main-items/${ITEM_ID}/revisions" "200")"
  assert_json_array_not_empty "$revisions" || fail "revision history did not return a list"
  ok

  scenario "lookup of a non-existent main item returns 404"
  request "GET" "${API_PREFIX}/main-items/00000000-0000-0000-0000-000000000000" "404" >/dev/null
  ok

  scenario "main item creation without name returns 400"
  local invalid_payload
  invalid_payload='{"description":"no name"}'
  request "POST" "${API_PREFIX}/main-items" "400" "$invalid_payload" >/dev/null
  ok

  # ── Categories ───────────────────────────────────────────────────────────────

  scenario "category creation returns 201 with id"
  local cat_payload cat_created
  cat_payload="$(python3 -c 'import json, sys; print(json.dumps({"name": sys.argv[1], "description": "Category created by black-box test", "icon": "outros", "active": True}))' "$CATEGORY_NAME")"
  cat_created="$(request "POST" "${API_PREFIX}/categories" "201" "$cat_payload")"
  CATEGORY_ID="$(json_get_field id <<<"$cat_created")" || fail "category creation did not return id"
  ok

  scenario "category lookup by id returns correct data"
  local cat_by_id
  cat_by_id="$(request "GET" "${API_PREFIX}/categories/${CATEGORY_ID}" "200")"
  json_assert_field "$cat_by_id" id "$CATEGORY_ID" || fail "category lookup by id did not return expected data"
  ok

  scenario "category listing contains the created category"
  local cat_list
  cat_list="$(request "GET" "${API_PREFIX}/categories" "200")"
  assert_json_array_contains_id "$cat_list" "$CATEGORY_ID" || fail "category listing did not return the created category"
  ok

  scenario "category search by name returns the created category"
  local cat_encoded cat_search
  cat_encoded="$(urlencode "$CATEGORY_NAME")"
  cat_search="$(request "GET" "${API_PREFIX}/categories/search?name=${cat_encoded}" "200")"
  assert_json_array_contains_id "$cat_search" "$CATEGORY_ID" || fail "category search by name did not return the created category"
  ok

  scenario "category update reflects the new name"
  local cat_update_name cat_update_payload cat_updated
  cat_update_name="${CATEGORY_NAME} updated"
  cat_update_payload="$(python3 -c 'import json, sys; print(json.dumps({"name": sys.argv[1], "icon": "livros", "active": True}))' "$cat_update_name")"
  cat_updated="$(request "PUT" "${API_PREFIX}/categories/${CATEGORY_ID}" "200" "$cat_update_payload")"
  json_assert_field "$cat_updated" name "$cat_update_name" || fail "category update did not reflect the new name"
  ok

  scenario "lookup of a non-existent category returns 404"
  request "GET" "${API_PREFIX}/categories/00000000-0000-0000-0000-000000000000" "404" >/dev/null
  ok

  # ── Storage locations ──────────────────────────────────────────────────────────

  scenario "storage location creation returns 201 with id"
  local location_payload location_created
  location_payload="$(python3 -c 'import json, sys; print(json.dumps({"name": sys.argv[1], "description": "Location created by black-box test", "active": True}))' "$LOCATION_NAME")"
  location_created="$(request "POST" "${API_PREFIX}/locations" "201" "$location_payload")"
  LOCATION_ID="$(json_get_field id <<<"$location_created")" || fail "location creation did not return id"
  ok

  scenario "location lookup by id returns correct data"
  local loc_by_id
  loc_by_id="$(request "GET" "${API_PREFIX}/locations/${LOCATION_ID}" "200")"
  json_assert_field "$loc_by_id" id "$LOCATION_ID" || fail "location lookup by id did not return expected data"
  ok

  scenario "location listing contains the created location"
  local loc_list
  loc_list="$(request "GET" "${API_PREFIX}/locations" "200")"
  assert_json_array_contains_id "$loc_list" "$LOCATION_ID" || fail "location listing did not return the created location"
  ok

  scenario "location search by name returns the created location"
  local loc_encoded loc_search
  loc_encoded="$(urlencode "$LOCATION_NAME")"
  loc_search="$(request "GET" "${API_PREFIX}/locations/search?name=${loc_encoded}" "200")"
  assert_json_array_contains_id "$loc_search" "$LOCATION_ID" || fail "location search by name did not return the created location"
  ok

  scenario "location update reflects the new name"
  local loc_update_name loc_update_payload loc_updated
  loc_update_name="${LOCATION_NAME} updated"
  loc_update_payload="$(python3 -c 'import json, sys; print(json.dumps({"name": sys.argv[1], "active": True}))' "$loc_update_name")"
  loc_updated="$(request "PUT" "${API_PREFIX}/locations/${LOCATION_ID}" "200" "$loc_update_payload")"
  json_assert_field "$loc_updated" name "$loc_update_name" || fail "location update did not reflect the new name"
  ok

  # ── Item instances ───────────────────────────────────────────────────────────

  scenario "item instance creation returns 201 with id"
  local inst_payload inst_created
  inst_payload="$(python3 -c 'import json, sys; print(json.dumps({"mainItemId": sys.argv[1], "currentLocationId": sys.argv[2], "identifier": "BLK-'${TEST_SUFFIX}'", "registrationOrigin": "BLACK_BOX_TEST", "active": True}))' "$ITEM_ID" "$LOCATION_ID")"
  inst_created="$(request "POST" "${API_PREFIX}/instances-item" "201" "$inst_payload")"
  INSTANCE_ID="$(json_get_field id <<<"$inst_created")" || fail "instance creation did not return id"
  ok

  scenario "instance lookup by id returns correct data"
  local inst_by_id
  inst_by_id="$(request "GET" "${API_PREFIX}/instances-item/${INSTANCE_ID}" "200")"
  json_assert_field "$inst_by_id" id "$INSTANCE_ID" || fail "instance lookup by id did not return expected data"
  ok

  scenario "instance listing contains the created instance"
  local inst_list
  inst_list="$(request "GET" "${API_PREFIX}/instances-item" "200")"
  assert_json_array_contains_id "$inst_list" "$INSTANCE_ID" || fail "instance listing did not return the created instance"
  ok

  scenario "instance filter by main item returns the created instance"
  local inst_filtered encoded_item_name
  encoded_item_name="$(urlencode "$ITEM_NAME")"
  inst_filtered="$(request "GET" "${API_PREFIX}/instances-item/filter?mainItem=${encoded_item_name}" "200")"
  assert_json_array_contains_id "$inst_filtered" "$INSTANCE_ID" || fail "filter by main item did not return the created instance"
  ok

  scenario "instance history returns the correct structure"
  local inst_hist
  inst_hist="$(request "GET" "${API_PREFIX}/instances-item/${INSTANCE_ID}/history" "200")"
  python3 -c 'import json, sys; d = json.load(sys.stdin); sys.exit(0 if d.get("instance", {}).get("id") == sys.argv[1] and isinstance(d.get("movements"), list) else 1)' "$INSTANCE_ID" <<<"$inst_hist" || fail "instance history did not return the expected structure"
  ok

  # ── People ──────────────────────────────────────────────────────────────────

  scenario "person creation returns 201 with id"
  local person_payload person_created person_name
  person_name="Test Person ${TEST_SUFFIX}"
  person_payload="$(python3 -c 'import json, sys; print(json.dumps({"name": sys.argv[1], "taxId": sys.argv[2], "email": "blackbox-'${TEST_SUFFIX}'@example.local"}))' "$person_name" "$PERSON_CPF")"
  person_created="$(request "POST" "${API_PREFIX}/people" "201" "$person_payload")"
  PERSON_ID="$(json_get_field id <<<"$person_created")" || fail "person creation did not return id"
  ok

  scenario "person lookup by id returns correct data"
  local person_by_id
  person_by_id="$(request "GET" "${API_PREFIX}/people/${PERSON_ID}" "200")"
  json_assert_field "$person_by_id" id "$PERSON_ID" || fail "person lookup by id did not return expected data"
  ok

  scenario "person listing contains the created person"
  local person_list
  person_list="$(request "GET" "${API_PREFIX}/people" "200")"
  assert_json_array_contains_id "$person_list" "$PERSON_ID" || fail "person listing did not return the created person"
  ok

  scenario "person search by name returns the created person"
  local person_encoded person_search
  person_encoded="$(urlencode "$person_name")"
  person_search="$(request "GET" "${API_PREFIX}/people/search?name=${person_encoded}" "200")"
  assert_json_array_contains_id "$person_search" "$PERSON_ID" || fail "person search by name did not return the created person"
  ok

  scenario "person creation with duplicate tax id returns 409"
  local dup_payload
  dup_payload="$(python3 -c 'import json, sys; print(json.dumps({"name": "Another Name", "taxId": sys.argv[1]}))' "$PERSON_CPF")"
  request "POST" "${API_PREFIX}/people" "409" "$dup_payload" >/dev/null
  ok

  # ── Dashboard ────────────────────────────────────────────────────────────────

  scenario "dashboard summary returns totals"
  local dashboard
  dashboard="$(request "GET" "${API_PREFIX}/dashboard/summary" "200")"
  python3 -c 'import json, sys; d = json.load(sys.stdin); sys.exit(0 if "mainItemCount" in d and "instanceCount" in d and "peopleCount" in d else 1)' <<<"$dashboard" || fail "dashboard did not return the expected fields"
  ok

  # ── Error-handling contract ────────────────────────────────────────────────────

  scenario "GET on a POST-only resource returns 405"
  request "GET" "${API_PREFIX}/loans-item" "405" >/dev/null
  ok

  scenario "unknown route returns 404"
  request "GET" "${API_PREFIX}/itens-mestre" "404" >/dev/null
  ok

  # ── Semantic search (optional) ──────────────────────────────────────────────────

  if [[ "$RUN_REINDEX" == "true" ]]; then
    scenario "semantic search reindex returns 200"
    request "POST" "${API_PREFIX}/main-items/semantic-search/reindex" "200" >/dev/null
    ok
  fi

  if [[ "$RUN_SEMANTIC_SEARCH" == "true" ]]; then
    scenario "semantic search returns the created item"
    local semantic_query semantic_result
    semantic_query="$(urlencode "$ITEM_NAME")"
    semantic_result="$(request "GET" "${API_PREFIX}/main-items/semantic-search?query=${semantic_query}" "200")"
    assert_json_array_contains_id "$semantic_result" "$ITEM_ID" || fail "semantic search did not return the created item"
    ok
  fi

  # ── Explicit removal before cleanup ─────────────────────────────────────────────

  scenario "instance removal returns 204"
  request "DELETE" "${API_PREFIX}/instances-item/${INSTANCE_ID}" "204" >/dev/null
  INSTANCE_ID=""
  ok

  scenario "main item removal returns 204"
  request "DELETE" "${API_PREFIX}/main-items/${ITEM_ID}" "204" >/dev/null
  ITEM_ID=""
  ok

  scenario "category removal returns 204"
  request "DELETE" "${API_PREFIX}/categories/${CATEGORY_ID}" "204" >/dev/null
  CATEGORY_ID=""
  ok

  scenario "location removal returns 204"
  request "DELETE" "${API_PREFIX}/locations/${LOCATION_ID}" "204" >/dev/null
  LOCATION_ID=""
  ok

  scenario "person removal returns 204"
  request "DELETE" "${API_PREFIX}/people/${PERSON_ID}" "204" >/dev/null
  PERSON_ID=""
  ok

  log "──────────────────────────────────────────────"
  log "result: ${PASSED}/${TOTAL} scenarios passed"
  if [[ "$PASSED" -ne "$TOTAL" ]]; then
    fail "$((TOTAL - PASSED)) scenarios failed"
  fi
  log "black-box test completed successfully"
}

main "$@"
