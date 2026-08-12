package com.citel.monitoramento_n8n.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Usuário interno da Citel, autenticado pelo Google Workspace (CADUSR).
 *
 * Não é um Cliente: o CADCLI guarda lojistas, e o idInt de um lojista é o que vai parar em
 * PROERR/PEDPEN. Um implantador não tem código de integração nenhum.
 *
 * Implementa UserDetails para poder ser o principal do SecurityContext, mas getPassword() devolve
 * null — não existe senha local, a prova de identidade é sempre o ID Token do Google.
 */
@Entity
@Table(name = "CADUSR")
@Getter
@Setter
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "USR_CODUSR")
    private Long id;

    @Column(name = "USR_EMAIL_", nullable = false)
    private String email;

    @Column(name = "USR_NOMUSR", nullable = false)
    private String nome;

    /** claim sub do Google: identificador estável, sobrevive à troca de e-mail. */
    @Column(name = "USR_GOOGID")
    private String googleId;

    /** char(1) 'S'/'N'. */
    @Column(name = "USR_ATIVO_", length = 1, nullable = false)
    private String ativo;

    @Column(name = "USR_DHUINC", insertable = false, updatable = false)
    private LocalDateTime dataInclusao;

    @Column(name = "USR_DHUACE")
    private LocalDateTime dataUltimoAcesso;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_INTERNO"));
    }

    /** Não há senha local: quem autentica é o Google. */
    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "S".equalsIgnoreCase(this.ativo);
    }
}
