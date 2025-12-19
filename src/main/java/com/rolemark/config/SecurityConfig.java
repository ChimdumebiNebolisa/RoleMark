package com.rolemark.config;

import com.rolemark.security.JwtAuthenticationFilter;
import com.rolemark.security.UserDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for JWT-based authentication.
 * 
 * IMPORTANT: This configuration ensures that Spring Security will NEVER print
 * "Using generated security password" because:
 * 1. formLogin() and httpBasic() are explicitly disabled
 * 2. An explicit AuthenticationManager is provided via DaoAuthenticationProvider
 * 3. No default UserDetailsService auto-configuration is triggered (we provide our own)
 * 4. Session creation policy is STATELESS (no session-based auth)
 * 
 * Spring Boot only generates a default password when no AuthenticationManager
 * or UserDetailsService bean is found AND form-based or HTTP basic auth is enabled.
 * Since we explicitly provide both and disable those auth methods, the default
 * password generation is completely bypassed.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsServiceImpl userDetailsService;
    
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, 
                         UserDetailsServiceImpl userDetailsService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * Explicitly defines AuthenticationManager using DaoAuthenticationProvider.
     * This prevents Spring Boot from auto-configuring a default user/password.
     * The provider is wired to our custom UserDetailsServiceImpl and BCryptPasswordEncoder.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    
    /**
     * Exposes AuthenticationManager bean for use in authentication flows.
     * This ensures the login endpoint can use authenticationManager.authenticate().
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Disable form login and HTTP basic auth - prevents default password generation
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .authorizeHttpRequests(auth -> auth
                // Permit GET /api/health without authentication
                .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                // Permit all /api/auth/** endpoints (login, signup, register)
                .requestMatchers("/api/auth/**").permitAll()
                // Require authentication for all other requests
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}

