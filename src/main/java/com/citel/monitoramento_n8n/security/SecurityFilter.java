package com.citel.monitoramento_n8n.security;

import com.citel.monitoramento_n8n.repository.ClienteRepository;
import com.citel.monitoramento_n8n.repository.UsuarioRepository;
import com.citel.monitoramento_n8n.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/**
 * Resolve o principal a partir do JWT.
 *
 * Duas populações usam a mesma API: lojistas (CADCLI), que é quem o n8n autentica, e usuários
 * internos da Citel (CADUSR), autenticados pelo Google. O claim "tipo" do token diz em qual tabela
 * procurar — sem ele, um e-mail do Google seria buscado em CLI_USRNME e nunca acharia nada.
 */
@Component
public class SecurityFilter extends OncePerRequestFilter {
    private final TokenService tokenService;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;

    public SecurityFilter(TokenService tokenService,
                          ClienteRepository clienteRepository,
                          UsuarioRepository usuarioRepository) {
        this.tokenService = tokenService;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        var token = recuperarToken(request);

        if (token != null)
        {
            var identidade = tokenService.decodificar(token);

            if (identidade != null)
            {
                UserDetails principal = resolverPrincipal(identidade);

                if (principal != null && principal.isEnabled())
                {
                    var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        filterChain.doFilter(request,response);
    }

    private UserDetails resolverPrincipal(TokenService.Identidade identidade)
    {
        if (TokenService.TIPO_USUARIO.equals(identidade.tipo()))
        {
            return usuarioRepository.findByEmail(identidade.subject()).orElse(null);
        }

        return clienteRepository.findByUserName(identidade.subject());
    }

    private String recuperarToken(HttpServletRequest request)
    {
        var authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null)
        {
            return authorizationHeader.replace("Bearer ", "").trim();
        }
        return null;
    }
}
