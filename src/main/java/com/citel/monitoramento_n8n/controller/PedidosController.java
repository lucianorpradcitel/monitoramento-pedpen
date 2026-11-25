package com.citel.monitoramento_n8n.controller;

import com.citel.monitoramento_n8n.DTO.CodigoMensagemDTO;
import com.citel.monitoramento_n8n.DTO.PedidoDTO;
import com.citel.monitoramento_n8n.DTO.PedidoLoteDTO;
import com.citel.monitoramento_n8n.model.Pedido;
import com.citel.monitoramento_n8n.model.Usuario;
import com.citel.monitoramento_n8n.service.ResponsabilidadeErro;
import com.citel.monitoramento_n8n.service.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
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
    public ResponseEntity<Pedido> registrarPedido(@RequestBody PedidoDTO request) {
        return ResponseEntity.ok(service.registrarPedido(request));
    }
    @Operation(summary = "Liga o pedido ao tipo de erro (responsabilidade 0 a 4)",
            description = """
                    Grava só o PEN_CODMSG (CADMSG.MSG_CODIGO): não mexe no status, no erro nem no \
                    sequencialProcessamento. É o último passo do n8n: grava no POST /pendentes, classifica \
                    no SUB_Error_Status e liga o tipo aqui, pelo id que o POST devolveu. codigoMensagem nulo desliga.""")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tipo ligado",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Pedido.class)) }),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado")
    })
    @PatchMapping("/pedidos/{id}/tipo-erro")
    public ResponseEntity<Pedido> definirTipoErro(@PathVariable String id,
                                                  @RequestBody(required = false) CodigoMensagemDTO dados) {
        return ResponseEntity.ok(service.definirTipoErro(id, dados == null ? null : dados.codigoMensagem()));
    }

    @PostMapping("pendentes-lote")
    public ResponseEntity<List<Pedido>> registrarPedidoList(@RequestBody List<PedidoLoteDTO> request) {
        return ResponseEntity.ok(service.registrarPedidosList(request));
    }


    @Operation(summary = "Lista os pedidos da fila de integração, filtrando por um ou mais status",
            description = """
                    O parâmetro `status` recebe uma lista e retorna os pedidos que estiverem em \
                    qualquer um deles. Aceita tanto `?status=1,2` quanto `?status=1&status=2`. \
                    Omitir o parâmetro traz todos os status. Os demais filtros são combinados \
                    com E (ex.: `?status=1,2&cliente=chiareli`).

                    `responsabilidade` filtra pelo tipo do erro (0 a 4; pedido sem tipo vale 0); omitido,
                    traz todas. Não é o `status`, que é a situação na fila.

                    O perfil USR só enxerga as responsabilidades 2 e 4: sem o parâmetro, vêm só essas, e pedir
                    outra é recusado com 403. DEV, ADM e o n8n enxergam todas.""")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de pedidos encontrada",
                    content = { @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = Pedido.class))) }),
            @ApiResponse(responseCode = "400", description = "Parâmetro inválido (ex.: status não numérico)"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    @GetMapping("/pendentes")
    public ResponseEntity<List<Pedido>> retornarPedidosPendentes(
            @RequestParam(required = false) String cliente,
            @RequestParam(required = false) String codigoPedido,
            @Parameter(description = "Status a incluir, separados por vírgula. Ex.: 1,2", explode = Explode.FALSE)
            @RequestParam(required = false) List<Integer> status,
            @RequestParam(required = false) String idIntegracao,
            @RequestParam(required = false)  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @Parameter(description = "Só erros destas responsabilidades (0 a 4): 0 não informado, 1 culpa nossa, 2 cliente, 3 instabilidade, 4 plataforma. Repita o parâmetro ou separe por vírgula. Omitido, traz todas")
            @RequestParam(required = false) List<Integer> responsabilidade,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(service.retornarPedidos(status, cliente, codigoPedido, idIntegracao, data,
                ResponsabilidadeErro.permitidas(usuario, responsabilidade)));
    }

    @Operation(summary = "Reprocessa um pedido com erro (usuário interno, admin ou comum)",
            description = """
                    Devolve o pedido à fila: o status volta a 0, que é o que o n8n busca para \
                    processar de novo. Só pedidos com erro (status 2) podem ser reprocessados; \
                    em qualquer outro status a resposta é 409. O sequencialProcessamento não muda.

                    Qualquer usuário interno (perfil ADM, DEV ou USR) pode; o lojista e quem não \
                    está logado recebem 403. O id vem no corpo do GET /pedidos e do GET /pendentes.""")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pedido de volta à fila (status 0)",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Pedido.class)) }),
            @ApiResponse(responseCode = "403", description = "Não é um usuário interno (lojista ou sem login)"),
            @ApiResponse(responseCode = "404", description = "Pedido inexistente"),
            @ApiResponse(responseCode = "409", description = "O pedido não está com erro"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    @PatchMapping("/pedidos/{id}/reprocessar")
    public ResponseEntity<Pedido> reprocessarPedido(@PathVariable String id,
                                                    @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(service.reprocessar(id, usuario));
    }

    @Operation(summary = "Lista pedidos filtrando por um ou mais status",
            description = """
                    Mesmo comportamento do GET /pendentes: o parâmetro `status` recebe uma lista
                    e retorna os pedidos que estiverem em qualquer um deles.
                    Aceita tanto `?status=1,2,3` quanto `?status=1&status=2&status=3`.
                    Omitir o parâmetro traz todos os status. Os demais filtros são combinados
                    com E (ex.: `?status=1,2&cliente=chiareli`).

                    `responsabilidade` filtra pelo tipo do erro (CADMSG.MSG_CODSTA): 0 não informado
                    (inclui o pedido ainda sem tipo), 1 culpa nossa, 2 cliente, 3 instabilidade,
                    4 plataforma externa; é outra coisa que o `status` (situação na fila). Cada pedido
                    traz o campo `responsabilidade`. Valor fora de 0 a 4 é recusado com 422.

                    O perfil USR só enxerga as responsabilidades 2 e 4: sem o parâmetro, vêm só essas, e pedir
                    outra é recusado com 403. DEV, ADM e o n8n enxergam todas.""")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de pedidos encontrada",
                    content = { @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = Pedido.class))) }),
            @ApiResponse(responseCode = "400", description = "Parâmetro inválido (ex.: status não numérico)"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    @GetMapping("/pedidos")
    public ResponseEntity<List<Pedido>> retornarPedidos(
            @Parameter(description = "Status a incluir, separados por vírgula. Ex.: 1,2,3", explode = Explode.FALSE)
            @RequestParam(required = false) List<Integer> status,
            @RequestParam(required = false) String cliente,
            @RequestParam(required = false) String codigoPedido,
            @RequestParam(required = false) String idIntegracao,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @Parameter(description = "Só erros destas responsabilidades (0 a 4): 0 não informado, 1 culpa nossa, 2 cliente, 3 instabilidade, 4 plataforma. Repita o parâmetro ou separe por vírgula. Omitido, traz todas")
            @RequestParam(required = false) List<Integer> responsabilidade,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(service.retornarPedidos(status, cliente, codigoPedido, idIntegracao, data,
                ResponsabilidadeErro.permitidas(usuario, responsabilidade)));
    }
}
