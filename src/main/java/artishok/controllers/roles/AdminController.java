package artishok.controllers.roles;

import artishok.entities.User;
import artishok.entities.enums.UserRole;
import artishok.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Администратор", description = "API для администраторов системы")
public class AdminController {

    private final UserService userService;

    @GetMapping("/users")
    @Operation(summary = "Получить всех пользователей")
    public ResponseEntity<?> getAllUsers(
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            List<User> users = userService.getAllUsers();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "users", users,
                    "total", users.size(),
                    "page", page,
                    "size", size
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ошибка получения пользователей"));
        }
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Получить пользователя по ID")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            User user = userService.getUserById(id)
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "user", user
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/users/{id}/activate")
    @Operation(summary = "Активировать/деактивировать пользователя")
    public ResponseEntity<?> toggleUserActivation(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request) {

        try {
            Boolean isActive = request.get("isActive");
            if (isActive == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Параметр isActive обязателен"));
            }

            userService.setUserActive(id, isActive);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", isActive ? "Пользователь активирован" : "Пользователь деактивирован",
                    "userId", id,
                    "isActive", isActive
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/users/{id}/role")
    @Operation(summary = "Изменить роль пользователя")
    public ResponseEntity<?> changeUserRole(
            @PathVariable Long id,
            @RequestBody Map<String, UserRole> request) {

        try {
            UserRole newRole = request.get("role");
            if (newRole == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Параметр role обязателен"));
            }

            userService.changeUserRole(id, newRole);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Роль пользователя изменена",
                    "userId", id,
                    "newRole", newRole.name()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/statistics")
    @Operation(summary = "Получить статистику системы")
    public ResponseEntity<?> getStatistics() {
        try {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "statistics", Map.of(
                            "totalUsers", userService.getTotalUsersCount(),
                            "activeUsers", userService.getActiveUsersCount(),
                            "artists", userService.countUsersByRole(UserRole.ARTIST),
                            "galleryOwners", userService.countUsersByRole(UserRole.GALLERY_OWNER),
                            "admins", userService.countUsersByRole(UserRole.ADMIN)
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ошибка получения статистики"));
        }
    }



    @GetMapping("/system-settings")
    @Operation(summary = "Получить системные настройки")
    public ResponseEntity<?> getSystemSettings() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "settings", Map.of(
                        "appName", "ARTISHOK",
                        "version", "1.0.0",
                        "emailVerificationEnabled", true,
                        "maxFileSize", "10MB",
                        "jwtExpiration", "24 часа"
                )
        ));
    }

    @PostMapping("/users/{id}/reset-password")
    @Operation(summary = "Сбросить пароль пользователя")
    public ResponseEntity<?> resetUserPassword(@PathVariable Long id) {
        try {
            String tempPassword = userService.resetUserPassword(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Пароль пользователя сброшен",
                    "userId", id,
                    "tempPassword", tempPassword,
                    "note", "Передайте временный пароль пользователю"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Ошибка сброса пароля"));
        }
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Удалить пользователя")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            // Вместо реального удаления деактивируем
            userService.setUserActive(id, false);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Пользователь деактивирован",
                    "userId", id
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Ошибка удаления пользователя"));
        }
    }
}