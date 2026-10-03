# Audit event contract v1

Core modules append a server-timestamped event with `actorId`, `actorRole`, `action`, `objectType`, `objectId`, `outcome`, `correlationId`, and minimal JSON `metadata`. The event stores the previous event hash and a SHA-256 hash over the canonical event fields. Metadata must not contain PDF bytes, access tokens, PINs, private keys, or grade contents.

The application exposes read-only query routes. There is no update or delete operation. Production database grants must give the application identity `INSERT` and `SELECT` on `audit_events` and must deny `UPDATE` and `DELETE`; the local profile uses the same application API but does not claim infrastructure-level WORM.
