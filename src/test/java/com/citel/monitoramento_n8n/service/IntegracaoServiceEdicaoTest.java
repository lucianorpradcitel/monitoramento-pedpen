package com.citel.monitoramento_n8n.service;

import com.citel.monitoramento_n8n.DTO.EdicaoIntegracaoDTO;
import com.citel.monitoramento_n8n.DTO.IntegracaoEdicaoDTO;
import com.citel.monitoramento_n8n.exception.NotFoundException;
import com.citel.monitoramento_n8n.model.Cliente;
import com.citel.monitoramento_n8n.model.Integracao;
import com.citel.monitoramento_n8n.model.LogIntegracao;
import com.citel.monitoramento_n8n.model.Usuario;
import com.citel.monitoramento_n8n.repository.ClienteRepository;
import com.citel.monitoramento_n8n.repository.IntegracaoRepository;
import com.citel.monitoramento_n8n.repository.LogIntegracaoRepository;
import com.citel.monitoramento_n8n.repository.PlataformaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Edição de integração: o que muda no CADINT e, principalmente, o que vira linha no LOGINT.
 * A regra de ouro: só grava log do que realmente mudou, e nada quando nada mudou.
 */
@ExtendWith(MockitoExtension.class)
class IntegracaoServiceEdicaoTest {

    private static final String PEM_ANTIGA = "-----BEGIN PRIVATE KEY-----\nAAA\n-----END PRIVATE KEY-----";
    private static final String PEM_NOVA = "-----BEGIN PRIVATE KEY-----\nBBB\n-----END PRIVATE KEY-----\n";

    @Mock
    IntegracaoRepository repository;
    @Mock
    ClienteRepository clienteRepository;
    @Mock
    PlataformaRepository plataformaRepository;
    @Mock
    LogIntegracaoRepository logRepository;

    IntegracaoService service;
    Integracao integracao;
    Usuario admin;

    @BeforeEach
    void preparar() {
        service = new IntegracaoService(repository, clienteRepository, plataformaRepository, logRepository);

        Cliente cliente = new Cliente("TODO TETO", "todoteto", "x");
        integracao = new Integracao();
        integracao.setCodigoIntegracao("0000004");
        integracao.setCodigoCliente(401L);
        integracao.setCliente(cliente);
        integracao.setPlataforma("tray");
        integracao.setSlug("todo-teto");
        integracao.setAtivo("S");
        integracao.setUrlWebservice("http://erp.local:25058");
        integracao.setUrlApi("https://loja.commercesuite.com.br/web_api");
        integracao.setChavePrivada(PEM_ANTIGA);

        admin = new Usuario();
        admin.setEmail("gabriel@citelsoftware.com.br");
        admin.setNome("Gabriel Beraldo");

        // lenient: o teste da integração inexistente consulta outro código, e o Mockito estrito o trataria como erro.
        lenient().when(repository.findByCodigoIntegracaoAndCodigoCliente("0000004", 401L))
                .thenReturn(Optional.of(integracao));
    }

    private void liberarSave() {
        when(repository.save(any(Integracao.class))).thenAnswer(i -> i.getArgument(0));
    }

