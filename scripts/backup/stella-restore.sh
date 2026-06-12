#!/usr/bin/env bash
set -Eeuo pipefail

MODE="${1:-}"
ARCHIVE="${2:-}"
ENV_FILE="${STELLA_BACKUP_ENV_FILE:-/etc/stella-backup/backup.env}"

if [[ -f "$ENV_FILE" ]]; then
  # shellcheck disable=SC1090
  source "$ENV_FILE"
fi

KUBECTL_CMD="${STELLA_KUBECTL_CMD:-sudo k3s kubectl}"
AGE_CMD="${STELLA_AGE_CMD:-age}"
MC_CMD="${STELLA_MC_CMD:-mc}"

read -r -a KUBECTL <<< "$KUBECTL_CMD"
read -r -a AGE <<< "$AGE_CMD"
read -r -a MC <<< "$MC_CMD"

RESTORE_ROOT="${STELLA_RESTORE_WORK_ROOT:-/tmp/stella-restore}"
EXTRACT_DIR="$RESTORE_ROOT/restore-$(date -u +%Y%m%d-%H%M%S)"
AGE_IDENTITY="${STELLA_BACKUP_AGE_IDENTITY:-/etc/stella-backup/age-key.txt}"
POSTGRES_NAMESPACE="${STELLA_POSTGRES_NAMESPACE:-platform}"
POSTGRES_WORKLOAD="${STELLA_POSTGRES_WORKLOAD:-statefulset/postgres}"
POSTGRES_USER="${STELLA_POSTGRES_USER:-postgres}"
POSTGRES_DATABASES="${STELLA_POSTGRES_DATABASES:-${STELLA_POSTGRES_DB:-stella_dev keycloak}}"
MINIO_NAMESPACE="${STELLA_MINIO_NAMESPACE:-platform}"
MINIO_SERVICE_URL="${STELLA_MINIO_SERVICE_URL:-http://minio.platform.svc.cluster.local:9000}"
MINIO_SECRET_NAME="${STELLA_MINIO_SECRET_NAME:-minio-secret}"
MINIO_ACCESS_KEY_FIELD="${STELLA_MINIO_ACCESS_KEY_FIELD:-MINIO_ROOT_USER}"
MINIO_SECRET_KEY_FIELD="${STELLA_MINIO_SECRET_KEY_FIELD:-MINIO_ROOT_PASSWORD}"
MINIO_ALIAS="${STELLA_MINIO_ALIAS:-stella-restore}"
ASSUME_YES="${STELLA_RESTORE_ASSUME_YES:-false}"

log() {
  printf '[%s] %s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$*" >&2
}

fail() {
  log "ERROR: $*"
  exit 1
}

usage() {
  cat <<EOF_USAGE
Usage: $0 <full|postgres|minio|kubernetes> <backup.tar.gz|backup.tar.gz.age>

Set STELLA_RESTORE_ASSUME_YES=true to skip confirmation prompts.
EOF_USAGE
}

confirm() {
  local message="$1"
  if [[ "$ASSUME_YES" == "true" ]]; then
    return
  fi
  printf '%s Type "yes" to continue: ' "$message"
  read -r answer
  [[ "$answer" == "yes" ]] || fail "Restore cancelled"
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "Required command not found: $1"
}

kget_secret_value() {
  local namespace="$1"
  local secret="$2"
  local field="$3"
  "${KUBECTL[@]}" get secret "$secret" -n "$namespace" -o "jsonpath={.data.$field}" | base64 -d
}

extract_archive() {
  [[ -n "$ARCHIVE" && -f "$ARCHIVE" ]] || {
    usage
    fail "Backup archive not found"
  }

  require_command tar
  require_command sha256sum
  require_command "${KUBECTL[0]}"
  mkdir -p "$EXTRACT_DIR"

  local archive_to_extract="$ARCHIVE"
  if [[ "$ARCHIVE" == *.age ]]; then
    require_command "${AGE[0]}"
    [[ -f "$AGE_IDENTITY" ]] || fail "Age identity file not found: $AGE_IDENTITY"
    archive_to_extract="$EXTRACT_DIR/$(basename "${ARCHIVE%.age}")"
    log "Decrypting archive"
    "${AGE[@]}" -d -i "$AGE_IDENTITY" -o "$archive_to_extract" "$ARCHIVE"
  fi

  log "Extracting archive"
  tar -C "$EXTRACT_DIR" -xzf "$archive_to_extract"

  local bundle
  bundle="$(find "$EXTRACT_DIR" -maxdepth 1 -type d -name 'backup-stella-*' | sort | tail -n 1)"
  [[ -n "$bundle" ]] || fail "No backup-stella-* bundle found inside archive"
  printf '%s\n' "$bundle"
}

verify_checksums() {
  local bundle="$1"
  if [[ -f "$bundle/checksums.sha256" ]]; then
    log "Verifying checksums"
    (cd "$bundle" && sha256sum -c checksums.sha256)
  else
    log "WARNING: no checksums.sha256 found"
  fi
}

