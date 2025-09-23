package com.citel.monitoramento_n8n.repository;

import com.citel.monitoramento_n8n.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProdutoRepository extends JpaRepository<Produto, String> {
    List<Produto> findByStatus(int status);
    Optional<Produto> findByCodigoProdutoAndCliente(String codigoPedido, String cliente);
}
