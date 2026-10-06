package com.citel.monitoramento_n8n.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.core.GrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioTest {

    private static Usuario comPerfil(String perfil) {
        Usuario usuario = new Usuario();
        usuario.setEmail("pessoa@citelsoftware.com.br");
        usuario.setAtivo("S");
        usuario.setPerfil(perfil);
        return usuario;
    }

    private static java.util.List<String> autoridades(Usuario usuario) {
        return usuario.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    }

    @Test
    void usuarioNovoNasceComoUsuarioComum() {
        assertThat(new Usuario().getPerfil()).isEqualTo(Usuario.PERFIL_USUARIO);
        assertThat(new Usuario().isAdmin()).isFalse();
    }

    @Test
    void adminRecebeInternoEAdmin() {
        assertThat(autoridades(comPerfil("ADMIN"))).containsExactly("ROLE_INTERNO", "ROLE_ADMIN");
    }

    @Test
    void usuarioComumRecebeSoInterno() {
        assertThat(autoridades(comPerfil("USUARIO"))).containsExactly("ROLE_INTERNO");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "admin", " Admin "})
    void adminSeReconheceSemCaixaNemEspacos(String perfil) {
        assertThat(comPerfil(perfil).isAdmin()).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"USUARIO", "ADMN", "SUPERADMIN", "ADMIN;", "1"})
    void qualquerOutroValorNaoEAdmin(String perfil) {
        Usuario usuario = comPerfil(perfil);

        assertThat(usuario.isAdmin()).isFalse();
        assertThat(autoridades(usuario)).containsExactly("ROLE_INTERNO");
    }

    @Test
    void perfilNaoMudaAContaAtivaNemInativa() {
        Usuario admin = comPerfil("ADMIN");
        assertThat(admin.isEnabled()).isTrue();

        admin.setAtivo("N");
        assertThat(admin.isEnabled()).isFalse();
    }
}
