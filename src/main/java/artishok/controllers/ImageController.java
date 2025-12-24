package artishok.controllers;

import artishok.services.ImageService;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {
    @Autowired
    private ImageService imageService;
    
    /**
     * Загрузка изображения
     * 
     * @param file файл изображения
     * @param category категория (artwork, gallery, avatar, map)
     * @param entityId ID сущности (опционально)
     * @return URL загруженного изображения
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("category") String category,
            @RequestParam(value = "entityId", required = false) Long entityId) {
        
        try {
            String imageUrl = imageService.uploadImage(file, category, entityId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "url", imageUrl,
                "filename", file.getOriginalFilename(),
                "size", file.getSize()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    /**
     * Простой тестовый эндпоинт
     */
    @GetMapping("/test")
    public ResponseEntity<?> testMinio() {
        return ResponseEntity.ok(Map.of(
            "status", "MinIO сервис работает",
            "endpoint", "http://localhost:9000",
            "bucket", "artishok-images"
        ));
    }
}