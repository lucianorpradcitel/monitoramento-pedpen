package com.citel.monitoramento_n8n.controller;

import com.citel.monitoramento_n8n.DTO.TipoErroDTO;
import com.citel.monitoramento_n8n.config.SecurityConfigurations;
import com.citel.monitoramento_n8n.exception.BusinessException;
import com.citel.monitoramento_n8n.model.Cliente;
import com.citel.monitoramento_n8n.model.Usuario;
import com.citel.monitoramento_n8n.repository.ClienteRepository;
import com.citel.monitoramento_n8n.repository.UsuarioRepository;
import com.citel.monitoramento_n8n.security.SecurityFilter;
import com.citel.monitoramento_n8n.service.TipoErroService;
import com.citel.monitoramento_n8n.service.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /tipos-erro: o n8n (lojista) consulta e grava; o time (DEV/ADM) valida. Usuário interno comum
 * consulta mas não grava nem valida.
 */
@WebMvcTest(controllers = TipoErroController.class)
@Import({SecurityConfigurations.class, SecurityFilter.class})
class TipoErroControllerTest {

    private static final String CORPO = """
            {"plataforma":"1005","codigoErro":"categoria_ausente","status":3,
             "validado":"N","origem":"I","exemplo":"Produto sem categoria","idExemplo":"PRO-1"}""";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    TipoErroService service;
    @MockitoBean
    TokenService tokenService;
    @MockitoBean
    ClienteRepository clienteRepository;
    @MockitoBean
    UsuarioRepository usuarioRepository;

    private static final Cliente LOJISTA = new Cliente("Loja", "loja", "x");

    private static Usuario interno(String perfil) {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setEmail("pessoa@citelsoftware.com.br");
        u.setNome("Pessoa");
        u.setAtivo("S");
        u.setPerfil(perfil);
        return u;
    }

    private static TipoErroDTO dto(int codigo, int status, String validado) {
        return new TipoErroDTO(codigo, "1005", "categoria_ausente", status, validado, "I",
                "Produto sem categoria", "PRO-1", null, null, null);
    }

    @Test
    void n8nListaOsTiposDaPlataforma() throws Exception {
        when(service.listar("1005", null, null)).thenReturn(List.of(dto(7, 3, "S")));

        mvc.perform(get("/tipos-erro").param("plataforma", "1005").with(user(LOJISTA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value(7))
                .andExpect(jsonPath("$[0].codigoErro").value("categoria_ausente"))
                .andExpect(jsonPath("$[0].status").value(3))
                .andExpect(jsonPath("$[0].validado").value("S"));
    }

    @Test
    void usuarioComumTambemLista() throws Exception {
        mvc.perform(get("/tipos-erro").param("validado", "N").with(user(interno("USR"))))
                .andExpect(status().isOk());
    }

    @Test
    void semLoginNaoLista() throws Exception {
        mvc.perform(get("/tipos-erro")).andExpect(status().isForbidden());
    }

    @Test
    void n8nGravaTipoNovoE201() throws Exception {
        when(service.registrar(any(), isNull())).thenReturn(new TipoErroService.Registro(dto(8, 3, "N"), true));

        mvc.perform(post("/tipos-erro").with(user(LOJISTA)).contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/tipos-erro/8")))
                .andExpect(jsonPath("$.codigo").value(8))
                .andExpect(jsonPath("$.validado").value("N"));
    }

    @Test
    void tipoQueJaExisteVolta200() throws Exception {
        when(service.registrar(any(), isNull())).thenReturn(new TipoErroService.Registro(dto(7, 3, "S"), false));

        mvc.perform(post("/tipos-erro").with(user(LOJISTA)).contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(7));
    }

    @Test
    void corpoSemCodigoVira400() throws Exception {
        mvc.perform(post("/tipos-erro").with(user(LOJISTA)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plataforma\":\"1005\",\"status\":3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("codigoErro")));

        verifyNoInteractions(service);
    }

    @Test
    void plataformaInexistenteVira422() throws Exception {
        when(service.registrar(any(), isNull())).thenThrow(new BusinessException("Plataforma não cadastrada na CADPLA: 1005"));

        mvc.perform(post("/tipos-erro").with(user(LOJISTA)).contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void usuarioComumNaoGrava() throws Exception {
        mvc.perform(post("/tipos-erro").with(user(interno("USR"))).contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void devValidaEReclassifica() throws Exception {
        when(service.validar(eq(7), any(), any(Usuario.class))).thenReturn(dto(7, 2, "S"));

        mvc.perform(patch("/tipos-erro/7").with(user(interno("DEV")))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(2))
                .andExpect(jsonPath("$.validado").value("S"));

        verify(service).validar(eq(7), any(), any(Usuario.class));
    }

    @Test
    void adminValidaSemCorpo() throws Exception {
        when(service.validar(eq(7), isNull(), any(Usuario.class))).thenReturn(dto(7, 3, "S"));

        mvc.perform(patch("/tipos-erro/7").with(user(interno("ADM"))))
                .andExpect(status().isOk());
    }

    @Test
    void lojistaEUsuarioComumNaoValidam() throws Exception {
        mvc.perform(patch("/tipos-erro/7").with(user(LOJISTA))).andExpect(status().isForbidden());
        mvc.perform(patch("/tipos-erro/7").with(user(interno("USR")))).andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }
}
