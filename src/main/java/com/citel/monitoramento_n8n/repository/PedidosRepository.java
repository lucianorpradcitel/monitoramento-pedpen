package com.citel.monitoramento_n8n.repository;

import com.citel.monitoramento_n8n.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PedidosRepository extends JpaRepository<Pedido, String> {

    List<Pedido> findByCodigoPedidoAndCliente(String codigoPedido, String cliente);

    List<Pedido> findByClienteInAndCodigoPedidoIn(List<String> clientes, List<String> codigosPedido);

    @Query("""
        SELECT p FROM Pedido p
        WHERE (:cliente IS NULL OR p.cliente = :cliente)
          AND (:codigoPedido IS NULL OR p.codigoPedido = :codigoPedido)
          AND (:status IS NULL OR p.status = :status)
          AND (:idIntegracao IS NULL OR p.idIntegracao = :idIntegracao)
          AND (:data IS NULL OR CAST(p.dataPedido AS DATE) = :data)
        """)
    List<Pedido> buscarPendentes(@Param("cliente") String cliente,
                                 @Param("codigoPedido") String codigoPedido,
                                 @Param("status") Integer status,
                                 @Param("idIntegracao") String idIntegracao,
                                 @Param("data") LocalDate data);
}
