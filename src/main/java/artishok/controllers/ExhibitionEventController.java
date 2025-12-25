package artishok.controllers;

import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import artishok.entities.ExhibitionEvent;
import artishok.services.ExhibitionEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequestMapping("/exhibition-events")
public class ExhibitionEventController {
    private final ExhibitionEventService exhibitionEventService;

    public ExhibitionEventController(ExhibitionEventService exhibitionEventService) {
        this.exhibitionEventService = exhibitionEventService;
    }

    @GetMapping
    @Operation(summary = "Получить все выставочные события")
    @ApiResponse(responseCode = "200", description = "Список событий успешно получен")
    @ApiResponse(responseCode = "204", description = "События не найдены")
    public ResponseEntity<List<ExhibitionEvent>> getAllExhibitionEvents() {
        List<ExhibitionEvent> events = exhibitionEventService.getAllExhibitionEvents();
        
        if (events.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить выставочное событие по ID")
    @ApiResponse(responseCode = "200", description = "Событие найдено")
    @ApiResponse(responseCode = "404", description = "Событие не найдено")
    public ResponseEntity<ExhibitionEvent> getExhibitionEventById(@PathVariable("id") Long id) {
        Optional<ExhibitionEvent> event = exhibitionEventService.getExhibitionEventById(id);
        return event.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/gallery/{galleryId}")
    @Operation(summary = "Получить события по ID галереи")
    @ApiResponse(responseCode = "200", description = "События найдены")
    @ApiResponse(responseCode = "204", description = "События не найдены")
    public ResponseEntity<List<ExhibitionEvent>> getEventsByGalleryId(@PathVariable("galleryId") Long galleryId) {
        List<ExhibitionEvent> events = exhibitionEventService.getExhibitionEventsByGalleryId(galleryId);
        if (events.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(events);
    }

    @PostMapping
    @Operation(summary = "Создать новое выставочное событие")
    @ApiResponse(responseCode = "200", description = "Событие успешно создано")
    public ResponseEntity<ExhibitionEvent> createExhibitionEvent(@RequestBody ExhibitionEvent exhibitionEvent) {
        ExhibitionEvent savedEvent = exhibitionEventService.saveExhibitionEvent(exhibitionEvent);
        return ResponseEntity.ok(savedEvent);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить выставочное событие")
    @ApiResponse(responseCode = "200", description = "Событие успешно обновлено")
    @ApiResponse(responseCode = "404", description = "Событие не найдено")
    public ResponseEntity<ExhibitionEvent> updateExhibitionEvent(@PathVariable("id") Long id, @RequestBody ExhibitionEvent exhibitionEvent) {
        if (!exhibitionEventService.getExhibitionEventById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        exhibitionEvent.setId(id);
        ExhibitionEvent updatedEvent = exhibitionEventService.saveExhibitionEvent(exhibitionEvent);
        return ResponseEntity.ok(updatedEvent);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить выставочное событие")
    @ApiResponse(responseCode = "200", description = "Событие успешно удалено")
    public ResponseEntity<Void> deleteExhibitionEvent(@PathVariable("id") Long id) {
        exhibitionEventService.deleteExhibitionEvent(id);
        return ResponseEntity.ok().build();
    }
}
