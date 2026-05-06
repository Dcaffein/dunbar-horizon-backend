package com.example.DunbarHorizon.account.application.port.out;

import com.example.DunbarHorizon.account.application.model.UploadFile;

public interface ProfileImageStoragePort {
    String upload(UploadFile file);
}
