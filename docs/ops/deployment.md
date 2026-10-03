# Local staging and on-premise deployment

## Local staging

The local stage runs the verification service on `127.0.0.1:8081`, Spring core on `127.0.0.1:8080`, and the Next.js portal on `127.0.0.1:3000`. It uses H2 and local filesystem storage. The local role selector is enabled only in this profile. Use `infra/deploy/start-local.ps1` from the repository root; the script builds before starting hidden processes and writes logs and process IDs under ignored `.local/`. The browser URL is `http://127.0.0.1:3000`.

## On-premise topology

Build the three application images only after `mvn -f backend/pom.xml verify` and `npm --prefix frontend run build` succeed. Configure the required environment variables and mount TLS certificate/key using the host secret mechanism before starting `docker compose -f infra/deploy/compose.yaml up -d --build`. The gateway and portal share an edge network; core, verification, and PostgreSQL share an isolated internal network. Verification has no published host port and the edge rejects `/internal/` paths. Transcript objects use the school's MinIO/S3 service; this compose file requires an external storage endpoint and credentials.

Core starts only with OIDC issuer, database password, service token, storage endpoint, and TLS material configured. It runs Flyway forward migrations at startup. Before each upgrade, create a coordinated DB/object backup and record the release tag. Promote the candidate, wait for core and verification health, and check the workflow using test certificates. Roll back by restoring the previous app image tag only when its schema is compatible; never remove PostgreSQL/object storage volumes during rollback. A failed migration or incompatible schema requires the [backup and recovery runbook](backup-recovery.md) and an isolated restore target.

## Upgrade, rollback, and secret rotation

1. Record the current image tag and successful backup/checksum.
2. Build an immutable release tag; do not use `latest` for application images.
3. Apply the release in staging and confirm readiness, OIDC login, upload/verify, approval, download, and audit.
4. Promote the same image digests. Keep the previous image available until acceptance completes.
5. Rotate OIDC/client, database, MinIO, service-to-service, and TLS credentials through the host secret store; restart only affected services and verify health.
6. If the release fails, switch back to the prior compatible image. Restore data only into a separate recovery environment and verify hashes before any production recovery.

The local staging profile does not include production OIDC, CA trust, revocation or TSA endpoints, USB-token drivers, an institutional database, or a school storage/backup account. A local healthy result is not production readiness.
