package com.rolemark.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Configuration
public class MultipartConfigLogger {
    
    private static final Logger log = LoggerFactory.getLogger(MultipartConfigLogger.class);
    private static final String LOG_PATH = ".cursor/debug.log";
    
    @Value("${spring.servlet.multipart.max-file-size}")
    private String maxFileSize;
    
    @Value("${spring.servlet.multipart.max-request-size}")
    private String maxRequestSize;
    
    @PostConstruct
    public void logMultipartConfig() {
        // #region agent log
        try {
            String logEntry = String.format(
                "{\"id\":\"log_%d_multipart\",\"timestamp\":%d,\"location\":\"MultipartConfigLogger.java:42\",\"message\":\"Multipart configuration loaded\",\"data\":{\"maxFileSize\":\"%s\",\"maxRequestSize\":\"%s\"},\"sessionId\":\"debug-session\",\"runId\":\"startup\",\"hypothesisId\":\"A\"}\n",
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                maxFileSize,
                maxRequestSize
            );
            Files.createDirectories(Paths.get(".cursor"));
            try (FileWriter writer = new FileWriter(LOG_PATH, true)) {
                writer.write(logEntry);
            }
        } catch (IOException e) {
            // Ignore logging errors
        }
        // #endregion
        
        log.info("Multipart configuration loaded - max-file-size: {}, max-request-size: {}", 
                 maxFileSize, maxRequestSize);
    }
}

