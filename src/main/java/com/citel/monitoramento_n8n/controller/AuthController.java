package com.citel.monitoramento_n8n.controller;

import com.citel.monitoramento_n8n.model.Cliente;
import com.citel.monitoramento_n8n.service.AuthGoogleService;
import com.citel.monitoramento_n8n.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;



record DadosAuth(String userName, String senha){}
record DadosTokenJWT(String token){}
record DadosAuthGoogle(String idToken){}



@RestController
@RequestMapping("/Autenticar")
@Tag(name= "Autenticação JWT", description = "Endpoint para Gerar o JWT")
public class AuthController {
    private final AuthenticationManager manager;
    private final TokenService tokenService;
    private final AuthGoogleService authGoogleService;

    public AuthController(AuthenticationManager manager, TokenService tokenService,
                          AuthGoogleService authGoogleService) {
        this.manager = manager;
        this.tokenService = tokenService;
        this.authGoogleService = authGoogleService;
    }

    @PostMapping()
    public ResponseEntity<DadosTokenJWT> fazerLogin(@RequestBody DadosAuth dados)
    {
        try
        {
            var authenticationToken = new UsernamePasswordAuthenticationToken(dados.userName(), dados.senha());

            var authentication = manager.authenticate(authenticationToken);

            var cliente = (Cliente) authentication.getPrincipal();

            var tokenJWT = tokenService.gerarToken(cliente);

            return ResponseEntity.ok(new DadosTokenJWT(tokenJWT));

        } catch (Exception e) {
            return ResponseEntity.status(401).build();
        }
    }

    @Operation(summary = "Autentica um usuário interno da Citel pelo Google",
            description = """
                    Recebe o ID Token devolvido pelo Google Sign-In e, se ele for válido e de uma \
                    conta do domínio autorizado, devolve o JWT do Monint. O usuário é criado no \
                    CADUSR na primeira entrada.

                    Este endpoint é para pessoas. O n8n continua usando o POST /Autenticar.""")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Autenticado"),
            @ApiResponse(responseCode = "401", description = "ID Token inválido, expirado, de outro domínio, ou usuário bloqueado"),
            @ApiResponse(responseCode = "422", description = "Login pelo Google não configurado nesta API")
    })
    @PostMapping("/google")
    public ResponseEntity<DadosTokenJWT> fazerLoginGoogle(@RequestBody DadosAuthGoogle dados)
    {
        return ResponseEntity.ok(new DadosTokenJWT(authGoogleService.autenticar(dados.idToken())));
    }
}
