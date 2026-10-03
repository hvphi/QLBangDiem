# Authentication and authorization contract v1

Production requests use OIDC JWT with the configured issuer. The token subject identifies the account; `roles` (or `role`) supplies one of `LECTURER`, `DEPT_HEAD`, `EXAMINATION`, `ADMIN`, and `department_id` scopes department-bound access. Missing role defaults to denied. Local staging uses `X-Demo-Role` only while Spring profile `local` is active and binds the backend to loopback; it is not a production login mechanism.

Lecturers can read and submit only their assigned course classes. Department heads can review only their department. Examination can search and download within the institution. The API checks scope on each operation even when the frontend hides a route.
