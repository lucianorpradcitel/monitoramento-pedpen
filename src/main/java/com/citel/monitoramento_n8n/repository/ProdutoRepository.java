package com.citel.monitoramento_n8n.repository;

import com.citel.monitoramento_n8n.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ProdutoRepository extends JpaRepository<Produto, String> {
    Optional<Produto> findByCodigoProdutoAndCliente(String codigoProduto, String cliente);
    Optional<Produto> findByCodigoProdutoAndClienteAndMensagemErro(String codigoProduto, String cliente, String mensagemErro);
    @Query("""
        SELECT p FROM Produto p
        WHERE p.status = 0
          AND (:codigoProduto IS NULL OR p.codigoProduto = :codigoProduto)
          AND (:cliente IS NULL OR p.cliente = :cliente)
          AND (:idIntegracao IS NULL OR p.idIntegracao = :idIntegracao)
        """)
    List<Produto> buscarPendentes(@Param("codigoProduto") String codigoProduto,
                                  @Param("cliente") String cliente,
                                  @Param("idIntegracao") String idIntegracao);
}