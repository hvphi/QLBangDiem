"""Read a PDF from stdin and emit the verifier service's JSON contract."""

import hashlib
import io
import json
import sys
from datetime import timezone

from pyhanko.pdf_utils.reader import PdfFileReader
from pyhanko.sign import validation


def signature_detail(embedded_signature):
    try:
        status = validation.validate_pdf_signature(embedded_signature)
    except Exception:
        return {
            "status": "INVALID",
            "reason": "SIGNATURE_VALIDATION_FAILED",
            "signerName": None,
            "certificateSerial": None,
            "issuer": None,
            "signedAt": None,
        }

    intact = bool(status.intact)
    certificate = status.signing_cert
    signer_name = "VGCA Signed"
    serial = None
    issuer = None
    if certificate is not None:
        try:
            signer_name = certificate.subject.native.get("common_name", signer_name)
        except Exception:
            pass
        try:
            serial = format(certificate.serial_number, "x")
        except Exception:
            pass
        try:
            issuer = str(certificate.issuer.native)
        except Exception:
            pass

    signed_at = getattr(embedded_signature, "signer_reported_dt", None)
    if signed_at is not None:
        if signed_at.tzinfo is None:
            signed_at = signed_at.replace(tzinfo=timezone.utc)
        signed_at = signed_at.isoformat()

    return {
        "status": "VALID" if intact else "INVALID",
        "reason": "SIGNATURE_INTEGRITY_VALID" if intact else "SIGNATURE_INTEGRITY_INVALID",
        "signerName": signer_name,
        "certificateSerial": serial,
        "issuer": issuer,
        "signedAt": signed_at,
    }


def verify(pdf_bytes):
    digest = hashlib.sha256(pdf_bytes).hexdigest()
    try:
        reader = PdfFileReader(io.BytesIO(pdf_bytes))
        embedded_signatures = reader.embedded_signatures
    except Exception:
        return {
            "status": "INVALID",
            "sha256": digest,
            "signatureCount": 0,
            "reason": "PDF_MALFORMED_OR_SIGNATURE_UNREADABLE",
            "signatures": [],
        }

    if not embedded_signatures:
        return {
            "status": "INVALID",
            "sha256": digest,
            "signatureCount": 0,
            "reason": "NO_SIGNATURE",
            "signatures": [],
        }

    # This follows the rule used by the supplied main.py: at least two embedded
    # signatures are required before their integrity is evaluated.
    if len(embedded_signatures) < 2:
        return {
            "status": "INVALID",
            "sha256": digest,
            "signatureCount": len(embedded_signatures),
            "reason": "INSUFFICIENT_SIGNATURES",
            "signatures": [],
        }

    signatures = [signature_detail(sig) for sig in embedded_signatures]
    all_intact = all(signature["status"] == "VALID" for signature in signatures)
    return {
        "status": "VALID" if all_intact else "INVALID",
        "sha256": digest,
        "signatureCount": len(signatures),
        "reason": "SIGNATURES_INTACT_TRUST_NOT_CHECKED" if all_intact else "SIGNATURE_INTEGRITY_INVALID",
        "signatures": signatures,
    }


if __name__ == "__main__":
    pdf_bytes = sys.stdin.buffer.read()
    try:
        result = verify(pdf_bytes)
    except Exception:
        result = {
            "status": "INVALID",
            "sha256": hashlib.sha256(pdf_bytes).hexdigest(),
            "signatureCount": 0,
            "reason": "PDF_MALFORMED_OR_SIGNATURE_UNREADABLE",
            "signatures": [],
        }
    sys.stdout.write(json.dumps(result, ensure_ascii=False, separators=(",", ":")))
