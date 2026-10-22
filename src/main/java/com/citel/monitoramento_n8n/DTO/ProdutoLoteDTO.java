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
     * Gravado como veio, sem conferir na CADINT: o n8n autentica o lote com o login de serviço,
     * que não é o dono das integrações.
     */
    @Schema(description = "Código da integração (CADINT.INT_CODAUT). Opcional.")
    private String idIntegracao;

    /**
     * PRO_STATUS. Opcional. Informado, o item só troca o status do produto já gravado (é o que o
     * monitor manda); omitido, o item é o registro de erro de sempre.
     */
    @Schema(description = """
            Opcional, 0 ou 1. Informado, o item só troca o status do produto já gravado com a mesma \
            chave (cliente + codigoProduto + rotina): mensagem, tentativas e demais campos ficam como \
            estão, e produto que não existe é ignorado. Omitido, o item registra o erro como sempre.""",
            allowableValues = {"0", "1"})
    private Integer status;

    // idIntegracao não entra aqui: o service grava depois, já normalizado (vazio vira nulo).
    public static Produto converterDTO(ProdutoLoteDTO dto, Produto pro) {
        pro.setCodigoProduto(dto.getCodigoProduto());
        pro.setCliente(dto.getCliente());
        pro.setPlataforma(dto.getPlataforma());
        pro.setErro(dto.getMensagemErro());
        pro.setRotina(dto.getRotina());

        return pro;
    }
}
