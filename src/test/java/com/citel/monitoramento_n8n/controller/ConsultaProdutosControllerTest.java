package com.citel.monitoramento_n8n.controller;

import com.citel.monitoramento_n8n.DTO.FiltrosProdutosDTO;
import com.citel.monitoramento_n8n.DTO.PaginaDTO;
import com.citel.monitoramento_n8n.config.SecurityConfigurations;
import com.citel.monitoramento_n8n.exception.BusinessException;
import com.citel.monitoramento_n8n.model.Cliente;
import com.citel.monitoramento_n8n.model.Produto;
import com.citel.monitoramento_n8n.repository.ClienteRepository;
import com.citel.monitoramento_n8n.repository.UsuarioRepository;
import com.citel.monitoramento_n8n.security.SecurityFilter;
import com.citel.monitoramento_n8n.service.ProdutoService;
import com.citel.monitoramento_n8n.service.TokenService;
import com.citel.monitoramento_n8n.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GET /produtos: sem parâmetros continua trazendo os pendentes (0), como o n8n já consulta hoje,
 * agora em páginas de 50, do erro mais recente para o mais antigo.
 */
@WebMvcTest(controllers = ProdutoController.class)
@Import({SecurityConfigurations.class, SecurityFilter.class})
class ConsultaProdutosControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ProdutoService service;
    @MockitoBean
    TokenService tokenService;
    @MockitoBean
    ClienteRepository clienteRepository;
    @MockitoBean
    UsuarioRepository usuarioRepository;

    private final Cliente lojista = new Cliente("Loja", "loja", "x");

    @Test
    void semParametrosConsultaOsPendentesDaPrimeiraPaginaDe50() throws Exception {
        mvc.perform(get("/produtos").with(user(lojista)))
                .andExpect(status().isOk());

        verify(service).retornarProdutos(0, null, null, null, null, null, null, null, null, null, null, null, 1, 50, "desc");
    }

    @Test
    void statusUmChegaAoService() throws Exception {
        mvc.perform(get("/produtos").param("status", "1").param("cliente", "LOJA").with(user(lojista)))
                .andExpect(status().isOk());

        verify(service).retornarProdutos(1, null, "LOJA", null, null, null, null, null, null, null, null, null, 1, 50, "desc");
    }

    @Test
    void paginacaoPeriodoEOrdemChegamAoService() throws Exception {
        mvc.perform(get("/produtos")
                        .param("pagina", "3")
                        .param("tamanho", "100")
                        .param("ordem", "asc")
                        .param("dataInicial", "2026-11-05T08:00")
                        .param("dataFinal", "2026-11-05T18:30:59.999")
                        .with(user(lojista)))
                .andExpect(status().isOk());

        verify(service).retornarProdutos(0, null, null, null, null, null,
                LocalDateTime.of(2026, 11, 5, 8, 0),
                LocalDateTime.of(2026, 11, 5, 18, 30, 59, 999_000_000),
                null, null, null, null,
                3, 100, "asc");
    }

    @Test
    void plataformaExataEPesquisasPorTrechoChegamAoService() throws Exception {
        mvc.perform(get("/produtos")
                        .param("produtoContem", "04464")
                        .param("plataforma", "Tray")
                        .param("erroContem", "peso")
                        .with(user(lojista)))
                .andExpect(status().isOk());

        verify(service).retornarProdutos(0, null, null, null, null, null, null, null,
                "Tray", "04464", "peso", null, 1, 50, "desc");
    }

    @Test
    void filtrosTrazOsClientesEAsPlataformasComProduto() throws Exception {
        when(service.retornarFiltros(0, null, null, null))
                .thenReturn(new FiltrosProdutosDTO(List.of("LOJA", "OUTRA"), List.of("Tray", "VTEX")));

        mvc.perform(get("/produtos/filtros").with(user(lojista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientes.length()").value(2))
                .andExpect(jsonPath("$.clientes[0]").value("LOJA"))
                .andExpect(jsonPath("$.plataformas[1]").value("VTEX"));
    }

    @Test
    void filtrosRespeitaOStatusPedido() throws Exception {
        mvc.perform(get("/produtos/filtros").param("status", "1").with(user(lojista)))
                .andExpect(status().isOk());

        verify(service).retornarFiltros(1, null, null, null);
    }

    @Test
    void filtrosRepassaClienteEPlataformaEscolhidos() throws Exception {
        mvc.perform(get("/produtos/filtros").param("cliente", "LOJA").param("plataforma", "Tray")
                        .with(user(lojista)))
                .andExpect(status().isOk());

        verify(service).retornarFiltros(0, "LOJA", "Tray", null);
    }

    @Test
    void filtrosComStatusRecusadoPeloServiceViraUnprocessableEntity() throws Exception {
        when(service.retornarFiltros(2, null, null, null)).thenThrow(new BusinessException("status deve ser 0 ou 1, mas veio 2"));

        mvc.perform(get("/produtos/filtros").param("status", "2").with(user(lojista)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void respostaTrazOsItensEOsTotais() throws Exception {
        when(service.retornarProdutos(anyInt(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                anyInt(), anyInt(), any()))
                .thenReturn(new PaginaDTO<>(List.of(new Produto()), 1, 50, 8342, 167));

        mvc.perform(get("/produtos").with(user(lojista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.tamanho").value(50))
                .andExpect(jsonPath("$.totalItens").value(8342))
                .andExpect(jsonPath("$.totalPaginas").value(167));
    }

    @Test
    void tamanhoRecusadoPeloServiceViraUnprocessableEntity() throws Exception {
        when(service.retornarProdutos(anyInt(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                anyInt(), anyInt(), any()))
                .thenThrow(new BusinessException("tamanho deve ser 50, 100, 1000 ou 0 (todos), mas veio 25"));

        mvc.perform(get("/produtos").param("tamanho", "25").with(user(lojista)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void statusNaoNumericoLevaBadRequest() throws Exception {
        mvc.perform(get("/produtos").param("status", "abc").with(user(lojista)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void paginaNaoNumericaEDataMalFormadaLevamBadRequest() throws Exception {
        mvc.perform(get("/produtos").param("pagina", "abc").with(user(lojista)))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/produtos").param("dataInicial", "05/11/2026").with(user(lojista)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void responsabilidadeRepetidaOuSeparadaPorVirgulaChegaAoService() throws Exception {
        mvc.perform(get("/produtos").param("responsabilidade", "1").param("responsabilidade", "0").with(user(lojista)))
                .andExpect(status().isOk());
        mvc.perform(get("/produtos").param("responsabilidade", "1,0").with(user(lojista)))
                .andExpect(status().isOk());

        // As duas formas da URL chegam como a mesma lista.
        verify(service, times(2)).retornarProdutos(0, null, null, null, null, null, null, null, null, null, null,
                List.of(1, 0), 1, 50, "desc");
    }

    private static Usuario interno(String perfil) {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setEmail("pessoa@citelsoftware.com.br");
        usuario.setNome("Pessoa");
        usuario.setAtivo("S");
        usuario.setPerfil(perfil);
        return usuario;
    }

    @Test
    void usuarioComumSemFiltroVeSo2E4() throws Exception {
        mvc.perform(get("/produtos").with(user(interno("USR"))))
                .andExpect(status().isOk());

        verify(service).retornarProdutos(0, null, null, null, null, null, null, null, null, null, null,
                List.of(2, 4), 1, 50, "desc");
    }

    @Test
    void usuarioComumPedindoResponsabilidadeDoTimeLevaForbidden() throws Exception {
        mvc.perform(get("/produtos").param("responsabilidade", "1").with(user(interno("USR"))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/produtos").param("responsabilidade", "2,0").with(user(interno("USR"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void usuarioComumPodePedirSo2() throws Exception {
        mvc.perform(get("/produtos").param("responsabilidade", "2").with(user(interno("USR"))))
                .andExpect(status().isOk());

        verify(service).retornarProdutos(0, null, null, null, null, null, null, null, null, null, null,
                List.of(2), 1, 50, "desc");
    }

    @Test
    void devEAdmVeemQualquerResponsabilidade() throws Exception {
        mvc.perform(get("/produtos").param("responsabilidade", "1,0").with(user(interno("DEV"))))
                .andExpect(status().isOk());
        mvc.perform(get("/produtos").param("responsabilidade", "1,0").with(user(interno("ADM"))))
                .andExpect(status().isOk());

        verify(service, times(2)).retornarProdutos(0, null, null, null, null, null, null, null, null, null, null,
                List.of(1, 0), 1, 50, "desc");
    }

    @Test
    void filtrosDoUsuarioComumSoVeem2E4() throws Exception {
        mvc.perform(get("/produtos/filtros").with(user(interno("USR"))))
                .andExpect(status().isOk());

        verify(service).retornarFiltros(0, null, null, List.of(2, 4));
    }

    @Test
    void filtrosDoUsuarioComumPedindoOutraResponsabilidadeLevamForbidden() throws Exception {
        mvc.perform(get("/produtos/filtros").param("responsabilidade", "1").with(user(interno("USR"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void filtrosDeDevELojistaPassamOQuePediram() throws Exception {
        mvc.perform(get("/produtos/filtros").param("responsabilidade", "1,0").with(user(interno("DEV"))))
                .andExpect(status().isOk());
        mvc.perform(get("/produtos/filtros").param("responsabilidade", "1,0").with(user(lojista)))
                .andExpect(status().isOk());

        verify(service, times(2)).retornarFiltros(0, null, null, List.of(1, 0));
    }
}
