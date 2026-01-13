package com.citel.monitoramento_n8n.controller;


import com.citel.monitoramento_n8n.DTO.PedidoDTO;
import com.citel.monitoramento_n8n.DTO.ProdutoDTO;
import com.citel.monitoramento_n8n.model.Pedido;
import com.citel.monitoramento_n8n.model.Produto;
import com.citel.monitoramento_n8n.service.ProdutoService;
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
@RequestMapping("/produtos")
@Tag(name= "Monitoramento de Produtos de Integração", description = "Endpoints para acompanhamento de produtos de Integração")
public class ProdutoController {
    private final ProdutoService service;

    public ProdutoController(ProdutoService service) {
        this.service = service;
    }
    @Operation(summary = "Registra um novo produto no monitoramento de erros de integração",
            description = "Recebe os dados de um produto onde a  comunicação com a plataforma falhou e o salva com o status 'Pendente'.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Erro registrado com sucesso",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ProdutoDTO.class)) }),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })

    @PostMapping
    public ResponseEntity<Produto> registrarProduto(@RequestBody ProdutoDTO request) {
        try{
            return ResponseEntity.ok(service.registrarProduto(request));
        } catch (Exception e) {
            throw new RuntimeException("Ocorreu um erro inesperado ao cadastrar este produto na lista de produtos pendentes: " + e);
        }
    }




    @Operation(summary = "Lista todos os produtos com status 'Erro'",
            description = "Retorna uma lista de todos os produtos que foram registrados com erros e que ainda não foram resolvidos.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de produtos com atualização pendente encontrada",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Pedido.class)) }),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    @GetMapping()
    public ResponseEntity<List<Produto>> retornarProdutosPendentes() {
        try{
            return ResponseEntity.ok(service.retornarProdutosPendentes());
        } catch (Exception e) {
            throw new RuntimeException("Ocorreu um erro inesperado ao recuperar a lista dos produtos que estão pendentes de atualização: " + e);
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
    public ResponseEntity<Produto> atualizarPedido(@RequestBody ProdutoDTO request) {
        try{
            return service.registraComoResolvido(request.codigoProduto(), request.cliente(), request.errStatus(), request.mensagemErro())
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            throw new RuntimeException("Ocorreu um erro inesperado ao atualizar o status deste produto: " + e);
        }

    }
}
