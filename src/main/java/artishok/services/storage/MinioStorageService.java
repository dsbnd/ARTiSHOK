package artishok.services.storage;

import io.minio.*;
import io.minio.errors.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Service
@Slf4j
public class MinioStorageService implements StorageService {
    
    private final MinioClient minioClient;
    private final String bucketName;
    
    @Value("${minio.public-url}")
    private String publicUrl;
    
    public MinioStorageService(MinioClient minioClient, String bucketName) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
        initializeBucket();
    }
    
    private void initializeBucket() {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());
            
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
                
                // Делаем bucket публичным для чтения
                String policy = """
                    {
                        "Version": "2012-10-17",
                        "Statement": [
                            {
                                "Effect": "Allow",
                                "Principal": {"AWS": ["*"]},
                                "Action": ["s3:GetObject"],
                                "Resource": ["arn:aws:s3:::%s/*"]
                            }
                        ]
                    }
                    """.formatted(bucketName);
                
                minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                        .bucket(bucketName)
                        .config(policy)
                        .build());
                
                System.out.println("Bucket '{}' создан и настроен как публичный");
            }
        } catch (Exception e) {
        	System.out.println("Ошибка при инициализации MinIO bucket");
        }
    }
    
    @Override
    public String uploadFile(MultipartFile file, String objectName, String contentType) {
        try {
            // Если objectName не указан, генерируем уникальный
            if (objectName == null || objectName.isEmpty()) {
                String originalFilename = file.getOriginalFilename();
                String extension = "";
                if (originalFilename != null && originalFilename.contains(".")) {
                    extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }
                objectName = UUID.randomUUID() + extension;
            }
            
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(inputStream, file.getSize(), -1)
                        .contentType(contentType != null ? contentType : file.getContentType())
                        .build());
            }
            
            
            return objectName;
            
        } catch (Exception e) {
            
            throw new RuntimeException("Не удалось загрузить файл", e);
        }
    }
    
    @Override
    public String getFileUrl(String objectName) {
        if (publicUrl.endsWith("/")) {
            return publicUrl + objectName;
        }
        return publicUrl + "/" + objectName;
    }
    
    @Override
    public void deleteFile(String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
            System.out.println("Файл '{}' удален из MinIO");
        } catch (Exception e) {
        	System.out.println("Ошибка при удалении файла из MinIO");
        }
    }
}