package ru.itis.dental.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@Slf4j
public class FileService {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    public String saveFile(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            Path uploadPath = Paths.get(uploadDir, subDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + extension;

            Path filePath = uploadPath.resolve(filename);
            Files.write(filePath, file.getBytes());

            String fileUrl = "/uploads/" + subDir + "/" + filename;
            log.info("File saved: {}", fileUrl);
            return fileUrl;

        } catch (IOException e) {
            log.error("Failed to save file: {}", e.getMessage());
            return null;
        }
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            log.warn("File URL is null or empty, skipping deletion");
            return;
        }

        if (!fileUrl.startsWith("/uploads/")) {
            log.warn("File URL does not start with /uploads/, skipping deletion: {}", fileUrl);
            return;
        }

        try {
            String relativePath = fileUrl.substring("/uploads/".length());
            Path filePath = Paths.get(uploadDir, relativePath);

            if (Files.exists(filePath)) {
                Files.deleteIfExists(filePath);
                log.info("File deleted: {}", fileUrl);
            } else {
                log.warn("File not found: {}", fileUrl);
            }
        } catch (IOException e) {
            log.error("Failed to delete file: {}", e.getMessage());
        }
    }
}