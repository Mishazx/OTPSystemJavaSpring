package ru.mishazx.otpsystemjavaspring.service;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.util.Properties;

@Service
@Slf4j
public class EmailService {
    private final String username;
    private final String password;
    // private final String fromEmail;
    private final Session session;
    private final TemplateEngine templateEngine;

    @Autowired
    public EmailService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
        
        Properties config = loadConfig();
        this.username = config.getProperty("email.username");
        this.password = config.getProperty("email.password");
        // this.fromEmail = config.getProperty("email.from", this.username);

        Properties sessionProps = new Properties();
        sessionProps.put("mail.smtp.auth", config.getProperty("mail.smtp.auth", "true"));
        sessionProps.put("mail.smtp.starttls.enable", config.getProperty("mail.smtp.starttls.enable", "true"));
        sessionProps.put("mail.smtp.host", config.getProperty("mail.smtp.host", "smtp.gmail.com"));
        sessionProps.put("mail.smtp.port", config.getProperty("mail.smtp.port", "587"));

        this.session = Session.getInstance(sessionProps, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }

    private Properties loadConfig() {
        Properties props = new Properties();
        try {
            var inputStream = getClass().getClassLoader().getResourceAsStream("secret.properties");
            if (inputStream == null) {
                log.error("Configuration file 'secret.properties' not found in classpath");
                throw new RuntimeException("Configuration file 'secret.properties' not found in classpath");
            }
            props.load(inputStream);
            return props;
        } catch (IOException e) {
            log.error("Failed to load email configuration", e);
            throw new RuntimeException("Failed to load email configuration", e);
        }
    }

    public void sendOTPCode(String toEmail, String code) {
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject("Ваш код подтверждения");

            // Создаем контекст с переменными для шаблона
            Context context = new Context();
            context.setVariable("code", code);
            
            // Обрабатываем шаблон
            String htmlContent = templateEngine.process("otp_email", context);

            message.setContent(htmlContent, "text/html; charset=UTF-8");

            Transport.send(message);
            log.info("OTP code sent to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }

    public void sendOTPEmail(String toEmail, String code) {
        sendOTPCode(toEmail, code);
    }
}
