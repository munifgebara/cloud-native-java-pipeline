#!/usr/bin/env bash
set -Eeuo pipefail

BASE_URL="${STELLA_API_BASE_URL:-http://localhost:8080}"
API_PREFIX="${STELLA_API_PREFIX:-/api/v0}"
USERNAME="${STELLA_API_USERNAME:-}"
PASSWORD="${STELLA_API_PASSWORD:-}"
TOKEN="${STELLA_API_TOKEN:-}"
RUN_SEMANTIC_SEARCH="${STELLA_RUN_SEMANTIC_SEARCH:-true}"
RUN_REINDEX="${STELLA_RUN_REINDEX:-false}"

ITEM_ID=""
TEST_SUFFIX="$(date +%Y%m%d%H%M%S)-$$"
ITEM_NAME="Teste caixa preta Stella ${TEST_SUFFIX}"
ITEM_DESCRIPTION="Item temporario criado por scripts/api-blackbox-test.sh"

log() {
  printf '[stella-api-blackbox] %s\n' "$*"
}

fail() {
  printf '[stella-api-blackbox] ERRO: %s\n' "$*" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "comando obrigatorio nao encontrado: $1"
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

assert_json_item_created() {
  local json="$1"
  local nome="$2"
  python3 -c 'import json, sys; data = json.load(sys.stdin); sys.exit(0 if data.get("nome") == sys.argv[1] and data.get("ativa") is True else 1)' "$nome" <<<"$json"
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
    fail "falha de conexao em ${method} ${url}"
  }

  if [[ "$status" != "$expected_status" ]]; then
    printf '[stella-api-blackbox] resposta inesperada em %s %s: HTTP %s, esperado %s\n' "$method" "$url" "$status" "$expected_status" >&2
    printf '[stella-api-blackbox] corpo da resposta:\n' >&2
    cat "$response_file" >&2
    printf '\n' >&2
    rm -f "$response_file"
    exit 1
  fi

  cat "$response_file"
  rm -f "$response_file"
}

cleanup() {
  if [[ -n "$ITEM_ID" ]]; then
    log "removendo item mestre criado no teste: ${ITEM_ID}"
    local delete_url
    local delete_status
    delete_url="$(join_url "$BASE_URL" "${API_PREFIX}/itens-mestre/${ITEM_ID}")"
    delete_status="$(curl -sS -X DELETE -H "Authorization: Bearer $TOKEN" -o /dev/null -w "%{http_code}" "$delete_url" || true)"
    if [[ "$delete_status" != "204" && "$delete_status" != "404" ]]; then
      printf '[stella-api-blackbox] AVISO: nao foi possivel remover item mestre %s\n' "$ITEM_ID" >&2
    fi
  fi
}

authenticate() {
  if [[ -n "$TOKEN" ]]; then
    log "usando token informado por STELLA_API_TOKEN"
    return
  fi

  if [[ -z "$USERNAME" || -z "$PASSWORD" ]]; then
    fail "informe STELLA_API_TOKEN ou STELLA_API_USERNAME/STELLA_API_PASSWORD para endpoints autenticados"
  fi

  log "autenticando via /api/public/login"
  local payload
  local response
  payload="$(json_object username "$USERNAME" password "$PASSWORD")"
  response="$(request "POST" "/api/public/login" "200" "$payload")"
  TOKEN="$(json_get_field accessToken <<<"$response")" || fail "login nao retornou accessToken"
}

assert_json_array_contains_id() {
  local json="$1"
  local id="$2"
  python3 -c 'import json, sys; data = json.load(sys.stdin); expected = sys.argv[1]; sys.exit(0 if any(item.get("id") == expected or item.get("itemMestreId") == expected for item in data) else 1)' "$id" <<<"$json"
}

main() {
  require_command curl
  require_command python3

  BASE_URL="${BASE_URL%/}"
  API_PREFIX="/${API_PREFIX#/}"
  API_PREFIX="${API_PREFIX%/}"

  trap cleanup EXIT

  log "testando API em ${BASE_URL}"
  request "GET" "/actuator/health" "200" >/dev/null

  authenticate

  log "criando item mestre temporario"
  local create_payload
  local created
  create_payload="$(python3 -c 'import json, sys; print(json.dumps({"nome": sys.argv[1], "descricao": sys.argv[2], "observacoes": "Criado por teste caixa-preta", "origemCadastro": "BLACK_BOX_TEST", "ativa": True}))' "$ITEM_NAME" "$ITEM_DESCRIPTION")"
  created="$(request "POST" "${API_PREFIX}/itens-mestre" "201" "$create_payload")"
  ITEM_ID="$(json_get_field id <<<"$created")" || fail "criacao de item mestre nao retornou id"
  assert_json_item_created "$created" "$ITEM_NAME" || fail "item criado nao possui os dados esperados"

  log "buscando item mestre por id"
  local by_id
  by_id="$(request "GET" "${API_PREFIX}/itens-mestre/${ITEM_ID}" "200")"
  python3 -c 'import json, sys; data = json.load(sys.stdin); sys.exit(0 if data.get("id") == sys.argv[1] and data.get("nome") == sys.argv[2] else 1)' "$ITEM_ID" "$ITEM_NAME" <<<"$by_id" || fail "busca por id nao retornou o item criado"

  log "validando listagem de itens mestre"
  local list
  list="$(request "GET" "${API_PREFIX}/itens-mestre" "200")"
  assert_json_array_contains_id "$list" "$ITEM_ID" || fail "listagem nao retornou o item criado"

  log "validando busca por nome"
  local encoded_name
  local search_by_name
  encoded_name="$(urlencode "$ITEM_NAME")"
  search_by_name="$(request "GET" "${API_PREFIX}/itens-mestre/buscar?nome=${encoded_name}" "200")"
  assert_json_array_contains_id "$search_by_name" "$ITEM_ID" || fail "busca por nome nao retornou o item criado"

  if [[ "$RUN_REINDEX" == "true" ]]; then
    log "reindexando busca semantica"
    request "POST" "${API_PREFIX}/itens-mestre/busca-semantica/reindexar" "200" >/dev/null
  fi

  if [[ "$RUN_SEMANTIC_SEARCH" == "true" ]]; then
    log "validando busca semantica"
    local semantic_query
    local semantic_result
    semantic_query="$(urlencode "$ITEM_NAME")"
    semantic_result="$(request "GET" "${API_PREFIX}/itens-mestre/busca-semantica?consulta=${semantic_query}" "200")"
    assert_json_array_contains_id "$semantic_result" "$ITEM_ID" || fail "busca semantica nao retornou o item criado"
  else
    log "busca semantica ignorada por STELLA_RUN_SEMANTIC_SEARCH=false"
  fi

  log "removendo item mestre temporario"
  request "DELETE" "${API_PREFIX}/itens-mestre/${ITEM_ID}" "204" >/dev/null
  ITEM_ID=""

  log "teste caixa-preta concluido com sucesso"
}

main "$@"
