package com.example.DunbarHorizon.global.infra;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

    @GetMapping("/")
    public ResponseEntity<Void> healthCheck() {
        return ResponseEntity.ok().build();
    }
}
