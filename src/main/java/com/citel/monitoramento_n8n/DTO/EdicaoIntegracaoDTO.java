package com.citel.monitoramento_n8n.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

/**
 * Request do PATCH /integracoes/{codigoIntegracao}/{codigoCliente}.
 *
 * Só estes três campos são editáveis. Qualquer outro que vier no JSON (slug, plataforma, tokens...)
 * é descartado na desserialização, porque o record simplesmente não os tem.
 *
 * Campo ausente ou nulo preserva o valor que já está no banco. Texto vazio é recusado: os três
 * existem para a integração funcionar, e apagar um deles não é uma edição, é um erro.
 */
@Schema(description = "Campos ausentes ou nulos preservam o valor atual. Só estes três podem ser alterados.")
public record EdicaoIntegracaoDTO(

        @Schema(example = "http://159.112.189.1:25058", description = "Webservice do ERP.")
        @Pattern(regexp = "(?s).*\\S.*", message = "urlWebservice não pode ser vazia")
        String urlWebservice,

        @Schema(example = "https://loja.commercesuite.com.br/web_api", description = "URL da API da plataforma.")
        @Pattern(regexp = "(?s).*\\S.*", message = "urlApi não pode ser vazia")
        String urlApi,

        // (?s) liga o DOTALL para o .* casar as quebras de linha do PEM.
        @Schema(example = "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----")
        @Pattern(regexp = "(?s)^-----BEGIN.*", message = "chavePrivada deve estar em formato PEM e começar com -----BEGIN")
        String chavePrivada
) {
}
