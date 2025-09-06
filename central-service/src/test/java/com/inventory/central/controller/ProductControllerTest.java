package com.inventory.central.controller;

import com.inventory.central.entity.ProductEntity;
import com.inventory.central.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import com.example.centralservice.CentralServiceApplication;
import org.springframework.context.annotation.Import;
import com.inventory.central.exception.GlobalExceptionHandler;

@SpringBootTest(classes = CentralServiceApplication.class)
@AutoConfigureMockMvc
@Import(GlobalExceptionHandler.class)
class ProductControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Test
    void get_products_ok() throws Exception {
        when(productService.findAll()).thenReturn(List.of(
                ProductEntity.builder().id("ABC-001").name("Laptop").price(new BigDecimal("1.00")).updatedAt(Instant.now()).build()
        ));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$[0].id").value("ABC-001"));
    }
}


