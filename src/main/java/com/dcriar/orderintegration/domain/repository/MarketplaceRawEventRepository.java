package com.dcriar.orderintegration.domain.repository;

import com.dcriar.orderintegration.domain.model.MarketplaceRawEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório Spring Data JPA para o Event Store imutável de webhooks e eventos brutos de marketplaces.
 */
@Repository
public interface MarketplaceRawEventRepository extends JpaRepository<MarketplaceRawEvent, Long> {

    /**
     * Recupera o histórico cronológico de todos os eventos brutos registrados para um pedido específico,
     * ordenados da data mais recente para a mais antiga.
     *
     * @param platform a plataforma de origem (ex: "SHOPEE", "TIKTOK")
     * @param orderSn  o identificador único do pedido na plataforma
     * @return lista de eventos brutos ordenados descendentemente por data de criação
     */
    List<MarketplaceRawEvent> findByPlatformAndOrderSnOrderByCreatedAtDesc(String platform, String orderSn);
}
