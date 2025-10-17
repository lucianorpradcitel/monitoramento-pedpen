package com.citel.monitoramento_n8n.sync.service;

import com.citel.monitoramento_n8n.sync.DTO.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class shopifyCategoryService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public shopifyCategoryService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public void iniciarSincronizacao(shopifySyncCategoryRequest request) {
        try {
            System.out.println("=== Iniciando sincronização de categorias Shopify ===");

            // 1. Buscar categorias raiz com childrenIds
            Map<String, CategoryWithChildren> mapaCompleto = buscarCategoriasComFilhos(
                    request.getShopifyURL(),
                    request.getShopifyApiKey()
            );

            System.out.println("Total de categorias raiz: " + mapaCompleto.size());

            // 2. Expandir recursivamente todos os filhos
            List<shopifyCategoryDTO> todasCategorias = expandirTodasCategorias(
                    mapaCompleto,
                    request.getShopifyURL(),
                    request.getShopifyApiKey()
            );

            System.out.println("Total de categorias após expansão: " + todasCategorias.size());

            // 3. Ordenar por hierarquia
            List<shopifyCategoryDTO> categoriasOrdenadas = ordenarPorHierarquia(todasCategorias);

            // 4. Sincronizar com ERP
            sincronizarComErp(
                    categoriasOrdenadas,
                    request.getWebserviceErp(),
                    request.getTokenErp()
            );

            System.out.println("=== Sincronização concluída com sucesso! ===");

        } catch (Exception e) {
            throw new RuntimeException("Erro ao sincronizar categorias: " + e.getMessage(), e);
        }
    }

    /**
     * Classe auxiliar para armazenar categoria com seus filhos
     */
    private static class CategoryWithChildren {
        shopifyCategoryDTO categoria;
        List<String> childrenIds;

        CategoryWithChildren(shopifyCategoryDTO cat, List<String> children) {
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
                    shopifyCategoryDTO categoria = new shopifyCategoryDTO();
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

                    System.out.println("Categoria: " + categoria.getName() +
                            " | Filhos: " + childrenIds.size());
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
    private List<shopifyCategoryDTO> expandirTodasCategorias(
            Map<String, CategoryWithChildren> mapaInicial,
            String shopifyURL,
            String apiKey) {

        List<shopifyCategoryDTO> todasCategorias = new ArrayList<>();
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
            System.out.println("\n=== Processando nível " + nivel + " (" + tamanhoFila + " categorias) ===");

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

                    System.out.println("  → " + cwc.categoria.getName() +
                            " (filhos: " + cwc.childrenIds.size() + ")");
                }
            }

            nivel++;

            // Proteção contra loop infinito
            if (nivel > 300) {
                System.err.println("AVISO: Limite de 300 níveis atingido. Interrompendo busca.");
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
                    shopifyCategoryDTO categoria = new shopifyCategoryDTO();
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
            System.err.println("Erro ao buscar categorias por IDs: " + e.getMessage());
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

    private List<shopifyCategoryDTO> ordenarPorHierarquia(List<shopifyCategoryDTO> categorias) {
        System.out.println("\n=== Ordenando " + categorias.size() + " categorias por hierarquia ===");

        List<shopifyCategoryDTO> ordenadas = new ArrayList<>();
        Map<String, shopifyCategoryDTO> mapaCategoria = categorias.stream()
                .collect(Collectors.toMap(shopifyCategoryDTO::getId, c -> c));

        Set<String> processadas = new HashSet<>();

        int nivel = 0;
        int tentativasVazias = 0;

        while (ordenadas.size() < categorias.size() && nivel < 100) {
            int processadasNesteNivel = 0;

            for (shopifyCategoryDTO categoria : categorias) {
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
                    System.err.println("AVISO: " + (categorias.size() - ordenadas.size()) +
                            " categorias órfãs (pais ausentes)");
                    break;
                }
            } else {
                tentativasVazias = 0;
                System.out.println("Nível hierárquico " + nivel + ": " + processadasNesteNivel + " categorias");
            }

            nivel++;
        }

        return ordenadas;
    }

    private void sincronizarComErp(List<shopifyCategoryDTO> categorias, String webserviceErp, String tokenErp) {
        System.out.println("\n=== Sincronizando " + categorias.size() + " categorias com ERP ===");

        Map<String, String> mapeamentoIds = new HashMap<>();
        int sucesso = 0;
        int falhas = 0;

        for (shopifyCategoryDTO catShopify : categorias) {
            try {
                autcomCategoryDTO catErp = new autcomCategoryDTO();
                catErp.setCodigoExterno(catShopify.getId());
                catErp.setDescricao(catShopify.getName());

                // Definir categoria pai usando código ERP
                if (catShopify.getParentId() != null && !catShopify.getParentId().isEmpty()) {
                    String codigoErpPai = mapeamentoIds.get(catShopify.getParentId());
                    if (codigoErpPai != null) {
                        catErp.setCategoriaPai(codigoErpPai);
                    } else {
                        System.err.println("  ⚠ Pai não encontrado para: " + catShopify.getName());
                        catErp.setCategoriaPai(null);
                    }
                } else {
                    catErp.setCategoriaPai(null);
                }

                // Enviar para ERP
                autcomCategoryDTO resultado = enviarParaErp(catErp, webserviceErp, tokenErp);

                if (resultado != null && resultado.getCodigo() != null) {
                    mapeamentoIds.put(catShopify.getId(), resultado.getCodigo());
                    sucesso++;

                    String indent = catShopify.getParentId() != null ? "  → " : "";
                    System.out.println(indent + "[" + sucesso + "/" + categorias.size() + "] " +
                            catShopify.getName() + " → " + resultado.getCodigo());
                }

            } catch (Exception e) {
                falhas++;
                System.err.println("  ✗ ERRO: " + catShopify.getName() + " - " + e.getMessage());
            }
        }

        System.out.println("\n=== Resumo: " + sucesso + " sucessos, " + falhas + " falhas ===");
    }

    private autcomCategoryDTO enviarParaErp(autcomCategoryDTO categoria, String webserviceErp, String tokenErp) {
        try {
            String baseUrl = webserviceErp;
            if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
                baseUrl = "http://" + baseUrl;
            }

            String url = baseUrl + "/V2/categorias";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", tokenErp);

            List<autcomCategoryDTO> payload = Collections.singletonList(categoria);
            HttpEntity<List<autcomCategoryDTO>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<autcomCategoryDTO[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    autcomCategoryDTO[].class
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