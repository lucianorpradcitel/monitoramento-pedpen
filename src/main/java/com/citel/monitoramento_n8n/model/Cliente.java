package com.citel.monitoramento_n8n.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Entity
@Table(name="CADCLI")
@Getter
public class Cliente implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="CLI_CODCLI")
    private Long id;

    @Column(name="CLI_NOMCLI")
    private String nome;

    @Column(name="CLI_SENCLI")
    private String senha;

    @Column(name="CLI_USRNME")
    private String userName;

    @Column(name="CLI_CODAUT")
    private String idInt;

    public Cliente(String nome, String userName, String senha) {
        this.nome = nome;
        this.senha = senha;
        this.userName = userName;
    }

    public Cliente() {}

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override
    public String getPassword() {
        return this.senha;
    }

    @Override
    public String getUsername() {
        return this.userName;
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
        return true;
    }
}
