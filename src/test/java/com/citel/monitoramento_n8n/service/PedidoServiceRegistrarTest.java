package com.citel.monitoramento_n8n.service;

import com.citel.monitoramento_n8n.DTO.PedidoDTO;
import com.citel.monitoramento_n8n.DTO.PedidoLoteDTO;
import com.citel.monitoramento_n8n.model.Pedido;
import com.citel.monitoramento_n8n.repository.PedidosRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/** POST /pendentes: pedido novo nasce com sequencial 1; cada regravação soma 1. */
@ExtendWith(MockitoExtension.class)
class PedidoServiceRegistrarTest {

    @Mock
    PedidosRepository repository;

    PedidoService service;

    @BeforeEach
    void preparar() {
        service = new PedidoService(repository);
        lenient().when(repository.save(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));
    }

    private PedidoDTO dto() {
        PedidoDTO dto = new PedidoDTO();
        dto.setCodigoPedido("TRY_001_001_636936");
        dto.setCliente("SERVTEC");
        dto.setErro("Produto não encontrado para o ID: 56548, pai: null");
        dto.setPlataforma("TRAY");
        return dto;
    }

    @Test
    void pedidoNovoComecaNoSequencialUm() {
        when(repository.findByCodigoPedidoAndCliente(anyString(), anyString())).thenReturn(List.of());

        Pedido salvo = service.registrarPedido(dto());

        assertThat(salvo.getSequencialProcessamento()).isEqualTo(1);
    }

    @Test
    void pedidoExistenteIncrementaOSequencialACadaGravacao() {
        Pedido existente = new Pedido();
        existente.setSequencialProcessamento(70);
        when(repository.findByCodigoPedidoAndCliente(anyString(), anyString())).thenReturn(List.of(existente));

        Pedido salvo = service.registrarPedido(dto());

        assertThat(salvo.getSequencialProcessamento()).isEqualTo(71);
    }

    @Test
    void loteNaoMexeNoSequencial() {
        Pedido existente = new Pedido();
        existente.setCliente("SERVTEC");
        existente.setCodigoPedido("EXISTENTE");
        existente.setSequencialProcessamento(5);
        when(repository.findByClienteInAndCodigoPedidoIn(any(), any())).thenReturn(List.of(existente));
        when(repository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

        PedidoLoteDTO dtoExistente = new PedidoLoteDTO();
        dtoExistente.setCliente("SERVTEC");
        dtoExistente.setCodigoPedido("EXISTENTE");
        PedidoLoteDTO dtoNovo = new PedidoLoteDTO();
        dtoNovo.setCliente("SERVTEC");
        dtoNovo.setCodigoPedido("NOVO");

        List<Pedido> salvos = service.registrarPedidosList(List.of(dtoExistente, dtoNovo));

        assertThat(salvos).extracting(Pedido::getSequencialProcessamento).containsExactly(5, 1);
    }
}