    @SuppressWarnings("unchecked")
    private List<LogIntegracao> logsGravados() {
        ArgumentCaptor<List<LogIntegracao>> captor = ArgumentCaptor.forClass(List.class);
        verify(logRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    @Test
    void semNenhumaMudancaNaoGravaNemAIntegracaoNemOLog() {
        IntegracaoEdicaoDTO resposta = service.editar("0000004", 401L, new EdicaoIntegracaoDTO(
                "http://erp.local:25058", "https://loja.commercesuite.com.br/web_api", PEM_ANTIGA), admin);

        verify(repository, never()).save(any());
        verifyNoInteractions(logRepository);
        assertThat(resposta.urlWebservice()).isEqualTo("http://erp.local:25058");
    }

    @Test
    void corpoSoComNulosNaoMudaNada() {
        service.editar("0000004", 401L, new EdicaoIntegracaoDTO(null, null, null), admin);

        verify(repository, never()).save(any());
        verifyNoInteractions(logRepository);
        assertThat(integracao.getChavePrivada()).isEqualTo(PEM_ANTIGA);
    }

    @Test
    void soOCampoQueMudouViraLinhaNoLog() {
        liberarSave();

        service.editar("0000004", 401L, new EdicaoIntegracaoDTO(
                "http://novo.local:25058", "https://loja.commercesuite.com.br/web_api", PEM_ANTIGA), admin);

        List<LogIntegracao> logs = logsGravados();
        assertThat(logs).hasSize(1);
        LogIntegracao linha = logs.get(0);
        assertThat(linha.getCampoAlterado()).isEqualTo("INT_URLWBS");
        assertThat(linha.getValorAnterior()).isEqualTo("http://erp.local:25058");
        assertThat(linha.getValorAtual()).isEqualTo("http://novo.local:25058");
        assertThat(integracao.getUrlWebservice()).isEqualTo("http://novo.local:25058");
    }

    @Test
    void cadaLinhaCarregaPlataformaClienteCodigoEUsuario() {
        liberarSave();

        service.editar("0000004", 401L, new EdicaoIntegracaoDTO("http://novo.local:25058", null, null), admin);

        LogIntegracao linha = logsGravados().get(0);
        assertThat(linha.getNomePlataforma()).isEqualTo("tray");
        assertThat(linha.getNomeCliente()).isEqualTo("TODO TETO");
        assertThat(linha.getCodigoIntegracao()).isEqualTo("0000004");
        assertThat(linha.getNomeUsuario()).isEqualTo("Gabriel Beraldo");
    }

    @Test
    void tresCamposAlteradosViramTresLinhasNaOrdemDaTela() {
        liberarSave();

        service.editar("0000004", 401L, new EdicaoIntegracaoDTO(
                "http://novo.local:25058", "https://outra.commercesuite.com.br/web_api", PEM_NOVA), admin);

        List<LogIntegracao> logs = logsGravados();
        assertThat(logs).extracting(LogIntegracao::getCampoAlterado)
                .containsExactly("INT_URLWBS", "INT_URLAPI", "INT_PRVKEY");
        assertThat(logs.get(1).getValorAnterior()).isEqualTo("https://loja.commercesuite.com.br/web_api");
        assertThat(logs.get(1).getValorAtual()).isEqualTo("https://outra.commercesuite.com.br/web_api");
        assertThat(logs.get(2).getValorAnterior()).isEqualTo(PEM_ANTIGA);
        assertThat(logs.get(2).getValorAtual()).isEqualTo(PEM_NOVA);
    }

    @Test
    void campoNuloPreservaOValorEntreOsOutrosQueMudam() {
        liberarSave();

        service.editar("0000004", 401L,
                new EdicaoIntegracaoDTO(null, "https://outra.commercesuite.com.br/web_api", null), admin);

        assertThat(logsGravados()).hasSize(1);
        assertThat(integracao.getUrlWebservice()).isEqualTo("http://erp.local:25058");
        assertThat(integracao.getChavePrivada()).isEqualTo(PEM_ANTIGA);
    }

    @Test
    void urlsSaemSemEspacosNasPontasMasAChaveVaiExatamenteComoVeio() {
        liberarSave();

        service.editar("0000004", 401L, new EdicaoIntegracaoDTO("  http://novo.local:25058  ", null, PEM_NOVA), admin);

        assertThat(integracao.getUrlWebservice()).isEqualTo("http://novo.local:25058");
        assertThat(integracao.getChavePrivada()).isEqualTo(PEM_NOVA);
    }

    @Test
    void urlIgualDepoisDeAparadaNaoViraMudanca() {
        service.editar("0000004", 401L, new EdicaoIntegracaoDTO("  http://erp.local:25058 ", null, null), admin);

        verify(repository, never()).save(any());
        verifyNoInteractions(logRepository);
    }

    @Test
    void urlApiQueEstavaNulaVirouValorGravaOLogComAnteriorNulo() {
        integracao.setUrlApi(null);
        liberarSave();

        service.editar("0000004", 401L,
                new EdicaoIntegracaoDTO(null, "https://loja.commercesuite.com.br/web_api", null), admin);

        LogIntegracao linha = logsGravados().get(0);
        assertThat(linha.getCampoAlterado()).isEqualTo("INT_URLAPI");
        assertThat(linha.getValorAnterior()).isNull();
    }

    @Test
    void usuarioSemNomeNoCadastroGravaOEmail() {
        liberarSave();
        admin.setNome(null);

        service.editar("0000004", 401L, new EdicaoIntegracaoDTO("http://novo.local:25058", null, null), admin);

        assertThat(logsGravados().get(0).getNomeUsuario()).isEqualTo("gabriel@citelsoftware.com.br");
    }

    @Test
    void integracaoInexistenteDaNotFoundESemLog() {
        assertThatThrownBy(() -> service.editar("9999999", 1L, new EdicaoIntegracaoDTO("http://x", null, null), admin))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(logRepository);
    }

    @Test
    void aRespostaTrazOsValoresNovos() {
        liberarSave();

        IntegracaoEdicaoDTO resposta = service.editar("0000004", 401L,
                new EdicaoIntegracaoDTO("http://novo.local:25058", null, PEM_NOVA), admin);

        assertThat(resposta.urlWebservice()).isEqualTo("http://novo.local:25058");
        assertThat(resposta.chavePrivada()).isEqualTo(PEM_NOVA);
        assertThat(resposta.nomeCliente()).isEqualTo("TODO TETO");
        assertThat(resposta.slug()).isEqualTo("todo-teto");
    }

    @Test
    void buscarParaEdicaoDevolveAChaveParaATela() {
        IntegracaoEdicaoDTO dados = service.buscarParaEdicao("0000004", 401L);

        assertThat(dados.chavePrivada()).isEqualTo(PEM_ANTIGA);
        assertThat(dados.plataforma()).isEqualTo("tray");
    }
}
