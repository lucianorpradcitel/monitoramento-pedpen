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
public class ShopifyCategoryService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ShopifyCategoryService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public void iniciarSincronizacao(ShopifySyncCategoryRequest request) {
        try {
            log.info("=== Iniciando sincronização de categorias Shopify ===");

            // 1. Buscar categorias raiz com childrenIds
            Map<String, CategoryWithChildren> mapaCompleto = buscarCategoriasComFilhos(
                    request.getShopifyURL(),
                    request.getShopifyApiKey()
            );

            log.info("Total de categorias raiz: {}", mapaCompleto.size());

            // 2. Expandir recursivamente todos os filhos
            List<ShopifyCategoryDTO> todasCategorias = expandirTodasCategorias(
                    mapaCompleto,
                    request.getShopifyURL(),
                    request.getShopifyApiKey()
            );

            log.info("Total de categorias após expansão: {}", todasCategorias.size());

            // 3. Ordenar por hierarquia
            List<ShopifyCategoryDTO> categoriasOrdenadas = ordenarPorHierarquia(todasCategorias);

            // 4. Sincronizar com ERP
            sincronizarComErp(
                    categoriasOrdenadas,
                    request.getWebserviceErp(),
                    request.getTokenErp()
            );

            log.info("=== Sincronização concluída com sucesso! ===");

        } catch (Exception e) {
            throw new RuntimeException("Erro ao sincronizar categorias: " + e.getMessage(), e);
        }
    }

    /**
     * Classe auxiliar para armazenar categoria com seus filhos
     */
    private static class CategoryWithChildren {
        ShopifyCategoryDTO categoria;
        List<String> childrenIds;

        CategoryWithChildren(ShopifyCategoryDTO cat, List<String> children) {
            this.categoria = cat;
            this.childrenIds = children;
        }
    }

    /**
     * Busca categorias raiz e extrai childrenIds
     */
    private Map<String, CategoryWithChildren> buscarCategoriasComFilhos(String shopifyURL, String apiKey) {
        Map<String, CategoryWithChildren> mapa = new HashMap<>();
        String cursor = null;
        boolean hasNextPage = true;

        try {
            while (hasNextPage) {
                String graphqlQuery = construirQueryComPaginacao(cursor);
                String response = executarQueryShopify(shopifyURL, apiKey, graphqlQuery);
                JsonNode root = objectMapper.readTree(response);

                JsonNode edges = root.path("data").path("taxonomy").path("categories").path("edges");

                for (JsonNode edge : edges) {
                    JsonNode node = edge.path("node");

                    // Criar categoria
                    ShopifyCategoryDTO categoria = new ShopifyCategoryDTO();
                    categoria.setId(node.path("id").asText());
                    categoria.setName(node.path("name").asText());

                    String parentId = node.path("parentId").asText();
                    if (parentId != null && !parentId.isEmpty() && !parentId.equals("null")) {
                        categoria.setParentId(parentId);
                    }

                    // Extrair childrenIds
                    List<String> childrenIds = new ArrayList<>();
                    JsonNode childrenNode = node.path("childrenIds");
                    if (childrenNode.isArray()) {
                        for (JsonNode childId : childrenNode) {
                            childrenIds.add(childId.asText());
                        }
                    }

                    mapa.put(categoria.getId(), new CategoryWithChildren(categoria, childrenIds));

                    log.info("Categoria: {} | Filhos: {}", categoria.getName(), childrenIds.size());
                }

                // Verificar próxima página
                JsonNode pageInfo = root.path("data").path("taxonomy").path("categories").path("pageInfo");
                hasNextPage = pageInfo.path("hasNextPage").asBoolean(false);
                cursor = hasNextPage ? pageInfo.path("endCursor").asText() : null;
            }

        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar categorias: " + e.getMessage(), e);
        }

        return mapa;
    }

    /**
     * Expande recursivamente todas as subcategorias
     */
    private List<ShopifyCategoryDTO> expandirTodasCategorias(
            Map<String, CategoryWithChildren> mapaInicial,
            String shopifyURL,
            String apiKey) {

        List<ShopifyCategoryDTO> todasCategorias = new ArrayList<>();
        Map<String, CategoryWithChildren> mapaCompleto = new HashMap<>(mapaInicial);
        Set<String> processados = new HashSet<>();

        // Adicionar categorias raiz
        for (CategoryWithChildren cwc : mapaInicial.values()) {
            todasCategorias.add(cwc.categoria);
            processados.add(cwc.categoria.getId());
        }

        // Processar filhos recursivamente
        Queue<String> filaIds = new LinkedList<>();

        // Adicionar todos os childrenIds à fila
        for (CategoryWithChildren cwc : mapaInicial.values()) {
            filaIds.addAll(cwc.childrenIds);
        }

        int nivel = 1;
        while (!filaIds.isEmpty()) {
            int tamanhoFila = filaIds.size();
            log.info("=== Processando nível {} ({} categorias) ===", nivel, tamanhoFila);

            // Processar em lotes de até 50 IDs por vez (limite da API)
            List<String> lote = new ArrayList<>();
            while (!filaIds.isEmpty() && lote.size() < 50) {
                String id = filaIds.poll();
                if (!processados.contains(id)) {
                    lote.add(id);
                }
            }

            if (lote.isEmpty()) {
                continue;
            }

            // Buscar categorias por IDs
            Map<String, CategoryWithChildren> categoriasBuscadas = buscarCategoriasPorIds(
                    lote,
                    shopifyURL,
                    apiKey
            );

            // Adicionar à lista e processar filhos
            for (CategoryWithChildren cwc : categoriasBuscadas.values()) {
                if (!processados.contains(cwc.categoria.getId())) {
                    todasCategorias.add(cwc.categoria);
                    processados.add(cwc.categoria.getId());
                    mapaCompleto.put(cwc.categoria.getId(), cwc);

                    // Adicionar filhos à fila
                    filaIds.addAll(cwc.childrenIds);

                    log.info("  → {} (filhos: {})", cwc.categoria.getName(), cwc.childrenIds.size());
                }
            }

            nivel++;

            // Proteção contra loop infinito
            if (nivel > 300) {
                log.error("AVISO: Limite de 300 níveis atingido. Interrompendo busca.");
                break;
            }
        }

        return todasCategorias;
    }

    /**
     * Busca múltiplas categorias por IDs específicos
     */
    private Map<String, CategoryWithChildren> buscarCategoriasPorIds(
            List<String> ids,
            String shopifyURL,
            String apiKey) {

        Map<String, CategoryWithChildren> resultado = new HashMap<>();

        try {
            // Construir query para buscar categorias específicas
            String graphqlQuery = construirQueryPorIds(ids);
            String response = executarQueryShopify(shopifyURL, apiKey, graphqlQuery);
            JsonNode root = objectMapper.readTree(response);

            // A resposta virá em nodes individuais
            for (int i = 0; i < ids.size(); i++) {
                String nodeKey = "node" + i;
                JsonNode node = root.path("data").path(nodeKey);

                if (!node.isMissingNode() && !node.isNull()) {
                    ShopifyCategoryDTO categoria = new ShopifyCategoryDTO();
                    categoria.setId(node.path("id").asText());
                    categoria.setName(node.path("name").asText());

                    String parentId = node.path("parentId").asText();
                    if (parentId != null && !parentId.isEmpty() && !parentId.equals("null")) {
                        categoria.setParentId(parentId);
                    }

                    // Extrair childrenIds
                    List<String> childrenIds = new ArrayList<>();
                    JsonNode childrenNode = node.path("childrenIds");
                    if (childrenNode.isArray()) {
                        for (JsonNode childId : childrenNode) {
                            childrenIds.add(childId.asText());
                        }
                    }

                    resultado.put(categoria.getId(), new CategoryWithChildren(categoria, childrenIds));
                }
            }

        } catch (Exception e) {
            log.error("Erro ao buscar categorias por IDs: {}", e.getMessage());
        }

        return resultado;
    }

    /**
     * Constrói query GraphQL para buscar categorias por IDs específicos
     */
    private String construirQueryPorIds(List<String> ids) {
        StringBuilder query = new StringBuilder("{\n");

        for (int i = 0; i < ids.size(); i++) {
            String alias = "node" + i;
            String id = ids.get(i);

            query.append(String.format("""
                  %s: node(id: "%s") {
                    ... on TaxonomyCategory {
                      id
                      name
                      fullName
                      parentId
                      childrenIds
                      isRoot
                      isLeaf
                      level
                    }
                  }
                """, alias, id));
        }

        query.append("}");

        return query.toString();
    }

    /**
     * Constrói query GraphQL com paginação para categorias raiz
     */
    private String construirQueryComPaginacao(String cursor) {
        if (cursor == null) {
            return """
                {
                  taxonomy {
                    categories(first: 250) {
                      edges {
                        node {
                          id
                          name
                          fullName
                          parentId
                          childrenIds
                          isRoot
                          isLeaf
                          level
                        }
                        cursor
                      }
                      pageInfo {
                        hasNextPage
                        endCursor
                      }
                    }
                  }
                }
                """;
        } else {
            return String.format("""
                {
                  taxonomy {
                    categories(first: 250, after: "%s") {
                      edges {
                        node {
                          id
                          name
                          fullName
                          parentId
                          childrenIds
                          isRoot
                          isLeaf
                          level
                        }
                        cursor
                      }
                      pageInfo {
                        hasNextPage
                        endCursor
                      }
                    }
                  }
                }
                """, cursor);
        }
    }

    /**
     * Executa query GraphQL na Shopify
     */
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

    private List<ShopifyCategoryDTO> ordenarPorHierarquia(List<ShopifyCategoryDTO> categorias) {
        log.info("=== Ordenando {} categorias por hierarquia ===", categorias.size());

        List<ShopifyCategoryDTO> ordenadas = new ArrayList<>();
        Set<String> processadas = new HashSet<>();

        int nivel = 0;
        int tentativasVazias = 0;

        while (ordenadas.size() < categorias.size() && nivel < 100) {
            int processadasNesteNivel = 0;

            for (ShopifyCategoryDTO categoria : categorias) {
                if (processadas.contains(categoria.getId())) {
                    continue;
                }

                // Nível 0: categorias raiz
                if (nivel == 0 && (categoria.getParentId() == null || categoria.getParentId().isEmpty())) {
                    ordenadas.add(categoria);
                    processadas.add(categoria.getId());
                    processadasNesteNivel++;
                }
                // Níveis seguintes: categorias cujo pai já foi processado
                else if (nivel > 0 && categoria.getParentId() != null && processadas.contains(categoria.getParentId())) {
                    ordenadas.add(categoria);
                    processadas.add(categoria.getId());
                    processadasNesteNivel++;
                }
            }

            if (processadasNesteNivel == 0) {
                tentativasVazias++;
                if (tentativasVazias > 3) {
                    log.error("AVISO: {} categorias órfãs (pais ausentes)", categorias.size() - ordenadas.size());
                    break;
                }
            } else {
                tentativasVazias = 0;
                log.info("Nível hierárquico {}: {} categorias", nivel, processadasNesteNivel);
            }

            nivel++;
        }

        return ordenadas;
    }

    private void sincronizarComErp(List<ShopifyCategoryDTO> categorias, String webserviceErp, String tokenErp) {
        log.info("=== Sincronizando {} categorias com ERP ===", categorias.size());

        Map<String, String> mapeamentoIds = new HashMap<>();
        int sucesso = 0;
        int falhas = 0;

        for (ShopifyCategoryDTO catShopify : categorias) {
            try {
                AutcomCategoryDTO catErp = new AutcomCategoryDTO();
                catErp.setCodigoExterno(catShopify.getId());
                catErp.setDescricao(catShopify.getName());

                // Definir categoria pai usando código ERP
                if (catShopify.getParentId() != null && !catShopify.getParentId().isEmpty()) {
                    String codigoErpPai = mapeamentoIds.get(catShopify.getParentId());
                    if (codigoErpPai != null) {
                        catErp.setCategoriaPai(codigoErpPai);
                    } else {
                        log.error("  ⚠ Pai não encontrado para: {}", catShopify.getName());
                        catErp.setCategoriaPai(null);
                    }
                } else {
                    catErp.setCategoriaPai(null);
                }

                // Enviar para ERP
                AutcomCategoryDTO resultado = enviarParaErp(catErp, webserviceErp, tokenErp);

                if (resultado != null && resultado.getCodigo() != null) {
                    mapeamentoIds.put(catShopify.getId(), resultado.getCodigo());
                    sucesso++;

                    String indent = catShopify.getParentId() != null ? "  → " : "";
                    log.info("{}[{}/{}] {} → {}", indent, sucesso, categorias.size(),
                            catShopify.getName(), resultado.getCodigo());
                }

            } catch (Exception e) {
                falhas++;
                log.error("  ✗ ERRO: {} - {}", catShopify.getName(), e.getMessage());
            }
        }

        log.info("=== Resumo: {} sucessos, {} falhas ===", sucesso, falhas);
    }

    private AutcomCategoryDTO enviarParaErp(AutcomCategoryDTO categoria, String webserviceErp, String tokenErp) {
        try {
            String baseUrl = webserviceErp;
            if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
                baseUrl = "http://" + baseUrl;
            }

            String url = baseUrl + "/V2/categorias";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", tokenErp);

            List<AutcomCategoryDTO> payload = Collections.singletonList(categoria);
            HttpEntity<List<AutcomCategoryDTO>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<AutcomCategoryDTO[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    AutcomCategoryDTO[].class
            );

            if (response.getBody() != null && response.getBody().length > 0) {
                return response.getBody()[0];
            }

            return null;

        } catch (Exception e) {
            throw new RuntimeException("Erro ao enviar para ERP: " + e.getMessage(), e);
        }
    }
}
