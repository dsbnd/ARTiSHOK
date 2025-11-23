package artishok.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import artishok.entities.Artwork;
import artishok.services.ArtworkService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequestMapping("/")
public class ArtworkController {
	private final ArtworkService artworkService;

	ArtworkController(ArtworkService artworkService) {
		this.artworkService = artworkService;
	}

	@GetMapping("/artworks")
	@ApiResponse(responseCode = "200", description = "Списки произведений успешно получены")
	@ApiResponse(responseCode = "204", description = "Произведения не найдены")
	public ResponseEntity<List<Artwork>> getAllArtworks() {
		List<Artwork> artworks = artworkService.getAllArtworks();
		if (artworks.isEmpty()) {
			return ResponseEntity.noContent().build();
		}
		System.out.println("Списки произведений отправлены");
		return ResponseEntity.ok(artworks);
	}

}
