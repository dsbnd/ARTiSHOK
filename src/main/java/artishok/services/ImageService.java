package artishok.services;

import artishok.services.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageService {
    @Autowired
    private StorageService storageService;
    
    /**
     * Основной метод загрузки изображения
     */
    public String uploadImage(MultipartFile file, String category, Long entityId) {
        validateImage(file);
        
        // Генерируем уникальное имя файла
        String fileName = generateFileName(file.getOriginalFilename(), category, entityId);
        
        // Загружаем в хранилище
        String objectName = storageService.uploadFile(file, fileName, file.getContentType());
        
        // Получаем публичный URL
        return storageService.getFileUrl(objectName);
    }
    
    /**
     * Генерация имени файла с правильной структурой
     */
    private String generateFileName(String originalFilename, String category, Long entityId) {
        // Извлекаем расширение
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        } else {
            extension = ".jpg"; // Дефолтное расширение
        }
        
        // Генерируем UUID
        String uuid = UUID.randomUUID().toString().replace("-", "");
        
        // Формируем путь
        if (entityId != null) {
            // Структура: категория/entityId/uuid.расширение
            return String.format("%s/%d/%s%s", category, entityId, uuid, extension);
        } else {
            // Структура: категория/uuid.расширение
            return String.format("%s/%s%s", category, uuid, extension);
        }
    }
    
    /**
     * Валидация изображения
     */
    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл не может быть пустым");
        }
        
        // Проверяем размер (макс. 10MB)
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("Размер файла не должен превышать 10MB");
        }
        
        // Проверяем MIME тип
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Файл должен быть изображением (JPEG, PNG, GIF)");
        }
        
        // Проверяем расширение
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            if (!extension.matches("\\.(jpg|jpeg|png|gif|webp|bmp)$")) {
                throw new IllegalArgumentException("Неподдерживаемый формат файла");
            }
        }
    }
    
    /**
     * Удаление изображения по URL
     */
    public void deleteImage(String imageUrl) {
        try {
            // Извлекаем имя объекта из URL
            String objectName = extractObjectNameFromUrl(imageUrl);
            storageService.deleteFile(objectName);
            System.out.println("Image deleted: ");
        } catch (Exception e) {
        	System.out.println("Failed to delete image: ");
        }
    }
    
    private String extractObjectNameFromUrl(String url) {
        // Удаляем базовый URL
        String prefix = "http://localhost:9000/artishok-images/";
        if (url.startsWith(prefix)) {
            return url.substring(prefix.length());
        }
        // Если URL в другом формате, возвращаем как есть
        return url;
    }
}