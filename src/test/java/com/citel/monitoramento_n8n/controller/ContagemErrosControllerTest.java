package com.citel.monitoramento_n8n.controller;

import com.citel.monitoramento_n8n.DTO.ContagemErrosDTO;
import com.citel.monitoramento_n8n.config.SecurityConfigurations;
import com.citel.monitoramento_n8n.exception.BusinessException;
import com.citel.monitoramento_n8n.model.Cliente;
import com.citel.monitoramento_n8n.model.Usuario;
import com.citel.monitoramento_n8n.repository.ClienteRepository;
import com.citel.monitoramento_n8n.repository.UsuarioRepository;
import com.citel.monitoramento_n8n.security.SecurityFilter;
import com.citel.monitoramento_n8n.service.ContagemErrosService;
import com.citel.monitoramento_n8n.service.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET /erros/contagem: totais por responsabilidade; os status padrão são 0 (produtos) e 2 (pedidos). */
@WebMvcTest(controllers = ContagemErrosController.class)
@Import({SecurityConfigurations.class, SecurityFilter.class})
class ContagemErrosControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ContagemErrosService service;
    @MockitoBean
    TokenService tokenService;
    @MockitoBean
    ClienteRepository clienteRepository;
    @MockitoBean
    UsuarioRepository usuarioRepository;

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
    void devRecebeAContagemComOsStatusPadrao() throws Exception {
        when(service.contar(any(Usuario.class), org.mockito.ArgumentMatchers.eq(0), org.mockito.ArgumentMatchers.eq(2)))
                .thenReturn(new ContagemErrosDTO(Map.of(0, 1231L, 1, 9L), Map.of(0, 155L)));

        mvc.perform(get("/erros/contagem").with(user(interno("DEV"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.produtos['0']").value(1231))
                .andExpect(jsonPath("$.produtos['1']").value(9))
                .andExpect(jsonPath("$.pedidos['0']").value(155));
    }

    @Test
    void statusPedidoEProdutoPodemSerEscolhidos() throws Exception {
        mvc.perform(get("/erros/contagem").param("statusProduto", "1").param("statusPedido", "0")
                        .with(user(interno("ADM"))))
                .andExpect(status().isOk());

        verify(service).contar(any(Usuario.class), org.mockito.ArgumentMatchers.eq(1), org.mockito.ArgumentMatchers.eq(0));
    }

    @Test
    void lojistaChegaComUsuarioNulo() throws Exception {
        mvc.perform(get("/erros/contagem").with(user(new Cliente("Loja", "loja", "x"))))
                .andExpect(status().isOk());

        verify(service).contar(isNull(), org.mockito.ArgumentMatchers.eq(0), org.mockito.ArgumentMatchers.eq(2));
    }

    @Test
    void statusInvalidoLevaUnprocessable() throws Exception {
        when(service.contar(any(Usuario.class), org.mockito.ArgumentMatchers.eq(5), org.mockito.ArgumentMatchers.eq(2)))
                .thenThrow(new BusinessException("statusProduto deve ser 0 ou 1, mas veio 5"));

        mvc.perform(get("/erros/contagem").param("statusProduto", "5").with(user(interno("DEV"))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void semLoginNaoPassa() throws Exception {
        mvc.perform(get("/erros/contagem")).andExpect(status().isForbidden());
    }
}
