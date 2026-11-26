package com.citel.monitoramento_n8n.service;

import com.citel.monitoramento_n8n.DTO.ContagemErrosDTO;
import com.citel.monitoramento_n8n.exception.BusinessException;
import com.citel.monitoramento_n8n.model.Usuario;
import com.citel.monitoramento_n8n.repository.PedidosRepository;
import com.citel.monitoramento_n8n.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Contagem de erros por responsabilidade: as cinco para DEV, ADM e n8n; só 2 e 4 para o USR; zero onde não há erro. */
@ExtendWith(MockitoExtension.class)
class ContagemErrosServiceTest {

    @Mock
    ProdutoRepository produtos;
    @Mock
    PedidosRepository pedidos;

    ContagemErrosService service;

    @BeforeEach
    void preparar() {
        service = new ContagemErrosService(produtos, pedidos);
    }

    private static Usuario interno(String perfil) {
        Usuario usuario = new Usuario();
        usuario.setPerfil(perfil);
        return usuario;
    }

    private void prepararContagens() {
        when(produtos.contarPorResponsabilidade(0))
                .thenReturn(List.of(new Object[]{0, 1231L}, new Object[]{1, 9L}, new Object[]{2, 2L}));
        when(pedidos.contarPorResponsabilidade(2)).thenReturn(List.<Object[]>of(new Object[]{0, 155L}));
    }

    @Test
    void devVeAsCincoResponsabilidadesComZeroOndeNaoHaErro() {
        prepararContagens();

        ContagemErrosDTO contagem = service.contar(interno("DEV"), 0, 2);

        assertThat(contagem.produtos()).containsOnlyKeys(0, 1, 2, 3, 4);
        assertThat(contagem.produtos()).containsEntry(0, 1231L).containsEntry(1, 9L).containsEntry(2, 2L)
                .containsEntry(3, 0L).containsEntry(4, 0L);
        assertThat(contagem.pedidos()).containsEntry(0, 155L).containsEntry(1, 0L);
    }

    @Test
    void lojistaDoN8nTambemVeTudo() {
        prepararContagens();

        ContagemErrosDTO contagem = service.contar(null, 0, 2);

        assertThat(contagem.produtos()).containsOnlyKeys(0, 1, 2, 3, 4);
    }

    @Test
    void usuarioComumSoRecebe2E4() {
        prepararContagens();

        ContagemErrosDTO contagem = service.contar(interno("USR"), 0, 2);

        assertThat(contagem.produtos()).containsOnlyKeys(2, 4).containsEntry(2, 2L).containsEntry(4, 0L);
        assertThat(contagem.pedidos()).containsOnlyKeys(2, 4).containsEntry(2, 0L);
    }

    @Test
    void statusInvalidoERecusadoSemConsultarOBanco() {
        assertThatThrownBy(() -> service.contar(null, 2, 2))
                .isInstanceOf(BusinessException.class).hasMessageContaining("statusProduto");
        assertThatThrownBy(() -> service.contar(null, 0, 9))
                .isInstanceOf(BusinessException.class).hasMessageContaining("statusPedido");

        verifyNoInteractions(produtos, pedidos);
    }
}
