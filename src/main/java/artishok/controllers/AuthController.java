package artishok.controllers;

import artishok.entities.User;
import artishok.entities.enums.UserRole;
import artishok.repositories.UserRepository;
import artishok.security.JwtTokenUtil;
import artishok.services.EmailVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Аутентификация", description = "Регистрация, вход и верификация email")
public class AuthController {

    private final EmailVerificationService emailVerificationService;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;

    // ========== РЕГИСТРАЦИЯ ==========
    @Operation(summary = "Регистрация нового пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Пользователь зарегистрирован"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации или email уже используется")
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        // Проверка существования email
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email уже используется"));
        }

        // Проверка существования телефона
        if (request.getPhoneNumber() != null && userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Номер телефона уже используется"));
        }

        // Создание пользователя (неактивного до подтверждения email)
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setRole(request.getRole());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setBio(request.getBio());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setRegistrationDate(LocalDateTime.now());
        user.setIsActive(false); // Неактивен до подтверждения email

        userRepository.save(user);

        // Отправка письма для подтверждения email
        emailVerificationService.sendVerificationEmail(user);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Регистрация успешна. Проверьте email для подтверждения.",
                "userId", user.getId(),
                "email", user.getEmail()
        ));
    }

    // ========== ВХОД В СИСТЕМУ ==========
    @Operation(summary = "Вход в систему")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Вход успешен"),
            @ApiResponse(responseCode = "400", description = "Неверные учетные данные")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            // Аутентификация
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            // Получение пользователя
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            // Проверка активации аккаунта
            if (!Boolean.TRUE.equals(user.getIsActive())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "error", "Аккаунт не активирован. Подтвердите email.",
                                "resendUrl", "/api/auth/resend-verification?email=" + user.getEmail()
                        ));
            }

            // Генерация JWT токена
            String token = jwtTokenUtil.generateToken(user);

            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "user", Map.of(
                            "id", user.getId(),
                            "email", user.getEmail(),
                            "fullName", user.getFullName(),
                            "role", user.getRole(),
                            "avatarUrl", user.getAvatarUrl(),
                            "isActive", user.getIsActive()
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Неверные учетные данные"));
        }
    }

    // ========== ПОДТВЕРЖДЕНИЕ EMAIL ==========
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
            // Генерация токена для автоматического входа после подтверждения
            User user = emailVerificationService.getUserByVerificationToken(token);
            String authToken = jwtTokenUtil.generateToken(user);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Email успешно подтвержден. Теперь вы можете войти в систему.",
                    "token", authToken,
                    "user", Map.of(
                            "id", user.getId(),
                            "email", user.getEmail(),
                            "fullName", user.getFullName(),
                            "role", user.getRole()
                    )
            ));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", "Неверный или просроченный токен верификации."
                    ));
        }
    }

    // ========== ПОВТОРНАЯ ОТПРАВКА ПИСЬМА ==========
    @Operation(summary = "Повторная отправка письма верификации",
            description = "Отправка нового письма для подтверждения email")
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

    // ========== ПРОВЕРКА ТОКЕНА ==========
    @Operation(summary = "Проверка токена верификации",
            description = "Проверка валидности токена верификации")
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

    // ========== ПОЛУЧЕНИЕ ИНФОРМАЦИИ О ТЕКУЩЕМ ПОЛЬЗОВАТЕЛЕ ==========
    @Operation(summary = "Получить информацию о текущем пользователе")
    @ApiResponse(responseCode = "200", description = "Информация о пользователе")
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Требуется авторизация"));
        }

        String token = authHeader.substring(7);
        String email = jwtTokenUtil.getUsernameFromToken(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "fullName", user.getFullName(),
                "role", user.getRole(),
                "avatarUrl", user.getAvatarUrl(),
                "phoneNumber", user.getPhoneNumber(),
                "bio", user.getBio(),
                "registrationDate", user.getRegistrationDate(),
                "isActive", user.getIsActive()
        ));
    }

    // ========== DTO КЛАССЫ ==========
    @Data
    public static class RegisterRequest {
        @jakarta.validation.constraints.Email(message = "Некорректный email")
        @jakarta.validation.constraints.NotBlank(message = "Email обязателен")
        private String email;

        @jakarta.validation.constraints.NotBlank(message = "Пароль обязателен")
        @jakarta.validation.constraints.Size(min = 6, message = "Пароль должен содержать минимум 6 символов")
        private String password;

        @jakarta.validation.constraints.NotBlank(message = "Имя обязательно")
        private String fullName;

        @jakarta.validation.constraints.NotNull(message = "Роль обязательна")
        private UserRole role;

        @jakarta.validation.constraints.Pattern(
                regexp = "^\\+?[0-9\\s\\-\\(\\)]{10,}$",
                message = "Некорректный номер телефона"
        )
        private String phoneNumber;

        private String bio;
        private String avatarUrl;
    }

    @Data
    public static class LoginRequest {
        @jakarta.validation.constraints.Email(message = "Некорректный email")
        @jakarta.validation.constraints.NotBlank(message = "Email обязателен")
        private String email;

        @jakarta.validation.constraints.NotBlank(message = "Пароль обязателен")
        private String password;
    }
}