package com.citel.monitoramento_n8n.service;

import com.citel.monitoramento_n8n.exception.UnauthorizedException;
import com.citel.monitoramento_n8n.model.Usuario;
import com.citel.monitoramento_n8n.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Login de usuário interno pelo Google.
 *
 * O usuário é criado no CADUSR na primeira entrada: quem já passou pela validação do ID Token tem
 * uma conta ativa no Workspace da Citel, e exigir um cadastro manual antes disso só criaria uma
 * fila. Desligar a conta no Workspace já revoga o acesso, porque sem ID Token não há login.
 *
 * O USR_ATIVO_ existe para bloquear alguém nesta aplicação sem mexer no Workspace.
 */
@Slf4j
@Service
public class AuthGoogleService {

    private static final String ATIVO = "S";

    private final GoogleTokenService googleTokenService;
    private final UsuarioRepository usuarioRepository;
    private final TokenService tokenService;

    public AuthGoogleService(GoogleTokenService googleTokenService,
                             UsuarioRepository usuarioRepository,
                             TokenService tokenService) {
        this.googleTokenService = googleTokenService;
        this.usuarioRepository = usuarioRepository;
        this.tokenService = tokenService;
    }

    @Transactional
    public String autenticar(String idToken) {
        GoogleTokenService.IdentidadeGoogle identidade = googleTokenService.validar(idToken);

        Usuario usuario = usuarioRepository.findByEmail(identidade.email())
                .map(existente -> atualizar(existente, identidade))
                .orElseGet(() -> criar(identidade));

        if (!usuario.isEnabled()) {
            log.warn("Login recusado - usuário {} está inativo no CADUSR", usuario.getEmail());
            throw new UnauthorizedException("Seu acesso a esta aplicação está bloqueado.");
        }

        return tokenService.gerarTokenUsuario(usuario);
    }

    private Usuario criar(GoogleTokenService.IdentidadeGoogle identidade) {
        Usuario novo = new Usuario();
        novo.setEmail(identidade.email());
        novo.setNome(identidade.nome());
        novo.setGoogleId(identidade.subject());
        novo.setAtivo(ATIVO);
        novo.setDataUltimoAcesso(LocalDateTime.now());

        log.info("Primeiro acesso - criando usuário interno {}", identidade.email());
        return usuarioRepository.save(novo);
    }

    private Usuario atualizar(Usuario usuario, GoogleTokenService.IdentidadeGoogle identidade) {
        usuario.setNome(identidade.nome());
        // O sub do Google é a identidade estável; guardá-lo permite reconhecer a pessoa mesmo que
        // o e-mail mude no Workspace.
        usuario.setGoogleId(identidade.subject());
        usuario.setDataUltimoAcesso(LocalDateTime.now());
        return usuarioRepository.save(usuario);
    }
}
