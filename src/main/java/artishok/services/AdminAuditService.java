package artishok.services;

import java.util.List;

import org.springframework.stereotype.Service;

import artishok.entities.AdminAuditLog;
import artishok.repositories.AdminAuditRepository;

@Service
public class AdminAuditService {
	private final AdminAuditRepository adminAuditRepository;

	AdminAuditService(AdminAuditRepository adminAuditRepository) {
		this.adminAuditRepository = adminAuditRepository;
	}

	public List<AdminAuditLog> getAllAdminAuditLogs() {
		return adminAuditRepository.findAll();
	}
}
