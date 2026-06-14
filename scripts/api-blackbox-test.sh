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
CATEGORIA_ID=""
LOCAL_ID=""
INSTANCIA_ID=""
PESSOA_ID=""
TEST_SUFFIX="$(date +%Y%m%d%H%M%S)-$$"
ITEM_NAME="Teste caixa preta Stella ${TEST_SUFFIX}"
ITEM_DESCRIPTION="Item temporario criado por scripts/api-blackbox-test.sh"
CATEGORIA_NAME="Categoria Teste ${TEST_SUFFIX}"
LOCAL_NAME="Local Teste ${TEST_SUFFIX}"
PESSOA_CPF="$(python3 -c 'import random; d=random.sample(range(10),9); rest=lambda s,w: (11-(sum(a*b for a,b in zip(s,w))%11))%10; v1=rest(d,[10,9,8,7,6,5,4,3,2]); v2=rest(d+[v1],[11,10,9,8,7,6,5,4,3,2]); print("".join(map(str,d+[v1,v2])))')"

TOTAL=0
PASSED=0

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

json_assert_field() {
  local json="$1" field="$2" expected="$3"
  python3 -c 'import json, sys; data = json.load(sys.stdin); actual = str(data.get(sys.argv[1], "")); sys.exit(0 if actual == sys.argv[2] else 1)' "$field" "$expected" <<<"$json"
}

assert_json_array_contains_id() {
  local json="$1"
  local id="$2"
  python3 -c 'import json, sys; data = json.load(sys.stdin); expected = sys.argv[1]; sys.exit(0 if any(item.get("id") == expected or item.get("itemMestreId") == expected for item in data) else 1)' "$id" <<<"$json"
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
  log "limpando recursos criados no teste..."
  local base_url token_header
  base_url="${BASE_URL%/}"
  token_header="Authorization: Bearer $TOKEN"
  local delete_status

  for resource_path in \
    "${API_PREFIX}/instancias-item/${INSTANCIA_ID}" \
    "${API_PREFIX}/itens-mestre/${ITEM_ID}" \
    "${API_PREFIX}/categorias/${CATEGORIA_ID}" \
    "${API_PREFIX}/locais/${LOCAL_ID}" \
    "${API_PREFIX}/pessoas/${PESSOA_ID}"; do
    local id="${resource_path##*/}"
    if [[ -n "$id" ]]; then
      delete_status="$(curl -sS -X DELETE -H "$token_header" -o /dev/null -w "%{http_code}" "${base_url}${resource_path}" || true)"
      if [[ "$delete_status" != "204" && "$delete_status" != "404" && "$delete_status" != "409" ]]; then
        printf '[stella-api-blackbox] AVISO: nao foi possivel remover %s (HTTP %s)\n' "$resource_path" "$delete_status" >&2
      fi
    fi
  done
}

authenticate() {
  if [[ -n "$TOKEN" ]]; then
    log "usando token informado por STELLA_API_TOKEN"
    return
  fi

  if [[ -z "$USERNAME" || -z "$PASSWORD" ]]; then
    fail "informe STELLA_API_TOKEN ou STELLA_API_USERNAME/STELLA_API_PASSWORD para endpoints autenticados"
  fi

  local payload response
  payload="$(json_object username "$USERNAME" password "$PASSWORD")"
  response="$(request "POST" "/api/public/login" "200" "$payload")"
  TOKEN="$(json_get_field accessToken <<<"$response")" || fail "login nao retornou accessToken"
}

