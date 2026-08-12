package com.citel.monitoramento_n8n.repository;

import com.citel.monitoramento_n8n.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** O e-mail é a chave natural — é ele que vem no subject do JWT. */
    Optional<Usuario> findByEmail(String email);
}
