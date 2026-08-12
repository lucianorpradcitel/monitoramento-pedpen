package com.citel.monitoramento_n8n.DTO;

import com.citel.monitoramento_n8n.model.Integracao;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Resposta do 201. É o único lugar em que o webhookToken aparece — depois disso ele não é mais
 * devolvido por nenhum endpoint.
 *
 * O nomeCliente vem do CADCLI resolvido pela FK: confirma ao implantador que o código do ERP
 * vinculou o lojista certo.
 */
public record IntegracaoCriadaDTO(
        String codigoIntegracao,
        Long codigoCliente,
        String nomeCliente,
        String plataforma,
        String slug,
        boolean ativo,
        @Schema(description = "Valor que o n8n envia no header x-ct-token. Exibido uma única vez.")
        String webhookToken,
        LocalDateTime dataInclusao
) {

    public static IntegracaoCriadaDTO de(Integracao integracao) {
        return new IntegracaoCriadaDTO(
                integracao.getCodigoIntegracao(),
                integracao.getCodigoCliente(),
                integracao.getCliente().getNome(),
                integracao.getPlataforma(),
                integracao.getSlug(),
                integracao.isAtivo(),
                integracao.getWebhookToken(),
                integracao.getDataInclusao()
        );
    }
}
