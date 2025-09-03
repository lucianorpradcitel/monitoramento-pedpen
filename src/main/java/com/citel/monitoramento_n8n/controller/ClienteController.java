package com.citel.monitoramento_n8n.controller;


import com.citel.monitoramento_n8n.model.Cliente;
import com.citel.monitoramento_n8n.service.ClienteService;
import com.citel.monitoramento_n8n.DTO.DadosCriacaoCliente;
import com.citel.monitoramento_n8n.service.PedidoService;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.net.URI;


@RestController
@RequestMapping("/cadastro")
@Tag(name= "Cadastro do Lojista", description = "Endpoint para cadastrar lojista no monitoramento de integrações")

public class ClienteController {
    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Cliente> cadastrarCliente(@RequestBody DadosCriacaoCliente dados, UriComponentsBuilder uriBuilder)
    {
        Cliente novoCliente = service.criarCliente(dados);

        URI uri = uriBuilder.path("/clientes/{id}").buildAndExpand(novoCliente.getId()).toUri();

        return ResponseEntity.created(uri).body(novoCliente);
    }

}
