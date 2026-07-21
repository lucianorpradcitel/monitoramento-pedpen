package com.citel.monitoramento_n8n.DTO;

import java.util.Date;


public record ProdutoDTO(
        String id,
        String codigoProduto,
        String mensagemErro,
        Date dataErro,
        String cliente,
        String plataforma,
        int errStatus,
        String idIntegracao,
        String rotina
) {
}
