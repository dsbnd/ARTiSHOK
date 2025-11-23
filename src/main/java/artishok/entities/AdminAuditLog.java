package artishok.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_audit_log")
public class AdminAuditLog {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	// Только когда явно обращаемся - тогда загружается
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "admin_id")
	private User admin;

	@Column(name = "action")
	private String action;

	@Column(name = "target_entity_id")
	private Long targetEntityId;

	@Column(name = "timestamp")
	private LocalDateTime timestamp = LocalDateTime.now();

	public AdminAuditLog() {
	}

	public AdminAuditLog(User admin, String action, Long targetEntityId) {
		this.admin = admin;
		this.action = action;
		this.targetEntityId = targetEntityId;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public User getAdmin() {
		return admin;
	}

	public void setAdmin(User admin) {
		this.admin = admin;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public Long getTargetEntityId() {
		return targetEntityId;
	}

	public void setTargetEntityId(Long targetEntityId) {
		this.targetEntityId = targetEntityId;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}
}
