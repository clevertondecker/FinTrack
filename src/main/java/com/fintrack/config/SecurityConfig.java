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
   * Configures the security filter chain with JWT authentication.
   *
   * @param http the HttpSecurity object. Configures security settings.
   *
   * @param jwtUtil the JWT utility service. Configures JWT token handling.
   *
   * @param uds the user details service. Configures user details retrieval.
   *
   * @param corsConfigurationSource the CORS configuration source. Configures CORS settings.
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
                                         final CorsConfigurationSource corsConfigurationSource) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
      .cors(cors -> cors.configurationSource(corsConfigurationSource))
      .sessionManagement(sm ->
        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/users/register").permitAll()
        .requestMatchers("/api/auth/login").permitAll()
        .anyRequest().authenticated());

    http.addFilterBefore(
      new JwtFilter(jwtUtil, uds),
      UsernamePasswordAuthenticationFilter.class);

    return http.build();
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
