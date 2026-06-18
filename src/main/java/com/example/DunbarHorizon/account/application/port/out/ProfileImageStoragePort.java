package com.example.DunbarHorizon.account.application.port.out;

import com.example.DunbarHorizon.global.imageStorage.PresignedUploadResult;

public interface ProfileImageStoragePort {
    PresignedUploadResult presignUpload(String contentType);
    String resolveUrl(String key);
}
