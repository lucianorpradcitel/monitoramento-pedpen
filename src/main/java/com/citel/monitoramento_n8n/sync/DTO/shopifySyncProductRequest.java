package com.citel.monitoramento_n8n.sync.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data

public class shopifySyncProductRequest {

    @JsonProperty("webserviceErp")
    private String webserviceErp;

    @JsonProperty("tokenErp")
    private String tokenErp;

    @JsonProperty("shopifyURL")
    private String shopifyURL;

    @JsonProperty("shopifyApiKey")
    private String shopifyApiKey;

    // Getters e Setters (mesma estrutura do categoryRequest)
}