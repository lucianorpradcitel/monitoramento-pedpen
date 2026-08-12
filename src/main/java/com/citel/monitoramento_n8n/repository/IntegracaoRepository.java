package com.citel.monitoramento_n8n.repository;

import com.citel.monitoramento_n8n.model.Integracao;
import com.citel.monitoramento_n8n.model.IntegracaoId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * O @EntityGraph em "cliente" é obrigatório nas leituras: spring.jpa.open-in-view=false, então
 * montar o nomeCliente fora da transação estoura LazyInitializationException.
 */
public interface IntegracaoRepository extends JpaRepository<Integracao, IntegracaoId> {

    @EntityGraph(attributePaths = "cliente")
    Optional<Integracao> findBySlugAndPlataforma(String slug, String plataforma);

    boolean existsBySlugAndPlataforma(String slug, String plataforma);

    @EntityGraph(attributePaths = "cliente")
    @Query("""
        SELECT i FROM Integracao i
        WHERE (:plataforma IS NULL OR i.plataforma = :plataforma)
          AND (:ativo IS NULL OR i.ativo = :ativo)
        ORDER BY i.codigoIntegracao
        """)
    List<Integracao> buscarPorFiltro(@Param("plataforma") String plataforma,
                                     @Param("ativo") String ativo);

    /** Busca pela PK completa — o código de autorização sozinho não identifica a integração. */
    @EntityGraph(attributePaths = "cliente")
    Optional<Integracao> findByCodigoIntegracaoAndCodigoCliente(String codigoIntegracao, Long codigoCliente);

    /**
     * Quais dos códigos informados realmente pertencem a este lojista.
     * Uma query só: os endpoints de lote chegam com centenas de itens.
     */
    @Query("""
        SELECT i.codigoIntegracao FROM Integracao i
        WHERE i.codigoCliente = :codigoCliente
          AND i.codigoIntegracao IN :codigos
        """)
    List<String> filtrarCodigosDoCliente(@Param("codigoCliente") Long codigoCliente,
                                         @Param("codigos") Collection<String> codigos);
}
