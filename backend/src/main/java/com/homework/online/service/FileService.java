package com.homework.online.service;

import com.homework.online.common.ApiException;
import com.homework.online.common.AuthUser;
import com.homework.online.domain.StoredFile;
import com.homework.online.dto.FileDto;
import com.homework.online.repo.StoredFileRepository;
import com.homework.online.repo.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FileService {

    private final StoredFileRepository fileRepository;
    private final UserRepository userRepository;
    private final Mapper mapper;
    private final Path uploadDir;

    public FileService(StoredFileRepository fileRepository,
                       UserRepository userRepository,
                       Mapper mapper,
                       @Value("${app.upload-dir:./uploads}") String uploadDir) {
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(uploadDir);
    }

    @Transactional
    public List<FileDto> store(MultipartFile[] files, AuthUser viewer) {
        if (files == null || files.length == 0) {
            throw ApiException.badRequest("未收到文件");
        }
        List<FileDto> result = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String originalName = file.getOriginalFilename() == null ? "未命名文件" : file.getOriginalFilename();
            String extension = "";
            int dot = originalName.lastIndexOf('.');
            if (dot >= 0 && dot > originalName.length() - 12) {
                extension = originalName.substring(dot);
            }
            String storedName = UUID.randomUUID().toString().replace("-", "") + extension;
            try {
                Files.copy(file.getInputStream(), uploadDir.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new ApiException("文件保存失败：" + e.getMessage());
            }
            StoredFile entity = new StoredFile();
            entity.setName(originalName);
            entity.setSize(file.getSize());
            entity.setContentType(file.getContentType());
            entity.setStoredName(storedName);
            entity.setUploader(userRepository.findById(viewer.id()).orElse(null));
            result.add(mapper.toFileDto(fileRepository.save(entity)));
        }
        if (result.isEmpty()) {
            throw ApiException.badRequest("未收到有效文件");
        }
        return result;
    }

    public StoredFile requireFile(Long id) {
        return fileRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("文件不存在"));
    }

    public Resource loadAsResource(StoredFile file) {
        try {
            Path path = uploadDir.resolve(file.getStoredName()).normalize();
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw ApiException.notFound("文件已丢失");
            }
            return resource;
        } catch (IOException e) {
            throw ApiException.notFound("文件读取失败");
        }
    }

    @Transactional
    public void delete(Long id, AuthUser viewer) {
        StoredFile file = requireFile(id);
        if (file.getUploader() != null && !file.getUploader().getId().equals(viewer.id())) {
            throw ApiException.forbidden("无权删除该文件");
        }
        try {
            Files.deleteIfExists(uploadDir.resolve(file.getStoredName()));
        } catch (IOException ignored) {
            // 磁盘文件不存在时忽略
        }
        fileRepository.delete(file);
    }
}
