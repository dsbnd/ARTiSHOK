package artishok.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import artishok.entities.Booking;
import artishok.entities.ExhibitionStand;
import artishok.entities.User;
import artishok.entities.enums.BookingStatus;
import artishok.repositories.BookingRepository;
import artishok.repositories.ExhibitionStandRepository;
import artishok.repositories.UserRepository;
import jakarta.transaction.Transactional;

@Service
public class BookingService {
	private final BookingRepository bookingRepository;
	private final UserRepository userRepository;
	private final ExhibitionStandRepository exhibitionStandRepository;

	public BookingService(BookingRepository bookingRepository, UserRepository userRepository,
			ExhibitionStandRepository exhibitionStandRepository) {
		this.bookingRepository = bookingRepository;
		this.userRepository = userRepository;
		this.exhibitionStandRepository = exhibitionStandRepository;
	}


    @Transactional
    public Booking createBooking(Long artistId, Long standId) {
        User artist = userRepository.findById(artistId)
                .orElseThrow(() -> new IllegalArgumentException("Художник не найден"));
        
        ExhibitionStand stand = exhibitionStandRepository.findById(standId)
                .orElseThrow(() -> new IllegalArgumentException("Стенд не найден"));
        
        if (!artist.getRole().name().equals("ARTIST")) {
            throw new IllegalArgumentException("Бронирование может выполнять только художник");
        }
        
        if (!stand.getStatus().name().equals("AVAILABLE")) {
            throw new IllegalArgumentException("Стенд недоступен для бронирования");
        }
        
        if (bookingRepository.existsByExhibitionStandIdAndStatus(standId, BookingStatus.CONFIRMED)) {
            throw new IllegalArgumentException("Стенд уже забронирован");
        }
        
        Booking booking = new Booking(stand, artist);
        booking.setBookingDate(LocalDateTime.now());
        booking.setStatus(BookingStatus.PENDING);
        
        return bookingRepository.save(booking);
    }
    

    public Optional<Booking> getBookingById(Long id) {
        return bookingRepository.findById(id);
    }
    
    @Transactional
    public Booking updateBookingStatus(Long bookingId, BookingStatus newStatus) {
        return bookingRepository.findById(bookingId)
                .map(booking -> {
                    booking.setStatus(newStatus);
                    

                    ExhibitionStand stand = booking.getExhibitionStand();
                    if (newStatus == BookingStatus.CONFIRMED) {
                        stand.setStatus(artishok.entities.enums.StandStatus.BOOKED);
                    } else if (newStatus == BookingStatus.CANCELLED) {
                        stand.setStatus(artishok.entities.enums.StandStatus.AVAILABLE);
                    }
                    exhibitionStandRepository.save(stand);
                    
                    return bookingRepository.save(booking);
                })
                .orElseThrow(() -> new RuntimeException("Бронирование не найдено"));
    }
    
    @Transactional
    public Booking confirmBooking(Long bookingId) {
        return updateBookingStatus(bookingId, BookingStatus.CONFIRMED);
    }
    
    @Transactional
    public Booking cancelBooking(Long bookingId) {
        return updateBookingStatus(bookingId, BookingStatus.CANCELLED);
    }
    
    @Transactional
    public void deleteBooking(Long id) {
        bookingRepository.deleteById(id);
    }
    
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }
    
    
    public List<Booking> getBookingsByArtist(Long artistId) {
        User artist = userRepository.findById(artistId)
                .orElseThrow(() -> new IllegalArgumentException("Художник не найден"));
        return bookingRepository.findByArtist(artist);
    }
    
    
    public List<Booking> getBookingsByStatus(BookingStatus status) {
        return bookingRepository.findByStatus(status);
    }
    
    public List<Booking> getBookingsByStand(Long standId) {
        return bookingRepository.findByExhibitionStandId(standId);
    }
    
    public List<Booking> getBookingsByExhibitionEvent(Long eventId) {
        return bookingRepository.findByExhibitionEventId(eventId);
    }
    
    public List<Booking> getBookingsByGallery(Long galleryId) {
        return bookingRepository.findByGalleryId(galleryId);
    }
    
    public List<Booking> getBookingsByGalleryAndStatus(Long galleryId, BookingStatus status) {
        return bookingRepository.findByGalleryIdAndStatus(galleryId, status);
    }
    
    public List<Booking> getActiveBookingsByArtist(Long artistId) {
        User artist = userRepository.findById(artistId)
                .orElseThrow(() -> new IllegalArgumentException("Художник не найден"));
        return bookingRepository.findByArtistAndStatus(artist, BookingStatus.CONFIRMED);
    }
    
    public List<Booking> getPendingBookings() {
        return bookingRepository.findByStatus(BookingStatus.PENDING);
    }
    
    public List<Booking> getRecentBookings() {
        return bookingRepository.findTop10ByOrderByBookingDateDesc();
    }
    
    public long countBookingsByStatus(BookingStatus status) {
        return bookingRepository.countByStatus(status);
    }
    
    public long countBookingsByArtist(Long artistId) {
        User artist = userRepository.findById(artistId)
                .orElseThrow(() -> new IllegalArgumentException("Художник не найден"));
        return bookingRepository.countByArtist(artist);
    }
    
    public boolean bookingExists(Long id) {
        return bookingRepository.existsById(id);
    }
    
    public List<Booking> getBookingsBetweenDates(LocalDateTime startDate, LocalDateTime endDate) {
        return bookingRepository.findByBookingDateBetween(startDate, endDate);
    }

    public Booking saveBooking(Booking booking) {

        if (booking.getExhibitionStand() == null) {
            throw new IllegalArgumentException("Бронирование должно быть связано со стендом");
        }

        if (booking.getArtist() == null) {
            throw new IllegalArgumentException("Бронирование должно быть связано с художником");
        }

        if (booking.getStatus() == null) {
            booking.setStatus(BookingStatus.PENDING);
        }

        if (booking.getBookingDate() == null) {
            booking.setBookingDate(LocalDateTime.now());
        }

        return bookingRepository.save(booking);
    }
}
