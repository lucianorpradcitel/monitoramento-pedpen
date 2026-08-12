package com.citel.monitoramento_n8n.service;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.citel.monitoramento_n8n.exception.BusinessException;
import com.citel.monitoramento_n8n.exception.UnauthorizedException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Valida o ID Token emitido pelo Google no login (OpenID Connect).
 *
 * O token é um JWT assinado com RS256 pela chave privada do Google. Aqui só se confere a
 * assinatura contra as chaves públicas publicadas em JWKS, mais os claims que impedem que um token
 * legítimo emitido para OUTRA aplicação seja aceito nesta.
 */
@Slf4j
@Service
public class GoogleTokenService {

    private static final String JWKS = "https://www.googleapis.com/oauth2/v3/certs";

    /** O Google emite ora com esquema, ora sem — os dois são válidos. */
    private static final List<String> EMISSORES = List.of("accounts.google.com", "https://accounts.google.com");

    @Value("${api.security.google.client-id:}")
    private String clientId;

    @Value("${api.security.google.dominio:}")
    private String dominioPermitido;

    private JwkProvider jwkProvider;

    @PostConstruct
    void iniciar() throws Exception {
        // Cacheia as chaves: sem isto, cada login iria à internet buscar o JWKS.
        this.jwkProvider = new JwkProviderBuilder(new URL(JWKS))
                .cached(10, 24, TimeUnit.HOURS)
                .rateLimited(10, 1, TimeUnit.MINUTES)
                .build();

        if (!StringUtils.hasText(clientId)) {
            log.warn("api.security.google.client-id não configurado - o login pelo Google responderá 422 até ser definido");
        }
    }

    /** Dados extraídos do ID Token depois de validado. */
    public record IdentidadeGoogle(String subject, String email, String nome, String dominio) {
    }

    public IdentidadeGoogle validar(String idToken) {
        if (!StringUtils.hasText(clientId)) {
            throw new BusinessException(
                    "Login pelo Google não está configurado nesta API: defina api.security.google.client-id.");
        }

        if (!StringUtils.hasText(idToken)) {
            throw new UnauthorizedException("idToken é obrigatório");
        }

        DecodedJWT jwt = verificarAssinatura(idToken);

        if (!EMISSORES.contains(jwt.getIssuer())) {
            log.warn("ID Token com emissor inesperado: {}", jwt.getIssuer());
            throw new UnauthorizedException("Token do Google inválido");
        }

        // email_verified falso significa que o Google não confirmou o endereço: aceitar abriria
        // caminho para alguém reivindicar um e-mail que não controla.
        // O claim costuma vir booleano, mas há emissões em que chega como string.
        Claim verificado = jwt.getClaim("email_verified");
        boolean emailVerificado = Boolean.TRUE.equals(verificado.asBoolean())
                || "true".equalsIgnoreCase(verificado.asString());

        if (!emailVerificado) {
            throw new UnauthorizedException("E-mail não verificado pelo Google");
        }

        String email = jwt.getClaim("email").asString();
        if (!StringUtils.hasText(email)) {
            throw new UnauthorizedException("Token do Google não trouxe o e-mail");
        }

        // hd é o domínio da conta Workspace. Contas Gmail comuns não têm este claim.
        String dominio = jwt.getClaim("hd").asString();
        if (StringUtils.hasText(dominioPermitido) && !dominioPermitido.equalsIgnoreCase(dominio)) {
            log.warn("Login recusado - e-mail {} com domínio '{}', esperado '{}'", email, dominio, dominioPermitido);
            throw new UnauthorizedException(
                    "Só é permitido entrar com uma conta " + dominioPermitido);
        }

        String nome = jwt.getClaim("name").asString();
        log.info("ID Token do Google validado para {}", email);

        return new IdentidadeGoogle(jwt.getSubject(), email, StringUtils.hasText(nome) ? nome : email, dominio);
    }

    /**
     * A verificação de aud entra aqui: sem ela, um ID Token emitido para qualquer outro aplicativo
     * Google seria aceito, e qualquer site conseguiria fabricar login nesta API.
     */
    private DecodedJWT verificarAssinatura(String idToken) {
        try {
            DecodedJWT naoVerificado = JWT.decode(idToken);
            Jwk jwk = jwkProvider.get(naoVerificado.getKeyId());
            Algorithm algoritmo = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);

            return JWT.require(algoritmo)
                    .withAudience(clientId)
                    .build()
                    .verify(idToken);

        } catch (Exception e) {
            log.warn("Falha ao verificar o ID Token do Google: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            throw new UnauthorizedException("Token do Google inválido ou expirado");
        }
    }
}
