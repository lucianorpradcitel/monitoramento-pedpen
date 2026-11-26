package com.citel.monitoramento_n8n.controller;

import com.citel.monitoramento_n8n.config.SecurityConfigurations;
import com.citel.monitoramento_n8n.model.Cliente;
import com.citel.monitoramento_n8n.repository.ClienteRepository;
import com.citel.monitoramento_n8n.repository.UsuarioRepository;
import com.citel.monitoramento_n8n.security.SecurityFilter;
import com.citel.monitoramento_n8n.service.PedidoService;
import com.citel.monitoramento_n8n.service.TokenService;
import com.citel.monitoramento_n8n.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET /pendentes e GET /pedidos: `status` aceita vários valores, separados por vírgula ou repetidos. */
@WebMvcTest(controllers = PedidosController.class)
@Import({SecurityConfigurations.class, SecurityFilter.class})
class ConsultaPedidosControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    PedidoService service;
    @MockitoBean
    TokenService tokenService;
    @MockitoBean
    ClienteRepository clienteRepository;
    @MockitoBean
    UsuarioRepository usuarioRepository;

    private final Cliente lojista = new Cliente("Loja", "loja", "x");

    @Test
    void pendentesAceitaStatusSeparadosPorVirgula() throws Exception {
        mvc.perform(get("/pendentes").param("status", "1,2").with(user(lojista)))
                .andExpect(status().isOk());

        verify(service).retornarPedidos(List.of(1, 2), null, null, null, null, null);
    }

    @Test
    void pendentesAceitaStatusRepetido() throws Exception {
        mvc.perform(get("/pendentes").param("status", "1").param("status", "2").with(user(lojista)))
                .andExpect(status().isOk());

        verify(service).retornarPedidos(List.of(1, 2), null, null, null, null, null);
    }

    @Test
    void pendentesComUmStatusSegueFuncionando() throws Exception {
        mvc.perform(get("/pendentes").param("status", "0").param("cliente", "LOJA").with(user(lojista)))
                .andExpect(status().isOk());

        verify(service).retornarPedidos(List.of(0), "LOJA", null, null, null, null);
    }

    @Test
    void pendentesSemStatusNaoFiltra() throws Exception {
        mvc.perform(get("/pendentes").with(user(lojista)))
                .andExpect(status().isOk());

        verify(service).retornarPedidos(null, null, null, null, null, null);
    }

    @Test
    void pendentesComStatusNaoNumericoLevaBadRequest() throws Exception {
        mvc.perform(get("/pendentes").param("status", "1,abc").with(user(lojista)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void responsabilidadeRepetidaOuSeparadaPorVirgulaChegaAoService() throws Exception {
        mvc.perform(get("/pedidos").param("status", "2").param("responsabilidade", "1").param("responsabilidade", "0").with(user(lojista)))
                .andExpect(status().isOk());
        mvc.perform(get("/pedidos").param("status", "2").param("responsabilidade", "1,0").with(user(lojista)))
                .andExpect(status().isOk());

        // As duas formas da URL chegam como a mesma lista.
        verify(service, times(2)).retornarPedidos(List.of(2), null, null, null, null, List.of(1, 0));
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
    void usuarioComumSemFiltroVeSo2E4NosDoisEndpoints() throws Exception {
        mvc.perform(get("/pedidos").param("status", "2").with(user(interno("USR"))))
                .andExpect(status().isOk());
        mvc.perform(get("/pendentes").param("status", "2").with(user(interno("USR"))))
                .andExpect(status().isOk());

        verify(service, times(2)).retornarPedidos(List.of(2), null, null, null, null, List.of(2, 4));
    }

    @Test
    void usuarioComumPedindoResponsabilidadeDoTimeLevaForbidden() throws Exception {
        mvc.perform(get("/pedidos").param("responsabilidade", "1").with(user(interno("USR"))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/pendentes").param("responsabilidade", "0").with(user(interno("USR"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void devVeQualquerResponsabilidade() throws Exception {
        mvc.perform(get("/pedidos").param("status", "2").param("responsabilidade", "1,0").with(user(interno("DEV"))))
                .andExpect(status().isOk());

        verify(service).retornarPedidos(List.of(2), null, null, null, null, List.of(1, 0));
    }
}
