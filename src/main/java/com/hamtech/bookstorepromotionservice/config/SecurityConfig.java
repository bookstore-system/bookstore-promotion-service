package com.hamtech.bookstorepromotionservice.config;

import com.hamtech.bookstorepromotionservice.security.RsaKeyLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.nio.file.Path;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

        @Autowired
        private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

        private final String[] PUBLIC_ENDPOINTS = {
                        "/favicon.ico",

                        // Swagger UI
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/v3/api-docs.yaml",
                        "/v3/api-docs"
        };

        private final RSAPublicKey publicKey;

        public SecurityConfig(
                        @Value("${app.jwt.keys-dir:/key}") String keysDir,
                        @Value("${app.jwt.public-key-file:public.pem}") String publicKeyFile) {
                Path dir = RsaKeyLoader.resolveKeysDir(keysDir);
                this.publicKey = (RSAPublicKey) RsaKeyLoader.loadPublicKey(dir, publicKeyFile);
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .cors(Customizer.withDefaults())
                                .csrf(AbstractHttpConfigurer::disable)
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/v1/promotions/active",
                                                                "/api/v1/promotions/book/**")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/v1/promotions/validate",
                                                                "/api/v1/promotions/apply")
                                                .permitAll()
                                                .requestMatchers("/api/v1/promotions", "/api/v1/promotions/**")
                                                .hasRole("ADMIN")
                                                .anyRequest().authenticated())
                                .oauth2ResourceServer(oauth2 -> oauth2
                                                .jwt(jwtConfigurer -> jwtConfigurer
                                                                .decoder(jwtDecoder())
                                                                .jwtAuthenticationConverter(jwtAuthenticationConverter()))
                                                .authenticationEntryPoint(jwtAuthenticationEntryPoint))
                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint(jwtAuthenticationEntryPoint));

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                configuration.setAllowedOriginPatterns(Arrays.asList(
                                "http://localhost:3000",
                                "http://localhost:3001",
                                "http://127.0.0.1:3000",
                                "http://localhost:8080",
                                "https://editor.swagger.io",
                                "https://www.nhasachcongdong.id.vn",
                                "https://nhasachcongdong.id.vn",
                                "https://api.nhasachcongdong.id.vn",
                                "https://*.swaggerhub.com"));

                configuration.setAllowedMethods(Arrays.asList(
                                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
                configuration.setAllowedHeaders(Arrays.asList("*"));
                configuration.setAllowCredentials(true);
                configuration.setMaxAge(3600L);
                configuration.setExposedHeaders(Arrays.asList(
                                "Authorization",
                                "Content-Type",
                                "X-Total-Count"));

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }

        private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
                return jwt -> {
                        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

                        String role = jwt.getClaimAsString("role");
                        if (role != null && !role.isBlank()) {
                                authorities.add(new SimpleGrantedAuthority(normalizeRole(role)));
                        }

                        List<String> roles = jwt.getClaimAsStringList("roles");
                        if (roles != null) {
                                roles.stream()
                                                .filter(item -> item != null && !item.isBlank())
                                                .map(this::normalizeRole)
                                                .map(SimpleGrantedAuthority::new)
                                                .forEach(authorities::add);
                        }

                        return new JwtAuthenticationToken(jwt, authorities);
                };
        }

        @Bean
        public JwtDecoder jwtDecoder() {
                return NimbusJwtDecoder
                                .withPublicKey(publicKey)
                                .signatureAlgorithm(SignatureAlgorithm.RS256)
                                .build();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder(10);
        }

        private String normalizeRole(String role) {
                return role.startsWith("ROLE_") ? role : "ROLE_" + role;
        }
}
