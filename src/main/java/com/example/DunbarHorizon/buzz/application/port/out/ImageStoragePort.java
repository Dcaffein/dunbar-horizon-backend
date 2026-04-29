package com.example.DunbarHorizon.buzz.application.port.out;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ImageStoragePort {
    List<String> upload(List<MultipartFile> files);
}
