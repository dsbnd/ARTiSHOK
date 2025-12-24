package artishok.controllers.roles;

import artishok.entities.*;
import artishok.entities.enums.*;
import artishok.services.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/gallery-owner")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('GALLERY_OWNER', 'ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Владелец галереи", description = "API для владельцев галерей")
public class GalleryOwnerController {
	@Autowired
	private UserService userService;
	@Autowired
	private GalleryService galleryService;
	@Autowired
	private BookingService bookingService;
	@Autowired
	private ExhibitionEventService exhibitionEventService;
	@Autowired
	private ExhibitionHallMapService exhibitionHallMapService;
	@Autowired
	private ExhibitionStandService exhibitionStandService;

	@GetMapping("/galleries")
	@Operation(summary = "Получить мои галереи")
	public ResponseEntity<?> getMyGalleries(@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "10") int size) {

		try {
			User currentUser = userService.getCurrentUser();

			List<Map<String, Object>> galleryDTOs = galleryService.getOwnerGalleries(currentUser.getId(), status, page,
					size);

			List<Gallery> allOwnerGalleries = galleryService.getGalleriesByOwnerId(currentUser.getId());

			if (status != null && !status.isEmpty()) {
				try {
					GalleryStatus galleryStatus = GalleryStatus.valueOf(status.toUpperCase());
					allOwnerGalleries = allOwnerGalleries.stream()
							.filter(gallery -> gallery.getStatus() == galleryStatus).collect(Collectors.toList());
				} catch (IllegalArgumentException e) {

				}
			}

			return ResponseEntity.ok(Map.of("success", true, "galleries", galleryDTOs, "total",
					allOwnerGalleries.size(), "page", page, "size", size));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", "Ошибка получения галерей: " + e.getMessage()));
		}
	}

	@PostMapping("/create-gallery")
	@Operation(summary = "Создать новую галерею")
	public ResponseEntity<?> createGallery(@RequestBody Map<String, Object> galleryData) {
		try {
			User currentUser = userService.getCurrentUser();

			if (!galleryData.containsKey("name") || !galleryData.containsKey("address")) {
				return ResponseEntity.badRequest().body(Map.of("error", "Поля name и address обязательны"));
			}

			if (!galleryData.containsKey("contactEmail")) {
				galleryData.put("contactEmail", currentUser.getEmail());
			}

			Map<String, Object> createdGallery = galleryService.createGalleryWithOwner(currentUser.getId(),
					galleryData);

			return ResponseEntity.ok(Map.of("success", true, "message",
					"Заявка на создание галереи отправлена на модерацию", "gallery", createdGallery));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Map.of("error", "Ошибка создания галереи: " + e.getMessage()));
		}
	}

	@PutMapping("/galleries/{id}")
	@Operation(summary = "Обновить информацию о галерее")
	public ResponseEntity<?> updateGallery(@PathVariable("id") Long id, @RequestBody Map<String, Object> updates) {

		try {
			User currentUser = userService.getCurrentUser();

			if (!galleryService.canOwnerUpdateGallery(currentUser.getId(), id)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN)
						.body(Map.of("error", "Нет прав на обновление этой галереи"));
			}

			Map<String, Object> updatedGallery = galleryService.updateGallery(id, updates);

			return ResponseEntity.ok(Map.of("success", true, "message",
					"Информация о галерее обновлена и отправлена на модерацию", "gallery", updatedGallery));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Map.of("error", "Ошибка обновления галереи: " + e.getMessage()));
		}
	}

	@GetMapping("/exhibitions")
	@Operation(summary = "Получить мои выставки")
	public ResponseEntity<?> getMyExhibitions(@RequestParam(value = "galleryId", required = false) Long galleryId,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "10") int size) {

		try {
			User currentUser = userService.getCurrentUser();

			List<Gallery> ownerGalleries = galleryService.getGalleriesByOwnerId(currentUser.getId());

			if (galleryId != null) {
				Optional<Gallery> specificGallery = ownerGalleries.stream().filter(g -> g.getId().equals(galleryId))
						.findFirst();
				if (specificGallery.isPresent()) {
					ownerGalleries = List.of(specificGallery.get());
				} else {
					return ResponseEntity.status(HttpStatus.FORBIDDEN)
							.body(Map.of("error", "Нет прав на просмотр выставок для этой галереи"));
				}
			}

			List<Long> ownerGalleryIds = ownerGalleries.stream().map(Gallery::getId).collect(Collectors.toList());

			List<ExhibitionEvent> allExhibitions = exhibitionEventService.getAllExhibitionEvents();

			List<ExhibitionEvent> ownerExhibitions = allExhibitions.stream()
					.filter(exhibition -> ownerGalleryIds.contains(exhibition.getGallery().getId()))
					.collect(Collectors.toList());

			if (status != null && !status.isEmpty()) {
				try {
					ExhibitionStatus exhibitionStatus = ExhibitionStatus.valueOf(status.toUpperCase());
					ownerExhibitions = ownerExhibitions.stream()
							.filter(exhibition -> exhibition.getStatus() == exhibitionStatus)
							.collect(Collectors.toList());
				} catch (IllegalArgumentException e) {
					return ResponseEntity.badRequest().body(Map.of("error", "Некорректный статус выставки"));
				}
			}

			int start = Math.min(page * size, ownerExhibitions.size());
			int end = Math.min(start + size, ownerExhibitions.size());
			List<ExhibitionEvent> paginatedExhibitions = ownerExhibitions.subList(start, end);

			List<Map<String, Object>> exhibitionDTOs = paginatedExhibitions.stream().map(this::convertExhibitionToDTO)
					.collect(Collectors.toList());

			return ResponseEntity.ok(Map.of("success", true, "exhibitions", exhibitionDTOs, "total",
					ownerExhibitions.size(), "page", page, "size", size));
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

			if (!exhibitionData.containsKey("title") || !exhibitionData.containsKey("galleryId")
					|| !exhibitionData.containsKey("startDate") || !exhibitionData.containsKey("endDate")) {

				return ResponseEntity.badRequest()
						.body(Map.of("error", "Поля title, galleryId, startDate и endDate обязательны"));
			}

			Long galleryId;
			try {
				galleryId = Long.parseLong(exhibitionData.get("galleryId").toString());
			} catch (NumberFormatException e) {
				return ResponseEntity.badRequest().body(Map.of("error", "Некорректный формат galleryId"));
			}

			if (!galleryService.canOwnerUpdateGallery(currentUser.getId(), galleryId)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN)
						.body(Map.of("error", "Нет прав на создание выставки в этой галерее"));
			}

			Optional<Gallery> galleryOpt = galleryService.getGalleryById(galleryId);
			if (!galleryOpt.isPresent()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Галерея не найдена"));
			}

			Gallery gallery = galleryOpt.get();

			if (gallery.getStatus() != GalleryStatus.APPROVED) {
				return ResponseEntity.badRequest()
						.body(Map.of("error", "Галерея должна быть одобрена (APPROVED) для создания выставки"));
			}

			LocalDateTime startDate, endDate;
			try {
				startDate = LocalDateTime.parse(exhibitionData.get("startDate").toString());
				endDate = LocalDateTime.parse(exhibitionData.get("endDate").toString());
			} catch (DateTimeParseException e) {
				return ResponseEntity.badRequest().body(
						Map.of("error", "Некорректный формат даты. Используйте формат ISO (yyyy-MM-dd'T'HH:mm:ss)"));
			}

			if (endDate.isBefore(startDate) || endDate.isEqual(startDate)) {
				return ResponseEntity.badRequest()
						.body(Map.of("error", "Дата окончания должна быть позже даты начала"));
			}

			if (startDate.isBefore(LocalDateTime.now())) {
				return ResponseEntity.badRequest().body(Map.of("error", "Дата начала должна быть в будущем"));
			}

			ExhibitionEvent exhibition = new ExhibitionEvent();
			exhibition.setTitle(exhibitionData.get("title").toString());
			exhibition.setDescription(
					exhibitionData.containsKey("description") ? exhibitionData.get("description").toString() : "");
			exhibition.setGallery(gallery);
			exhibition.setStartDate(startDate);
			exhibition.setEndDate(endDate);
			exhibition.setStatus(ExhibitionStatus.ACTIVE);

			ExhibitionEvent savedExhibition = exhibitionEventService.saveExhibitionEvent(exhibition);
			
			return ResponseEntity.ok(Map.of("success", true, "message", "Выставка создана (статус: DRAFT)",
					"exhibition", convertExhibitionToDTO(savedExhibition)));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Map.of("error", "Ошибка создания выставки: " + e.getMessage()));
		}
	}

	@PutMapping("/exhibitions/{id}/submit")
	@Operation(summary = "Отправить выставку на модерацию")
	public ResponseEntity<?> submitExhibition(@PathVariable("id") Long id) {
		try {
			User currentUser = userService.getCurrentUser();

			Optional<ExhibitionEvent> exhibitionOpt = exhibitionEventService.getExhibitionEventById(id);
			if (!exhibitionOpt.isPresent()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Выставка не найдена"));
			}

			ExhibitionEvent exhibition = exhibitionOpt.get();

			if (exhibition.getGallery() == null) {
				return ResponseEntity.badRequest().body(Map.of("error", "Выставка не привязана к галерее"));
			}

			Long galleryId = exhibition.getGallery().getId();

			if (!galleryService.canOwnerUpdateGallery(currentUser.getId(), galleryId)
					&& !userService.isCurrentUserAdmin()) {

				Optional<User> galleryOwnerOpt = galleryService.getGalleryOwner(galleryId);
				String errorMessage = "Нет прав на изменение этой выставки.";

				if (galleryOwnerOpt.isPresent()) {
					User galleryOwner = galleryOwnerOpt.get();
					errorMessage += " Галерея принадлежит пользователю: " + galleryOwner.getFullName() + " (ID: "
							+ galleryOwner.getId() + ")";
				} else {
					errorMessage += " Не удалось определить владельца галереи.";
				}

				return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", errorMessage));
			}

			exhibition.setStatus(ExhibitionStatus.ACTIVE);
			ExhibitionEvent updatedExhibition = exhibitionEventService.saveExhibitionEvent(exhibition);

			return ResponseEntity.ok(Map.of("success", true, "message", "Выставка отправлена на модерацию",
					"exhibition", convertExhibitionToDTO(updatedExhibition)));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Map.of("error", "Ошибка отправки выставки: " + e.getMessage()));
		}
	}

	@PostMapping("/exhibitions/{id}/hall-map")
	@Operation(summary = "Загрузить карту зала")
	public ResponseEntity<?> uploadHallMap(@PathVariable("id") Long id, @RequestBody Map<String, String> request) {

		try {
			User currentUser = userService.getCurrentUser();
			String mapImageUrl = request.get("mapImageUrl");

			if (mapImageUrl == null || mapImageUrl.trim().isEmpty()) {
				return ResponseEntity.badRequest().body(Map.of("error", "Параметр mapImageUrl обязателен"));
			}

			Optional<ExhibitionEvent> exhibitionOpt = exhibitionEventService.getExhibitionEventById(id);
			if (!exhibitionOpt.isPresent()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Выставка не найдена"));
			}

			ExhibitionEvent exhibition = exhibitionOpt.get();
			Long galleryId = exhibition.getGallery().getId();

			if (!galleryService.canOwnerUpdateGallery(currentUser.getId(), galleryId)
					&& !userService.isCurrentUserAdmin()) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN)
						.body(Map.of("error", "Нет прав на добавление карты зала к этой выставке"));
			}

			ExhibitionHallMap hallMap = new ExhibitionHallMap();
			hallMap.setExhibitionEvent(exhibition);
			hallMap.setMapImageUrl(mapImageUrl);
			hallMap.setName(request.containsKey("name") ? request.get("name") : "Карта зала");

			ExhibitionHallMap savedHallMap = exhibitionHallMapService.saveExhibitionHallMap(hallMap);

			return ResponseEntity.ok(Map.of("success", true, "message", "Карта зала загружена", "hallMap",
					convertHallMapToDTO(savedHallMap)));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Map.of("error", "Ошибка загрузки карты зала: " + e.getMessage()));
		}
	}

	@GetMapping("/exhibitions/{id}/stands")
	@Operation(summary = "Получить стенды для выставки")
	public ResponseEntity<?> getExhibitionStands(@PathVariable("id") Long id) {
		try {
			User currentUser = userService.getCurrentUser();

			Optional<ExhibitionEvent> exhibitionOpt = exhibitionEventService.getExhibitionEventById(id);
			if (!exhibitionOpt.isPresent()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Выставка не найдена"));
			}

			ExhibitionEvent exhibition = exhibitionOpt.get();
			Long galleryId = exhibition.getGallery().getId();

			if (!galleryService.canOwnerUpdateGallery(currentUser.getId(), galleryId)
					&& !userService.isCurrentUserAdmin()) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN)
						.body(Map.of("error", "Нет прав на просмотр стендов этой выставки"));
			}

			List<ExhibitionHallMap> hallMaps = exhibitionHallMapService.getExhibitionHallMapsByEventId(id);

			List<ExhibitionStand> stands = hallMaps.stream()
					.flatMap(hallMap -> exhibitionStandService.getExhibitionStandsByHallMapId(hallMap.getId()).stream())
					.collect(Collectors.toList());

			List<Map<String, Object>> standDTOs = stands.stream().map(this::convertStandToDTO)
					.collect(Collectors.toList());

			return ResponseEntity
					.ok(Map.of("success", true, "stands", standDTOs, "total", standDTOs.size(), "exhibitionId", id));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", "Ошибка получения стендов: " + e.getMessage()));
		}
	}

	@PostMapping("/exhibitions/{id}/stands")
	@Operation(summary = "Добавить новый стенд на выставку")
	public ResponseEntity<?> addExhibitionStand(@PathVariable("id") Long id,
			@RequestBody Map<String, Object> standData) {
		try {
			User currentUser = userService.getCurrentUser();

			Optional<ExhibitionEvent> exhibitionOpt = exhibitionEventService.getExhibitionEventById(id);
			if (!exhibitionOpt.isPresent()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Выставка не найдена"));
			}

			ExhibitionEvent exhibition = exhibitionOpt.get();
			Long galleryId = exhibition.getGallery().getId();

			if (!galleryService.canOwnerUpdateGallery(currentUser.getId(), galleryId)
					&& !userService.isCurrentUserAdmin()) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN)
						.body(Map.of("error", "Нет прав на добавление стендов к этой выставке"));
			}

			if (!standData.containsKey("standNumber") || !standData.containsKey("width")
					|| !standData.containsKey("height") || !standData.containsKey("type")) {
				return ResponseEntity.badRequest()
						.body(Map.of("error", "Поля standNumber, width, height и type обязательны"));
			}

			List<ExhibitionHallMap> hallMaps = exhibitionHallMapService.getExhibitionHallMapsByEventId(id);
			ExhibitionHallMap hallMap;

			if (hallMaps.isEmpty()) {

				hallMap = new ExhibitionHallMap();
				hallMap.setExhibitionEvent(exhibition);
				hallMap.setName("Основной зал");
				hallMap.setMapImageUrl("");
				hallMap = exhibitionHallMapService.saveExhibitionHallMap(hallMap);
			} else {
				hallMap = hallMaps.get(0);
			}

			ExhibitionStand stand = new ExhibitionStand();
			stand.setExhibitionHallMap(hallMap);
			stand.setStandNumber(standData.get("standNumber").toString());

			if (standData.containsKey("positionX")) {
				try {
					stand.setPositionX(Integer.parseInt(standData.get("positionX").toString()));
				} catch (NumberFormatException e) {
					return ResponseEntity.badRequest().body(Map.of("error", "Некорректный формат positionX"));
				}
			} else {
				stand.setPositionX(0);
			}

			if (standData.containsKey("positionY")) {
				try {
					stand.setPositionY(Integer.parseInt(standData.get("positionY").toString()));
				} catch (NumberFormatException e) {
					return ResponseEntity.badRequest().body(Map.of("error", "Некорректный формат positionY"));
				}
			} else {
				stand.setPositionY(0);
			}

			try {
				stand.setWidth(Integer.parseInt(standData.get("width").toString()));
				stand.setHeight(Integer.parseInt(standData.get("height").toString()));
			} catch (NumberFormatException e) {
				return ResponseEntity.badRequest().body(Map.of("error", "Некорректный формат width или height"));
			}

			String typeStr = standData.get("type").toString().toUpperCase();
			StandType standType;
			try {
				standType = StandType.valueOf(typeStr);
			} catch (IllegalArgumentException e) {
				return ResponseEntity.badRequest().body(Map.of("error",
						"Некорректный тип стенда. Допустимые значения: " + Arrays.toString(StandType.values())));
			}
			stand.setType(standType);

			stand.setStatus(StandStatus.AVAILABLE);

			ExhibitionStand savedStand = exhibitionStandService.saveExhibitionStand(stand);

			return ResponseEntity.ok(Map.of("success", true, "message", "Стенд успешно добавлен", "stand",
					convertStandToDTO(savedStand)));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Map.of("error", "Ошибка добавления стенда: " + e.getMessage()));
		}
	}

	@PutMapping("/stands/{standId}/status")
	@Operation(summary = "Изменить статус стенда (AVAILABLE/BOOKED)")
	public ResponseEntity<?> changeStandStatus(@PathVariable("standId") Long standId,
			@RequestBody Map<String, String> request) {
		try {
			User currentUser = userService.getCurrentUser();

			Optional<ExhibitionStand> standOpt = exhibitionStandService.getExhibitionStandById(standId);
			if (!standOpt.isPresent()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Стенд не найден"));
			}

			ExhibitionStand stand = standOpt.get();

			Long galleryId = stand.getExhibitionHallMap().getExhibitionEvent().getGallery().getId();

			if (!galleryService.canOwnerUpdateGallery(currentUser.getId(), galleryId)
					&& !userService.isCurrentUserAdmin()) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN)
						.body(Map.of("error", "Нет прав на изменение статуса этого стенда"));
			}

			if (!request.containsKey("status")) {
				return ResponseEntity.badRequest().body(Map.of("error", "Параметр status обязателен"));
			}

			String statusStr = request.get("status").toUpperCase();
			StandStatus newStatus;

			try {
				newStatus = StandStatus.valueOf(statusStr);
				if (newStatus != StandStatus.AVAILABLE && newStatus != StandStatus.BOOKED) {
					return ResponseEntity.badRequest()
							.body(Map.of("error", "Некорректный статус. Допустимые значения: AVAILABLE, BOOKED"));
				}
			} catch (IllegalArgumentException e) {
				return ResponseEntity.badRequest()
						.body(Map.of("error", "Некорректный статус. Допустимые значения: AVAILABLE, BOOKED"));
			}

			if (newStatus == StandStatus.BOOKED && stand.getStatus() == StandStatus.BOOKED) {
				return ResponseEntity.badRequest().body(Map.of("error", "Стенд уже забронирован"));
			}

			if (newStatus == StandStatus.AVAILABLE && stand.getStatus() == StandStatus.AVAILABLE) {
				return ResponseEntity.badRequest().body(Map.of("error", "Стенд уже доступен"));
			}

			if (newStatus == StandStatus.BOOKED) {
				List<Booking> standBookings = bookingService.getAllBookings().stream()
						.filter(booking -> booking.getExhibitionStand().getId().equals(standId)
								&& (booking.getStatus() == BookingStatus.CONFIRMED
										|| booking.getStatus() == BookingStatus.PENDING))
						.collect(Collectors.toList());

				if (!standBookings.isEmpty()) {
					return ResponseEntity.badRequest().body(Map.of("error",
							"Нельзя пометить стенд как BOOKED, так как на него есть активные бронирования"));
				}
			}

			stand.setStatus(newStatus);
			ExhibitionStand updatedStand = exhibitionStandService.saveExhibitionStand(stand);

			return ResponseEntity.ok(Map.of("success", true, "message",
					"Статус стенда изменен на " + newStatus.toString(), "stand", convertStandToDTO(updatedStand)));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Map.of("error", "Ошибка изменения статуса стенда: " + e.getMessage()));
		}
	}

	@GetMapping("/exhibitions/{id}/available-stands")
	@Operation(summary = "Получить только доступные стенды выставки")
	public ResponseEntity<?> getAvailableExhibitionStands(@PathVariable("id") Long id) {
		try {
			User currentUser = userService.getCurrentUser();

			Optional<ExhibitionEvent> exhibitionOpt = exhibitionEventService.getExhibitionEventById(id);
			if (!exhibitionOpt.isPresent()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Выставка не найдена"));
			}

			ExhibitionEvent exhibition = exhibitionOpt.get();
			Long galleryId = exhibition.getGallery().getId();

			if (!galleryService.canOwnerUpdateGallery(currentUser.getId(), galleryId)
					&& !userService.isCurrentUserAdmin()) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN)
						.body(Map.of("error", "Нет прав на просмотр стендов этой выставки"));
			}

			List<ExhibitionHallMap> hallMaps = exhibitionHallMapService.getExhibitionHallMapsByEventId(id);

			List<ExhibitionStand> availableStands = hallMaps.stream()
					.flatMap(hallMap -> exhibitionStandService.getExhibitionStandsByHallMapId(hallMap.getId()).stream())
					.filter(stand -> stand.getStatus() == StandStatus.AVAILABLE).collect(Collectors.toList());

			List<Booking> allBookings = bookingService.getAllBookings();

			List<ExhibitionStand> bookedStands = hallMaps.stream()
					.flatMap(hallMap -> exhibitionStandService.getExhibitionStandsByHallMapId(hallMap.getId()).stream())
					.filter(stand -> {
						boolean isBooked = allBookings.stream()
								.anyMatch(booking -> booking.getExhibitionStand().getId().equals(stand.getId())
										&& (booking.getStatus() == BookingStatus.CONFIRMED
												|| booking.getStatus() == BookingStatus.PENDING));
						return isBooked;
					}).collect(Collectors.toList());

			List<Map<String, Object>> availableStandDTOs = availableStands.stream().map(this::convertStandToDTO)
					.collect(Collectors.toList());

			List<Map<String, Object>> bookedStandDTOs = bookedStands.stream().map(this::convertStandToDTO)
					.collect(Collectors.toList());

			return ResponseEntity.ok(Map.of("success", true, "availableStands", availableStandDTOs, "bookedStands",
					bookedStandDTOs, "totalAvailable", availableStands.size(), "totalBooked", bookedStands.size(),
					"exhibitionId", id));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", "Ошибка получения доступных стендов: " + e.getMessage()));
		}
	}

	@PostMapping("/exhibitions/{id}/stands/batch")
	@Operation(summary = "Добавить несколько стендов на выставку")
	public ResponseEntity<?> addExhibitionStandsBatch(@PathVariable("id") Long id,
			@RequestBody List<Map<String, Object>> standsData) {
		try {
			User currentUser = userService.getCurrentUser();

			Optional<ExhibitionEvent> exhibitionOpt = exhibitionEventService.getExhibitionEventById(id);
			if (!exhibitionOpt.isPresent()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Выставка не найдена"));
			}

			ExhibitionEvent exhibition = exhibitionOpt.get();
			Long galleryId = exhibition.getGallery().getId();

			if (!galleryService.canOwnerUpdateGallery(currentUser.getId(), galleryId)
					&& !userService.isCurrentUserAdmin()) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN)
						.body(Map.of("error", "Нет прав на добавление стендов к этой выставке"));
			}

			List<ExhibitionHallMap> hallMaps = exhibitionHallMapService.getExhibitionHallMapsByEventId(id);
			ExhibitionHallMap hallMap;

			if (hallMaps.isEmpty()) {
				hallMap = new ExhibitionHallMap();
				hallMap.setExhibitionEvent(exhibition);
				hallMap.setName("Основной зал");
				hallMap.setMapImageUrl("");
				hallMap = exhibitionHallMapService.saveExhibitionHallMap(hallMap);
			} else {
				hallMap = hallMaps.get(0);
			}

			List<ExhibitionStand> createdStands = new ArrayList<>();
			List<String> errors = new ArrayList<>();

			for (int i = 0; i < standsData.size(); i++) {
				Map<String, Object> standData = standsData.get(i);

				try {

					if (!standData.containsKey("standNumber") || !standData.containsKey("width")
							|| !standData.containsKey("height") || !standData.containsKey("type")) {
						errors.add("Стенд " + (i + 1)
								+ ": отсутствуют обязательные поля standNumber, width, height или type");
						continue;
					}

					ExhibitionStand stand = new ExhibitionStand();
					stand.setExhibitionHallMap(hallMap);
					stand.setStandNumber(standData.get("standNumber").toString());

					stand.setPositionX(
							standData.containsKey("positionX") ? Integer.parseInt(standData.get("positionX").toString())
									: 0);
					stand.setPositionY(
							standData.containsKey("positionY") ? Integer.parseInt(standData.get("positionY").toString())
									: 0);

					stand.setWidth(Integer.parseInt(standData.get("width").toString()));
					stand.setHeight(Integer.parseInt(standData.get("height").toString()));

					String typeStr = standData.get("type").toString().toUpperCase();
					StandType standType;
					try {
						standType = StandType.valueOf(typeStr);
					} catch (IllegalArgumentException e) {
						errors.add("Стенд " + (i + 1) + ": некорректный тип стенда. Допустимые значения: "
								+ Arrays.toString(StandType.values()));
						continue;
					}
					stand.setType(standType);

					stand.setStatus(StandStatus.AVAILABLE);

					ExhibitionStand savedStand = exhibitionStandService.saveExhibitionStand(stand);
					createdStands.add(savedStand);

				} catch (Exception e) {
					errors.add("Стенд " + (i + 1) + ": " + e.getMessage());
				}
			}

			List<Map<String, Object>> standDTOs = createdStands.stream().map(this::convertStandToDTO)
					.collect(Collectors.toList());

			Map<String, Object> response = new HashMap<>();
			response.put("success", true);
			response.put("message", "Добавлено " + createdStands.size() + " из " + standsData.size() + " стендов");
			response.put("stands", standDTOs);
			response.put("totalCreated", createdStands.size());

			if (!errors.isEmpty()) {
				response.put("errors", errors);
				response.put("totalErrors", errors.size());
			}

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Map.of("error", "Ошибка добавления стендов: " + e.getMessage()));
		}
	}

	@GetMapping("/bookings")
	@Operation(summary = "Получить все бронирования для моих галерей")
	public ResponseEntity<?> getGalleryBookings(@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "exhibitionId", required = false) Long exhibitionId,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "20") int size) {
		try {
			User currentUser = userService.getCurrentUser();

			List<Gallery> ownerGalleries = galleryService.getGalleriesByOwnerId(currentUser.getId());
			List<Long> ownerGalleryIds = ownerGalleries.stream().map(Gallery::getId).collect(Collectors.toList());

			List<Booking> allBookings = bookingService.getAllBookings();

			List<Booking> galleryBookings = allBookings.stream().filter(booking -> {
				ExhibitionEvent exhibition = booking.getExhibitionStand().getExhibitionHallMap().getExhibitionEvent();
				return ownerGalleryIds.contains(exhibition.getGallery().getId());
			}).collect(Collectors.toList());

			if (exhibitionId != null) {
				galleryBookings = galleryBookings.stream().filter(booking -> booking.getExhibitionStand()
						.getExhibitionHallMap().getExhibitionEvent().getId().equals(exhibitionId))
						.collect(Collectors.toList());
			}

			if (status != null && !status.isEmpty()) {
				try {
					BookingStatus bookingStatus = BookingStatus.valueOf(status.toUpperCase());
					galleryBookings = galleryBookings.stream().filter(booking -> booking.getStatus() == bookingStatus)
							.collect(Collectors.toList());
				} catch (IllegalArgumentException e) {
					return ResponseEntity.badRequest().body(Map.of("error", "Некорректный статус бронирования"));
				}
			}

			int start = Math.min(page * size, galleryBookings.size());
			int end = Math.min(start + size, galleryBookings.size());
			List<Booking> paginatedBookings = galleryBookings.subList(start, end);

			List<Map<String, Object>> bookingDTOs = paginatedBookings.stream().map(this::convertBookingToDTO)
					.collect(Collectors.toList());

			return ResponseEntity.ok(Map.of("success", true, "bookings", bookingDTOs, "total", galleryBookings.size(),
					"page", page, "size", size));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", "Ошибка получения бронирований: " + e.getMessage()));
		}
	}

	@PutMapping("/bookings/{id}/confirm")
	@Operation(summary = "Подтвердить бронирование")
	public ResponseEntity<?> confirmBooking(@PathVariable("id") Long id,
			@RequestBody(required = false) Map<String, String> request) {
		try {
			User currentUser = userService.getCurrentUser();

			Optional<Booking> bookingOpt = bookingService.getBookingById(id);
			if (!bookingOpt.isPresent()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Бронирование не найдено"));
			}

			Booking booking = bookingOpt.get();

			ExhibitionEvent exhibition = booking.getExhibitionStand().getExhibitionHallMap().getExhibitionEvent();
			Long galleryId = exhibition.getGallery().getId();

			if (!galleryService.canOwnerUpdateGallery(currentUser.getId(), galleryId)
					&& !userService.isCurrentUserAdmin()) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN)
						.body(Map.of("error", "Нет прав на подтверждение этого бронирования"));
			}

			if (booking.getStatus() != BookingStatus.PENDING) {
				return ResponseEntity.badRequest()
						.body(Map.of("error", "Нельзя подтвердить бронирование в статусе: " + booking.getStatus()));
			}

			Long standId = booking.getExhibitionStand().getId();
			List<Booking> standBookings = bookingService.getAllBookings().stream()
					.filter(b -> b.getExhibitionStand().getId().equals(standId))
					.filter(b -> b.getStatus() == BookingStatus.CONFIRMED && !b.getId().equals(id))
					.collect(Collectors.toList());

			if (!standBookings.isEmpty()) {
				return ResponseEntity.badRequest().body(Map.of("error", "Стенд уже забронирован другим художником"));
			}

			if (exhibition.getEndDate().isBefore(LocalDateTime.now())) {
				return ResponseEntity.badRequest().body(Map.of("error", "Выставка уже завершена"));
			}

			booking.setStatus(BookingStatus.CONFIRMED);

			ExhibitionStand stand = booking.getExhibitionStand();
			stand.setStatus(StandStatus.BOOKED);
			exhibitionStandService.saveExhibitionStand(stand);

			Booking updatedBooking = bookingService.saveBooking(booking);

			String message = "Бронирование подтверждено";
			if (request != null && request.containsKey("message")) {
				message += ". " + request.get("message");
			}

			return ResponseEntity
					.ok(Map.of("success", true, "message", message, "booking", convertBookingToDTO(updatedBooking)));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Map.of("error", "Ошибка подтверждения бронирования: " + e.getMessage()));
		}
	}

	@PutMapping("/bookings/{id}/reject")
	@Operation(summary = "Отклонить бронирование")
	public ResponseEntity<?> rejectBooking(@PathVariable("id") Long id, @RequestBody Map<String, String> request) {
		try {
			User currentUser = userService.getCurrentUser();

			Optional<Booking> bookingOpt = bookingService.getBookingById(id);
			if (!bookingOpt.isPresent()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Бронирование не найдено"));
			}

			Booking booking = bookingOpt.get();

			ExhibitionEvent exhibition = booking.getExhibitionStand().getExhibitionHallMap().getExhibitionEvent();
			Long galleryId = exhibition.getGallery().getId();

			if (!galleryService.canOwnerUpdateGallery(currentUser.getId(), galleryId)
					&& !userService.isCurrentUserAdmin()) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN)
						.body(Map.of("error", "Нет прав на отклонение этого бронирования"));
			}

			if (booking.getStatus() != BookingStatus.PENDING) {
				return ResponseEntity.badRequest()
						.body(Map.of("error", "Нельзя отклонить бронирование в статусе: " + booking.getStatus()));
			}

			String reason = request.get("reason");
			if (reason == null || reason.trim().isEmpty()) {
				return ResponseEntity.badRequest().body(Map.of("error", "Причина отклонения обязательна"));
			}

			booking.setStatus(BookingStatus.CANCELLED);

			ExhibitionStand stand = booking.getExhibitionStand();
			stand.setStatus(StandStatus.AVAILABLE);
			exhibitionStandService.saveExhibitionStand(stand);

			Booking updatedBooking = bookingService.saveBooking(booking);

			return ResponseEntity.ok(Map.of("success", true, "message", "Бронирование отклонено", "reason", reason,
					"booking", convertBookingToDTO(updatedBooking)));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Map.of("error", "Ошибка отклонения бронирования: " + e.getMessage()));
		}
	}
