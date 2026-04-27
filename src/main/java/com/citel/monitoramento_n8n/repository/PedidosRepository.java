package com.citel.monitoramento_n8n.repository;

import com.citel.monitoramento_n8n.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PedidosRepository extends JpaRepository<Pedido, Long> {
    // Combinações de 3 filtros
    List<Pedido> findByCodigoPedidoAndClienteAndStatus(String codigoPedido, String cliente, Integer status);

    // Combinações de 2 filtros
    List<Pedido> findByCodigoPedidoAndCliente(String codigoPedido, String cliente);
    List<Pedido> findByCodigoPedidoAndStatus(String codigoPedido, Integer status); // ✅ NOVO
    List<Pedido> findByClienteAndStatus(String cliente, Integer status); // ✅ NOVO

    // Filtros individuais
    List<Pedido> findByCliente(String cliente);
    List<Pedido> findByCodigoPedido(String codigoPedido);
    List<Pedido> findByStatus(Integer status);
}




