package com.citel.monitoramento_n8n.service;

import com.citel.monitoramento_n8n.exception.BusinessException;
import com.citel.monitoramento_n8n.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * DELETE /produtos: sem rotina remove o produto de todas as rotinas (varredor das 5 tentativas);
 * com rotina remove só aquela, preservando o erro ainda aberto de outra rotina do mesmo produto.
 */
@ExtendWith(MockitoExtension.class)
class ProdutoServiceRemocaoTest {

    private static final String ROTINA = "MASTER_Shopify_Produto_Imagem";

    @Mock
    ProdutoRepository repository;
    @Mock
    IntegracaoService integracaoService;

    ProdutoService service;

    @BeforeEach
    void preparar() {
        service = new ProdutoService(repository, integracaoService);
    }

    @Test
    void semRotinaRemoveTodasAsRotinas() {
        when(repository.removerTodasAsRotinas("0226895", "LOJA", "0000004")).thenReturn(2);

        assertThat(service.removerProduto("0226895", "LOJA", "0000004", null)).isEqualTo(2);
        verify(repository, never()).removerDaRotina(any(), any(), any(), any());
    }

    @Test
    void comRotinaRemoveSoAquelaRotina() {
        when(repository.removerDaRotina("0226895", "LOJA", "0000004", ROTINA)).thenReturn(1);

        assertThat(service.removerProduto("0226895", "LOJA", "0000004", ROTINA)).isEqualTo(1);
        verify(repository, never()).removerTodasAsRotinas(any(), any(), any());
    }

    @Test
    void rotinaEmBrancoERecusadaSemApagarNada() {
        // ?rotina= vindo de expressão vazia no n8n não pode cair no "apaga todas as rotinas".
        assertThatThrownBy(() -> service.removerProduto("0226895", "LOJA", "0000004", "  "))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(repository);
    }
}
