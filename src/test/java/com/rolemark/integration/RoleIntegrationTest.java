package com.rolemark.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rolemark.dto.AuthResponse;
import com.rolemark.dto.RoleRequest;
import com.rolemark.dto.SignupRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:roletestdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "jwt.secret=test-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm-test"
})
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RoleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String userAToken;
    private String userBToken;

    @BeforeEach
    void setUp() throws Exception {
        // Create user A
        SignupRequest signupA = new SignupRequest("usera@example.com", "password123");
        String signupResponseA = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupA)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        userAToken = objectMapper.readValue(signupResponseA, AuthResponse.class).getToken();

        // Create user B
        SignupRequest signupB = new SignupRequest("userb@example.com", "password123");
        String signupResponseB = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupB)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        userBToken = objectMapper.readValue(signupResponseB, AuthResponse.class).getToken();

        // Create a role for user A (not used in tests but ensures user A has data)
        RoleRequest roleRequest = new RoleRequest("Software Engineer", "Job description here");
        mockMvc.perform(post("/api/roles")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleRequest)))
                .andExpect(status().isCreated());
    }

    @Test
    void unauthorizedGetRolesReturns401() throws Exception {
        // Test 2: unauthorized GET /api/roles returns 401
        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.correlationId").exists())
                .andExpect(jsonPath("$.path").value("/api/roles"));
    }

    @Test
    void userACannotAccessUserBRole() throws Exception {
        // Test 3: ownership enforced: user A cannot read/update/delete user B role (404)

        // User B creates a role
        RoleRequest roleRequestB = new RoleRequest("Product Manager", "PM job description");
        String roleResponseB = mockMvc.perform(post("/api/roles")
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleRequestB)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID userBRoleId = UUID.fromString(objectMapper.readTree(roleResponseB).get("id").asText());

        // User A tries to read user B's role - should get 404
        mockMvc.perform(get("/api/roles/" + userBRoleId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.correlationId").exists())
                .andExpect(jsonPath("$.path").exists());

        // User A tries to update user B's role - should get 404
        RoleRequest updateRequest = new RoleRequest("Updated Title", "Updated description");
        mockMvc.perform(put("/api/roles/" + userBRoleId)
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.correlationId").exists())
                .andExpect(jsonPath("$.path").exists());

        // User A tries to delete user B's role - should get 404
        mockMvc.perform(delete("/api/roles/" + userBRoleId)
                        .header("Authorization", "Bearer " + userAToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.correlationId").exists())
                .andExpect(jsonPath("$.path").exists());
    }

    @Test
    void validationErrorReturns400WithRequiredShape() throws Exception {
        // Test 4: validation error returns 400 with required JSON shape (including correlationId + path)
        RoleRequest invalidRequest = new RoleRequest("", ""); // Empty title and description should fail validation

        mockMvc.perform(post("/api/roles")
                        .header("Authorization", "Bearer " + userAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.correlationId").exists())
                .andExpect(jsonPath("$.path").value("/api/roles"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}

