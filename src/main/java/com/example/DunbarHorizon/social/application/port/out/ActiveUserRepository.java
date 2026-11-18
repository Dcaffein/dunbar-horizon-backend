package com.example.DunbarHorizon.social.application.port.out;

public interface ActiveUserRepository {

    boolean existsById(Long userId);
}
