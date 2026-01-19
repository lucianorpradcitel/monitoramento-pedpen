package com.citel.monitoramento_n8n.sync.controller;

import com.citel.monitoramento_n8n.sync.DTO.MercosNfeDTO;
import com.citel.monitoramento_n8n.sync.DTO.MercosNfeResponseDTO;
import com.citel.monitoramento_n8n.sync.service.MercosNfeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mercos")
@RequiredArgsConstructor
@Tag(name= "Mercos Helpers", description = "Sincronizações diversas")

public class MercosNfeController {

    private final MercosNfeService mercosNfeService;

    @PostMapping("/nfe")
    public ResponseEntity<MercosNfeResponseDTO> enviarNotaFiscal(@RequestBody MercosNfeDTO request) {

        MercosNfeResponseDTO response = mercosNfeService.enviarNotaFiscal(request);

        return ResponseEntity
                .status(response.getStatusCode())
                .body(response);
    }
}