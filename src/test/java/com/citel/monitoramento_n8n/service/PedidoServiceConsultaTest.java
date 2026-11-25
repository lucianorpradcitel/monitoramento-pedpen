package com.citel.monitoramento_n8n.service;

import com.citel.monitoramento_n8n.exception.BusinessException;
import com.citel.monitoramento_n8n.repository.PedidosRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Filtro por responsabilidade do erro (0 a 4) em GET /pendentes e GET /pedidos. */
@ExtendWith(MockitoExtension.class)
class PedidoServiceConsultaTest {

    @Mock
    PedidosRepository repository;

    PedidoService service;

    @BeforeEach
    void preparar() {
        service = new PedidoService(repository);
    }

    @Test
    void semResponsabilidadeConsultaTodasAs5() {
        when(repository.buscarPorStatus(anyBoolean(), any(), any(), any(), any(), any(), any())).thenReturn(List.of());

        service.retornarPedidos(List.of(2), null, null, null, null, null);

        // Nulo ou vazio = sem filtro: o service manda as 5 responsabilidades (inclui o 0, que é o erro sem tipo).
        verify(repository).buscarPorStatus(eq(true), eq(List.of(2)), isNull(), isNull(), isNull(), isNull(),
                eq(List.of(0, 1, 2, 3, 4)));
    }

    @Test
    void responsabilidadesPedidasChegamAoRepository() {
        when(repository.buscarPorStatus(anyBoolean(), any(), any(), any(), any(), any(), any())).thenReturn(List.of());

        service.retornarPedidos(List.of(2), null, null, null, null, List.of(1, 0));

        verify(repository).buscarPorStatus(eq(true), eq(List.of(2)), isNull(), isNull(), isNull(), isNull(),
                eq(List.of(1, 0)));
    }

    @Test
    void responsabilidadeForaDeZeroAQuatroEhRecusada() {
        assertThatThrownBy(() -> service.retornarPedidos(List.of(2), null, null, null, null, List.of(1, 5)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("0 a 4");

        verifyNoInteractions(repository);
    }
}
