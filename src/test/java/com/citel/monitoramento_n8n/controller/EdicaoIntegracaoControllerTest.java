package com.citel.monitoramento_n8n.controller;

import com.citel.monitoramento_n8n.DTO.EdicaoIntegracaoDTO;
import com.citel.monitoramento_n8n.DTO.IntegracaoEdicaoDTO;
import com.citel.monitoramento_n8n.config.SecurityConfigurations;
import com.citel.monitoramento_n8n.model.Cliente;
import com.citel.monitoramento_n8n.model.Usuario;
import com.citel.monitoramento_n8n.repository.ClienteRepository;
import com.citel.monitoramento_n8n.repository.UsuarioRepository;
import com.citel.monitoramento_n8n.security.SecurityFilter;
import com.citel.monitoramento_n8n.service.IntegracaoService;
import com.citel.monitoramento_n8n.service.TokenService;
import org.hamcrest.Matchers;
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
 * GET e PATCH /integracoes/{codigoIntegracao}/{codigoCliente}: só usuário interno ADMIN.
 * A barreira é do servidor — o botão escondido no portal não protege nada contra Swagger/Postman.
 */
@WebMvcTest(controllers = IntegracaoController.class)
@Import({SecurityConfigurations.class, SecurityFilter.class})
class EdicaoIntegracaoControllerTest {

    private static final String CAMINHO = "/integracoes/0000004/401";
    private static final String CORPO = "{\"urlWebservice\":\"http://novo.local:25058\"}";

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

    Usuario admin;
    Usuario comum;

    @BeforeEach
    void preparar() {
        admin = interno("Gabriel Beraldo", "ADMIN");
        comum = interno("Pessoa Comum", "USUARIO");

        IntegracaoEdicaoDTO resposta = new IntegracaoEdicaoDTO("0000004", 401L, "TODO TETO", "tray", "todo-teto",
                true, "http://novo.local:25058", "https://loja.commercesuite.com.br/web_api",
                "-----BEGIN PRIVATE KEY-----");
        when(service.buscarParaEdicao("0000004", 401L)).thenReturn(resposta);
        when(service.editar(eq("0000004"), eq(401L), any(), any())).thenReturn(resposta);
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

    private ResultActions fazPatch(RequestPostProcessor quem, String corpo) throws Exception {
        return mvc.perform(patch(CAMINHO).with(quem).contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    // ---- quem não pode ----

    @Test
    void usuarioComumNaoEditaNemLe() throws Exception {
        fazPatch(user(comum), CORPO).andExpect(status().isForbidden());
        mvc.perform(get(CAMINHO).with(user(comum))).andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void lojistaNaoEditaNemLe() throws Exception {
        RequestPostProcessor lojista = user(new Cliente("Loja", "loja", "x"));

        fazPatch(lojista, CORPO).andExpect(status().isForbidden());
        mvc.perform(get(CAMINHO).with(lojista)).andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void semLoginNaoEdita() throws Exception {
        mvc.perform(patch(CAMINHO).contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    // ---- quem pode ----

    @Test
    void adminLeOsDadosParaATela() throws Exception {
        mvc.perform(get(CAMINHO).with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoIntegracao").value("0000004"))
                .andExpect(jsonPath("$.chavePrivada").value("-----BEGIN PRIVATE KEY-----"))
                .andExpect(jsonPath("$.apiToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.webhookToken").doesNotExist());
    }

    @Test
    void adminEditaEOServicoRecebeOUsuarioLogado() throws Exception {
        fazPatch(user(admin), CORPO)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urlWebservice").value("http://novo.local:25058"));

        ArgumentCaptor<EdicaoIntegracaoDTO> dados = ArgumentCaptor.forClass(EdicaoIntegracaoDTO.class);
        ArgumentCaptor<Usuario> quem = ArgumentCaptor.forClass(Usuario.class);
        verify(service).editar(eq("0000004"), eq(401L), dados.capture(), quem.capture());
        assertThat(dados.getValue().urlWebservice()).isEqualTo("http://novo.local:25058");
        assertThat(dados.getValue().urlApi()).isNull();
        assertThat(quem.getValue().getNome()).isEqualTo("Gabriel Beraldo");
    }

    @Test
    void camposForaDosTresSaoDescartadosSemErro() throws Exception {
        fazPatch(user(admin), "{\"urlWebservice\":\"http://novo.local:25058\",\"slug\":\"hackeado\","
                + "\"plataforma\":\"mercos\",\"ativo\":\"N\",\"webhookToken\":\"ct_x\",\"apiToken\":\"y\","
                + "\"codigoIntegracao\":\"9999999\"}")
                .andExpect(status().isOk());

        ArgumentCaptor<EdicaoIntegracaoDTO> dados = ArgumentCaptor.forClass(EdicaoIntegracaoDTO.class);
        verify(service).editar(eq("0000004"), eq(401L), dados.capture(), any());
        // O record só tem os três campos: não existe por onde o resto chegar ao service.
        assertThat(dados.getValue()).isEqualTo(new EdicaoIntegracaoDTO("http://novo.local:25058", null, null));
    }

    // ---- validação ----

    @Test
    void camposVaziosSaoRecusados() throws Exception {
        fazPatch(user(admin), "{\"urlWebservice\":\"   \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(Matchers.containsString("urlWebservice")));
        fazPatch(user(admin), "{\"urlApi\":\"\"}").andExpect(status().isBadRequest());
        fazPatch(user(admin), "{\"chavePrivada\":\"\"}").andExpect(status().isBadRequest());

        verify(service, never()).editar(any(), any(), any(), any());
    }

    @Test
    void chaveForaDoFormatoPemERecusada() throws Exception {
        fazPatch(user(admin), "{\"chavePrivada\":\"isto nao e uma chave\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(Matchers.containsString("PEM")));
    }

    @Test
    void chavePemComQuebrasDeLinhaPassa() throws Exception {
        fazPatch(user(admin),
                "{\"chavePrivada\":\"-----BEGIN PRIVATE KEY-----\\nAAA\\n-----END PRIVATE KEY-----\\n\"}")
                .andExpect(status().isOk());
    }

    // ---- o que não pode ter mudado ----

    @Test
    void oPatchDeTokensDoN8nContinuaAbertoAoLojista() throws Exception {
        int statusLojista = mvc.perform(patch("/integracoes/0000004/401/tokens")
                        .with(user(new Cliente("Loja", "loja", "x")))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn().getResponse().getStatus();

        assertThat(statusLojista).isNotEqualTo(403);
    }

    @Test
    void oContextoPorSlugContinuaAbertoAoLojista() throws Exception {
        int statusLojista = mvc.perform(get("/integracoes/casa-furadeiras?plataforma=tray")
                        .with(user(new Cliente("Loja", "loja", "x"))))
                .andReturn().getResponse().getStatus();

        assertThat(statusLojista).isNotEqualTo(403);
    }
}
