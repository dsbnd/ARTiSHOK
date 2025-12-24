package artishok.controllers;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import artishok.entities.Artwork;
import artishok.entities.Booking;
import artishok.entities.enums.ArtworkStatus;
import artishok.services.*;
import artishok.repositories.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/artworks")
public class ArtworkController {

    private final ArtworkRepository artworkRepository;
	private final ArtworkService artworkService;
	private final ImageService imageService;

	ArtworkController(ArtworkService artworkService, ImageService imageService, ArtworkRepository artworkRepository) {
		this.artworkService = artworkService;
		this.imageService = imageService;
		this.artworkRepository = artworkRepository;
		
	}

	@Operation(summary = "Создание нового произведения", description = "Добавление произведения к подтвержденному бронированию")
	@ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Произведение успешно создано"),
			@ApiResponse(responseCode = "400", description = "Неверные входные данные"),
			@ApiResponse(responseCode = "404", description = "Бронирование не найдено") })
	@PostMapping
	public ResponseEntity<?> createArtwork(@RequestParam("bookingId") Long bookingId, @RequestBody Artwork artwork) {
		try {
			Artwork createdArtwork = artworkService.createArtwork(bookingId, artwork);
			return ResponseEntity.status(HttpStatus.CREATED).body(createdArtwork);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
		}
	}

	@Operation(summary = "Получение произведения по ID", description = "Получение информации о произведении по его идентификатору")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Произведение найдено"),
			@ApiResponse(responseCode = "404", description = "Произведение не найдено") })
	@GetMapping("/{id}")
	public ResponseEntity<?> getArtworkById(@PathVariable("id") Long id) {
		Optional<Artwork> opt_artwork = artworkService.getArtworkById(id);
		if (opt_artwork.isPresent()) {
			Artwork artwork = opt_artwork.get();
			return ResponseEntity.ok(artwork);
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Произведение не найдено"));
		}

	}

	@Operation(summary = "Обновление произведения", description = "Обновление информации о произведении")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Произведение успешно обновлено"),
			@ApiResponse(responseCode = "400", description = "Неверные данные"),
			@ApiResponse(responseCode = "404", description = "Произведение не найдено") })
	@PutMapping("/{id}")
	public ResponseEntity<?> updateArtwork(@PathVariable("id") Long id, @RequestBody Artwork artwork) {

		try {
			Artwork updatedArtwork = artworkService.updateArtwork(id, artwork);
			return ResponseEntity.ok(updatedArtwork);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
		} catch (RuntimeException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
		}
	}

	@Operation(summary = "Публикация произведения", description = "Изменение статуса произведения на PUBLISHED")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Произведение опубликовано"),
			@ApiResponse(responseCode = "404", description = "Произведение не найдено") })
	@PostMapping("/{id}/publish")
	public ResponseEntity<?> publishArtwork(@PathVariable("id") Long id) {
		try {
			Artwork publishedArtwork = artworkService.publishArtwork(id);
			return ResponseEntity.ok(publishedArtwork);
		} catch (RuntimeException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
		}
	}

	@Operation(summary = "Перевод произведения в черновик", description = "Изменение статуса произведения на DRAFT")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Произведение переведено в черновик"),
			@ApiResponse(responseCode = "404", description = "Произведение не найдено") })
	@PostMapping("/{id}/draft")
	public ResponseEntity<?> draftArtwork(@PathVariable("id") Long id) {
		try {
			Artwork draftedArtwork = artworkService.draftArtwork(id);
			return ResponseEntity.ok(draftedArtwork);
		} catch (RuntimeException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
		}
	}

	@Operation(summary = "Удаление произведения", description = "Полное удаление произведения из системы")
	@ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Произведение удалено"),
			@ApiResponse(responseCode = "404", description = "Произведение не найдено") })
	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteArtwork(@PathVariable("id") Long id) {
		if (!artworkService.artworkExists(id)) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Произведение не найдено"));
		}

		artworkService.deleteArtwork(id);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Получение всех произведений", description = "Получение списка всех произведений в системе")
	@ApiResponse(responseCode = "200", description = "Список произведений получен")
	@GetMapping
	public ResponseEntity<List<Artwork>> getAllArtworks() {
		List<Artwork> artworks = artworkService.getAllArtworks();
		return ResponseEntity.ok(artworks);
	}

	@Operation(summary = "Получение произведений по художнику", description = "Получение всех произведений конкретного художника")
	@ApiResponse(responseCode = "200", description = "Список произведений получен")
	@GetMapping("/artist/{artistId}")
	public ResponseEntity<List<Artwork>> getArtworksByArtist(@PathVariable("artistId") Long artistId) {
		List<Artwork> artworks = artworkService.getArtworksByArtist(artistId);
		return ResponseEntity.ok(artworks);
	}

	@Operation(summary = "Получение произведений по статусу", description = "Фильтрация произведений по статусу")
	@ApiResponse(responseCode = "200", description = "Список произведений получен")
	@GetMapping("/status/{status}")
	public ResponseEntity<?> getArtworksByStatus(@PathVariable("status") String status) {
		try {
			ArtworkStatus artworkStatus = ArtworkStatus.valueOf(status.toUpperCase());
			List<Artwork> artworks = artworkService.getArtworksByStatus(artworkStatus);
			return ResponseEntity.ok(artworks);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Неверный статус: " + status));
		}
	}

	@Operation(summary = "Получение опубликованных произведений", description = "Получение всех опубликованных произведений")
	@ApiResponse(responseCode = "200", description = "Список произведений получен")
	@GetMapping("/published")
	public ResponseEntity<List<Artwork>> getPublishedArtworks() {
		List<Artwork> artworks = artworkService.getPublishedArtworks();
		return ResponseEntity.ok(artworks);
	}

	@Operation(summary = "Получение черновиков", description = "Получение всех произведений со статусом DRAFT")
	@ApiResponse(responseCode = "200", description = "Список черновиков получен")
	@GetMapping("/drafts")
	public ResponseEntity<List<Artwork>> getDraftArtworks() {
		List<Artwork> artworks = artworkService.getDraftArtworks();
		return ResponseEntity.ok(artworks);
	}

	@Operation(summary = "Поиск произведений по названию", description = "Поиск произведений по части названия")
	@ApiResponse(responseCode = "200", description = "Результаты поиска")
	@GetMapping("/search/title")
	public ResponseEntity<List<Artwork>> searchArtworksByTitle(@RequestParam("title") String title) {
		List<Artwork> artworks = artworkService.searchArtworksByTitle(title);
		return ResponseEntity.ok(artworks);
	}

	@Operation(summary = "Поиск произведений по технике", description = "Поиск произведений по технике исполнения")
	@ApiResponse(responseCode = "200", description = "Результаты поиска")
	@GetMapping("/search/technique")
	public ResponseEntity<List<Artwork>> searchArtworksByTechnique(@RequestParam("technique") String technique) {
		List<Artwork> artworks = artworkService.searchArtworksByTechnique(technique);
		return ResponseEntity.ok(artworks);
	}

	@Operation(summary = "Получение произведений по году создания", description = "Фильтрация произведений по году создания")
	@ApiResponse(responseCode = "200", description = "Список произведений получен")
	@GetMapping("/year/{year}")
	public ResponseEntity<List<Artwork>> getArtworksByYear(@PathVariable("year") Integer year) {
		List<Artwork> artworks = artworkService.getArtworksByYear(year);
		return ResponseEntity.ok(artworks);
	}

	@Operation(summary = "Получение произведений по диапазону годов", description = "Фильтрация произведений по диапазону годов создания")
	@ApiResponse(responseCode = "200", description = "Список произведений получен")
	@GetMapping("/year-range")
	public ResponseEntity<List<Artwork>> getArtworksByYearRange(@RequestParam("startYear") Integer startYear,
			@RequestParam("endYear") Integer endYear) {
		List<Artwork> artworks = artworkService.getArtworksByYearRange(startYear, endYear);
		return ResponseEntity.ok(artworks);
	}

	@Operation(summary = "Получение произведений по мероприятию", description = "Получение всех произведений для конкретного мероприятия")
	@ApiResponse(responseCode = "200", description = "Список произведений получен")
	@GetMapping("/event/{eventId}")
	public ResponseEntity<List<Artwork>> getArtworksByExhibitionEvent(@PathVariable("eventId") Long eventId) {

		List<Artwork> artworks = artworkService.getArtworksByExhibitionEvent(eventId);
		return ResponseEntity.ok(artworks);
	}

	@Operation(summary = "Получение произведений по галерее", description = "Получение всех произведений для конкретной галереи")
	@ApiResponse(responseCode = "200", description = "Список произведений получен")
	@GetMapping("/gallery/{galleryId}")
	public ResponseEntity<List<Artwork>> getArtworksByGallery(@PathVariable("galleryId") Long galleryId) {
		List<Artwork> artworks = artworkService.getArtworksByGallery(galleryId);
		return ResponseEntity.ok(artworks);
	}

	@Operation(summary = "Получение опубликованных произведений по галерее", description = "Получение опубликованных произведений для конкретной галереи")
	@ApiResponse(responseCode = "200", description = "Список произведений получен")
	@GetMapping("/gallery/{galleryId}/published")
	public ResponseEntity<List<Artwork>> getPublishedArtworksByGallery(@PathVariable("galleryId") Long galleryId) {
		List<Artwork> artworks = artworkService.getPublishedArtworksByGallery(galleryId);
		return ResponseEntity.ok(artworks);
	}
	
	@PostMapping("/{id}/image")
    public ResponseEntity<?> uploadImageToArtwork(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        
        try {
            Artwork artwork = artworkRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Artwork not found"));
            
            String imageUrl = imageService.uploadImage(file, "artwork", id);
            artwork.setImageUrl(imageUrl);
            artworkRepository.save(artwork);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Image uploaded",
                "imageUrl", imageUrl
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    // 2. Создание artwork + изображение одним запросом
    @PostMapping("/create-with-image")
    public ResponseEntity<?> createArtworkWithImage(
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam("bookingId") Long bookingId) {
        
        try {
            // Сначала создаем artwork
            Artwork artwork = new Artwork();
            artwork.setTitle(title);
            artwork.setDescription(description);
            // booking нужно достать из репозитория
            // artwork.setBooking(bookingRepository.findById(bookingId).orElseThrow());
            
            Artwork savedArtwork = artworkRepository.save(artwork);
            
            // Если есть файл - загружаем
            if (file != null && !file.isEmpty()) {
                String imageUrl = imageService.uploadImage(file, "artwork", savedArtwork.getId());
                savedArtwork.setImageUrl(imageUrl);
                artworkRepository.save(savedArtwork);
            }
            
            return ResponseEntity.ok(savedArtwork);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    // 3. Обновление изображения
    @PutMapping("/{id}/image")
    public ResponseEntity<?> updateArtworkImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        
        try {
            Artwork artwork = artworkRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Artwork not found"));
            
            // Удаляем старое изображение
            if (artwork.getImageUrl() != null) {
                imageService.deleteImage(artwork.getImageUrl());
            }
            
            String newImageUrl = imageService.uploadImage(file, "artwork", id);
            artwork.setImageUrl(newImageUrl);
            artworkRepository.save(artwork);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "imageUrl", newImageUrl
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

}
