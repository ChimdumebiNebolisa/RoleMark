package com.rolemark.controller;

import com.rolemark.entity.Resume;
import com.rolemark.service.ResumeService;
import com.rolemark.util.SecurityUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {
    
    private final ResumeService resumeService;
    
    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }
    
    @PostMapping
    public ResponseEntity<Resume> uploadResume(@RequestParam("file") MultipartFile file) throws IOException {
        Long userId = SecurityUtil.getCurrentUserId();
        Resume resume = resumeService.uploadResume(userId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(resume);
    }
    
    @GetMapping
    public ResponseEntity<List<Resume>> getAllResumes() {
        Long userId = SecurityUtil.getCurrentUserId();
        List<Resume> resumes = resumeService.getAllResumes(userId);
        return ResponseEntity.ok(resumes);
    }
    
    @GetMapping("/{resumeId}")
    public ResponseEntity<Resume> getResumeById(@PathVariable Long resumeId) {
        Long userId = SecurityUtil.getCurrentUserId();
        Resume resume = resumeService.getResumeById(userId, resumeId);
        return ResponseEntity.ok(resume);
    }
    
    @DeleteMapping("/{resumeId}")
    public ResponseEntity<Void> deleteResume(@PathVariable Long resumeId) {
        Long userId = SecurityUtil.getCurrentUserId();
        resumeService.deleteResume(userId, resumeId);
        return ResponseEntity.noContent().build();
    }
}

