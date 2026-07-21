package com.citel.monitoramento_n8n.sync.service;

import com.citel.monitoramento_n8n.sync.DTO.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
public class ShopifyProductService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ShopifyProductService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    private String padLeft(String value, int length) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return null;
        }
        return String.format("%0" + length + "d", new java.math.BigInteger(digits));
    }
    public void iniciarSincronizacao(ShopifySyncProductRequest request) {
        try {
            log.info("=== Iniciando sincronização de produtos Shopify ===");

            // 1. Buscar todos os produtos com paginação
            List<ShopifyProductVariantDTO> variants = buscarTodosProdutos(
                    request.getShopifyURL(),
                    request.getShopifyApiKey()
            );

            log.info("Total de variantes encontradas: {}", variants.size());

            // 2. Sincronizar com ERP
            sincronizarComErp(
                    variants,
                    request.getWebserviceErp(),
                    request.getTokenErp()
            );

            log.info("=== Sincronização concluída com sucesso! ===");

        } catch (Exception e) {
            throw new RuntimeException("Erro ao sincronizar produtos: " + e.getMessage(), e);
        }
    }

    private List<ShopifyProductVariantDTO> buscarTodosProdutos(String shopifyURL, String apiKey) {
        List<ShopifyProductVariantDTO> allVariants = new ArrayList<>();
        String cursor = null;
        boolean hasNextPage = true;

        try {
            while (hasNextPage) {
                String graphqlQuery = construirQueryComPaginacao(cursor);
                String response = executarQueryShopify(shopifyURL, apiKey, graphqlQuery);
                JsonNode root = objectMapper.readTree(response);

                JsonNode edges = root.path("data").path("products").path("edges");

                for (JsonNode edge : edges) {
                    JsonNode productNode = edge.path("node");
                    String productId = productNode.path("id").asText();
                    String productTitle = productNode.path("title").asText();

                    JsonNode variantsNode = productNode.path("variants").path("nodes");

                    for (JsonNode variantNode : variantsNode) {
                        ShopifyProductVariantDTO variant = new ShopifyProductVariantDTO();
                        variant.setProductId(productId);
                        variant.setVariantId(variantNode.path("id").asText());
                        variant.setSku(variantNode.path("sku").asText());
                        variant.setProductTitle(productTitle);
                        variant.setVariantTitle(variantNode.path("title").asText());

                        allVariants.add(variant);
                    }
                }

                // Verificar próxima página
                JsonNode pageInfo = root.path("data").path("products").path("pageInfo");
                hasNextPage = pageInfo.path("hasNextPage").asBoolean(false);
                cursor = hasNextPage ? pageInfo.path("endCursor").asText() : null;

                log.info("Processadas {} variantes...", allVariants.size());
            }

        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar produtos: " + e.getMessage(), e);
        }

        return allVariants;
    }

    private String construirQueryComPaginacao(String cursor) {
        if (cursor == null) {
            return """
                    {
                      products(first: 250) {
                        edges {
                          node {
                            id
                            title
                            variants(first: 100) {
                              nodes {
                                id
                                sku
                                title
                              }
                            }
                          }
                        }
                        pageInfo {
                          hasNextPage
                          endCursor
                        }
                      }
                    }
                    """;
        } else {
            return String.format("""
                    {
                      products(first: 250, after: "%s") {
                        edges {
                          node {
                            id
                            title
                            variants(first: 100) {
                              nodes {
                                id
                                sku
                                title
                              }
                            }
                          }
                        }
                        pageInfo {
                          hasNextPage
                          endCursor
                        }
                      }
                    }
                    """, cursor);
        }
    }

    private String executarQueryShopify(String shopifyURL, String apiKey, String query) {
        String url = shopifyURL;
        if (url != null && !url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Shopify-Access-Token", apiKey);

            Map<String, String> body = new HashMap<>();
            body.put("query", query);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            log.info(">>> [SHOPIFY] URL: {}", url);
            log.info(">>> [SHOPIFY] Status: {}", response.getStatusCode());

            return response.getBody();

        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            log.error(">>> [SHOPIFY] ERRO {}", e.getStatusCode());
            log.error(">>> [SHOPIFY] Body: {}", e.getResponseBodyAsString());
            log.error(">>> [SHOPIFY] URL chamada: {}", url);
            throw new RuntimeException("Erro Shopify: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao executar query Shopify: " + e.getMessage(), e);
        }
    }
    private void sincronizarComErp(List<ShopifyProductVariantDTO> variants, String webserviceErp, String tokenErp) {
        log.info("=== Sincronizando {} variantes com ERP ===", variants.size());

        int sucesso = 0;
        int falhas = 0;

        for (ShopifyProductVariantDTO variant : variants) {
            try {
                enviarParaErp(variant, webserviceErp, tokenErp);
                sucesso++;
                log.info("[{}/{}] SKU: {}", sucesso, variants.size(), variant.getSku());

            } catch (Exception e) {
                falhas++;
                log.error("✗ ERRO SKU {}: {}", variant.getSku(), e.getMessage());
            }
        }

        log.info("=== Resumo: {} sucessos, {} falhas ===", sucesso, falhas);
    }

    private void enviarParaErp(ShopifyProductVariantDTO variant, String webserviceErp, String tokenErp) {
        String baseUrl = webserviceErp;
        if (baseUrl != null && !baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            baseUrl = "http://" + baseUrl;
        }
        String url = baseUrl + "/V2/mapeamento-produtos";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", tokenErp);

            // Montar objeto conforme esperado pela API
            AutcomProductMappingDTO mapping = new AutcomProductMappingDTO();
            mapping.setCodigoExterno(variant.getVariantId());

            // Padronizar SKU com 5 dígitos (zeros à esquerda) — usa padLeft pra não quebrar com SKU não-numérico/vazio
            String skuPadded = padLeft(variant.getSku(), 5);
            if (skuPadded == null) {
                throw new RuntimeException("SKU inválido ou vazio: '" + variant.getSku() + "'");
            }
            mapping.setCodigoInterno(skuPadded);

            mapping.setCodigoPaiExterno(variant.getProductId());
            mapping.setCodigoPaiInterno(null);
            mapping.setInformacoesAdicionais(new HashMap<>());

            List<AutcomProductMappingDTO> payload = Collections.singletonList(mapping);
            HttpEntity<List<AutcomProductMappingDTO>> entity = new HttpEntity<>(payload, headers);

            log.info(">>> [ERP] URL: {}", url);
            log.info(">>> [ERP] Authorization presente? {}", (tokenErp != null && !tokenErp.isBlank()));
            log.info(">>> [ERP] Payload: {}", objectMapper.writeValueAsString(payload));

            restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            log.error(">>> [ERP] ERRO {}", e.getStatusCode());
            log.error(">>> [ERP] Body: {}", e.getResponseBodyAsString());
            log.error(">>> [ERP] URL chamada: {}", url);
            throw new RuntimeException("ERP " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao enviar para ERP: " + e.getMessage(), e);
        }
    }
}
