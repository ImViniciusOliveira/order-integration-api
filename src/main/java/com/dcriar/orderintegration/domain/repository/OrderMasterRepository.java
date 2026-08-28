package com.dcriar.orderintegration.domain.repository;

import com.dcriar.orderintegration.domain.model.OrderMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositório Spring Data JPA para a entidade mestre unificada de pedidos {@link OrderMaster}.
 * <p>
 * Fornece operações de persistência relacional e suporte a consultas dinâmicas paginadas
 * através de {@link JpaSpecificationExecutor}.
 */
@Repository
public interface OrderMasterRepository extends JpaRepository<OrderMaster, Long>, JpaSpecificationExecutor<OrderMaster> {

    /**
     * Localiza um pedido específico através da sua chave única composta (plataforma e número do pedido).
     *
     * @param platform a plataforma de origem (ex: "SHOPEE", "TIKTOK")
     * @param orderSn  o número único do pedido no marketplace
     * @return um {@link Optional} contendo o pedido correspondente caso encontrado
     */
    Optional<OrderMaster> findByPlatformAndOrderSn(String platform, String orderSn);
}
