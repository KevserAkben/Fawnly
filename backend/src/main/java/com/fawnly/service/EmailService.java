package com.fawnly.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtp(String to, String code, String purpose) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Fawnly - Verification Code");
        message.setText(String.format(
                "Your Fawnly verification code for %s is: %s\n\nThis code expires in 10 minutes.\n\nIf you did not request this, please ignore this email.",
                purpose, code));
        mailSender.send(message);
    }
}
