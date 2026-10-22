package com.citel.monitoramento_n8n.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProdutoAtzDTO(
        // Ignorado pelo service: o id é gerado na entidade.
        @Schema(hidden = true)
        String id,
        String nomeEmpresa,
        String nomePlataforma,
        String codite,
        String idPlataforma,
        String acrescimoPreco,
        String variacaoEstoque,
        String valorRecebido,
        String valorEnviado,
        String codigoResposta,
        String tipoEnvio
) {
}
