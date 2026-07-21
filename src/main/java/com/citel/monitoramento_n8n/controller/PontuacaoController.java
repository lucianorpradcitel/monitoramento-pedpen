package com.citel.monitoramento_n8n.controller;


import com.citel.monitoramento_n8n.model.PontuacaoPendente;
import com.citel.monitoramento_n8n.service.PontuacaoPendenteService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pontuacao")
@Tag(name= "Monitoramento de Envio de Pontuações", description = "Endpoints para gerenciar envio do pontuações pendentes de Integração")
public class PontuacaoController {


    private final PontuacaoPendenteService service;
    public PontuacaoController(PontuacaoPendenteService service) {
        this.service = service;
    }

    @PostMapping("/ponpee")
    public ResponseEntity<PontuacaoPendente> registrarPedido(@RequestBody PontuacaoPendente request) {
        return ResponseEntity.ok(service.registrarPontuacao(request));
    }


    @GetMapping("/ponpee")
    public ResponseEntity<List<PontuacaoPendente>> retornarPontuacoesPendentes() {
        return ResponseEntity.ok(service.retornarPontuacoes());
    }

}
