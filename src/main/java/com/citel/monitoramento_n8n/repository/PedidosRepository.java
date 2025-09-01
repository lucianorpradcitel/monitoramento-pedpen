package com.citel.monitoramento_n8n.repository;

import com.citel.monitoramento_n8n.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PedidosRepository extends JpaRepository<Pedido, String> {
    List<Pedido> findByStatus(int status);
    Optional<Pedido> findByCodigoPedidoAndCliente(String codigoPedido, String cliente);
}




