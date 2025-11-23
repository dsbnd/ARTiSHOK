package artishok.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import artishok.entities.Booking;
import artishok.services.BookingService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequestMapping("/")
public class BookingController {
	private final BookingService bookingService;
	
	BookingController(BookingService bookingService){
		this.bookingService = bookingService;
	}
	
	@GetMapping("/bookings")
	@ApiResponse(responseCode = "200", description = "Списки броней успешно получены")
	@ApiResponse(responseCode = "204", description = "Брони не найдены")
	public ResponseEntity<List<Booking>> getAllBookings() {
		List<Booking> bookings = bookingService.getAllBookings();
		if (bookings.isEmpty()) {
			return ResponseEntity.noContent().build();
		}
		System.out.println("Списки броней отправлены");
		return ResponseEntity.ok(bookings);
	}

}
