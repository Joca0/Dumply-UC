package com.dumply.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendDisable2FACode(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Desativação de Autenticação de Dois Fatores (2FA)");
        message.setText("Seu código para desativar o 2FA é: " + code + ". Se você não solicitou isso, altere sua senha imediatamente.");
        mailSender.send(message);
    }

    public void sendPasswordResetEmail(String to, String token) {
        String resetLink = frontendUrl + "/auth/reset-password?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Dumply - Redefinição de Senha");
        message.setText("Você solicitou a redefinição de sua senha no Dumply.\n\n" +
                "Para criar uma nova senha, clique no link abaixo:\n" +
                resetLink + "\n\n" +
                "Este link expirará em 1 hora. Se você não solicitou isso, ignore este e-mail.");

        mailSender.send(message);
    }
}