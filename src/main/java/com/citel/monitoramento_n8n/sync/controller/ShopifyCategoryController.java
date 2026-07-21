package com.citel.monitoramento_n8n.sync.controller;


import com.citel.monitoramento_n8n.sync.DTO.ShopifySyncCategoryRequest;
import com.citel.monitoramento_n8n.sync.DTO.ShopifySyncProductRequest;
import com.citel.monitoramento_n8n.sync.service.ShopifyCategoryService;
import com.citel.monitoramento_n8n.sync.service.ShopifyProductService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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


    @PostMapping("/categorias-shopify")
    public ResponseEntity<Void> sincronizaCategorias(@RequestBody ShopifySyncCategoryRequest request)
    {
        categoryService.iniciarSincronizacao(request);
        return ResponseEntity.accepted().build();


    }

    @PostMapping("/produtos-shopify")
    public ResponseEntity<Void> sincronizaProdutos(@RequestBody ShopifySyncProductRequest request) {
        productService.iniciarSincronizacao(request);
        return ResponseEntity.accepted().build();
    }

}
