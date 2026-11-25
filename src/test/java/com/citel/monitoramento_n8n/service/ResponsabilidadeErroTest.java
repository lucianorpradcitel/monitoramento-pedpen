package com.citel.monitoramento_n8n.service;

import com.citel.monitoramento_n8n.exception.ForbiddenException;
import com.citel.monitoramento_n8n.model.Usuario;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Quem enxerga qual responsabilidade: USR só 2 e 4; DEV, ADM e o n8n (principal nulo) veem tudo. */
class ResponsabilidadeErroTest {

    private static Usuario interno(String perfil) {
        Usuario usuario = new Usuario();
        usuario.setPerfil(perfil);
        return usuario;
    }

    @Test
    void lojistaDoN8nNaoEhRestringido() {
        assertThat(ResponsabilidadeErro.permitidas(null, null)).isNull();
        assertThat(ResponsabilidadeErro.permitidas(null, List.of(1, 0))).containsExactly(1, 0);
    }

    @Test
    void devEAdmRecebemOQuePediram() {
        assertThat(ResponsabilidadeErro.permitidas(interno("DEV"), List.of(1))).containsExactly(1);
        assertThat(ResponsabilidadeErro.permitidas(interno("ADM"), null)).isNull();
    }

    @Test
    void usuarioComumSemPedidoRecebe2E4() {
        assertThat(ResponsabilidadeErro.permitidas(interno("USR"), null)).containsExactly(2, 4);
        assertThat(ResponsabilidadeErro.permitidas(interno("USR"), List.of())).containsExactly(2, 4);
    }

    @Test
    void usuarioComumPodePedirSoO2OuO4() {
        assertThat(ResponsabilidadeErro.permitidas(interno("USR"), List.of(4))).containsExactly(4);
        assertThat(ResponsabilidadeErro.permitidas(interno("USR"), List.of(2, 4))).containsExactly(2, 4);
    }

    @Test
    void usuarioComumPedindoOutraLevaForbidden() {
        assertThatThrownBy(() -> ResponsabilidadeErro.permitidas(interno("USR"), List.of(1)))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> ResponsabilidadeErro.permitidas(interno("USR"), List.of(2, 3)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void perfilDesconhecidoCaiEmUsuarioComum() {
        // Na dúvida o acesso fecha: perfil inválido no banco nunca promove ninguém.
        assertThatThrownBy(() -> ResponsabilidadeErro.permitidas(interno("ADMIN"), List.of(1)))
                .isInstanceOf(ForbiddenException.class);
    }
}
