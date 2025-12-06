package artishok.controllers.roles;

import artishok.entities.*;
import artishok.entities.enums.ExhibitionStatus;
import artishok.entities.enums.GalleryStatus;
import artishok.services.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/gallery-owner")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('GALLERY_OWNER', 'ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Владелец галереи", description = "API для владельцев галерей")
public class GalleryOwnerController {

    private final UserService userService;
    private final GalleryService galleryService;
    private final ExhibitionEventService exhibitionEventService;
    private final ExhibitionHallMapService exhibitionHallMapService;
    private final ExhibitionStandService exhibitionStandService;

    // ==================== ГАЛЕРЕИ ====================

    @GetMapping("/galleries")
    @Operation(summary = "Получить мои галереи")
    public ResponseEntity<?> getMyGalleries(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            User currentUser = userService.getCurrentUser();

            // Используем метод сервиса для получения галерей владельца
            List<Map<String, Object>> galleryDTOs = galleryService.getOwnerGalleries(
                    currentUser.getId(), status, page, size
            );

            // Получаем общее количество для пагинации
            List<Gallery> allOwnerGalleries = galleryService.getGalleriesByOwnerId(currentUser.getId());

            // Фильтрация по статусу, если указан
            if (status != null && !status.isEmpty()) {
                try {
                    GalleryStatus galleryStatus = GalleryStatus.valueOf(status.toUpperCase());
                    allOwnerGalleries = allOwnerGalleries.stream()
                            .filter(gallery -> gallery.getStatus() == galleryStatus)
                            .collect(Collectors.toList());
                } catch (IllegalArgumentException e) {
                    // Игнорируем некорректный статус, используем все галереи
                }
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "galleries", galleryDTOs,
                    "total", allOwnerGalleries.size(),
                    "page", page,
                    "size", size
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ошибка получения галерей: " + e.getMessage()));
        }
    }

