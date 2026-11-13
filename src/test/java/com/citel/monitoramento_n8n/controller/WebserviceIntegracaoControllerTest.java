package com.citel.monitoramento_n8n.controller;

import com.citel.monitoramento_n8n.DTO.IntegracaoResumoDTO;
import com.citel.monitoramento_n8n.config.SecurityConfigurations;
import com.citel.monitoramento_n8n.model.Cliente;
import com.citel.monitoramento_n8n.model.Usuario;
import com.citel.monitoramento_n8n.repository.ClienteRepository;
import com.citel.monitoramento_n8n.repository.UsuarioRepository;
import com.citel.monitoramento_n8n.security.SecurityFilter;
import com.citel.monitoramento_n8n.service.IntegracaoService;
import com.citel.monitoramento_n8n.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PATCH /integracoes/{codigoIntegracao}/{codigoCliente}/webservice: qualquer usuário interno (USR, DEV, ADM) troca o
 * webservice. Lojista (o n8n) e quem não fez login levam 403. O resto da edição continua só do ADM.
 */
@WebMvcTest(controllers = IntegracaoController.class)
@Import({SecurityConfigurations.class, SecurityFilter.class})
class WebserviceIntegracaoControllerTest {

    private static final String CAMINHO = "/integracoes/0000004/401/webservice";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    IntegracaoService service;
    @MockitoBean
    TokenService tokenService;
    @MockitoBean
    ClienteRepository clienteRepository;
    @MockitoBean
    UsuarioRepository usuarioRepository;

    @BeforeEach
    void preparar() {
        when(service.alterarWebservice(eq("0000004"), eq(401L), any(), any()))
                .thenReturn(new IntegracaoResumoDTO("0000004", 401L, "TODO TETO", "tray", "todo-teto", true, "S",
                        "https://loja.commercesuite.com.br/web_api", "http://novo.local:25058", null, null));
    }

    private static Usuario interno(String perfil) {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setEmail("pessoa." + perfil.toLowerCase() + "@citelsoftware.com.br");
        usuario.setNome("Pessoa " + perfil);
        usuario.setAtivo("S");
        usuario.setPerfil(perfil);
        return usuario;
    }

    private ResultActions fazPatch(RequestPostProcessor quem, String corpo) throws Exception {
        return mvc.perform(patch(CAMINHO).with(quem).contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    @Test
    void usuarioComumDevEAdminTrocamOWebservice() throws Exception {
        for (String perfil : new String[]{"USR", "DEV", "ADM"}) {
            fazPatch(user(interno(perfil)), "{\"urlWebservice\":\"http://novo.local:25058\"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.urlWebservice").value("http://novo.local:25058"))
                    .andExpect(jsonPath("$.chavePrivada").doesNotExist());
        }
    }

    @Test
    void oServiceRecebeSoOWebserviceEOUsuarioLogado() throws Exception {
        // urlApi e chavePrivada no corpo são descartadas: o DTO só tem o webservice.
        fazPatch(user(interno("USR")),
                "{\"urlWebservice\":\"http://novo.local:25058\",\"urlApi\":\"https://x\",\"chavePrivada\":\"-----BEGIN\"}")
                .andExpect(status().isOk());

        ArgumentCaptor<Usuario> quem = ArgumentCaptor.forClass(Usuario.class);
        verify(service).alterarWebservice(eq("0000004"), eq(401L), eq("http://novo.local:25058"), quem.capture());
        assertThat(quem.getValue().getNome()).isEqualTo("Pessoa USR");
        verify(service, never()).editar(any(), any(), any(), any());
    }

    @Test
    void oUsuarioComumContinuaSemAEdicaoCompletaDoAdmin() throws Exception {
        mvc.perform(get("/integracoes/0000004/401").with(user(interno("USR")))).andExpect(status().isForbidden());
        mvc.perform(patch("/integracoes/0000004/401").with(user(interno("USR")))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"urlWebservice\":\"http://x\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void lojistaNaoTrocaOWebservice() throws Exception {
        fazPatch(user(new Cliente("Loja", "loja", "x")), "{\"urlWebservice\":\"http://x\"}")
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void semLoginNaoTroca() throws Exception {
        mvc.perform(patch(CAMINHO).contentType(MediaType.APPLICATION_JSON).content("{\"urlWebservice\":\"http://x\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void webserviceVazioOuAusenteERecusado() throws Exception {
        fazPatch(user(interno("USR")), "{\"urlWebservice\":\"\"}").andExpect(status().isBadRequest());
        fazPatch(user(interno("USR")), "{\"urlWebservice\":\"   \"}").andExpect(status().isBadRequest());
        fazPatch(user(interno("USR")), "{\"urlWebservice\":null}").andExpect(status().isBadRequest());
        fazPatch(user(interno("USR")), "{}").andExpect(status().isBadRequest());

        verify(service, never()).alterarWebservice(any(), any(), any(), any());
    }
}
