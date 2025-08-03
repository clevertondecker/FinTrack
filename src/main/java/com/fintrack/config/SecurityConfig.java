package com.fintrack.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.context.annotation.Profile;
import org.springframework.web.cors.CorsConfigurationSource;

import com.fintrack.infrastructure.security.CustomUserDetailsService;
import com.fintrack.infrastructure.security.JwtFilter;
import com.fintrack.infrastructure.security.JwtOAuth2SuccessHandler;
import com.fintrack.infrastructure.security.JwtUtil;

/**
 * Security configuration for the application.
 * Configures authentication, authorization, and JWT-based security.
 */
@Profile("!test")
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  /**
   * Creates a BCrypt password encoder bean.
   *
   * @return the password encoder
   */
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * Configures the security filter chain with JWT authentication and OAuth2 support.
   *
   * @param jwtUtil the JWT utility service. Configures JWT token handling.
   *
   * @param uds the user details service. Configures user details retrieval.
   *
   * @param corsConfigurationSource the CORS configuration source. Configures CORS settings.
   *
   * @param successHandler the OAuth2 success handler. Configures the success handler for OAuth2 login.
   *
   * @return the configured SecurityFilterChain. Configures security settings
   * for the application and applies JWT authentication.
   *
   * @throws Exception if configuration fails.
   */
  @Bean
  public SecurityFilterChain filterChain(final HttpSecurity http,
                                         final JwtUtil jwtUtil,
                                         final CustomUserDetailsService uds,
                                         final JwtOAuth2SuccessHandler successHandler,
                                         final CorsConfigurationSource corsConfigurationSource) throws Exception {
    configureSecuritySettings(http, corsConfigurationSource);
    configureAuthorization(http);
    configureOAuth2(http, successHandler);
    configureJwtFilter(http, jwtUtil, uds);

    return http.build();
  }

  /**
   * Configures basic security settings.
   *
   * @param http the HttpSecurity object
   *
   * @param corsConfigurationSource the CORS configuration source
   *
   * @throws Exception if configuration fails
   */
  private void configureSecuritySettings(HttpSecurity http,
                                         CorsConfigurationSource corsConfigurationSource)
          throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
  }

  /**
   * Configures request authorization rules.
   *
   * @param http the HttpSecurity object. Cannot be null.
   *
   * @throws Exception if configuration fails.
   */
  private void configureAuthorization(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/users/register").permitAll()
        .requestMatchers("/api/auth/login").permitAll()
        .requestMatchers("/api/banks").permitAll()
        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
        .anyRequest().authenticated());
  }

  /**
   * Configures OAuth2 login with custom success handler.
   *
   * @param http the HttpSecurity object. Cannot be null.
   *
   * @param successHandler the OAuth2 success handler. Cannot be null.
   *
   * @throws Exception if configuration fails.
   */
  private void configureOAuth2(HttpSecurity http,
                               JwtOAuth2SuccessHandler successHandler) throws Exception {
    http.oauth2Login(oauth -> oauth.successHandler(successHandler));
  }

  /**
   * Configures JWT filter for authentication.
   *
   * @param http the HttpSecurity object. Cannot be null.
   *
   * @param jwtUtil the JWT utility service. Configures JWT token handling.
   *
   * @param uds the user details service. Cannot be null.
   *
   * @throws Exception if configuration fails
   */
  private void configureJwtFilter(HttpSecurity http, JwtUtil jwtUtil, CustomUserDetailsService uds) throws Exception {
    http.addFilterBefore(new JwtFilter(jwtUtil, uds), UsernamePasswordAuthenticationFilter.class);
  }

  /**
   * Creates an AuthenticationManager bean.
   *
   * @param cfg the authentication configuration. Provides the configuration
   * for authentication.
   *
   * @return the authentication manager. Manages authentication operations
   * for the application.
   *
   * @throws Exception if creation fails.
   */
  @Bean
  public AuthenticationManager authManager(
      final AuthenticationConfiguration cfg) throws Exception {
    return cfg.getAuthenticationManager();
  }
}
