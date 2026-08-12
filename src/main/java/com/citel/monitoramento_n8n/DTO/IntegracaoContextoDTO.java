package com.citel.monitoramento_n8n.DTO;

import com.citel.monitoramento_n8n.model.Integracao;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Contexto completo do tenant, COM os segredos. Sai em dois lugares apenas:
 * o GET /integracoes/{slug} (protegido por x-ct-token) e o GET /integracoes com
 * incluirCredenciais=true.
 *
 * A chavePrivada vai exatamente como está no banco — o n8n assina RS256 com ela e qualquer
 * reformatação do PEM quebra a assinatura.
 */
public record IntegracaoContextoDTO(
        String codigoIntegracao,
        Long codigoCliente,
        String nomeCliente,
        String plataforma,
        String slug,
        boolean ativo,
        String urlApi,
        String apiToken,
        String refreshToken,
        String urlWebservice,
        @Schema(description = "Chave RSA em PEM, com as quebras de linha preservadas.")
        String chavePrivada
) {

    public static IntegracaoContextoDTO de(Integracao integracao) {
        return new IntegracaoContextoDTO(
                integracao.getCodigoIntegracao(),
                integracao.getCodigoCliente(),
                integracao.getCliente().getNome(),
                integracao.getPlataforma(),
                integracao.getSlug(),
                integracao.isAtivo(),
                integracao.getUrlApi(),
                integracao.getApiToken(),
                integracao.getRefreshToken(),
                integracao.getUrlWebservice(),
                integracao.getChavePrivada()
        );
    }
}
