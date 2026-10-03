package app.webbangdiem.verification.validation;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class SignatureVerificationServiceTest {
    private final SignatureVerificationService verifier = new SignatureVerificationService();

    @Test
    void unsignedPdfIsNeverReportedAsValid() throws Exception {
        byte[] pdf;
        try (var document = new PDDocument(); var output = new ByteArrayOutputStream()) {
            document.addPage(new org.apache.pdfbox.pdmodel.PDPage());
            document.save(output);
            pdf = output.toByteArray();
        }

        var result = verifier.verify(pdf);

        assertThat(result.status()).isEqualTo("INVALID");
        assertThat(result.reason()).isEqualTo("NO_SIGNATURE");
        assertThat(result.sha256()).hasSize(64);
    }
}
