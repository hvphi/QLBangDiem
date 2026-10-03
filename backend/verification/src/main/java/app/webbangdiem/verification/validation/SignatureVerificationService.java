package app.webbangdiem.verification.validation;

import app.webbangdiem.verification.api.SignatureDetail;
import app.webbangdiem.verification.api.VerificationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class SignatureVerificationService {
    private static final long PROCESS_TIMEOUT_SECONDS = 60;
    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();

    private final String pythonExecutable;
    private final Path helperScript;

    public SignatureVerificationService() {
        String configuredPython = System.getenv("VERIFICATION_PYTHON");
        this.pythonExecutable = configuredPython == null || configuredPython.isBlank()
                ? (System.getProperty("os.name").toLowerCase().contains("win") ? "python" : "python3")
                : configuredPython;
        this.helperScript = extractHelperScript();
    }

    public VerificationResult verify(byte[] pdf) {
        String hash = sha256(pdf);
        if (pdf == null || pdf.length == 0) {
            return result("INVALID", hash, 0, "PDF_EMPTY", List.of());
        }
        // Keep the unsigned-PDF path independent from the Python runtime. Every signed PDF
        // is delegated to pyHanko, including PDFs PDFBox cannot parse correctly.
        if (!containsByteRange(pdf)) {
            return result("INVALID", hash, 0, "NO_SIGNATURE", List.of());
        }

        Process process = null;
        try {
            process = new ProcessBuilder(pythonExecutable, helperScript.toString())
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            Process verifierProcess = process;
            CompletableFuture<byte[]> output = CompletableFuture.supplyAsync(() -> readAll(verifierProcess.getInputStream()));
            try (OutputStream input = process.getOutputStream()) {
                input.write(pdf);
            }
            if (!process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return result("UNAVAILABLE", hash, 0, "VERIFICATION_TIMEOUT", List.of());
            }
            byte[] response = output.get(2, TimeUnit.SECONDS);
            if (process.exitValue() != 0) {
                return result("UNAVAILABLE", hash, 0, "VERIFIER_PROCESS_FAILED", List.of());
            }
            VerificationResult verified = JSON.readValue(response, VerificationResult.class);
            if (!hash.equalsIgnoreCase(verified.sha256())) {
                return result("INVALID", hash, verified.signatureCount(), "HASH_MISMATCH", verified.signatures());
            }
            return verified;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            if (process != null) process.destroyForcibly();
            return result("UNAVAILABLE", hash, 0, "VERIFICATION_INTERRUPTED", List.of());
        } catch (Exception ex) {
            if (process != null) process.destroyForcibly();
            return result("UNAVAILABLE", hash, 0, "PYHANKO_BRIDGE_UNAVAILABLE", List.of());
        }
    }

    private static byte[] readAll(InputStream stream) {
        try (stream) {
            return stream.readAllBytes();
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static Path extractHelperScript() {
        try (InputStream resource = SignatureVerificationService.class.getResourceAsStream("/pyhanko_verify.py")) {
            if (resource == null) throw new IllegalStateException("pyHanko verification helper is missing.");
            Path script = Files.createTempFile("qlbangdiem-pyhanko-", ".py");
            Files.copy(resource, script, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            script.toFile().deleteOnExit();
            return script;
        } catch (IOException ex) {
            throw new IllegalStateException("Could not prepare the pyHanko verification helper.", ex);
        }
    }

    private static boolean containsByteRange(byte[] pdf) {
        byte[] token = "/ByteRange".getBytes(StandardCharsets.US_ASCII);
        outer:
        for (int start = 0; start <= pdf.length - token.length; start++) {
            for (int i = 0; i < token.length; i++) {
                if (pdf[start + i] != token[i]) continue outer;
            }
            return true;
        }
        return false;
    }

    private static String sha256(byte[] pdf) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(pdf == null ? new byte[0] : pdf));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static VerificationResult result(String status, String hash, int count, String reason,
                                             List<SignatureDetail> signatures) {
        return new VerificationResult(status, hash, count, reason, signatures);
    }
}
