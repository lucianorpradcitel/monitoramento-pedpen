package com.citel.monitoramento_n8n.service;

import com.citel.monitoramento_n8n.DTO.DadosCriacaoTipoErro;
import com.citel.monitoramento_n8n.DTO.TipoErroDTO;
import com.citel.monitoramento_n8n.DTO.ValidacaoTipoErroDTO;
import com.citel.monitoramento_n8n.exception.BusinessException;
import com.citel.monitoramento_n8n.exception.NotFoundException;
import com.citel.monitoramento_n8n.model.TipoErro;
import com.citel.monitoramento_n8n.model.Usuario;
import com.citel.monitoramento_n8n.repository.PlataformaRepository;
import com.citel.monitoramento_n8n.repository.StatusErroRepository;
import com.citel.monitoramento_n8n.repository.TipoErroRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TipoErroServiceTest {

    @Mock
    TipoErroRepository tipoErroRepository;
    @Mock
    PlataformaRepository plataformaRepository;
    @Mock
    StatusErroRepository statusErroRepository;
    @InjectMocks
    TipoErroService service;

    private static DadosCriacaoTipoErro dados(Integer status) {
        return new DadosCriacaoTipoErro(" 1005 ", " categoria_ausente ", status, "Produto sem categoria no ERP.", "PRO-1");
    }

    private static TipoErro tipo(Integer codigo, Integer status, String validado) {
        TipoErro t = new TipoErro();
        t.setCodigo(codigo);
        t.setPlataforma("1005");
        t.setCodigoErro("categoria_ausente");
        t.setStatus(status);
        t.setValidado(validado);
        t.setOrigem(TipoErro.ORIGEM_IA);
        return t;
    }

    private static Usuario admin() {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setEmail("gabriel.beraldo@citelsoftware.com.br");
        u.setNome("Gabriel Beraldo");
        u.setAtivo("S");
        u.setPerfil(Usuario.PERFIL_ADM);
        return u;
    }

    private void plataformaEStatusExistem() {
        when(plataformaRepository.existsById("1005")).thenReturn(true);
        when(statusErroRepository.existsById(3)).thenReturn(true);
    }

    @Test
    void tipoQueJaExisteVoltaSemGravarNada() {
        when(tipoErroRepository.findByPlataformaAndCodigoErro("1005", "categoria_ausente"))
                .thenReturn(Optional.of(tipo(7, 3, "S")));

        TipoErroService.Registro registro = service.registrar(dados(2), null);

        assertThat(registro.criado()).isFalse();
        assertThat(registro.tipo().codigo()).isEqualTo(7);
        assertThat(registro.tipo().status()).isEqualTo(3); // vale o da tabela, não o que veio
        verify(tipoErroRepository, never()).saveAndFlush(any());
    }

    @Test
    void n8nGravaComoIaENaoValidado() {
        when(tipoErroRepository.findByPlataformaAndCodigoErro("1005", "categoria_ausente")).thenReturn(Optional.empty());
        plataformaEStatusExistem();
        when(tipoErroRepository.saveAndFlush(any(TipoErro.class))).thenAnswer(c -> {
            TipoErro t = c.getArgument(0);
            t.setCodigo(8);
            return t;
        });

        TipoErroService.Registro registro = service.registrar(dados(3), null);

        assertThat(registro.criado()).isTrue();
        TipoErroDTO dto = registro.tipo();
        assertThat(dto.codigo()).isEqualTo(8);
        assertThat(dto.plataforma()).isEqualTo("1005");
        assertThat(dto.codigoErro()).isEqualTo("categoria_ausente");
        assertThat(dto.origem()).isEqualTo("I");
        assertThat(dto.validado()).isEqualTo("N");
        assertThat(dto.usuarioValidacao()).isNull();
        assertThat(dto.dataCadastro()).isNotNull();
        assertThat(dto.idExemplo()).isEqualTo("PRO-1");
    }

    @Test
    void adminPeloPortalGravaComoUsuarioJaValidado() {
        when(tipoErroRepository.findByPlataformaAndCodigoErro("1005", "categoria_ausente")).thenReturn(Optional.empty());
        plataformaEStatusExistem();
        when(tipoErroRepository.saveAndFlush(any(TipoErro.class))).thenAnswer(c -> c.getArgument(0));

        TipoErroDTO dto = service.registrar(dados(3), admin()).tipo();

        assertThat(dto.origem()).isEqualTo("U");
        assertThat(dto.validado()).isEqualTo("S");
        assertThat(dto.usuarioValidacao()).isEqualTo("gabriel.beraldo");
        assertThat(dto.dataValidacao()).isNotNull();
    }

    @Test
    void corridaNaChaveUnicaDevolveOTipoDoOutro() {
        when(tipoErroRepository.findByPlataformaAndCodigoErro("1005", "categoria_ausente"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(tipo(9, 3, "N")));
        plataformaEStatusExistem();
        when(tipoErroRepository.saveAndFlush(any(TipoErro.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry"));

        TipoErroService.Registro registro = service.registrar(dados(3), null);

        assertThat(registro.criado()).isFalse();
        assertThat(registro.tipo().codigo()).isEqualTo(9);
    }

    @Test
    void plataformaForaDaCadplaVira422() {
        when(tipoErroRepository.findByPlataformaAndCodigoErro("1005", "categoria_ausente")).thenReturn(Optional.empty());
        when(plataformaRepository.existsById("1005")).thenReturn(false);

        assertThatThrownBy(() -> service.registrar(dados(3), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Plataforma");
    }

    @Test
    void statusForaDaStaerrVira422() {
        when(tipoErroRepository.findByPlataformaAndCodigoErro("1005", "categoria_ausente")).thenReturn(Optional.empty());
        when(plataformaRepository.existsById("1005")).thenReturn(true);
        when(statusErroRepository.existsById(5)).thenReturn(false);

        assertThatThrownBy(() -> service.registrar(dados(5), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("status");
    }

    @Test
    void validarSemStatusSoConfirma() {
        TipoErro t = tipo(7, 2, "N");
        when(tipoErroRepository.findById(7)).thenReturn(Optional.of(t));
        when(tipoErroRepository.save(any(TipoErro.class))).thenAnswer(c -> c.getArgument(0));

        TipoErroDTO dto = service.validar(7, new ValidacaoTipoErroDTO(null), admin());

        assertThat(dto.status()).isEqualTo(2);
        assertThat(dto.validado()).isEqualTo("S");
        assertThat(dto.usuarioValidacao()).isEqualTo("gabriel.beraldo");
    }

    @Test
    void validarComStatusReclassifica() {
        TipoErro t = tipo(7, 2, "N");
        when(tipoErroRepository.findById(7)).thenReturn(Optional.of(t));
        when(statusErroRepository.existsById(3)).thenReturn(true);
        when(tipoErroRepository.save(any(TipoErro.class))).thenAnswer(c -> c.getArgument(0));

        TipoErroDTO dto = service.validar(7, new ValidacaoTipoErroDTO(3), admin());

        ArgumentCaptor<TipoErro> salvo = ArgumentCaptor.forClass(TipoErro.class);
        verify(tipoErroRepository).save(salvo.capture());
        assertThat(salvo.getValue().getStatus()).isEqualTo(3);
        assertThat(dto.validado()).isEqualTo("S");
    }

    @Test
    void validarTipoInexistenteVira404() {
        when(tipoErroRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validar(99, null, admin())).isInstanceOf(NotFoundException.class);
    }

    @Test
    void listarAceitaValidadoMinusculoEFiltroVazio() {
        when(tipoErroRepository.buscar("1005", "N", null)).thenReturn(List.of(tipo(7, 2, "N")));

        assertThat(service.listar("1005", "n", " ")).hasSize(1);
    }

    @Test
    void listarRecusaValidadoInvalido() {
        assertThatThrownBy(() -> service.listar(null, "X", null)).isInstanceOf(BusinessException.class);
    }

    @Test
    void usuarioValidacaoCabeEm30Caracteres() {
        Usuario u = admin();
        u.setEmail("um.nome.muito.comprido.de.pessoa.qualquer@citelsoftware.com.br");

        assertThat(TipoErroService.identificar(u)).hasSize(30);
        assertThat(TipoErroService.identificar(null)).isNull();
    }
}
