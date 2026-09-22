package com.citel.monitoramento_n8n.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

/** Resposta do DELETE /produtos: ecoa a chave usada e quantas linhas saíram. */
public record ProdutoRemovidoDTO(
        String codigoProduto,
        String cliente,
        String idIntegracao,
        @Schema(description = "Quantas linhas foram apagadas - uma por rotina em que o produto aparecia")
        int quantidadeRemovida
) {
}
