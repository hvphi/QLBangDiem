package app.webbangdiem.verification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.Map;

@SpringBootApplication
public class VerificationApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(VerificationApplication.class);
        app.setDefaultProperties(Map.of("server.port", System.getenv().getOrDefault("VERIFICATION_PORT", "8081"),
                "server.address", System.getenv().getOrDefault("VERIFICATION_ADDRESS", "127.0.0.1")));
        app.run(args);
    }
}
