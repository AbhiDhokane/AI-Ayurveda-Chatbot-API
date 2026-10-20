package com.ayurveda.bot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spring.mail.username:}")
    private String sender;

    @Value("${resend.api.key:}")
    private String resendApiKey;

    @Value("${brevo.api.key:}")
    private String brevoApiKey;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendTreatment(String recipient, String question, String answer, String language) {
        String cleanRecipient = (recipient != null) ? recipient.trim().replaceAll("\\s+", "") : "";
        String cleanSender = (sender != null && !sender.isBlank()) ? sender.trim().replaceAll("\\s+", "") : "onboarding@resend.dev";

        String subject = "AyurvedaCare AI – Your Wellness Information";
        String body = "Namaste!\n\nYour AyurvedaCare AI request\n\nQuestion:\n" + question
                + "\n\nAI-generated information:\n" + answer
                + "\n\nLanguage: " + language
                + "\n\nIMPORTANT: This email is for educational information only. It is not a diagnosis or a substitute for professional medical advice. Do not stop prescribed medicines based on this information. Consult a qualified healthcare professional or Ayurvedic practitioner for personal treatment decisions.\n\n— AyurvedaCare";

        // 1. Brevo HTTPS API (Port 443 - Can send to ANY recipient worldwide without custom domain)
        if (brevoApiKey != null && !brevoApiKey.isBlank()) {
            String brevoSender = (sender != null && !sender.isBlank() && !sender.contains("resend.dev")) ? cleanSender : "abhishekdhokane1331@gmail.com";
            sendViaBrevo(brevoSender, cleanRecipient, subject, body);
            return;
        }

        // 2. Resend HTTPS API (Port 443 - Bypasses Render port 587 restrictions)
        if (resendApiKey != null && !resendApiKey.isBlank()) {
            sendViaResend(cleanRecipient, subject, body);
            return;
        }

        // 3. Standard SMTP fallback (Port 587 - Works on Railway, local, and standard servers)
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(cleanRecipient);
            if (sender != null && !sender.isBlank()) {
                mail.setFrom(cleanSender);
            }
            mail.setSubject(subject);
            mail.setText(body);
            mailSender.send(mail);
        } catch (Exception e) {
            String msg = (e.getMessage() != null) ? e.getMessage() : "";
            if (msg.contains("timed out") || msg.contains("SocketTimeoutException")) {
                throw new RuntimeException("Render Free Tier blocks outbound SMTP port 587. To send emails on Render, add BREVO_API_KEY (from brevo.com) or RESEND_API_KEY.");
            }
            throw e;
        }
    }

    private void sendViaBrevo(String senderEmail, String recipient, String subject, String body) {
        String url = "https://api.brevo.com/v3/smtp/email";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", brevoApiKey.trim());

        Map<String, Object> payload = Map.of(
                "sender", Map.of("name", "AyurvedaCare AI", "email", senderEmail),
                "to", List.of(Map.of("email", recipient)),
                "subject", subject,
                "textContent", body
        );

        try {
            restTemplate.postForEntity(url, new HttpEntity<>(payload, headers), String.class);
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException("Brevo API Error (" + e.getStatusCode() + "): " + e.getResponseBodyAsString());
        }
    }

    private void sendViaResend(String recipient, String subject, String body) {
        String url = "https://api.resend.com/emails";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(resendApiKey.trim());

        Map<String, Object> payload = Map.of(
                "from", "AyurvedaCare <onboarding@resend.dev>",
                "to", List.of(recipient),
                "subject", subject,
                "text", body
        );

        try {
            restTemplate.postForEntity(url, new HttpEntity<>(payload, headers), String.class);
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException("Resend API Error (" + e.getStatusCode() + "): " + e.getResponseBodyAsString());
        }
    }
}
