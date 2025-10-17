package com.citel.monitoramento_n8n.sync.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;

public class shopifySyncCategoryRequest {

    @JsonProperty("webserviceErp")
    private String webserviceErp;

    @JsonProperty("tokenErp")
    private String tokenErp;

    @JsonProperty("shopifyURL")
    private String shopifyURL;

    @JsonProperty("shopifyApiKey")
    private String shopifyApiKey;

    // Getters e Setters
    public String getWebserviceErp()
    {
        return webserviceErp;
    }
    public void setWebserviceErp(String webserviceErp)
    {
        this.webserviceErp = webserviceErp;
    }
    public String getTokenErp()
    {
        return tokenErp;
    }
    public void setTokenErp(String tokenErp)
    {
        this.tokenErp = tokenErp;
    }

    public String getShopifyURL()
    {
        return shopifyURL;
    }

    public void setShopifyURL(String shopifyURL)
    {
        this.shopifyURL = shopifyURL;
    }

    public String getShopifyApiKey()
    {
        return shopifyApiKey;
    }

    public void setShopifyApiKey(String shopifyApiKey)
    {
        this.shopifyApiKey = shopifyApiKey;
    }
}

