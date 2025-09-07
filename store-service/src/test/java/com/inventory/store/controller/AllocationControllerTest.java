package com.inventory.store.controller;

import com.inventory.store.dto.StockAllocateRequestDTO;
import com.inventory.store.dto.StockAllocationResponseDTO;
import com.inventory.store.service.StockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = { StockController.class })
class AllocationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StockService stockService;

    @Test
    void allocate_ok() throws Exception {
        when(stockService.allocate(nullable(String.class), any(StockAllocateRequestDTO.class)))
                .thenReturn(StockAllocationResponseDTO.builder().status("ALLOCATED").productId("ABC-001").onHand(10).allocated(2).build());

        mockMvc.perform(post("/stock/allocate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\n  \"orderId\": \"o-1\",\n  \"productId\": \"ABC-001\",\n  \"quantity\": 2\n}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.status").value("ALLOCATED"))
                .andExpect(jsonPath("$.productId").value("ABC-001"));
    }

    @Test
    void commit_ok() throws Exception {
        when(stockService.commit(any(StockAllocateRequestDTO.class)))
                .thenReturn(StockAllocationResponseDTO.builder().status("COMMITTED").productId("ABC-001").onHand(9).allocated(1).build());

        mockMvc.perform(post("/stock/commit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\n  \"orderId\": \"o-1\",\n  \"productId\": \"ABC-001\",\n  \"quantity\": 1\n}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.status").value("COMMITTED"));
    }

    @Test
    void release_ok() throws Exception {
        when(stockService.release(any(StockAllocateRequestDTO.class)))
                .thenReturn(StockAllocationResponseDTO.builder().status("RELEASED").productId("ABC-001").onHand(10).allocated(0).build());

        mockMvc.perform(post("/stock/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\n  \"orderId\": \"o-1\",\n  \"productId\": \"ABC-001\",\n  \"quantity\": 1\n}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.status").value("RELEASED"));
    }
}


