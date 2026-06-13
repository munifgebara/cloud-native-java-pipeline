#!/usr/bin/env bash
set -Eeuo pipefail

REASON="${1:-manual}"
ENV_FILE="${STELLA_BACKUP_ENV_FILE:-/etc/stella-backup/backup.env}"

ENV_REQUIRE_UPLOAD_SET="${STELLA_BACKUP_REQUIRE_UPLOAD+x}"
ENV_REQUIRE_UPLOAD_VALUE="${STELLA_BACKUP_REQUIRE_UPLOAD:-}"

if [[ -f "$ENV_FILE" ]]; then
  # shellcheck disable=SC1090
  source "$ENV_FILE"
fi

if [[ -n "$ENV_REQUIRE_UPLOAD_SET" ]]; then
  STELLA_BACKUP_REQUIRE_UPLOAD="$ENV_REQUIRE_UPLOAD_VALUE"
fi

KUBECTL_CMD="${STELLA_KUBECTL_CMD:-sudo k3s kubectl}"
RCLONE_CMD="${STELLA_RCLONE_CMD:-rclone}"
AGE_CMD="${STELLA_AGE_CMD:-age}"
MC_CMD="${STELLA_MC_CMD:-mc}"

read -r -a KUBECTL <<< "$KUBECTL_CMD"
read -r -a RCLONE <<< "$RCLONE_CMD"
read -r -a AGE <<< "$AGE_CMD"
read -r -a MC <<< "$MC_CMD"

BACKUP_ROOT="${STELLA_BACKUP_ROOT:-/var/backups/stella}"
WORK_ROOT="${STELLA_BACKUP_WORK_ROOT:-/tmp/stella-backup}"
NAMESPACES="${STELLA_BACKUP_NAMESPACES:-platform}"
POSTGRES_NAMESPACE="${STELLA_POSTGRES_NAMESPACE:-platform}"
POSTGRES_WORKLOAD="${STELLA_POSTGRES_WORKLOAD:-statefulset/postgres}"
POSTGRES_USER="${STELLA_POSTGRES_USER:-postgres}"
POSTGRES_DATABASES="${STELLA_POSTGRES_DATABASES:-${STELLA_POSTGRES_DB:-stella_dev stella_staging stella_prod keycloak}}"
MINIO_NAMESPACE="${STELLA_MINIO_NAMESPACE:-platform}"
MINIO_SERVICE_URL="${STELLA_MINIO_SERVICE_URL:-http://minio.platform.svc.cluster.local:9000}"
MINIO_SECRET_NAME="${STELLA_MINIO_SECRET_NAME:-minio-secret}"
MINIO_ACCESS_KEY_FIELD="${STELLA_MINIO_ACCESS_KEY_FIELD:-MINIO_ROOT_USER}"
MINIO_SECRET_KEY_FIELD="${STELLA_MINIO_SECRET_KEY_FIELD:-MINIO_ROOT_PASSWORD}"
MINIO_ALIAS="${STELLA_MINIO_ALIAS:-stella-backup}"
RCLONE_REMOTE="${STELLA_BACKUP_RCLONE_REMOTE:-}"
RETENTION_AGE="${STELLA_BACKUP_RETENTION_AGE:-30d}"
REQUIRE_UPLOAD="${STELLA_BACKUP_REQUIRE_UPLOAD:-false}"
ALLOW_PLAINTEXT_SECRETS="${STELLA_BACKUP_ALLOW_PLAINTEXT_SECRETS:-false}"
AGE_RECIPIENT="${STELLA_BACKUP_AGE_RECIPIENT:-}"

RUN_ID="backup-stella-$(date -u +%Y%m%d-%H%M%S)-${REASON}"
BUNDLE_DIR="$WORK_ROOT/$RUN_ID"
ARCHIVE_DIR="$BACKUP_ROOT/archives"
ARCHIVE_PATH="$ARCHIVE_DIR/$RUN_ID.tar.gz"
ENCRYPTED_ARCHIVE_PATH="$ARCHIVE_PATH.age"

log() {
  printf '[%s] %s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$*" >&2
}

fail() {
  log "ERROR: $*"
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "Required command not found: $1"
}

