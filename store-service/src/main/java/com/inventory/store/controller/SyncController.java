package com.inventory.store.controller;

import com.inventory.store.dto.SyncResultDTO;
import com.inventory.store.service.SyncPushService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sync")
public class SyncController {
    private final SyncPushService syncPushService;

    public SyncController(SyncPushService syncPushService) {
        this.syncPushService = syncPushService;
    }

    @PostMapping("/push")
    public ResponseEntity<SyncResultDTO> push() {
        SyncResultDTO result = syncPushService.pushNow();
        return ResponseEntity.ok(result);
    }
}


