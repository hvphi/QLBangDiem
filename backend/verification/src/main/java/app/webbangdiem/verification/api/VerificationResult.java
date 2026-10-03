package app.webbangdiem.verification.api;

import java.util.List;

public record VerificationResult(String status, String sha256, int signatureCount, String reason,
                                 List<SignatureDetail> signatures) {}
