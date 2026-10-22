package com.citel.monitoramento_n8n.service;

import com.citel.monitoramento_n8n.DTO.ProdutoLoteDTO;
import com.citel.monitoramento_n8n.exception.BusinessException;
import com.citel.monitoramento_n8n.model.Produto;
import com.citel.monitoramento_n8n.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * POST /produtos/lote: vários produtos numa chamada, com upsert pela chave
 * cliente + codigoProduto + rotina. Item com status é do monitor e só troca o status.
 */
@ExtendWith(MockitoExtension.class)
class ProdutoServiceLoteTest {

    private static final String ROTINA = "MASTER_Tray_Produto_Item";

    @Mock
    ProdutoRepository repository;

    ProdutoService service;

    @BeforeEach
    void preparar() {
        service = new ProdutoService(repository);
        lenient().when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static ProdutoLoteDTO item(String codigoProduto, String mensagem) {
        ProdutoLoteDTO dto = new ProdutoLoteDTO();
        dto.setCodigoProduto(codigoProduto);
        dto.setCliente("VENTURE");
        dto.setPlataforma("TRAY");
        dto.setMensagemErro(mensagem);
        dto.setRotina(ROTINA);
        dto.setIdIntegracao("0000004");
        return dto;
    }

    /** O que o monitor manda: a chave e o status, nada mais. */
    private static ProdutoLoteDTO status(String codigoProduto, int status) {
        ProdutoLoteDTO dto = new ProdutoLoteDTO();
        dto.setCodigoProduto(codigoProduto);
        dto.setCliente("VENTURE");
        dto.setRotina(ROTINA);
        dto.setStatus(status);
        return dto;
    }

    private static Produto gravado(String codigoProduto) {
        Produto produto = new Produto();
        produto.setCodigoProduto(codigoProduto);
        produto.setCliente("VENTURE");
        produto.setPlataforma("TRAY");
        produto.setErro("erro antigo");
        produto.setRotina(ROTINA);
        produto.setIdIntegracao("0000004");
        produto.setLibera("N");
        produto.setTentativa(4);
        return produto;
    }

    @Test
    void gravaVariosProdutosNovosNumaChamada() {
        List<Produto> salvos = service.registrarProdutosList(
                List.of(item("001", "erro 1"), item("002", "erro 2"), item("003", "erro 3")));

        assertThat(salvos).hasSize(3);
        assertThat(salvos).extracting(Produto::getCodigoProduto).containsExactly("001", "002", "003");
        assertThat(salvos).allSatisfy(p -> {
            assertThat(p.getStatus()).isZero();
            assertThat(p.getTentativa()).isEqualTo(1);
        });
    }

    @Test
    void produtoJaGravadoEAtualizadoNaMesmaLinha() {
        Produto existente = gravado("001");
        when(repository.findByClienteInAndCodigoProdutoIn(List.of("VENTURE"), List.of("001", "002")))
                .thenReturn(List.of(existente));

        List<Produto> salvos = service.registrarProdutosList(
                List.of(item("001", "erro novo"), item("002", "erro 2")));

        assertThat(salvos).hasSize(2);
        assertThat(salvos.get(0)).isSameAs(existente);
        assertThat(existente.getTentativa()).isEqualTo(5);
        assertThat(existente.getMensagemErro()).isEqualTo("erro novo");
        assertThat(salvos.get(1).getTentativa()).isEqualTo(1);
    }

    @Test
    void mesmoProdutoDuasVezesNoLoteViraUmaLinhaSo() {
        List<Produto> salvos = service.registrarProdutosList(
                List.of(item("001", "primeiro"), item("001", "segundo")));

        assertThat(salvos).hasSize(1);
        assertThat(salvos.get(0).getTentativa()).isEqualTo(2);
        assertThat(salvos.get(0).getMensagemErro()).isEqualTo("segundo");
    }

    @Test
    void idIntegracaoEGravadoComoVeioSemConferirNaCadint() {
        // O n8n manda o lote com o login de serviço, que não é dono da integração 0000008.
        ProdutoLoteDTO dto = item("001", "erro");
        dto.setIdIntegracao(" 0000008 ");

        List<Produto> salvos = service.registrarProdutosList(List.of(dto));

        assertThat(salvos.get(0).getIdIntegracao()).isEqualTo("0000008");
    }

    @Test
    void statusSoTrocaOStatusDoProdutoGravado() {
        Produto existente = gravado("001");
        when(repository.findByClienteInAndCodigoProdutoIn(List.of("VENTURE"), List.of("001")))
                .thenReturn(List.of(existente));

        List<Produto> salvos = service.registrarProdutosList(List.of(status("001", 1)));

        assertThat(salvos).containsExactly(existente);
        assertThat(existente.getStatus()).isEqualTo(1);
        // Nada além do status muda, mesmo com os campos ausentes no item.
        assertThat(existente.getMensagemErro()).isEqualTo("erro antigo");
        assertThat(existente.getTentativa()).isEqualTo(4);
        assertThat(existente.getPlataforma()).isEqualTo("TRAY");
        assertThat(existente.getIdIntegracao()).isEqualTo("0000004");
        assertThat(existente.getLibera()).isEqualTo("N");
    }

    @Test
    void statusDeProdutoQueNaoExisteNaoCriaNada() {
        List<Produto> salvos = service.registrarProdutosList(List.of(status("999", 1)));

        assertThat(salvos).isEmpty();
    }

    @Test
    void loteMisturaRegistroDeErroETrocaDeStatus() {
        Produto existente = gravado("001");
        when(repository.findByClienteInAndCodigoProdutoIn(List.of("VENTURE"), List.of("001", "002")))
                .thenReturn(List.of(existente));

        List<Produto> salvos = service.registrarProdutosList(
                List.of(status("001", 1), item("002", "erro 2")));

        assertThat(salvos).hasSize(2);
        assertThat(existente.getStatus()).isEqualTo(1);
        assertThat(existente.getTentativa()).isEqualTo(4);
        assertThat(salvos.get(1).getStatus()).isZero();
        assertThat(salvos.get(1).getMensagemErro()).isEqualTo("erro 2");
    }

    @Test
    void statusForaDeZeroEUmRecusaOLoteInteiro() {
        assertThatThrownBy(() -> service.registrarProdutosList(
                List.of(item("002", "erro 2"), status("001", 2))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("0 ou 1");

        verifyNoInteractions(repository);
    }
}
