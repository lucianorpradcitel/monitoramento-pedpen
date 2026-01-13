package com.citel.monitoramento_n8n.sync.controller;


import com.citel.monitoramento_n8n.sync.DTO.shopifySyncCategoryRequest;
import com.citel.monitoramento_n8n.sync.DTO.shopifySyncProductRequest;
import com.citel.monitoramento_n8n.sync.service.shopifyCategoryService;
import com.citel.monitoramento_n8n.sync.service.shopifyProductService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sincronizacao")
@Tag(name= "Sincronização", description = "Sincronizações diversas")
public class shopifyCategoryController {

    private final shopifyCategoryService categoryService;
    private final shopifyProductService productService;

    public shopifyCategoryController(shopifyCategoryService categoryService,     shopifyProductService productService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }


    @PostMapping("/categorias-shopify")
    public ResponseEntity<Void> sincronizaCategorias(@RequestBody shopifySyncCategoryRequest request)
    {
        categoryService.iniciarSincronizacao(request);
        return ResponseEntity.accepted().build();


    }

    @PostMapping("/produtos-shopify")
    public ResponseEntity<Void> sincronizaProdutos(@RequestBody shopifySyncProductRequest request) {
        productService.iniciarSincronizacao(request);
        return ResponseEntity.accepted().build();
    }

}
