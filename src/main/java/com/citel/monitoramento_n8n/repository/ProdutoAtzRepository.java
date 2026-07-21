package com.citel.monitoramento_n8n.repository;

import com.citel.monitoramento_n8n.model.ProdutoAtz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProdutoAtzRepository extends JpaRepository<ProdutoAtz, String> {


}
