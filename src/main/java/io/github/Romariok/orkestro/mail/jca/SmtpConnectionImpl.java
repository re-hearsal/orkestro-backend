package io.github.Romariok.orkestro.mail.jca;

import jakarta.mail.internet.MimeMessage;
import jakarta.resource.ResourceException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

public class SmtpConnectionImpl implements SmtpConnection {
    private final JavaMailSender mailSender;
    private final String fromAddress;

    public SmtpConnectionImpl(JavaMailSender mailSender, String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendEmail(String to, String subject, String htmlText) throws ResourceException {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlText, true);
            mailSender.send(mimeMessage);
        } catch (Exception ex) {
            throw new ResourceException("Failed to send email via SMTP RA connection", ex);
        }
    }
}
