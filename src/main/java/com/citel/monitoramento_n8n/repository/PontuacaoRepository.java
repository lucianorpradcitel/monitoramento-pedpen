package com.citel.monitoramento_n8n.repository;


import com.citel.monitoramento_n8n.model.Pedido;
import com.citel.monitoramento_n8n.model.PontuacaoPendente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PontuacaoRepository extends JpaRepository<PontuacaoPendente, String> {
    List<PontuacaoPendente> findByStatus(int status);
    Optional<PontuacaoPendente> findByNumeroDocumentoAndNomeCliente(String numeroDocumento, String cliente);


}
