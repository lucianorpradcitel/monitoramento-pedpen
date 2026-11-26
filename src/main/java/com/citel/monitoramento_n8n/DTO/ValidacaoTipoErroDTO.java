package com.citel.monitoramento_n8n.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request do PATCH /tipos-erro/{codigo}. Sem status, só confirma o que já está gravado;
 * com status, reclassifica e confirma. Nos dois casos o tipo fica validado.
 */
public record ValidacaoTipoErroDTO(

        @Schema(example = "2", description = "Novo status (0 a 4, o STA_CODIGO da STAERR). Omitir para só confirmar o atual.")
        Integer status
) {
}
