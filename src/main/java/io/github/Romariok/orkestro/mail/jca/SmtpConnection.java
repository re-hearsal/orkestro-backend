package io.github.Romariok.orkestro.mail.jca;

import jakarta.resource.ResourceException;

public interface SmtpConnection {
    void sendEmail(String to, String subject, String htmlText) throws ResourceException;
}
