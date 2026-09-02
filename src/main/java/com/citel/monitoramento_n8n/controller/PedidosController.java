package com.citel.monitoramento_n8n.controller;

import com.citel.monitoramento_n8n.DTO.PedidoDTO;
import com.citel.monitoramento_n8n.DTO.PedidoLoteDTO;
import com.citel.monitoramento_n8n.model.Cliente;
import com.citel.monitoramento_n8n.model.Pedido;
import com.citel.monitoramento_n8n.service.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
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
    @PostMapping("/pendentes")
    public ResponseEntity<Pedido> registrarPedido(@RequestBody PedidoDTO request,
                                                  @AuthenticationPrincipal Cliente cliente) {
        return ResponseEntity.ok(service.registrarPedido(request, cliente.getId()));
    }
    @PostMapping("pendentes-lote")
    public ResponseEntity<List<Pedido>> registrarPedidoList(@RequestBody List<PedidoLoteDTO> request,
                                                            @AuthenticationPrincipal Cliente cliente) {
        return ResponseEntity.ok(service.registrarPedidosList(request, cliente.getId()));
    }


    @Operation(summary = "Lista todos os pedidos com status 'Pendente'",
            description = "Retorna uma lista de todos os pedidos que foram registrados  e ainda não foram importados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de pedidos pendentes encontrada",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Pedido.class)) }),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    @GetMapping("/pendentes")
    public ResponseEntity<List<Pedido>> retornarPedidosPendentes(
            @RequestParam(required = false) String cliente,
            @RequestParam(required = false) String codigoPedido,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String idIntegracao,
            @RequestParam(required = false)  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return ResponseEntity.ok(service.retornarPedidosPendentes(cliente, codigoPedido, status, idIntegracao, data));
    }

    @Operation(summary = "Lista pedidos filtrando por um ou mais status",
            description = """
                    Diferente de /pendentes, que aceita um único status, aqui o parâmetro `status`
                    recebe uma lista e retorna os pedidos que estiverem em qualquer um deles.
                    Aceita tanto `?status=1,2,3` quanto `?status=1&status=2&status=3`.
                    Omitir o parâmetro traz todos os status. Os demais filtros são combinados
                    com E (ex.: `?status=1,2&cliente=chiareli`).""")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de pedidos encontrada",
                    content = { @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = Pedido.class))) }),
            @ApiResponse(responseCode = "400", description = "Parâmetro inválido (ex.: status não numérico)"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    @GetMapping("/pedidos")
    public ResponseEntity<List<Pedido>> retornarPedidos(
            @Parameter(description = "Status a incluir, separados por vírgula. Ex.: 1,2,3")
            @RequestParam(required = false) List<Integer> status,
            @RequestParam(required = false) String cliente,
            @RequestParam(required = false) String codigoPedido,
            @RequestParam(required = false) String idIntegracao,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return ResponseEntity.ok(service.retornarPedidos(status, cliente, codigoPedido, idIntegracao, data));
    }
}
