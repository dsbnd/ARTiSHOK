package artishok.controllers;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import artishok.entities.Gallery;
import artishok.repositories.*;
import artishok.services.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequestMapping("/galleries")
public class GalleryController {
    private final GalleryService galleryService;
    private final ImageService imageService;
    private final GalleryRepository galleryRepository;

    public GalleryController(GalleryService galleryService, ImageService imageService, GalleryRepository galleryRepository) {
        this.galleryService = galleryService;
        this.imageService = imageService;
        this.galleryRepository = galleryRepository;
    }

    @GetMapping
    @Operation(summary = "Получить все галереи")
    @ApiResponse(responseCode = "200", description = "Список галерей успешно получен")
    @ApiResponse(responseCode = "204", description = "Галереи не найдены")
    public ResponseEntity<List<Gallery>> getAllGalleries() {
        List<Gallery> galleries = galleryService.getAllGalleries();
        if (galleries.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(galleries);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить галерею по ID")
    @ApiResponse(responseCode = "200", description = "Галерея найдена")
    @ApiResponse(responseCode = "404", description = "Галерея не найдена")
    public ResponseEntity<Gallery> getGalleryById(@PathVariable Long id) {
        Optional<Gallery> gallery = galleryService.getGalleryById(id);
        return gallery.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Создать новую галерею")
    @ApiResponse(responseCode = "200", description = "Галерея успешно создана")
    public ResponseEntity<Gallery> createGallery(@RequestBody Gallery gallery) {
        Gallery savedGallery = galleryService.saveGallery(gallery);
        return ResponseEntity.ok(savedGallery);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить галерею")
    @ApiResponse(responseCode = "200", description = "Галерея успешно обновлена")
    @ApiResponse(responseCode = "404", description = "Галерея не найдена")
    public ResponseEntity<Gallery> updateGallery(@PathVariable Long id, @RequestBody Gallery gallery) {
        if (!galleryService.getGalleryById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        gallery.setId(id);
        Gallery updatedGallery = galleryService.saveGallery(gallery);
        return ResponseEntity.ok(updatedGallery);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить галерею")
    @ApiResponse(responseCode = "200", description = "Галерея успешно удалена")
    public ResponseEntity<Void> deleteGallery(@PathVariable Long id) {
        galleryService.deleteGallery(id);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{id}/logo")
    public ResponseEntity<?> uploadLogo(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        
        try {
            Gallery gallery = galleryRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Gallery not found"));
            
            // Удаляем старый логотип
            if (gallery.getLogoUrl() != null) {
                imageService.deleteImage(gallery.getLogoUrl());
            }
            
            String logoUrl = imageService.uploadImage(file, "gallery", id);
            gallery.setLogoUrl(logoUrl);
            galleryRepository.save(gallery);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "logoUrl", logoUrl
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    
    // Создание галереи с логотипом
    @PostMapping("/create-with-logo")
    public ResponseEntity<?> createGalleryWithLogo(
            @RequestParam("name") String name,
            @RequestParam("address") String address,
            @RequestParam("contactEmail") String contactEmail,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        
        try {
            Gallery gallery = new Gallery();
            gallery.setName(name);
            gallery.setAddress(address);
            gallery.setContactEmail(contactEmail);
            
            Gallery savedGallery = galleryRepository.save(gallery);
            
            if (file != null && !file.isEmpty()) {
                String logoUrl = imageService.uploadImage(file, "gallery", savedGallery.getId());
                savedGallery.setLogoUrl(logoUrl);
                galleryRepository.save(savedGallery);
            }
            
            return ResponseEntity.ok(savedGallery);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}