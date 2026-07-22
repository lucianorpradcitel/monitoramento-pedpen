package com.citel.monitoramento_n8n.DTO;

import com.citel.monitoramento_n8n.model.Produto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProdutoLoteDTO {

    private String codigoProduto;
    private String cliente;
    private String plataforma;
    private String mensagemErro;
    private String rotina;

    public static Produto converterDTO(ProdutoLoteDTO dto, Produto pro) {
        pro.setCodigoProduto(dto.getCodigoProduto());
        pro.setCliente(dto.getCliente());
        pro.setPlataforma(dto.getPlataforma());
        pro.setErro(dto.getMensagemErro());
        pro.setRotina(dto.getRotina());

        return pro;
    }
}
