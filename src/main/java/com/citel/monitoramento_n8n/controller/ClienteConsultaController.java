package com.citel.monitoramento_n8n.controller;

import com.citel.monitoramento_n8n.DTO.ClienteResumoDTO;
import com.citel.monitoramento_n8n.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Consulta de lojistas, separada do ClienteController de propósito: o POST /cadastro está em
 * permitAll e esta consulta exige JWT, então manter as duas em classes distintas evita que uma
 * mudança de mapeamento arraste a outra para a superfície aberta.
 */
@RestController
@RequestMapping("/clientes")
@Tag(name = "Consulta de Lojistas", description = "Busca de lojistas do CADCLI para vincular no onboarding")
public class ClienteConsultaController {

    private final ClienteService service;

    public ClienteConsultaController(ClienteService service) {
        this.service = service;
    }

    @Operation(summary = "Lista lojistas para vincular a uma integração",
            description = """
                    Alimenta o seletor da tela de onboarding. O codigoCliente de cada item é o \
                    valor que vai no codigoCliente do POST /integracoes. Um mesmo lojista pode \
                    receber várias integrações.""")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lojistas encontrados",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = ClienteResumoDTO.class))}),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    @GetMapping
    public ResponseEntity<List<ClienteResumoDTO>> listarClientes(
            @RequestParam(required = false) String nome) {
        return ResponseEntity.ok(service.listar(nome));
    }
}
