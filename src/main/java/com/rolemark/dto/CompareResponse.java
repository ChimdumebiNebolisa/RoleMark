package com.rolemark.dto;

import java.util.List;

public class CompareResponse {
    private List<EvaluationResponse> results;
    
    public CompareResponse() {
    }
    
    public CompareResponse(List<EvaluationResponse> results) {
        this.results = results;
    }
    
    public List<EvaluationResponse> getResults() {
        return results;
    }
    
    public void setResults(List<EvaluationResponse> results) {
        this.results = results;
    }
}

