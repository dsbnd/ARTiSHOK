package artishok.controllers;

import artishok.services.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Tag(name = "Тестирование", description = "API для тестирования")
public class TestController {
    
	@Autowired
    private EmailService emailService;
    
    @PostMapping("/mailhog")
    @Operation(summary = "Отправить тестовое письмо в MailHog")
    public ResponseEntity<?> testMailHog(@RequestParam(value = "email", required = false) String email) {
        String testEmail = email != null ? email : "test@artishok.com";
        
        try {
            emailService.sendSimpleEmail(testEmail, "Тест MailHog", 
                    "Привет! Это тестовое письмо из ARTISHOK.\n\n" +
                    "Если ты видишь это письмо в MailHog, значит всё работает!");
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Тестовое письмо отправлено в MailHog",
                "email", testEmail,
                "mailhog_url", "http://localhost:8025",
                "instructions", "Откройте http://localhost:8025 в браузере чтобы увидеть письмо"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage(),
                "details", "Убедитесь, что MailHog запущен: ./MailHog_linux_amd64"
            ));
        }
    }
    
    @GetMapping("/health")
    @Operation(summary = "Проверить здоровье приложения")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "ARTISHOK",
            "timestamp", java.time.LocalDateTime.now(),
            "mailhog", Map.of(
                "smtp_port", 1025,
                "web_ui", "http://localhost:8025",
                "status", "Должен быть запущен"
            )
        ));
    }
}