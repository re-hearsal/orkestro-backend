package io.github.Romariok.orkestro.mail.service;

import io.github.Romariok.orkestro.mail.jca.SmtpConnection;
import jakarta.resource.ResourceException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class JcaEmailService {
    private final SmtpConnection smtpConnection;
    private final ResourceLoader resourceLoader;
    private final Map<String, String> templateCache = new ConcurrentHashMap<>();

    public void sendTemplateMessage(String to, String subject, String templateName, Map<String, Object> templateModel) {
        try {
            String template = loadTemplate(templateName);
            String htmlBody = applyTemplate(template, templateModel);
            smtpConnection.sendEmail(to, subject, htmlBody);
            log.info("Template email sent to {} using template {}", to, templateName);
        } catch (ResourceException ex) {
            log.error("Failed to send template email to {} via SMTP RA", to, ex);
            throw new RuntimeException("Failed to send template email", ex);
        } catch (IllegalStateException ex) {
            log.error("Failed to load email template {}", templateName, ex);
            throw new RuntimeException("Failed to load email template", ex);
        }
    }

    private String loadTemplate(String templateName) {
        return templateCache.computeIfAbsent(templateName, this::readTemplateUnchecked);
    }

    private String readTemplateUnchecked(String templateName) {
        try {
            Resource resource = resourceLoader.getResource("classpath:templates/email/" + templateName);
            try (InputStream inputStream = resource.getInputStream()) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read email template " + templateName, ex);
        }
    }

    private String applyTemplate(String template, Map<String, Object> model) {
        String result = template;
        for (Map.Entry<String, Object> entry : model.entrySet()) {
            String token = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() == null ? "" : String.valueOf(entry.getValue());
            result = result.replace(token, value);
        }
        return result;
    }
}
