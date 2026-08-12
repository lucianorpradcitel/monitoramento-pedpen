package com.citel.monitoramento_n8n.repository;

import com.citel.monitoramento_n8n.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    UserDetails findByUserName(String userName);

    @Query("""
        SELECT c FROM Cliente c
        WHERE (:nome IS NULL OR UPPER(c.nome) LIKE UPPER(CONCAT('%', :nome, '%')))
        ORDER BY c.nome
        """)
    List<Cliente> buscarPorNome(@Param("nome") String nome);
}
