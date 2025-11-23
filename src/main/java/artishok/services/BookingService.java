package artishok.services;

import java.util.List;

import org.springframework.stereotype.Service;


import artishok.entities.Booking;
import artishok.repositories.BookingRepository;


@Service
public class BookingService {
	private final BookingRepository bookingRepository;
	
	BookingService(BookingRepository bookingRepository){
		
		this.bookingRepository=bookingRepository;
	}
	public List<Booking> getAllBookings() {
		return bookingRepository.findAll();
	}
}
