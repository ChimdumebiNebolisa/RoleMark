package com.rolemark.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rolemark.dto.AuthResponse;
import com.rolemark.dto.LoginRequest;
import com.rolemark.dto.SignupRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:authtestdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "jwt.secret=test-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm-test"
})
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void signupThenLoginReturnsJWT() throws Exception {
        // Test 1: signup → login returns JWT
        SignupRequest signupRequest = new SignupRequest("test@example.com", "password123");

        // Signup
        String signupResponse = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        AuthResponse signupAuthResponse = objectMapper.readValue(signupResponse, AuthResponse.class);
        String signupToken = signupAuthResponse.getToken();
        assert signupToken != null && !signupToken.isEmpty();

        // Login
        LoginRequest loginRequest = new LoginRequest("test@example.com", "password123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }
}

