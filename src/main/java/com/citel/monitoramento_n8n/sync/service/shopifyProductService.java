package com.citel.monitoramento_n8n.sync.service;

import com.citel.monitoramento_n8n.sync.DTO.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class shopifyProductService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public shopifyProductService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    private String padLeft(String value, int length) {
        if (value == null) {
            value = "";
        }
        return String.format("%0" + length + "d", Integer.parseInt(value.replaceAll("[^0-9]", "")));
    }

    public void iniciarSincronizacao(shopifySyncProductRequest request) {
        try {
            System.out.println("=== Iniciando sincronização de produtos Shopify ===");

            // 1. Buscar todos os produtos com paginação
            List<shopifyProductVariantDTO> variants = buscarTodosProdutos(
                    request.getShopifyURL(),
                    request.getShopifyApiKey()
            );

            System.out.println("Total de variantes encontradas: " + variants.size());

            // 2. Sincronizar com ERP
            sincronizarComErp(
                    variants,
                    request.getWebserviceErp(),
                    request.getTokenErp()
            );

            System.out.println("=== Sincronização concluída com sucesso! ===");

        } catch (Exception e) {
            throw new RuntimeException("Erro ao sincronizar produtos: " + e.getMessage(), e);
        }
    }

    private List<shopifyProductVariantDTO> buscarTodosProdutos(String shopifyURL, String apiKey) {
        List<shopifyProductVariantDTO> allVariants = new ArrayList<>();
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
                        shopifyProductVariantDTO variant = new shopifyProductVariantDTO();
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

                System.out.println("Processadas " + allVariants.size() + " variantes...");
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
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Shopify-Access-Token", apiKey);

            Map<String, String> body = new HashMap<>();
            body.put("query", query);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

            String url = shopifyURL;
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            return response.getBody();

        } catch (Exception e) {
            throw new RuntimeException("Erro ao executar query Shopify: " + e.getMessage(), e);
        }
    }

    private void sincronizarComErp(List<shopifyProductVariantDTO> variants, String webserviceErp, String tokenErp) {
        System.out.println("\n=== Sincronizando " + variants.size() + " variantes com ERP ===");

        int sucesso = 0;
        int falhas = 0;

        for (shopifyProductVariantDTO variant : variants) {
            try {
                enviarParaErp(variant, webserviceErp, tokenErp);
                sucesso++;
                System.out.println("[" + sucesso + "/" + variants.size() + "] SKU: " + variant.getSku());

            } catch (Exception e) {
                falhas++;
                System.err.println("✗ ERRO SKU " + variant.getSku() + ": " + e.getMessage());
            }
        }

        System.out.println("\n=== Resumo: " + sucesso + " sucessos, " + falhas + " falhas ===");
    }

    private void enviarParaErp(shopifyProductVariantDTO variant, String webserviceErp, String tokenErp) {
        try {
            String baseUrl = webserviceErp;
            if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
                baseUrl = "http://" + baseUrl;
            }

            String url = baseUrl + "/V2/mapeamento-produtos";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", tokenErp);

            // Montar objeto conforme esperado pela API
            autcomProductMappingDTO mapping = new autcomProductMappingDTO();
            mapping.setCodigoExterno(variant.getVariantId());

            // Padronizar SKU com 5 dígitos (zeros à esquerda)
            String skuPadded = String.format("%05d", Integer.parseInt(variant.getSku()));
            mapping.setCodigoInterno(skuPadded);

            mapping.setCodigoPaiExterno(variant.getProductId());
            mapping.setCodigoPaiInterno(null);
            mapping.setInformacoesAdicionais(new HashMap<>());

            List<autcomProductMappingDTO> payload = Collections.singletonList(mapping);
            HttpEntity<List<autcomProductMappingDTO>> entity = new HttpEntity<>(payload, headers);

            restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        } catch (Exception e) {
            throw new RuntimeException("Erro ao enviar para ERP: " + e.getMessage(), e);
        }
    }
    }
