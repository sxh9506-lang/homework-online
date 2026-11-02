package com.homework.online.dto;

/** 文件信息（url 可直接用于 img src 或下载） */
public class FileDto {

    public Long id;
    public String name;
    public Long size;
    public String contentType;
    public String url;

    public FileDto() {
    }

    public FileDto(Long id, String name, Long size, String contentType) {
        this.id = id;
        this.name = name;
        this.size = size;
        this.contentType = contentType;
        this.url = "/api/files/" + id;
    }
}
