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
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Value("${app.email.verification.base-url:http://localhost:8080}")
    private String baseUrl;
    
    public void sendVerificationEmail(String toEmail, String verificationToken, String userName) {
        String subject = "Подтверждение email для сервиса АРТиШОК";
        String verificationUrl = baseUrl + "/api/auth/verify-email?token=" + verificationToken;
        
        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4a4a9c; color: white; padding: 20px; text-align: center; }
                    .content { padding: 30px; background-color: #f9f9f9; }
                    .button { display: inline-block; padding: 12px 24px; background-color: #4a4a9c; 
                             color: white; text-decoration: none; border-radius: 4px; margin: 20px 0; }
                    .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #ddd; 
                             color: #777; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>АРТиШОК</h1>
                        <p>Сервис организации художественных галерей</p>
                    </div>
                    <div class="content">
                        <h2>Здравствуйте, %s!</h2>
                        <p>Благодарим вас за регистрацию в сервисе АРТиШОК.</p>
                        <p>Для завершения регистрации и активации вашего аккаунта, пожалуйста, подтвердите ваш email:</p>
                        <p style="text-align: center;">
                            <a href="%s" class="button">Подтвердить Email</a>
                        </p>
                        <p>Или скопируйте и вставьте следующую ссылку в браузер:</p>
                        <p style="word-break: break-all; background-color: #eee; padding: 10px; border-radius: 4px;">
                            %s
                        </p>
                        <p>Ссылка действительна в течение 24 часов.</p>
                        <p>Если вы не регистрировались в нашем сервисе, пожалуйста, проигнорируйте это письмо.</p>
                    </div>
                    <div class="footer">
                        <p>© 2024 АРТиШОК. Все права защищены.</p>
                        <p>Это письмо было отправлено автоматически, пожалуйста, не отвечайте на него.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName, verificationUrl, verificationUrl);
        
        sendHtmlEmail(toEmail, subject, htmlContent);
    }
    
    public void sendWelcomeEmail(String toEmail, String userName) {
        String subject = "Добро пожаловать в АРТиШОК!";
        
        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4a4a9c; color: white; padding: 20px; text-align: center; }
                    .content { padding: 30px; background-color: #f9f9f9; }
                    .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #ddd; 
                             color: #777; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>АРТиШОК</h1>
                        <p>Сервис организации художественных галерей</p>
                    </div>
                    <div class="content">
                        <h2>Добро пожаловать, %s!</h2>
                        <p>Ваш аккаунт в сервисе АРТиШОК успешно подтвержден и активирован.</p>
                        <p>Теперь вы можете:</p>
                        <ul>
                            <li>Просматривать текущие и будущие выставки</li>
                            <li>Бронировать места для ваших произведений искусства</li>
                            <li>Добавлять информацию о ваших работах</li>
                            <li>Взаимодействовать с галереями</li>
                        </ul>
                        <p>Для начала работы перейдите в <a href="%s">личный кабинет</a>.</p>
                        <p>Если у вас возникнут вопросы, наша служба поддержки всегда готова помочь.</p>
                    </div>
                    <div class="footer">
                        <p>© 2024 АРТиШОК. Все права защищены.</p>
                        <p>Это письмо было отправлено автоматически, пожалуйста, не отвечайте на него.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName, baseUrl);
        
        sendHtmlEmail(toEmail, subject, htmlContent);
    }
    
    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Ошибка при отправке email", e);
        }
    }
    
    public void sendSimpleEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        
        mailSender.send(message);
    }
}