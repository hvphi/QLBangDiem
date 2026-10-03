package app.webbangdiem.submission;

import java.time.OffsetDateTime;

public record SignatureCheck(String status, String reason, String signerName, String certificateSerial,
                             String issuer, OffsetDateTime signedAt) {}
