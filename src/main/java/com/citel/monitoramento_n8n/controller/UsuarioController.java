package com.citel.monitoramento_n8n.controller;

import com.citel.monitoramento_n8n.DTO.UsuarioLogadoDTO;
import com.citel.monitoramento_n8n.model.Usuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/usuarios")
@Tag(name = "Usuários internos", description = "Dados do usuário interno da Citel autenticado pelo Google")
public class UsuarioController {

    @Operation(summary = "Devolve o usuário interno dono do token",
            description = """
                    Devolve e-mail, nome e perfil (ADMIN ou USUARIO). O perfil é lido do CADUSR a \
                    cada chamada, então uma promoção ou um rebaixamento vale na hora — ao contrário \
                    do claim "perfil" do token, que só muda no próximo login.

                    É o que o n8n consulta para decidir quem pode exportar workflows. Token de \
                    lojista recebe 403: este endpoint é só para usuário interno.""")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário interno autenticado",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = UsuarioLogadoDTO.class))}),
            @ApiResponse(responseCode = "403", description = "Sem token válido, ou token de lojista"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    @GetMapping("/me")
    public ResponseEntity<UsuarioLogadoDTO> usuarioLogado(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(UsuarioLogadoDTO.de(usuario));
    }
}
