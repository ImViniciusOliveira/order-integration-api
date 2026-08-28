package com.dcriar.orderintegration.domain.order.repository;

import com.dcriar.orderintegration.domain.order.entity.OrderMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositório Spring Data JPA para a entidade mestre de pedidos {@link OrderMaster}.
 * Suporta consultas avançadas e filtros dinâmicos através de {@link JpaSpecificationExecutor}.
 */
@Repository
public interface OrderMasterRepository extends JpaRepository<OrderMaster, Long>, JpaSpecificationExecutor<OrderMaster> {

    /**
     * Localiza um pedido mestre pela combinação única de plataforma e identificador do pedido no marketplace.
     *
     * @param platform o código da plataforma (ex: "SHOPEE")
     * @param orderSn  o identificador único do pedido no marketplace
     * @return um {@link Optional} contendo o pedido caso exista
     */
    Optional<OrderMaster> findByPlatformAndOrderSn(String platform, String orderSn);

    /**
     * Verifica a existência de um pedido pela plataforma e número do pedido.
     *
     * @param platform o código da plataforma
     * @param orderSn  o número do pedido
     * @return {@code true} se existir, {@code false} caso contrário
     */
    boolean existsByPlatformAndOrderSn(String platform, String orderSn);
}
