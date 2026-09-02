package com.citel.monitoramento_n8n.config;

import com.citel.monitoramento_n8n.security.SecurityFilter;
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

                    req.anyRequest().authenticated();
                })
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class).build();
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
