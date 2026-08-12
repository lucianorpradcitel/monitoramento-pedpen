package com.citel.monitoramento_n8n.DTO;

import com.citel.monitoramento_n8n.model.Produto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProdutoLoteDTO {

    private String codigoProduto;
    private String cliente;
    private String plataforma;
    private String mensagemErro;
    private String rotina;

    /**
     * INT_CODAUT da integração que está reportando. Opcional: omitido, PRO_ID_INT fica nulo.
     * O service confere que o código pertence ao lojista autenticado antes de gravar.
     */
    @Schema(description = "Código da integração (CADINT.INT_CODAUT). Opcional.")
    private String idIntegracao;

    // idIntegracao não entra aqui de propósito: quem grava é o service, depois de validar
    // o código contra a CADINT do lojista autenticado.
    public static Produto converterDTO(ProdutoLoteDTO dto, Produto pro) {
        pro.setCodigoProduto(dto.getCodigoProduto());
        pro.setCliente(dto.getCliente());
        pro.setPlataforma(dto.getPlataforma());
        pro.setErro(dto.getMensagemErro());
        pro.setRotina(dto.getRotina());

        return pro;
    }
}
