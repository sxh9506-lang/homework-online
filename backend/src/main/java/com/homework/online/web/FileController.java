package com.homework.online.web;

import com.homework.online.common.CurrentUser;
import com.homework.online.domain.StoredFile;
import com.homework.online.dto.FileDto;
import com.homework.online.service.FileService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping
    public List<FileDto> upload(@RequestParam("files") MultipartFile[] files) {
        return fileService.store(files, CurrentUser.get());
    }

    /** 预览 / 下载：?download=1 时以附件形式下载 */
    @GetMapping("/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id,
                                             @RequestParam(required = false) String download) {
        StoredFile file = fileService.requireFile(id);
        Resource resource = fileService.loadAsResource(file);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (file.getContentType() != null && !file.getContentType().isBlank()) {
            try {
                mediaType = MediaType.parseMediaType(file.getContentType());
            } catch (Exception ignored) {
                // 类型不合法时回退为二进制流
            }
        }
        String encodedName = URLEncoder.encode(file.getName(), StandardCharsets.UTF_8).replace("+", "%20");
        String disposition = ("1".equals(download) ? "attachment" : "inline") + "; filename*=UTF-8''" + encodedName;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        fileService.delete(id, CurrentUser.get());
        return Map.of("ok", true);
    }
}
