package com.hms.identity.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class PasswordResetEmailService {
    private final JavaMailSender mailSender;

    @Value("${hms.app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    public void sendResetLink(String recipient, String hospitalName, String tenantCode, String token) {
        String resetUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/reset-password")
                .queryParam("tenantCode", tenantCode)
                .queryParam("token", token)
                .build().encode().toUriString();
        SimpleMailMessage message = new SimpleMailMessage();
        if (!fromAddress.isBlank()) message.setFrom(fromAddress);
        message.setTo(recipient);
        message.setSubject("Reset your " + hospitalName + " HMS password");
        message.setText("A password reset was requested for your hospital account.\n\n"
                + "Use this one-time link within 30 minutes:\n" + resetUrl + "\n\n"
                + "If you did not request this, you can ignore this email.");
        mailSender.send(message);
    }
}