require_tooling() {
  require_command date
  require_command tar
  require_command sha256sum
  require_command base64
  require_command "${KUBECTL[0]}"
  require_command "${MC[0]}"

  if [[ -z "$AGE_RECIPIENT" && "$ALLOW_PLAINTEXT_SECRETS" != "true" ]]; then
    fail "Set STELLA_BACKUP_AGE_RECIPIENT or explicitly set STELLA_BACKUP_ALLOW_PLAINTEXT_SECRETS=true"
  fi

  if [[ -n "$AGE_RECIPIENT" ]]; then
    require_command "${AGE[0]}"
  fi

  if [[ -n "$RCLONE_REMOTE" || "$REQUIRE_UPLOAD" == "true" ]]; then
    require_command "${RCLONE[0]}"
  fi
}

kget_secret_value() {
  local namespace="$1"
  local secret="$2"
  local field="$3"
  "${KUBECTL[@]}" get secret "$secret" -n "$namespace" -o "jsonpath={.data.$field}" | base64 -d
}

namespace_exists() {
  local namespace="$1"
  "${KUBECTL[@]}" get namespace "$namespace" >/dev/null 2>&1
}

write_metadata() {
  local git_commit="unknown"
  if command -v git >/dev/null 2>&1 && git rev-parse --git-dir >/dev/null 2>&1; then
    git_commit="$(git rev-parse HEAD)"
  fi

  cat > "$BUNDLE_DIR/metadata.json" <<EOF_META
{
  "id": "$RUN_ID",
  "reason": "$REASON",
  "created_at_utc": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "git_commit": "$git_commit",
  "namespaces": "$(printf '%s' "$NAMESPACES")",
  "postgres_namespace": "$POSTGRES_NAMESPACE",
  "postgres_workload": "$POSTGRES_WORKLOAD",
  "postgres_databases": "$(printf '%s' "$POSTGRES_DATABASES")",
  "minio_namespace": "$MINIO_NAMESPACE",
  "minio_service_url": "$MINIO_SERVICE_URL"
}
EOF_META
}

backup_postgres() {
  log "Backing up PostgreSQL globals and databases: $POSTGRES_DATABASES"
  mkdir -p "$BUNDLE_DIR/postgres"

  "${KUBECTL[@]}" exec -n "$POSTGRES_NAMESPACE" "$POSTGRES_WORKLOAD" -- \
    pg_dumpall -U "$POSTGRES_USER" --globals-only \
    > "$BUNDLE_DIR/postgres/globals.sql"

  local database
  for database in $POSTGRES_DATABASES; do
    log "Backing up PostgreSQL database $database"
    "${KUBECTL[@]}" exec -n "$POSTGRES_NAMESPACE" "$POSTGRES_WORKLOAD" -- \
      pg_dump -U "$POSTGRES_USER" -d "$database" -Fc \
      > "$BUNDLE_DIR/postgres/$database.dump"
  done
}

backup_minio() {
  log "Backing up MinIO objects"
  mkdir -p "$BUNDLE_DIR/minio"

  local access_key
  local secret_key
  access_key="$(kget_secret_value "$MINIO_NAMESPACE" "$MINIO_SECRET_NAME" "$MINIO_ACCESS_KEY_FIELD")"
  secret_key="$(kget_secret_value "$MINIO_NAMESPACE" "$MINIO_SECRET_NAME" "$MINIO_SECRET_KEY_FIELD")"

  "${MC[@]}" alias set "$MINIO_ALIAS" "$MINIO_SERVICE_URL" "$access_key" "$secret_key" >/dev/null
  "${MC[@]}" mirror --overwrite --remove "$MINIO_ALIAS" "$BUNDLE_DIR/minio"
}

