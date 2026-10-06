package com.citel.monitoramento_n8n.repository;

import com.citel.monitoramento_n8n.model.LogIntegracao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogIntegracaoRepository extends JpaRepository<LogIntegracao, Long> {
}
