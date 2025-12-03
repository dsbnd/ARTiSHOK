package artishok.controllers;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import artishok.entities.User;
import artishok.entities.enums.UserRole;
import artishok.services.UserService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/users")
public class UserController {
	private final UserService userService;

	UserController(UserService userService) {
		this.userService = userService;
	}

	@PostMapping
	@ApiResponse(responseCode = "201", description = "Пользователь успешно создан")
	@ApiResponse(responseCode = "400", description = "Неверные входные данные")
	@ApiResponse(responseCode = "409", description = "Пользователь с таким email уже существует")
	public ResponseEntity<?> createUser(
			@Parameter(description = "Данные для создания пользователя", required = true) @Valid @RequestBody Map<String, Object> userData) {

		try {
			User user = new User();
			user.setEmail((String) userData.get("email"));
			user.setFullName((String) userData.get("fullName"));

			String roleStr = (String) userData.get("role");
			UserRole role = UserRole.valueOf(roleStr.toUpperCase());
			user.setRole(role);

			user.setPhoneNumber((String) userData.get("phoneNumber"));
			user.setBio((String) userData.get("bio"));
			user.setAvatarUrl((String) userData.get("avatarUrl"));

			String password = (String) userData.get("password");

			User createdUser = userService.createUser(user, password);

			Map<String, Object> response = Map.of("user", createdUser, "message",
					"Пользователь создан. Пожалуйста, проверьте вашу почту для подтверждения email.");

			System.out.println("Пользователь успешно создан");
			return ResponseEntity.status(HttpStatus.CREATED).body(response);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
		}
	}

	@ApiResponse(responseCode = "200", description = "Пользователь найден")
	@ApiResponse(responseCode = "404", description = "Пользователь не найден")
	@GetMapping("/{id}")
	public ResponseEntity<?> getUserById(
			@Parameter(description = "ID пользователя", required = true) @PathVariable("id") Long id) {

		Optional<User> userOptional = userService.getUserById(id);
		if (userOptional.isPresent()) {
			User user = userOptional.get();
			return ResponseEntity.ok(user);
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Пользователь не найден"));
		}
	}

	@ApiResponse(responseCode = "200", description = "Пользователь найден")
	@ApiResponse(responseCode = "404", description = "Пользователь не найден")
	@GetMapping("/email/{email}")
	public ResponseEntity<?> getUserByEmail(
			@Parameter(description = "Email пользователя", required = true, example = "user@example.com") @PathVariable("email") String email) {
		Optional<User> userOptional = userService.getUserByEmail(email);
		if (userOptional.isPresent()) {
			User user = userOptional.get();
			return ResponseEntity.ok(user);
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Пользователь не найден"));
		}
	}

	@ApiResponse(responseCode = "200", description = "Список пользователей получен")
	@GetMapping
	public ResponseEntity<List<User>> getAllUsers() {
		List<User> users = userService.getAllUsers();
		return ResponseEntity.ok(users);
	}

	@ApiResponse(responseCode = "200", description = "Пользователь успешно обновлен")
	@ApiResponse(responseCode = "404", description = "Пользователь не найден")
	@PutMapping("/{id}")
	public ResponseEntity<?> updateUser(@PathVariable("id") Long id, @RequestBody Map<String, Object> updates) {

		try {
			User user = new User();

			if (updates.containsKey("fullName")) {
				user.setFullName((String) updates.get("fullName"));
			}
			if (updates.containsKey("phoneNumber")) {
				user.setPhoneNumber((String) updates.get("phoneNumber"));
			}
			if (updates.containsKey("bio")) {
				user.setBio((String) updates.get("bio"));
			}
			if (updates.containsKey("avatarUrl")) {
				user.setAvatarUrl((String) updates.get("avatarUrl"));
			}
			if (updates.containsKey("isActive")) {
				user.setIsActive((Boolean) updates.get("isActive"));
			}

			User updatedUser = userService.updateUser(id, user);
			return ResponseEntity.ok(updatedUser);
		} catch (RuntimeException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Пользователь не найден"));
		}
	}

