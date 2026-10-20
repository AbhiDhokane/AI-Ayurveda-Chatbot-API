package com.ayurveda.bot.controller;

import com.ayurveda.bot.model.EmailTreatmentRequest;
import com.ayurveda.bot.service.EmailService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/email")
public class EmailController {
    private final EmailService emailService;
    public EmailController(EmailService emailService) { this.emailService = emailService; }

    @PostMapping("/treatment")
    public ResponseEntity<Map<String, Object>> sendTreatment(@RequestBody EmailTreatmentRequest request, HttpSession session) {
        if (session.getAttribute(AuthController.USER_ID) == null)
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Please log in first."));
        String email = request.getEmail();
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"))
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Please enter a valid email address."));
        if (request.getAnswer() == null || request.getAnswer().isBlank())
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "There is no AI answer to email yet."));
        try {
            emailService.sendTreatment(email, request.getQuestion(), request.getAnswer(), request.getLanguage());
            return ResponseEntity.ok(Map.of("success", true, "message", "Treatment information sent successfully."));
        } catch (Exception e) {
            e.printStackTrace();
            String detail = (e.getMessage() != null) ? e.getMessage() : "Check SMTP credentials and App Password.";
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "Email could not be sent: " + detail));
        }
    }
}
