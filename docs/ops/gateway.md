# API gateway

Nginx terminates TLS from mounted Docker secrets, applies request-size/time limits, adds a correlation ID, and routes `/api/` to core and other paths to the portal. `/internal/` always returns 404 at the edge, so the verification engine has no public route. Access logs include method/path/status/bytes/request ID only; they do not write headers, bearer tokens, or request bodies.

Mount the school's certificate and private key as `tls_certificate` and `tls_key`; renew them using the school's certificate process, validate Nginx configuration, and reload the gateway during the approved change window. Do not store certificates or private keys in this repository. Tune rate and upload limits only after checking expected institutional load.
