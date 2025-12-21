package com.rolemark.controller;

import com.rolemark.entity.Resume;
import com.rolemark.service.ResumeService;
import com.rolemark.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {
    
    private static final Logger logger = LoggerFactory.getLogger(ResumeController.class);
    
    private final ResumeService resumeService;
    
    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }
    
    @PostMapping
    public ResponseEntity<Resume> uploadResume(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "roleId", required = false) Long roleId) throws IOException {
        UUID userId = SecurityUtil.getCurrentUserId();
        logger.info("Uploading resume: filename={}, roleId={}, userId={}", 
                file.getOriginalFilename(), roleId, userId);
        Resume resume = resumeService.uploadResume(userId, file, roleId);
        return ResponseEntity.status(HttpStatus.CREATED).body(resume);
    }
    
    @GetMapping
    public ResponseEntity<List<Resume>> getAllResumes() {
        UUID userId = SecurityUtil.getCurrentUserId();
        logger.info("Fetching all resumes for userId={}", userId);
        List<Resume> resumes = resumeService.getAllResumes(userId);
        return ResponseEntity.ok(resumes);
    }
    
    @GetMapping("/{resumeId}")
    public ResponseEntity<Resume> getResumeById(@PathVariable Long resumeId) {
        UUID userId = SecurityUtil.getCurrentUserId();
        logger.info("Fetching resume: id={}, userId={}", resumeId, userId);
        Resume resume = resumeService.getResumeById(userId, resumeId);
        return ResponseEntity.ok(resume);
    }
    
    @DeleteMapping("/{resumeId}")
    public ResponseEntity<Void> deleteResume(@PathVariable Long resumeId) {
        UUID userId = SecurityUtil.getCurrentUserId();
        resumeService.deleteResume(userId, resumeId);
        return ResponseEntity.noContent().build();
    }
}

