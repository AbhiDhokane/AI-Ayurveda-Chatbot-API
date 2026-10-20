package com.ayurveda.bot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private final JavaMailSender mailSender;
    @Value("${spring.mail.username:}") private String sender;

    public EmailService(JavaMailSender mailSender) { this.mailSender = mailSender; }

    public void sendTreatment(String recipient, String question, String answer, String language) {
        SimpleMailMessage mail = new SimpleMailMessage();
        if (recipient != null) {
            mail.setTo(recipient.trim().replaceAll("\\s+", ""));
        }
        if (sender != null && !sender.isBlank()) {
            mail.setFrom(sender.trim().replaceAll("\\s+", ""));
        }
        mail.setSubject("AyurvedaCare AI – Your Wellness Information");
        mail.setText("Namaste!\n\nYour AyurvedaCare AI request\n\nQuestion:\n" + question
                + "\n\nAI-generated information:\n" + answer
                + "\n\nLanguage: " + language
                + "\n\nIMPORTANT: This email is for educational information only. It is not a diagnosis or a substitute for professional medical advice. Do not stop prescribed medicines based on this information. Consult a qualified healthcare professional or Ayurvedic practitioner for personal treatment decisions.\n\n— AyurvedaCare");
        mailSender.send(mail);
    }
}
