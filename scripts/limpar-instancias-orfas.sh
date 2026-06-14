#!/usr/bin/env bash
set -Eeuo pipefail

BASE_URL="${STELLA_API_BASE_URL:-http://localhost:8080}"
USERNAME="${STELLA_API_USERNAME:-}"
PASSWORD="${STELLA_API_PASSWORD:-}"
TOKEN="${STELLA_API_TOKEN:-}"

log() { echo "[limpar-instancias-orfas] $*"; }
fail() { log "ERRO: $*"; exit 1; }

# ── Autenticação ──────────────────────────────────────────────────────────────

if [ -z "$TOKEN" ]; then
  [ -z "$USERNAME" ] && fail "Defina STELLA_API_USERNAME ou STELLA_API_TOKEN."
  [ -z "$PASSWORD" ] && fail "Defina STELLA_API_PASSWORD ou STELLA_API_TOKEN."

  log "Autenticando como '$USERNAME'..."
  login_resp="$(curl -sf -X POST "${BASE_URL}/api/public/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"${USERNAME}\",\"password\":\"${PASSWORD}\"}" 2>/dev/null)" \
    || fail "Falha ao autenticar em ${BASE_URL}/api/public/login"

  TOKEN="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["accessToken"])' <<<"$login_resp")" \
    || fail "Resposta de login nao contem accessToken."
fi

auth_header="Authorization: Bearer ${TOKEN}"

api_get() {
  curl -sf -H "$auth_header" "${BASE_URL}${1}" 2>/dev/null \
    || fail "Falha em GET ${1}"
}

api_delete() {
  curl -sf -o /dev/null -w "%{http_code}" -X DELETE -H "$auth_header" "${BASE_URL}${1}" 2>/dev/null \
    || fail "Falha em DELETE ${1}"
}

# ── Coleta de dados ───────────────────────────────────────────────────────────

log "Buscando instancias ativas..."
instancias_json="$(api_get "/api/v0/instancias-item")"

log "Buscando itens mestre ativos..."
itens_json="$(api_get "/api/v0/itens-mestre")"

# ── Detecção de órfãs ─────────────────────────────────────────────────────────

orfas_json="$(python3 - "$instancias_json" "$itens_json" <<'PYTHON'
import json, sys

instancias = json.loads(sys.argv[1])
itens = json.loads(sys.argv[2])

ids_ativos = {item["id"] for item in itens}

orfas = [i for i in instancias if i["itemMestreId"] not in ids_ativos]
print(json.dumps(orfas))
PYTHON
)"

total_orfas="$(python3 -c 'import json,sys; print(len(json.loads(sys.argv[1])))' "$orfas_json")"

if [ "$total_orfas" -eq 0 ]; then
  log "Nenhuma instancia orfao encontrada. Nada a fazer."
  exit 0
fi

# ── Relatório ─────────────────────────────────────────────────────────────────

log "──────────────────────────────────────────────────────"
log "Instancias orfas encontradas: ${total_orfas}"
log "──────────────────────────────────────────────────────"

python3 - "$orfas_json" <<'PYTHON'
import json, sys

orfas = json.loads(sys.argv[1])
for inst in orfas:
    ident = inst.get("identificador") or inst.get("patrimonio") or inst.get("numeroSerie") or "(sem identificacao)"
    print(f"  ID: {inst['id']}")
    print(f"    Identificacao : {ident}")
    print(f"    Item mestre ID: {inst['itemMestreId']} (NAO ENCONTRADO nos itens ativos)")
    print(f"    Status        : {inst['statusOperacional']}")
    print(f"    Local atual   : {inst.get('localAtualNome') or '-'}")
    print()
PYTHON

log "──────────────────────────────────────────────────────"

# ── Confirmação ───────────────────────────────────────────────────────────────

read -r -p "[limpar-instancias-orfas] Excluir as ${total_orfas} instancias listadas acima? [s/N] " confirmacao
if [[ "${confirmacao,,}" != "s" ]]; then
  log "Operacao cancelada."
  exit 0
fi

# ── Exclusão ──────────────────────────────────────────────────────────────────

excluidas=0
falhas=0

while IFS= read -r inst_id; do
  status="$(api_delete "/api/v0/instancias-item/${inst_id}")"
  if [ "$status" = "204" ]; then
    log "Excluida: ${inst_id}"
    excluidas=$((excluidas + 1))
  else
    log "FALHA ao excluir ${inst_id} (HTTP ${status})"
    falhas=$((falhas + 1))
  fi
done < <(python3 -c 'import json,sys; [print(i["id"]) for i in json.loads(sys.argv[1])]' "$orfas_json")

log "──────────────────────────────────────────────────────"
log "Concluido: ${excluidas} excluidas, ${falhas} falhas."
