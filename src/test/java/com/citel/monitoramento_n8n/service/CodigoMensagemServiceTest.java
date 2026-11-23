package com.citel.monitoramento_n8n.service;

import com.citel.monitoramento_n8n.DTO.PedidoDTO;
import com.citel.monitoramento_n8n.DTO.ProdutoDTO;
import com.citel.monitoramento_n8n.DTO.ProdutoLoteDTO;
import com.citel.monitoramento_n8n.exception.NotFoundException;
import com.citel.monitoramento_n8n.model.Pedido;
import com.citel.monitoramento_n8n.model.Produto;
import com.citel.monitoramento_n8n.repository.PedidosRepository;
import com.citel.monitoramento_n8n.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * PRO_CODMSG / PEN_CODMSG: o tipo do erro (responsabilidade 2/3) acompanha a mensagem nos POSTs, e o
 * PATCH .../tipo-erro liga o tipo sem contar tentativa.
 */
@ExtendWith(MockitoExtension.class)
class CodigoMensagemServiceTest {

    private static final String ROTINA = "MASTER_Climba_Produto_Item";

    @Mock
    ProdutoRepository produtoRepository;
    @Mock
    PedidosRepository pedidosRepository;

    ProdutoService produtoService;
    PedidoService pedidoService;

    @BeforeEach
    void preparar() {
        produtoService = new ProdutoService(produtoRepository);
        pedidoService = new PedidoService(pedidosRepository);
        lenient().when(produtoRepository.save(any(Produto.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(pedidosRepository.save(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static ProdutoDTO post(String mensagem, Integer codigoMensagem) {
        return new ProdutoDTO(null, "11775", mensagem, null, "DV AUTO PECAS", "CLIMBA", 0,
                "0000007", ROTINA, codigoMensagem);
    }

    private Produto existente(Integer codigoMensagem) {
        Produto p = new Produto();
        p.setCodigoProduto("11775");
        p.setCliente("DV AUTO PECAS");
        p.setRotina(ROTINA);
        p.setErro("Produto sem categoria no ERP.");
        p.setTentativa(2);
        p.setCodigoMensagem(codigoMensagem);
        when(produtoRepository.findByCodigoProdutoAndClienteAndRotina("11775", "DV AUTO PECAS", ROTINA))
                .thenReturn(List.of(p));
        return p;
    }

    @Test
    void produtoNovoGravaOTipo() {
        when(produtoRepository.findByCodigoProdutoAndClienteAndRotina(anyString(), anyString(), anyString()))
                .thenReturn(List.of());

        Produto salvo = produtoService.registrarProduto(post("Produto sem categoria no ERP.", 7));

        assertThat(salvo.getCodigoMensagem()).isEqualTo(7);
    }

    @Test
    void produtoNovoSemTipoFicaNulo() {
        when(produtoRepository.findByCodigoProdutoAndClienteAndRotina(anyString(), anyString(), anyString()))
                .thenReturn(List.of());

        assertThat(produtoService.registrarProduto(post("x", null)).getCodigoMensagem()).isNull();
    }

    @Test
    void mensagemNovaSemTipoLimpaOTipoAntigo() {
        existente(7);

        Produto salvo = produtoService.registrarProduto(post("ERP respondeu 500.", null));

        assertThat(salvo.getCodigoMensagem()).isNull();
        assertThat(salvo.getTentativa()).isEqualTo(3);
    }

    @Test
    void mensagemNovaComTipoTrocaOTipo() {
        existente(7);

        assertThat(produtoService.registrarProduto(post("ERP respondeu 500.", 9)).getCodigoMensagem()).isEqualTo(9);
    }

    @Test
    void mensagemVaziaMantemOTipoGravado() {
        existente(7);

        Produto salvo = produtoService.registrarProduto(post("", null));

        assertThat(salvo.getCodigoMensagem()).isEqualTo(7);
        assertThat(salvo.getMensagemErro()).isEqualTo("Produto sem categoria no ERP.");
    }

    @Test
    void mensagemVaziaComTipoSoTrocaOTipo() {
        existente(7);

        assertThat(produtoService.registrarProduto(post(" ", 9)).getCodigoMensagem()).isEqualTo(9);
    }

    @Test
    void loteGravaOTipoDoItem() {
        when(produtoRepository.findByClienteInAndCodigoProdutoIn(anyList(), anyList())).thenReturn(List.of());
        when(produtoRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));
        ProdutoLoteDTO item = new ProdutoLoteDTO();
        item.setCodigoProduto("11775");
        item.setCliente("DV AUTO PECAS");
        item.setRotina(ROTINA);
        item.setMensagemErro("Produto sem marca.");
        item.setCodigoMensagem(4);

        List<Produto> salvos = produtoService.registrarProdutosList(List.of(item));

        assertThat(salvos).singleElement().extracting(Produto::getCodigoMensagem).isEqualTo(4);
    }

    @Test
    void patchDoProdutoLigaOTipoSemContarTentativa() {
        Produto p = new Produto();
        p.setTentativa(2);
        p.setErro("Produto sem categoria no ERP.");
        when(produtoRepository.findById(p.getId())).thenReturn(Optional.of(p));

        Produto salvo = produtoService.definirTipoErro(p.getId(), 7);

        assertThat(salvo.getCodigoMensagem()).isEqualTo(7);
        assertThat(salvo.getTentativa()).isEqualTo(2);
        assertThat(salvo.getMensagemErro()).isEqualTo("Produto sem categoria no ERP.");
    }

    @Test
    void patchDeProdutoInexistenteVira404() {
        when(produtoRepository.findById("x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> produtoService.definirTipoErro("x", 7)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void pedidoGravaOTipoQueVeioNoPost() {
        Pedido gravado = new Pedido();
        gravado.setCodigoMensagem(5);
        when(pedidosRepository.findByCodigoPedidoAndCliente(anyString(), anyString())).thenReturn(List.of(gravado));
        PedidoDTO dto = new PedidoDTO();
        dto.setCodigoPedido("CLM_001_001_1");
        dto.setCliente("DV AUTO PECAS");
        dto.setErro("Pedido sem rua no endereço.");

        assertThat(pedidoService.registrarPedido(dto).getCodigoMensagem()).isNull();

        dto.setCodigoMensagem(8);
        assertThat(pedidoService.registrarPedido(dto).getCodigoMensagem()).isEqualTo(8);
    }

    @Test
    void patchDoPedidoNaoMexeNoSequencialNemNoStatus() {
        Pedido pedido = new Pedido();
        pedido.setSequencialProcessamento(2);
        pedido.setStatus(2);
        when(pedidosRepository.findById(pedido.getId())).thenReturn(Optional.of(pedido));

        Pedido salvo = pedidoService.definirTipoErro(pedido.getId(), 8);

        assertThat(salvo.getCodigoMensagem()).isEqualTo(8);
        assertThat(salvo.getSequencialProcessamento()).isEqualTo(2);
        assertThat(salvo.getStatus()).isEqualTo(2);
    }

    @Test
    void patchDePedidoInexistenteVira404() {
        when(pedidosRepository.findById("x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoService.definirTipoErro("x", 8)).isInstanceOf(NotFoundException.class);
    }
}
