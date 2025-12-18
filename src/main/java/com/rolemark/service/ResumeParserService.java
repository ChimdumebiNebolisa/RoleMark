package com.rolemark.service;

import com.rolemark.entity.ExtractedSignal;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ResumeParserService {
    
    private static final int SNIPPET_CONTEXT = 40;
    
    // Date patterns
    private static final Pattern DATE_PATTERN_1 = Pattern.compile(
            "([A-Z][a-z]{2,8})\\s+(\\d{4})\\s*[-–—]\\s*([A-Z][a-z]{2,8})?\\s*(\\d{4}|Present|present|PRESENT|Current|current|CURRENT)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DATE_PATTERN_2 = Pattern.compile(
            "(\\d{1,2})/(\\d{4})\\s*[-–—]\\s*(\\d{1,2})?/?\\s*(\\d{4}|Present|present|PRESENT|Current|current|CURRENT)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DATE_PATTERN_3 = Pattern.compile(
            "(\\d{4})\\s*[-–—]\\s*(\\d{4}|Present|present|PRESENT|Current|current|CURRENT)",
            Pattern.CASE_INSENSITIVE
    );
    
    // Education level patterns
    private static final List<Pattern> EDUCATION_PATTERNS = Arrays.asList(
            Pattern.compile("\\b(PhD|Ph\\.D\\.|Doctor|Doctorate)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(Master|M\\.S\\.|M\\.A\\.|MS|MA)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(Bachelor|B\\.S\\.|B\\.A\\.|BS|BA|B\\.Sc\\.|B\\.A\\.)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(Associate|A\\.S\\.|AA|A\\.A\\.)\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(High School|HS|H\\.S\\.)\\b", Pattern.CASE_INSENSITIVE)
    );
    
    public List<ExtractedSignal> parseResume(String text) {
        List<ExtractedSignal> signals = new ArrayList<>();
        
        // Normalize text for keyword matching
        String normalizedText = normalizeText(text);
        
        // Extract date ranges for experience
        signals.addAll(extractDateRanges(text, normalizedText));
        
        // Extract education level
        signals.addAll(extractEducationLevel(text, normalizedText));
        
        return signals;
    }
    
    public String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
    
    public List<ExtractedSignal> extractKeywordMatches(String text, String normalizedText, List<String> keywords, String matchMode) {
        List<ExtractedSignal> signals = new ArrayList<>();
        Set<String> matchedKeywords = new HashSet<>();
        
        for (String keyword : keywords) {
            String normalizedKeyword = normalizeText(keyword);
            if (normalizedText.contains(normalizedKeyword)) {
                matchedKeywords.add(keyword);
                
                // Find first occurrence for evidence snippet
                int index = normalizedText.indexOf(normalizedKeyword);
                if (index >= 0) {
                    String snippet = extractSnippet(text, index, normalizedKeyword.length());
                    ExtractedSignal signal = new ExtractedSignal();
                    signal.setType("KEYWORD_MATCH");
                    signal.setValue(keyword);
                    signal.setEvidenceSnippet(snippet);
                    signal.setConfidence(ExtractedSignal.Confidence.HIGH);
                    signals.add(signal);
                }
            }
        }
        
        // If matchMode is ALL and not all keywords matched, still return partial matches
        // The scoring logic will handle partial credit
        
        return signals;
    }
    
    private List<ExtractedSignal> extractDateRanges(String originalText, String normalizedText) {
        List<ExtractedSignal> signals = new ArrayList<>();
        List<DateRange> dateRanges = new ArrayList<>();
        
        // Try pattern 1: "MMM YYYY - MMM YYYY" or "MMM YYYY - Present"
        Matcher matcher1 = DATE_PATTERN_1.matcher(originalText);
        while (matcher1.find()) {
            try {
                String startMonth = matcher1.group(1);
                String startYear = matcher1.group(2);
                String endMonth = matcher1.group(3);
                String endYearStr = matcher1.group(4);
                
                LocalDate startDate = parseMonthYear(startMonth, startYear);
                LocalDate endDate = null;
                
                if (endYearStr != null && (endYearStr.equalsIgnoreCase("Present") || 
                    endYearStr.equalsIgnoreCase("Current"))) {
                    endDate = LocalDate.now();
                } else if (endYearStr != null) {
                    if (endMonth != null) {
                        endDate = parseMonthYear(endMonth, endYearStr);
                    } else {
                        endDate = LocalDate.of(Integer.parseInt(endYearStr), 12, 31);
                    }
                }
                
                if (startDate != null && endDate != null) {
                    String snippet = extractSnippet(originalText, matcher1.start(), matcher1.end() - matcher1.start());
                    dateRanges.add(new DateRange(startDate, endDate, snippet));
                }
            } catch (Exception e) {
                // Skip invalid date ranges
            }
        }
        
        // Try pattern 2: "MM/YYYY - MM/YYYY" or "MM/YYYY - Present"
        Matcher matcher2 = DATE_PATTERN_2.matcher(originalText);
        while (matcher2.find()) {
            try {
                int startMonth = Integer.parseInt(matcher2.group(1));
                int startYear = Integer.parseInt(matcher2.group(2));
                String endMonthStr = matcher2.group(3);
                String endYearStr = matcher2.group(4);
                
                LocalDate startDate = LocalDate.of(startYear, startMonth, 1);
                LocalDate endDate = null;
                
                if (endYearStr != null && (endYearStr.equalsIgnoreCase("Present") || 
                    endYearStr.equalsIgnoreCase("Current"))) {
                    endDate = LocalDate.now();
                } else if (endYearStr != null) {
                    int endYear = Integer.parseInt(endYearStr);
                    int endMonth = endMonthStr != null ? Integer.parseInt(endMonthStr) : 12;
                    endDate = LocalDate.of(endYear, endMonth, 1).withDayOfMonth(
                        LocalDate.of(endYear, endMonth, 1).lengthOfMonth()
                    );
                }
                
                if (startDate != null && endDate != null) {
                    String snippet = extractSnippet(originalText, matcher2.start(), matcher2.end() - matcher2.start());
                    dateRanges.add(new DateRange(startDate, endDate, snippet));
                }
            } catch (Exception e) {
                // Skip invalid date ranges
            }
        }
        
        // Try pattern 3: "YYYY - YYYY" or "YYYY - Present"
        Matcher matcher3 = DATE_PATTERN_3.matcher(originalText);
        while (matcher3.find()) {
            try {
                int startYear = Integer.parseInt(matcher3.group(1));
                String endYearStr = matcher3.group(2);
                
                LocalDate startDate = LocalDate.of(startYear, 1, 1);
                LocalDate endDate = null;
                
                if (endYearStr != null && (endYearStr.equalsIgnoreCase("Present") || 
                    endYearStr.equalsIgnoreCase("Current"))) {
                    endDate = LocalDate.now();
                } else if (endYearStr != null) {
                    int endYear = Integer.parseInt(endYearStr);
                    endDate = LocalDate.of(endYear, 12, 31);
                }
                
                if (startDate != null && endDate != null) {
                    String snippet = extractSnippet(originalText, matcher3.start(), matcher3.end() - matcher3.start());
                    dateRanges.add(new DateRange(startDate, endDate, snippet));
                }
            } catch (Exception e) {
                // Skip invalid date ranges
            }
        }
        
        // Merge overlapping ranges and calculate total months
        if (!dateRanges.isEmpty()) {
            List<DateRange> merged = mergeDateRanges(dateRanges);
            long totalMonths = calculateTotalMonths(merged);
            double years = totalMonths / 12.0;
            
            ExtractedSignal signal = new ExtractedSignal();
            signal.setType("EXPERIENCE_YEARS_ESTIMATE");
            signal.setValue(String.valueOf(years));
            signal.setConfidence(merged.size() > 0 ? ExtractedSignal.Confidence.MEDIUM : ExtractedSignal.Confidence.LOW);
            if (!merged.isEmpty()) {
                signal.setEvidenceSnippet(merged.get(0).snippet);
            }
            signals.add(signal);
            
            // Store individual date ranges as signals
            for (DateRange range : merged) {
                ExtractedSignal rangeSignal = new ExtractedSignal();
                rangeSignal.setType("DATE_RANGE");
                rangeSignal.setValue(range.startDate + " to " + range.endDate);
                rangeSignal.setEvidenceSnippet(range.snippet);
                rangeSignal.setConfidence(ExtractedSignal.Confidence.HIGH);
                signals.add(rangeSignal);
            }
        } else {
            // No date ranges found
            ExtractedSignal signal = new ExtractedSignal();
            signal.setType("EXPERIENCE_YEARS_ESTIMATE");
            signal.setValue("0");
            signal.setConfidence(ExtractedSignal.Confidence.LOW);
            signal.setEvidenceSnippet("No date ranges detected in resume");
            signals.add(signal);
        }
        
        return signals;
    }
    
    private List<ExtractedSignal> extractEducationLevel(String originalText, String normalizedText) {
        List<ExtractedSignal> signals = new ArrayList<>();
        String[] levels = {"PHD", "MASTER", "BACHELOR", "ASSOCIATE", "HS"};
        
        for (int i = 0; i < EDUCATION_PATTERNS.size(); i++) {
            Pattern pattern = EDUCATION_PATTERNS.get(i);
            Matcher matcher = pattern.matcher(originalText);
            if (matcher.find()) {
                ExtractedSignal signal = new ExtractedSignal();
                signal.setType("EDUCATION_LEVEL_ESTIMATE");
                signal.setValue(levels[i]);
                signal.setEvidenceSnippet(extractSnippet(originalText, matcher.start(), matcher.end() - matcher.start()));
                signal.setConfidence(ExtractedSignal.Confidence.HIGH);
                signals.add(signal);
                return signals; // Return highest level found
            }
        }
        
        // No education level found
        ExtractedSignal signal = new ExtractedSignal();
        signal.setType("EDUCATION_LEVEL_ESTIMATE");
        signal.setValue("UNKNOWN");
        signal.setConfidence(ExtractedSignal.Confidence.LOW);
        signal.setEvidenceSnippet("No education token detected");
        signals.add(signal);
        return signals;
    }
    
    private String extractSnippet(String text, int startIndex, int length) {
        int snippetStart = Math.max(0, startIndex - SNIPPET_CONTEXT);
        int snippetEnd = Math.min(text.length(), startIndex + length + SNIPPET_CONTEXT);
        return text.substring(snippetStart, snippetEnd).trim();
    }
    
    private LocalDate parseMonthYear(String month, String year) {
        Map<String, Integer> monthMap = new HashMap<>();
        monthMap.put("january", 1); monthMap.put("jan", 1);
        monthMap.put("february", 2); monthMap.put("feb", 2);
        monthMap.put("march", 3); monthMap.put("mar", 3);
        monthMap.put("april", 4); monthMap.put("apr", 4);
        monthMap.put("may", 5);
        monthMap.put("june", 6); monthMap.put("jun", 6);
        monthMap.put("july", 7); monthMap.put("jul", 7);
        monthMap.put("august", 8); monthMap.put("aug", 8);
        monthMap.put("september", 9); monthMap.put("sep", 9); monthMap.put("sept", 9);
        monthMap.put("october", 10); monthMap.put("oct", 10);
        monthMap.put("november", 11); monthMap.put("nov", 11);
        monthMap.put("december", 12); monthMap.put("dec", 12);
        
        Integer monthNum = monthMap.get(month.toLowerCase());
        if (monthNum == null) {
            return null;
        }
        int yearNum = Integer.parseInt(year);
        return LocalDate.of(yearNum, monthNum, 1);
    }
    
    private List<DateRange> mergeDateRanges(List<DateRange> ranges) {
        if (ranges.isEmpty()) {
            return ranges;
        }
        
        // Sort by start date
        ranges.sort(Comparator.comparing(r -> r.startDate));
        
        List<DateRange> merged = new ArrayList<>();
        DateRange current = ranges.get(0);
        
        for (int i = 1; i < ranges.size(); i++) {
            DateRange next = ranges.get(i);
            if (current.endDate.isAfter(next.startDate) || current.endDate.equals(next.startDate)) {
                // Overlapping or adjacent - merge
                if (next.endDate.isAfter(current.endDate)) {
                    current = new DateRange(current.startDate, next.endDate, current.snippet);
                }
            } else {
                // No overlap - add current and move to next
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        
        return merged;
    }
    
    private long calculateTotalMonths(List<DateRange> ranges) {
        long totalMonths = 0;
        for (DateRange range : ranges) {
            totalMonths += java.time.temporal.ChronoUnit.MONTHS.between(
                range.startDate.withDayOfMonth(1),
                range.endDate.withDayOfMonth(1)
            ) + 1; // +1 to include both start and end months
        }
        return totalMonths;
    }
    
    private static class DateRange {
        LocalDate startDate;
        LocalDate endDate;
        String snippet;
        
        DateRange(LocalDate startDate, LocalDate endDate, String snippet) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.snippet = snippet;
        }
    }
}

