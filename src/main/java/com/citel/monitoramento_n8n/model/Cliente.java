package com.citel.monitoramento_n8n.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id; // Import necessário
import jakarta.persistence.Table;
import lombok.Getter; // Opcional, mas útil
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Entity
@Table(name="CADCLI")
@Getter // Adicionado para não precisar escrever os getters manualmente
public class Cliente implements UserDetails {

    @Id //
    @Column(name="CLI_CODCLI")
    private String id;

    @Column(name="CLI_NOMCLI")
    private String nome;

    @Column(name="CLI_SENCLI")
    private String senha;

    @Column(name="CLI_USRNME")
    private String userName;


    public Cliente(String id, String nome,  String userName, String senha)
    {
        this.id = id;
        this.nome = nome;
        this.senha = senha;
        this.userName = userName;

    };

    public Cliente (){}

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList(); // Usar Collections.emptyList() é mais seguro
    }

    @Override
    public String getPassword() {
        return this.senha;
    }

    @Override
    public String getUsername() {
        return this.userName;
    }

    public String getId()
    {
        return this.id;
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