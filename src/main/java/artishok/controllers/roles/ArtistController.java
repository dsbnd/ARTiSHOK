package artishok.controllers.roles;

import artishok.entities.*;
import artishok.entities.enums.ArtworkStatus;
import artishok.entities.enums.BookingStatus;
import artishok.entities.enums.ExhibitionStatus;
import artishok.entities.enums.StandStatus;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/artist")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ARTIST', 'ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Художник", description = "API для художников")
public class ArtistController {
	@Autowired
	private UserService userService;
	@Autowired
	private ArtworkService artworkService;
	@Autowired
	private BookingService bookingService;
	@Autowired
	private ExhibitionEventService exhibitionEventService;
	@Autowired
	private ExhibitionStandService exhibitionStandService;
	@Autowired
	private ExhibitionHallMapService exhibitionHallMapService;

	// ==================== ПРОИЗВЕДЕНИЯ ИСКУССТВА ====================

	@GetMapping("/artworks")
	@Operation(summary = "Получить мои произведения")
	public ResponseEntity<?> getMyArtworks(@RequestParam(required = false) String status,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

		try {
			User currentUser = userService.getCurrentUser();

			// Получаем все бронирования художника
			List<Booking> artistBookings = bookingService.getAllBookings().stream()
					.filter(booking -> booking.getArtist().getId().equals(currentUser.getId()))
					.collect(Collectors.toList());

			// Получаем ID бронирований
			List<Long> bookingIds = artistBookings.stream().map(Booking::getId).collect(Collectors.toList());

			// Получаем все произведения по бронированиям
			List<Artwork> allArtworks = artworkService.getAllArtworks().stream()
					.filter(artwork -> bookingIds.contains(artwork.getBooking().getId())).collect(Collectors.toList());

			// Фильтрация по статусу, если указан
			if (status != null && !status.isEmpty()) {
				try {
					ArtworkStatus artworkStatus = ArtworkStatus.valueOf(status.toUpperCase());
					allArtworks = allArtworks.stream().filter(artwork -> artwork.getStatus() == artworkStatus)
							.collect(Collectors.toList());
				} catch (IllegalArgumentException e) {
					return ResponseEntity.badRequest().body(Map.of("error", "Некорректный статус произведения"));
				}
			}

			// Применяем пагинацию
			int start = Math.min(page * size, allArtworks.size());
			int end = Math.min(start + size, allArtworks.size());
			List<Artwork> paginatedArtworks = allArtworks.subList(start, end);

			// Преобразование в DTO
			List<Map<String, Object>> artworkDTOs = paginatedArtworks.stream().map(this::convertArtworkToDTO)
					.collect(Collectors.toList());

			return ResponseEntity.ok(Map.of("success", true, "artworks", artworkDTOs, "total", allArtworks.size(),
					"artistId", currentUser.getId(), "page", page, "size", size));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", "Ошибка получения произведений: " + e.getMessage()));
		}
	}

	@PostMapping("/artworks")
	@Operation(summary = "Добавить новое произведение")
	public ResponseEntity<?> createArtwork(@RequestBody Map<String, Object> artworkData) {
		try {
			User currentUser = userService.getCurrentUser();

			// Проверка обязательных полей
			if (!artworkData.containsKey("title") || !artworkData.containsKey("bookingId")) {
				return ResponseEntity.badRequest().body(Map.of("error", "Поля title и bookingId обязательны"));
			}

			Long bookingId;
			try {
				bookingId = Long.parseLong(artworkData.get("bookingId").toString());
			} catch (NumberFormatException e) {
				return ResponseEntity.badRequest().body(Map.of("error", "Некорректный формат bookingId"));
			}

			// Проверяем бронирование
			Optional<Booking> bookingOpt = bookingService.getBookingById(bookingId);
			if (!bookingOpt.isPresent()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Бронирование не найдено"));
			}

			Booking booking = bookingOpt.get();

			// Проверяем, что бронирование принадлежит художнику
			if (!booking.getArtist().getId().equals(currentUser.getId())) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN)
						.body(Map.of("error", "Бронирование не принадлежит вам"));
			}

			// Проверяем, что бронирование подтверждено
			if (booking.getStatus() != BookingStatus.CONFIRMED) {
				return ResponseEntity.badRequest()
						.body(Map.of("error", "Можно добавить произведение только к подтвержденному бронированию"));
			}

