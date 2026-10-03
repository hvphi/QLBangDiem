package app.webbangdiem.verification.api;

import java.time.OffsetDateTime;

public record SignatureDetail(String status, String reason, String signerName, String certificateSerial,
                              String issuer, OffsetDateTime signedAt) {}
