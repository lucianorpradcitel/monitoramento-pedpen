package com.citel.monitoramento_n8n.controller;

import com.citel.monitoramento_n8n.model.Cliente;
import com.citel.monitoramento_n8n.service.TokenService;
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



@RestController
@RequestMapping("/Autenticar")
@Tag(name= "Autenticação JWT", description = "Endpoint para Gerar o JWT")
public class AuthController {
    private final AuthenticationManager manager;
    private final TokenService tokenService;

    public AuthController(AuthenticationManager manager, TokenService tokenService) {
        this.manager = manager;
        this.tokenService = tokenService;
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
}