main() {
  require_command curl
  require_command python3

  BASE_URL="${BASE_URL%/}"
  API_PREFIX="/${API_PREFIX#/}"
  API_PREFIX="${API_PREFIX%/}"

  trap cleanup EXIT

  # ── Infraestrutura ──────────────────────────────────────────────────────────

  scenario "health check retorna UP"
  request "GET" "/actuator/health" "200" >/dev/null
  ok

  # ── Autenticação ─────────────────────────────────────────────────────────────

  scenario "login com credenciais validas retorna accessToken"
  authenticate
  ok

  scenario "endpoint protegido sem token retorna 401"
  local saved_token="$TOKEN"
  TOKEN=""
  request "GET" "${API_PREFIX}/itens-mestre" "401" >/dev/null
  TOKEN="$saved_token"
  ok

  scenario "login com senha errada retorna 401"
  local bad_payload
  bad_payload="$(json_object username "$USERNAME" password "senha-errada-blackbox")"
  request "POST" "/api/public/login" "401" "$bad_payload" >/dev/null
  ok

  scenario "GET /api/v0/usuarios/me retorna dados do usuario autenticado"
  local me
  me="$(request "GET" "${API_PREFIX}/usuarios/me" "200")"
  json_get_field username <<<"$me" >/dev/null || fail "me nao retornou username"
  ok

  # ── Item mestre ──────────────────────────────────────────────────────────────

  scenario "criacao de item mestre retorna 201 com id"
  local create_payload created
  create_payload="$(python3 -c 'import json, sys; print(json.dumps({"nome": sys.argv[1], "descricao": sys.argv[2], "observacoes": "Criado por teste caixa-preta", "origemCadastro": "BLACK_BOX_TEST", "ativa": True}))' "$ITEM_NAME" "$ITEM_DESCRIPTION")"
  created="$(request "POST" "${API_PREFIX}/itens-mestre" "201" "$create_payload")"
  ITEM_ID="$(json_get_field id <<<"$created")" || fail "criacao de item mestre nao retornou id"
  ok

  scenario "busca de item mestre por id retorna dados corretos"
  local by_id
  by_id="$(request "GET" "${API_PREFIX}/itens-mestre/${ITEM_ID}" "200")"
  python3 -c 'import json, sys; d = json.load(sys.stdin); sys.exit(0 if d.get("id") == sys.argv[1] and d.get("nome") == sys.argv[2] else 1)' "$ITEM_ID" "$ITEM_NAME" <<<"$by_id" || fail "busca por id nao retornou o item criado"
  ok

  scenario "atualizacao de item mestre retorna dados atualizados"
  local update_payload updated_name updated
  updated_name="${ITEM_NAME} atualizado"
  update_payload="$(python3 -c 'import json, sys; print(json.dumps({"nome": sys.argv[1], "ativa": True}))' "$updated_name")"
  updated="$(request "PUT" "${API_PREFIX}/itens-mestre/${ITEM_ID}" "200" "$update_payload")"
  json_assert_field "$updated" nome "$updated_name" || fail "atualizacao nao refletiu o novo nome"
  ITEM_NAME="$updated_name"
  ok

  scenario "listagem de itens mestre contem o item criado"
  local list
  list="$(request "GET" "${API_PREFIX}/itens-mestre" "200")"
  assert_json_array_contains_id "$list" "$ITEM_ID" || fail "listagem nao retornou o item criado"
  ok

  scenario "busca de item mestre por nome retorna o item criado"
  local encoded_name search_by_name
  encoded_name="$(urlencode "$ITEM_NAME")"
  search_by_name="$(request "GET" "${API_PREFIX}/itens-mestre/buscar?nome=${encoded_name}" "200")"
  assert_json_array_contains_id "$search_by_name" "$ITEM_ID" || fail "busca por nome nao retornou o item criado"
  ok

  scenario "historico de revisoes do item mestre retorna lista"
  local revisoes
  revisoes="$(request "GET" "${API_PREFIX}/itens-mestre/${ITEM_ID}/revisoes" "200")"
  assert_json_array_not_empty "$revisoes" || fail "historico de revisoes nao retornou lista"
  ok

  scenario "busca de item mestre inexistente retorna 404"
  request "GET" "${API_PREFIX}/itens-mestre/00000000-0000-0000-0000-000000000000" "404" >/dev/null
  ok

  scenario "criacao de item mestre sem nome retorna 400"
  local invalid_payload
  invalid_payload='{"descricao":"sem nome"}'
  request "POST" "${API_PREFIX}/itens-mestre" "400" "$invalid_payload" >/dev/null
  ok

  # ── Categorias ───────────────────────────────────────────────────────────────

  scenario "criacao de categoria retorna 201 com id"
  local cat_payload cat_created
  cat_payload="$(python3 -c 'import json, sys; print(json.dumps({"nome": sys.argv[1], "descricao": "Categoria criada por teste caixa-preta", "icone": "category", "ativa": True}))' "$CATEGORIA_NAME")"
  cat_created="$(request "POST" "${API_PREFIX}/categorias" "201" "$cat_payload")"
  CATEGORIA_ID="$(json_get_field id <<<"$cat_created")" || fail "criacao de categoria nao retornou id"
  ok

  scenario "busca de categoria por id retorna dados corretos"
  local cat_by_id
  cat_by_id="$(request "GET" "${API_PREFIX}/categorias/${CATEGORIA_ID}" "200")"
  json_assert_field "$cat_by_id" id "$CATEGORIA_ID" || fail "busca de categoria por id nao retornou dado esperado"
  ok

  scenario "listagem de categorias contem a categoria criada"
  local cat_list
  cat_list="$(request "GET" "${API_PREFIX}/categorias" "200")"
  assert_json_array_contains_id "$cat_list" "$CATEGORIA_ID" || fail "listagem de categorias nao retornou a categoria criada"
  ok

  scenario "busca de categoria por nome retorna a categoria criada"
  local cat_encoded cat_search
  cat_encoded="$(urlencode "$CATEGORIA_NAME")"
  cat_search="$(request "GET" "${API_PREFIX}/categorias/buscar?nome=${cat_encoded}" "200")"
  assert_json_array_contains_id "$cat_search" "$CATEGORIA_ID" || fail "busca de categoria por nome nao retornou a categoria criada"
  ok

  scenario "atualizacao de categoria reflete novo nome"
  local cat_update_name cat_update_payload cat_updated
  cat_update_name="${CATEGORIA_NAME} atualizada"
  cat_update_payload="$(python3 -c 'import json, sys; print(json.dumps({"nome": sys.argv[1], "ativa": True}))' "$cat_update_name")"
  cat_updated="$(request "PUT" "${API_PREFIX}/categorias/${CATEGORIA_ID}" "200" "$cat_update_payload")"
  json_assert_field "$cat_updated" nome "$cat_update_name" || fail "atualizacao de categoria nao refletiu o novo nome"
  ok

  scenario "busca de categoria inexistente retorna 404"
  request "GET" "${API_PREFIX}/categorias/00000000-0000-0000-0000-000000000000" "404" >/dev/null
  ok

  # ── Locais de armazenamento ──────────────────────────────────────────────────

  scenario "criacao de local de armazenamento retorna 201 com id"
  local local_payload local_created
  local_payload="$(python3 -c 'import json, sys; print(json.dumps({"nome": sys.argv[1], "descricao": "Local criado por teste caixa-preta", "ativa": True}))' "$LOCAL_NAME")"
  local_created="$(request "POST" "${API_PREFIX}/locais" "201" "$local_payload")"
  LOCAL_ID="$(json_get_field id <<<"$local_created")" || fail "criacao de local nao retornou id"
  ok

  scenario "busca de local por id retorna dados corretos"
  local loc_by_id
  loc_by_id="$(request "GET" "${API_PREFIX}/locais/${LOCAL_ID}" "200")"
  json_assert_field "$loc_by_id" id "$LOCAL_ID" || fail "busca de local por id nao retornou dado esperado"
  ok

  scenario "listagem de locais contem o local criado"
  local loc_list
  loc_list="$(request "GET" "${API_PREFIX}/locais" "200")"
  assert_json_array_contains_id "$loc_list" "$LOCAL_ID" || fail "listagem de locais nao retornou o local criado"
  ok

  scenario "busca de local por nome retorna o local criado"
  local loc_encoded loc_search
  loc_encoded="$(urlencode "$LOCAL_NAME")"
  loc_search="$(request "GET" "${API_PREFIX}/locais/buscar?nome=${loc_encoded}" "200")"
  assert_json_array_contains_id "$loc_search" "$LOCAL_ID" || fail "busca de local por nome nao retornou o local criado"
  ok

  scenario "atualizacao de local reflete novo nome"
  local loc_update_name loc_update_payload loc_updated
  loc_update_name="${LOCAL_NAME} atualizado"
  loc_update_payload="$(python3 -c 'import json, sys; print(json.dumps({"nome": sys.argv[1], "ativa": True}))' "$loc_update_name")"
  loc_updated="$(request "PUT" "${API_PREFIX}/locais/${LOCAL_ID}" "200" "$loc_update_payload")"
  json_assert_field "$loc_updated" nome "$loc_update_name" || fail "atualizacao de local nao refletiu o novo nome"
  ok

  # ── Instâncias de item ───────────────────────────────────────────────────────

  scenario "criacao de instancia de item retorna 201 com id"
  local inst_payload inst_created
  inst_payload="$(python3 -c 'import json, sys; print(json.dumps({"itemMestreId": sys.argv[1], "localAtualId": sys.argv[2], "identificador": "BLK-'${TEST_SUFFIX}'", "origemCadastro": "BLACK_BOX_TEST", "ativa": True}))' "$ITEM_ID" "$LOCAL_ID")"
  inst_created="$(request "POST" "${API_PREFIX}/instancias-item" "201" "$inst_payload")"
  INSTANCIA_ID="$(json_get_field id <<<"$inst_created")" || fail "criacao de instancia nao retornou id"
  ok

  scenario "busca de instancia por id retorna dados corretos"
  local inst_by_id
  inst_by_id="$(request "GET" "${API_PREFIX}/instancias-item/${INSTANCIA_ID}" "200")"
  json_assert_field "$inst_by_id" id "$INSTANCIA_ID" || fail "busca de instancia por id nao retornou dado esperado"
  ok

  scenario "listagem de instancias contem a instancia criada"
  local inst_list
  inst_list="$(request "GET" "${API_PREFIX}/instancias-item" "200")"
  assert_json_array_contains_id "$inst_list" "$INSTANCIA_ID" || fail "listagem de instancias nao retornou a instancia criada"
  ok

  scenario "filtro de instancias por item mestre retorna a instancia criada"
  local inst_filtered
  inst_filtered="$(request "GET" "${API_PREFIX}/instancias-item/filtrar?itemMestreId=${ITEM_ID}" "200")"
  assert_json_array_contains_id "$inst_filtered" "$INSTANCIA_ID" || fail "filtro por item mestre nao retornou a instancia criada"
  ok

  scenario "historico de instancia retorna lista de movimentacoes"
  local inst_hist
  inst_hist="$(request "GET" "${API_PREFIX}/instancias-item/${INSTANCIA_ID}/historico" "200")"
  assert_json_array_not_empty "$inst_hist" || fail "historico de instancia nao retornou lista"
  ok

  # ── Pessoas ──────────────────────────────────────────────────────────────────

  scenario "criacao de pessoa retorna 201 com id"
  local pessoa_payload pessoa_created pessoa_nome
  pessoa_nome="Pessoa Teste ${TEST_SUFFIX}"
  pessoa_payload="$(python3 -c 'import json, sys; print(json.dumps({"nome": sys.argv[1], "cpfCnpj": sys.argv[2], "email": "blackbox-'${TEST_SUFFIX}'@example.local"}))' "$pessoa_nome" "$PESSOA_CPF")"
  pessoa_created="$(request "POST" "${API_PREFIX}/pessoas" "201" "$pessoa_payload")"
  PESSOA_ID="$(json_get_field id <<<"$pessoa_created")" || fail "criacao de pessoa nao retornou id"
  ok

  scenario "busca de pessoa por id retorna dados corretos"
  local pessoa_by_id
  pessoa_by_id="$(request "GET" "${API_PREFIX}/pessoas/${PESSOA_ID}" "200")"
  json_assert_field "$pessoa_by_id" id "$PESSOA_ID" || fail "busca de pessoa por id nao retornou dado esperado"
  ok

  scenario "listagem de pessoas contem a pessoa criada"
  local pessoa_list
  pessoa_list="$(request "GET" "${API_PREFIX}/pessoas" "200")"
  assert_json_array_contains_id "$pessoa_list" "$PESSOA_ID" || fail "listagem de pessoas nao retornou a pessoa criada"
  ok

  scenario "busca de pessoa por nome retorna a pessoa criada"
  local pessoa_encoded pessoa_search
  pessoa_encoded="$(urlencode "$pessoa_nome")"
  pessoa_search="$(request "GET" "${API_PREFIX}/pessoas/buscar?nome=${pessoa_encoded}" "200")"
  assert_json_array_contains_id "$pessoa_search" "$PESSOA_ID" || fail "busca de pessoa por nome nao retornou a pessoa criada"
  ok

  scenario "criacao de pessoa com CPF duplicado retorna 409"
  local dup_payload
  dup_payload="$(python3 -c 'import json, sys; print(json.dumps({"nome": "Outro Nome", "cpfCnpj": sys.argv[1]}))' "$PESSOA_CPF")"
  request "POST" "${API_PREFIX}/pessoas" "409" "$dup_payload" >/dev/null
  ok

  # ── Dashboard ────────────────────────────────────────────────────────────────

  scenario "dashboard resumo retorna totais"
  local dashboard
  dashboard="$(request "GET" "${API_PREFIX}/dashboard/resumo" "200")"
  json_get_field totalItens <<<"$dashboard" >/dev/null || fail "dashboard nao retornou totalItens"
  ok

  # ── Busca semântica (opcional) ────────────────────────────────────────────────

  if [[ "$RUN_REINDEX" == "true" ]]; then
    scenario "reindexacao da busca semantica retorna 200"
    request "POST" "${API_PREFIX}/itens-mestre/busca-semantica/reindexar" "200" >/dev/null
    ok
  fi

  if [[ "$RUN_SEMANTIC_SEARCH" == "true" ]]; then
    scenario "busca semantica retorna o item criado"
    local semantic_query semantic_result
    semantic_query="$(urlencode "$ITEM_NAME")"
    semantic_result="$(request "GET" "${API_PREFIX}/itens-mestre/busca-semantica?consulta=${semantic_query}" "200")"
    assert_json_array_contains_id "$semantic_result" "$ITEM_ID" || fail "busca semantica nao retornou o item criado"
    ok
  fi

  # ── Remoção explícita antes do cleanup ────────────────────────────────────────

  scenario "remocao de instancia retorna 204"
  request "DELETE" "${API_PREFIX}/instancias-item/${INSTANCIA_ID}" "204" >/dev/null
  INSTANCIA_ID=""
  ok

  scenario "remocao de item mestre retorna 204"
  request "DELETE" "${API_PREFIX}/itens-mestre/${ITEM_ID}" "204" >/dev/null
  ITEM_ID=""
  ok

  scenario "remocao de categoria retorna 204"
  request "DELETE" "${API_PREFIX}/categorias/${CATEGORIA_ID}" "204" >/dev/null
  CATEGORIA_ID=""
  ok

  scenario "remocao de local retorna 204"
  request "DELETE" "${API_PREFIX}/locais/${LOCAL_ID}" "204" >/dev/null
  LOCAL_ID=""
  ok

  scenario "remocao de pessoa retorna 204"
  request "DELETE" "${API_PREFIX}/pessoas/${PESSOA_ID}" "204" >/dev/null
  PESSOA_ID=""
  ok

  log "──────────────────────────────────────────────"
  log "resultado: ${PASSED}/${TOTAL} cenarios passaram"
  if [[ "$PASSED" -ne "$TOTAL" ]]; then
    fail "${TOTAL - PASSED} cenarios falharam"
  fi
  log "teste caixa-preta concluido com sucesso"
}

main "$@"
