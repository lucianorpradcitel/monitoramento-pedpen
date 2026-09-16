package com.citel.monitoramento_n8n.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Request do POST /plataformas. */
public record DadosCriacaoPlataforma(

        @Schema(example = "tray", description = "Identificador gravado em CADINT.INT_NOMPLA e usado nas regras de negócio da integração.")
        @NotBlank(message = "descricao é obrigatória")
        String descricao,

        @Schema(example = "1001", description = "Código da plataforma no sistema externo.")
        @NotBlank(message = "sistemaExterno é obrigatório")
        String sistemaExterno
) {
}
