package com.hamtech.bookstorepromotionservice.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;

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
                        "/v3/api-docs",

                        // Promotions
                        "/api/promotions/**",
        };

        @Value("${jwt.signerKey}")
        private String signerKey;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                // 1. Kích hoạt tích hợp CORS vào Security (CỰC QUAN TRỌNG!)
                                .cors(Customizer.withDefaults())

                                // 2. Tắt CSRF vì dùng JWT (stateless)
                                .csrf(AbstractHttpConfigurer::disable)

                                .authorizeHttpRequests(auth -> auth
                                                // Cho phép tất cả request OPTIONS (preflight) đi qua
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                                                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                                                .anyRequest().authenticated())

                                // Cấu hình OAuth2 Resource Server với JWT
                                .oauth2ResourceServer(oauth2 -> oauth2
                                                .jwt(jwtConfigurer -> jwtConfigurer
                                                                .decoder(jwtDecoder())
                                                                .jwtAuthenticationConverter(
                                                                                jwtAuthenticationConverter()))
                                                .authenticationEntryPoint(jwtAuthenticationEntryPoint))

                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint(jwtAuthenticationEntryPoint));

                return http.build();
        }

        /**
         * Cấu hình CORS cho phép Frontend (localhost:3000) truy cập Backend
         * (localhost:8080)
         */
        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                // Cho phép các origin cụ thể (localhost:3000 cho development, Swagger UI)
                configuration.setAllowedOriginPatterns(Arrays.asList(
                                "http://localhost:3000",
                                "http://localhost:3001",
                                "http://127.0.0.1:3000",
                                "http://localhost:8080",
                                "https://editor.swagger.io",
                                "https://*.swaggerhub.com"));

                // Cho phép các HTTP methods
                configuration.setAllowedMethods(Arrays.asList(
                                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

                // Cho phép tất cả headers
                configuration.setAllowedHeaders(Arrays.asList("*"));

                // Cho phép gửi credentials (cookies, authorization headers)
                configuration.setAllowCredentials(true);

                // Cache preflight response trong 1 giờ
                configuration.setMaxAge(3600L);

                // Expose các headers để frontend có thể đọc
                configuration.setExposedHeaders(Arrays.asList(
                                "Authorization",
                                "Content-Type",
                                "X-Total-Count"));

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }

        @Bean
        public JwtAuthenticationConverter jwtAuthenticationConverter() {
                JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
                grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");

                JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
                jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);

                return jwtAuthenticationConverter;
        }

        @Bean
        public JwtDecoder jwtDecoder() {
                SecretKeySpec secretKeySpec = new SecretKeySpec(signerKey.getBytes(), "HS512");
                return NimbusJwtDecoder
                                .withSecretKey(secretKeySpec)
                                .macAlgorithm(MacAlgorithm.HS512)
                                .build();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder(10);
        }
}