			// Создаем произведение
			Artwork artwork = new Artwork();
			artwork.setBooking(booking);
			artwork.setTitle(artworkData.get("title").toString());
			artwork.setDescription(
					artworkData.containsKey("description") ? artworkData.get("description").toString() : "");
			artwork.setStatus(ArtworkStatus.DRAFT);

			// Дополнительные поля
			if (artworkData.containsKey("creationYear")) {
				try {
					artwork.setCreationYear(Integer.parseInt(artworkData.get("creationYear").toString()));
				} catch (NumberFormatException e) {
					return ResponseEntity.badRequest().body(Map.of("error", "Некорректный год создания"));
				}
			}

			if (artworkData.containsKey("technique")) {
				artwork.setTechnique(artworkData.get("technique").toString());
			}

			if (artworkData.containsKey("imageUrl")) {
				artwork.setImageUrl(artworkData.get("imageUrl").toString());
			}

			Artwork savedArtwork = artworkService.saveArtwork(artwork);

			return ResponseEntity.ok(Map.of("success", true, "message", "Произведение добавлено", "artwork",
					convertArtworkToDTO(savedArtwork)));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Map.of("error", "Ошибка создания произведения: " + e.getMessage()));
		}
	}

	@PutMapping("/artworks/{id}")
	@Operation(summary = "Обновить произведение")
	public ResponseEntity<?> updateArtwork(@PathVariable Long id, @RequestBody Map<String, Object> updates) {

		try {
			User currentUser = userService.getCurrentUser();

			// Получаем произведение
			Optional<Artwork> artworkOpt = artworkService.getArtworkById(id);
			if (!artworkOpt.isPresent()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Произведение не найдено"));
			}

			Artwork artwork = artworkOpt.get();

			// Проверяем, что произведение принадлежит художнику
			if (!artwork.getBooking().getArtist().getId().equals(currentUser.getId())) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN)
						.body(Map.of("error", "Нет прав на обновление этого произведения"));
			}

			// Обновляем поля
			if (updates.containsKey("title")) {
				artwork.setTitle(updates.get("title").toString());
			}
			if (updates.containsKey("description")) {
				artwork.setDescription(updates.get("description").toString());
			}
			if (updates.containsKey("technique")) {
				artwork.setTechnique(updates.get("technique").toString());
			}
			if (updates.containsKey("imageUrl")) {
				artwork.setImageUrl(updates.get("imageUrl").toString());
			}
			if (updates.containsKey("creationYear")) {
				try {
					artwork.setCreationYear(Integer.parseInt(updates.get("creationYear").toString()));
				} catch (NumberFormatException e) {
					return ResponseEntity.badRequest().body(Map.of("error", "Некорректный год создания"));
				}
			}
			if (updates.containsKey("status")) {
				try {
					ArtworkStatus status = ArtworkStatus.valueOf(updates.get("status").toString().toUpperCase());
					artwork.setStatus(status);
				} catch (IllegalArgumentException e) {
					return ResponseEntity.badRequest().body(Map.of("error", "Некорректный статус"));
				}
			}

			Artwork updatedArtwork = artworkService.saveArtwork(artwork);

			return ResponseEntity.ok(Map.of("success", true, "message", "Произведение обновлено", "artwork",
					convertArtworkToDTO(updatedArtwork)));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Map.of("error", "Ошибка обновления произведения: " + e.getMessage()));
		}
	}

	@DeleteMapping("/artworks/{id}")
	@Operation(summary = "Удалить произведение")
	public ResponseEntity<?> deleteArtwork(@PathVariable Long id) {
		try {
			User currentUser = userService.getCurrentUser();

			// Получаем произведение
			Optional<Artwork> artworkOpt = artworkService.getArtworkById(id);
			if (!artworkOpt.isPresent()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Произведение не найдено"));
			}

			Artwork artwork = artworkOpt.get();

			// Проверяем, что произведение принадлежит художнику
			if (!artwork.getBooking().getArtist().getId().equals(currentUser.getId())) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN)
						.body(Map.of("error", "Нет прав на удаление этого произведения"));
			}

			// Проверяем статус произведения
			if (artwork.getStatus() == ArtworkStatus.PUBLISHED) {
				return ResponseEntity.badRequest().body(Map.of("error", "Нельзя удалить опубликованное произведение"));
			}

			artworkService.deleteArtwork(id);

			return ResponseEntity.ok(Map.of("success", true, "message", "Произведение удалено", "artworkId", id));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Map.of("error", "Ошибка удаления произведения: " + e.getMessage()));
		}
	}

	// ==================== БРОНИРОВАНИЯ ====================

	@GetMapping("/bookings")
	@Operation(summary = "Получить мои бронирования")
	public ResponseEntity<?> getMyBookings(@RequestParam(required = false) String status,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

		try {
			User currentUser = userService.getCurrentUser();

			// Получаем все бронирования
			List<Booking> allBookings = bookingService.getAllBookings();

			// Фильтруем по художнику
			List<Booking> artistBookings = allBookings.stream()
					.filter(booking -> booking.getArtist().getId().equals(currentUser.getId()))
					.collect(Collectors.toList());

			// Фильтрация по статусу, если указан
			if (status != null && !status.isEmpty()) {
				try {
					BookingStatus bookingStatus = BookingStatus.valueOf(status.toUpperCase());
					artistBookings = artistBookings.stream().filter(booking -> booking.getStatus() == bookingStatus)
							.collect(Collectors.toList());
				} catch (IllegalArgumentException e) {
					return ResponseEntity.badRequest().body(Map.of("error", "Некорректный статус бронирования"));
				}
			}

			// Применяем пагинацию
			int start = Math.min(page * size, artistBookings.size());
			int end = Math.min(start + size, artistBookings.size());
			List<Booking> paginatedBookings = artistBookings.subList(start, end);

			// Преобразование в DTO
			List<Map<String, Object>> bookingDTOs = paginatedBookings.stream().map(this::convertBookingToDTO)
					.collect(Collectors.toList());

			return ResponseEntity.ok(Map.of("success", true, "bookings", bookingDTOs, "total", artistBookings.size(),
					"page", page, "size", size));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", "Ошибка получения бронирований: " + e.getMessage()));
		}
	}

	@GetMapping("/bookings/active")
	@Operation(summary = "Получить активные бронирования")
	public ResponseEntity<?> getActiveBookings() {
		try {
			User currentUser = userService.getCurrentUser();

			// Получаем подтвержденные бронирования художника
			List<Booking> allBookings = bookingService.getAllBookings();

			// Фильтруем: художник + статус CONFIRMED + выставка ACTIVE + дата окончания в
			// будущем
			List<Booking> activeBookings = allBookings.stream()
					.filter(booking -> booking.getArtist().getId().equals(currentUser.getId()))
					.filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED).filter(booking -> {
						ExhibitionEvent exhibition = booking.getExhibitionStand().getExhibitionHallMap()
								.getExhibitionEvent();
						return exhibition.getStatus() == ExhibitionStatus.ACTIVE
								&& exhibition.getEndDate().isAfter(LocalDateTime.now());
					}).collect(Collectors.toList());

			// Преобразование в DTO
			List<Map<String, Object>> bookingDTOs = activeBookings.stream().map(this::convertBookingToDTO)
					.collect(Collectors.toList());

			return ResponseEntity.ok(Map.of("success", true, "bookings", bookingDTOs, "count", activeBookings.size()));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", "Ошибка получения активных бронирований: " + e.getMessage()));
		}
	}

	@PostMapping("/bookings")
	@Operation(summary = "Создать бронирование")
	public ResponseEntity<?> createBooking(@RequestBody Map<String, Object> bookingData) {
		try {
			User currentUser = userService.getCurrentUser();

			// Проверка обязательных полей
			if (!bookingData.containsKey("exhibitionStandId")) {
				return ResponseEntity.badRequest().body(Map.of("error", "Поле exhibitionStandId обязательно"));
			}

			Long exhibitionStandId;
			try {
				exhibitionStandId = Long.parseLong(bookingData.get("exhibitionStandId").toString());
			} catch (NumberFormatException e) {
				return ResponseEntity.badRequest().body(Map.of("error", "Некорректный формат exhibitionStandId"));
			}

			// Проверяем стенд
			Optional<ExhibitionStand> standOpt = exhibitionStandService.getExhibitionStandById(exhibitionStandId);
			if (!standOpt.isPresent()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Стенд не найден"));
			}

			ExhibitionStand stand = standOpt.get();

			// Проверяем выставку
			ExhibitionEvent exhibition = stand.getExhibitionHallMap().getExhibitionEvent();

			// Проверяем, что выставка активна
			if (exhibition.getStatus() != ExhibitionStatus.ACTIVE) {
				return ResponseEntity.badRequest().body(Map.of("error", "Выставка не активна для бронирования"));
			}

			// Проверяем даты выставки
			if (exhibition.getEndDate().isBefore(LocalDateTime.now())) {
				return ResponseEntity.badRequest().body(Map.of("error", "Выставка уже завершена"));
			}



			// Проверяем, не занят ли уже стенд
			List<Booking> standBookings = bookingService.getAllBookings().stream()
					.filter(b -> b.getExhibitionStand().getId().equals(exhibitionStandId))
					.filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.PENDING)
					.collect(Collectors.toList());

			if (!standBookings.isEmpty()) {
				return ResponseEntity.badRequest().body(Map.of("error", "Стенд уже забронирован"));
			}

			// Создаем бронирование
			Booking booking = new Booking();
			booking.setExhibitionStand(stand);
			booking.setArtist(currentUser);
			booking.setStatus(BookingStatus.PENDING);

			Booking savedBooking = bookingService.saveBooking(booking);

			return ResponseEntity.ok(Map.of("success", true, "message", "Запрос на бронирование создан", "booking",
					convertBookingToDTO(savedBooking)));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Map.of("error", "Ошибка создания бронирования: " + e.getMessage()));
		}
	}

	@PutMapping("/bookings/{id}/cancel")
	@Operation(summary = "Отменить бронирование")
	public ResponseEntity<?> cancelBooking(@PathVariable Long id,
			@RequestBody(required = false) Map<String, String> request) {

		try {
			User currentUser = userService.getCurrentUser();

			// Получаем бронирование
			Optional<Booking> bookingOpt = bookingService.getBookingById(id);
			if (!bookingOpt.isPresent()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Бронирование не найдено"));
			}

			Booking booking = bookingOpt.get();

			// Проверяем, что бронирование принадлежит художнику
			if (!booking.getArtist().getId().equals(currentUser.getId())) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN)
						.body(Map.of("error", "Нет прав на отмену этого бронирования"));
			}

			// Проверяем статус бронирования
			if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
				return ResponseEntity.badRequest()
						.body(Map.of("error", "Нельзя отменить бронирование в текущем статусе"));
			}

			// Проверяем даты выставки
			ExhibitionEvent exhibition = booking.getExhibitionStand().getExhibitionHallMap().getExhibitionEvent();

			if (exhibition.getStartDate().isBefore(LocalDateTime.now())) {
				return ResponseEntity.badRequest()
						.body(Map.of("error", "Нельзя отменить бронирование после начала выставки"));
			}

			// Отменяем бронирование
			booking.setStatus(BookingStatus.CANCELLED);
			String reason = request != null ? request.get("reason") : "Отменено художником";
			// TODO: добавить поле reason в сущность Booking

			Booking savedBooking = bookingService.saveBooking(booking);

			return ResponseEntity.ok(Map.of("success", true, "message", "Бронирование отменено", "booking",
					convertBookingToDTO(savedBooking)));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Map.of("error", "Ошибка отмены бронирования: " + e.getMessage()));
		}
	}

	// ==================== ДОСТУПНЫЕ ВЫСТАВКИ И СТЕНДЫ ====================

	@GetMapping("/available-exhibitions")
	@Operation(summary = "Получить доступные выставки")
	public ResponseEntity<?> getAvailableExhibitions() {
		try {
			// Получаем все активные выставки
			List<ExhibitionEvent> allExhibitions = exhibitionEventService.getAllExhibitionEvents();

			// Фильтруем активные выставки
			List<ExhibitionEvent> activeExhibitions = allExhibitions.stream()
					.filter(exhibition -> exhibition.getStatus() == ExhibitionStatus.ACTIVE)
					.filter(exhibition -> exhibition.getEndDate().isAfter(LocalDateTime.now()))
					.collect(Collectors.toList());

			// Преобразование в DTO
			List<Map<String, Object>> exhibitionDTOs = activeExhibitions.stream().map(this::convertExhibitionToDTO)
					.collect(Collectors.toList());

			return ResponseEntity
					.ok(Map.of("success", true, "exhibitions", exhibitionDTOs, "total", activeExhibitions.size()));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", "Ошибка получения выставок: " + e.getMessage()));
		}
	}

	@GetMapping("/exhibitions/{id}/available-stands")
	@Operation(summary = "Получить доступные стенды для выставки")
	public ResponseEntity<?> getAvailableStands(@PathVariable Long id) {
		try {
			// Получаем выставку
			Optional<ExhibitionEvent> exhibitionOpt = exhibitionEventService.getExhibitionEventById(id);
			if (!exhibitionOpt.isPresent()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Выставка не найдена"));
			}

			ExhibitionEvent exhibition = exhibitionOpt.get();

			// Проверяем, что выставка активна
			if (exhibition.getStatus() != ExhibitionStatus.ACTIVE) {
				return ResponseEntity.badRequest().body(Map.of("error", "Выставка не активна"));
			}

			// Получаем все карты залов для выставки
			List<ExhibitionHallMap> hallMaps = exhibitionHallMapService.getExhibitionHallMapsByEventId(id);

			// Получаем все стенды для этих карт
			List<ExhibitionStand> allStands = hallMaps.stream()
					.flatMap(hallMap -> exhibitionStandService.getExhibitionStandsByHallMapId(hallMap.getId()).stream())
					.collect(Collectors.toList());

			// Получаем все бронирования для этих стендов
			List<Booking> allBookings = bookingService.getAllBookings();

			// Фильтруем доступные стенды (не забронированные)
			List<ExhibitionStand> availableStands = allStands.stream().filter(stand -> {
				boolean isBooked = allBookings.stream()
						.anyMatch(booking -> booking.getExhibitionStand().getId().equals(stand.getId())
								&& (booking.getStatus() == BookingStatus.CONFIRMED
										|| booking.getStatus() == BookingStatus.PENDING));
				return !isBooked;
			}).collect(Collectors.toList());

			// Преобразование в DTO
			List<Map<String, Object>> standDTOs = availableStands.stream().map(this::convertStandToDTO)
					.collect(Collectors.toList());

			return ResponseEntity.ok(Map.of("success", true, "stands", standDTOs, "total", availableStands.size(),
					"exhibitionId", id, "exhibitionTitle", exhibition.getTitle()));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", "Ошибка получения доступных стендов: " + e.getMessage()));
		}
	}

	// ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

	private Map<String, Object> convertArtworkToDTO(Artwork artwork) {
		Map<String, Object> dto = new HashMap<>();
		dto.put("id", artwork.getId());
		dto.put("title", artwork.getTitle());
		dto.put("description", artwork.getDescription());
		dto.put("creationYear", artwork.getCreationYear());
		dto.put("technique", artwork.getTechnique());
		dto.put("imageUrl", artwork.getImageUrl());
		dto.put("status", artwork.getStatus().toString());

		if (artwork.getBooking() != null) {
			dto.put("bookingId", artwork.getBooking().getId());

			if (artwork.getBooking().getArtist() != null) {
				dto.put("artistId", artwork.getBooking().getArtist().getId());
				dto.put("artistName", artwork.getBooking().getArtist().getFullName());
			}
		}

		return dto;
	}

	private Map<String, Object> convertBookingToDTO(Booking booking) {
		Map<String, Object> dto = new HashMap<>();
		dto.put("id", booking.getId());
		dto.put("bookingDate", booking.getBookingDate());
		dto.put("status", booking.getStatus().toString());

		if (booking.getArtist() != null) {
			dto.put("artistId", booking.getArtist().getId());
			dto.put("artistName", booking.getArtist().getFullName());
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

	private Map<String, Object> convertExhibitionToDTO(ExhibitionEvent exhibition) {
		Map<String, Object> dto = new HashMap<>();
		dto.put("id", exhibition.getId());
		dto.put("title", exhibition.getTitle());
		dto.put("description", exhibition.getDescription());
		dto.put("startDate", exhibition.getStartDate());
		dto.put("endDate", exhibition.getEndDate());
		dto.put("status", exhibition.getStatus().toString());

		if (exhibition.getGallery() != null) {
			dto.put("galleryId", exhibition.getGallery().getId());
			dto.put("galleryName", exhibition.getGallery().getName());
			dto.put("galleryAddress", exhibition.getGallery().getAddress());
		}

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

		if (stand.getExhibitionHallMap() != null) {
			dto.put("hallMapId", stand.getExhibitionHallMap().getId());
			dto.put("hallMapName", stand.getExhibitionHallMap().getName());

			if (stand.getExhibitionHallMap().getExhibitionEvent() != null) {
				dto.put("exhibitionId", stand.getExhibitionHallMap().getExhibitionEvent().getId());
				dto.put("exhibitionTitle", stand.getExhibitionHallMap().getExhibitionEvent().getTitle());
			}
		}

		return dto;
	}
}