package artishok.controllers;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import artishok.entities.ExhibitionEvent;
import artishok.services.ExhibitionEventService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import artishok.entities.ExhibitionHallMap;
import artishok.services.ExhibitionHallMapService;
import artishok.services.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequestMapping("/exhibition-hall-maps")
public class ExhibitionHallMapController {
    private final ExhibitionHallMapService exhibitionHallMapService;
    private final ImageService imageService;
    private final ExhibitionEventService eventService;

    public ExhibitionHallMapController(
            ExhibitionHallMapService exhibitionHallMapService,
            ImageService imageService,
            ExhibitionEventService eventService) {
        this.exhibitionHallMapService = exhibitionHallMapService;
        this.imageService = imageService;
        this.eventService = eventService;
    }

    @GetMapping
    @Operation(summary = "Получить все карты выставочных залов")
    @ApiResponse(responseCode = "200", description = "Список карт успешно получен")
    @ApiResponse(responseCode = "204", description = "Карты не найдены")
    public ResponseEntity<List<ExhibitionHallMap>> getAllExhibitionHallMaps() {
        List<ExhibitionHallMap> maps = exhibitionHallMapService.getAllExhibitionHallMaps();
        if (maps.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(maps);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить карту зала по ID")
    @ApiResponse(responseCode = "200", description = "Карта найдена")
    @ApiResponse(responseCode = "404", description = "Карта не найдена")
    public ResponseEntity<ExhibitionHallMap> getExhibitionHallMapById(@PathVariable("id") Long id) {
        Optional<ExhibitionHallMap> map = exhibitionHallMapService.getExhibitionHallMapById(id);
        return map.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/event/{eventId}")
    @Operation(summary = "Получить карты по ID события")
    @ApiResponse(responseCode = "200", description = "Карты найдены")
    @ApiResponse(responseCode = "204", description = "Карты не найдены")
    public ResponseEntity<List<ExhibitionHallMap>> getMapsByEventId(@PathVariable("eventId") Long eventId) {
        // Используем метод с загрузкой стендов
        List<ExhibitionHallMap> maps = exhibitionHallMapService.getExhibitionHallMapsByEventIdWithStands(eventId);

        if (maps.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        // Убедитесь, что стенды загружены
        maps.forEach(map -> {
            if (map.getExhibitionStands() != null) {
                System.out.println("Карта " + map.getId() + " имеет " + map.getExhibitionStands().size() + " стендов");
            }
        });

        return ResponseEntity.ok(maps);
    }

    // Вариант 1: Старый метод для JSON (оставляем для совместимости)
    @PostMapping("/create")
    @Operation(summary = "Создать новую карту выставочного зала (JSON)")
    @ApiResponse(responseCode = "200", description = "Карта успешно создана")
    public ResponseEntity<ExhibitionHallMap> createExhibitionHallMap(@RequestBody ExhibitionHallMap exhibitionHallMap) {
        ExhibitionHallMap savedMap = exhibitionHallMapService.saveExhibitionHallMap(exhibitionHallMap);
        return ResponseEntity.ok(savedMap);
    }

    // Вариант 2: Новый метод для загрузки с файлом
    @PostMapping(value = "/create-with-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Создать новую карту с изображением")
    @ApiResponse(responseCode = "200", description = "Карта успешно создана")
    public ResponseEntity<?> createExhibitionHallMapWithImage(
            @RequestParam("name") String name,
            @RequestParam("exhibitionEventId") Long exhibitionEventId,
            @RequestParam(value = "mapImage", required = false) MultipartFile mapImage) {

        try {
            // Получаем событие
            ExhibitionEvent event = eventService.getExhibitionEventById(exhibitionEventId)
                    .orElseThrow(() -> new IllegalArgumentException("Событие не найдено с ID: " + exhibitionEventId));

            // Создаём карту
            ExhibitionHallMap map = new ExhibitionHallMap();
            map.setName(name);
            map.setExhibitionEvent(event); // ✅ Вот это критически важно

            ExhibitionHallMap savedMap = exhibitionHallMapService.saveExhibitionHallMap(map);

            // Загружаем изображение, если есть
            if (mapImage != null && !mapImage.isEmpty()) {
                String mapImageUrl = imageService.uploadImage(mapImage, "map", savedMap.getId());
                savedMap.setMapImageUrl(mapImageUrl);
                savedMap = exhibitionHallMapService.saveExhibitionHallMap(savedMap);
            }

            return ResponseEntity.ok(savedMap);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/upload-map-image")
    @Operation(summary = "Загрузить/обновить изображение карты")
    @ApiResponse(responseCode = "200", description = "Изображение успешно загружено")
    @ApiResponse(responseCode = "404", description = "Карта не найдена")
    public ResponseEntity<?> uploadMapImage(
            @PathVariable("id") Long id,
            @RequestParam("file") MultipartFile file) {
        
        try {
            ExhibitionHallMap map = exhibitionHallMapService.getExhibitionHallMapById(id)
                    .orElseThrow(() -> new RuntimeException("Карта не найдена с ID: " + id));
            
            // Удаляем старое изображение если есть
            if (map.getMapImageUrl() != null) {
                imageService.deleteImage(map.getMapImageUrl());
            }
            
            String mapImageUrl = imageService.uploadImage(file, "map", id);
            map.setMapImageUrl(mapImageUrl);
            ExhibitionHallMap updatedMap = exhibitionHallMapService.saveExhibitionHallMap(map);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "mapImageUrl", mapImageUrl,
                "mapId", id,
                "mapName", updatedMap.getName()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить карту выставочного зала")
    @ApiResponse(responseCode = "200", description = "Карта успешно обновлена")
    @ApiResponse(responseCode = "404", description = "Карта не найдена")
    public ResponseEntity<ExhibitionHallMap> updateExhibitionHallMap(
            @PathVariable("id") Long id, 
            @RequestBody ExhibitionHallMap exhibitionHallMap) {
        
        if (!exhibitionHallMapService.getExhibitionHallMapById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        exhibitionHallMap.setId(id);
        ExhibitionHallMap updatedMap = exhibitionHallMapService.saveExhibitionHallMap(exhibitionHallMap);
        return ResponseEntity.ok(updatedMap);
    }

    // Дополнительный метод для обновления с файлом
    @PutMapping(value = "/{id}/with-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Обновить карту с возможностью загрузки изображения")
    @ApiResponse(responseCode = "200", description = "Карта успешно обновлена")
    public ResponseEntity<?> updateExhibitionHallMapWithImage(
            @PathVariable Long id,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        
        try {
            ExhibitionHallMap map = exhibitionHallMapService.getExhibitionHallMapById(id)
                    .orElseThrow(() -> new RuntimeException("Карта не найдена"));
            
            if (name != null) {
                map.setName(name);
            }
            
            // Обновляем изображение если есть новое
            if (file != null && !file.isEmpty()) {
                // Удаляем старое изображение
                if (map.getMapImageUrl() != null) {
                    imageService.deleteImage(map.getMapImageUrl());
                }
                
                String mapImageUrl = imageService.uploadImage(file, "map", id);
                map.setMapImageUrl(mapImageUrl);
            }
            
            ExhibitionHallMap updatedMap = exhibitionHallMapService.saveExhibitionHallMap(map);
            return ResponseEntity.ok(updatedMap);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить карту выставочного зала")
    @ApiResponse(responseCode = "200", description = "Карта успешно удалена")
    public ResponseEntity<Void> deleteExhibitionHallMap(@PathVariable("id") Long id) {
        // Сначала удаляем изображение если есть
        ExhibitionHallMap map = exhibitionHallMapService.getExhibitionHallMapById(id).orElse(null);
        if (map != null && map.getMapImageUrl() != null) {
            imageService.deleteImage(map.getMapImageUrl());
        }
        
        exhibitionHallMapService.deleteExhibitionHallMap(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/image")
    @Operation(summary = "Удалить изображение карты")
    @ApiResponse(responseCode = "200", description = "Изображение удалено")
    public ResponseEntity<?> deleteMapImage(@PathVariable("id") Long id) {
        
        try {
            ExhibitionHallMap map = exhibitionHallMapService.getExhibitionHallMapById(id)
                    .orElseThrow(() -> new RuntimeException("Карта не найдена"));
            
            if (map.getMapImageUrl() != null) {
                imageService.deleteImage(map.getMapImageUrl());
                map.setMapImageUrl(null);
                exhibitionHallMapService.saveExhibitionHallMap(map);
                
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Изображение удалено",
                    "mapId", id
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "У карты нет изображения",
                    "mapId", id
                ));
            }
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    @GetMapping("/{id}/with-stands")
    @Operation(summary = "Получить карту зала со стендами")
    @ApiResponse(responseCode = "200", description = "Карта найдена")
    @ApiResponse(responseCode = "404", description = "Карта не найдена")
    public ResponseEntity<ExhibitionHallMap> getHallMapWithStands(@PathVariable("id") Long id) {
        Optional<ExhibitionHallMap> map = exhibitionHallMapService.getExhibitionHallMapByIdWithStands(id);
        return map.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}