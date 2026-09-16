package com.citel.monitoramento_n8n.controller;

import com.citel.monitoramento_n8n.DTO.DadosCriacaoPlataforma;
import com.citel.monitoramento_n8n.DTO.PlataformaResumoDTO;
import com.citel.monitoramento_n8n.service.PlataformaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/plataformas")
@Tag(name = "Plataformas", description = "Cadastro e leitura das plataformas de integração (CADPLA)")
public class PlataformaController {

    private final PlataformaService service;

    public PlataformaController(PlataformaService service) {
        this.service = service;
    }

    @Operation(summary = "Cadastra uma nova plataforma",
            description = """
                    Alimenta o seletor de plataforma da tela de Nova Integração. O sistemaExterno \
                    passa a ser aceito pelo POST /integracoes assim que cadastrado aqui.""")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Plataforma cadastrada",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = PlataformaResumoDTO.class))}),
            @ApiResponse(responseCode = "409", description = "Já existe plataforma com esse sistemaExterno ou descricao"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    @PostMapping
    public ResponseEntity<PlataformaResumoDTO> cadastrarPlataforma(@RequestBody @Valid DadosCriacaoPlataforma dados,
                                                                    UriComponentsBuilder uriBuilder) {
        PlataformaResumoDTO criada = service.criar(dados);

        URI uri = uriBuilder.path("/plataformas/{id}").buildAndExpand(criada.sistemaExterno()).toUri();

        return ResponseEntity.created(uri).body(criada);
    }

    @Operation(summary = "Lista as plataformas cadastradas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de plataformas",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = PlataformaResumoDTO.class))}),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    @GetMapping
    public ResponseEntity<List<PlataformaResumoDTO>> listarPlataformas() {
        return ResponseEntity.ok(service.listar());
    }
}
