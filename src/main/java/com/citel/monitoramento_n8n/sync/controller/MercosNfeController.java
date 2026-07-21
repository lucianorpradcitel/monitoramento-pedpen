package com.citel.monitoramento_n8n.sync.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mercos")
@Tag(name = "Mercos Helpers", description = "Sincronizações diversas")
public class MercosNfeController {

    /**
     * @deprecated Endpoint descontinuado. Não é mais processado; responde 410 Gone.
     */
    @Deprecated
    @Operation(
            summary = "[DESCONTINUADO] Envio de NF-e para a Mercos",
            description = "Endpoint descontinuado e não é mais processado. Sempre responde 410 Gone.",
            deprecated = true
    )
    @PostMapping("/nfe")
    public ResponseEntity<Void> enviarNotaFiscal() {
        return ResponseEntity.status(HttpStatus.GONE).build();
    }
}
