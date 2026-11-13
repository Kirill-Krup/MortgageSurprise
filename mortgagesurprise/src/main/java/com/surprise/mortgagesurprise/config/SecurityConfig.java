package com.surprise.mortgagesurprise.config;

import com.surprise.mortgagesurprise.security.jwt.JwtAuthenticationFilter;
import com.surprise.mortgagesurprise.service.CustomUserDetailsService;
import com.surprise.mortgagesurprise.service.impl.CustomOAuth2UserService;
import com.surprise.mortgagesurprise.service.impl.OAuth2AuthenticationFailureHandler;
import com.surprise.mortgagesurprise.service.impl.OAuth2AuthenticationSuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;
    private final com.surprise.mortgagesurprise.security.CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final com.surprise.mortgagesurprise.security.CustomAccessDeniedHandler accessDeniedHandler;
    private final String frontendUrl;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          CustomUserDetailsService userDetailsService,
                          com.surprise.mortgagesurprise.security.CustomAuthenticationEntryPoint authenticationEntryPoint,
                          com.surprise.mortgagesurprise.security.CustomAccessDeniedHandler accessDeniedHandler,
                          @Value("${app.frontend-url}") String frontendUrl) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.frontendUrl = frontendUrl;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   CustomOAuth2UserService customOAuth2UserService,
                                                   OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler,
                                                   OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/magic-link",
                                "/api/v1/auth/magic-link/verify",
                                "/api/v1/auth/oauth2/exchange").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/code/**").permitAll()
                        .requestMatchers("/api/v1/calculator/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/bank-offers/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/bank-offers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/bank-offers/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/bank-offers/**").hasRole("ADMIN")
                        .requestMatchers("/actuator/**", "/error").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .oauth2Login(oauth2 -> oauth2
                        .tokenEndpoint(tokenEndpoint -> tokenEndpoint
                                .accessTokenResponseClient(accessTokenResponseClient())
                        )
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        .failureHandler(oAuth2AuthenticationFailureHandler)
                );
        return http.build();
    }

    @Bean
    public org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient<org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient() {
        var client = new org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient();
        client.addHeadersConverter(grantRequest -> {
            var headers = new org.springframework.http.HttpHeaders();
            headers.set(org.springframework.http.HttpHeaders.USER_AGENT, "MortgageSurprise-App/1.0");
            return headers;
        });
        return client;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("http://localhost", "http://localhost:3000", "http://localhost:5173", frontendUrl));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With", "Origin"));
        configuration.setExposedHeaders(List.of("Set-Cookie", "Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
