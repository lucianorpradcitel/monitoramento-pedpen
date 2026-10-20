package com.citel.monitoramento_n8n.service;

import com.citel.monitoramento_n8n.exception.ConflictException;
import com.citel.monitoramento_n8n.exception.ForbiddenException;
import com.citel.monitoramento_n8n.exception.NotFoundException;
import com.citel.monitoramento_n8n.model.Cliente;
import com.citel.monitoramento_n8n.model.Pedido;
import com.citel.monitoramento_n8n.model.Usuario;
import com.citel.monitoramento_n8n.repository.PedidosRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Reprocessar volta o pedido com erro (2) para a fila (0), e só ele. O usuário interno reprocessa qualquer
 * pedido; o lojista, só os das integrações dele.
 */
@ExtendWith(MockitoExtension.class)
class PedidoServiceReprocessarTest {

    @Mock
    PedidosRepository repository;
    @Mock
    IntegracaoService integracaoService;

    PedidoService service;
    Usuario admin;

    @BeforeEach
    void preparar() {
        service = new PedidoService(repository, integracaoService);
        admin = new Usuario();
        admin.setEmail("gabriel@citelsoftware.com.br");
        admin.setNome("Gabriel Beraldo");
    }

    private Pedido pedidoComStatus(int status) {
        Pedido pedido = new Pedido();
        pedido.setCodigoPedido("1234");
        pedido.setCliente("TODO TETO");
        pedido.setPlataforma("tray");
        pedido.setErro("Produto sem estoque");
        pedido.setStatus(status);
        pedido.setSequencialProcessamento(3);
        pedido.setUltimaAlteracao(LocalDateTime.of(2026, 1, 1, 0, 0));
        when(repository.findById(pedido.getId())).thenReturn(Optional.of(pedido));
        return pedido;
    }

    @Test
    void pedidoComErroVoltaParaAFila() {
        Pedido pedido = pedidoComStatus(2);
        when(repository.save(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));

        Pedido salvo = service.reprocessar(pedido.getId(), admin);

        assertThat(salvo.getStatus()).isZero();
        assertThat(salvo.getUltimaAlteracao()).isAfter(LocalDateTime.of(2026, 1, 1, 0, 0));
        verify(repository).save(pedido);
    }

    @Test
    void aMensagemDeErroEOSequencialNaoMudam() {
        Pedido pedido = pedidoComStatus(2);
        when(repository.save(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));

        service.reprocessar(pedido.getId(), admin);

        assertThat(pedido.getErro()).isEqualTo("Produto sem estoque");
        assertThat(pedido.getSequencialProcessamento()).isEqualTo(3);
        assertThat(pedido.getCodigoPedido()).isEqualTo("1234");
    }

    @Test
    void pedidoQueNaoEstaComErroEhRecusadoESemGravar() {
        for (int status : new int[]{0, 1, 3}) {
            Pedido pedido = pedidoComStatus(status);

            assertThatThrownBy(() -> service.reprocessar(pedido.getId(), admin))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("status atual deste pedido é " + status);
        }

        verify(repository, never()).save(any());
    }

    @Test
    void lojistaReprocessaPedidoDaPropriaIntegracao() {
        Pedido pedido = pedidoComStatus(2);
        pedido.setIdIntegracao("INT-1");
        Cliente lojista = new Cliente("TODO TETO", "todoteto", "x");
        when(integracaoService.pertenceAoLojista(eq("INT-1"), any())).thenReturn(true);
        when(repository.save(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));

        Pedido salvo = service.reprocessar(pedido.getId(), lojista);

        assertThat(salvo.getStatus()).isZero();
        verify(repository).save(pedido);
    }

    @Test
    void lojistaNaoReprocessaPedidoDeOutraIntegracao() {
        Pedido pedido = pedidoComStatus(2);
        pedido.setIdIntegracao("INT-DE-OUTRO");
        Cliente lojista = new Cliente("TODO TETO", "todoteto", "x");
        when(integracaoService.pertenceAoLojista(eq("INT-DE-OUTRO"), any())).thenReturn(false);

        assertThatThrownBy(() -> service.reprocessar(pedido.getId(), lojista))
                .isInstanceOf(ForbiddenException.class);

        assertThat(pedido.getStatus()).isEqualTo(2);
        verify(repository, never()).save(any());
    }

    @Test
    void lojistaNaoReprocessaPedidoSemIntegracao() {
        Pedido pedido = pedidoComStatus(2);
        Cliente lojista = new Cliente("TODO TETO", "todoteto", "x");
        when(integracaoService.pertenceAoLojista(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.reprocessar(pedido.getId(), lojista))
                .isInstanceOf(ForbiddenException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void usuarioInternoNaoPrecisaSerDonoDaIntegracao() {
        Pedido pedido = pedidoComStatus(2);
        pedido.setIdIntegracao("QUALQUER");
        when(repository.save(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));

        service.reprocessar(pedido.getId(), admin);

        verify(integracaoService, never()).pertenceAoLojista(any(), any());
        verify(repository).save(pedido);
    }

    @Test
    void pedidoInexistenteDaNotFound() {
        when(repository.findById("nao-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reprocessar("nao-existe", admin))
                .isInstanceOf(NotFoundException.class);

        verify(repository, never()).save(any());
    }
}
