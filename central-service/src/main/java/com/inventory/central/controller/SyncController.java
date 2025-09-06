package com.inventory.central.controller;

import com.inventory.central.dto.SyncBatchDTO;
import com.inventory.central.dto.SyncResultDTO;
import com.inventory.central.service.SyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/sync")
public class SyncController {
    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping("/pull")
    public ResponseEntity<SyncResultDTO> pull(@Valid @RequestBody SyncBatchDTO batch) {
        SyncResultDTO result = syncService.applyBatchLWW(batch);
        return ResponseEntity.ok(result);
    }
}


