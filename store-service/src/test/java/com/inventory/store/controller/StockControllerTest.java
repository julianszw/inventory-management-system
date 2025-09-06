package com.inventory.store.controller;

import com.inventory.store.dto.StockSnapshotDTO;
import com.inventory.store.exception.NotFoundException;
import com.inventory.store.service.StockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = { StockController.class })
class StockControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StockService stockService;

    @Test
    void get_snapshot_ok() throws Exception {
        when(stockService.getSnapshot("ABC-001")).thenReturn(StockSnapshotDTO.builder()
                .productId("ABC-001").quantity(10).updatedAt(Instant.now()).build());

        mockMvc.perform(get("/stock/ABC-001"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.productId").value("ABC-001"))
                .andExpect(jsonPath("$.quantity").value(10));
    }

    @Test
    void get_snapshot_not_found() throws Exception {
        when(stockService.getSnapshot("NOPE-999")).thenThrow(new NotFoundException("not found"));

        mockMvc.perform(get("/stock/NOPE-999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/stock/NOPE-999"));
    }
}