    @PostMapping("/galleries")
    @Operation(summary = "Создать новую галерею")
    public ResponseEntity<?> createGallery(@RequestBody Map<String, Object> galleryData) {
        try {
            User currentUser = userService.getCurrentUser();

            // Проверка обязательных полей
            if (!galleryData.containsKey("name") || !galleryData.containsKey("address")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Поля name и address обязательны"));
            }

            // Проверка email
            if (!galleryData.containsKey("contactEmail")) {
                galleryData.put("contactEmail", currentUser.getEmail());
            }

            Map<String, Object> createdGallery = galleryService.createGalleryWithOwner(
                    currentUser.getId(), galleryData
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Заявка на создание галереи отправлена на модерацию",
                    "gallery", createdGallery
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Ошибка создания галереи: " + e.getMessage()));
        }
    }

    @PutMapping("/galleries/{id}")
    @Operation(summary = "Обновить информацию о галерее")
    public ResponseEntity<?> updateGallery(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) {

        try {
            User currentUser = userService.getCurrentUser();

            // Проверяем права на обновление галереи
            if (!galleryService.canOwnerUpdateGallery(currentUser.getId(), id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Нет прав на обновление этой галереи"));
            }

            Map<String, Object> updatedGallery = galleryService.updateGallery(id, updates);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Информация о галерее обновлена и отправлена на модерацию",
                    "gallery", updatedGallery
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Ошибка обновления галереи: " + e.getMessage()));
        }
    }

    // ==================== ВЫСТАВКИ (ExhibitionEvent) ====================

    @GetMapping("/exhibitions")
    @Operation(summary = "Получить мои выставки")
    public ResponseEntity<?> getMyExhibitions(
            @RequestParam(required = false) Long galleryId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            User currentUser = userService.getCurrentUser();

            // Получаем все галереи владельца
            List<Gallery> ownerGalleries = galleryService.getGalleriesByOwnerId(currentUser.getId());

            // Если указана конкретная галерея, фильтруем
            if (galleryId != null) {
                Optional<Gallery> specificGallery = ownerGalleries.stream()
                        .filter(g -> g.getId().equals(galleryId))
                        .findFirst();
                if (specificGallery.isPresent()) {
                    ownerGalleries = List.of(specificGallery.get());
                } else {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("error", "Нет прав на просмотр выставок для этой галереи"));
                }
            }

            // Получаем ID галерей владельца
            List<Long> ownerGalleryIds = ownerGalleries.stream()
                    .map(Gallery::getId)
                    .collect(Collectors.toList());

            // Получаем все выставки
            List<ExhibitionEvent> allExhibitions = exhibitionEventService.getAllExhibitionEvents();

            // Фильтруем по галереям владельца
            List<ExhibitionEvent> ownerExhibitions = allExhibitions.stream()
                    .filter(exhibition -> ownerGalleryIds.contains(exhibition.getGallery().getId()))
                    .collect(Collectors.toList());

            // Фильтрация по статусу, если указан
            if (status != null && !status.isEmpty()) {
                try {
                    ExhibitionStatus exhibitionStatus = ExhibitionStatus.valueOf(status.toUpperCase());
                    ownerExhibitions = ownerExhibitions.stream()
                            .filter(exhibition -> exhibition.getStatus() == exhibitionStatus)
                            .collect(Collectors.toList());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Некорректный статус выставки"));
                }
            }

            // Пагинация
            int start = Math.min(page * size, ownerExhibitions.size());
            int end = Math.min(start + size, ownerExhibitions.size());
            List<ExhibitionEvent> paginatedExhibitions = ownerExhibitions.subList(start, end);

            // Преобразование в DTO
            List<Map<String, Object>> exhibitionDTOs = paginatedExhibitions.stream()
                    .map(this::convertExhibitionToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "exhibitions", exhibitionDTOs,
                    "total", ownerExhibitions.size(),
                    "page", page,
                    "size", size
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ошибка получения выставок: " + e.getMessage()));
        }
    }

    @PostMapping("/exhibitions")
    @Operation(summary = "Создать выставку")
    public ResponseEntity<?> createExhibition(@RequestBody Map<String, Object> exhibitionData) {
        try {
            User currentUser = userService.getCurrentUser();

            // Проверка обязательных полей
            if (!exhibitionData.containsKey("title") ||
                    !exhibitionData.containsKey("galleryId") ||
                    !exhibitionData.containsKey("startDate") ||
                    !exhibitionData.containsKey("endDate")) {

                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Поля title, galleryId, startDate и endDate обязательны"));
            }

            Long galleryId;
            try {
                galleryId = Long.parseLong(exhibitionData.get("galleryId").toString());
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Некорректный формат galleryId"));
            }

            // Проверяем права на галерею
            if (!galleryService.canOwnerUpdateGallery(currentUser.getId(), galleryId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Нет прав на создание выставки в этой галерее"));
            }

            // Получаем галерею
            Optional<Gallery> galleryOpt = galleryService.getGalleryById(galleryId);
            if (!galleryOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Галерея не найдена"));
            }

            Gallery gallery = galleryOpt.get();

            // Проверяем, что галерея одобрена (APPROVED)
            if (gallery.getStatus() != GalleryStatus.APPROVED) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Галерея должна быть одобрена (APPROVED) для создания выставки"));
            }

            // Парсим даты
            LocalDateTime startDate, endDate;
            try {
                startDate = LocalDateTime.parse(exhibitionData.get("startDate").toString());
                endDate = LocalDateTime.parse(exhibitionData.get("endDate").toString());
            } catch (DateTimeParseException e) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Некорректный формат даты. Используйте формат ISO (yyyy-MM-dd'T'HH:mm:ss)"));
            }

            // Проверяем, что endDate после startDate
            if (endDate.isBefore(startDate) || endDate.isEqual(startDate)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Дата окончания должна быть позже даты начала"));
            }

            // Проверяем, что дата начала в будущем
            if (startDate.isBefore(LocalDateTime.now())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Дата начала должна быть в будущем"));
            }

            // Создаем выставку
            ExhibitionEvent exhibition = new ExhibitionEvent();
            exhibition.setTitle(exhibitionData.get("title").toString());
            exhibition.setDescription(exhibitionData.containsKey("description") ?
                    exhibitionData.get("description").toString() : "");
            exhibition.setGallery(gallery);
            exhibition.setStartDate(startDate);
            exhibition.setEndDate(endDate);
            exhibition.setStatus(ExhibitionStatus.DRAFT); // По умолчанию DRAFT



            ExhibitionEvent savedExhibition = exhibitionEventService.saveExhibitionEvent(exhibition);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Выставка создана (статус: DRAFT)",
                    "exhibition", convertExhibitionToDTO(savedExhibition)
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Ошибка создания выставки: " + e.getMessage()));
        }
    }

