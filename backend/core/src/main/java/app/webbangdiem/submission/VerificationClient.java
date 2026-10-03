package app.webbangdiem.submission;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class VerificationClient {
    private final RestClient client;
    private final String token;

    public VerificationClient(@Value("${app.verification.base-url}") String baseUrl,
                              @Value("${app.verification.service-token}") String token) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
        this.token = token;
    }

    public VerificationResult verify(byte[] bytes, String filename) {
        ByteArrayResource resource = new ByteArrayResource(bytes) {
            @Override public String getFilename() { return filename == null ? "transcript.pdf" : filename; }
        };
        var parts = new LinkedMultiValueMap<String, Object>();
        parts.add("file", resource);
        try {
            return client.post().uri("/internal/v1/verify")
                    .header("X-Service-Token", token)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(parts).retrieve().body(VerificationResult.class);
        } catch (RestClientException ex) {
            return new VerificationResult("UNAVAILABLE", app.webbangdiem.storage.LocalImmutableStorage.sha256(bytes), 0,
                    "VERIFICATION_SERVICE_UNAVAILABLE", List.of());
        }
    }
}
