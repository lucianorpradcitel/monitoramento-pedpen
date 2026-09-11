package com.citel.monitoramento_n8n.sync.controller;


import com.citel.monitoramento_n8n.sync.DTO.ShopifySyncCategoryRequest;
import com.citel.monitoramento_n8n.sync.DTO.ShopifySyncProductRequest;
import com.citel.monitoramento_n8n.sync.service.ShopifyCategoryService;
import com.citel.monitoramento_n8n.sync.service.ShopifyProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/sincronizacao")
@Tag(name= "Sincronização", description = "Sincronizações diversas")
public class ShopifyCategoryController {

    private final ShopifyCategoryService categoryService;
    private final ShopifyProductService productService;

    public ShopifyCategoryController(ShopifyCategoryService categoryService, ShopifyProductService productService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }


    /**
     * Dispara a sincronização e responde imediatamente.
     *
     * A expansão da taxonomia mais o envio ao ERP passam de nove mil categorias e levam dezenas de
     * minutos. Executando na thread da requisição, o cliente estourava timeout sem resposta alguma
     * e sem que o processamento parasse — dava a impressão de que o endpoint não fazia nada.
     */
    @Operation(summary = "Inicia a sincronização de categorias da Shopify com o ERP",
            description = """
                    O processamento roda em segundo plano e leva dezenas de minutos. A resposta \
                    confirma apenas que a execução foi aceita; o resultado é acompanhado pelos logs \
                    da aplicação, filtrando pelo execucaoId devolvido aqui.""")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Sincronização aceita e iniciada em segundo plano"),
            @ApiResponse(responseCode = "429", description = "Já há sincronizações suficientes na fila")
    })
    @PostMapping("/categorias-shopify")
    public ResponseEntity<Map<String, Object>> sincronizaCategorias(@RequestBody ShopifySyncCategoryRequest request)
    {
        String execucaoId = UUID.randomUUID().toString().substring(0, 8);

        try
        {
            categoryService.iniciarSincronizacaoAsync(request, execucaoId);

        } catch (TaskRejectedException e) {
            return corpo(HttpStatus.TOO_MANY_REQUESTS, execucaoId,
                    "Já existe sincronização em andamento e a fila está cheia. Tente novamente mais tarde.");
        }

        return corpo(HttpStatus.ACCEPTED, execucaoId,
                "Sincronização de categorias iniciada em segundo plano. Acompanhe pelos logs usando o execucaoId.");
    }

    @PostMapping("/produtos-shopify")
    public ResponseEntity<Void> sincronizaProdutos(@RequestBody ShopifySyncProductRequest request) {
        productService.iniciarSincronizacao(request);
        return ResponseEntity.accepted().build();
    }

    private ResponseEntity<Map<String, Object>> corpo(HttpStatus status, String execucaoId, String mensagem)
    {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("execucaoId", execucaoId);
        body.put("mensagem", mensagem);

        return ResponseEntity.status(status).body(body);
    }

}
