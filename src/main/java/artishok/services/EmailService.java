package artishok.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Value("${spring.mail.from:dashainastya@artishok.com}")
    private String fromEmail;
    
    @Value("${app.email.verification.base-url:http://localhost:8080}")
    private String baseUrl;
    
    public void sendVerificationEmail(String toEmail, String verificationToken, String userName) {
        String subject = "Подтверждение email для сервиса АРТиШОК";
        String verificationUrl = baseUrl + "/api/auth/verify-email?token=" + verificationToken;
        
        String htmlContent = createVerificationEmailHtml(userName, verificationUrl);
        
        try {
            sendHtmlEmail(toEmail, subject, htmlContent);
        } catch (Exception e) {
            // В режиме разработки просто пропускаем ошибку
            System.out.println("Внимание: письмо не отправлено (режим разработки)");
        }
    }
    
    public void sendWelcomeEmail(String toEmail, String userName) {
        String subject = "Добро пожаловать в АРТиШОК!";
        String htmlContent = createWelcomeEmailHtml(userName, baseUrl);
        
        try {
            sendHtmlEmail(toEmail, subject, htmlContent);
        } catch (Exception e) {
            System.out.println("Внимание: приветственное письмо не отправлено");
        }
    }
    
    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        
        mailSender.send(message);
    }
    
    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            
            mailSender.send(message);
            System.out.println("Тестовое письмо отправлено в MailHog");
        } catch (Exception e) {
            System.out.println("Ошибка отправки письма: " + e.getMessage());
            throw e;
        }
    }
    
    public void testMailHogConnection() {
        try {
            sendSimpleEmail("test@artishok.com", "MailHog Test", 
                "Тестовое письмо из АРТиШОК. Проверьте MailHog UI: http://localhost:8025");
            System.out.println("Соединение с MailHog работает");
        } catch (Exception e) {
            System.out.println("Ошибка соединения с MailHog: " + e.getMessage());
        }
    }
    
    private String createVerificationEmailHtml(String userName, String verificationUrl) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }
                    .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; }
                    .header { background-color: #4a4a9c; color: white; padding: 30px; text-align: center; }
                    .content { padding: 40px; }
                    .button { display: inline-block; padding: 15px 30px; background-color: #4a4a9c; 
                             color: white; text-decoration: none; border-radius: 5px; font-size: 16px; 
                             font-weight: bold; margin: 20px 0; }
                    .footer { background-color: #f5f5f5; padding: 20px; text-align: center; 
                             color: #777; font-size: 12px; margin-top: 30px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🎨 АРТиШОК</h1>
                        <p>Платформа для удобного планирования выставок</p>
                    </div>
                    <div class="content">
                        <h2>Здравствуйте, %s!</h2>
                        <p>Спасибо за регистрацию в АРТиШОК!</p>
                        <p>Для завершения регистрации, пожалуйста, подтвердите ваш email:</p>
                        
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="%s" class="button">Подтвердить Email</a>
                        </div>
                        
                        <p>Или скопируйте ссылку в браузер:</p>
                        <div style="background: #f5f5f5; padding: 15px; border-radius: 5px; word-break: break-all;">
                            %s
                        </div>
                        
                        <p><strong>Ссылка действительна 24 часа.</strong></p>
                    </div>
                    <div class="footer">
                        <p>© 2025 АРТиШОК. Все права защищены.</p>
                        <p>Это письмо отправлено автоматически.</p>
                    </div>
                </div>
            </body>
            </html>
            """, userName, verificationUrl, verificationUrl);
    }
    
    private String createWelcomeEmailHtml(String userName, String baseUrl) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }
                    .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; }
                    .header { background-color: #27ae60; color: white; padding: 30px; text-align: center; }
                    .content { padding: 40px; }
                    .feature { background: #f9f9f9; padding: 15px; margin: 10px 0; border-radius: 5px; 
                              border-left: 4px solid #4a4a9c; }
                    .footer { background-color: #f5f5f5; padding: 20px; text-align: center; 
                             color: #777; font-size: 12px; margin-top: 30px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🎉 Добро пожаловать в АРТиШОК!</h1>
                        <p>Ваш аккаунт успешно активирован</p>
                    </div>
                    <div class="content">
                        <h2>Приветствуем, %s!</h2>
                        <p>Рады видеть вас в нашем сообществе художников и галерей!</p>
                        
                        <div style="text-align: center; margin: 30px 0;">
                            <a href="%s" style="display: inline-block; padding: 15px 30px; 
                               background-color: #4a4a9c; color: white; text-decoration: none; 
                               border-radius: 5px; font-weight: bold;">
                                Начать работу →
                            </a>
                        </div>
                    </div>
                    <div class="footer">
                        <p>© 2024 АРТиШОК. Платформа для арт-сообщества</p>
                        <p>Это письмо отправлено автоматически.</p>
                    </div>
                </div>
            </body>
            </html>
            """, userName, baseUrl);
    }
}