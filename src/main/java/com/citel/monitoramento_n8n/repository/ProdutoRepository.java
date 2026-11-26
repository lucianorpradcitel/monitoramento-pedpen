package com.citel.monitoramento_n8n.repository;

import com.citel.monitoramento_n8n.model.Produto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public interface ProdutoRepository extends JpaRepository<Produto, String> {
    List<Produto> findByCodigoProdutoAndClienteAndRotina(String codigoProduto, String cliente, String rotina);
    List<Produto> findByClienteInAndCodigoProdutoIn(List<String> clientes, List<String> codigosProduto);
    @Query("""
        SELECT p FROM Produto p
        WHERE p.status = :status
          AND (:codigoProduto IS NULL OR p.codigoProduto = :codigoProduto)
          AND (:cliente IS NULL OR p.cliente = :cliente)
          AND (:idIntegracao IS NULL OR p.idIntegracao = :idIntegracao)
          AND (:tentativaMaiorQue IS NULL OR p.tentativa > :tentativaMaiorQue)
          AND (:tentativaMenorQue IS NULL OR p.tentativa < :tentativaMenorQue)
          AND (:dataInicial IS NULL OR p.dataErro >= :dataInicial)
          AND (:dataFinal IS NULL OR p.dataErro <= :dataFinal)
          AND (:plataforma IS NULL OR p.plataforma = :plataforma)
          AND (:produtoContem IS NULL OR LOWER(p.codigoProduto) LIKE LOWER(CONCAT('%', :produtoContem, '%')) ESCAPE '!')
          AND (:erroContem IS NULL OR LOWER(p.mensagemErro) LIKE LOWER(CONCAT('%', :erroContem, '%')) ESCAPE '!')
          AND p.responsabilidade IN :responsabilidades
        """)
    Page<Produto> buscar(@Param("status") int status,
                         @Param("codigoProduto") String codigoProduto,
                         @Param("cliente") String cliente,
                         @Param("idIntegracao") String idIntegracao,
                         @Param("tentativaMaiorQue") Integer tentativaMaiorQue,
                         @Param("tentativaMenorQue") Integer tentativaMenorQue,
                         @Param("dataInicial") Date dataInicial,
                         @Param("dataFinal") Date dataFinal,
                         @Param("plataforma") String plataforma,
                         @Param("produtoContem") String produtoContem,
                         @Param("erroContem") String erroContem,
                         @Param("responsabilidades") Collection<Integer> responsabilidades,
                         Pageable pageable);

    /**
     * Clientes que têm produto no status, para o filtro de cliente da tela. Sem repetir e sem nulos.
     * Com plataforma informada, só os clientes que têm produto naquela plataforma.
     */
    @Query("""
        SELECT DISTINCT p.cliente FROM Produto p
        WHERE p.status = :status AND p.cliente IS NOT NULL
          AND (:plataforma IS NULL OR p.plataforma = :plataforma)
          AND p.responsabilidade IN :responsabilidades
        ORDER BY p.cliente
        """)
    List<String> clientesComProduto(@Param("status") int status, @Param("plataforma") String plataforma,
                                    @Param("responsabilidades") Collection<Integer> responsabilidades);

    /**
     * Plataformas que têm produto no status, para o filtro de plataforma da tela. Sem repetir e sem nulos.
     * Com cliente informado, só as plataformas em que aquele cliente tem produto.
     */
    @Query("""
        SELECT DISTINCT p.plataforma FROM Produto p
        WHERE p.status = :status AND p.plataforma IS NOT NULL
          AND (:cliente IS NULL OR p.cliente = :cliente)
          AND p.responsabilidade IN :responsabilidades
        ORDER BY p.plataforma
        """)
    List<String> plataformasComProduto(@Param("status") int status, @Param("cliente") String cliente,
                                       @Param("responsabilidades") Collection<Integer> responsabilidades);

    /**
     * Quantos produtos há no status, por responsabilidade do tipo do erro (erro sem tipo conta como 0). Cada linha
     * é {responsabilidade, total}; só vêm as responsabilidades que têm algum produto.
     */
    @Query("""
        SELECT p.responsabilidade, COUNT(p) FROM Produto p
        WHERE p.status = :status
        GROUP BY p.responsabilidade
        """)
    List<Object[]> contarPorResponsabilidade(@Param("status") int status);

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

    /**
     * Apaga o produto só na rotina informada, deixando intactos os erros das demais rotinas
     * do mesmo produto.
     *
     * @return quantas linhas foram apagadas
     */
    @Modifying
    @Query("""
        DELETE FROM Produto p
        WHERE p.codigoProduto = :codigoProduto
          AND p.cliente = :cliente
          AND p.idIntegracao = :idIntegracao
          AND p.rotina = :rotina
        """)
    int removerDaRotina(@Param("codigoProduto") String codigoProduto,
                        @Param("cliente") String cliente,
                        @Param("idIntegracao") String idIntegracao,
                        @Param("rotina") String rotina);
}
