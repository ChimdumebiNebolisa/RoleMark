package com.rolemark.service;

import com.rolemark.entity.ExtractedSignal;
import com.rolemark.entity.Resume;
import com.rolemark.exception.AccessDeniedException;
import com.rolemark.exception.NotFoundException;
import com.rolemark.repository.ExtractedSignalRepository;
import com.rolemark.repository.ResumeRepository;
import com.rolemark.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;

@Service
public class ResumeService {
    
    private static final Logger logger = LoggerFactory.getLogger(ResumeService.class);
    
    private final ResumeRepository resumeRepository;
    private final ExtractedSignalRepository extractedSignalRepository;
    private final PdfExtractionService pdfExtractionService;
    private final ResumeParserService resumeParserService;
    private final RoleRepository roleRepository;
    
    public ResumeService(ResumeRepository resumeRepository,
                        ExtractedSignalRepository extractedSignalRepository,
                        PdfExtractionService pdfExtractionService,
                        ResumeParserService resumeParserService,
                        RoleRepository roleRepository) {
        this.resumeRepository = resumeRepository;
        this.extractedSignalRepository = extractedSignalRepository;
        this.pdfExtractionService = pdfExtractionService;
        this.resumeParserService = resumeParserService;
        this.roleRepository = roleRepository;
    }
    
    @Transactional
    public Resume uploadResume(UUID userId, MultipartFile file, Long roleId) throws IOException {
        // Validate role ownership if roleId is provided
        if (roleId != null) {
            boolean roleExists = roleRepository.findByIdAndUserId(roleId, userId).isPresent();
            if (!roleExists) {
                throw new AccessDeniedException("Role not found or does not belong to user");
            }
        }
        
        // Extract text from PDF
        String extractedText;
        try {
            extractedText = pdfExtractionService.extractText(file);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to extract text from PDF: " + e.getMessage());
        }
        
        // Compute SHA256 checksum
        String checksum = computeSHA256(file.getBytes());
        
        // Create resume entity (metadata-first)
        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setRoleId(roleId);
        resume.setFilename(file.getOriginalFilename());
        resume.setContentType(file.getContentType());
        resume.setFileSize(file.getSize());
        resume.setChecksumSha256(checksum);
        resume.setRawExtractedText(extractedText);
        // storagePath is optional - not setting it for MVP
        
        resume = resumeRepository.save(resume);
        
        logger.info("Resume uploaded: id={}, filename={}, roleId={}, userId={}", 
                resume.getId(), file.getOriginalFilename(), roleId, userId);
        
        // Parse and extract signals
        List<ExtractedSignal> signals = resumeParserService.parseResume(extractedText);
        for (ExtractedSignal signal : signals) {
            signal.setResumeId(resume.getId());
            extractedSignalRepository.save(signal);
        }
        
        return resume;
    }
    
    public List<Resume> getAllResumes(UUID userId) {
        return resumeRepository.findByUserId(userId);
    }
    
    public Resume getResumeById(UUID userId, Long resumeId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new NotFoundException("Resume not found"));
        
        // Return 403 if resume exists but doesn't belong to user
        if (!resume.getUserId().equals(userId)) {
            throw new AccessDeniedException("Access denied: Resume does not belong to user");
        }
        
        return resume;
    }
    
    @Transactional
    public void deleteResume(UUID userId, Long resumeId) {
        Resume resume = resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new NotFoundException("Resume not found"));
        
        // Delete associated signals first
        List<ExtractedSignal> signals = extractedSignalRepository.findByResumeId(resumeId);
        extractedSignalRepository.deleteAll(signals);
        
        resumeRepository.delete(resume);
    }
    
    private String computeSHA256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}

