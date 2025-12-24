package artishok.services.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    String uploadFile(MultipartFile file, String objectName, String contentType);
    String getFileUrl(String objectName);
    void deleteFile(String objectName);
}