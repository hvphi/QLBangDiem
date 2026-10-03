# Signing bridge contract

The browser adapter probes a loopback agent at `http://127.0.0.1:49152/v1/health` and sends a PDF only after an explicit confirmation for that file. A compatible school-provided agent must open the PIN prompt in its own trusted UI, never return or persist the PIN, select the user's token certificate, and return an incremental PAdES PDF as `application/pdf`. The web app sends every returned file through the server-side verification service before workflow changes.

No VGCA/PKCS#11 vendor driver, agent package, test token, or certificate policy was present in this repository, so this adapter does not claim to sign locally. Until the school supplies and allow-lists the matching native agent, users sign with vSignPDF and upload the result. Agent unavailable, missing token, cancelled consent, or signing failure returns an actionable error without uploading a document.
