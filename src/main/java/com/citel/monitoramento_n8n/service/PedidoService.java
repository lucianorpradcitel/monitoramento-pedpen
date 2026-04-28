package com.citel.monitoramento_n8n.service;

import com.citel.monitoramento_n8n.DTO.PedidoDTO;
import com.citel.monitoramento_n8n.model.Pedido;
import com.citel.monitoramento_n8n.repository.PedidosRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.ToIntFunction;

@Service
@Slf4j
public class PedidoService {

    private final PedidosRepository repository;

    public PedidoService(PedidosRepository repository) {
        this.repository = repository;
    }

    public Integer converterStatus(String status)
    {
        return Integer.parseInt(status);
    }

    public Pedido registrarPedido(PedidoDTO pedidoComErro) {
        log.info("📝 Registrando/Atualizando pedido - Código: {}, Cliente: {}",
                pedidoComErro.getCodigoPedido(), pedidoComErro.getCliente());

        Pedido pedido = repository.findByCodigoPedidoAndCliente(
                        pedidoComErro.getCodigoPedido(),
                        pedidoComErro.getCliente())
                .stream()
                .findFirst()
                .orElse(new Pedido());

        // Atualiza ou seta os valores
        pedido.setStatus(pedidoComErro.getStatus());
        pedido.setCodigoPedido(pedidoComErro.getCodigoPedido());
        pedido.setErro(pedidoComErro.getErro());
        pedido.setCliente(pedidoComErro.getCliente());
        pedido.setPlataforma(pedidoComErro.getPlataforma());

        Pedido salvo = repository.save(pedido);

        if (pedido.getCodigoPedido() == null) {
            log.info("✅ Pedido criado - ID: {}", salvo.getCodigoPedido());
        } else {
            log.info("🔄 Pedido atualizado - ID: {}", salvo.getCodigoPedido());
        }

        return salvo;
    }

    public List<Pedido> retornarPedidosPendentes(String cliente, String codigoPedido, String status) {
        log.info("🔍 Buscando pedidos - Cliente: {}, Código: {}, Status: {}",
                cliente, codigoPedido, status);

        Integer statusInt = (status != null) ? Integer.parseInt(status) : null;


        // ✅ ORDEM IMPORTA! Mais específico primeiro (3 filtros)
        if (cliente != null && codigoPedido != null && statusInt != null) {

            log.debug("Buscando por: cliente + código + status");
            return repository.findByCodigoPedidoAndClienteAndStatus(codigoPedido, cliente, statusInt);
        }
        // ✅ NOVO: cliente + código (sem status)
        else if (cliente != null && codigoPedido != null) {
            log.debug("Buscando por: cliente + código");
            return repository.findByCodigoPedidoAndCliente(codigoPedido, cliente);
        }
        // ✅ NOVO: código + status (sem cliente) ← AQUI ESTAVA FALTANDO!
        else if (codigoPedido != null && statusInt != null) {
            log.debug("Buscando por: código + status");
            return repository.findByCodigoPedidoAndStatus(codigoPedido, statusInt);
        }
        // ✅ Cliente + Status (sem código)
        else if (cliente != null && statusInt != null) {
            log.debug("Buscando por: cliente + status");
            return repository.findByClienteAndStatus(cliente, statusInt);
        }
        // Apenas cliente
        else if (cliente != null) {
            log.debug("Buscando por: cliente");
            return repository.findByCliente(cliente);
        }
        // Apenas código
        else if (codigoPedido != null) {
            log.debug("Buscando por: código");
            return repository.findByCodigoPedido(codigoPedido);
        }
        // Apenas status
        else if (statusInt != null) {
            log.debug("Buscando por: status");
            return repository.findByStatus(statusInt);
        }
        // Todos
        else {
            log.debug("Retornando TODOS os pedidos");
            return repository.findAll();
        }
    }

    public void removePedidoDoMonitoramento(String codigoPedido, String cliente) {
        log.info("🗑️  Removendo pedido - Código: {}, Cliente: {}", codigoPedido, cliente);

        List<Pedido> pedido = repository.findByCodigoPedidoAndCliente(codigoPedido, cliente);

        if (pedido.isEmpty()) {
            log.warn("❌ Pedido não encontrado para remover");
            throw new RuntimeException("Pedido não encontrado");
        }

        Pedido pedidoRemovido = pedido.get(0);
        repository.delete(pedidoRemovido);

        log.info("✅ Pedido removido com sucesso");
    }
}