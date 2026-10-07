package com.citel.monitoramento_n8n.controller;

import com.citel.monitoramento_n8n.DTO.AtualizacaoTokensDTO;
import com.citel.monitoramento_n8n.DTO.DadosCriacaoIntegracao;
import com.citel.monitoramento_n8n.DTO.EdicaoIntegracaoDTO;
import com.citel.monitoramento_n8n.DTO.IntegracaoContextoDTO;
import com.citel.monitoramento_n8n.DTO.IntegracaoCriadaDTO;
import com.citel.monitoramento_n8n.DTO.IntegracaoEdicaoDTO;
import com.citel.monitoramento_n8n.DTO.IntegracaoResumoDTO;
import com.citel.monitoramento_n8n.model.Usuario;
import com.citel.monitoramento_n8n.service.IntegracaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/integracoes")
@Tag(name = "Registry de Integrações", description = "Cadastro e leitura da configuração multi-tenant (CADINT)")
public class IntegracaoController {

    private final IntegracaoService service;

    public IntegracaoController(IntegracaoService service) {
        this.service = service;
    }

    @Operation(summary = "Cadastra uma nova integração de lojista",
            description = """
                    Onboarding de tenant. O lojista já deve existir em CADCLI com o mesmo código \
                    (POST /cadastro). O webhookToken é gerado no servidor e devolvido uma única vez \
                    nesta resposta — é o valor que o n8n envia no header x-ct-token.""")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Integração criada",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = IntegracaoCriadaDTO.class))}),
            @ApiResponse(responseCode = "400", description = "Payload inválido (slug fora do padrão, PEM sem -----BEGIN, campo obrigatório ausente)"),
            @ApiResponse(responseCode = "409", description = "Par código + lojista já cadastrado, ou slug já usado nessa plataforma"),
            @ApiResponse(responseCode = "422", description = "Não há lojista em CADCLI com o codigoCliente informado"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    @PostMapping
    public ResponseEntity<IntegracaoCriadaDTO> criarIntegracao(@RequestBody @Valid DadosCriacaoIntegracao dados,
                                                               UriComponentsBuilder uriBuilder) {
        IntegracaoCriadaDTO criada = service.criar(dados);

        URI uri = uriBuilder.path("/integracoes/{slug}")
                .queryParam("plataforma", criada.plataforma())
                .buildAndExpand(criada.slug())
                .toUri();

        return ResponseEntity.created(uri).body(criada);
    }

    @Operation(summary = "Retorna o contexto completo de um tenant (MASTER_Context_Loader)",
            description = """
                    Devolve a configuração com os segredos, incluindo a chave privada RSA. Exige o \
                    header x-ct-token com o webhookToken da integração, além do JWT. Integração \
                    inativa responde 404.""")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contexto do tenant",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = IntegracaoContextoDTO.class))}),
            @ApiResponse(responseCode = "401", description = "Header x-ct-token ausente ou divergente"),
            @ApiResponse(responseCode = "404", description = "Slug/plataforma inexistente ou integração inativa"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    @GetMapping("/{slug}")
    public ResponseEntity<IntegracaoContextoDTO> retornarContexto(
            @PathVariable String slug,
            @RequestParam String plataforma,
            @Parameter(in = ParameterIn.HEADER, required = true,
                    description = "webhookToken da integração, devolvido no POST /integracoes")
            @RequestHeader(value = "x-ct-token", required = false) String webhookToken) {
        return ResponseEntity.ok(service.buscarContexto(slug, plataforma, webhookToken));
    }

    @Operation(summary = "Lista as integrações cadastradas",
            description = """
                    Por padrão devolve apenas identidade, sem nenhum segredo. Com \
                    incluirCredenciais=true devolve o contexto completo — é o que os pais de \
                    fan-out do n8n precisam, porque repassam a linha inteira ao workflow filho.""")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de integrações",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = IntegracaoResumoDTO.class))}),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    @GetMapping
    public ResponseEntity<List<?>> listarIntegracoes(
            @RequestParam(required = false) String plataforma,
            @Parameter(description = "'S' ou 'N'") @RequestParam(required = false) String ativo,
            @Parameter(description = "Inclui a chave privada e os tokens na resposta") @RequestParam(required = false, defaultValue = "false") boolean incluirCredenciais,
            @Parameter(description = "Slug da integração (busca exata)") @RequestParam(required = false) String slug) {

        return ResponseEntity.ok(incluirCredenciais
                ? service.listarComCredenciais(plataforma, ativo, slug)
                : service.listarResumo(plataforma, ativo, slug));
    }

    @Operation(summary = "Dados de uma integração para a tela de edição (somente ADMIN)",
            description = """
                    Devolve a identidade da integração e os três campos editáveis, com a chave \
                    privada. Não traz apiToken, refreshToken nem webhookToken. Só usuário interno \
                    com perfil ADMIN: usuário comum e lojista recebem 403.""")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Integração encontrada",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = IntegracaoEdicaoDTO.class))}),
            @ApiResponse(responseCode = "403", description = "Não é um usuário interno ADMIN"),
            @ApiResponse(responseCode = "404", description = "Par código de integração + lojista inexistente"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    @GetMapping("/{codigoIntegracao}/{codigoCliente}")
    public ResponseEntity<IntegracaoEdicaoDTO> buscarParaEdicao(
            @PathVariable String codigoIntegracao,
            @Parameter(description = "CLI_CODCLI do lojista — o código de autorização se repete entre lojistas")
            @PathVariable Long codigoCliente) {
        return ResponseEntity.ok(service.buscarParaEdicao(codigoIntegracao, codigoCliente));
    }

    @Operation(summary = "Edita webservice, URL da API e chave privada de uma integração (somente ADMIN)",
            description = """
                    Só estes três campos podem ser alterados; qualquer outro no corpo é ignorado. \
                    Campo ausente ou nulo preserva o valor atual. Cada campo que realmente mudou \
                    gera uma linha no LOGINT com o valor anterior, o atual e o nome do usuário; se \
                    nada mudar, nada é gravado. Só usuário interno com perfil ADMIN: usuário comum \
                    e lojista recebem 403.""")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Integração com os valores atuais",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = IntegracaoEdicaoDTO.class))}),
            @ApiResponse(responseCode = "400", description = "Campo vazio, ou chave que não está em formato PEM"),
            @ApiResponse(responseCode = "403", description = "Não é um usuário interno ADMIN"),
            @ApiResponse(responseCode = "404", description = "Par código de integração + lojista inexistente"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    @PatchMapping("/{codigoIntegracao}/{codigoCliente}")
    public ResponseEntity<IntegracaoEdicaoDTO> editar(
            @PathVariable String codigoIntegracao,
            @Parameter(description = "CLI_CODCLI do lojista — o código de autorização se repete entre lojistas")
            @PathVariable Long codigoCliente,
            @RequestBody @Valid EdicaoIntegracaoDTO dados,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(service.editar(codigoIntegracao, codigoCliente, dados, usuario));
    }

    @Operation(summary = "Atualiza os tokens de uma integração (ADMIN_Tray_Token_*)",
            description = "Campos nulos ou vazios preservam o valor que já está no banco.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tokens atualizados",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = IntegracaoResumoDTO.class))}),
            @ApiResponse(responseCode = "404", description = "Par código de integração + lojista inexistente"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    @PatchMapping("/{codigoIntegracao}/{codigoCliente}/tokens")
    public ResponseEntity<IntegracaoResumoDTO> atualizarTokens(
            @PathVariable String codigoIntegracao,
            @Parameter(description = "CLI_CODCLI do lojista — necessário porque o código de autorização se repete entre lojistas")
            @PathVariable Long codigoCliente,
            @RequestBody AtualizacaoTokensDTO dados) {
        return ResponseEntity.ok(service.atualizarTokens(codigoIntegracao, codigoCliente, dados));
    }
}
