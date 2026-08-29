package com.dcriar.orderintegration.api.controller.order;

import com.dcriar.orderintegration.api.dto.response.OrderMasterResponse;
import com.dcriar.orderintegration.api.hateoas.OrderMasterModelAssembler;
import com.dcriar.orderintegration.config.OrderIntegrationProperties;
import com.dcriar.orderintegration.domain.order.entity.OrderMaster;
import com.dcriar.orderintegration.domain.order.service.MarketplaceIngestionService;
import com.dcriar.orderintegration.exception.custom.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador REST para recepção, validação de autenticação interna e ingestão segura de webhooks de marketplaces.
 */
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
    @PostMapping("/{platform}")
    public ResponseEntity<EntityModel<OrderMasterResponse>> ingestWebhook(
            @PathVariable String platform,
            @RequestHeader(value = "X-Internal-API-Key", required = false) String apiKey,
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
