package com.citel.monitoramento_n8n.controller;


import com.citel.monitoramento_n8n.model.Cliente;
import com.citel.monitoramento_n8n.service.ClienteService;
import com.citel.monitoramento_n8n.DTO.ClienteCriadoDTO;
import com.citel.monitoramento_n8n.DTO.DadosCriacaoCliente;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @Operation(summary = "Cadastra um lojista no monitoramento",
            description = """
                    Cria o usuário de login do lojista (CADCLI). É pré-requisito do \
                    POST /integracoes, que vincula a integração a um lojista já existente.""")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Lojista cadastrado",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = ClienteCriadoDTO.class))}),
            @ApiResponse(responseCode = "409", description = "Já existe lojista com esse userName"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    @PostMapping
    public ResponseEntity<ClienteCriadoDTO> cadastrarCliente(@RequestBody DadosCriacaoCliente dados, UriComponentsBuilder uriBuilder)
    {
        Cliente novoCliente = service.criarCliente(dados);

        URI uri = uriBuilder.path("/clientes/{id}").buildAndExpand(novoCliente.getId()).toUri();

        return ResponseEntity.created(uri).body(ClienteCriadoDTO.de(novoCliente));
    }

}
