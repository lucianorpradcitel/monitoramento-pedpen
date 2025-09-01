package com.citel.monitoramento_n8n.controller;

import com.citel.monitoramento_n8n.DTO.PedidoDTO;
import com.citel.monitoramento_n8n.model.Pedido;
import com.citel.monitoramento_n8n.service.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pendentes")
@Tag(name= "Monitoramento de Pedidos de Integração", description = "Endpoints para gerenciar pedidos pendentes de Integração")
public class PedidosController {

    private final PedidoService service;

    public PedidosController(PedidoService service) {
        this.service = service;
    }

    @Operation(summary = "Registra um novo pedido na fila de integração",
            description = "Recebe os dados de um pedido onde será feita a importação e o salva com o status 'Pendente'.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Erro registrado com sucesso",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Pedido.class)) }),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    @PostMapping
    public ResponseEntity<Pedido> registrarPedido(@RequestBody PedidoDTO request) {
        try{
            return ResponseEntity.ok(service.registrarPedido(request));
        } catch (Exception e) {
            throw new RuntimeException("Ocorreu um erro inesperado ao cadastrar este pedido na lista de pedidos pendentes: " + e);
        }
    }

    @Operation(summary = "Lista todos os pedidos com status 'Pendente'",
            description = "Retorna uma lista de todos os pedidos que foram registrados  e ainda não foram importados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de pedidos pendentes encontrada",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Pedido.class)) }),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    @GetMapping()
    public ResponseEntity<List<Pedido>> retornarPedidosPendentes() {
        try{
            return ResponseEntity.ok(service.retornarPedidosPendentes());
        } catch (Exception e) {
            throw new RuntimeException("Ocorreu um erro inesperado ao recuperar a lista dos pedidos que estão pendentes: " + e);
        }
    }

    @Operation(summary = "Atualiza o status de um pedido para 'Integrado'",
            description = "Busca um pedido pelo código e cliente e, se encontrado, atualiza seu status para 'Integrado'.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status do Pedido atualizado com sucesso",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Pedido.class)) }),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado com o código e cliente informados"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    @PatchMapping()
    public ResponseEntity<Pedido> atualizarPedido(@RequestBody PedidoDTO request) {
        try{
            return service.registraComoIntegrado(request.getCodigoPedido(), request.getCliente())
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            throw new RuntimeException("Ocorreu um erro inesperado ao atualizar o status deste pedido: " + e);
        }

    }
}