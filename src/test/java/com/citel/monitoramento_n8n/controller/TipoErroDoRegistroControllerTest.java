package com.citel.monitoramento_n8n.controller;

import com.citel.monitoramento_n8n.config.SecurityConfigurations;
import com.citel.monitoramento_n8n.exception.NotFoundException;
import com.citel.monitoramento_n8n.model.Cliente;
import com.citel.monitoramento_n8n.model.Pedido;
import com.citel.monitoramento_n8n.model.Produto;
import com.citel.monitoramento_n8n.repository.ClienteRepository;
import com.citel.monitoramento_n8n.repository.UsuarioRepository;
import com.citel.monitoramento_n8n.security.SecurityFilter;
import com.citel.monitoramento_n8n.service.PedidoService;
import com.citel.monitoramento_n8n.service.ProdutoService;
import com.citel.monitoramento_n8n.service.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** PATCH /produtos/{id}/tipo-erro e /pedidos/{id}/tipo-erro: só o n8n (lojista) liga o tipo. */
@WebMvcTest(controllers = {ProdutoController.class, PedidosController.class})
@Import({SecurityConfigurations.class, SecurityFilter.class})
class TipoErroDoRegistroControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ProdutoService produtoService;
    @MockitoBean
    PedidoService pedidoService;
    @MockitoBean
    TokenService tokenService;
    @MockitoBean
    ClienteRepository clienteRepository;
    @MockitoBean
    UsuarioRepository usuarioRepository;

    private static final Cliente LOJISTA = new Cliente("Loja", "loja", "x");

    @Test
    void n8nLigaOTipoDoProduto() throws Exception {
        Produto produto = new Produto();
        produto.setCodigoMensagem(7);
        when(produtoService.definirTipoErro("abc", 7)).thenReturn(produto);

        mvc.perform(patch("/produtos/abc/tipo-erro").with(user(LOJISTA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"codigoMensagem\":7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoMensagem").value(7));
    }

    @Test
    void n8nLigaOTipoDoPedido() throws Exception {
        Pedido pedido = new Pedido();
        pedido.setCodigoMensagem(8);
        when(pedidoService.definirTipoErro("abc", 8)).thenReturn(pedido);

        mvc.perform(patch("/pedidos/abc/tipo-erro").with(user(LOJISTA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"codigoMensagem\":8}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoMensagem").value(8));
    }

    @Test
    void semCorpoDesligaOTipo() throws Exception {
        when(produtoService.definirTipoErro(eq("abc"), isNull())).thenReturn(new Produto());

        mvc.perform(patch("/produtos/abc/tipo-erro").with(user(LOJISTA))).andExpect(status().isOk());

        verify(produtoService).definirTipoErro(eq("abc"), isNull());
    }

    @Test
    void registroInexistenteVira404() throws Exception {
        when(pedidoService.definirTipoErro("x", 8)).thenThrow(new NotFoundException("Pedido não encontrado: x"));

        mvc.perform(patch("/pedidos/x/tipo-erro").with(user(LOJISTA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"codigoMensagem\":8}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void usuarioInternoNaoLigaTipo() throws Exception {
        mvc.perform(patch("/produtos/abc/tipo-erro").with(user("pessoa").roles("INTERNO", "DEV", "ADMIN")))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/pedidos/abc/tipo-erro").with(user("pessoa").roles("INTERNO")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(produtoService, pedidoService);
    }

    @Test
    void reprocessarContinuaSoDoInterno() throws Exception {
        mvc.perform(patch("/pedidos/abc/reprocessar").with(user(LOJISTA))).andExpect(status().isForbidden());
    }
}
