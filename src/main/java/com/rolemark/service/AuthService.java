package com.rolemark.service;

import com.rolemark.config.JwtUtil;
import com.rolemark.dto.AuthResponse;
import com.rolemark.dto.LoginRequest;
import com.rolemark.dto.SignupRequest;
import com.rolemark.entity.User;
import com.rolemark.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    
    public AuthService(UserRepository userRepository, 
                      PasswordEncoder passwordEncoder, 
                      JwtUtil jwtUtil,
                      AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }
    
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user = userRepository.save(user);
        
        String token = jwtUtil.generateToken(user.getEmail(), user.getId());
        return new AuthResponse(token, user.getEmail());
    }
    
    /**
     * Login using AuthenticationManager.authenticate() to leverage the configured
     * DaoAuthenticationProvider with UserDetailsServiceImpl and BCryptPasswordEncoder.
     * This ensures consistent authentication logic and prevents default user/password auto-config.
     */
    public AuthResponse login(LoginRequest request) {
        // Use AuthenticationManager to authenticate - this will use our DaoAuthenticationProvider
        // which is wired to UserDetailsServiceImpl and BCryptPasswordEncoder
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        
        // Get the authenticated user's email from the authentication principal
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found after authentication"));
        
        String token = jwtUtil.generateToken(user.getEmail(), user.getId());
        return new AuthResponse(token, user.getEmail());
    }
}

