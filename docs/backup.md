# Backup and Restore

Stella backups are operational host tasks on Gimli. The repository provides scripts and templates, but credentials, private keys and the rclone configuration live only on the server.

## Design

The backup script creates a versioned bundle with independent components:

```text
backup-stella-YYYYmmdd-HHMMSS-reason/
  metadata.json
  postgres/
    globals.sql
    stella_dev.dump
    keycloak.dump
  minio/
    ...
  kubernetes/
    cluster-resources.yaml
    nodes.yaml
    namespaces.yaml
    resources.yaml
    configmaps.yaml
    secrets.enc.yaml
  checksums.sha256
```

By default, PostgreSQL backup includes the Stella application database and the Keycloak database. The database list is configurable with `STELLA_POSTGRES_DATABASES`.

The final archive is encrypted with `age` when `STELLA_BACKUP_AGE_RECIPIENT` is configured, then uploaded with `rclone`. The initial destination is Google Drive, but the scripts only depend on an rclone remote, so the backend can later become S3, NAS or another supported remote.

## Gimli Setup

Install required tools on Gimli:

```bash
sudo apt-get update
sudo apt-get install -y rclone age
```

Install the MinIO client if it is not already present:

```bash
curl -fsSL https://dl.min.io/client/mc/release/linux-amd64/mc -o /tmp/mc
sudo install -m 0755 /tmp/mc /usr/local/bin/mc
```

Configure Google Drive for the user that will run backups. If the GitHub runner/CD step runs the script through `sudo`, configure rclone for root:

```bash
sudo rclone config
sudo rclone mkdir gdrive:StellaBackups
sudo rclone lsd gdrive:
```

Create the age key. Store the private key only on Gimli and in an offline recovery location:

```bash
sudo install -d -m 0700 /etc/stella-backup
sudo age-keygen -o /etc/stella-backup/age-key.txt
sudo chmod 0600 /etc/stella-backup/age-key.txt
sudo grep '^# public key:' /etc/stella-backup/age-key.txt
```

Copy the example environment and set the real public recipient:

```bash
sudo cp scripts/backup/backup.env.example /etc/stella-backup/backup.env
sudo editor /etc/stella-backup/backup.env
```

The default namespace list is intentionally narrow:

```bash
STELLA_BACKUP_NAMESPACES="platform"
```

Add `monitoring` or `logging` only when those namespaces exist and you want them included in the Stella operational backup.

Install the repository checkout used by the systemd timer. One simple approach is to keep a clone under `/opt/stella-backup/current`:

```bash
sudo install -d -m 0755 /opt/stella-backup
sudo git clone https://github.com/munifgebara/cloud-native-java-pipeline.git /opt/stella-backup/current
sudo chmod +x /opt/stella-backup/current/scripts/backup/stella-backup.sh
sudo chmod +x /opt/stella-backup/current/scripts/backup/stella-restore.sh
```

## Manual Backup

Run a full backup:

```bash
sudo /opt/stella-backup/current/scripts/backup/stella-backup.sh manual
```

Check local archives:

```bash
sudo ls -lh /var/backups/stella/archives
```

Check Google Drive:

```bash
sudo rclone ls gdrive:StellaBackups
```

## Daily Schedule

Install the timer:

```bash
sudo cp /opt/stella-backup/current/scripts/backup/systemd/stella-backup.service /etc/systemd/system/
sudo cp /opt/stella-backup/current/scripts/backup/systemd/stella-backup.timer /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now stella-backup.timer
```

Inspect runs:

```bash
systemctl list-timers stella-backup.timer
sudo journalctl -u stella-backup.service -n 200
```

## Pre-CD Backup

The CD workflow runs:

```bash
sudo env STELLA_BACKUP_REQUIRE_UPLOAD=true bash scripts/backup/stella-backup.sh pre-cd
```

That step runs before applying manifests or changing the `stella-api` image. It fails the deploy if the backup or upload fails.

## Restore

Download the archive from Google Drive first if it is not already local:

```bash
sudo rclone copy gdrive:StellaBackups/backup-stella-YYYYmmdd-HHMMSS-pre-cd.tar.gz.age /tmp/
```

Before restoring PostgreSQL or MinIO in a live environment, stop writers such as `stella-api` and `keycloak` or restore into an isolated test namespace/host.

Restore only PostgreSQL:

```bash
sudo /opt/stella-backup/current/scripts/backup/stella-restore.sh postgres /tmp/backup-stella-YYYYmmdd-HHMMSS-pre-cd.tar.gz.age
```

Restore only MinIO:

```bash
sudo /opt/stella-backup/current/scripts/backup/stella-restore.sh minio /tmp/backup-stella-YYYYmmdd-HHMMSS-pre-cd.tar.gz.age
```

Restore only Kubernetes resources and secrets:

```bash
sudo /opt/stella-backup/current/scripts/backup/stella-restore.sh kubernetes /tmp/backup-stella-YYYYmmdd-HHMMSS-pre-cd.tar.gz.age
```

Restore everything:

```bash
sudo /opt/stella-backup/current/scripts/backup/stella-restore.sh full /tmp/backup-stella-YYYYmmdd-HHMMSS-pre-cd.tar.gz.age
```

By default, restore commands ask for confirmation. For controlled automation, set:

```bash
sudo env STELLA_RESTORE_ASSUME_YES=true /opt/stella-backup/current/scripts/backup/stella-restore.sh postgres /tmp/backup.tar.gz.age
```

## Validation

For every operational change, validate at least:

```bash
sudo /opt/stella-backup/current/scripts/backup/stella-backup.sh manual
sudo rclone ls gdrive:StellaBackups
```

For a restore drill, prefer a safe test namespace or disposable k3s host. At minimum, test a PostgreSQL restore against a temporary database before relying on the backup for production recovery.

## Security Notes

- Do not commit `/etc/stella-backup/backup.env` if it contains real values.
- Do not commit `/etc/stella-backup/age-key.txt`.
- Google Drive is a pragmatic first remote, not the only supported design. Keep the rclone remote name configurable.
- Losing the age private key makes encrypted backups unrecoverable.
