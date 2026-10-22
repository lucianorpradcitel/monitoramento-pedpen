package com.citel.monitoramento_n8n.controller;

import com.citel.monitoramento_n8n.config.SecurityConfigurations;
import com.citel.monitoramento_n8n.model.Cliente;
import com.citel.monitoramento_n8n.repository.ClienteRepository;
import com.citel.monitoramento_n8n.repository.UsuarioRepository;
import com.citel.monitoramento_n8n.security.SecurityFilter;
import com.citel.monitoramento_n8n.service.ProdutoService;
import com.citel.monitoramento_n8n.service.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GET /produtos?status=: omitido continua trazendo os pendentes (0), que é o que o n8n já consulta
 * hoje sem mandar o parâmetro.
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
    void semStatusConsultaOsPendentes() throws Exception {
        mvc.perform(get("/produtos").with(user(lojista)))
                .andExpect(status().isOk());

        verify(service).retornarProdutos(0, null, null, null, null, null);
    }

    @Test
    void statusUmChegaAoService() throws Exception {
        mvc.perform(get("/produtos").param("status", "1").param("cliente", "LOJA").with(user(lojista)))
                .andExpect(status().isOk());

        verify(service).retornarProdutos(1, null, "LOJA", null, null, null);
    }

    @Test
    void statusNaoNumericoLevaBadRequest() throws Exception {
        mvc.perform(get("/produtos").param("status", "abc").with(user(lojista)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}
