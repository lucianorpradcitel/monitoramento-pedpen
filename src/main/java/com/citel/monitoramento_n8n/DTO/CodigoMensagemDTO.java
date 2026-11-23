package com.citel.monitoramento_n8n.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

/** Request do PATCH /produtos/{id}/tipo-erro e /pedidos/{id}/tipo-erro. */
public record CodigoMensagemDTO(

        @Schema(example = "12", description = "CADMSG.MSG_CODIGO do tipo do erro. Nulo desliga o tipo.")
        Integer codigoMensagem
) {
}
