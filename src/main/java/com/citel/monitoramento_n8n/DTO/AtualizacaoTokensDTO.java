package com.citel.monitoramento_n8n.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request do PATCH /integracoes/{codigoIntegracao}/tokens, usado pelos fluxos ADMIN_Tray_Token_*.
 *
 * Todos os campos são opcionais e nulo NÃO sobrescreve o valor existente — o Token_Refresh manda
 * só apiToken e refreshToken.
 */
@Schema(description = "Campos nulos ou ausentes preservam o valor que já está no banco.")
public record AtualizacaoTokensDTO(
        String apiToken,
        String refreshToken,
        String urlApi
) {
}
