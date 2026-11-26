package com.citel.monitoramento_n8n.service;

import com.citel.monitoramento_n8n.DTO.FiltrosProdutosDTO;
import com.citel.monitoramento_n8n.DTO.PaginaDTO;
import com.citel.monitoramento_n8n.exception.BusinessException;
import com.citel.monitoramento_n8n.model.Produto;
import com.citel.monitoramento_n8n.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * GET /produtos: só status 0 e 1, páginas de 50, 100 ou 1000 (ou todos, com 0) a partir da 1 e período coerente chegam à
 * consulta. Valor fora disso é recusado antes de ir ao banco, para não devolver uma lista vazia que
 * se confunde com "nenhum produto".
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

    /** Consulta sem filtros, só mudando a paginação. */
    private PaginaDTO<Produto> consultar(int pagina, int tamanho, String ordem) {
        return service.retornarProdutos(0, null, null, null, null, null, null, null, null, null, null, null, pagina, tamanho, ordem);
    }

    private Pageable paginaPedida() {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).buscar(any(Integer.class), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), captor.capture());
        return captor.getValue();
    }

    @Test
    void filtrosChegamAConsulta() {
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 5, 8, 0);
        LocalDateTime fim = LocalDateTime.of(2026, 11, 5, 18, 30, 59);
        when(repository.buscar(any(Integer.class), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));

        service.retornarProdutos(1, "04464", "LOJA", "0000004", 2, 6, inicio, fim, "Tray", "abc", "peso", null, 1, 50, "desc");

        verify(repository).buscar(eq(1), eq("04464"), eq("LOJA"), eq("0000004"), eq(2), eq(6),
                eq(Timestamp.valueOf(inicio)), eq(Timestamp.valueOf(fim)), eq("Tray"), eq("abc"), eq("peso"), eq(List.of(0, 1, 2, 3, 4)),
                any(Pageable.class));
    }

    @Test
    void pesquisaPorTrechoChegaComOsCuringasDoLikeEscapados() {
        when(repository.buscar(any(Integer.class), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));

        // "_" e "%" são curingas do LIKE: sem escape, "ABC_1" acharia também "ABCX1".
        service.retornarProdutos(0, null, null, null, null, null, null, null,
                "Tray", " ABC_1 ", "50%!", null, 1, 50, "desc");

        verify(repository).buscar(eq(0), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq("Tray"), eq("ABC!_1"), eq("50!%!!"), any(), any(Pageable.class));
    }

    @Test
    void pesquisaEmBrancoNaoFiltra() {
        when(repository.buscar(any(Integer.class), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));

        service.retornarProdutos(0, null, null, null, null, null, null, null, "", "   ", "", null, 1, 50, "desc");

        verify(repository).buscar(eq(0), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), any(), any(Pageable.class));
    }

    @Test
    void paginaComecaEmUmEOrdenaPeloMaisRecenteComDesempatePorId() {
        when(repository.buscar(any(Integer.class), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(2, 100), 0));

        consultar(3, 100, null);

        Pageable pedida = paginaPedida();
        assertThat(pedida.getPageNumber()).isEqualTo(2);
        assertThat(pedida.getPageSize()).isEqualTo(100);
        assertThat(pedida.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "dataErro", "id"));
    }

    @Test
    void ordemAscTrazOMaisAntigoPrimeiro() {
        when(repository.buscar(any(Integer.class), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));

        consultar(1, 50, "ASC");

        assertThat(paginaPedida().getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "dataErro", "id"));
    }

    @Test
    void respostaTrazOsTotaisDeTodosOsRegistrosQueAtendemAosFiltros() {
        List<Produto> daPagina = List.of(new Produto(), new Produto());
        // 8.342 registros em páginas de 50: 167 páginas. O total não depende de quantos vieram.
        when(repository.buscar(any(Integer.class), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(daPagina, PageRequest.of(1, 50), 8342));

        PaginaDTO<Produto> resposta = consultar(2, 50, null);

        assertThat(resposta.itens()).isEqualTo(daPagina);
        assertThat(resposta.pagina()).isEqualTo(2);
        assertThat(resposta.tamanho()).isEqualTo(50);
        assertThat(resposta.totalItens()).isEqualTo(8342);
        assertThat(resposta.totalPaginas()).isEqualTo(167);
    }

    @Test
    void statusForaDeZeroEUmERecusadoSemConsultar() {
        assertThatThrownBy(() -> service.retornarProdutos(2, null, null, null, null, null, null, null, null, null, null, null, 1, 50, "desc"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("0 ou 1");

        verifyNoInteractions(repository);
    }

    @Test
    void tamanhoForaDe50E100ERecusadoSemConsultar() {
        for (int invalido : new int[]{-1, 10, 49, 51, 101, 999, 1001}) {
            assertThatThrownBy(() -> consultar(1, invalido, "desc"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("50, 100, 1000");
        }

        verifyNoInteractions(repository);
    }

    @Test
    void tamanhoMilEAceito() {
        when(repository.buscar(any(Integer.class), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 1000), 0));

        consultar(2, 1000, null);

        Pageable pedida = paginaPedida();
        assertThat(pedida.getPageNumber()).isEqualTo(1);
        assertThat(pedida.getPageSize()).isEqualTo(1000);
    }

    @Test
    void tamanhoZeroTrazTodosNumaPaginaSoComOsTotais() {
        List<Produto> todos = List.of(new Produto(), new Produto(), new Produto());
        when(repository.buscar(any(Integer.class), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(todos, PageRequest.of(0, Integer.MAX_VALUE), 3));

        // A página pedida é ignorada: tudo vem junto, na página 1.
        PaginaDTO<Produto> resposta = consultar(4, 0, "asc");

        Pageable pedida = paginaPedida();
        assertThat(pedida.getPageNumber()).isZero();
        assertThat(pedida.getPageSize()).isEqualTo(Integer.MAX_VALUE);
        assertThat(pedida.getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "dataErro", "id"));
        assertThat(resposta.itens()).hasSize(3);
        assertThat(resposta.pagina()).isEqualTo(1);
        assertThat(resposta.tamanho()).isZero();
        assertThat(resposta.totalItens()).isEqualTo(3);
        assertThat(resposta.totalPaginas()).isEqualTo(1);
    }

    @Test
    void filtrosTrazClientesEPlataformasDoStatusPedido() {
        when(repository.clientesComProduto(1, null, List.of(0, 1, 2, 3, 4))).thenReturn(List.of("LOJA", "OUTRA"));
        when(repository.plataformasComProduto(1, null, List.of(0, 1, 2, 3, 4))).thenReturn(List.of("Tray"));

        FiltrosProdutosDTO filtros = service.retornarFiltros(1, null, null, null);

        assertThat(filtros.clientes()).containsExactly("LOJA", "OUTRA");
        assertThat(filtros.plataformas()).containsExactly("Tray");
    }

    @Test
    void filtrosSeRestringemUmAoOutroEIgnoramOProprioFiltro() {
        when(repository.clientesComProduto(0, "Tray", List.of(0, 1, 2, 3, 4))).thenReturn(List.of("LOJA"));
        when(repository.plataformasComProduto(0, "OUTRA", List.of(0, 1, 2, 3, 4))).thenReturn(List.of("VTEX"));

        // A plataforma escolhida limita os clientes; o cliente escolhido limita as plataformas.
        FiltrosProdutosDTO filtros = service.retornarFiltros(0, "OUTRA", "Tray", null);

        assertThat(filtros.clientes()).containsExactly("LOJA");
        assertThat(filtros.plataformas()).containsExactly("VTEX");
    }

    @Test
    void filtrosEmBrancoNaoRestringem() {
        when(repository.clientesComProduto(0, null, List.of(0, 1, 2, 3, 4))).thenReturn(List.of("LOJA"));
        when(repository.plataformasComProduto(0, null, List.of(0, 1, 2, 3, 4))).thenReturn(List.of("Tray"));

        FiltrosProdutosDTO filtros = service.retornarFiltros(0, "  ", "", null);

        assertThat(filtros.clientes()).containsExactly("LOJA");
        assertThat(filtros.plataformas()).containsExactly("Tray");
    }

    @Test
    void filtrosComStatusInvalidoERecusadoSemConsultar() {
        assertThatThrownBy(() -> service.retornarFiltros(2, null, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("0 ou 1");

        verifyNoInteractions(repository);
    }

    @Test
    void paginaMenorQueUmERecusadaSemConsultar() {
        assertThatThrownBy(() -> consultar(0, 50, "desc"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("pagina");
        assertThatThrownBy(() -> consultar(-1, 50, "desc"))
                .isInstanceOf(BusinessException.class);

        verifyNoInteractions(repository);
    }

    @Test
    void ordemDiferenteDeAscEDescERecusadaSemConsultar() {
        assertThatThrownBy(() -> consultar(1, 50, "recente"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("asc ou desc");

        verifyNoInteractions(repository);
    }

    @Test
    void dataInicialDepoisDeDataFinalERecusadaSemConsultar() {
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 5, 18, 0);
        LocalDateTime fim = LocalDateTime.of(2026, 11, 5, 8, 0);

        assertThatThrownBy(() -> service.retornarProdutos(0, null, null, null, null, null, inicio, fim, null, null, null, null, 1, 50, "desc"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("dataInicial");

        verifyNoInteractions(repository);
    }

    @Test
    void faixaDeTentativasImpossivelERecusadaSemConsultar() {
        assertThatThrownBy(() -> service.retornarProdutos(0, null, null, null, 5, 5, null, null, null, null, null, null, 1, 50, "desc"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("tentativaMenorQue");

        verifyNoInteractions(repository);
    }

    @Test
    void semResponsabilidadeConsultaTodasAs5() {
        when(repository.buscar(any(Integer.class), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));

        service.retornarProdutos(0, null, null, null, null, null, null, null, null, null, null, List.of(), 1, 50, "desc");

        // Lista vazia ou nula = sem filtro: o service manda as 5 responsabilidades (inclui o 0, que e o erro sem tipo).
        verify(repository).buscar(eq(0), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), eq(List.of(0, 1, 2, 3, 4)), any(Pageable.class));
    }

    @Test
    void responsabilidadesPedidasChegamAoRepository() {
        when(repository.buscar(any(Integer.class), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));

        service.retornarProdutos(0, null, null, null, null, null, null, null, null, null, null, List.of(1, 0), 1, 50, "desc");

        verify(repository).buscar(eq(0), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), eq(List.of(1, 0)), any(Pageable.class));
    }

    @Test
    void responsabilidadeForaDeZeroAQuatroEhRecusada() {
        assertThatThrownBy(() -> service.retornarProdutos(0, null, null, null, null, null, null, null, null, null, null, List.of(1, 5), 1, 50, "desc"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("0 a 4");

        verifyNoInteractions(repository);
    }

    @Test
    void filtrosRespeitamAResponsabilidadePedida() {
        when(repository.clientesComProduto(0, null, List.of(2, 4))).thenReturn(List.of("LOJA"));
        when(repository.plataformasComProduto(0, null, List.of(2, 4))).thenReturn(List.of("Climba"));

        FiltrosProdutosDTO filtros = service.retornarFiltros(0, null, null, List.of(2, 4));

        assertThat(filtros.clientes()).containsExactly("LOJA");
        assertThat(filtros.plataformas()).containsExactly("Climba");
    }

    @Test
    void filtrosRecusamResponsabilidadeForaDeZeroAQuatro() {
        assertThatThrownBy(() -> service.retornarFiltros(0, null, null, List.of(7)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("0 a 4");

        verifyNoInteractions(repository);
    }
}
