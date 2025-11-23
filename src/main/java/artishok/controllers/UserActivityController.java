package artishok.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import artishok.entities.UserActivityLog;
import artishok.services.UserActivityService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequestMapping("/")
public class UserActivityController {
	private final UserActivityService userActivityService;

	UserActivityController(UserActivityService userActivityService) {
		this.userActivityService = userActivityService;
	}

	@GetMapping("/userlogs")
	@ApiResponse(responseCode = "200", description = "Списки действий пользователей успешно получены")
	@ApiResponse(responseCode = "204", description = "Действия пользователей не найдены")
	public ResponseEntity<List<UserActivityLog>> getAllUserActivityLogs() {
		List<UserActivityLog> userlogs = userActivityService.getAllUserActivityLogs();
		if (userlogs.isEmpty()) {
			return ResponseEntity.noContent().build();
		}
		System.out.println("Списки действий пользователей отправлены");
		return ResponseEntity.ok(userlogs);
	}

}
