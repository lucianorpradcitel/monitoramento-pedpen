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
         * Gravado como veio, sem conferir na CADINT.
         */
        String idIntegracao,
        String rotina,
        /**
         * CADMSG.MSG_CODIGO do tipo do erro. Opcional: omitido junto com uma mensagem nova, o tipo
         * antigo é limpo (a mensagem mudou, o tipo pode não valer mais).
         */
        @Schema(description = "Tipo do erro (CADMSG.MSG_CODIGO), que define a responsabilidade (0 a 4). Opcional.")
        Integer codigoMensagem
) {
}
