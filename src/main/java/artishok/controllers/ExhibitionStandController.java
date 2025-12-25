package artishok.controllers;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import artishok.entities.enums.StandStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import artishok.dto.StandRequestDto;
import artishok.dto.StandResponseDto;
import artishok.entities.ExhibitionHallMap;
import artishok.entities.ExhibitionStand;
import artishok.services.ExhibitionHallMapService;
import artishok.services.ExhibitionStandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/exhibition-stands")
@Tag(name = "Выставочные стенды", description = "API для управления стендами выставки")
public class ExhibitionStandController {
    private final ExhibitionStandService exhibitionStandService;
    private final ExhibitionHallMapService exhibitionHallMapService;

    public ExhibitionStandController(ExhibitionStandService exhibitionStandService,
                                     ExhibitionHallMapService exhibitionHallMapService) {
        this.exhibitionStandService = exhibitionStandService;
        this.exhibitionHallMapService = exhibitionHallMapService;
    }

    @GetMapping
    @Operation(summary = "Получить все выставочные стенды")
    @ApiResponse(responseCode = "200", description = "Список стендов успешно получен")
    @ApiResponse(responseCode = "204", description = "Стенды не найдены")
    public ResponseEntity<List<StandResponseDto>> getAllExhibitionStands() {
        List<ExhibitionStand> stands = exhibitionStandService.getAllExhibitionStands();
        if (stands.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        List<StandResponseDto> response = stands.stream()
                .map(StandResponseDto::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить стенд по ID")
    @ApiResponse(responseCode = "200", description = "Стенд найден")
    @ApiResponse(responseCode = "404", description = "Стенд не найден")
    public ResponseEntity<StandResponseDto> getExhibitionStandById(@PathVariable Long id) {
        Optional<ExhibitionStand> stand = exhibitionStandService.getExhibitionStandById(id);
        return stand.map(s -> ResponseEntity.ok(new StandResponseDto(s)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/hall-map/{hallMapId}")
    @Operation(summary = "Получить стенды по ID карты зала")
    @ApiResponse(responseCode = "200", description = "Стенды найдены")
    @ApiResponse(responseCode = "204", description = "Стенды не найдены")
    public ResponseEntity<List<StandResponseDto>> getStandsByHallMapId(@PathVariable Long hallMapId) {
        List<ExhibitionStand> stands = exhibitionStandService.getExhibitionStandsByHallMapId(hallMapId);
        if (stands.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        List<StandResponseDto> response = stands.stream()
                .map(StandResponseDto::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(summary = "Создать новый выставочный стенд")
    @ApiResponse(responseCode = "201", description = "Стенд успешно создан")
    @ApiResponse(responseCode = "400", description = "Некорректные данные")
    @ApiResponse(responseCode = "404", description = "Карта зала не найдена")
    public ResponseEntity<?> createExhibitionStand(@RequestBody StandRequestDto standRequestDto) {
        try {
            // Валидация входных данных
            validateStandRequest(standRequestDto);

            // Находим карту зала
            ExhibitionHallMap hallMap = exhibitionHallMapService.getExhibitionHallMapById(standRequestDto.getExhibitionHallMapId())
                    .orElseThrow(() -> new RuntimeException("Карта зала не найдена с ID: " + standRequestDto.getExhibitionHallMapId()));

            // Создаем объект стенда
            ExhibitionStand stand = new ExhibitionStand();
            stand.setExhibitionHallMap(hallMap);
            stand.setStandNumber(standRequestDto.getStandNumber());
            stand.setPositionX(standRequestDto.getPositionX());
            stand.setPositionY(standRequestDto.getPositionY());
            stand.setWidth(standRequestDto.getWidth());
            stand.setHeight(standRequestDto.getHeight());
            stand.setType(standRequestDto.getType());
            stand.setStatus(standRequestDto.getStatus());

            ExhibitionStand savedStand = exhibitionStandService.saveExhibitionStand(stand);
            return ResponseEntity.status(HttpStatus.CREATED).body(new StandResponseDto(savedStand));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить выставочный стенд")
    @ApiResponse(responseCode = "200", description = "Стенд успешно обновлен")
    @ApiResponse(responseCode = "400", description = "Некорректные данные")
    @ApiResponse(responseCode = "404", description = "Стенд или карта зала не найдены")
    public ResponseEntity<?> updateExhibitionStand(@PathVariable Long id, @RequestBody StandRequestDto standRequestDto) {
        try {
            // Находим существующий стенд
            ExhibitionStand existingStand = exhibitionStandService.getExhibitionStandById(id)
                    .orElseThrow(() -> new RuntimeException("Стенд не найден с ID: " + id));

            // Валидация
            validateStandRequest(standRequestDto);

            // Обновляем карту зала если изменилась
            if (!existingStand.getExhibitionHallMap().getId().equals(standRequestDto.getExhibitionHallMapId())) {
                ExhibitionHallMap newHallMap = exhibitionHallMapService.getExhibitionHallMapById(standRequestDto.getExhibitionHallMapId())
                        .orElseThrow(() -> new RuntimeException("Новая карта зала не найдена"));
                existingStand.setExhibitionHallMap(newHallMap);
            }

            // Обновляем остальные поля
            existingStand.setStandNumber(standRequestDto.getStandNumber());
            existingStand.setPositionX(standRequestDto.getPositionX());
            existingStand.setPositionY(standRequestDto.getPositionY());
            existingStand.setWidth(standRequestDto.getWidth());
            existingStand.setHeight(standRequestDto.getHeight());
            existingStand.setType(standRequestDto.getType());
            existingStand.setStatus(standRequestDto.getStatus());

            ExhibitionStand updatedStand = exhibitionStandService.saveExhibitionStand(existingStand);
            return ResponseEntity.ok(new StandResponseDto(updatedStand));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Изменить статус стенда")
    @ApiResponse(responseCode = "200", description = "Статус успешно изменен")
    @ApiResponse(responseCode = "404", description = "Стенд не найден")
    public ResponseEntity<StandResponseDto> changeStandStatus(@PathVariable Long id, @RequestParam String status) {
        Optional<ExhibitionStand> stand = exhibitionStandService.getExhibitionStandById(id);
        if (!stand.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        try {
            ExhibitionStand existingStand = stand.get();
            existingStand.setStatus(StandStatus.valueOf(status.toUpperCase()));
            ExhibitionStand updatedStand = exhibitionStandService.saveExhibitionStand(existingStand);
            return ResponseEntity.ok(new StandResponseDto(updatedStand));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить выставочный стенд")
    @ApiResponse(responseCode = "200", description = "Стенд успешно удален")
    @ApiResponse(responseCode = "404", description = "Стенд не найден")
    public ResponseEntity<Void> deleteExhibitionStand(@PathVariable Long id) {
        if (!exhibitionStandService.getExhibitionStandById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }

        exhibitionStandService.deleteExhibitionStand(id);
        return ResponseEntity.ok().build();
    }

    private void validateStandRequest(StandRequestDto dto) {
        if (dto.getExhibitionHallMapId() == null) {
            throw new IllegalArgumentException("ID карты зала обязательно");
        }
        if (dto.getStandNumber() == null || dto.getStandNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Номер стенда обязателен");
        }
        if (dto.getPositionX() == null || dto.getPositionY() == null) {
            throw new IllegalArgumentException("Координаты обязательны");
        }
        if (dto.getWidth() == null || dto.getWidth() <= 0) {
            throw new IllegalArgumentException("Ширина должна быть положительной");
        }
        if (dto.getHeight() == null || dto.getHeight() <= 0) {
            throw new IllegalArgumentException("Высота должна быть положительной");
        }
        if (dto.getType() == null) {
            throw new IllegalArgumentException("Тип стенда обязателен");
        }
        if (dto.getStatus() == null) {
            dto.setStatus(StandStatus.AVAILABLE);
        }
    }

    // Вспомогательный класс для ошибок
    private static class ErrorResponse {
        private final String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }
    }
}