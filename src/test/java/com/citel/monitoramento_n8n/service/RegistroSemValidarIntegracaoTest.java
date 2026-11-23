package com.citel.monitoramento_n8n.service;

import com.citel.monitoramento_n8n.DTO.PedidoDTO;
import com.citel.monitoramento_n8n.DTO.ProdutoDTO;
import com.citel.monitoramento_n8n.model.Pedido;
import com.citel.monitoramento_n8n.model.Produto;
import com.citel.monitoramento_n8n.repository.PedidosRepository;
import com.citel.monitoramento_n8n.repository.ProdutoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * POST /produtos e POST /pendentes gravam o idIntegracao como veio, sem conferir se a integração
 * é do lojista do token: o n8n autentica com o login de serviço da Citel, que não é dono de nenhuma.
 */
@ExtendWith(MockitoExtension.class)
class RegistroSemValidarIntegracaoTest {

    @Mock
    ProdutoRepository produtoRepository;
    @Mock
    PedidosRepository pedidosRepository;

    @Test
    void produtoNovoGravaOIdIntegracaoComoVeio() {
        // GUARU TINTAS com a integração 0000004, postado pelo MASTER_Tray_Preco_Batch_Cliente com o
        // token da Citel: antes respondia 422 "não pertence ao lojista autenticado".
        when(produtoRepository.save(any(Produto.class))).thenAnswer(inv -> inv.getArgument(0));

        Produto salvo = new ProdutoService(produtoRepository).registrarProduto(new ProdutoDTO(
                null, "00018", "Produto 00018: preço zerado ou tabela 5 não encontrada no ERP.", null,
                "GUARU TINTAS", "TRAY", 0, " 0000004 ", "MASTER_Tray_Preco_Batch_Cliente", null));

        assertThat(salvo.getIdIntegracao()).isEqualTo("0000004");
    }

    @Test
    void pedidoNovoGravaOIdIntegracaoComoVeio() {
        when(pedidosRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));
        PedidoDTO dto = new PedidoDTO();
        dto.setCodigoPedido("1234");
        dto.setCliente("GUARU TINTAS");
        dto.setIdIntegracao("0000004");

        Pedido salvo = new PedidoService(pedidosRepository).registrarPedido(dto);

        assertThat(salvo.getIdIntegracao()).isEqualTo("0000004");
    }
}
