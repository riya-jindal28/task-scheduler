package com.project.taskscheduler.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;
    private final String fromAddress = "noreplytest2001@gmail.com";

    @Value("${app.alert.admin.email}")
    private String adminEmail;


    public void sendEmail(String to, String subject, String body) throws MailException {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        message.setFrom(fromAddress);
        mailSender.send(message);
    }

    public void sendAlert(String subject, String body) {
        try {
            sendEmail(adminEmail, "[ALERT]" + subject, body);
        } catch (Exception e) {
            System.err.println("Failed to send alert email: " + e.getMessage());
        }
    }
}