//	@PostMapping("/exhibitions/{id}/upload-map")
//	@Operation(summary = "Загрузить изображение карты зала")
//	public ResponseEntity<?> uploadHallMapImage(@PathVariable("id") Long id,
//												@RequestParam("file") MultipartFile file) {
//		try {
//			User currentUser = userService.getCurrentUser();
//
//			Optional<ExhibitionEvent> exhibitionOpt = exhibitionEventService.getExhibitionEventById(id);
//			if (!exhibitionOpt.isPresent()) {
//				return ResponseEntity.status(HttpStatus.NOT_FOUND)
//						.body(Map.of("error", "Выставка не найдена"));
//			}
//
//			ExhibitionEvent exhibition = exhibitionOpt.get();
//			Long galleryId = exhibition.getGallery().getId();
//
//			if (!galleryService.canOwnerUpdateGallery(currentUser.getId(), galleryId)) {
//				return ResponseEntity.status(HttpStatus.FORBIDDEN)
//						.body(Map.of("error", "Нет прав на загрузку карты"));
//			}
//
//			// Сохраняем файл и получаем URL
//			String imageUrl = fileStorageService.saveFile(file);
//
//			ExhibitionHallMap hallMap = new ExhibitionHallMap();
//			hallMap.setExhibitionEvent(exhibition);
//			hallMap.setName("План зала");
//			hallMap.setMapImageUrl(imageUrl);
//
//			ExhibitionHallMap savedHallMap = exhibitionHallMapService.saveExhibitionHallMap(hallMap);
//
//			return ResponseEntity.ok(Map.of(
//					"success", true,
//					"message", "Карта зала загружена",
//					"hallMap", convertHallMapToDTO(savedHallMap)
//			));
//		} catch (Exception e) {
//			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//					.body(Map.of("error", "Ошибка загрузки: " + e.getMessage()));
//		}
//	}

	@PostMapping("/exhibitions/{id}/stand")
	@Operation(summary = "Создать стенд с координатами")
	public ResponseEntity<?> createStandWithCoordinates(@PathVariable("id") Long id,
			@RequestBody Map<String, Object> standData) {
		try {
			User currentUser = userService.getCurrentUser();

			Optional<ExhibitionEvent> exhibitionOpt = exhibitionEventService.getExhibitionEventById(id);
			if (!exhibitionOpt.isPresent()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Выставка не найдена"));
			}

			ExhibitionEvent exhibition = exhibitionOpt.get();
			Long galleryId = exhibition.getGallery().getId();

			if (!galleryService.canOwnerUpdateGallery(currentUser.getId(), galleryId)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Нет прав на создание стенда"));
			}

			// Получаем карту зала выставки
			List<ExhibitionHallMap> hallMaps = exhibitionHallMapService.getExhibitionHallMapsByEventId(id);
			if (hallMaps.isEmpty()) {
				return ResponseEntity.badRequest().body(Map.of("error", "Сначала загрузите карту зала"));
			}

			ExhibitionHallMap hallMap = hallMaps.get(0);

			ExhibitionStand stand = new ExhibitionStand();
			stand.setExhibitionHallMap(hallMap);
			stand.setStandNumber(standData.get("standNumber").toString());
			stand.setPositionX(Integer.parseInt(standData.get("positionX").toString()));
			stand.setPositionY(Integer.parseInt(standData.get("positionY").toString()));
			stand.setWidth(Integer.parseInt(standData.get("width").toString()));
			stand.setHeight(Integer.parseInt(standData.get("height").toString()));
			stand.setType(StandType.valueOf(standData.get("type").toString()));
			stand.setStatus(StandStatus.AVAILABLE);

			ExhibitionStand savedStand = exhibitionStandService.saveExhibitionStand(stand);

			return ResponseEntity
					.ok(Map.of("success", true, "message", "Стенд создан", "stand", convertStandToDTO(savedStand)));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Map.of("error", "Ошибка создания стенда: " + e.getMessage()));
		}
	}

	@PutMapping("/stands/{id}")
	@Operation(summary = "Обновить стенд")
	public ResponseEntity<?> updateStand(@PathVariable("id") Long id, @RequestBody Map<String, Object> updates) {
		try {
			User currentUser = userService.getCurrentUser();

			Optional<ExhibitionStand> standOpt = exhibitionStandService.getExhibitionStandById(id);
			if (!standOpt.isPresent()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Стенд не найден"));
			}

			ExhibitionStand stand = standOpt.get();
			Long galleryId = stand.getExhibitionHallMap().getExhibitionEvent().getGallery().getId();

			if (!galleryService.canOwnerUpdateGallery(currentUser.getId(), galleryId)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN)
						.body(Map.of("error", "Нет прав на изменение стенда"));
			}

			if (updates.containsKey("positionX")) {
				stand.setPositionX(Integer.parseInt(updates.get("positionX").toString()));
			}
			if (updates.containsKey("positionY")) {
				stand.setPositionY(Integer.parseInt(updates.get("positionY").toString()));
			}
			if (updates.containsKey("width")) {
				stand.setWidth(Integer.parseInt(updates.get("width").toString()));
			}
			if (updates.containsKey("height")) {
				stand.setHeight(Integer.parseInt(updates.get("height").toString()));
			}
			if (updates.containsKey("standNumber")) {
				stand.setStandNumber(updates.get("standNumber").toString());
			}
			if (updates.containsKey("type")) {
				stand.setType(StandType.valueOf(updates.get("type").toString()));
			}
			if (updates.containsKey("status")) {
				stand.setStatus(StandStatus.valueOf(updates.get("status").toString()));
			}

			ExhibitionStand updatedStand = exhibitionStandService.saveExhibitionStand(stand);

			return ResponseEntity
					.ok(Map.of("success", true, "message", "Стенд обновлен", "stand", convertStandToDTO(updatedStand)));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Map.of("error", "Ошибка обновления стенда: " + e.getMessage()));
		}
	}

	@DeleteMapping("/stands/{id}")
	@Operation(summary = "Удалить стенд")
	public ResponseEntity<?> deleteStand(@PathVariable("id") Long id) {
		try {
			User currentUser = userService.getCurrentUser();

			Optional<ExhibitionStand> standOpt = exhibitionStandService.getExhibitionStandById(id);
			if (!standOpt.isPresent()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Стенд не найден"));
			}

			ExhibitionStand stand = standOpt.get();
			Long galleryId = stand.getExhibitionHallMap().getExhibitionEvent().getGallery().getId();

			if (!galleryService.canOwnerUpdateGallery(currentUser.getId(), galleryId)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Нет прав на удаление стенда"));
			}

			exhibitionStandService.deleteExhibitionStand(id);

			return ResponseEntity.ok(Map.of("success", true, "message", "Стенд удален"));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Map.of("error", "Ошибка удаления стенда: " + e.getMessage()));
		}
	}

	private Map<String, Object> convertBookingToDTO(Booking booking) {
		Map<String, Object> dto = new HashMap<>();
		dto.put("id", booking.getId());
		dto.put("bookingDate", booking.getBookingDate());
		dto.put("status", booking.getStatus().toString());

		if (booking.getArtist() != null) {
			dto.put("artistId", booking.getArtist().getId());
			dto.put("artistName", booking.getArtist().getFullName());
			dto.put("artistEmail", booking.getArtist().getEmail());
		}

		if (booking.getExhibitionStand() != null) {
			ExhibitionStand stand = booking.getExhibitionStand();
			dto.put("exhibitionStandId", stand.getId());
			dto.put("standNumber", stand.getStandNumber());
			dto.put("positionX", stand.getPositionX());
			dto.put("positionY", stand.getPositionY());
			dto.put("width", stand.getWidth());
			dto.put("height", stand.getHeight());
			dto.put("standType", stand.getType().toString());
			dto.put("standStatus", stand.getStatus().toString());

			if (stand.getExhibitionHallMap() != null) {
				ExhibitionHallMap hallMap = stand.getExhibitionHallMap();
				dto.put("hallMapId", hallMap.getId());
				dto.put("hallMapName", hallMap.getName());

				if (hallMap.getExhibitionEvent() != null) {
					ExhibitionEvent exhibition = hallMap.getExhibitionEvent();
					dto.put("exhibitionId", exhibition.getId());
					dto.put("exhibitionTitle", exhibition.getTitle());
					dto.put("exhibitionStatus", exhibition.getStatus().toString());
					dto.put("exhibitionStartDate", exhibition.getStartDate());
					dto.put("exhibitionEndDate", exhibition.getEndDate());

					if (exhibition.getGallery() != null) {
						dto.put("galleryId", exhibition.getGallery().getId());
						dto.put("galleryName", exhibition.getGallery().getName());
					}
				}
			}
		}

		return dto;
	}

	@GetMapping("/statistics")
	@Operation(summary = "Получить статистику по моим галереям")
	public ResponseEntity<?> getStatistics(@RequestParam(value="galleryId", required = false) Long galleryId) {

		try {
			User currentUser = userService.getCurrentUser();

			Map<String, Object> stats = galleryService.getGalleryStatistics(currentUser.getId(), galleryId);

			return ResponseEntity.ok(Map.of("success", true, "statistics", stats, "ownerId", currentUser.getId()));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", "Ошибка получения статистики: " + e.getMessage()));
		}
	}

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

	private List<Gallery> getGalleriesByOwnerId(Long ownerId) {
		return galleryService.getAllGalleries().stream().filter(gallery -> {
			if (gallery.getOwner() == null) {
				return false;
			}
			return gallery.getOwner().getId().equals(ownerId);
		}).collect(Collectors.toList());
	}
}