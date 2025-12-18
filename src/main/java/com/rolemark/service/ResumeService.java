package com.rolemark.service;

import com.rolemark.entity.ExtractedSignal;
import com.rolemark.entity.Resume;
import com.rolemark.repository.ExtractedSignalRepository;
import com.rolemark.repository.ResumeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Service
public class ResumeService {
    
    private final ResumeRepository resumeRepository;
    private final ExtractedSignalRepository extractedSignalRepository;
    private final PdfExtractionService pdfExtractionService;
    private final ResumeParserService resumeParserService;
    
    public ResumeService(ResumeRepository resumeRepository,
                        ExtractedSignalRepository extractedSignalRepository,
                        PdfExtractionService pdfExtractionService,
                        ResumeParserService resumeParserService) {
        this.resumeRepository = resumeRepository;
        this.extractedSignalRepository = extractedSignalRepository;
        this.pdfExtractionService = pdfExtractionService;
        this.resumeParserService = resumeParserService;
    }
    
    @Transactional
    public Resume uploadResume(Long userId, MultipartFile file) throws IOException {
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
        resume.setFilename(file.getOriginalFilename());
        resume.setContentType(file.getContentType());
        resume.setFileSize(file.getSize());
        resume.setChecksumSha256(checksum);
        resume.setRawExtractedText(extractedText);
        // storagePath is optional - not setting it for MVP
        
        resume = resumeRepository.save(resume);
        
        // Parse and extract signals
        List<ExtractedSignal> signals = resumeParserService.parseResume(extractedText);
        for (ExtractedSignal signal : signals) {
            signal.setResumeId(resume.getId());
            extractedSignalRepository.save(signal);
        }
        
        return resume;
    }
    
    public List<Resume> getAllResumes(Long userId) {
        return resumeRepository.findByUserId(userId);
    }
    
    public Resume getResumeById(Long userId, Long resumeId) {
        return resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Resume not found"));
    }
    
    @Transactional
    public void deleteResume(Long userId, Long resumeId) {
        Resume resume = resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Resume not found"));
        
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

