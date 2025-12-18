package com.rolemark.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rolemark.entity.*;
import com.rolemark.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EvaluationService {
    
    private final EvaluationRepository evaluationRepository;
    private final EvaluationSnapshotRepository snapshotRepository;
    private final EvaluationCandidateRepository candidateRepository;
    private final ScoreBreakdownRepository breakdownRepository;
    private final RoleRepository roleRepository;
    private final CriterionRepository criterionRepository;
    private final ResumeRepository resumeRepository;
    private final ScoringService scoringService;
    private final CriterionService criterionService;
    private final ObjectMapper objectMapper;
    
    public EvaluationService(EvaluationRepository evaluationRepository,
                            EvaluationSnapshotRepository snapshotRepository,
                            EvaluationCandidateRepository candidateRepository,
                            ScoreBreakdownRepository breakdownRepository,
                            RoleRepository roleRepository,
                            CriterionRepository criterionRepository,
                            ResumeRepository resumeRepository,
                            ScoringService scoringService,
                            CriterionService criterionService) {
        this.evaluationRepository = evaluationRepository;
        this.snapshotRepository = snapshotRepository;
        this.candidateRepository = candidateRepository;
        this.breakdownRepository = breakdownRepository;
        this.roleRepository = roleRepository;
        this.criterionRepository = criterionRepository;
        this.resumeRepository = resumeRepository;
        this.scoringService = scoringService;
        this.criterionService = criterionService;
        this.objectMapper = new ObjectMapper();
    }
    
    @Transactional
    public Evaluation createEvaluation(Long userId, Long roleId, List<Long> resumeIds) {
        // Validate resume count (2-10)
        if (resumeIds.size() < 2 || resumeIds.size() > 10) {
            throw new IllegalArgumentException("Evaluation must include 2-10 resumes");
        }
        
        // Verify role ownership
        Role role = roleRepository.findByIdAndUserId(roleId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));
        
        // Verify all resumes belong to user
        for (Long resumeId : resumeIds) {
            resumeRepository.findByIdAndUserId(resumeId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("Resume not found: " + resumeId));
        }
        
        // Create evaluation
        Evaluation evaluation = new Evaluation();
        evaluation.setUserId(userId);
        evaluation.setRoleId(roleId);
        evaluation.setStatus(Evaluation.Status.CREATED);
        evaluation = evaluationRepository.save(evaluation);
        
        // Create evaluation candidates
        for (Long resumeId : resumeIds) {
            Resume resume = resumeRepository.findById(resumeId).orElseThrow();
            EvaluationCandidate candidate = new EvaluationCandidate();
            candidate.setEvaluationId(evaluation.getId());
            candidate.setResumeId(resumeId);
            candidate.setResumeChecksum(resume.getChecksumSha256());
            candidateRepository.save(candidate);
        }
        
        return evaluation;
    }
    
    @Transactional
    public void runEvaluation(Long userId, Long evaluationId) {
        Evaluation evaluation = evaluationRepository.findByIdAndUserId(evaluationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Evaluation not found"));
        
        // Validate criteria weights sum to 100
        criterionService.validateWeightsSumTo100(evaluation.getRoleId());
        
        evaluation.setStatus(Evaluation.Status.RUNNING);
        evaluationRepository.save(evaluation);
        
        try {
            // Get role and criteria (for snapshot)
            Role role = roleRepository.findById(evaluation.getRoleId()).orElseThrow();
            List<Criterion> criteria = criterionRepository.findByRoleId(evaluation.getRoleId());
            
            // Create snapshot (immutability)
            EvaluationSnapshot snapshot = new EvaluationSnapshot();
            snapshot.setEvaluationId(evaluation.getId());
            snapshot.setRoleTitle(role.getTitle());
            snapshot.setRoleJobDescription(role.getJobDescription());
            
            // Convert criteria to JSON
            List<Map<String, Object>> criteriaJson = criteria.stream()
                    .map(c -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", c.getId());
                        map.put("name", c.getName());
                        map.put("description", c.getDescription());
                        map.put("weight", c.getWeight());
                        map.put("type", c.getType());
                        map.put("config", c.getConfigJson());
                        return map;
                    })
                    .collect(Collectors.toList());
            
            Map<String, Object> snapshotData = new HashMap<>();
            snapshotData.put("criteria", criteriaJson);
            snapshot.setCriteriaJson(snapshotData);
            snapshotRepository.save(snapshot);
            
            // Get candidates
            List<EvaluationCandidate> candidates = candidateRepository.findByEvaluationId(evaluation.getId());
            
            // Score each resume
            List<ScoreBreakdown> breakdowns = new ArrayList<>();
            for (EvaluationCandidate candidate : candidates) {
                Resume resume = resumeRepository.findById(candidate.getResumeId()).orElseThrow();
                Map<String, Object> breakdown = scoringService.scoreResume(resume, criteria, resume.getRawExtractedText());
                
                ScoreBreakdown scoreBreakdown = new ScoreBreakdown();
                scoreBreakdown.setEvaluationId(evaluation.getId());
                scoreBreakdown.setResumeId(resume.getId());
                
                double totalScore = (Double) breakdown.get("totalScore");
                double totalScorePct = (Double) breakdown.get("totalScorePct");
                
                scoreBreakdown.setTotalScore(BigDecimal.valueOf(totalScore).setScale(4, RoundingMode.HALF_UP));
                scoreBreakdown.setTotalScorePct(BigDecimal.valueOf(totalScorePct).setScale(1, RoundingMode.HALF_UP));
                scoreBreakdown.setBreakdownJson(breakdown);
                scoreBreakdown.setExplanationText("Scored " + totalScorePct + "% based on " + criteria.size() + " criteria");
                
                breakdowns.add(scoreBreakdown);
            }
            
            breakdownRepository.saveAll(breakdowns);
            
            evaluation.setStatus(Evaluation.Status.COMPLETED);
            evaluationRepository.save(evaluation);
            
        } catch (Exception e) {
            evaluation.setStatus(Evaluation.Status.FAILED);
            evaluationRepository.save(evaluation);
            throw new RuntimeException("Evaluation failed: " + e.getMessage(), e);
        }
    }
    
    public List<Evaluation> getAllEvaluations(Long userId) {
        return evaluationRepository.findByUserId(userId);
    }
    
    public Evaluation getEvaluationById(Long userId, Long evaluationId) {
        return evaluationRepository.findByIdAndUserId(evaluationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Evaluation not found"));
    }
    
    public List<Map<String, Object>> getEvaluationResults(Long userId, Long evaluationId) {
        Evaluation evaluation = evaluationRepository.findByIdAndUserId(evaluationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Evaluation not found"));
        
        List<ScoreBreakdown> breakdowns = breakdownRepository.findByEvaluationId(evaluationId);
        
        return breakdowns.stream()
                .map(b -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("resumeId", b.getResumeId());
                    result.put("totalScore", b.getTotalScore());
                    result.put("totalScorePct", b.getTotalScorePct());
                    result.put("breakdown", b.getBreakdownJson());
                    result.put("explanation", b.getExplanationText());
                    return result;
                })
                .sorted((a, b) -> {
                    BigDecimal scoreA = (BigDecimal) a.get("totalScore");
                    BigDecimal scoreB = (BigDecimal) b.get("totalScore");
                    return scoreB.compareTo(scoreA); // Descending order
                })
                .collect(Collectors.toList());
    }
    
    public Map<String, Object> compareResumes(Long userId, Long evaluationId, Long leftResumeId, Long rightResumeId) {
        Evaluation evaluation = evaluationRepository.findByIdAndUserId(evaluationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Evaluation not found"));
        
        ScoreBreakdown leftBreakdown = breakdownRepository.findByEvaluationIdAndResumeId(evaluationId, leftResumeId)
                .orElseThrow(() -> new IllegalArgumentException("Left resume not found in evaluation"));
        
        ScoreBreakdown rightBreakdown = breakdownRepository.findByEvaluationIdAndResumeId(evaluationId, rightResumeId)
                .orElseThrow(() -> new IllegalArgumentException("Right resume not found in evaluation"));
        
        Map<String, Object> comparison = new HashMap<>();
        comparison.put("leftResumeId", leftResumeId);
        comparison.put("rightResumeId", rightResumeId);
        comparison.put("leftBreakdown", leftBreakdown.getBreakdownJson());
        comparison.put("rightBreakdown", rightBreakdown.getBreakdownJson());
        comparison.put("explanation", ExplanationService.generateExplanation(
                leftBreakdown.getBreakdownJson(),
                rightBreakdown.getBreakdownJson()
        ));
        
        return comparison;
    }
}

