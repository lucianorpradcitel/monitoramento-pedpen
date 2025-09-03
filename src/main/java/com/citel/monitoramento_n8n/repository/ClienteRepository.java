package com.citel.monitoramento_n8n.repository;

import com.citel.monitoramento_n8n.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;

public interface ClienteRepository extends JpaRepository<Cliente, String> {
    UserDetails findByUserName(String userName);
}
