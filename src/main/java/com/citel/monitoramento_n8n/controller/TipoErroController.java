package com.citel.monitoramento_n8n.controller;

import com.citel.monitoramento_n8n.DTO.DadosCriacaoTipoErro;
import com.citel.monitoramento_n8n.DTO.TipoErroDTO;
import com.citel.monitoramento_n8n.DTO.ValidacaoTipoErroDTO;
import com.citel.monitoramento_n8n.model.Usuario;
import com.citel.monitoramento_n8n.service.TipoErroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/tipos-erro")
@Tag(name = "Tipos de erro", description = "Responsabilidade dos erros de integração, de 0 a 4: não informado, nossa, cliente, instabilidade, plataforma externa (CADERR)")
public class TipoErroController {

    private final TipoErroService service;

    public TipoErroController(TipoErroService service) {
        this.service = service;
    }

    @Operation(summary = "Lista os tipos de erro",
            description = """
                    Filtros opcionais, combinados com E. O n8n usa `?plataforma=1005` para procurar o código \
                    antes de chamar a IA; o portal usa `?validado=N` para a fila de validação.""")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de tipos (vazia se nada bater)",
                    content = {@Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = TipoErroDTO.class)))}),
            @ApiResponse(responseCode = "422", description = "validado diferente de S ou N")
    })
    @GetMapping
    public ResponseEntity<List<TipoErroDTO>> listar(
            @Parameter(example = "1005") @RequestParam(required = false) String plataforma,
            @Parameter(example = "N") @RequestParam(required = false) String validado,
            @Parameter(example = "categoria_ausente") @RequestParam(required = false) String codigoErro) {
        return ResponseEntity.ok(service.listar(plataforma, validado, codigoErro));
    }

    @Operation(summary = "Grava um tipo de erro novo",
            description = """
                    Se a plataforma + código já existir, devolve o existente com 200, sem erro (duas execuções \
                    do n8n podem tentar gravar o mesmo tipo ao mesmo tempo). Pelo token de lojista (n8n) o tipo \
                    entra como decisão da IA e não validado; por admin no portal, entra como cadastro manual já validado.""")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Tipo criado",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = TipoErroDTO.class))}),
            @ApiResponse(responseCode = "200", description = "Tipo já existia; devolve o existente",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = TipoErroDTO.class))}),
            @ApiResponse(responseCode = "400", description = "Corpo inválido"),
            @ApiResponse(responseCode = "422", description = "Plataforma ou status inexistente")
    })
    @PostMapping
    public ResponseEntity<TipoErroDTO> registrar(@RequestBody @Valid DadosCriacaoTipoErro dados,
                                                 @AuthenticationPrincipal Usuario usuario,
                                                 UriComponentsBuilder uriBuilder) {
        TipoErroService.Registro registro = service.registrar(dados, usuario);

        if (!registro.criado()) {
            return ResponseEntity.ok(registro.tipo());
        }

        URI uri = uriBuilder.path("/tipos-erro/{codigo}").buildAndExpand(registro.tipo().codigo()).toUri();
        return ResponseEntity.created(uri).body(registro.tipo());
    }

    @Operation(summary = "Valida ou reclassifica um tipo de erro",
            description = """
                    Sem status, confirma o que está gravado; com status, troca e confirma. Grava quem validou \
                    e quando. Todos os erros ligados ao tipo passam a mostrar o status novo.""")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tipo validado",
                    content = {@Content(mediaType = "application/json", schema = @Schema(implementation = TipoErroDTO.class))}),
            @ApiResponse(responseCode = "404", description = "Tipo não encontrado"),
            @ApiResponse(responseCode = "422", description = "Status inexistente")
    })
    @PatchMapping("/{codigo}")
    public ResponseEntity<TipoErroDTO> validar(@PathVariable Integer codigo,
                                               @RequestBody(required = false) ValidacaoTipoErroDTO dados,
                                               @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(service.validar(codigo, dados, usuario));
    }
}
