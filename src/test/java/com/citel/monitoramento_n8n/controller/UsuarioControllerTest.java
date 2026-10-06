package com.citel.monitoramento_n8n.controller;

import com.citel.monitoramento_n8n.config.SecurityConfigurations;
import com.citel.monitoramento_n8n.model.Cliente;
import com.citel.monitoramento_n8n.model.Usuario;
import com.citel.monitoramento_n8n.repository.ClienteRepository;
import com.citel.monitoramento_n8n.repository.UsuarioRepository;
import com.citel.monitoramento_n8n.security.SecurityFilter;
import com.citel.monitoramento_n8n.service.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GET /usuarios/me: é o que o n8n consulta para saber se quem exporta workflows é admin. O perfil
 * precisa vir do principal, que o SecurityFilter relê do banco a cada chamada.
 */
@WebMvcTest(controllers = UsuarioController.class)
@Import({SecurityConfigurations.class, SecurityFilter.class})
class UsuarioControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    TokenService tokenService;
    @MockitoBean
    ClienteRepository clienteRepository;
    @MockitoBean
    UsuarioRepository usuarioRepository;

    private static Usuario usuario(String perfil) {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setEmail("pessoa@citelsoftware.com.br");
        usuario.setNome("Pessoa Teste");
        usuario.setAtivo("S");
        usuario.setPerfil(perfil);
        return usuario;
    }

    @Test
    void adminRecebeSeuPerfil() throws Exception {
        mvc.perform(get("/usuarios/me").with(user(usuario("ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("pessoa@citelsoftware.com.br"))
                .andExpect(jsonPath("$.nome").value("Pessoa Teste"))
                .andExpect(jsonPath("$.perfil").value("ADMIN"));
    }

    @Test
    void usuarioComumRecebeUsuario() throws Exception {
        mvc.perform(get("/usuarios/me").with(user(usuario("USUARIO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("USUARIO"));
    }

    @Test
    void valorEstranhoNoBancoAparecemComoUsuario() throws Exception {
        mvc.perform(get("/usuarios/me").with(user(usuario("ADMN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("USUARIO"));
    }

    @Test
    void adminComCaixaDiferenteNoBancoContaComoAdmin() throws Exception {
        mvc.perform(get("/usuarios/me").with(user(usuario(" admin "))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("ADMIN"));
    }

    @Test
    void naoDevolveNadaAlemDeEmailNomeEPerfil() throws Exception {
        Usuario comGoogleId = usuario("ADMIN");
        comGoogleId.setGoogleId("1234567890");

        mvc.perform(get("/usuarios/me").with(user(comGoogleId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.googleId").doesNotExist())
                .andExpect(jsonPath("$.authorities").doesNotExist())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("1234567890"))));
    }

    @Test
    void lojistaLevaForbidden() throws Exception {
        mvc.perform(get("/usuarios/me").with(user(new Cliente("Loja", "loja", "x"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void semLoginLevaForbidden() throws Exception {
        mvc.perform(get("/usuarios/me")).andExpect(status().isForbidden());
    }
}
