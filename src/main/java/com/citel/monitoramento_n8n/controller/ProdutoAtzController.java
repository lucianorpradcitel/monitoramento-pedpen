package com.citel.monitoramento_n8n.controller;


import com.citel.monitoramento_n8n.DTO.ProdutoAtzDTO;
import com.citel.monitoramento_n8n.model.ProdutoAtz;
import com.citel.monitoramento_n8n.service.ProdutoAtzService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/logs")
@Tag(name= "Log de atualização de preço e estoque da Integração", description = "Endpoints para acompanhamento  de preço e estoque da integração")
public class ProdutoAtzController {
    private final ProdutoAtzService service;

    public ProdutoAtzController(ProdutoAtzService service) {
        this.service = service;
    }



    @PostMapping
    public ResponseEntity<ProdutoAtz> registrarAtzProduto(@RequestBody ProdutoAtzDTO request) {
        return ResponseEntity.ok(service.registrarAtzProduto(request));
    }

}