restore_kubernetes() {
  local bundle="$1"
  confirm "This will apply Kubernetes resources from backup."

  if [[ -f "$bundle/kubernetes/resources.yaml" ]]; then
    if [[ -f "$bundle/kubernetes/cluster-resources.yaml" ]]; then
      "${KUBECTL[@]}" apply -f "$bundle/kubernetes/cluster-resources.yaml"
    fi
    "${KUBECTL[@]}" apply -f "$bundle/kubernetes/resources.yaml"
  fi

  if [[ -f "$bundle/kubernetes/secrets.enc.yaml" ]]; then
    require_command "${AGE[0]}"
    [[ -f "$AGE_IDENTITY" ]] || fail "Age identity file not found: $AGE_IDENTITY"
    local secrets_plain="$EXTRACT_DIR/secrets.yaml"
    "${AGE[@]}" -d -i "$AGE_IDENTITY" -o "$secrets_plain" "$bundle/kubernetes/secrets.enc.yaml"
    "${KUBECTL[@]}" apply -f "$secrets_plain"
    rm -f "$secrets_plain"
  elif [[ -f "$bundle/kubernetes/secrets.yaml" ]]; then
    confirm "This backup contains plaintext Kubernetes secrets."
    "${KUBECTL[@]}" apply -f "$bundle/kubernetes/secrets.yaml"
  fi
}

restore_postgres_globals() {
  local bundle="$1"
  local globals="$bundle/postgres/globals.sql"

  if [[ -f "$globals" ]]; then
    log "Restoring PostgreSQL global objects"
    "${KUBECTL[@]}" exec -i -n "$POSTGRES_NAMESPACE" "$POSTGRES_WORKLOAD" -- \
      psql -v ON_ERROR_STOP=0 -U "$POSTGRES_USER" -d postgres < "$globals"
  else
    log "WARNING: no PostgreSQL globals.sql found"
  fi
}

ensure_postgres_database() {
  local database="$1"
  if ! "${KUBECTL[@]}" exec -n "$POSTGRES_NAMESPACE" "$POSTGRES_WORKLOAD" -- \
    psql -U "$POSTGRES_USER" -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname = '$database'" | grep -q 1; then
    log "Creating PostgreSQL database $database"
    "${KUBECTL[@]}" exec -n "$POSTGRES_NAMESPACE" "$POSTGRES_WORKLOAD" -- \
      createdb -U "$POSTGRES_USER" "$database"
  fi
}

restore_postgres_database() {
  local bundle="$1"
  local database="$2"
  local dump="$bundle/postgres/$database.dump"
  [[ -f "$dump" ]] || fail "PostgreSQL dump not found: $dump"

  ensure_postgres_database "$database"
  log "Restoring PostgreSQL database $database"
  "${KUBECTL[@]}" exec -i -n "$POSTGRES_NAMESPACE" "$POSTGRES_WORKLOAD" -- \
    pg_restore -U "$POSTGRES_USER" -d "$database" --clean --if-exists < "$dump"
}

restore_postgres() {
  local bundle="$1"
  confirm "This will restore PostgreSQL databases ($POSTGRES_DATABASES) and may replace existing data."

  restore_postgres_globals "$bundle"

  local database
  for database in $POSTGRES_DATABASES; do
    restore_postgres_database "$bundle" "$database"
  done
}

restore_minio() {
  local bundle="$1"
  [[ -d "$bundle/minio" ]] || fail "MinIO backup directory not found: $bundle/minio"
  require_command "${MC[0]}"

  confirm "This will mirror backup objects into MinIO and may overwrite objects."
  local access_key
  local secret_key
  access_key="$(kget_secret_value "$MINIO_NAMESPACE" "$MINIO_SECRET_NAME" "$MINIO_ACCESS_KEY_FIELD")"
  secret_key="$(kget_secret_value "$MINIO_NAMESPACE" "$MINIO_SECRET_NAME" "$MINIO_SECRET_KEY_FIELD")"

  "${MC[@]}" alias set "$MINIO_ALIAS" "$MINIO_SERVICE_URL" "$access_key" "$secret_key" >/dev/null
  "${MC[@]}" mirror --overwrite "$bundle/minio" "$MINIO_ALIAS"
}

main() {
  case "$MODE" in
    full|postgres|minio|kubernetes) ;;
    *)
      usage
      exit 2
      ;;
  esac

  local bundle
  bundle="$(extract_archive)"
  verify_checksums "$bundle"

  case "$MODE" in
    full)
      restore_kubernetes "$bundle"
      restore_postgres "$bundle"
      restore_minio "$bundle"
      ;;
    postgres)
      restore_postgres "$bundle"
      ;;
    minio)
      restore_minio "$bundle"
      ;;
    kubernetes)
      restore_kubernetes "$bundle"
      ;;
  esac

  log "Restore completed: $MODE"
}

main "$@"
