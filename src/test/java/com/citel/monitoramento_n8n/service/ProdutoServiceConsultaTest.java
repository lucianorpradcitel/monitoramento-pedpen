package com.citel.monitoramento_n8n.service;

import com.citel.monitoramento_n8n.exception.BusinessException;
import com.citel.monitoramento_n8n.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * GET /produtos?status=: só 0 e 1 chegam à consulta. Outro valor é recusado antes de ir ao banco,
 * para não devolver uma lista vazia que se confunde com "nenhum produto nesse status".
 */
@ExtendWith(MockitoExtension.class)
class ProdutoServiceConsultaTest {

    @Mock
    ProdutoRepository repository;

    ProdutoService service;

    @BeforeEach
    void preparar() {
        service = new ProdutoService(repository);
    }

    @Test
    void statusZeroConsultaOsPendentes() {
        service.retornarProdutos(0, "04464", "LOJA", "0000004", null, null);

        verify(repository).buscar(0, "04464", "LOJA", "0000004", null, null);
    }

    @Test
    void statusUmChegaAConsulta() {
        service.retornarProdutos(1, null, null, null, 2, 6);

        verify(repository).buscar(1, null, null, null, 2, 6);
    }

    @Test
    void statusForaDeZeroEUmERecusadoSemConsultar() {
        assertThatThrownBy(() -> service.retornarProdutos(2, null, null, null, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("0 ou 1");

        verifyNoInteractions(repository);
    }
}
