package com.citel.monitoramento_n8n.controller;

import com.citel.monitoramento_n8n.config.SecurityConfigurations;
import com.citel.monitoramento_n8n.exception.ConflictException;
import com.citel.monitoramento_n8n.exception.ForbiddenException;
import com.citel.monitoramento_n8n.exception.NotFoundException;
import com.citel.monitoramento_n8n.model.Cliente;
import com.citel.monitoramento_n8n.model.Pedido;
import com.citel.monitoramento_n8n.model.Usuario;
import com.citel.monitoramento_n8n.repository.ClienteRepository;
import com.citel.monitoramento_n8n.repository.UsuarioRepository;
import com.citel.monitoramento_n8n.security.SecurityFilter;
import com.citel.monitoramento_n8n.service.PedidoService;
import com.citel.monitoramento_n8n.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PATCH /pedidos/{id}/reprocessar: usuário interno (admin ou comum) e lojista. Sem login é 403, e o
 * lojista só reprocessa pedido das integrações dele (quem confere é o service; aqui o 403 dele vira JSON).
 */
@WebMvcTest(controllers = PedidosController.class)
@Import({SecurityConfigurations.class, SecurityFilter.class})
class ReprocessarPedidoControllerTest {

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

    Usuario admin;
    Usuario comum;
    Pedido pedido;

    @BeforeEach
    void preparar() {
        admin = interno("Gabriel Beraldo", "ADMIN");
        comum = interno("Pessoa Comum", "USUARIO");

        pedido = new Pedido();
        pedido.setCodigoPedido("1234");
        pedido.setCliente("TODO TETO");
        pedido.setStatus(0);
    }

    private static Usuario interno(String nome, String perfil) {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setEmail(nome.toLowerCase().replace(' ', '.') + "@citelsoftware.com.br");
        usuario.setNome(nome);
        usuario.setAtivo("S");
        usuario.setPerfil(perfil);
        return usuario;
    }

    @Test
    void adminReprocessaEORetornoTrazOIdEOStatusZero() throws Exception {
        when(service.reprocessar(eq(pedido.getId()), any(UserDetails.class))).thenReturn(pedido);

        mvc.perform(patch("/pedidos/" + pedido.getId() + "/reprocessar").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pedido.getId()))
                .andExpect(jsonPath("$.status").value(0))
                .andExpect(jsonPath("$.codigoPedido").value("1234"));

        verify(service).reprocessar(eq(pedido.getId()), any(UserDetails.class));
    }

    @Test
    void usuarioComumTambemReprocessa() throws Exception {
        when(service.reprocessar(eq(pedido.getId()), any(UserDetails.class))).thenReturn(pedido);

        mvc.perform(patch("/pedidos/" + pedido.getId() + "/reprocessar").with(user(comum)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(0));

        verify(service).reprocessar(eq(pedido.getId()), any(UserDetails.class));
    }

    @Test
    void lojistaChegaAoServiceQueConferePeloDono() throws Exception {
        when(service.reprocessar(eq(pedido.getId()), any(UserDetails.class))).thenReturn(pedido);

        mvc.perform(patch("/pedidos/" + pedido.getId() + "/reprocessar").with(user(new Cliente("Loja", "loja", "x"))))
                .andExpect(status().isOk());

        verify(service).reprocessar(eq(pedido.getId()), any(Cliente.class));
    }

    @Test
    void lojistaDePedidoAlheioLevaForbiddenEmJson() throws Exception {
        when(service.reprocessar(eq("abc"), any(UserDetails.class)))
                .thenThrow(new ForbiddenException("Este pedido não pertence ao lojista autenticado."));

        mvc.perform(patch("/pedidos/abc/reprocessar").with(user(new Cliente("Loja", "loja", "x"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("não pertence")));
    }

    @Test
    void semLoginLevaForbidden() throws Exception {
        mvc.perform(patch("/pedidos/abc/reprocessar")).andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void pedidoForaDoErroVira409() throws Exception {
        when(service.reprocessar(eq("abc"), any(UserDetails.class)))
                .thenThrow(new ConflictException("Só pedidos com erro (status 2) podem ser reprocessados."));

        mvc.perform(patch("/pedidos/abc/reprocessar").with(user(admin)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("com erro")));
    }

    @Test
    void pedidoInexistenteVira404() throws Exception {
        when(service.reprocessar(eq("abc"), any(UserDetails.class)))
                .thenThrow(new NotFoundException("Pedido não encontrado: abc"));

        mvc.perform(patch("/pedidos/abc/reprocessar").with(user(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void asListagensContinuamAbertasAoUsuarioComum() throws Exception {
        mvc.perform(get("/pedidos").with(user(comum))).andExpect(status().isOk());
    }
}
