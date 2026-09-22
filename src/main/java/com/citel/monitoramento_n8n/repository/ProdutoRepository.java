package com.citel.monitoramento_n8n.repository;

import com.citel.monitoramento_n8n.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ProdutoRepository extends JpaRepository<Produto, String> {
    List<Produto> findByCodigoProdutoAndClienteAndRotina(String codigoProduto, String cliente, String rotina);
    List<Produto> findByClienteInAndCodigoProdutoIn(List<String> clientes, List<String> codigosProduto);
    @Query("""
        SELECT p FROM Produto p
        WHERE p.status = 0
          AND (:codigoProduto IS NULL OR p.codigoProduto = :codigoProduto)
          AND (:cliente IS NULL OR p.cliente = :cliente)
          AND (:idIntegracao IS NULL OR p.idIntegracao = :idIntegracao)
          AND (:tentativaMaiorQue IS NULL OR p.tentativa > :tentativaMaiorQue)
          AND (:tentativaMenorQue IS NULL OR p.tentativa < :tentativaMenorQue)
          AND ((:libera IS NOT NULL AND p.libera = :libera)
               OR (:libera IS NULL AND (p.libera IS NULL OR p.libera <> 'N')))
        """)
    List<Produto> buscarPendentes(@Param("codigoProduto") String codigoProduto,
                                  @Param("cliente") String cliente,
                                  @Param("idIntegracao") String idIntegracao,
                                  @Param("tentativaMaiorQue") Integer tentativaMaiorQue,
                                  @Param("tentativaMenorQue") Integer tentativaMenorQue,
                                  @Param("libera") String libera);

    /**
     * Apaga o produto em TODAS as rotinas em que ele aparece: a rotina de propósito não entra
     * na chave. Bulk delete numa instrução só, em vez de carregar as entidades para removê-las
     * uma a uma.
     *
     * @return quantas linhas foram apagadas
     */
    @Modifying
    @Query("""
        DELETE FROM Produto p
        WHERE p.codigoProduto = :codigoProduto
          AND p.cliente = :cliente
          AND p.idIntegracao = :idIntegracao
        """)
    int removerTodasAsRotinas(@Param("codigoProduto") String codigoProduto,
                              @Param("cliente") String cliente,
                              @Param("idIntegracao") String idIntegracao);
}
