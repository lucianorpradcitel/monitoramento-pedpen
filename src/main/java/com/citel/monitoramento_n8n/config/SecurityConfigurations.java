package com.citel.monitoramento_n8n.config;

import com.citel.monitoramento_n8n.security.SecurityFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfigurations {

    private final SecurityFilter securityFilter;

    public SecurityConfigurations(SecurityFilter securityFilter) {
        this.securityFilter = securityFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception
    {
        return http.csrf(csrf -> csrf.disable()).sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(req -> {
                    req.requestMatchers(HttpMethod.POST, "/Autenticar").permitAll();
                    req.requestMatchers(HttpMethod.POST, "/Autenticar/google").permitAll();
                    req.requestMatchers(HttpMethod.POST, "/cadastro").permitAll();
                    req.requestMatchers("/doc**", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll();

                    // Estes recebem @AuthenticationPrincipal Cliente e resolvem a integração pelo
                    // lojista autenticado. Um usuário interno (CADUSR) não é Cliente: sem esta
                    // restrição o argumento chegaria nulo e o endpoint quebraria com 500.
                    //
                    // Atenção aos caminhos: PedidosController não declara @RequestMapping de
                    // classe, então cada rota vive na raiz: POST /pendentes e /pendentes-lote,
                    // GET /pendentes e GET /pedidos. Só os POSTs precisam do Cliente autenticado;
                    // os GETs ficam no anyRequest().authenticated() abaixo.
                    req.requestMatchers(HttpMethod.POST, "/produtos", "/produtos/lote",
                            "/pendentes", "/pendentes-lote").hasRole("LOJISTA");

                    // Telas de admin do portal (Plataformas e Nova integração). A única barreira é
                    // para o usuário interno SEM perfil ADMIN (USR_PERFIL em CADUSR): ele leva 403.
                    // Lojistas — o n8n — e admins passam exatamente como antes, de propósito.
                    // O POST /cadastro, mais acima, continua aberto: ainda é usado por outros clientes.
                    req.requestMatchers(HttpMethod.POST, "/plataformas", "/integracoes").hasAnyRole("ADMIN", "LOJISTA");
                    req.requestMatchers(HttpMethod.GET, "/clientes").hasAnyRole("ADMIN", "LOJISTA");

                    req.anyRequest().authenticated();
                })
                .exceptionHandling(erros -> erros.accessDeniedHandler(acessoNegado()))
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class).build();
    }

    /** 403 em JSON, no mesmo formato {"error": ...} que a tela já sabe ler. */
    private static AccessDeniedHandler acessoNegado()
    {
        return (request, response, e) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"Você não tem permissão para esta ação.\"}");
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception
    {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder()
    {
        return new BCryptPasswordEncoder();
    }



}
