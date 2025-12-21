package com.rolemark;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rolemark.dto.AuthResponse;
import com.rolemark.dto.CompareResponse;
import com.rolemark.dto.CreateCriterionRequest;
import com.rolemark.dto.EvaluationResponse;
import com.rolemark.dto.LoginRequest;
import com.rolemark.dto.RoleRequest;
import com.rolemark.dto.RoleResponse;
import com.rolemark.dto.SignupRequest;
import com.rolemark.entity.Resume;
import com.rolemark.exception.GlobalExceptionHandler;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class EvaluationIT extends AbstractIntegrationTest {
    
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
    
    // Helper: Create a criterion
    private Long createCriterion(String jwtToken, Long roleId, String name, Integer weight, List<String> keywords) throws Exception {
        CreateCriterionRequest request = new CreateCriterionRequest();
        request.setName(name);
        request.setWeight(weight);
        request.setKeywords(keywords);
        
        MvcResult result = mockMvc.perform(post("/api/roles/" + roleId + "/criteria")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        
        com.rolemark.dto.CriterionResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), com.rolemark.dto.CriterionResponse.class);
        return response.getId();
    }
    
    // PDF Factory: Generate a PDF with specific text content
    private byte[] createPdfWithText(String text) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                contentStream.setFont(font, 12);
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText(text);
                contentStream.endText();
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }
    
    // Helper: Upload a resume and return its ID
    private Long uploadResume(String jwtToken, String filename, String textContent, Long roleId) throws Exception {
        byte[] pdfBytes = createPdfWithText(textContent);
        MockMultipartFile file = new MockMultipartFile(
                "file", filename, "application/pdf", pdfBytes);
        
        MvcResult result = mockMvc.perform(multipart("/api/resumes")
                        .file(file)
                        .param("roleId", roleId != null ? roleId.toString() : "")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isCreated())
                .andReturn();
        
        Resume resume = objectMapper.readValue(
                result.getResponse().getContentAsString(), Resume.class);
        return resume.getId();
    }
    
    @Test
    void testEvaluateHappyPathWithMatchingKeyword() throws Exception {
        // Register and login user
        String email = "test1@example.com";
        String password = "password123";
        String jwt = registerUser(email, password);
        
        // Create a role
        Long roleId = createRole(jwt, "Software Engineer", "Job description");
        
        // Create a criterion with keywords
        Long criterionId = createCriterion(jwt, roleId, "Java Experience", 50, List.of("Java", "Spring", "Hibernate"));
        
        // Upload a resume with matching keyword
        Long resumeId = uploadResume(jwt, "resume.pdf", "I have extensive Java experience with Spring framework", roleId);
        
        // Evaluate the resume
        MvcResult result = mockMvc.perform(post("/api/roles/" + roleId + "/evaluate/" + resumeId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andReturn();
        
        EvaluationResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), EvaluationResponse.class);
        
        // Assertions
        assertEquals(roleId, response.getRoleId());
        assertEquals(resumeId, response.getResumeId());
        assertEquals(50, response.getTotalScore()); // Should equal the weight
        assertNotNull(response.getBreakdown());
        assertNotNull(response.getCreatedAt());
        
        // Check breakdown contains the criterion with matched keywords
        Map<String, Object> criterionBreakdown = (Map<String, Object>) response.getBreakdown().get(criterionId.toString());
        assertNotNull(criterionBreakdown);
        assertEquals(50, criterionBreakdown.get("points"));
        @SuppressWarnings("unchecked")
        List<String> matched = (List<String>) criterionBreakdown.get("matched");
        assertTrue(matched.contains("Java") || matched.contains("Spring"));
    }
    
    @Test
    void testEvaluateNoMatchReturnsScoreZero() throws Exception {
        // Register and login user
        String email = "test2@example.com";
        String password = "password123";
        String jwt = registerUser(email, password);
        
        // Create a role
        Long roleId = createRole(jwt, "Software Engineer", "Job description");
        
        // Create a criterion with keywords
        Long criterionId = createCriterion(jwt, roleId, "Python Experience", 50, List.of("Python", "Django", "Flask"));
        
        // Upload a resume without matching keywords
        Long resumeId = uploadResume(jwt, "resume.pdf", "I have extensive Java experience with Spring framework", roleId);
        
        // Evaluate the resume
        MvcResult result = mockMvc.perform(post("/api/roles/" + roleId + "/evaluate/" + resumeId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andReturn();
        
        EvaluationResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), EvaluationResponse.class);
        
        // Assertions
        assertEquals(0, response.getTotalScore()); // No match should return 0
        assertNotNull(response.getBreakdown());
        
        // Check breakdown contains the criterion with 0 points and empty matched list
        Map<String, Object> criterionBreakdown = (Map<String, Object>) response.getBreakdown().get(criterionId.toString());
        assertNotNull(criterionBreakdown);
        assertEquals(0, criterionBreakdown.get("points"));
        @SuppressWarnings("unchecked")
        List<String> matched = (List<String>) criterionBreakdown.get("matched");
        assertTrue(matched.isEmpty());
    }
    
    @Test
    void testEvaluateCrossUserAccessReturns403() throws Exception {
        // Register and login user A
        String emailA = "userA@example.com";
        String passwordA = "password123";
        String jwtA = registerUser(emailA, passwordA);
        
        // Register and login user B
        String emailB = "userB@example.com";
        String passwordB = "password123";
        String jwtB = registerUser(emailB, passwordB);
        
        // User A creates a role
        Long roleIdA = createRole(jwtA, "Role A", "Job description");
        
        // User A creates a resume
        Long resumeIdA = uploadResume(jwtA, "resume.pdf", "Resume content", roleIdA);
        
        // User B creates a role
        Long roleIdB = createRole(jwtB, "Role B", "Job description");
        
        // User B creates a resume
        Long resumeIdB = uploadResume(jwtB, "resume.pdf", "Resume content", roleIdB);
        
        // User B attempts to evaluate User A's resume against User A's role -> should return 403
        MvcResult result1 = mockMvc.perform(post("/api/roles/" + roleIdA + "/evaluate/" + resumeIdA)
                        .header("Authorization", "Bearer " + jwtB))
                .andExpect(status().isForbidden())
                .andReturn();
        
        GlobalExceptionHandler.ErrorResponse error1 = objectMapper.readValue(
                result1.getResponse().getContentAsString(), GlobalExceptionHandler.ErrorResponse.class);
        assertEquals(403, error1.getStatus());
        
        // User B attempts to evaluate User B's resume against User A's role -> should return 403
        MvcResult result2 = mockMvc.perform(post("/api/roles/" + roleIdA + "/evaluate/" + resumeIdB)
                        .header("Authorization", "Bearer " + jwtB))
                .andExpect(status().isForbidden())
                .andReturn();
        
        GlobalExceptionHandler.ErrorResponse error2 = objectMapper.readValue(
                result2.getResponse().getContentAsString(), GlobalExceptionHandler.ErrorResponse.class);
        assertEquals(403, error2.getStatus());
        
        // User B attempts to evaluate User A's resume against User B's role -> should return 403
        MvcResult result3 = mockMvc.perform(post("/api/roles/" + roleIdB + "/evaluate/" + resumeIdA)
                        .header("Authorization", "Bearer " + jwtB))
                .andExpect(status().isForbidden())
                .andReturn();
        
        GlobalExceptionHandler.ErrorResponse error3 = objectMapper.readValue(
                result3.getResponse().getContentAsString(), GlobalExceptionHandler.ErrorResponse.class);
        assertEquals(403, error3.getStatus());
    }
    
    @Test
    void testCompareRankingOrdersByScoreDesc() throws Exception {
        // Register and login user
        String email = "test3@example.com";
        String password = "password123";
        String jwt = registerUser(email, password);
        
        // Create a role
        Long roleId = createRole(jwt, "Software Engineer", "Job description");
        
        // Create criteria
        createCriterion(jwt, roleId, "Java Experience", 50, List.of("Java", "Spring"));
        createCriterion(jwt, roleId, "Python Experience", 30, List.of("Python", "Django"));
        
        // Upload resume 1 with Java keyword (should score 50)
        Long resumeId1 = uploadResume(jwt, "resume1.pdf", "I have extensive Java experience", roleId);
        
        // Upload resume 2 with Python keyword (should score 30)
        Long resumeId2 = uploadResume(jwt, "resume2.pdf", "I have extensive Python experience", roleId);
        
        // Upload resume 3 with both keywords (should score 80)
        Long resumeId3 = uploadResume(jwt, "resume3.pdf", "I have extensive Java and Python experience", roleId);
        
        // Evaluate all resumes
        mockMvc.perform(post("/api/roles/" + roleId + "/evaluate/" + resumeId1)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());
        
        mockMvc.perform(post("/api/roles/" + roleId + "/evaluate/" + resumeId2)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());
        
        mockMvc.perform(post("/api/roles/" + roleId + "/evaluate/" + resumeId3)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());
        
        // Compare results
        MvcResult result = mockMvc.perform(get("/api/roles/" + roleId + "/compare")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andReturn();
        
        CompareResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), CompareResponse.class);
        
        // Assertions: should be ordered by totalScore desc
        assertNotNull(response.getResults());
        assertEquals(3, response.getResults().size());
        
        // First should be resume 3 (score 80)
        assertEquals(resumeId3, response.getResults().get(0).getResumeId());
        assertEquals(80, response.getResults().get(0).getTotalScore());
        
        // Second should be resume 1 (score 50)
        assertEquals(resumeId1, response.getResults().get(1).getResumeId());
        assertEquals(50, response.getResults().get(1).getTotalScore());
        
        // Third should be resume 2 (score 30)
        assertEquals(resumeId2, response.getResults().get(2).getResumeId());
        assertEquals(30, response.getResults().get(2).getTotalScore());
    }
    
    @Test
    void testReEvaluateSameResumeOverwritesResult() throws Exception {
        // Register and login user
        String email = "test4@example.com";
        String password = "password123";
        String jwt = registerUser(email, password);
        
        // Create a role
        Long roleId = createRole(jwt, "Software Engineer", "Job description");
        
        // Create a criterion
        Long criterionId = createCriterion(jwt, roleId, "Java Experience", 50, List.of("Java", "Spring"));
        
        // Upload a resume
        Long resumeId = uploadResume(jwt, "resume.pdf", "I have extensive Java experience", roleId);
        
        // First evaluation
        MvcResult result1 = mockMvc.perform(post("/api/roles/" + roleId + "/evaluate/" + resumeId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andReturn();
        
        EvaluationResponse response1 = objectMapper.readValue(
                result1.getResponse().getContentAsString(), EvaluationResponse.class);
        assertEquals(50, response1.getTotalScore());
        
        // Update the criterion to have different keywords
        // First delete the old criterion
        mockMvc.perform(delete("/api/criteria/" + criterionId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNoContent());
        
        // Create a new criterion with different keywords
        Long newCriterionId = createCriterion(jwt, roleId, "Python Experience", 30, List.of("Python", "Django"));
        
        // Re-evaluate the same resume
        MvcResult result2 = mockMvc.perform(post("/api/roles/" + roleId + "/evaluate/" + resumeId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andReturn();
        
        EvaluationResponse response2 = objectMapper.readValue(
                result2.getResponse().getContentAsString(), EvaluationResponse.class);
        assertEquals(0, response2.getTotalScore()); // No Python keywords in resume
        
        // Verify only one result exists per (userId, roleId, resumeId) by checking compare
        MvcResult compareResult = mockMvc.perform(get("/api/roles/" + roleId + "/compare")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andReturn();
        
        CompareResponse compareResponse = objectMapper.readValue(
                compareResult.getResponse().getContentAsString(), CompareResponse.class);
        
        // Should have exactly one result for this resume
        long count = compareResponse.getResults().stream()
                .filter(r -> r.getResumeId().equals(resumeId))
                .count();
        assertEquals(1, count, "Should have exactly one evaluation result per (userId, roleId, resumeId)");
    }
    
    @Test
    void testEvaluateWithNoCriteriaReturnsScoreZero() throws Exception {
        // Register and login user
        String email = "test5@example.com";
        String password = "password123";
        String jwt = registerUser(email, password);
        
        // Create a role without criteria
        Long roleId = createRole(jwt, "Software Engineer", "Job description");
        
        // Upload a resume
        Long resumeId = uploadResume(jwt, "resume.pdf", "I have extensive Java experience", roleId);
        
        // Evaluate the resume (role has no criteria)
        MvcResult result = mockMvc.perform(post("/api/roles/" + roleId + "/evaluate/" + resumeId)
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andReturn();
        
        EvaluationResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), EvaluationResponse.class);
        
        // Should return 200 with score 0 and empty breakdown
        assertEquals(0, response.getTotalScore());
        assertNotNull(response.getBreakdown());
        assertTrue(response.getBreakdown().isEmpty());
    }
}

