package com.citel.monitoramento_n8n.repository;

import com.citel.monitoramento_n8n.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface PedidosRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByCodigoPedidoAndClienteAndStatus(String codigoPedido, String cliente, Integer status);

    List<Pedido> findByCodigoPedidoAndCliente(String codigoPedido, String cliente);
    List<Pedido> findByCodigoPedidoAndStatus(String codigoPedido, Integer status); //
    List<Pedido> findByClienteAndStatus(String cliente, Integer status); //

    List<Pedido> findByCliente(String cliente);
    List<Pedido> findByCodigoPedido(String codigoPedido);
    List<Pedido> findByStatus(Integer status);
    @Query("SELECT p FROM Pedido p WHERE p.cliente = :cliente " +
            "AND p.status = :status " +
            "AND CAST(p.dataPedido AS DATE) = :data")
    List<Pedido> findByClienteAndStatusAndDataPedido(
            @Param("cliente") String cliente,
            @Param("status") Integer status,
            @Param("data") LocalDate data);
}




