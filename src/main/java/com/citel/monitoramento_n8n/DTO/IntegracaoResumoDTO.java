package com.citel.monitoramento_n8n.DTO;

import com.citel.monitoramento_n8n.model.Integracao;

import java.time.LocalDateTime;

/**
 * Identidade da integração, sem nenhum segredo. É o default do GET /integracoes e a resposta do
 * PATCH /tokens.
 *
 * Não expõe chavePrivada, apiToken, refreshToken nem webhookToken.
 */
public record IntegracaoResumoDTO(
        String codigoIntegracao,
        Long codigoCliente,
        String nomeCliente,
        String plataforma,
        String slug,
        boolean ativo,
        String urlApi,
        String urlWebservice,
        LocalDateTime dataInclusao,
        LocalDateTime dataAlteracao
) {

    public static IntegracaoResumoDTO de(Integracao integracao) {
        return new IntegracaoResumoDTO(
                integracao.getCodigoIntegracao(),
                integracao.getCodigoCliente(),
                integracao.getCliente().getNome(),
                integracao.getPlataforma(),
                integracao.getSlug(),
                integracao.isAtivo(),
                integracao.getUrlApi(),
                integracao.getUrlWebservice(),
                integracao.getDataInclusao(),
                integracao.getDataAlteracao()
        );
    }
}
