package com.citel.monitoramento_n8n.sync.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;

public class autcomProductMappingDTO {

    @JsonProperty("codigoExterno")
    private String codigoExterno;

    @JsonProperty("codigoInterno")
    private String codigoInterno;

    @JsonProperty("codigoPaiExterno")
    private String codigoPaiExterno;

    @JsonProperty("codigoPaiInterno")
    private String codigoPaiInterno;

    @JsonProperty("informacoesAdicionais")
    private Map<String, Object> informacoesAdicionais = new HashMap<>();

    // Getters e Setters
    public String getCodigoExterno() {
        return codigoExterno;
    }

    public void setCodigoExterno(String codigoExterno) {
        this.codigoExterno = codigoExterno;
    }

    public String getCodigoInterno() {
        return codigoInterno;
    }

    public void setCodigoInterno(String codigoInterno) {
        this.codigoInterno = codigoInterno;
    }

    public String getCodigoPaiExterno() {
        return codigoPaiExterno;
    }

    public void setCodigoPaiExterno(String codigoPaiExterno) {
        this.codigoPaiExterno = codigoPaiExterno;
    }

    public String getCodigoPaiInterno() {
        return codigoPaiInterno;
    }

    public void setCodigoPaiInterno(String codigoPaiInterno) {
        this.codigoPaiInterno = codigoPaiInterno;
    }

    public Map<String, Object> getInformacoesAdicionais() {
        return informacoesAdicionais;
    }

    public void setInformacoesAdicionais(Map<String, Object> informacoesAdicionais) {
        this.informacoesAdicionais = informacoesAdicionais;
    }
}