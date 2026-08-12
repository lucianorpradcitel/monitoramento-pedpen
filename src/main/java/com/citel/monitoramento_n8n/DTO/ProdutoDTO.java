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
        /**
         * INT_CODAUT da integração que está reportando. Opcional: omitido, PRO_ID_INT fica nulo.
         * O service confere que o código pertence ao lojista autenticado antes de gravar.
         */
        String idIntegracao,
        String rotina
) {
}
