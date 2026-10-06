package com.citel.monitoramento_n8n.DTO;

import com.citel.monitoramento_n8n.model.Integracao;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * O que a tela de edição precisa: a identidade da integração (somente leitura) e os três campos
 * editáveis, com a chave privada. Não traz apiToken, refreshToken nem webhookToken — a edição não
 * mexe neles.
 *
 * Sai só em rotas de ADMIN: GET e PATCH de /integracoes/{codigoIntegracao}/{codigoCliente}.
 */
public record IntegracaoEdicaoDTO(
        String codigoIntegracao,
        Long codigoCliente,
        String nomeCliente,
        String plataforma,
        String slug,
        boolean ativo,
        String urlWebservice,
        String urlApi,
        @Schema(description = "Chave RSA em PEM, com as quebras de linha preservadas.")
        String chavePrivada
) {

    public static IntegracaoEdicaoDTO de(Integracao integracao) {
        return new IntegracaoEdicaoDTO(
                integracao.getCodigoIntegracao(),
                integracao.getCodigoCliente(),
                integracao.getCliente().getNome(),
                integracao.getPlataforma(),
                integracao.getSlug(),
                integracao.isAtivo(),
                integracao.getUrlWebservice(),
                integracao.getUrlApi(),
                integracao.getChavePrivada()
        );
    }
}
