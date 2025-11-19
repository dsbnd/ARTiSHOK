package lab1.entities;

import jakarta.persistence.*;
import lab1.entities.enums.ArtworkStatus;

@Entity
@Table(name = "artworks")
public class Artwork {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "booking_id", nullable = false)
	private Booking booking;

	@Column(name = "title", nullable = false, length = 255)
	private String title;

	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@Column(name = "creation_year")
	private Integer creationYear;

	@Column(name = "technique", length = 255)
	private String technique;

	@Column(name = "image_url", length = 500)
	private String imageUrl;

	@Enumerated(EnumType.STRING)
	@Column(name = "status")
	private ArtworkStatus status = ArtworkStatus.DRAFT;

	public Artwork() {
	}

	public Artwork(Booking booking, String title) {
		this.booking = booking;
		this.title = title;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Booking getBooking() {
		return booking;
	}

	public void setBooking(Booking booking) {
		this.booking = booking;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Integer getCreationYear() {
		return creationYear;
	}

	public void setCreationYear(Integer creationYear) {
		this.creationYear = creationYear;
	}

	public String getTechnique() {
		return technique;
	}

	public void setTechnique(String technique) {
		this.technique = technique;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public ArtworkStatus getStatus() {
		return status;
	}

	public void setStatus(ArtworkStatus status) {
		this.status = status;
	}
}
