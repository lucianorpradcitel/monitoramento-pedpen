package com.citel.monitoramento_n8n.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.citel.monitoramento_n8n.model.Cliente;
import com.citel.monitoramento_n8n.model.Usuario;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@Slf4j
public class TokenService {

    @Value("${api.security.token.secret}")
    private String secret;

    @Value("${api.security.token.issuer}")
    private String issuer;

    private Instant dataExpiracao() {
        return LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("-03:00"));
    }

    /** Claim que diz em qual tabela o SecurityFilter deve resolver o principal. */
    public static final String CLAIM_TIPO = "tipo";
    public static final String TIPO_LOJISTA = "lojista";
    public static final String TIPO_USUARIO = "usuario";

    public String gerarToken(Cliente cliente) {
        try {
            log.info("🔄 Gerando token para cliente: {}", cliente.getUsername());

            Algorithm algoritmo = Algorithm.HMAC256(secret);

            String token = JWT.create()
                    .withIssuer(issuer)
                    .withSubject(cliente.getUsername())
                    .withClaim("id", cliente.getId())
                    .withClaim(CLAIM_TIPO, TIPO_LOJISTA)
                    .withExpiresAt(dataExpiracao())
                    .sign(algoritmo);

            log.info("✅ Token gerado com sucesso para: {}", cliente.getUsername());
            log.debug("Token gerado (primeiros 50 chars): {}", token.substring(0, Math.min(50, token.length())));

            return token;

        } catch (JWTCreationException e) {
            log.error("Erro ao gerar o token: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao gerar o token, tente novamente", e);
        } catch (Exception e) {
            log.error("Erro inesperado ao gerar token: {}", e.getMessage(), e);
            throw new RuntimeException("Erro inesperado ao gerar token", e);
        }
    }

    public String gerarTokenUsuario(Usuario usuario) {
        try {
            log.info("🔄 Gerando token para usuário interno: {}", usuario.getEmail());

            Algorithm algoritmo = Algorithm.HMAC256(secret);

            return JWT.create()
                    .withIssuer(issuer)
                    .withSubject(usuario.getEmail())
                    .withClaim("id", usuario.getId())
                    .withClaim(CLAIM_TIPO, TIPO_USUARIO)
                    .withExpiresAt(dataExpiracao())
                    .sign(algoritmo);

        } catch (JWTCreationException e) {
            log.error("Erro ao gerar o token: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao gerar o token, tente novamente", e);
        }
    }

    /** Subject e tipo de um token já verificado. */
    public record Identidade(String subject, String tipo) {
    }

    /**
     * Verifica o token e devolve subject e tipo.
     *
     * Token sem o claim tipo conta como lojista: os que o n8n já tem em mãos foram emitidos antes
     * do claim existir e precisam continuar valendo até expirarem.
     */
    public Identidade decodificar(String token) {
        try {
            if (token == null || token.isEmpty()) {
                log.warn("Token é nulo ou vazio");
                return null;
            }

            Algorithm algoritmo = Algorithm.HMAC256(secret);

            var jwt = JWT.require(algoritmo)
                    .withIssuer(issuer)
                    .build()
                    .verify(token);

            String tipo = jwt.getClaim(CLAIM_TIPO).asString();
            return new Identidade(jwt.getSubject(), tipo == null ? TIPO_LOJISTA : tipo);

        } catch (JWTVerificationException e) {
            log.warn(" Erro de verificação do JWT: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("❌ Erro inesperado ao validar token: {} - {}",
                    e.getClass().getSimpleName(), e.getMessage(), e);
            return null;
        }
    }

    public String validarToken(String token) {
        try {

            if (token == null || token.isEmpty()) {
                log.warn("Token é nulo ou vazio");
                return null;
            }

            Algorithm algoritmo = Algorithm.HMAC256(secret);

            String subject = JWT.require(algoritmo)
                    .withIssuer(issuer)
                    .build()
                    .verify(token)
                    .getSubject();

            log.info("✅ Token validado com sucesso. Subject: {}", subject);
            return subject;

        } catch (JWTVerificationException e) {
            log.warn(" Erro de verificação do JWT: {}", e.getMessage());
            log.warn("Detalhes: {}", e.getClass().getSimpleName());
            return null;
        } catch (IllegalArgumentException e) {
            log.warn("❌ Algoritmo ou chave inválida: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("❌ Erro inesperado ao validar token: {} - {}",
                    e.getClass().getSimpleName(), e.getMessage(), e);
            return null;
        }
    }
}