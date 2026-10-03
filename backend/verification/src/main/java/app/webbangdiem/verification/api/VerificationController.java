package app.webbangdiem.verification.api;

import app.webbangdiem.verification.validation.SignatureVerificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.security.MessageDigest;

@RestController
@RequestMapping("/internal/v1")
public class VerificationController {
    private final SignatureVerificationService verifier;
    private final String serviceToken;

    public VerificationController(SignatureVerificationService verifier,
                                  @Value("${VERIFICATION_SERVICE_TOKEN:local-stage-only}") String serviceToken) {
        this.verifier = verifier;
        this.serviceToken = serviceToken;
    }

    @PostMapping(path = "/verify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VerificationResult verify(@RequestHeader(value = "X-Service-Token", required = false) String token,
                                     @RequestPart("file") MultipartFile file) throws Exception {
        if (serviceToken.isBlank() || token == null || !MessageDigest.isEqual(
                serviceToken.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                token.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Service identity is required.");
        }
        if (file.isEmpty() || file.getSize() > 20L * 1024 * 1024) {
            throw new ResponseStatusException(file.isEmpty() ? HttpStatus.BAD_REQUEST : HttpStatus.PAYLOAD_TOO_LARGE,
                    "PDF is empty or larger than 20 MB.");
        }
        return verifier.verify(file.getBytes());
    }
}
