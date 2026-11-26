package com.citel.monitoramento_n8n.controller;

import com.citel.monitoramento_n8n.DTO.ContagemErrosDTO;
import com.citel.monitoramento_n8n.model.Usuario;
import com.citel.monitoramento_n8n.service.ContagemErrosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/erros")
@Tag(name = "Contagem de erros", description = "Totais de erros de integração por responsabilidade")
public class ContagemErrosController {

    private final ContagemErrosService service;

    public ContagemErrosController(ContagemErrosService service) {
        this.service = service;
    }

    @Operation(summary = "Conta os erros de produto e de pedido por responsabilidade",
            description = """
                    Devolve, para produtos e para pedidos, quantos erros há em cada responsabilidade (0 não
                    informado, que inclui o erro ainda sem tipo, 1 culpa nossa, 2 culpa do cliente, 3 instabilidade,
                    4 culpa da plataforma externa). Serve para a tela mostrar o total de cada visão sem baixar as listas.

                    Por padrão conta os produtos ainda não resolvidos (`statusProduto=0`) e os pedidos com erro
                    (`statusPedido=2`). O perfil USR só recebe as responsabilidades 2 e 4; DEV, ADM e o n8n recebem
                    as cinco. Responsabilidade sem erro vem com 0.""")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contagem por responsabilidade",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = ContagemErrosDTO.class))}),
            @ApiResponse(responseCode = "422", description = "statusProduto diferente de 0 e 1, ou statusPedido fora de 0 a 3"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    @GetMapping("/contagem")
    public ResponseEntity<ContagemErrosDTO> contar(
            @Parameter(description = "PRO_STATUS dos produtos a contar: 0 ou 1. Omitido, vale 0")
            @RequestParam(defaultValue = "0") int statusProduto,
            @Parameter(description = "PEN_STATUS dos pedidos a contar: 0 a 3. Omitido, vale 2 (com erro)")
            @RequestParam(defaultValue = "2") int statusPedido,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(service.contar(usuario, statusProduto, statusPedido));
    }
}
