package com.dcriar.orderintegration.api.controller.root;

import com.dcriar.orderintegration.api.controller.channel.MarketplaceChannelController;
import com.dcriar.orderintegration.api.controller.order.MarketplaceWebhookController;
import com.dcriar.orderintegration.api.controller.order.OrderMasterController;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Ponto de entrada raiz da API (Root Entry Point) para descoberta dinâmica de endpoints e recursos HATEOAS.
 */
@RestController
@RequestMapping("/api/v1")
public class RootEntryPointController {

    /**
     * Fornece os links principais de navegação para os recursos do ecossistema de integração.
     *
     * @return modelo HATEOAS contendo os links de navegação da raiz
     */
    @GetMapping
    public ResponseEntity<RepresentationModel<?>> root() {
        RepresentationModel<?> model = new RepresentationModel<>();

        model.add(linkTo(methodOn(RootEntryPointController.class).root()).withSelfRel());
        model.add(linkTo(methodOn(MarketplaceChannelController.class).listAll()).withRel("channels"));
        model.add(linkTo(methodOn(OrderMasterController.class).searchOrders(null, null)).withRel("orders"));
        model.add(linkTo(MarketplaceWebhookController.class).slash("{platform}").withRel("webhooks"));

        return ResponseEntity.ok(model);
    }
}
