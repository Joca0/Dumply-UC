package com.dumply.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

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
}