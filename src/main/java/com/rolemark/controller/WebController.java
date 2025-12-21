package com.rolemark.controller;

import com.rolemark.dto.*;
import com.rolemark.entity.*;
import com.rolemark.service.*;
import com.rolemark.util.SecurityUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class WebController {
    
    private final RoleService roleService;
    private final CriterionService criterionService;
    private final ResumeService resumeService;
    private final EvaluationService evaluationService;
    
    public WebController(RoleService roleService, CriterionService criterionService,
                        ResumeService resumeService, EvaluationService evaluationService) {
        this.roleService = roleService;
        this.criterionService = criterionService;
        this.resumeService = resumeService;
        this.evaluationService = evaluationService;
    }
    
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }
    
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        UUID userId = SecurityUtil.getCurrentUserId();
        List<RoleResponse> roles = roleService.getAllRoles(userId);
        List<Resume> resumes = resumeService.getAllResumes(userId);
        List<Evaluation> evaluations = evaluationService.getAllEvaluations(userId);
        
        model.addAttribute("roles", roles);
        model.addAttribute("resumes", resumes);
        model.addAttribute("evaluations", evaluations);
        return "dashboard";
    }
    
    @GetMapping("/roles/new")
    public String newRoleForm(Model model) {
        model.addAttribute("role", new RoleRequest());
        return "role-form";
    }
    
    @PostMapping("/roles")
    public String createRole(@ModelAttribute RoleRequest request, RedirectAttributes redirectAttributes) {
        UUID userId = SecurityUtil.getCurrentUserId();
        RoleResponse role = roleService.createRole(userId, request);
        redirectAttributes.addFlashAttribute("message", "Role created successfully");
        return "redirect:/roles/" + role.getId() + "/criteria";
    }
    
    @GetMapping("/roles/{roleId}/criteria")
    public String criteriaPage(@PathVariable Long roleId, Model model) {
        UUID userId = SecurityUtil.getCurrentUserId();
        RoleResponse role = roleService.getRoleById(userId, roleId);
        List<CriterionResponse> criteria = criterionService.getAllCriteria(userId, roleId);
        
        int totalWeight = criteria.stream().mapToInt(CriterionResponse::getWeight).sum();
        
        model.addAttribute("role", role);
        model.addAttribute("criteria", criteria);
        model.addAttribute("totalWeight", totalWeight);
        model.addAttribute("criterion", new CriterionRequest());
        return "criteria-form";
    }
    
    @PostMapping("/roles/{roleId}/criteria")
    public String createCriterion(@PathVariable Long roleId, @ModelAttribute CriterionRequest request,
                                 RedirectAttributes redirectAttributes) {
        UUID userId = SecurityUtil.getCurrentUserId();
        criterionService.createCriterion(userId, roleId, request);
        redirectAttributes.addFlashAttribute("message", "Criterion added successfully");
        return "redirect:/roles/" + roleId + "/criteria";
    }
    
    @GetMapping("/resumes/upload")
    public String uploadResumeForm() {
        return "resume-upload";
    }
    
    @PostMapping("/resumes")
    public String uploadResume(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        try {
            UUID userId = SecurityUtil.getCurrentUserId();
            resumeService.uploadResume(userId, file);
            redirectAttributes.addFlashAttribute("message", "Resume uploaded successfully");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Failed to upload resume: " + e.getMessage());
        }
        return "redirect:/dashboard";
    }
    
    @GetMapping("/evaluations/new")
    public String newEvaluationForm(Model model) {
        UUID userId = SecurityUtil.getCurrentUserId();
        List<RoleResponse> roles = roleService.getAllRoles(userId);
        List<Resume> resumes = resumeService.getAllResumes(userId);
        
        model.addAttribute("roles", roles);
        model.addAttribute("resumes", resumes);
        model.addAttribute("evaluation", new EvaluationRequest());
        return "evaluation-form";
    }
    
    @PostMapping("/evaluations")
    public String createEvaluation(@ModelAttribute EvaluationRequest request, RedirectAttributes redirectAttributes) {
        UUID userId = SecurityUtil.getCurrentUserId();
        Evaluation evaluation = evaluationService.createEvaluation(userId, request.getRoleId(), request.getResumeIds());
        redirectAttributes.addFlashAttribute("message", "Evaluation created successfully");
        return "redirect:/evaluations/" + evaluation.getId() + "/run";
    }
    
    @PostMapping("/evaluations/{evaluationId}/run")
    public String runEvaluation(@PathVariable Long evaluationId, RedirectAttributes redirectAttributes) {
        UUID userId = SecurityUtil.getCurrentUserId();
        try {
            evaluationService.runEvaluation(userId, evaluationId);
            redirectAttributes.addFlashAttribute("message", "Evaluation completed successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Evaluation failed: " + e.getMessage());
        }
        return "redirect:/evaluations/" + evaluationId + "/results";
    }
    
    @GetMapping("/evaluations/{evaluationId}/results")
    public String evaluationResults(@PathVariable Long evaluationId, Model model) {
        UUID userId = SecurityUtil.getCurrentUserId();
        Evaluation evaluation = evaluationService.getEvaluationById(userId, evaluationId);
        List<Map<String, Object>> results = evaluationService.getEvaluationResults(userId, evaluationId);
        
        model.addAttribute("evaluation", evaluation);
        model.addAttribute("results", results);
        return "evaluation-results";
    }
    
    @GetMapping("/evaluations/{evaluationId}/compare")
    public String compareResumes(@PathVariable Long evaluationId,
                                @RequestParam Long leftResumeId,
                                @RequestParam Long rightResumeId,
                                Model model) {
        UUID userId = SecurityUtil.getCurrentUserId();
        Map<String, Object> comparison = evaluationService.compareResumes(
                userId, evaluationId, leftResumeId, rightResumeId);
        
        model.addAttribute("comparison", comparison);
        return "compare";
    }
}

