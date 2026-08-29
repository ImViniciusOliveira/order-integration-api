package com.dcriar.orderintegration.api.controller.order;

import com.dcriar.orderintegration.api.dto.response.OrderMasterResponse;
import com.dcriar.orderintegration.api.hateoas.OrderMasterModelAssembler;
import com.dcriar.orderintegration.config.OrderIntegrationProperties;
import com.dcriar.orderintegration.domain.order.entity.OrderMaster;
import com.dcriar.orderintegration.domain.order.service.MarketplaceIngestionService;
import com.dcriar.orderintegration.exception.custom.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador REST para recepção, validação de autenticação interna e ingestão segura de webhooks de marketplaces.
 */
@Tag(name = "Webhooks", description = "Ingestão resiliente e autenticada de eventos e webhooks de marketplaces")
@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class MarketplaceWebhookController {

    private final MarketplaceIngestionService ingestionService;
    private final OrderMasterModelAssembler orderMasterModelAssembler;
    private final OrderIntegrationProperties properties;

    /**
     * Ponto de entrada para ingestão de eventos/webhooks de plataformas de marketplace.
     *
     * @param platform nome da plataforma de origem (ex: "SHOPEE")
     * @param apiKey   chave de segurança interna informada no cabeçalho HTTP
     * @param shopId   identificador opcional da loja informado no cabeçalho HTTP
     * @param payload  corpo JSON estruturado do webhook
     * @return representação HATEOAS do pedido ingerido e atualizado
     */
    @Operation(summary = "Ingerir e processar webhook de eventos de pedidos de marketplaces")
    @PostMapping("/{platform}")
    public ResponseEntity<EntityModel<OrderMasterResponse>> ingestWebhook(
            @Parameter(description = "Identificador da plataforma de marketplace", example = "SHOPEE")
            @PathVariable String platform,
            @Parameter(description = "Chave de autenticação interna da API", example = "dcr_sec_live_9f83a7c2e1g6B3Ld8e6a1f3ck7bGETe4")
            @RequestHeader(value = "X-Internal-API-Key", required = false) String apiKey,
            @Parameter(description = "Identificador opcional da loja no marketplace", example = "12345678")
            @RequestHeader(value = "X-Shop-Id", required = false) String shopId,
            @RequestBody Map<String, Object> payload) {

        String expectedApiKey = properties.security().internalApiKey();
        if (apiKey == null || !apiKey.equals(expectedApiKey)) {
            log.warn("Tentativa de ingestão de webhook não autorizada para a plataforma: {}", platform);
            throw new BusinessException("Chave de autenticação interna inválida ou ausente.");
        }

        OrderMaster processedOrder = ingestionService.ingestEvent(platform, shopId, payload);
        return ResponseEntity.ok(orderMasterModelAssembler.toModel(processedOrder));
    }
}
