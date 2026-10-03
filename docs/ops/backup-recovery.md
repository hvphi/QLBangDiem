# Backup and restore runbook

## Local staging

The local archive covers the H2 database files and local immutable storage under `.local/`. Stop the application first so the H2 files are closed and the database and PDFs are captured in one consistent snapshot.

1. Stop local services with `infra/deploy/stop-local.ps1`.
2. Provide `QLBD_BACKUP_KEY` from the local secret store as base64 for a randomly generated 32-byte key. Never put the key in this repository, command history, or logs.
3. Optionally set `QLBD_BACKUP_REMOTE` to an already configured `rclone` remote path. Keep the remote credentials in the machine's protected rclone configuration.
4. Run `node infra/backup/backup.mjs`. The command writes an AES-256-GCM archive to `.local/backups/` and, if configured, copies that encrypted archive to the rclone destination.
5. Confirm the command reports the expected file count and remote result before restarting with `infra/deploy/start-local.ps1`.

The script writes a temporary archive and renames it after the write completes. It excludes its own `backups` directory from the source walk so previous archives are not recursively included.

## Restore drill

Restore into a new, empty directory. The tool checks the authenticated-encryption tag, validates every manifest path, and checks each file size and SHA-256 before writing any file.

```powershell
$env:QLBD_BACKUP_KEY = '<load from the secret store>'
node infra/backup/restore.mjs .local/backups/<archive>.qlbackup .local/restore-drill
```

Compare restored file hashes with the source manifest or known fixture hashes. The restore tool refuses to overwrite files already in the destination. Do not copy restored data over a running staging or production database; inspect it in isolation and follow the deployment runbook for recovery.

## Production boundary

The local script is not a coordinated PostgreSQL plus MinIO backup job. Production requires database-native snapshots/dumps and object-store versioning or snapshots captured under one recorded release/snapshot ID, followed by a manifest that ties the two sets together. Configure encryption-key custody, scheduled execution, retention, bounded retry, monitoring, and an alert destination with the institution's operations team before relying on production recovery. Keep the last verified backup until a new backup and restore drill both pass.

Run the local fixture checks with `node --test tests/integration/backup.test.mjs`. They cover successful encrypted restore/checksum validation and rejection of a wrong key.
