package com.citel.monitoramento_n8n.sync.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;


@Data
public class shopifyProductVariantDTO {

    @JsonProperty("productId")
    private String productId;

    @JsonProperty("variantId")
    private String variantId;

    @JsonProperty("sku")
    private String sku;

    @JsonProperty("productTitle")
    private String productTitle;

    @JsonProperty("variantTitle")
    private String variantTitle;

    // Getters e Setters
}