package com.citel.monitoramento_n8n.DTO;

public record ProdutoAtzDTO(
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
