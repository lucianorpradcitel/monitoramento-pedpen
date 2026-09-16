package com.citel.monitoramento_n8n.repository;

import com.citel.monitoramento_n8n.model.Plataforma;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlataformaRepository extends JpaRepository<Plataforma, String> {
    boolean existsByDescricaoIgnoreCase(String descricao);

    List<Plataforma> findAllByOrderByDescricaoAsc();
}
