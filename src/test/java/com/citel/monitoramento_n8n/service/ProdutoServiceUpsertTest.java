package com.citel.monitoramento_n8n.service;

import com.citel.monitoramento_n8n.DTO.ProdutoDTO;
import com.citel.monitoramento_n8n.model.Produto;
import com.citel.monitoramento_n8n.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Upsert do POST /produtos quando o registro já existe: soma a tentativa e a mensagem passa a
 * ser a mais recente (mensagem vazia mantém a gravada).
 */
@ExtendWith(MockitoExtension.class)
class ProdutoServiceUpsertTest {

    private static final String ROTINA = "MASTER_Tray_Produto_Item";
    private static final String MSG_NOVA = "Produto 04464: limite de produtos do plano da Tray atingido.";

    @Mock
    ProdutoRepository repository;

    ProdutoService service;
    Produto existente;

    @BeforeEach
    void preparar() {
        service = new ProdutoService(repository);

        existente = new Produto();
        existente.setCodigoProduto("04464");
        existente.setCliente("VENTURE ");
        existente.setRotina(ROTINA);
        existente.setIdIntegracao("0000004");
        existente.setTentativa(3186);

        when(repository.findByCodigoProdutoAndClienteAndRotina("04464", "VENTURE", ROTINA))
                .thenReturn(List.of(existente));
        when(repository.save(any(Produto.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private ProdutoDTO post(String mensagem) {
        return new ProdutoDTO(null, "04464", mensagem, null, "VENTURE", "TRAY", 0,
                "0000004", ROTINA, null);
    }

    @Test
    void trocaAMensagemEContaATentativa() {
        existente.setErro("Produto 04464: corrija o peso no cadastro do ERP.");

        Produto salvo = service.registrarProduto(post(MSG_NOVA));

        assertThat(salvo.getMensagemErro()).isEqualTo(MSG_NOVA);
        assertThat(salvo.getTentativa()).isEqualTo(3187);
    }

    @Test
    void mensagemVaziaMantemAGravada() {
        existente.setErro("Produto 04464: corrija o peso no cadastro do ERP.");

        Produto salvo = service.registrarProduto(post("  "));

        assertThat(salvo.getMensagemErro()).isEqualTo("Produto 04464: corrija o peso no cadastro do ERP.");
        assertThat(salvo.getTentativa()).isEqualTo(3187);
    }
}
