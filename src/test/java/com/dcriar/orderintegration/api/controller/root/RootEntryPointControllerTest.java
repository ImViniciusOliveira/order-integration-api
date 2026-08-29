package com.dcriar.orderintegration.api.controller.root;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários para validação do ponto de entrada raiz HATEOAS no RootEntryPointController.
 */
class RootEntryPointControllerTest {

    private RootEntryPointController controller;

    @BeforeEach
    void setUp() {
        controller = new RootEntryPointController();
    }

    @Test
    @DisplayName("GET /api/v1 - Deve retornar links de auto-descoberta HATEOAS com status 200 OK")
    void deveRetornarLinksDeAutoDescobertaNaRaiz() {
        ResponseEntity<RepresentationModel<?>> response = controller.root();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().hasLink("self")).isTrue();
        assertThat(response.getBody().hasLink("channels")).isTrue();
        assertThat(response.getBody().hasLink("orders")).isTrue();
        assertThat(response.getBody().hasLink("webhooks")).isTrue();
    }
}
