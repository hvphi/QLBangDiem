# Incident response runbook

- **Verification service unavailable:** stop submission/approval transitions; keep existing immutable files and audit rows; restore service connectivity and re-run verification before resuming.
- **CA, OCSP/CRL, or TSA unavailable:** treat the result as `UNKNOWN` or `UNAVAILABLE`; do not approve or archive. Record the reason and contact the school's PKI operator.
- **Object hash mismatch:** deny download/approval, isolate the object key and database row, preserve audit data, compare verified backup copies, and restore only after checksum review.
- **OIDC outage or role mismatch:** fail closed, preserve logs/correlation IDs without tokens, and coordinate with the identity provider administrator.
- **Backup/restore failure:** retain the last verified encrypted backup, do not prune prior copies, check key access and rclone status, and repeat the drill into a fresh restore folder.
- **Suspected credential exposure:** revoke the affected service credential through the host secret manager, rotate linked secrets, inspect access records, and verify service health before reopening traffic.

Record incident time, affected services, correlation IDs, actions, hashes, and recovery sign-off. Never put PDFs, grades, tokens, PINs, or private keys in incident logs.
