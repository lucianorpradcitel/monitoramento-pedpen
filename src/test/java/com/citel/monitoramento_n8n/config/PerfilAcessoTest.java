package com.citel.monitoramento_n8n.config;

import com.citel.monitoramento_n8n.controller.ClienteConsultaController;
import com.citel.monitoramento_n8n.controller.IntegracaoController;
import com.citel.monitoramento_n8n.controller.PlataformaController;
import com.citel.monitoramento_n8n.repository.ClienteRepository;
import com.citel.monitoramento_n8n.repository.UsuarioRepository;
import com.citel.monitoramento_n8n.security.SecurityFilter;
import com.citel.monitoramento_n8n.service.ClienteService;
import com.citel.monitoramento_n8n.service.IntegracaoService;
import com.citel.monitoramento_n8n.service.PlataformaService;
import com.citel.monitoramento_n8n.service.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regra das telas de admin: o usuário interno sem perfil ADMIN leva 403; admin e lojista (o n8n)
 * passam como antes. Aqui só a camada de segurança é real; serviços e repositórios são falsos.
 */
@WebMvcTest(controllers = {PlataformaController.class, ClienteConsultaController.class, IntegracaoController.class})
@Import({SecurityConfigurations.class, SecurityFilter.class})
class PerfilAcessoTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    PlataformaService plataformaService;
    @MockitoBean
    ClienteService clienteService;
    @MockitoBean
    IntegracaoService integracaoService;
    @MockitoBean
    TokenService tokenService;
    @MockitoBean
    ClienteRepository clienteRepository;
    @MockitoBean
    UsuarioRepository usuarioRepository;

    /** Corpo vazio: se a regra deixar passar, a validação do controller responde 400, nunca 403. */
    private int statusDoPost(String caminho, String... roles) throws Exception {
        return mvc.perform(post(caminho).with(user("pessoa").roles(roles))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn().getResponse().getStatus();
    }

    @Test
    void usuarioComumNaoCriaPlataforma() throws Exception {
        mvc.perform(post("/plataformas").with(user("pessoa").roles("INTERNO"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value(containsString("permissão")));
    }

    @Test
    void usuarioComumNaoCriaIntegracao() throws Exception {
        assertThat(statusDoPost("/integracoes", "INTERNO")).isEqualTo(403);
    }

    @Test
    void usuarioComumNaoListaLojistas() throws Exception {
        mvc.perform(get("/clientes").with(user("pessoa").roles("INTERNO")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminPassaPelaRegra() throws Exception {
        assertThat(statusDoPost("/plataformas", "INTERNO", "ADMIN")).isNotEqualTo(403);
        assertThat(statusDoPost("/integracoes", "INTERNO", "ADMIN")).isNotEqualTo(403);
        mvc.perform(get("/clientes").with(user("pessoa").roles("INTERNO", "ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void lojistaContinuaPassandoComoAntes() throws Exception {
        assertThat(statusDoPost("/plataformas", "LOJISTA")).isNotEqualTo(403);
        assertThat(statusDoPost("/integracoes", "LOJISTA")).isNotEqualTo(403);
        mvc.perform(get("/clientes").with(user("n8n").roles("LOJISTA")))
                .andExpect(status().isOk());
    }

    @Test
    void aListagemDePlataformasContinuaAbertaParaUsuarioComum() throws Exception {
        mvc.perform(get("/plataformas").with(user("pessoa").roles("INTERNO")))
                .andExpect(status().isOk());
    }
}
