package com.rolemark.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class PdfExtractionService {
    
    private static final int MAX_PAGES = 5;
    private static final long MAX_FILE_SIZE = 2_500_000; // 2.5 MB in bytes
    
    public String extractText(MultipartFile file) throws IOException {
        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds 2.5 MB limit");
        }
        
        // Validate content type
        if (!file.getContentType().equals("application/pdf")) {
            throw new IllegalArgumentException("File must be a PDF");
        }
        
        // PDFBox 3.0.1 doesn't support Loader.loadPDF(InputStream)
        // Using byte[] instead, which is the supported API
        byte[] pdfBytes = file.getBytes();
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            
            // Validate page count
            int pageCount = document.getNumberOfPages();
            if (pageCount > MAX_PAGES) {
                throw new IllegalArgumentException("PDF exceeds 5 page limit. Found: " + pageCount);
            }
            
            // Extract text
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(pageCount);
            return stripper.getText(document);
        }
    }
    
    public int getPageCount(MultipartFile file) throws IOException {
        // PDFBox 3.0.1 doesn't support Loader.loadPDF(InputStream)
        // Using byte[] instead, which is the supported API
        byte[] pdfBytes = file.getBytes();
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            return document.getNumberOfPages();
        }
    }
}

