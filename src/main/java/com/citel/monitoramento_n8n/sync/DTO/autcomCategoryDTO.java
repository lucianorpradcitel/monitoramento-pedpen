package com.citel.monitoramento_n8n.sync.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;

public class autcomCategoryDTO {

    // Campo que o ERP retorna como "codigo"
    @JsonProperty("codigo")
    private String codigo;

    @JsonProperty("categoriaPai")
    private String categoriaPai;

    @JsonProperty("codigoExterno")
    private String codigoExterno;

    @JsonProperty("descricao")
    private String descricao;

    // Getters e Setters
    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getCategoriaPai() {
        return categoriaPai;
    }

    public void setCategoriaPai(String categoriaPai) {
        this.categoriaPai = categoriaPai;
    }

    public String getCodigoExterno() {
        return codigoExterno;
    }

    public void setCodigoExterno(String codigoExterno) {
        this.codigoExterno = codigoExterno;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
}