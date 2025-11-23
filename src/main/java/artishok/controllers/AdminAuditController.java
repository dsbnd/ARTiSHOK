package artishok.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import artishok.entities.AdminAuditLog;
import artishok.services.AdminAuditService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequestMapping("/")
public class AdminAuditController {
	private final AdminAuditService adminAuditService;

	AdminAuditController(AdminAuditService adminAuditService) {
		this.adminAuditService = adminAuditService;
	}

	@GetMapping("/adminlog")
	@ApiResponse(responseCode = "200", description = "Списки действий админа успешно получены")
	@ApiResponse(responseCode = "204", description = "Действия админа не найдены")
	public ResponseEntity<List<AdminAuditLog>> getAllAdminAuditLogs() {
		List<AdminAuditLog> adminlogs = adminAuditService.getAllAdminAuditLogs();
		if (adminlogs.isEmpty()) {
			return ResponseEntity.noContent().build();
		}
		System.out.println("Списки действий админа отправлены");
		return ResponseEntity.ok(adminlogs);
	}

}
