# Staging backup and restore

`backup.mjs` walks a selected source folder, writes an AES-256-GCM encrypted archive with a SHA-256 manifest, and can copy the finished archive with `rclone` when `QLBD_BACKUP_REMOTE` is configured. `restore.mjs` authenticates and checks every file before writing into a separate destination; it refuses path traversal and never overwrites existing files.

Set `QLBD_BACKUP_KEY` to a base64-encoded random 32-byte key through the host secret store. Keep that key separate from the archive. For local staging, stop the core process before backing up `.local/data` so the H2 database is consistent, then run:

```powershell
node infra/backup/backup.mjs
node infra/backup/restore.mjs .local/backups/<archive>.qlbackup .local/restore-drill
```

Production should use the configured `rclone` service identity and a scheduled host job at the school-approved timezone. The account, remote, key rotation, retention, and alert destination are not included here. Restore drills should target a fresh folder and verify the returned manifest count before connecting a recovered DB to any service.
