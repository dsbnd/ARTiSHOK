package artishok.controllers;

import artishok.services.EmailVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Аутентификация", description = "Управление аутентификацией и верификацией email")
public class AuthController {
    
    @Autowired
    private EmailVerificationService emailVerificationService;
    
    @Operation(summary = "Подтверждение email", description = "Подтверждение email по токену")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Email успешно подтвержден"),
        @ApiResponse(responseCode = "400", description = "Неверный или просроченный токен")
    })
    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(
            @Parameter(description = "Токен верификации", required = true)
            @RequestParam String token) {
        
        boolean verified = emailVerificationService.verifyEmail(token);
        
        if (verified) {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Email успешно подтвержден. Теперь вы можете войти в систему."
            ));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "success", false,
                        "message", "Неверный или просроченный токен верификации."
                    ));
        }
    }
    
    @Operation(summary = "Повторная отправка письма верификации", description = "Отправка нового письма для подтверждения email")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Письмо отправлено"),
        @ApiResponse(responseCode = "400", description = "Пользователь не найден или уже активирован")
    })
    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerificationEmail(
            @Parameter(description = "Email пользователя", required = true)
            @RequestParam String email) {
        
        try {
            emailVerificationService.resendVerificationEmailByEmail(email);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Письмо с подтверждением отправлено на " + email
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "success", false,
                        "message", e.getMessage()
                    ));
        }
    }
    
    @Operation(summary = "Проверка токена верификации", description = "Проверка валидности токена верификации")
    @ApiResponse(responseCode = "200", description = "Результат проверки")
    @GetMapping("/check-token")
    public ResponseEntity<?> checkToken(
            @Parameter(description = "Токен для проверки", required = true)
            @RequestParam String token) {
        
        boolean isValid = emailVerificationService.isTokenValid(token);
        
        return ResponseEntity.ok(Map.of(
            "valid", isValid,
            "token", token
        ));
    }
}