	@ApiResponse(responseCode = "204", description = "Пользователь деактивирован")
	@ApiResponse(responseCode = "404", description = "Пользователь не найден")
	@DeleteMapping("/{id}")
	public ResponseEntity<?> deactivateUser(
			@Parameter(description = "ID пользователя", required = true, example = "1") @PathVariable("id") Long id) {
		if (!userService.userExists(id)) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Пользователь не найден"));
		}

		userService.deactivateUser(id);
		return ResponseEntity.noContent().build();
	}

	@ApiResponse(responseCode = "204", description = "Пользователь активирован")
	@ApiResponse(responseCode = "404", description = "Пользователь не найден")
	@PostMapping("/{id}/activate")
	public ResponseEntity<?> activateUser(@PathVariable("id") Long id) {

		if (!userService.userExists(id)) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Пользователь не найден"));
		}

		userService.activateUser(id);
		return ResponseEntity.noContent().build();
	}

	@ApiResponse(responseCode = "200", description = "Список пользователей получен")
	@GetMapping("/role/{role}")
	public ResponseEntity<?> getUsersByRole(@PathVariable("role") String role) {

		try {
			UserRole userRole = UserRole.valueOf(role.toUpperCase());
			List<User> users = userService.getUsersByRole(userRole);
			return ResponseEntity.ok(users);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Неверная роль: " + role));
		}
	}

	@ApiResponse(responseCode = "200", description = "Список активных пользователей получен")
	@GetMapping("/active")
	public ResponseEntity<List<User>> getActiveUsers() {
		List<User> users = userService.getActiveUsers();
		return ResponseEntity.ok(users);
	}

	@ApiResponse(responseCode = "200", description = "Статистика получена")
	@GetMapping("/stats/count-by-role")
	public ResponseEntity<Map<String, Object>> getUserCountByRole() {
		Map<String, Object> stats = Map.of("adminCount", userService.countUsersByRole(UserRole.ADMIN),
				"galleryOwnerCount", userService.countUsersByRole(UserRole.GALLERY_OWNER), "artistCount",
				userService.countUsersByRole(UserRole.ARTIST), "totalCount", userService.getAllUsers().size());
		return ResponseEntity.ok(stats);
	}

	@ApiResponse(responseCode = "204", description = "Пароль успешно изменен")
	@ApiResponse(responseCode = "404", description = "Пользователь не найден")
	@PostMapping("/{id}/change-password")
	public ResponseEntity<?> changePassword(@PathVariable("id") Long id, @RequestParam("password") String newPassword) {

		if (!userService.userExists(id)) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Пользователь не найден"));
		}

		userService.changePassword(id, newPassword);
		return ResponseEntity.noContent().build();
	}

	@ApiResponse(responseCode = "200", description = "Результат проверки")
	@GetMapping("/check-email")
	public ResponseEntity<Map<String, Object>> checkEmailExists(@RequestParam("email") String email) {

		boolean exists = userService.emailExists(email);
		return ResponseEntity.ok(Map.of("email", email, "exists", exists));
	}

	@ApiResponse(responseCode = "200", description = "Список последних пользователей")
	@GetMapping("/recent")
	public ResponseEntity<List<User>> getRecentlyRegisteredUsers() {
		List<User> users = userService.getRecentlyRegisteredUsers();
		return ResponseEntity.ok(users);
	}

	@ApiResponse(responseCode = "200", description = "Результат проверки")
	@GetMapping("/check-activation")
	public ResponseEntity<?> checkUserActivation(
			@RequestParam("email") String email) {

		return userService.getUserByEmail(email)
				.map(user -> ResponseEntity.ok(Map.of("email", email, "isActive", user.getIsActive(), "message",
						user.getIsActive() ? "Пользователь активирован"
								: "Пользователь не активирован. Проверьте email для подтверждения.")))
				.orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Пользователь не найден")));
	}

}
