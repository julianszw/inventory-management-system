package com.inventory.store.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SyncScheduler {
    private static final Logger log = LoggerFactory.getLogger(SyncScheduler.class);

    private final SyncPushService syncPushService;
    private final boolean enabled;

    public SyncScheduler(SyncPushService syncPushService,
                         @Value("${store.sync.enabled:true}") boolean enabled) {
        this.syncPushService = syncPushService;
        this.enabled = enabled;
    }

    @Scheduled(fixedDelayString = "${store.sync.fixedDelayMs:900000}")
    public void scheduledPush() {
        if (!enabled) {
            return;
        }
        String traceId = UUID.randomUUID().toString();
        MDC.put("traceId", traceId);
        try {
            syncPushService.pushNow();
        } catch (Exception ex) {
            log.warn("scheduler push error: {}", ex.getMessage());
        } finally {
            MDC.remove("traceId");
        }
    }
}


