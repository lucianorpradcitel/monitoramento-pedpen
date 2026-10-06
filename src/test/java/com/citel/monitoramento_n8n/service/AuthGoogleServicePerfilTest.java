package com.citel.monitoramento_n8n.service;

import com.citel.monitoramento_n8n.model.Usuario;
import com.citel.monitoramento_n8n.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthGoogleServicePerfilTest {

    private static final String EMAIL = "pessoa@citelsoftware.com.br";

    @Mock
    GoogleTokenService googleTokenService;
    @Mock
    UsuarioRepository usuarioRepository;
    @Mock
    TokenService tokenService;
    @InjectMocks
    AuthGoogleService service;

    private void googleValida() {
        when(googleTokenService.validar("id-token"))
                .thenReturn(new GoogleTokenService.IdentidadeGoogle("sub-123", EMAIL, "Pessoa", "citelsoftware.com.br"));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(chamada -> chamada.getArgument(0));
        when(tokenService.gerarTokenUsuario(any(Usuario.class))).thenReturn("jwt");
    }

    @Test
    void primeiroLoginCriaOUsuarioComoComum() {
        googleValida();
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThat(service.autenticar("id-token")).isEqualTo("jwt");

        ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(salvo.capture());
        assertThat(salvo.getValue().getPerfil()).isEqualTo(Usuario.PERFIL_USUARIO);
        assertThat(salvo.getValue().isAdmin()).isFalse();
        assertThat(salvo.getValue().getAtivo()).isEqualTo("S");
    }

    @Test
    void oLoginNaoRebaixaUmAdmin() {
        googleValida();
        Usuario existente = new Usuario();
        existente.setEmail(EMAIL);
        existente.setNome("Nome antigo");
        existente.setAtivo("S");
        existente.setPerfil(Usuario.PERFIL_ADMIN);
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existente));

        service.autenticar("id-token");

        assertThat(existente.getPerfil()).isEqualTo(Usuario.PERFIL_ADMIN);
        assertThat(existente.getNome()).isEqualTo("Pessoa");
    }

    @Test
    void oLoginNaoPromoveUmUsuarioComum() {
        googleValida();
        Usuario existente = new Usuario();
        existente.setEmail(EMAIL);
        existente.setAtivo("S");
        existente.setPerfil(Usuario.PERFIL_USUARIO);
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existente));

        service.autenticar("id-token");

        assertThat(existente.getPerfil()).isEqualTo(Usuario.PERFIL_USUARIO);
    }
}
