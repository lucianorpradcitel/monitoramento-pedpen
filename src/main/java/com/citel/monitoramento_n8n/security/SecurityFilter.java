package com.citel.monitoramento_n8n.security;


import com.citel.monitoramento_n8n.repository.ClienteRepository;
import com.citel.monitoramento_n8n.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityFilter extends OncePerRequestFilter {
    @Autowired
    private TokenService tokenService;

    @Autowired
    private ClienteRepository clienteRepository;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        var token = recuperarToken(request);

        if (token != null)
        {
            var subject = tokenService.validarToken(token);
            if (subject != null)
            {
                var cliente = clienteRepository.findByUserName(subject);
                if(cliente != null)
                {
                    var authentication = new UsernamePasswordAuthenticationToken(cliente, null, cliente.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                }
            }
        }

        filterChain.doFilter(request,response);

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
