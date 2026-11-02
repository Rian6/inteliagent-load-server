package com.inteliagent.load_server.configuration;

import java.util.List;
import java.util.Base64;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;

@Configuration
public class SecurityConfig {
    @Bean
    SecretKeySpec jwtKey(@Value("${security.jwt.secret}") String secret) {
        byte[] bytes = Base64.getDecoder().decode(secret);
        if (bytes.length < 32) throw new IllegalArgumentException("JWT_SECRET requires at least 32 random bytes in Base64");
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
    @Bean
    JwtDecoder jwtDecoder(SecretKeySpec key, @Value("${security.jwt.issuer}") String issuer, org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate db) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(issuer), token -> {
                try {
                    var rows = db.queryForList("SELECT id FROM usuario WHERE id=:uid AND email=:email AND id_cede=:cede AND token_version=:version AND role=:role AND situacao=1 AND situacao_aprovacao=1",
                        java.util.Map.<String, Object>of("uid", token.getClaim("uid"), "email", token.getSubject(), "cede", token.getClaim("cede"), "version", token.getClaim("ver"), "role", token.getClaimAsStringList("roles").getFirst()));
                    if (rows.size() == 1) return org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.success();
                } catch (Exception ignored) { }
                return org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.failure(new org.springframework.security.oauth2.core.OAuth2Error("invalid_token", "Sessão inválida", null));
            }));
        return decoder;
    }
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        var roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        http.csrf(AbstractHttpConfigurer::disable).cors(c -> {})
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/actuator/health").permitAll()
                
                .requestMatchers("/aprove").denyAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter)));
        return http.build();
    }
    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${security.cors.origins}") List<String> origins) {
        var cors = new CorsConfiguration();
        cors.setAllowedOrigins(origins);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cors.setExposedHeaders(List.of("Authorization"));
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }
}
