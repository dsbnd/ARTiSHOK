package artishok.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import artishok.entities.User;
import artishok.services.UserService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequestMapping("/")
public class UserController {
	private final UserService userService;
	
	UserController(UserService userService){
		this.userService = userService;
	}
	
	@GetMapping("/users")
	@ApiResponse(responseCode = "200", description = "Списки пользователей успешно получены")
	@ApiResponse(responseCode = "204", description = "Пользователи не найдены")
	public ResponseEntity<List<User>> getAllRoutes() {
		List<User> users = userService.getAllUsers();
		if (users.isEmpty()) {
			return ResponseEntity.noContent().build();
		}
		System.out.println("Списки пользователей отправлены");
		return ResponseEntity.ok(users);
	}

}
