package com.dcriar.orderintegration.domain.marketplace.common.calculator.mapper;

import com.dcriar.orderintegration.domain.marketplace.common.calculator.model.FeeCalculationItem;
import com.dcriar.orderintegration.domain.marketplace.common.calculator.model.FeeCalculationResult;
import com.dcriar.orderintegration.domain.marketplace.shopee.calculator.model.ShopeeFeeCalculationDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários para o {@link FeeCalculationMapper}.
 */
class FeeCalculationMapperTest {

    private FeeCalculationMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new FeeCalculationMapper();
    }

    @Test
    @DisplayName("Deve retornar mapa vazio quando o resultado for nulo")
    void shouldReturnEmptyMapWhenResultIsNull() {
        Map<String, Object> map = mapper.toMap(null);
        assertThat(map).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Deve converter FeeCalculationResult em mapa com nós estruturados para persistência em JSONB")
    void shouldConvertFeeCalculationResultToMap() {
        FeeCalculationItem item = new FeeCalculationItem(
                123L,
                "Produto Teste",
                "Azul",
                new BigDecimal("50.00"),
                1,
                new BigDecimal("50.00"),
                false,
                BigDecimal.ZERO
        );

        FeeCalculationResult result = new FeeCalculationResult(
                "SHOPEE_CPF_BR_2026",
                OffsetDateTime.now(),
                false,
                new BigDecimal("0.05"),
                new BigDecimal("50.00"),
                1,
                new BigDecimal("7.00"),
                new BigDecimal("3.00"),
                new BigDecimal("4.00"),
                BigDecimal.ZERO,
                new BigDecimal("14.00"),
                BigDecimal.ZERO,
                new BigDecimal("36.00"),
                new BigDecimal("36.00"),
                BigDecimal.ZERO,
                List.of(item),
                new ShopeeFeeCalculationDetails(
                        new BigDecimal("7.00"),
                        new BigDecimal("3.00"),
                        new BigDecimal("4.00"),
                        BigDecimal.ZERO
                ),
                null
        );

        Map<String, Object> map = mapper.toMap(result);

        assertThat(map).containsKey("versao_regra");
        assertThat(map.get("versao_regra")).isEqualTo("SHOPEE_CPF_BR_2026");
        assertThat(map.get("has_divergence")).isEqualTo(false);
        assertThat(map.get("motivo_divergencia")).isNull();

        @SuppressWarnings("unchecked")
        Map<String, Object> resumo = (Map<String, Object>) map.get("resumo_financeiro");
        assertThat(resumo).isNotNull();
        assertThat(resumo.get("subtotal_itens")).isEqualTo(new BigDecimal("50.00"));
        assertThat(resumo.get("total_taxas_marketplace")).isEqualTo(new BigDecimal("14.00"));
        assertThat(resumo.get("repasse_liquido_teorico")).isEqualTo(new BigDecimal("36.00"));
        assertThat(resumo.get("repasse_liquido_real")).isEqualTo(new BigDecimal("36.00"));

        @SuppressWarnings("unchecked")
        Map<String, Object> detalhes = (Map<String, Object>) map.get("detalhes_plataforma");
        assertThat(detalhes.get("comissao_base_14")).isEqualTo(new BigDecimal("7.00"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> itens = (List<Map<String, Object>>) map.get("itens_auditados");
        assertThat(itens).hasSize(1);
        assertThat(itens.getFirst().get("item_id")).isEqualTo(123L);
        assertThat(itens.getFirst().get("item_name")).isEqualTo("Produto Teste");
    }
}
