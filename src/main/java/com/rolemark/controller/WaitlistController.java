package com.rolemark.controller;

import com.rolemark.dto.WaitlistRequest;
import com.rolemark.entity.WaitlistSignup;
import com.rolemark.repository.WaitlistSignupRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/waitlist")
public class WaitlistController {
    
    private final WaitlistSignupRepository waitlistRepository;
    
    public WaitlistController(WaitlistSignupRepository waitlistRepository) {
        this.waitlistRepository = waitlistRepository;
    }
    
    @PostMapping
    public ResponseEntity<WaitlistSignup> signup(@Valid @RequestBody WaitlistRequest request, HttpServletRequest httpRequest) {
        WaitlistSignup signup = new WaitlistSignup();
        signup.setEmail(request.getEmail());
        signup.setUserAgent(httpRequest.getHeader("User-Agent"));
        signup.setReferrer(httpRequest.getHeader("Referer"));
        signup = waitlistRepository.save(signup);
        return ResponseEntity.status(HttpStatus.CREATED).body(signup);
    }
}

