package artishok.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;

import artishok.entities.enums.BookingStatus;

@Entity
@Table(name = "bookings")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exhibition_stand_id", nullable = false)
    private ExhibitionStand exhibitionStand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id", nullable = false)
    private User artist;

    @Column(name = "booking_date")
    private LocalDateTime bookingDate = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BookingStatus status = BookingStatus.PENDING;
    
    public Booking() {}

    public Booking(ExhibitionStand exhibitionStand, User artist) {
        this.exhibitionStand = exhibitionStand;
        this.artist = artist;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ExhibitionStand getExhibitionStand() { return exhibitionStand; }
    public void setExhibitionStand(ExhibitionStand exhibitionStand) { this.exhibitionStand = exhibitionStand; }

    public User getArtist() { return artist; }
    public void setArtist(User artist) { this.artist = artist; }

    public LocalDateTime getBookingDate() { return bookingDate; }
    public void setBookingDate(LocalDateTime bookingDate) { this.bookingDate = bookingDate; }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
}
