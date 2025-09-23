package com.citel.monitoramento_n8n.DTO;

import com.citel.monitoramento_n8n.model.Produto;
import java.util.Date;


public record ProdutoDTO(
        // Definição com 7 campos e seus tipos
        String id,
        String codigoProduto,
        String mensagemErro,
        Date dataErro,
        String cliente,
        String plataforma,
        int errStatus
) {


    public ProdutoDTO(Produto produto) {
        this(
                produto.getId(),
                produto.getCodigoProduto(),
                produto.getErro(),
                produto.getDataErro(), //
                produto.getCliente(),
                produto.getPlataforma(),
                produto.getStatus()
        );
    }
}
