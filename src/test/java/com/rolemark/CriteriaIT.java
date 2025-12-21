package com.rolemark;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rolemark.dto.AuthResponse;
import com.rolemark.dto.CreateCriterionRequest;
import com.rolemark.dto.CriterionResponse;
import com.rolemark.dto.LoginRequest;
import com.rolemark.dto.RoleRequest;
import com.rolemark.dto.RoleResponse;
import com.rolemark.dto.SignupRequest;
import com.rolemark.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class CriteriaIT extends AbstractIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // Helper: Register a new user
    private String registerUser(String email, String password) throws Exception {
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setEmail(email);
        signupRequest.setPassword(password);
        
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isOk())
                .andReturn();
        
        AuthResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);
        return response.getToken();
    }
    
    // Helper: Login and get JWT token
    private String loginAndGetJwt(String email, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);
        
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        
        AuthResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);
        return response.getToken();
    }
    
    // Helper: Create a role and return its ID
    private Long createRole(String jwtToken, String title, String jobDescription) throws Exception {
        RoleRequest roleRequest = new RoleRequest();
        roleRequest.setTitle(title);
        roleRequest.setJobDescription(jobDescription);
        
        MvcResult result = mockMvc.perform(post("/api/roles")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roleRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        
        RoleResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), RoleResponse.class);
        return response.getId();
    }
    
    @Test
    void testCreateAndListCriteriaHappyPath() throws Exception {
        // Register and login user
        String email = "test1@example.com";
        String password = "password123";
        String jwt = registerUser(email, password);
        
        // Create a role
        Long roleId = createRole(jwt, "Software Engineer", "Job description");
        
        // Create a criterion
        CreateCriterionRequest request = new CreateCriterionRequest();
        request.setName("Java Experience");
        request.setWeight(50);
        request.setKeywords(List.of("Java", "Spring", "Hibernate"));
        
        MvcResult createResult = mockMvc.perform(post("/api/roles/" + roleId + "/criteria")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        
        CriterionResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), CriterionResponse.class);
        assertNotNull(created.getId());
        assertEquals(roleId, created.getRoleId());
        assertEquals("Java Experience", created.getName());
        assertEquals(50, created.getWeight());
        assertEquals(3, created.getKeywords().size());
        assertTrue(created.getKeywords().contains("Java"));
        
        // List criteria
        MvcResult listResult = mockMvc.perform(get("/api/roles/" + roleId + "/criteria")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andReturn();
        
        List<CriterionResponse> criteria = objectMapper.readValue(
                listResult.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, CriterionResponse.class));
        assertEquals(1, criteria.size());
        assertEquals(created.getId(), criteria.get(0).getId());
    }
    
    @Test
    void testCreateCriteria403WhenRoleBelongsToAnotherUser() throws Exception {
        // Register and login user A
        String emailA = "userA@example.com";
        String passwordA = "password123";
        String jwtA = registerUser(emailA, passwordA);
        
        // Register and login user B
        String emailB = "userB@example.com";
        String passwordB = "password123";
        String jwtB = registerUser(emailB, passwordB);
        
        // User A creates a role
        Long roleIdA = createRole(jwtA, "Role A", "Job description for Role A");
        
        // User B attempts to create criteria under User A's role -> should return 403
        CreateCriterionRequest request = new CreateCriterionRequest();
        request.setName("Test Criterion");
        request.setWeight(50);
        request.setKeywords(List.of("test"));
        
        MvcResult result = mockMvc.perform(post("/api/roles/" + roleIdA + "/criteria")
                        .header("Authorization", "Bearer " + jwtB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andReturn();
        
        GlobalExceptionHandler.ErrorResponse error = objectMapper.readValue(
                result.getResponse().getContentAsString(), GlobalExceptionHandler.ErrorResponse.class);
        assertEquals(403, error.getStatus());
        assertTrue(error.getMessage().contains("does not belong") || 
                   error.getMessage().contains("Role"));
    }
    
    @Test
    void testCreateCriteria409OnDuplicateName() throws Exception {
        // Register and login user
        String email = "test2@example.com";
        String password = "password123";
        String jwt = registerUser(email, password);
        
        // Create a role
        Long roleId = createRole(jwt, "Software Engineer", "Job description");
        
        // Create first criterion
        CreateCriterionRequest request1 = new CreateCriterionRequest();
        request1.setName("Java Experience");
        request1.setWeight(50);
        request1.setKeywords(List.of("Java"));
        
        mockMvc.perform(post("/api/roles/" + roleId + "/criteria")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());
        
        // Attempt to create duplicate criterion with same name -> should return 409
        CreateCriterionRequest request2 = new CreateCriterionRequest();
        request2.setName("Java Experience");
        request2.setWeight(30);
        request2.setKeywords(List.of("Spring"));
        
        MvcResult result = mockMvc.perform(post("/api/roles/" + roleId + "/criteria")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict())
                .andReturn();
        
        GlobalExceptionHandler.ErrorResponse error = objectMapper.readValue(
                result.getResponse().getContentAsString(), GlobalExceptionHandler.ErrorResponse.class);
        assertEquals(409, error.getStatus());
        assertTrue(error.getMessage().contains("already exists") || 
                   error.getMessage().contains("duplicate"));
    }
    
    @Test
    void testDeleteCriterionReturns204() throws Exception {
        // Register and login user
        String email = "test3@example.com";
        String password = "password123";
        String jwt = registerUser(email, password);
        
        // Create a role
        Long roleId = createRole(jwt, "Software Engineer", "Job description");
        
        // Create a criterion
        CreateCriterionRequest request = new CreateCriterionRequest();
        request.setName("Java Experience");
        request.setWeight(50);
        request.setKeywords(List.of("Java"));
        
        MvcResult createResult = mockMvc.perform(post("/api/roles/" + roleId + "/criteria")
                        .header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        
        CriterionResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), CriterionResponse.class);
        Long criterionId = created.getId();
        
        // Delete criterion -> should return 204
        mockMvc.perform(delete("/api/criteria/" + criterionId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNoContent());
        
        // Verify criterion is no longer in the list
        MvcResult listResult = mockMvc.perform(get("/api/roles/" + roleId + "/criteria")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andReturn();
        
        List<CriterionResponse> criteria = objectMapper.readValue(
                listResult.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, CriterionResponse.class));
        assertEquals(0, criteria.size());
    }
    
    @Test
    void testDeleteCriterion404WhenNotFound() throws Exception {
        // Register and login user
        String email = "test4@example.com";
        String password = "password123";
        String jwt = registerUser(email, password);
        
        // Attempt to delete non-existent criterion -> should return 404
        Long nonExistentId = 999999999L;
        MvcResult result = mockMvc.perform(delete("/api/criteria/" + nonExistentId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNotFound())
                .andReturn();
        
        GlobalExceptionHandler.ErrorResponse error = objectMapper.readValue(
                result.getResponse().getContentAsString(), GlobalExceptionHandler.ErrorResponse.class);
        assertEquals(404, error.getStatus());
        assertTrue(error.getMessage().contains("not found"));
    }
    
    @Test
    void testListCriteriaOnlyReturnsCallersCriteria() throws Exception {
        // Register and login user A
        String emailA = "userC@example.com";
        String passwordA = "password123";
        String jwtA = registerUser(emailA, passwordA);
        
        // Register and login user B
        String emailB = "userD@example.com";
        String passwordB = "password123";
        String jwtB = registerUser(emailB, passwordB);
        
        // User A creates a role and criterion
        Long roleIdA = createRole(jwtA, "Role A", "Job description");
        CreateCriterionRequest requestA = new CreateCriterionRequest();
        requestA.setName("Criterion A");
        requestA.setWeight(50);
        requestA.setKeywords(List.of("test"));
        
        mockMvc.perform(post("/api/roles/" + roleIdA + "/criteria")
                        .header("Authorization", "Bearer " + jwtA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestA)))
                .andExpect(status().isCreated());
        
        // User B creates a role and criterion
        Long roleIdB = createRole(jwtB, "Role B", "Job description");
        CreateCriterionRequest requestB = new CreateCriterionRequest();
        requestB.setName("Criterion B");
        requestB.setWeight(50);
        requestB.setKeywords(List.of("test"));
        
        mockMvc.perform(post("/api/roles/" + roleIdB + "/criteria")
                        .header("Authorization", "Bearer " + jwtB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestB)))
                .andExpect(status().isCreated());
        
        // User A should see only their criterion
        MvcResult resultA = mockMvc.perform(get("/api/roles/" + roleIdA + "/criteria")
                        .header("Authorization", "Bearer " + jwtA))
                .andExpect(status().isOk())
                .andReturn();
        
        List<CriterionResponse> criteriaA = objectMapper.readValue(
                resultA.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, CriterionResponse.class));
        assertEquals(1, criteriaA.size());
        assertEquals("Criterion A", criteriaA.get(0).getName());
        
        // User B should see only their criterion
        MvcResult resultB = mockMvc.perform(get("/api/roles/" + roleIdB + "/criteria")
                        .header("Authorization", "Bearer " + jwtB))
                .andExpect(status().isOk())
                .andReturn();
        
        List<CriterionResponse> criteriaB = objectMapper.readValue(
                resultB.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, CriterionResponse.class));
        assertEquals(1, criteriaB.size());
        assertEquals("Criterion B", criteriaB.get(0).getName());
    }
}

