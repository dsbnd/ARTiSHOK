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
    private final GalleryRepository galleryRepository;
    private final GalleryOwnershipRepository galleryOwnershipRepository;
    private final UserService userService;
//    GalleryService(UserService userService, GalleryRepository galleryRepository,
//    		GalleryOwnershipRepository galleryOwnershipRepository) {
//		this.galleryRepository = galleryRepository;
//		this.userService = userService;
//		this.galleryOwnershipRepository = galleryOwnershipRepository;
//	}

    // ==================== ОСНОВНЫЕ МЕТОДЫ ====================

    public List<Gallery> getAllGalleries() {
        List<Gallery> galleries = galleryRepository.findAll();
        // Заполняем поле owner и дату создания для каждой галереи
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
        // Сначала удаляем связи владения
        galleryOwnershipRepository.findByGalleryId(id)
                .forEach(galleryOwnershipRepository::delete);
        // Затем удаляем галерею
        galleryRepository.deleteById(id);
    }

    // ==================== МЕТОДЫ ДЛЯ ВЛАДЕНИЯ ====================

    /**
     * Создать галерею и назначить владельца
     */
    public Map<String, Object> createGalleryWithOwner(Long ownerId, Map<String, Object> galleryData) {
        // Получаем владельца
        User owner = userService.getUserById(ownerId)
                .orElseThrow(() -> new RuntimeException("Владелец не найден"));

        // Проверяем роль владельца
        if (owner.getRole() != UserRole.GALLERY_OWNER && owner.getRole() != UserRole.ADMIN) {
            throw new RuntimeException("Только владельцы галерей и администраторы могут создавать галереи");
        }

        // Валидация обязательных полей
        if (!galleryData.containsKey("name") || !galleryData.containsKey("address")) {
            throw new RuntimeException("Поля name и address обязательны");
        }

        // Создаем галерею
        Gallery gallery = new Gallery();
        gallery.setName(galleryData.get("name").toString());
        gallery.setAddress(galleryData.get("address").toString());
        gallery.setDescription(galleryData.containsKey("description") ?
                galleryData.get("description").toString() : "");
        gallery.setContactPhone(galleryData.containsKey("contactPhone") ?
                galleryData.get("contactPhone").toString() : "");
        gallery.setContactEmail(galleryData.containsKey("contactEmail") ?
                galleryData.get("contactEmail").toString() : owner.getEmail());
        gallery.setLogoUrl(galleryData.containsKey("logoUrl") ?
                galleryData.get("logoUrl").toString() : null);
//        gallery.setStatus(GalleryStatus.PENDING);
        gallery.setAdminComment(null);

        // Сохраняем галерею
        Gallery savedGallery = galleryRepository.save(gallery);

        // Создаем связь владения
        GalleryOwnership ownership = new GalleryOwnership();
        ownership.setGallery(savedGallery);
        ownership.setOwner(owner);
        ownership.setIsPrimary(true);
        ownership.setCreatedAt(LocalDateTime.now());

        galleryOwnershipRepository.save(ownership);

        // Загружаем владельца и дату создания для галереи
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

        // Загружаем владельцев и даты создания для каждой галереи
        galleries.forEach(this::loadOwnerAndDateForGallery);

        // Применяем пагинацию
        int start = Math.min(page * size, galleries.size());
        int end = Math.min(start + size, galleries.size());
        List<Gallery> paginatedGalleries = galleries.subList(start, end);

        return paginatedGalleries.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
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

        // Обновляем поля
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

        // Если галерея была APPROVED, возвращаем на модерацию после изменений
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

        // Получаем галереи владельца
        List<Gallery> ownerGalleries;
        if (galleryId != null) {
            ownerGalleries = galleryRepository.findByOwnerId(ownerId).stream()
                    .filter(g -> g.getId().equals(galleryId))
                    .collect(Collectors.toList());
        } else {
            ownerGalleries = galleryRepository.findByOwnerId(ownerId);
        }

        // Загружаем владельцев и даты
        ownerGalleries.forEach(this::loadOwnerAndDateForGallery);

        // Основная статистика
        stats.put("totalGalleries", ownerGalleries.size());
        stats.put("pendingGalleries", countByStatus(ownerGalleries, GalleryStatus.PENDING));
        stats.put("approvedGalleries", countByStatus(ownerGalleries, GalleryStatus.APPROVED));
        stats.put("rejectedGalleries", countByStatus(ownerGalleries, GalleryStatus.REJECTED));

        // Если нужна статистика по конкретной галерее
        if (galleryId != null && !ownerGalleries.isEmpty()) {
            Gallery gallery = ownerGalleries.get(0);
            Map<String, Object> galleryDetails = convertToDTO(gallery);
            stats.put("galleryDetails", galleryDetails);
        }

        return stats;
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    /**
     * Загрузить владельца и дату создания для галереи
     */
    private void loadOwnerAndDateForGallery(Gallery gallery) {
        // Загружаем основного владельца
        Optional<User> primaryOwnerOpt = galleryRepository.findPrimaryOwnerByGalleryId(gallery.getId());
        if (primaryOwnerOpt.isPresent()) {
            gallery.setOwner(primaryOwnerOpt.get());
        }

        // Загружаем дату создания из gallery_ownership
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

        // Добавляем дату создания, если есть
        if (gallery.getCreatedAt() != null) {
            dto.put("createdAt", gallery.getCreatedAt());
        }

        // Добавляем информацию о владельце, если она есть
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
        return galleries.stream()
                .filter(gallery -> gallery.getStatus() == status)
                .count();
    }

    // ==================== ДОПОЛНИТЕЛЬНЫЕ МЕТОДЫ ====================

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