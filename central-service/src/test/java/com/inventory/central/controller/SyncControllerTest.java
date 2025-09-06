package com.inventory.central.controller;

import com.inventory.central.dto.SyncResultDTO;
import com.inventory.central.service.SyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import com.example.centralservice.CentralServiceApplication;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.context.annotation.Import;
import com.inventory.central.exception.GlobalExceptionHandler;

@SpringBootTest(classes = CentralServiceApplication.class)
@AutoConfigureMockMvc
@Import(GlobalExceptionHandler.class)
class SyncControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SyncService syncService;

    @Test
    void post_sync_pull_ok() throws Exception {
        when(syncService.applyBatchLWW(any())).thenReturn(SyncResultDTO.builder().received(2).applied(2).skipped(0).build());

        String body = "{\n  \"items\": [\n    {\"productId\":\"ABC-001\",\"quantity\":10,\"updatedAt\":\"2025-01-01T00:00:00Z\"}\n  ]\n}";
        mockMvc.perform(post("/sync/pull").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.received").value(2))
                .andExpect(jsonPath("$.applied").value(2))
                .andExpect(jsonPath("$.skipped").value(0));
    }

    @Test
    void post_sync_pull_empty_items_is_bad_request() throws Exception {
        String body = "{\n  \"items\": []\n}";
        mockMvc.perform(post("/sync/pull").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
}


