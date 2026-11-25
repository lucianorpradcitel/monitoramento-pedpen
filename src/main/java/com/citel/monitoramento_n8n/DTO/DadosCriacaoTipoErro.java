package com.citel.monitoramento_n8n.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request do POST /tipos-erro. Validado e origem não vêm do corpo: o service decide pelo login
 * (n8n grava como IA e não validado; admin pelo portal grava como usuário e já validado).
 * Campos a mais no JSON são ignorados.
 */
public record DadosCriacaoTipoErro(

        @Schema(example = "1005", description = "PLA_SISEXT da plataforma (CADPLA).")
        @NotBlank(message = "plataforma é obrigatória")
        String plataforma,

        @Schema(example = "categoria_ausente", description = "Código estável do erro, nunca o texto da mensagem.")
        @NotBlank(message = "codigoErro é obrigatório")
        @Size(max = 60, message = "codigoErro tem no máximo 60 caracteres")
        String codigoErro,

        @Schema(example = "3", description = "STA_CODIGO da STAERR: 0 não informado, 1 culpa nossa, 2 culpa do cliente, 3 instabilidade, 4 culpa da plataforma externa.")
        @NotNull(message = "status é obrigatório")
        Integer status,

        @Schema(example = "Produto sem categoria no ERP.", description = "Mensagem real de exemplo (cortada em 255).")
        String exemplo,

        @Schema(example = "a1b2c3", description = "PRO_IDERRO ou PEN_IDPED_ da linha que gerou o tipo (opcional).")
        String idExemplo
) {
}
