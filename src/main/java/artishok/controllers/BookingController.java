package artishok.controllers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import artishok.entities.Booking;
import artishok.entities.enums.BookingStatus;
import artishok.services.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/bookings")
public class BookingController {
	private final BookingService bookingService;

	BookingController(BookingService bookingService) {
		this.bookingService = bookingService;
	}

	@Operation(summary = "Создание нового бронирования", description = "Создание бронирования места художником")
	@ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Бронирование успешно создано"),
			@ApiResponse(responseCode = "400", description = "Неверные входные данные"),
			@ApiResponse(responseCode = "404", description = "Художник или стенд не найден") })
	@PostMapping
	public ResponseEntity<?> createBooking(@RequestParam("artistId") Long artistId,
			@RequestParam("standId") Long standId) {

		try {
			Booking booking = bookingService.createBooking(artistId, standId);
			return ResponseEntity.status(HttpStatus.CREATED).body(booking);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
		}
	}

	@Operation(summary = "Получение бронирования по ID", description = "Получение информации о бронировании по его идентификатору")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Бронирование найдено"),
			@ApiResponse(responseCode = "404", description = "Бронирование не найдено") })
	@GetMapping("/{id}")
	public ResponseEntity<?> getBookingById(@PathVariable("id") Long id) {
		Optional<Booking> bookingOptional = bookingService.getBookingById(id);
		if (bookingOptional.isPresent()) {
			Booking booking = bookingOptional.get();
			return ResponseEntity.ok(booking);
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Бронирование не найдено"));
		}
	}

	@Operation(summary = "Получение всех бронирований", description = "Получение списка всех бронирований в системе")
	@ApiResponse(responseCode = "200", description = "Список бронирований получен")
	@GetMapping
	public ResponseEntity<List<Booking>> getAllBookings() {
		List<Booking> bookings = bookingService.getAllBookings();
		return ResponseEntity.ok(bookings);
	}

	@Operation(summary = "Обновление статуса бронирования", description = "Изменение статуса бронирования (PENDING, CONFIRMED, CANCELLED)")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Статус успешно обновлен"),
			@ApiResponse(responseCode = "404", description = "Бронирование не найдено") })
	@PutMapping("/{id}/status")
	public ResponseEntity<?> updateBookingStatus(@PathVariable("id") Long id, @RequestParam("status") String status) {
		try {
			BookingStatus bookingStatus = BookingStatus.valueOf(status.toUpperCase());
			Booking updatedBooking = bookingService.updateBookingStatus(id, bookingStatus);
			return ResponseEntity.ok(updatedBooking);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Неверный статус: " + status));
		} catch (RuntimeException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
		}
	}

	@Operation(summary = "Подтверждение бронирования", description = "Подтверждение бронирования (установка статуса CONFIRMED)")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Бронирование подтверждено"),
			@ApiResponse(responseCode = "404", description = "Бронирование не найдено") })
	@PostMapping("/{id}/confirm")
	public ResponseEntity<?> confirmBooking(@PathVariable("id") Long id) {
		try {
			Booking confirmedBooking = bookingService.confirmBooking(id);
			return ResponseEntity.ok(confirmedBooking);
		} catch (RuntimeException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
		}
	}

	@Operation(summary = "Отмена бронирования", description = "Отмена бронирования (установка статуса CANCELLED)")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Бронирование отменено"),
			@ApiResponse(responseCode = "404", description = "Бронирование не найдено") })
	@PostMapping("/{id}/cancel")
	public ResponseEntity<?> cancelBooking(@PathVariable("id") Long id) {

		try {
			Booking cancelledBooking = bookingService.cancelBooking(id);
			return ResponseEntity.ok(cancelledBooking);
		} catch (RuntimeException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
		}
	}

	@Operation(summary = "Удаление бронирования", description = "Полное удаление бронирования из системы")
	@ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Бронирование удалено"),
			@ApiResponse(responseCode = "404", description = "Бронирование не найдено") })
	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteBooking(@PathVariable("id") Long id) {

		if (!bookingService.bookingExists(id)) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Бронирование не найдено"));
		}

		bookingService.deleteBooking(id);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Получение бронирований по художнику", description = "Получение всех бронирований конкретного художника")
	@ApiResponse(responseCode = "200", description = "Список бронирований получен")
	@GetMapping("/artist/{artistId}")
	public ResponseEntity<?> getBookingsByArtist(@PathVariable("artistId") Long artistId) {
		try {
			List<Booking> bookings = bookingService.getBookingsByArtist(artistId);
			return ResponseEntity.ok(bookings);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
		}
	}

	@Operation(summary = "Получение бронирований по статусу", description = "Фильтрация бронирований по статусу")
	@ApiResponse(responseCode = "200", description = "Список бронирований получен")
	@GetMapping("/status/{status}")
	public ResponseEntity<?> getBookingsByStatus(@PathVariable("status") String status) {
		try {
			BookingStatus bookingStatus = BookingStatus.valueOf(status.toUpperCase());
			List<Booking> bookings = bookingService.getBookingsByStatus(bookingStatus);
			return ResponseEntity.ok(bookings);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Неверный статус: " + status));
		}
	}

	@Operation(summary = "Получение бронирований по стенду", description = "Получение всех бронирований для конкретного стенда")
	@ApiResponse(responseCode = "200", description = "Список бронирований получен")
	@GetMapping("/stand/{standId}")
	public ResponseEntity<List<Booking>> getBookingsByStand(@PathVariable("standId") Long standId) {

		List<Booking> bookings = bookingService.getBookingsByStand(standId);
		return ResponseEntity.ok(bookings);
	}

	@Operation(summary = "Получение бронирований по мероприятию", description = "Получение всех бронирований для конкретного мероприятия")
	@ApiResponse(responseCode = "200", description = "Список бронирований получен")
	@GetMapping("/event/{eventId}")
	public ResponseEntity<List<Booking>> getBookingsByExhibitionEvent(@PathVariable("eventId") Long eventId) {
		List<Booking> bookings = bookingService.getBookingsByExhibitionEvent(eventId);
		return ResponseEntity.ok(bookings);
	}

	@Operation(summary = "Получение бронирований по галерее", description = "Получение всех бронирований для конкретной галереи")
	@ApiResponse(responseCode = "200", description = "Список бронирований получен")
	@GetMapping("/gallery/{galleryId}")
	public ResponseEntity<List<Booking>> getBookingsByGallery(@PathVariable("galleryId") Long galleryId) {

		List<Booking> bookings = bookingService.getBookingsByGallery(galleryId);
		return ResponseEntity.ok(bookings);
	}

	@Operation(summary = "Получение активных бронирований художника", description = "Получение подтвержденных бронирований конкретного художника")
	@ApiResponse(responseCode = "200", description = "Список бронирований получен")
	@GetMapping("/artist/{artistId}/active")
	public ResponseEntity<?> getActiveBookingsByArtist(@PathVariable("artistId") Long artistId) {
		try {
			List<Booking> bookings = bookingService.getActiveBookingsByArtist(artistId);
			return ResponseEntity.ok(bookings);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
		}
	}

	@Operation(summary = "Получение ожидающих бронирований", description = "Получение всех бронирований со статусом PENDING")
	@ApiResponse(responseCode = "200", description = "Список ожидающих бронирований получен")
	@GetMapping("/pending")
	public ResponseEntity<List<Booking>> getPendingBookings() {
		List<Booking> bookings = bookingService.getPendingBookings();
		return ResponseEntity.ok(bookings);
	}

	@Operation(summary = "Поиск бронирований в диапазоне дат", description = "Поиск бронирований, созданных в указанном диапазоне дат")
	@ApiResponse(responseCode = "200", description = "Список бронирований получен")
	@GetMapping("/search/by-date")
	public ResponseEntity<List<Booking>> getBookingsBetweenDates(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
		List<Booking> bookings = bookingService.getBookingsBetweenDates(startDate, endDate);
		return ResponseEntity.ok(bookings);
	}

	@Operation(summary = "Подсчет бронирований художника", description = "Получение общего количества бронирований конкретного художника")
	@ApiResponse(responseCode = "200", description = "Количество получено")
	@GetMapping("/artist/{artistId}/count")
	public ResponseEntity<?> countBookingsByArtist(@PathVariable("artistId") Long artistId) {
		try {
			long count = bookingService.countBookingsByArtist(artistId);
			return ResponseEntity.ok(Map.of("artistId", artistId, "bookingCount", count));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
		}
	}

	@Operation(summary = "Получение бронирований по галерее и статусу", description = "Фильтрация бронирований конкретной галереи по статусу")
	@ApiResponse(responseCode = "200", description = "Список бронирований получен")
	@GetMapping("/gallery/{galleryId}/status/{status}")
	public ResponseEntity<?> getBookingsByGalleryAndStatus(@PathVariable("galleryId") Long galleryId, @PathVariable("status") String status) {
		try {
			BookingStatus bookingStatus = BookingStatus.valueOf(status.toUpperCase());
			List<Booking> bookings = bookingService.getBookingsByGalleryAndStatus(galleryId, bookingStatus);
			return ResponseEntity.ok(bookings);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Неверный статус: " + status));
		}
	}

}
