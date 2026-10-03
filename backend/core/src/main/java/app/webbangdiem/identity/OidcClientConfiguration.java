package app.webbangdiem.identity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

@Configuration
@Profile("!local")
public class OidcClientConfiguration {
    @Bean
    ClientRegistrationRepository clientRegistrationRepository(
            @Value("${OIDC_ISSUER_URI}") String issuer,
            @Value("${OIDC_CLIENT_ID}") String clientId,
            @Value("${OIDC_CLIENT_SECRET}") String clientSecret,
            @Value("${OIDC_REDIRECT_URI}") String redirectUri) {
        ClientRegistration registration = ClientRegistrations.fromIssuerLocation(issuer)
                .registrationId("school")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(redirectUri)
                .scope("openid", "profile", "email")
                .clientName("School identity provider")
                .build();
        return new InMemoryClientRegistrationRepository(registration);
    }
}
