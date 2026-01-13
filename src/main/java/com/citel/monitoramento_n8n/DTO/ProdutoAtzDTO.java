package com.citel.monitoramento_n8n.DTO;

import com.citel.monitoramento_n8n.model.ProdutoAtz;
import jakarta.persistence.Column;
import jakarta.persistence.Id;

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

    public ProdutoAtzDTO (ProdutoAtz produtoAtz){
        this(
                produtoAtz.getId(),
                produtoAtz.getNomeEmpresa(),
                produtoAtz.getNomePlataforma(),
                produtoAtz.getCodite(),
                produtoAtz.getIdPlataforma(),
                produtoAtz.getAcrescimoPreco(),
                produtoAtz.getVariacaoEstoque(),
                produtoAtz.getValorRecebido(),
                produtoAtz.getValorEnviado(),
                produtoAtz.getCodigoResposta(),
                produtoAtz.getTipoEnvio()
        );
    }




}
