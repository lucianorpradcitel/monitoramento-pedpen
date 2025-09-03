package com.citel.monitoramento_n8n.service;


import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.citel.monitoramento_n8n.model.Cliente;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.RowSet;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class TokenService {
    @Value("${api.security.token.secret}")
    private String secret;
    @Value("${api.security.token.issuer}")
    private String issuer;


    private Instant dataExpiracao()
    {
        return LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("-03:00"));
    }


    public String gerarToken(Cliente cliente)
    {
        try
        {
            Algorithm algoritmo = Algorithm.HMAC256(secret);

            return JWT.create().withIssuer(issuer).withSubject(cliente.getUsername()).withClaim("id", cliente.getId()).withExpiresAt(dataExpiracao()).sign(algoritmo);
        } catch (JWTCreationException e)

        {
            throw new RuntimeException("Erro ao gerar o token, tente novamente", e);
        }

    }

    public String validarToken(String token)
    {
        try
        {
            Algorithm algoritmo= Algorithm.HMAC256(secret);
            return JWT.require(algoritmo).withIssuer(issuer).build().verify(token).getSubject();
        }

        catch (Exception e)
        {
            return null;
        }
    }

}