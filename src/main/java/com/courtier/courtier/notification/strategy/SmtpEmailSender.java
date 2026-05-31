package com.courtier.courtier.notification.strategy;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailSender implements EmailSenderStrategy {

    private final JavaMailSender mailSender;

    @Async
    @Override
    public void sendEmail(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom("CourTier <courtier.noreply@gmail.com>");
            helper.setReplyTo("ershashwat@gmail.com");

            String html = buildHtml(subject, body);
            helper.setText(body, html); // plain-text fallback, then HTML
            mailSender.send(message);
            log.info("Email sent via SMTP to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Email send failed", e);
        }
    }

    private String buildHtml(String subject, String body) {
        // body may contain \n — convert to <br> for HTML
        String htmlBody = body.replace("\n", "<br>");

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8"/>
              <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
              <title>%s</title>
            </head>
            <body style="margin:0;padding:0;background:#f4f6f9;font-family:Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f6f9;padding:32px 0;">
                <tr><td align="center">
                  <table width="600" cellpadding="0" cellspacing="0"
                         style="background:#ffffff;border-radius:8px;overflow:hidden;
                                box-shadow:0 2px 8px rgba(0,0,0,0.08);">

                    <!-- Header -->
                    <tr>
                      <td style="background:#1a56db;padding:24px 32px;">
                        <h1 style="margin:0;color:#ffffff;font-size:22px;font-weight:700;
                                   letter-spacing:0.5px;">CourTier</h1>
                        <p style="margin:4px 0 0;color:#bfdbfe;font-size:13px;">
                          Court Case Tracking System
                        </p>
                      </td>
                    </tr>

                    <!-- Body -->
                    <tr>
                      <td style="padding:32px;">
                        <h2 style="margin:0 0 16px;color:#111827;font-size:18px;">%s</h2>
                        <p style="margin:0 0 24px;color:#374151;font-size:15px;line-height:1.7;">
                          %s
                        </p>
                        <hr style="border:none;border-top:1px solid #e5e7eb;margin:24px 0;"/>
                        <p style="margin:0;color:#6b7280;font-size:12px;line-height:1.6;">
                          This is an automated notification from CourTier.<br/>
                          If you did not expect this email, you can safely ignore it.
                        </p>
                      </td>
                    </tr>

                    <!-- Footer -->
                    <tr>
                      <td style="background:#f9fafb;padding:16px 32px;border-top:1px solid #e5e7eb;">
                        <p style="margin:0;color:#9ca3af;font-size:11px;text-align:center;">
                          &copy; 2026 CourTier &nbsp;|&nbsp;
                          <a href="mailto:ershashwat@gmail.com"
                             style="color:#6b7280;text-decoration:none;">
                            ershashwat@gmail.com
                          </a>
                        </p>
                      </td>
                    </tr>

                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(subject, subject, htmlBody);
    }

    @Override
    public String getProviderName() {
        return "smtp";
    }
}