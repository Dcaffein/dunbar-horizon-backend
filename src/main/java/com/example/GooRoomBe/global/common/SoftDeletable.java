package com.example.GooRoomBe.global.common;

import java.time.LocalDateTime;

public interface SoftDeletable {
    void softDelete();
    boolean isDeleted();
    LocalDateTime getDeletedAt();
}