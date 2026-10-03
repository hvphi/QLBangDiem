package app.webbangdiem.identity;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.Customizer;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collection;

@Configuration
public class SecurityConfiguration {
    @Bean
    @Profile("local")
    SecurityFilterChain localSecurity(HttpSecurity http, DemoHeaderAuthFilter demoHeaderAuthFilter) throws Exception {
        http.cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/internal/health").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(demoHeaderAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    @Profile("!local")
    SecurityFilterChain oidcSecurity(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
        http.cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(Customizer.withDefaults())
                .logout(logout -> logout.logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)))
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(this::toActor)));
        return http.build();
    }

    @Bean
    @Profile("local")
    CorsConfigurationSource localCorsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(java.util.List.of("http://localhost:3000", "http://127.0.0.1:3000"));
        config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(java.util.List.of("Content-Type", "X-Demo-Role", "X-Correlation-ID", "X-Idempotency-Key"));
        config.setExposedHeaders(java.util.List.of("Content-Disposition", "X-Correlation-ID"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    @Profile("!local")
    JwtDecoder jwtDecoder() {
        String issuer = System.getenv("OIDC_ISSUER_URI");
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalStateException("OIDC_ISSUER_URI is required outside the local profile.");
        }
        return JwtDecoders.fromIssuerLocation(issuer);
    }

    private org.springframework.security.authentication.AbstractAuthenticationToken toActor(Jwt jwt) {
        Object rolesClaim = jwt.getClaims().getOrDefault("roles", jwt.getClaims().get("role"));
        Collection<?> roles = rolesClaim instanceof Collection<?> collection ? collection : rolesClaim == null ? java.util.List.of() : java.util.List.of(rolesClaim);
        var authorities = roles.stream().map(Object::toString).map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .map(SimpleGrantedAuthority::new).toList();
        String role = roles.stream().map(Object::toString).map(value -> value.replaceFirst("^ROLE_", ""))
                .filter(value -> java.util.List.of("LECTURER", "DEPT_HEAD", "EXAMINATION", "ADMIN").contains(value))
                .findFirst().orElse("UNASSIGNED");
        Actor actor = new Actor(jwt.getSubject(), jwt.getClaimAsString("name"), role, jwt.getClaimAsString("department_id"));
        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(actor, jwt, authorities);
    }
}
