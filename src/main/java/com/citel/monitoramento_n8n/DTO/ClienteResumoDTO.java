package com.citel.monitoramento_n8n.DTO;

import com.citel.monitoramento_n8n.model.Cliente;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Identidade do lojista para a tela de onboarding escolher quem vincular.
 *
 * Cliente implementa UserDetails e carrega CLI_SENCLI — por isso a consulta nunca devolve a
 * entidade, só este recorte. Também não expõe o userName: nome e código já desambiguam.
 *
 * Não expõe CLI_CODAUT de propósito. O código de autorização é da integração, não do lojista, e
 * mostrá-lo aqui sugeriria que ele serve para preencher o codigoIntegracao do POST — não serve.
 */
public record ClienteResumoDTO(
        @Schema(description = "CLI_CODCLI. É o valor que vai no codigoCliente do POST /integracoes.")
        Long codigoCliente,
        String nome
) {

    public static ClienteResumoDTO de(Cliente cliente) {
        return new ClienteResumoDTO(cliente.getId(), cliente.getNome());
    }
}
