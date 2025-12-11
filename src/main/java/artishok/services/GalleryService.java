package artishok.services;

import artishok.entities.Gallery;
import artishok.entities.GalleryOwnership;
import artishok.entities.User;
import artishok.entities.enums.GalleryStatus;
import artishok.entities.enums.UserRole;
import artishok.repositories.GalleryOwnershipRepository;
import artishok.repositories.GalleryRepository;
import artishok.repositories.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class GalleryService {
	@Autowired
	private GalleryRepository galleryRepository;

	@Autowired
	private GalleryOwnershipRepository galleryOwnershipRepository;
	@Autowired
	private UserService userService;

	public List<Gallery> getAllGalleries() {
		List<Gallery> galleries = galleryRepository.findAll();

		galleries.forEach(this::loadOwnerAndDateForGallery);
		return galleries;
	}

	public Optional<Gallery> getGalleryById(Long id) {
		Optional<Gallery> galleryOpt = galleryRepository.findById(id);
		galleryOpt.ifPresent(this::loadOwnerAndDateForGallery);
		return galleryOpt;
	}

	public Gallery saveGallery(Gallery gallery) {
		Gallery savedGallery = galleryRepository.save(gallery);
		loadOwnerAndDateForGallery(savedGallery);
		return savedGallery;
	}

	public void deleteGallery(Long id) {

		galleryOwnershipRepository.findByGalleryId(id).forEach(galleryOwnershipRepository::delete);

		galleryRepository.deleteById(id);
	}

	/**
	 * Создать галерею и назначить владельца
	 */
	public Map<String, Object> createGalleryWithOwner(Long ownerId, Map<String, Object> galleryData) {

		User owner = userService.getUserById(ownerId).orElseThrow(() -> new RuntimeException("Владелец не найден"));

		if (owner.getRole() != UserRole.GALLERY_OWNER && owner.getRole() != UserRole.ADMIN) {
			throw new RuntimeException("Только владельцы галерей и администраторы могут создавать галереи");
		}

		if (!galleryData.containsKey("name") || !galleryData.containsKey("address")) {
			throw new RuntimeException("Поля name и address обязательны");
		}

		Gallery gallery = new Gallery();
		gallery.setName(galleryData.get("name").toString());
		gallery.setAddress(galleryData.get("address").toString());
		gallery.setDescription(galleryData.containsKey("description") ? galleryData.get("description").toString() : "");
		gallery.setContactPhone(
				galleryData.containsKey("contactPhone") ? galleryData.get("contactPhone").toString() : "");
		gallery.setContactEmail(galleryData.containsKey("contactEmail") ? galleryData.get("contactEmail").toString()
				: owner.getEmail());
		gallery.setLogoUrl(galleryData.containsKey("logoUrl") ? galleryData.get("logoUrl").toString() : null);

		gallery.setAdminComment(null);

		Gallery savedGallery = galleryRepository.save(gallery);

		GalleryOwnership ownership = new GalleryOwnership();
		ownership.setGallery(savedGallery);
		ownership.setOwner(owner);
		ownership.setIsPrimary(true);
		ownership.setCreatedAt(LocalDateTime.now());

		galleryOwnershipRepository.save(ownership);

		loadOwnerAndDateForGallery(savedGallery);

		return convertToDTO(savedGallery);
	}

	public Optional<User> getGalleryOwner(Long galleryId) {
		return galleryRepository.findPrimaryOwnerByGalleryId(galleryId);
	}

	/**
	 * Получить галереи владельца
	 */
	public List<Map<String, Object>> getOwnerGalleries(Long ownerId, String status, int page, int size) {
		List<Gallery> galleries;

		if (status != null && !status.isEmpty()) {
			try {
				GalleryStatus galleryStatus = GalleryStatus.valueOf(status.toUpperCase());
				galleries = galleryRepository.findByOwnerIdAndStatus(ownerId, galleryStatus);
			} catch (IllegalArgumentException e) {
				galleries = galleryRepository.findByOwnerId(ownerId);
			}
		} else {
			galleries = galleryRepository.findByOwnerId(ownerId);
		}

		galleries.forEach(this::loadOwnerAndDateForGallery);

		int start = Math.min(page * size, galleries.size());
		int end = Math.min(start + size, galleries.size());
		List<Gallery> paginatedGalleries = galleries.subList(start, end);

		return paginatedGalleries.stream().map(this::convertToDTO).collect(Collectors.toList());
	}

	/**
	 * Проверить, может ли владелец обновить галерею
	 */
	public boolean canOwnerUpdateGallery(Long ownerId, Long galleryId) {
		return galleryOwnershipRepository.existsByGalleryIdAndOwnerId(galleryId, ownerId);
	}

	/**
	 * Обновить галерею от имени владельца
	 */
	public Map<String, Object> updateGallery(Long galleryId, Map<String, Object> updates) {
		Gallery gallery = galleryRepository.findById(galleryId)
				.orElseThrow(() -> new RuntimeException("Галерея не найдена"));

		if (updates.containsKey("name")) {
			gallery.setName(updates.get("name").toString());
		}
		if (updates.containsKey("address")) {
			gallery.setAddress(updates.get("address").toString());
		}
		if (updates.containsKey("description")) {
			gallery.setDescription(updates.get("description").toString());
		}
		if (updates.containsKey("contactPhone")) {
			gallery.setContactPhone(updates.get("contactPhone").toString());
		}
		if (updates.containsKey("contactEmail")) {
			gallery.setContactEmail(updates.get("contactEmail").toString());
		}
		if (updates.containsKey("logoUrl")) {
			gallery.setLogoUrl(updates.get("logoUrl").toString());
		}

		if (gallery.getStatus() == GalleryStatus.APPROVED) {
			gallery.setStatus(GalleryStatus.PENDING);
			gallery.setAdminComment(null);
		}

		Gallery updatedGallery = galleryRepository.save(gallery);
		loadOwnerAndDateForGallery(updatedGallery);

		return convertToDTO(updatedGallery);
	}

	/**
	 * Получить статистику галереи для владельца
	 */
	public Map<String, Object> getGalleryStatistics(Long ownerId, Long galleryId) {
		Map<String, Object> stats = new HashMap<>();

		List<Gallery> ownerGalleries;
		if (galleryId != null) {
			ownerGalleries = galleryRepository.findByOwnerId(ownerId).stream().filter(g -> g.getId().equals(galleryId))
					.collect(Collectors.toList());
		} else {
			ownerGalleries = galleryRepository.findByOwnerId(ownerId);
		}

		ownerGalleries.forEach(this::loadOwnerAndDateForGallery);

		stats.put("totalGalleries", ownerGalleries.size());
		stats.put("pendingGalleries", countByStatus(ownerGalleries, GalleryStatus.PENDING));
		stats.put("approvedGalleries", countByStatus(ownerGalleries, GalleryStatus.APPROVED));
		stats.put("rejectedGalleries", countByStatus(ownerGalleries, GalleryStatus.REJECTED));

		if (galleryId != null && !ownerGalleries.isEmpty()) {
			Gallery gallery = ownerGalleries.get(0);
			Map<String, Object> galleryDetails = convertToDTO(gallery);
			stats.put("galleryDetails", galleryDetails);
		}

		return stats;
	}

	/**
	 * Загрузить владельца и дату создания для галереи
	 */
	private void loadOwnerAndDateForGallery(Gallery gallery) {

		Optional<User> primaryOwnerOpt = galleryRepository.findPrimaryOwnerByGalleryId(gallery.getId());
		if (primaryOwnerOpt.isPresent()) {
			gallery.setOwner(primaryOwnerOpt.get());
		}

		Optional<GalleryOwnership> ownershipOpt = galleryOwnershipRepository.findPrimaryOwner(gallery.getId());
		if (ownershipOpt.isPresent()) {
			gallery.setCreatedAt(ownershipOpt.get().getCreatedAt());
		}
	}

	private Map<String, Object> convertToDTO(Gallery gallery) {
		Map<String, Object> dto = new HashMap<>();
		dto.put("id", gallery.getId());
		dto.put("name", gallery.getName());
		dto.put("description", gallery.getDescription());
		dto.put("address", gallery.getAddress());
		dto.put("contactPhone", gallery.getContactPhone());
		dto.put("contactEmail", gallery.getContactEmail());
		dto.put("logoUrl", gallery.getLogoUrl());
		dto.put("status", gallery.getStatus().toString());
		dto.put("adminComment", gallery.getAdminComment());

		if (gallery.getCreatedAt() != null) {
			dto.put("createdAt", gallery.getCreatedAt());
		}

		if (gallery.getOwner() != null) {
			Map<String, Object> ownerInfo = new HashMap<>();
			ownerInfo.put("id", gallery.getOwner().getId());
			ownerInfo.put("fullName", gallery.getOwner().getFullName());
			ownerInfo.put("email", gallery.getOwner().getEmail());
			ownerInfo.put("phone", gallery.getOwner().getPhoneNumber());
			dto.put("owner", ownerInfo);
		}

		return dto;
	}

	private long countByStatus(List<Gallery> galleries, GalleryStatus status) {
		return galleries.stream().filter(gallery -> gallery.getStatus() == status).count();
	}

	/**
	 * Получить галереи по статусу
	 */
	public List<Gallery> getGalleriesByStatus(GalleryStatus status) {
		List<Gallery> galleries = galleryRepository.findByStatus(status);
		galleries.forEach(this::loadOwnerAndDateForGallery);
		return galleries;
	}

	/**
	 * Получить все галереи, ожидающие модерации
	 */
	public List<Gallery> getPendingGalleries() {
		return getGalleriesByStatus(GalleryStatus.PENDING);
	}

	/**
	 * Получить одобренные галереи
	 */
	public List<Gallery> getApprovedGalleries() {
		return getGalleriesByStatus(GalleryStatus.APPROVED);
	}

	/**
	 * Изменить статус галереи (для модератора/админа)
	 */
	public Gallery changeGalleryStatus(Long galleryId, GalleryStatus status, String adminComment) {
		Gallery gallery = galleryRepository.findById(galleryId)
				.orElseThrow(() -> new RuntimeException("Галерея не найдена"));

		gallery.setStatus(status);
		if (adminComment != null) {
			gallery.setAdminComment(adminComment);
		}

		Gallery updatedGallery = galleryRepository.save(gallery);
		loadOwnerAndDateForGallery(updatedGallery);
		return updatedGallery;
	}

	/**
	 * Поиск галерей по имени
	 */
	public List<Gallery> searchGalleriesByName(String name) {
		List<Gallery> galleries = galleryRepository.findByNameContainingIgnoreCase(name);
		galleries.forEach(this::loadOwnerAndDateForGallery);
		return galleries;
	}

	/**
	 * Проверить, существует ли галерея с таким именем
	 */
	public boolean existsByName(String name) {
		return galleryRepository.existsByName(name);
	}

	/**
	 * Проверить, существует ли галерея с таким email
	 */
	public boolean existsByContactEmail(String email) {
		return galleryRepository.existsByContactEmail(email);
	}

	public List<Gallery> getGalleriesByOwnerId(Long ownerId) {
		return galleryRepository.findByOwnerId(ownerId);
	}
}