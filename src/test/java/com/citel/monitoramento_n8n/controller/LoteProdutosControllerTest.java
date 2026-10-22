package com.citel.monitoramento_n8n.controller;

import com.citel.monitoramento_n8n.DTO.ProdutoLoteDTO;
import com.citel.monitoramento_n8n.config.SecurityConfigurations;
import com.citel.monitoramento_n8n.model.Cliente;
import com.citel.monitoramento_n8n.repository.ClienteRepository;
import com.citel.monitoramento_n8n.repository.UsuarioRepository;
import com.citel.monitoramento_n8n.security.SecurityFilter;
import com.citel.monitoramento_n8n.service.ProdutoService;
import com.citel.monitoramento_n8n.service.TokenService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * POST /produtos/lote: o campo status do JSON chega ao service, e item sem ele chega nulo
 * (é o que separa a troca de status do monitor do registro de erro de sempre).
 */
@WebMvcTest(controllers = ProdutoController.class)
@Import({SecurityConfigurations.class, SecurityFilter.class})
class LoteProdutosControllerTest {

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

    @Test
    @SuppressWarnings("unchecked")
    void statusDoJsonChegaAoServiceEAusenteChegaNulo() throws Exception {
        String corpo = """
                [
                  {"codigoProduto": "001", "cliente": "VENTURE", "rotina": "R", "status": 1},
                  {"codigoProduto": "002", "cliente": "VENTURE", "rotina": "R", "mensagemErro": "erro"}
                ]""";

        mvc.perform(post("/produtos/lote").contentType(MediaType.APPLICATION_JSON).content(corpo)
                        .with(user(new Cliente("Loja", "loja", "x"))))
                .andExpect(status().isOk());

        ArgumentCaptor<List<ProdutoLoteDTO>> lote = ArgumentCaptor.forClass(List.class);
        verify(service).registrarProdutosList(lote.capture());
        assertThat(lote.getValue()).extracting(ProdutoLoteDTO::getStatus).containsExactly(1, null);
    }
}
