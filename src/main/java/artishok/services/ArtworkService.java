package artishok.services;

import artishok.entities.Artwork;
import artishok.entities.Booking;
import artishok.entities.enums.ArtworkStatus;
import artishok.entities.enums.BookingStatus;
import artishok.repositories.ArtworkRepository;
import artishok.repositories.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ArtworkService {

	private final ArtworkRepository artworkRepository;
	private final BookingRepository bookingRepository;

	public ArtworkService(ArtworkRepository artworkRepository, BookingRepository bookingRepository) {
		this.artworkRepository = artworkRepository;
		this.bookingRepository = bookingRepository;
	}

	@Transactional
	public Artwork createArtwork(Long bookingId, Artwork artwork) {
		Booking booking = bookingRepository.findById(bookingId)
				.orElseThrow(() -> new IllegalArgumentException("Бронирование не найдено"));

		if (!booking.getStatus().equals(BookingStatus.CONFIRMED)) {
			throw new IllegalArgumentException("Можно добавлять произведения только к подтвержденным бронированиям");
		}

		if (artworkRepository.existsByTitleAndArtistId(artwork.getTitle(), booking.getArtist().getId())) {
			throw new IllegalArgumentException("У вас уже есть произведение с таким названием");
		}

		artwork.setBooking(booking);

		if (artwork.getStatus() == null) {
			artwork.setStatus(ArtworkStatus.DRAFT);
		}

		if (artwork.getCreationYear() != null && artwork.getCreationYear() > LocalDateTime.now().getYear()) {
			throw new IllegalArgumentException("Год создания не может быть в будущем");
		}

		return artworkRepository.save(artwork);
	}

	public Optional<Artwork> getArtworkById(Long id) {
		return artworkRepository.findById(id);
	}

	@Transactional
	public Artwork updateArtwork(Long id, Artwork updatedArtwork) {
		return artworkRepository.findById(id).map(existingArtwork -> {
			if (updatedArtwork.getTitle() != null) {
				if (!existingArtwork.getTitle().equalsIgnoreCase(updatedArtwork.getTitle())) {
					Long artistId = existingArtwork.getBooking().getArtist().getId();
					if (artworkRepository.existsByTitleAndArtistId(updatedArtwork.getTitle(), artistId)) {
						throw new IllegalArgumentException("У вас уже есть произведение с таким названием");
					}
				}
				existingArtwork.setTitle(updatedArtwork.getTitle());
			}

			if (updatedArtwork.getDescription() != null) {
				existingArtwork.setDescription(updatedArtwork.getDescription());
			}

			if (updatedArtwork.getCreationYear() != null) {
				if (updatedArtwork.getCreationYear() > LocalDateTime.now().getYear()) {
					throw new IllegalArgumentException("Год создания не может быть в будущем");
				}
				existingArtwork.setCreationYear(updatedArtwork.getCreationYear());
			}

			if (updatedArtwork.getTechnique() != null) {
				existingArtwork.setTechnique(updatedArtwork.getTechnique());
			}

			if (updatedArtwork.getImageUrl() != null) {
				existingArtwork.setImageUrl(updatedArtwork.getImageUrl());
			}

			if (updatedArtwork.getStatus() != null) {
				existingArtwork.setStatus(updatedArtwork.getStatus());
			}

			return artworkRepository.save(existingArtwork);
		}).orElseThrow(() -> new RuntimeException("Произведение не найдено"));
	}


	@Transactional
	public Artwork publishArtwork(Long id) {
		return artworkRepository.findById(id).map(artwork -> {
			artwork.setStatus(ArtworkStatus.PUBLISHED);
			return artworkRepository.save(artwork);
		}).orElseThrow(() -> new RuntimeException("Произведение не найдено"));
	}

	@Transactional
	public Artwork draftArtwork(Long id) {
		return artworkRepository.findById(id).map(artwork -> {
			artwork.setStatus(ArtworkStatus.DRAFT);
			return artworkRepository.save(artwork);
		}).orElseThrow(() -> new RuntimeException("Произведение не найдено"));
	}

	@Transactional
	public void deleteArtwork(Long id) {
		artworkRepository.deleteById(id);
	}

	public List<Artwork> getAllArtworks() {
		return artworkRepository.findAll();
	}

	public List<Artwork> getArtworksByBooking(Long bookingId) {
		return artworkRepository.findByBookingId(bookingId);
	}

	public List<Artwork> getArtworksByArtist(Long artistId) {
		return artworkRepository.findByArtistId(artistId);
	}

	public List<Artwork> getArtworksByStatus(ArtworkStatus status) {
		return artworkRepository.findByStatus(status);
	}

	public List<Artwork> getPublishedArtworks() {
		return artworkRepository.findByStatus(ArtworkStatus.PUBLISHED);
	}

	public List<Artwork> getDraftArtworks() {
		return artworkRepository.findByStatus(ArtworkStatus.DRAFT);
	}

	public List<Artwork> getArtworksByArtistAndStatus(Long artistId, ArtworkStatus status) {
		return artworkRepository.findByArtistIdAndStatus(artistId, status);
	}

	public List<Artwork> searchArtworksByTitle(String title) {
		return artworkRepository.findByTitleContainingIgnoreCase(title);
	}

	public List<Artwork> searchArtworksByTechnique(String technique) {
		return artworkRepository.findByTechniqueContainingIgnoreCase(technique);
	}

	public List<Artwork> getArtworksByYear(Integer year) {
		return artworkRepository.findByCreationYear(year);
	}

	public List<Artwork> getArtworksByYearRange(Integer startYear, Integer endYear) {
		return artworkRepository.findByCreationYearBetween(startYear, endYear);
	}

	public List<Artwork> getArtworksByExhibitionEvent(Long eventId) {
		return artworkRepository.findByExhibitionEventId(eventId);
	}

	public List<Artwork> getArtworksByGallery(Long galleryId) {
		return artworkRepository.findByGalleryId(galleryId);
	}

	public List<Artwork> getPublishedArtworksByGallery(Long galleryId) {
		return artworkRepository.findByGalleryIdAndStatus(galleryId, ArtworkStatus.PUBLISHED);
	}

	public boolean artworkExists(Long id) {
		return artworkRepository.existsById(id);
	}
	@Transactional
	public Artwork saveArtwork(Artwork artwork) {
		// Валидация
		if (artwork.getBooking() == null) {
			throw new IllegalArgumentException("Произведение должно быть связано с бронированием");
		}

		if (artwork.getTitle() == null || artwork.getTitle().trim().isEmpty()) {
			throw new IllegalArgumentException("Название произведения обязательно");
		}

		if (artwork.getStatus() == null) {
			artwork.setStatus(ArtworkStatus.DRAFT);
		}

		if (artwork.getCreationYear() != null && artwork.getCreationYear() > LocalDateTime.now().getYear()) {
			throw new IllegalArgumentException("Год создания не может быть в будущем");
		}

		// Проверка уникальности названия для художника
		if (artwork.getId() == null) { // только для новых записей
			Long artistId = artwork.getBooking().getArtist().getId();
			if (artworkRepository.existsByTitleAndArtistId(artwork.getTitle(), artistId)) {
				throw new IllegalArgumentException("У вас уже есть произведение с таким названием");
			}
		}

		return artworkRepository.save(artwork);
	}
}