package artishok.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import artishok.entities.enums.GalleryStatus;

@Entity
@Table(name = "gallery")
@Getter
@Setter
public class Gallery {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	private String description;
	@Column(nullable = false)
	private String address;

	@Column(name = "contact_phone")
	private String contactPhone;

	@Column(name = "contact_email", nullable = false)
	private String contactEmail;

	@Column(name = "logo_url")
	private String logoUrl;
	@Transient
	private User owner;
	@Transient
	private java.time.LocalDateTime createdAt;

	public java.time.LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(java.time.LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public User getOwner() {
		return owner;
	}

	public void setOwner(User owner) {
		this.owner = owner;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getContactPhone() {
		return contactPhone;
	}

	public void setContactPhone(String contactPhone) {
		this.contactPhone = contactPhone;
	}

	public String getContactEmail() {
		return contactEmail;
	}

	public void setContactEmail(String contactEmail) {
		this.contactEmail = contactEmail;
	}

	public String getLogoUrl() {
		return logoUrl;
	}

	public void setLogoUrl(String logoUrl) {
		this.logoUrl = logoUrl;
	}

	public GalleryStatus getStatus() {
		return status;
	}

	public void setStatus(GalleryStatus status) {
		this.status = status;
	}

	public String getAdminComment() {
		return adminComment;
	}

	public void setAdminComment(String adminComment) {
		this.adminComment = adminComment;
	}

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private GalleryStatus status = GalleryStatus.PENDING;

	@Column(name = "admin_comment")
	private String adminComment;

}
