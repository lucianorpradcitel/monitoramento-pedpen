package com.citel.monitoramento_n8n.controller;


import com.citel.monitoramento_n8n.DTO.PedidoDTO;
import com.citel.monitoramento_n8n.DTO.ProdutoDTO;
import com.citel.monitoramento_n8n.DTO.ProdutoLoteDTO;
import com.citel.monitoramento_n8n.DTO.ProdutoRemovidoDTO;
import com.citel.monitoramento_n8n.model.Cliente;
import com.citel.monitoramento_n8n.model.Pedido;
import com.citel.monitoramento_n8n.model.Produto;
import com.citel.monitoramento_n8n.service.ProdutoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            description = """
                    Recebe os dados de um produto onde a comunicação com a plataforma falhou e o
                    salva com o status 'Pendente'. É um upsert pela tripla
                    codigoProduto + cliente + rotina: se o produto já existe, apenas soma uma tentativa.

                    O campo `libera` é opcional. Enviando `N`, o produto sai da liberação e a
                    mensagem de erro vigente recebe o sufixo ` - REMOVIDO DA LIBERACAO`, passando
                    a ficar de fora do GET /produtos padrão. O carimbo só é aplicado na transição
                    para `N`, então reenviar o mesmo POST não duplica o sufixo. Omitindo o campo,
                    a liberação do registro existente permanece como está.""")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Erro registrado com sucesso",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ProdutoDTO.class)) }),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })

    @PostMapping
    public ResponseEntity<Produto> registrarProduto(@RequestBody ProdutoDTO request,
                                                    @AuthenticationPrincipal Cliente cliente) {
        return ResponseEntity.ok(service.registrarProduto(request, cliente.getId()));
    }

    @Operation(summary = "Registra uma lista de produtos no monitoramento de erros de integração",
            description = "Recebe uma lista de produtos onde a comunicação com a plataforma falhou e os salva/atualiza em lote com o status 'Pendente'.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Produtos registrados com sucesso",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Produto.class)) }),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    @PostMapping("/lote")
    public ResponseEntity<List<Produto>> registrarProdutoList(@RequestBody List<ProdutoLoteDTO> request,
                                                              @AuthenticationPrincipal Cliente cliente) {
        return ResponseEntity.ok(service.registrarProdutosList(request, cliente.getId()));
    }


    @Operation(summary = "Lista todos os produtos com status 'Erro'",
            description = """
                    Retorna uma lista de todos os produtos que foram registrados com erros e que ainda
                    não foram resolvidos. Os filtros são combinados com E.

                    O número de tentativas é filtrado por comparação estrita, com as duas pontas
                    opcionais: `?tentativaMaiorQue=5` traz os que já falharam mais de 5 vezes,
                    `?tentativaMenorQue=5` os que falharam menos de 5 vezes, e as duas juntas
                    delimitam uma faixa (`?tentativaMaiorQue=2&tentativaMenorQue=6` traz 3, 4 e 5).

                    Sem o parâmetro `libera`, a lista traz todos os produtos que **não** estão
                    marcados com `PRO_LIBERA = 'N'` (inclusive os que estão nulos). Informando o
                    parâmetro, filtra pelo valor exato: `?libera=N` traz só os represados.""")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de produtos com atualização pendente encontrada",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Pedido.class)) }),
            @ApiResponse(responseCode = "422", description = "Faixa de tentativas impossível (ex.: maiorQue=5 com menorQue=5)"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    @GetMapping()
    public ResponseEntity<List<Produto>> retornarProdutosPendentes(
            @RequestParam(required = false) String codigoProduto,
            @RequestParam(required = false) String cliente,
            @RequestParam(required = false) String idIntegracao,
            @Parameter(description = "Só produtos com MAIS tentativas que este valor (exclusivo). Ex.: 5 traz 6 ou mais")
            @RequestParam(required = false) Integer tentativaMaiorQue,
            @Parameter(description = "Só produtos com MENOS tentativas que este valor (exclusivo). Ex.: 5 traz 4 ou menos")
            @RequestParam(required = false) Integer tentativaMenorQue,
            @Parameter(description = "Valor exato da PRO_LIBERA. Omitido, traz tudo que não for 'N' (nulos inclusos)")
            @RequestParam(required = false) String libera
    ) {
        return ResponseEntity.ok(service.retornarProdutosPendentes(
                codigoProduto, cliente, idIntegracao, tentativaMaiorQue, tentativaMenorQue, libera));
    }

    @Operation(summary = "Remove um produto do monitoramento",
            description = """
                    Apaga definitivamente os registros de erro do produto. A chave é
                    codigoProduto (PRO_CODITE) + idIntegracao (INT_CODAUT) + cliente (PRO_CLIENT):
                    a rotina NÃO entra na chave, então o produto é removido de todas as rotinas
                    em que aparecer, numa chamada só.

                    A remoção é definitiva e não tem desfazer - não há endpoint que recrie o
                    registro.

                    É idempotente: 200 com o corpo quando apagou alguma linha, 204 quando não
                    havia nada com aquela chave. Nos dois casos o produto está fora do
                    monitoramento ao fim da chamada, então reenviar o mesmo DELETE não é erro.""")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Produto removido; o corpo informa quantas linhas saíram",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ProdutoRemovidoDTO.class)) }),
            @ApiResponse(responseCode = "204", description = "Nenhuma linha casava com a chave - nada a remover"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    @DeleteMapping()
    public ResponseEntity<ProdutoRemovidoDTO> removerProduto(
            @Parameter(description = "PRO_CODITE - código do produto", required = true)
            @RequestParam String codigoProduto,
            @Parameter(description = "INT_CODAUT - código da integração", required = true)
            @RequestParam String idIntegracao,
            @Parameter(description = "PRO_CLIENT - código do lojista", required = true)
            @RequestParam String cliente
    ) {
        int removidos = service.removerProduto(codigoProduto, cliente, idIntegracao);
        if (removidos == 0) {
            // Não é erro: o estado pedido (produto fora do monitoramento) já valia antes da
            // chamada. 204 mantém o DELETE idempotente para o n8n reprocessar sem tratar falha.
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(
                new ProdutoRemovidoDTO(codigoProduto, cliente, idIntegracao, removidos));
    }
}