    @PutMapping("/exhibitions/{id}/submit")
    @Operation(summary = "Отправить выставку на модерацию")
    public ResponseEntity<?> submitExhibition(@PathVariable Long id) {
        try {
            User currentUser = userService.getCurrentUser();

            // Получаем выставку
            Optional<ExhibitionEvent> exhibitionOpt = exhibitionEventService.getExhibitionEventById(id);
            if (!exhibitionOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Выставка не найдена"));
            }

            ExhibitionEvent exhibition = exhibitionOpt.get();

            // Проверяем права
            if (!exhibition.getGallery().getOwner().getId().equals(currentUser.getId()) &&
                    !userService.isCurrentUserAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Нет прав на изменение этой выставки"));
            }

            // Меняем статус на ACTIVE (или PENDING в зависимости от логики)
            // В вашей БД статусы: DRAFT, ACTIVE, FINISHED
            exhibition.setStatus(ExhibitionStatus.ACTIVE);
            ExhibitionEvent updatedExhibition = exhibitionEventService.saveExhibitionEvent(exhibition);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Выставка отправлена на модерацию",
                    "exhibition", convertExhibitionToDTO(updatedExhibition)
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Ошибка отправки выставки: " + e.getMessage()));
        }
    }

    @PostMapping("/exhibitions/{id}/hall-map")
    @Operation(summary = "Загрузить карту зала")
    public ResponseEntity<?> uploadHallMap(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {

        try {
            User currentUser = userService.getCurrentUser();
            String mapImageUrl = request.get("mapImageUrl");

            if (mapImageUrl == null || mapImageUrl.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Параметр mapImageUrl обязателен"));
            }

            // Получаем выставку
            Optional<ExhibitionEvent> exhibitionOpt = exhibitionEventService.getExhibitionEventById(id);
            if (!exhibitionOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Выставка не найдена"));
            }

            ExhibitionEvent exhibition = exhibitionOpt.get();

            // Проверяем права
            if (!exhibition.getGallery().getOwner().getId().equals(currentUser.getId()) &&
                    !userService.isCurrentUserAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Нет прав на добавление карты зала к этой выставке"));
            }

            // Создаем карту зала
            ExhibitionHallMap hallMap = new ExhibitionHallMap();
            hallMap.setExhibitionEvent(exhibition);
            hallMap.setMapImageUrl(mapImageUrl);
            hallMap.setName(request.containsKey("name") ? request.get("name") : "Карта зала");

            ExhibitionHallMap savedHallMap = exhibitionHallMapService.saveExhibitionHallMap(hallMap);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Карта зала загружена",
                    "hallMap", convertHallMapToDTO(savedHallMap)
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Ошибка загрузки карты зала: " + e.getMessage()));
        }
    }

    @GetMapping("/exhibitions/{id}/stands")
    @Operation(summary = "Получить стенды для выставки")
    public ResponseEntity<?> getExhibitionStands(@PathVariable Long id) {
        try {
            User currentUser = userService.getCurrentUser();

            // Получаем выставку
            Optional<ExhibitionEvent> exhibitionOpt = exhibitionEventService.getExhibitionEventById(id);
            if (!exhibitionOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Выставка не найдена"));
            }

            ExhibitionEvent exhibition = exhibitionOpt.get();

            // Проверяем права
            if (!exhibition.getGallery().getOwner().getId().equals(currentUser.getId()) &&
                    !userService.isCurrentUserAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Нет прав на просмотр стендов этой выставки"));
            }

            // Получаем карты залов для этой выставки
            List<ExhibitionHallMap> hallMaps = exhibitionHallMapService.getExhibitionHallMapsByEventId(id);

            // Получаем стенды для всех карт залов
            List<ExhibitionStand> stands = hallMaps.stream()
                    .flatMap(hallMap -> exhibitionStandService.getExhibitionStandsByHallMapId(hallMap.getId()).stream())
                    .collect(Collectors.toList());

            // Преобразуем в DTO
            List<Map<String, Object>> standDTOs = stands.stream()
                    .map(this::convertStandToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "stands", standDTOs,
                    "total", standDTOs.size(),
                    "exhibitionId", id
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ошибка получения стендов: " + e.getMessage()));
        }
    }

    // ==================== СТАТИСТИКА ====================

    @GetMapping("/statistics")
    @Operation(summary = "Получить статистику по моим галереям")
    public ResponseEntity<?> getStatistics(
            @RequestParam(required = false) Long galleryId) {

        try {
            User currentUser = userService.getCurrentUser();

            Map<String, Object> stats = galleryService.getGalleryStatistics(
                    currentUser.getId(), galleryId
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "statistics", stats,
                    "ownerId", currentUser.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Ошибка получения статистики: " + e.getMessage()));
        }
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    private Map<String, Object> convertGalleryToDTO(Gallery gallery) {
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
            dto.put("ownerId", gallery.getOwner().getId());
            dto.put("ownerName", gallery.getOwner().getFullName());
            dto.put("ownerEmail", gallery.getOwner().getEmail());
        }

        return dto;
    }

    private Map<String, Object> convertExhibitionToDTO(ExhibitionEvent exhibition) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", exhibition.getId());
        dto.put("title", exhibition.getTitle());
        dto.put("description", exhibition.getDescription());
        dto.put("galleryId", exhibition.getGallery().getId());
        dto.put("galleryName", exhibition.getGallery().getName());
        dto.put("startDate", exhibition.getStartDate());
        dto.put("endDate", exhibition.getEndDate());
        dto.put("status", exhibition.getStatus().toString());


        return dto;
    }

    private Map<String, Object> convertHallMapToDTO(ExhibitionHallMap hallMap) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", hallMap.getId());
        dto.put("exhibitionId", hallMap.getExhibitionEvent().getId());
        dto.put("exhibitionTitle", hallMap.getExhibitionEvent().getTitle());
        dto.put("name", hallMap.getName());
        dto.put("mapImageUrl", hallMap.getMapImageUrl());
        return dto;
    }

    private Map<String, Object> convertStandToDTO(ExhibitionStand stand) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", stand.getId());
        dto.put("standNumber", stand.getStandNumber());
        dto.put("positionX", stand.getPositionX());
        dto.put("positionY", stand.getPositionY());
        dto.put("width", stand.getWidth());
        dto.put("height", stand.getHeight());
        dto.put("type", stand.getType().toString());
        dto.put("status", stand.getStatus().toString());
        dto.put("hallMapId", stand.getExhibitionHallMap().getId());
        return dto;
    }

    // Вспомогательный метод для получения галерей владельца
    private List<Gallery> getGalleriesByOwnerId(Long ownerId) {
        return galleryService.getAllGalleries().stream()
                .filter(gallery -> {
                    if (gallery.getOwner() == null) {
                        return false;
                    }
                    return gallery.getOwner().getId().equals(ownerId);
                })
                .collect(Collectors.toList());
    }
}