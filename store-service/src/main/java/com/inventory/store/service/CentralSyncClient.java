package com.inventory.store.service;

import com.inventory.store.dto.SyncBatchDTO;
import com.inventory.store.dto.SyncResultDTO;
import com.inventory.store.exception.SyncNetworkException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
public class CentralSyncClient {
    private final RestClient restClient;

    public CentralSyncClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public SyncResultDTO pushBatch(SyncBatchDTO batch) {
        try {
            return restClient.post()
                    .uri("/sync/pull")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(batch)
                    .retrieve()
                    .body(SyncResultDTO.class);
        } catch (HttpClientErrorException | ResourceAccessException ex) {
            throw new SyncNetworkException("Error al sincronizar con el servicio central", ex);
        }
    }
}