backup_kubernetes() {
  log "Backing up Kubernetes resources"
  mkdir -p "$BUNDLE_DIR/kubernetes"

  "${KUBECTL[@]}" version --client > "$BUNDLE_DIR/kubernetes/kubectl-version.txt" 2>&1 || true
  "${KUBECTL[@]}" get nodes -o yaml > "$BUNDLE_DIR/kubernetes/nodes.yaml"
  "${KUBECTL[@]}" get namespaces -o yaml > "$BUNDLE_DIR/kubernetes/namespaces.yaml"
  "${KUBECTL[@]}" get storageclass,ingressclass,clusterrole,clusterrolebinding -o yaml \
    > "$BUNDLE_DIR/kubernetes/cluster-resources.yaml"

  : > "$BUNDLE_DIR/kubernetes/resources.yaml"
  : > "$BUNDLE_DIR/kubernetes/configmaps.yaml"
  : > "$BUNDLE_DIR/kubernetes/secrets.yaml"

  local namespace
  for namespace in $NAMESPACES; do
    if ! namespace_exists "$namespace"; then
      log "WARNING: namespace $namespace does not exist; skipping"
      continue
    fi

    log "Collecting namespace $namespace"
    {
      echo "---"
      "${KUBECTL[@]}" get namespace "$namespace" -o yaml
      echo "---"
      "${KUBECTL[@]}" get deploy,statefulset,daemonset,service,ingress,pvc,serviceaccount,role,rolebinding,configmap -n "$namespace" -o yaml
    } >> "$BUNDLE_DIR/kubernetes/resources.yaml"

    {
      echo "---"
      "${KUBECTL[@]}" get configmap -n "$namespace" -o yaml
    } >> "$BUNDLE_DIR/kubernetes/configmaps.yaml"

    {
      echo "---"
      "${KUBECTL[@]}" get secret -n "$namespace" -o yaml
    } >> "$BUNDLE_DIR/kubernetes/secrets.yaml"
  done

  if [[ -n "$AGE_RECIPIENT" ]]; then
    "${AGE[@]}" -r "$AGE_RECIPIENT" \
      -o "$BUNDLE_DIR/kubernetes/secrets.enc.yaml" \
      "$BUNDLE_DIR/kubernetes/secrets.yaml"
    rm -f "$BUNDLE_DIR/kubernetes/secrets.yaml"
  elif [[ "$ALLOW_PLAINTEXT_SECRETS" == "true" ]]; then
    log "WARNING: secrets are stored in plaintext because STELLA_BACKUP_ALLOW_PLAINTEXT_SECRETS=true"
  fi
}

write_checksums() {
  log "Writing checksums"
  (
    cd "$BUNDLE_DIR"
    find . -type f ! -name checksums.sha256 -print0 | sort -z | xargs -0 sha256sum > checksums.sha256
  )
}

archive_bundle() {
  log "Creating archive $ARCHIVE_PATH"
  mkdir -p "$ARCHIVE_DIR"
  tar -C "$WORK_ROOT" -czf "$ARCHIVE_PATH" "$RUN_ID"

  if [[ -n "$AGE_RECIPIENT" ]]; then
    log "Encrypting archive $ENCRYPTED_ARCHIVE_PATH"
    "${AGE[@]}" -r "$AGE_RECIPIENT" -o "$ENCRYPTED_ARCHIVE_PATH" "$ARCHIVE_PATH"
    rm -f "$ARCHIVE_PATH"
    ARCHIVE_PATH="$ENCRYPTED_ARCHIVE_PATH"
  fi
}

upload_archive() {
  if [[ -z "$RCLONE_REMOTE" ]]; then
    if [[ "$REQUIRE_UPLOAD" == "true" ]]; then
      fail "STELLA_BACKUP_REQUIRE_UPLOAD=true but STELLA_BACKUP_RCLONE_REMOTE is empty"
    fi
    log "No STELLA_BACKUP_RCLONE_REMOTE configured; archive kept locally at $ARCHIVE_PATH"
    return
  fi

  log "Uploading archive to $RCLONE_REMOTE"
  "${RCLONE[@]}" copy "$ARCHIVE_PATH" "$RCLONE_REMOTE"

  if [[ -n "$RETENTION_AGE" ]]; then
    log "Applying remote retention: min age $RETENTION_AGE"
    "${RCLONE[@]}" delete "$RCLONE_REMOTE" --min-age "$RETENTION_AGE" || true
    "${RCLONE[@]}" rmdirs "$RCLONE_REMOTE" || true
  fi
}

cleanup() {
  if [[ "${STELLA_BACKUP_KEEP_WORKDIR:-false}" != "true" ]]; then
    rm -rf "$BUNDLE_DIR"
  fi
}

main() {
  require_tooling
  mkdir -p "$BUNDLE_DIR"
  trap cleanup EXIT

  write_metadata
  backup_postgres
  backup_minio
  backup_kubernetes
  write_checksums
  archive_bundle
  upload_archive

  log "Backup completed: $RUN_ID"
  log "Archive: $ARCHIVE_PATH"
}

main "$@"
