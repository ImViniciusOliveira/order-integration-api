package com.dcriar.orderintegration.domain.channel.entity;

import com.dcriar.orderintegration.domain.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidade que representa um canal de venda (Marketplace) dinâmico cadastrado no sistema.
 * <p>
 * Permite cadastrar e habilitar novos marketplaces sem a necessidade de alterar enums
 * ou reconstruir a base de código.
 */
@Entity
@Table(
    name = "marketplace_channels",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_marketplace_channels_code", columnNames = {"code"})
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketplaceChannel extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    /**
     * Ativa o canal de marketplace para recebimento de pedidos.
     */
    public void ativar() {
        this.active = true;
    }

    /**
     * Desativa o canal de marketplace, suspendendo a ingestão de pedidos.
     */
    public void desativar() {
        this.active = false;
    }

    /**
     * Atualiza o nome legível do canal de marketplace.
     *
     * @param nome novo nome de exibição
     */
    public void atualizar(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome do canal de marketplace não pode ser vazio.");
        }
        this.name = nome;
    }
}
