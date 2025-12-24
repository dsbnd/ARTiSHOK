package artishok.services;

import artishok.services.storage.StorageService;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageService {
    @Autowired
    private StorageService storageService;
    
    /**
     * Загружает изображение и возвращает его URL
     * 
     * @param file файл изображения
     * @param category категория (artwork, gallery, avatar, map, etc.)
     * @param entityId ID сущности (опционально)
     * @return URL загруженного изображения
     */
    public String uploadImage(MultipartFile file, String category, Long entityId) {
        // Проверяем тип файла
        validateImageFile(file);
        
        // Генерируем имя файла
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String fileName = generateFileName(category, entityId, extension);
        
        // Загружаем в хранилище
        String objectName = storageService.uploadFile(file, fileName, file.getContentType());
        
        // Возвращаем публичный URL
        return storageService.getFileUrl(objectName);
    }
    
    /**
     * Генерирует структурированное имя файла
     */
    private String generateFileName(String category, Long entityId, String extension) {
        StringBuilder fileName = new StringBuilder();
        
        // Добавляем категорию
        fileName.append(category).append("/");
        
        // Добавляем ID сущности, если есть
        if (entityId != null) {
            fileName.append(entityId).append("/");
        }
        
        // Добавляем временную метку и UUID для уникальности
        fileName.append(System.currentTimeMillis())
               .append("_")
               .append(UUID.randomUUID().toString().substring(0, 8))
               .append(extension);
        
        return fileName.toString();
    }
    
    /**
     * Получает расширение файла
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
    
    /**
     * Простая валидация файла
     */
    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("Файл пустой");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Файл должен быть изображением");
        }
        
        // Проверяем размер (макс. 10MB)
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (file.getSize() > maxSize) {
            throw new RuntimeException("Размер файла не должен превышать 10MB");
        }
    }
    
    /**
     * Удаляет изображение по URL
     */
    public void deleteImage(String imageUrl) {
        try {
            // Извлекаем имя объекта из URL
            String objectName = extractObjectNameFromUrl(imageUrl);
            storageService.deleteFile(objectName);
        } catch (Exception e) {
            // Логируем ошибку, но не падаем
            e.printStackTrace();
        }
    }
    
    private String extractObjectNameFromUrl(String url) {
        // Упрощенная логика - удаляем базовый URL
        if (url.startsWith("http://localhost:9000/artishok-images/")) {
            return url.substring("http://localhost:9000/artishok-images/".length());
        }
        // Можно добавить обработку других форматов URL
        return url;
    }
}