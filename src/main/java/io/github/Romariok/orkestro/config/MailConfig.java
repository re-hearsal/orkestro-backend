package io.github.Romariok.orkestro.config;

import io.github.Romariok.orkestro.mail.jca.SmtpConnection;
import io.github.Romariok.orkestro.mail.jca.SmtpConnectionImpl;
import java.util.Properties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class MailConfig {

    @Bean
    public JavaMailSender javaMailSender(
            @Value("${orkestro.mail.host:localhost}") String host,
            @Value("${orkestro.mail.port:25}") int port,
            @Value("${orkestro.mail.username:}") String username,
            @Value("${orkestro.mail.password:}") String password,
            @Value("${orkestro.mail.properties.mail.smtp.auth:false}") boolean smtpAuth,
            @Value("${orkestro.mail.properties.mail.smtp.starttls.enable:false}") boolean smtpStartTls,
            @Value("${orkestro.mail.properties.mail.smtp.connectiontimeout:5000}") int connectionTimeout,
            @Value("${orkestro.mail.properties.mail.smtp.timeout:5000}") int timeout,
            @Value("${orkestro.mail.properties.mail.smtp.writetimeout:5000}") int writeTimeout) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        if (username != null && !username.isBlank()) {
            mailSender.setUsername(username);
        }
        if (password != null && !password.isBlank()) {
            mailSender.setPassword(password);
        }

        Properties properties = mailSender.getJavaMailProperties();
        properties.put("mail.smtp.auth", smtpAuth);
        properties.put("mail.smtp.starttls.enable", smtpStartTls);
        properties.put("mail.smtp.connectiontimeout", connectionTimeout);
        properties.put("mail.smtp.timeout", timeout);
        properties.put("mail.smtp.writetimeout", writeTimeout);

        return mailSender;
    }

    @Bean
    public SmtpConnection smtpConnection(
            JavaMailSender javaMailSender,
            @Value("${orkestro.mail.from:no-reply@orkestro.local}") String fromAddress) {
        return new SmtpConnectionImpl(javaMailSender, fromAddress);
    }
}
