package app.webbangdiem.submission;

import java.time.OffsetDateTime;

public record SignatureRecord(String signerType, String signerName, String certificateSerial, String issuer,
                              OffsetDateTime signedAt, String validationStatus, String validationReason) {}
