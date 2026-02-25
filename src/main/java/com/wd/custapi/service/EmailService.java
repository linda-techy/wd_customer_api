package com.wd.custapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${spring.mail.username:noreply@walldotbuilders.com}")
    private String fromEmail;

    /**
     * Sends a branded HTML password reset email containing a secure link.
     * Falls back to log simulation when email is disabled or mail sender is unavailable.
     */
    @Async
    public void sendPasswordResetEmail(String to, String firstName, String resetLink) {
        if (emailEnabled && mailSender != null) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(fromEmail);
                helper.setTo(to);
                helper.setSubject("Reset Your Walldot Password");
                helper.setText(buildPasswordResetHtml(firstName, resetLink), true);
                mailSender.send(message);
                logger.info("Password reset email sent successfully to {}", to);
            } catch (MessagingException e) {
                logger.error("Failed to send password reset email to {}. Falling back to simulation.", to, e);
                logEmailSimulation(to, firstName, resetLink);
            }
        } else {
            logEmailSimulation(to, firstName, resetLink);
        }
    }

    private String buildPasswordResetHtml(String firstName, String resetLink) {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Reset Your Password</title>
                </head>
                <body style="margin:0;padding:0;background-color:#f4f4f4;font-family:'Helvetica Neue',Helvetica,Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f4f4;padding:40px 0;">
                    <tr>
                      <td align="center">
                        <table width="600" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.08);">

                          <!-- Header -->
                          <tr>
                            <td style="background-color:#C62828;padding:32px 40px;text-align:center;">
                              <h1 style="margin:0;color:#ffffff;font-size:26px;font-weight:700;letter-spacing:1px;">WALLDOT BUILDERS</h1>
                              <p style="margin:6px 0 0;color:#ffcdd2;font-size:13px;">Customer Portal</p>
                            </td>
                          </tr>

                          <!-- Body -->
                          <tr>
                            <td style="padding:40px 40px 32px;">
                              <h2 style="margin:0 0 16px;color:#1a1a1a;font-size:22px;font-weight:600;">Reset Your Password</h2>
                              <p style="margin:0 0 12px;color:#444444;font-size:15px;line-height:1.6;">
                                Hi %s,
                              </p>
                              <p style="margin:0 0 24px;color:#444444;font-size:15px;line-height:1.6;">
                                We received a request to reset the password for your Walldot Builders customer account.
                                Click the button below to set a new password. This link is valid for <strong>15 minutes</strong>.
                              </p>

                              <!-- CTA Button -->
                              <table cellpadding="0" cellspacing="0" style="margin:0 0 28px;">
                                <tr>
                                  <td style="background-color:#C62828;border-radius:6px;">
                                    <a href="%s"
                                       style="display:inline-block;padding:14px 32px;color:#ffffff;text-decoration:none;font-size:15px;font-weight:600;letter-spacing:0.5px;">
                                      Reset My Password
                                    </a>
                                  </td>
                                </tr>
                              </table>

                              <p style="margin:0 0 12px;color:#666666;font-size:13px;line-height:1.6;">
                                If the button above doesn't work, copy and paste this link into your browser:
                              </p>
                              <p style="margin:0 0 24px;word-break:break-all;">
                                <a href="%s" style="color:#C62828;font-size:13px;">%s</a>
                              </p>

                              <p style="margin:0;color:#888888;font-size:13px;line-height:1.6;">
                                If you did not request a password reset, you can safely ignore this email.
                                Your password will remain unchanged.
                              </p>
                            </td>
                          </tr>

                          <!-- Footer -->
                          <tr>
                            <td style="background-color:#f9f9f9;padding:20px 40px;border-top:1px solid #eeeeee;text-align:center;">
                              <p style="margin:0;color:#aaaaaa;font-size:12px;">
                                &copy; 2025 Walldot Builders. All rights reserved.<br>
                                This is an automated email, please do not reply.
                              </p>
                            </td>
                          </tr>

                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(firstName, resetLink, resetLink, resetLink);
    }

    private void logEmailSimulation(String to, String firstName, String resetLink) {
        logger.info("================ EMAIL SIMULATION ================");
        logger.info("TO: {}", to);
        logger.info("SUBJECT: Reset Your Walldot Password");
        logger.info("Hi {}, click the following link to reset your password:", firstName);
        logger.info("RESET LINK: {}", resetLink);
        logger.info("(Link expires in 15 minutes)");
        logger.info("==================================================");
    }
}
