package com.dcriar.orderintegration.domain.model;

import com.dcriar.orderintegration.domain.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidade de domínio que representa os canais de marketplace suportados pela aplicação
 * (ex: SHOPEE, TIKTOK, MERCADO_LIVRE, AMAZON).
 * <p>
 * Permite o cadastro e gerenciamento dinâmico de novas plataformas sem necessidade
 * de alteração de código ou migrations adicionais.
 */
@Entity
@Table(name = "marketplace_channels")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketplaceChannel extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    public void ativar() {
        this.active = true;
    }

    public void desativar() {
        this.active = false;
    }

    public void atualizar(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("O nome do canal de marketplace não pode ser vazio.");
        }
        this.name = name;
    }
}
