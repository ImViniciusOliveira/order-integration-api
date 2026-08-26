package com.dcriar.orderintegration.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    public record OrderRequest(
            String orderId,
            String customerName,
            BigDecimal totalAmount
    ) {}

    private static final List<OrderRequest> ORDERS_IN_MEMORY = Collections.synchronizedList(new ArrayList<>());

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody OrderRequest request) {
        ORDERS_IN_MEMORY.add(request);

        log.info("Novo pedido recebido: {}", request);
        log.info("Total de pedidos acumulados em memória ({}): {}", ORDERS_IN_MEMORY.size(), ORDERS_IN_MEMORY);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Pedido recebido com sucesso!",
                "totalOrdersInMemory", ORDERS_IN_MEMORY.size()
        ));
    }
}
