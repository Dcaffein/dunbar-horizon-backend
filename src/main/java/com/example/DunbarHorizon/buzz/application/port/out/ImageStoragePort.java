package com.example.DunbarHorizon.buzz.application.port.out;

import com.example.DunbarHorizon.global.model.PresignRequest;
import com.example.DunbarHorizon.global.model.PresignedUploadResult;

import java.util.List;

public interface ImageStoragePort {
    List<PresignedUploadResult> presignUploads(List<PresignRequest> requests);
    List<String> resolveUrls(List<String> keys);
}
