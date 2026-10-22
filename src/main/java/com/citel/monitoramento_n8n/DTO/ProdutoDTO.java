package com.citel.monitoramento_n8n.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Date;


// id, dataErro e errStatus são ignorados pelo service: o id é gerado na entidade, a data vem do
// default da coluna e o status nasce 0. Ficam no record para não quebrar quem ainda os envia.
public record ProdutoDTO(
        @Schema(hidden = true)
        String id,
        String codigoProduto,
        String mensagemErro,
        @Schema(hidden = true)
        Date dataErro,
        String cliente,
        String plataforma,
        @Schema(hidden = true)
        int errStatus,
        /**
         * INT_CODAUT da integração que está reportando. Opcional: omitido, PRO_ID_INT fica nulo.
         * O service confere que o código pertence ao lojista autenticado antes de gravar.
         */
        String idIntegracao,
        String rotina,
        /**
         * Liberação do produto (PROERR.PRO_LIBERA). Opcional e case-insensitive.
         * Enviar 'N' remove o produto da liberação e carimba a mensagem de erro.
         * Omitido, a liberação do registro existente fica como está.
         */
        String libera
) {
}
