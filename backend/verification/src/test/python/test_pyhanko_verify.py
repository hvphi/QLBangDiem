import importlib.util
import logging
import json
import os
import subprocess
import sys
from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[5]
spec = importlib.util.spec_from_file_location(
    "pyhanko_verify", ROOT / "backend/verification/src/main/resources/pyhanko_verify.py"
)
verifier = importlib.util.module_from_spec(spec)
spec.loader.exec_module(verifier)
logging.disable(logging.CRITICAL)


class LecturerSignatureVerificationTest(unittest.TestCase):
    def test_subprocess_returns_utf8_signer_metadata(self):
        process = subprocess.run(
            [sys.executable, str(ROOT / "backend/verification/src/main/resources/pyhanko_verify.py")],
            input=(ROOT / "docs/bang-diem-CK_N1.signed.pdf").read_bytes(),
            capture_output=True, timeout=30,
            env={**os.environ, "PYTHONIOENCODING": "cp1252"},
        )
        self.assertEqual(0, process.returncode, process.stderr.decode("utf-8", errors="replace"))
        result = json.loads(process.stdout.decode("utf-8"))
        self.assertEqual("VALID", result["status"])
        self.assertEqual(1, result["signatureCount"])
        self.assertTrue(result["signatures"][0]["signerName"])

    def test_one_embedded_signature_is_accepted_for_lecturer_submission(self):
        pdf = (ROOT / "docs/bang-diem-CK_N1.signed.pdf").read_bytes()
        result = verifier.verify(pdf)
        self.assertEqual("VALID", result["status"])
        self.assertEqual(1, result["signatureCount"])
        self.assertEqual(1, len(result["signatures"]))
        self.assertEqual("VALID", result["signatures"][0]["status"])
        self.assertTrue(result["signatures"][0]["certificateSerial"])

    def test_tampered_signed_pdf_is_rejected(self):
        pdf = (ROOT / "docs/bang-diem-CK_N1.signed.pdf").read_bytes()
        self.assertTrue(pdf.startswith(b"%PDF-"))
        # Change the PDF version inside the signed byte range, keeping it parseable.
        replacement = b"6" if pdf[7:8] != b"6" else b"7"
        tampered = pdf[:7] + replacement + pdf[8:]
        result = verifier.verify(tampered)
        self.assertEqual("INVALID", result["status"])
        self.assertEqual("SIGNATURE_INTEGRITY_INVALID", result["reason"])


if __name__ == "__main__":
    unittest.main()
