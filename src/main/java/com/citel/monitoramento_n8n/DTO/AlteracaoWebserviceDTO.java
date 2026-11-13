package com.citel.monitoramento_n8n.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request do PATCH /integracoes/{codigoIntegracao}/{codigoCliente}/webservice.
 *
 * Só o webservice do ERP: é o que qualquer usuário interno pode trocar. URL da API e chave privada continuam no PATCH
 * de ADMIN (EdicaoIntegracaoDTO), e qualquer outro campo que vier no JSON é descartado, porque o record não o tem.
 */
public record AlteracaoWebserviceDTO(

        @Schema(example = "http://159.112.189.1:25058", description = "Webservice do ERP.")
        @NotBlank(message = "urlWebservice é obrigatória")
        String urlWebservice
) {
}
