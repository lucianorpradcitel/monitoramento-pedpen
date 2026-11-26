package com.citel.monitoramento_n8n.repository;

import com.citel.monitoramento_n8n.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface PedidosRepository extends JpaRepository<Pedido, String> {

    List<Pedido> findByCodigoPedidoAndCliente(String codigoPedido, String cliente);

    List<Pedido> findByClienteInAndCodigoPedidoIn(List<String> clientes, List<String> codigosPedido);

    /**
     * Quantos pedidos há no status, por responsabilidade do tipo do erro (pedido sem tipo conta como 0). Cada linha
     * é {responsabilidade, total}; só vêm as responsabilidades que têm algum pedido.
     */
    @Query("""
        SELECT p.responsabilidade, COUNT(p) FROM Pedido p
        WHERE p.status = :status
        GROUP BY p.responsabilidade
        """)
    List<Object[]> contarPorResponsabilidade(@Param("status") int status);

    @Query("""
        SELECT p FROM Pedido p
        WHERE (:filtrarStatus = FALSE OR p.status IN :status)
          AND (:cliente IS NULL OR p.cliente = :cliente)
          AND (:codigoPedido IS NULL OR p.codigoPedido = :codigoPedido)
          AND (:idIntegracao IS NULL OR p.idIntegracao = :idIntegracao)
          AND (:data IS NULL OR CAST(p.dataPedido AS DATE) = :data)
          AND p.responsabilidade IN :responsabilidades
        ORDER BY p.dataPedido DESC
        """)
    List<Pedido> buscarPorStatus(@Param("filtrarStatus") boolean filtrarStatus,
                                 @Param("status") Collection<Integer> status,
                                 @Param("cliente") String cliente,
                                 @Param("codigoPedido") String codigoPedido,
                                 @Param("idIntegracao") String idIntegracao,
                                 @Param("data") LocalDate data,
                                 @Param("responsabilidades") Collection<Integer> responsabilidades);
}
