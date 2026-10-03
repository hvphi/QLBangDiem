# Local staging acceptance record

Run `infra/deploy/start-local.ps1`, then `infra/deploy/smoke-local.ps1`. Run browser flows with `npm --prefix frontend run e2e` while staging is active. On Windows, set `PLAYWRIGHT_CHROMIUM_EXECUTABLE` to an installed Chrome or Edge executable if the Playwright-managed Chromium is unavailable. Local demo records are synthetic and stored only under `.local/`.

## Verified on 2026-09-28

- Local startup completed with H2 migration V1, seeded demo catalog, verification service on `127.0.0.1:8081`, core on `127.0.0.1:8080`, and portal on `127.0.0.1:3000`.
- `infra/deploy/smoke-local.ps1`: portal HTTP 200, core `UP`, verification health `UP`, lecturer identity and two assigned classes.
- Browser E2E: 2/2 passed (lecturer class view and unsigned-PDF denial; department and examination workspace navigation).
- API checks: unauthenticated and out-of-role audit requests denied; unsigned PDF rejected with HTTP 422; audit hash chain verified after the rejection.
- Backup tests: 2/2 passed (encrypted fixture restores with matching checksum; wrong key is rejected).
- Backend `mvn verify`, frontend TypeScript check, and Next.js production build completed successfully. Frontend unit tests: 4 passed for role-to-workspace and role-label mapping.

| Area | Local-stage result | Boundary before production |
|---|---|---|
| H2 migration and sample catalog | Verified at startup; smoke check returned two lecturer classes. | PostgreSQL/Redis/MinIO compose was not run because Docker is not installed on this host. |
| Lecturer/department/examination roles | Loopback-only local demo role switch works; API blocks unauthenticated and out-of-scope requests; E2E workspace checks pass. | School OIDC issuer/client/role claims have not been supplied or exercised. |
| PDF verification | Malformed/unsigned PDFs return `INVALID`; upload is rejected with HTTP 422 and audited. Cryptographically intact signatures without trust configuration remain `UNKNOWN`. | CA trust anchors, revocation policy/source, TSA endpoint, and an approved signed test fixture are unavailable; valid-signature and full approval flows are not verified. |
| USB Token signing | Consent-gated loopback agent contract and vSignPDF fallback are implemented. | School PKCS#11 agent, model driver, token, and signing certificate are unavailable. |
| Storage | Local filesystem immutable-revision adapter verifies SHA-256 on reads; no valid signed upload was available to exercise a stored transcript. | Production MinIO/S3 endpoint, credentials, versioning/object-lock policy are not present. |
| Audit | Rejected upload audit event was recorded and the hash-chain integrity endpoint passed. | Production database grants denying UPDATE/DELETE must be applied by the DBA. |
| Backup | AES-256-GCM fixture backup/restore and wrong-key rejection passed; restore targets a separate folder. | School encryption-key custody, rclone remote, schedule, retention, and alert target remain to be configured. |
| Production deployment | Dockerfiles, isolated networks, Nginx routes, required secrets, health checks, and runbooks are present. | Docker Compose, TLS files, production identity, database, storage, and network environment were not available here. |

The local profile is a staging demo, not a production PKI validation environment. No transcript advances unless the configured trust and revocation checks return `VALID`.
