package com.dcriar.orderintegration.domain.order.repository;

import com.dcriar.orderintegration.domain.order.entity.MarketplaceRawEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositório Spring Data JPA para persistência e consulta imutável do Event Store de eventos brutos de marketplaces.
 */
@Repository
public interface MarketplaceRawEventRepository extends JpaRepository<MarketplaceRawEvent, Long> {

    /**
     * Recupera o histórico cronológico de todos os eventos recebidos para um determinado pedido em uma plataforma.
     *
     * @param platform a plataforma de origem (ex: "SHOPEE")
     * @param orderSn  o número do pedido
     * @return lista de eventos brutos ordenados do mais recente para o mais antigo
     */
    List<MarketplaceRawEvent> findByPlatformAndOrderSnOrderByCreatedAtDesc(String platform, String orderSn);
}
