package app.webbangdiem.submission;

import java.util.List;

public record VerificationResult(String status, String sha256, int signatureCount, String reason,
                                 List<SignatureCheck> signatures) {}
