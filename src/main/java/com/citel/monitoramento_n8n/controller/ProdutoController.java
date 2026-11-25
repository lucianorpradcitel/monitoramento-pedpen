package com.citel.monitoramento_n8n.controller;


import com.citel.monitoramento_n8n.DTO.CodigoMensagemDTO;
import com.citel.monitoramento_n8n.DTO.FiltrosProdutosDTO;
import com.citel.monitoramento_n8n.DTO.PaginaDTO;
import com.citel.monitoramento_n8n.DTO.ProdutoDTO;
import com.citel.monitoramento_n8n.DTO.ProdutoLoteDTO;
import com.citel.monitoramento_n8n.DTO.ProdutoRemovidoDTO;
import com.citel.monitoramento_n8n.model.Produto;
import com.citel.monitoramento_n8n.model.Usuario;
import com.citel.monitoramento_n8n.service.ProdutoService;
import com.citel.monitoramento_n8n.service.ResponsabilidadeErro;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
                    codigoProduto + cliente + rotina: se o produto já existe, soma uma tentativa e
                    troca a mensagem de erro pela enviada (mensagem vazia mantém a gravada).""")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Erro registrado com sucesso",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Produto.class)) }),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })

    @PostMapping
    public ResponseEntity<Produto> registrarProduto(@RequestBody ProdutoDTO request) {
        return ResponseEntity.ok(service.registrarProduto(request));
    }

    @Operation(summary = "Liga o erro do produto ao tipo de erro (responsabilidade 0 a 4)",
            description = """
                    Grava só o PRO_CODMSG (CADMSG.MSG_CODIGO): não soma tentativa e não mexe na mensagem. \
                    É o último passo do n8n: grava o erro no POST /produtos, classifica no SUB_Error_Status \
                    e liga o tipo aqui, pelo id que o POST devolveu. codigoMensagem nulo desliga.""")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tipo ligado",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Produto.class)) }),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado")
    })
    @PatchMapping("/{id}/tipo-erro")
    public ResponseEntity<Produto> definirTipoErro(@PathVariable String id,
                                                   @RequestBody(required = false) CodigoMensagemDTO dados) {
        return ResponseEntity.ok(service.definirTipoErro(id, dados == null ? null : dados.codigoMensagem()));
    }

    @Operation(summary = "Registra uma lista de produtos no monitoramento de erros de integração",
            description = """
                    Recebe vários produtos numa chamada. A chave de cada um é
                    cliente + codigoProduto + rotina, e cada item segue um de dois caminhos:

                    - **sem `status`**: registro de erro (upsert). Produto novo nasce com status 0 e
                      uma tentativa; produto já gravado soma uma tentativa e troca a mensagem. O mesmo
                      produto repetido no lote cai numa linha só.
                    - **com `status` (0 ou 1)**: é o que o monitor manda. Só o status do produto já
                      gravado muda; mensagem, tentativas e demais campos ficam como estão. Produto
                      que não existe é ignorado e não sai na resposta.

                    A resposta traz os produtos gravados, um por chave.""")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Produtos gravados, um por chave",
                    content = { @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = Produto.class))) }),
            @ApiResponse(responseCode = "422", description = "status diferente de 0 e 1. Nada é gravado"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    @PostMapping("/lote")
    public ResponseEntity<List<Produto>> registrarProdutoList(@RequestBody List<ProdutoLoteDTO> request) {
        return ResponseEntity.ok(service.registrarProdutosList(request));
    }


    @Operation(summary = "Lista os clientes e as plataformas que têm produto com erro, para montar os filtros",
            description = """
                    Retorna, sem repetir e em ordem alfabética, os clientes e as plataformas que têm
                    ao menos um produto no status pedido (`status` 0 ou 1; omitido, vale 0). Serve
                    para preencher os filtros da tela sem baixar a lista de produtos; a lista em si,
                    paginada, vem do GET /produtos.

                    Os dois filtros se restringem um ao outro: com `plataforma`, `clientes` traz só
                    os que têm produto naquela plataforma; com `cliente`, `plataformas` traz só as
                    em que ele tem produto. Cada lista ignora o próprio filtro.""")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Clientes e plataformas com produto no status",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = FiltrosProdutosDTO.class)) }),
            @ApiResponse(responseCode = "400", description = "status em formato inválido"),
            @ApiResponse(responseCode = "422", description = "status diferente de 0 e 1"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    @GetMapping("/filtros")
    public ResponseEntity<FiltrosProdutosDTO> retornarFiltros(
            @Parameter(description = "PRO_STATUS a consultar: 0 ou 1. Omitido, vale 0")
            @RequestParam(defaultValue = "0") int status,
            @Parameter(description = "Restringe as plataformas às em que este cliente (exato) tem produto")
            @RequestParam(required = false) String cliente,
            @Parameter(description = "Restringe os clientes aos que têm produto nesta plataforma (exata)")
            @RequestParam(required = false) String plataforma
    ) {
        return ResponseEntity.ok(service.retornarFiltros(status, cliente, plataforma));
    }

    @Operation(summary = "Lista os produtos com erro de integração, filtrando pelo status e paginando",
            description = """
                    Retorna uma página dos produtos registrados com erro que estão no status pedido,
                    junto com o total de registros que atendem aos filtros. Os filtros são
                    combinados com E.

                    O `status` aceita 0 ou 1. Omitido, vale 0: traz os que ainda não foram
                    resolvidos, como antes de o parâmetro existir. `?status=1` traz os que estão com
                    PRO_STATUS 1. Qualquer outro valor é recusado com 422.

                    O número de tentativas é filtrado por comparação estrita, com as duas pontas
                    opcionais: `?tentativaMaiorQue=5` traz os que já falharam mais de 5 vezes,
                    `?tentativaMenorQue=5` os que falharam menos de 5 vezes, e as duas juntas
                    delimitam uma faixa (`?tentativaMaiorQue=2&tentativaMenorQue=6` traz 3, 4 e 5).

                    O período filtra a data do erro, com as duas pontas inclusivas e opcionais, em
                    data e hora ISO sem fuso (`?dataInicial=2026-11-05T08:00:00&dataFinal=2026-11-05T18:00:00`).
                    `dataInicial` depois de `dataFinal` é recusada com 422.

                    `produtoContem` e `erroContem` pesquisam por trecho, sem distinguir maiúsculas,
                    em todos os registros e não só na página atual. `cliente` e `plataforma` são
                    exatos; as opções válidas vêm do GET /produtos/filtros.

                    `responsabilidade` filtra pelo tipo do erro (CADMSG.MSG_CODSTA): 0 não informado
                    (inclui o erro ainda sem tipo), 1 culpa nossa, 2 culpa do cliente, 3 instabilidade,
                    4 culpa da plataforma externa. Aceita mais de um valor (`?responsabilidade=1&responsabilidade=0`
                    ou `?responsabilidade=1,0`); omitido, traz todas. Cada produto da resposta traz o campo
                    `responsabilidade`. Valor fora de 0 a 4 é recusado com 422.

                    O perfil USR só enxerga as responsabilidades 2 e 4: sem o parâmetro, vêm só essas, e pedir
                    outra é recusado com 403. DEV, ADM e o n8n enxergam todas.

                    A resposta é paginada: `pagina` começa em 1 (padrão 1) e `tamanho` aceita 50, 100
                    ou 1000 (padrão 50), ou 0 para trazer todos os registros numa única página.
                    Qualquer outro valor é recusado com 422. Com `tamanho=0` a `pagina` é ignorada e
                    a consulta pode devolver milhares de registros. A ordem é pela data
                    do erro: `ordem=desc` (padrão) traz o mais recente primeiro e `ordem=asc` o mais
                    antigo. Uma página além da última volta com `itens` vazio e os totais corretos.

                    `totalItens` e `totalPaginas` contam todos os registros que atendem aos filtros,
                    inclusive os que não vieram na página. A contagem é feita no banco (COUNT), sem
                    carregar os registros.""")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Página de produtos no status pedido, com os totais",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PaginaDTO.class)) }),
            @ApiResponse(responseCode = "400", description = "status, tentativas, pagina, tamanho ou datas em formato inválido"),
            @ApiResponse(responseCode = "403", description = "Perfil USR pediu uma responsabilidade diferente de 2 e 4"),
            @ApiResponse(responseCode = "422", description = "status diferente de 0 e 1, faixa de tentativas ou de datas impossível, pagina menor que 1, tamanho diferente de 50, 100, 1000 e 0 ou ordem diferente de asc e desc"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    @GetMapping()
    public ResponseEntity<PaginaDTO<Produto>> retornarProdutosPendentes(
            @Parameter(description = "PRO_STATUS a consultar: 0 ou 1. Omitido, traz os pendentes (0)")
            @RequestParam(defaultValue = "0") int status,
            @RequestParam(required = false) String codigoProduto,
            @RequestParam(required = false) String cliente,
            @RequestParam(required = false) String idIntegracao,
            @Parameter(description = "Só produtos com MAIS tentativas que este valor (exclusivo). Ex.: 5 traz 6 ou mais")
            @RequestParam(required = false) Integer tentativaMaiorQue,
            @Parameter(description = "Só produtos com MENOS tentativas que este valor (exclusivo). Ex.: 5 traz 4 ou menos")
            @RequestParam(required = false) Integer tentativaMenorQue,
            @Parameter(description = "Só erros a partir desta data e hora (inclusive). ISO sem fuso, ex.: 2026-11-05T08:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicial,
            @Parameter(description = "Só erros até esta data e hora (inclusive). ISO sem fuso, ex.: 2026-11-05T18:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFinal,
            @Parameter(description = "Só a plataforma exata (PRO_INTEGR). As opções vêm do GET /produtos/filtros")
            @RequestParam(required = false) String plataforma,
            @Parameter(description = "Pesquisa no código do produto: traz os que CONTÊM o texto, sem distinguir maiúsculas")
            @RequestParam(required = false) String produtoContem,
            @Parameter(description = "Pesquisa na mensagem de erro: traz as que CONTÊM o texto, sem distinguir maiúsculas")
            @RequestParam(required = false) String erroContem,
            @Parameter(description = "Só erros destas responsabilidades (0 a 4): 0 não informado, 1 culpa nossa, 2 cliente, 3 instabilidade, 4 plataforma. Repita o parâmetro ou separe por vírgula. Omitido, traz todas")
            @RequestParam(required = false) List<Integer> responsabilidade,
            @Parameter(description = "Página a retornar, a partir de 1")
            @RequestParam(defaultValue = "1") int pagina,
            @Parameter(description = "Registros por página: 50, 100 ou 1000; 0 traz todos numa só página",
                    schema = @Schema(allowableValues = {"50", "100", "1000", "0"}, defaultValue = "50"))
            @RequestParam(defaultValue = "50") int tamanho,
            @Parameter(description = "Ordem pela data do erro: desc (mais recente primeiro, padrão) ou asc (mais antigo primeiro)",
                    schema = @Schema(allowableValues = {"desc", "asc"}, defaultValue = "desc"))
            @RequestParam(defaultValue = "desc") String ordem,
            @AuthenticationPrincipal Usuario usuario
    ) {
        return ResponseEntity.ok(service.retornarProdutos(
                status, codigoProduto, cliente, idIntegracao, tentativaMaiorQue, tentativaMenorQue,
                dataInicial, dataFinal, plataforma, produtoContem, erroContem,
                ResponsabilidadeErro.permitidas(usuario, responsabilidade),
                pagina, tamanho, ordem));
    }

    @Operation(summary = "Remove um produto do monitoramento",
            description = """
                    Apaga definitivamente os registros de erro do produto. A chave é
                    codigoProduto (PRO_CODITE) + idIntegracao (INT_CODAUT) + cliente (PRO_CLIENT)
                    e, opcionalmente, rotina (PRO_ROTINA):

                    - sem `rotina`: o produto é removido de todas as rotinas em que aparecer,
                      numa chamada só (comportamento original);
                    - com `rotina`: sai só a linha daquela rotina, e o erro de outra rotina do
                      mesmo produto continua no monitor. É a mesma rotina que o POST /produtos
                      usa na chave.

                    `rotina` enviada em branco (`?rotina=`) é recusada com 422, para que uma
                    expressão vazia no n8n não caia no "remove de todas as rotinas".

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
            @ApiResponse(responseCode = "422", description = "rotina enviada em branco"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    @DeleteMapping()
    public ResponseEntity<ProdutoRemovidoDTO> removerProduto(
            @Parameter(description = "PRO_CODITE - código do produto", required = true)
            @RequestParam String codigoProduto,
            @Parameter(description = "INT_CODAUT - código da integração", required = true)
            @RequestParam String idIntegracao,
            @Parameter(description = "PRO_CLIENT - código do lojista", required = true)
            @RequestParam String cliente,
            @Parameter(description = "PRO_ROTINA - opcional. Informada, remove só esta rotina; omitida, remove todas")
            @RequestParam(required = false) String rotina
    ) {
        int removidos = service.removerProduto(codigoProduto, cliente, idIntegracao, rotina);
        if (removidos == 0) {
            // Não é erro: o estado pedido (produto fora do monitoramento) já valia antes da
            // chamada. 204 mantém o DELETE idempotente para o n8n reprocessar sem tratar falha.
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(
                new ProdutoRemovidoDTO(codigoProduto, cliente, idIntegracao, rotina, removidos));
    }
}
