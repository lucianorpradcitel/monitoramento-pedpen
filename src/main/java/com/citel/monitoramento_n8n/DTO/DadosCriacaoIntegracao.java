package com.citel.monitoramento_n8n.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request do POST /integracoes.
 *
 * Não recebe apiToken/refreshToken: quem preenche é o OAuth, pelo PATCH /tokens.
 * Não recebe webhookToken: é gerado no servidor.
 * Não recebe consumerKey/consumerSecret: vêm do /V2/config do webservice e não são guardados aqui.
 */
public record DadosCriacaoIntegracao(

        @Schema(example = "0000123", description = """
                Código de autorização desta integração no ERP. É da integração, não do lojista: \
                quem tem Tray e Mercos cadastra duas, com códigos diferentes.""")
        @NotBlank(message = "codigoIntegracao é obrigatório")
        @Size(min = 7, max = 7, message = "codigoIntegracao deve ter exatamente 7 caracteres")
        String codigoIntegracao,

        @Schema(example = "7", description = """
                CLI_CODCLI do lojista dono da integração. Obrigatório porque o código de \
                autorização pode se repetir entre lojistas.""")
        @NotNull(message = "codigoCliente é obrigatório")
        Long codigoCliente,

        @Schema(example = "tray", allowableValues = {"tray", "mercos"})
        @NotBlank(message = "plataforma é obrigatória")
        @Pattern(regexp = "tray|mercos", message = "plataforma deve ser 'tray' ou 'mercos'")
        String plataforma,

        @Schema(example = "casa-furadeiras")
        @NotBlank(message = "slug é obrigatório")
        @Pattern(regexp = "^[a-z0-9-]{3,40}$", message = "slug deve conter apenas letras minúsculas, números e hífen, entre 3 e 40 caracteres")
        String slug,

        @Schema(example = "https://loja.commercesuite.com.br/web_api")
        String urlApi,

        @Schema(example = "http://159.112.189.1:25058", description = "Webservice do ERP.")
        @NotBlank(message = "urlWebservice é obrigatória")
        String urlWebservice,

        // (?s) liga o DOTALL para o .* casar as quebras de linha do PEM.
        @Schema(example = "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----")
        @NotBlank(message = "chavePrivada é obrigatória")
        @Pattern(regexp = "(?s)^-----BEGIN.*", message = "chavePrivada deve estar em formato PEM e começar com -----BEGIN")
        String chavePrivada
) {
}
