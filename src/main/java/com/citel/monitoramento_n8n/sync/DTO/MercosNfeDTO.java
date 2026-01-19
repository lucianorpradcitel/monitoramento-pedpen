package com.citel.monitoramento_n8n.sync.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MercosNfeDTO {

        @JsonProperty("xmlUrl")
        private String xmlUrl;

        @JsonProperty("pdfUrl")
        private String pdfUrl;

        @JsonProperty("nota_fiscal")
        private Object nota_fiscal;

        @JsonProperty("applicationToken")
        private String applicationToken;

        @JsonProperty("companyToken")
        private String companyToken;

        @JsonProperty("urlMercos")
        private String urlMercos;

        @JsonProperty("jwtWs")
        private String jwtWs;
}

