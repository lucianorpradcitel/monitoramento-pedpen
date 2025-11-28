package com.citel.monitoramento_n8n.service;

import com.citel.monitoramento_n8n.DTO.PedidoDTO;
import com.citel.monitoramento_n8n.model.Pedido;
import com.citel.monitoramento_n8n.repository.PedidosRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PedidoService {

    private final PedidosRepository repository;

    public PedidoService(PedidosRepository repository) {

        this.repository = repository;
    }

    public Pedido registrarPedido(PedidoDTO pedidoComErro) {

        Optional<Pedido> pedido = repository.findByCodigoPedidoAndCliente(pedidoComErro.getCodigoPedido(), pedidoComErro.getCliente());


        if (pedido.isEmpty())
        {
            Pedido pedidoNaoImportado = new Pedido();
            pedidoNaoImportado.setStatus(0);
            pedidoNaoImportado.setCodigoPedido(pedidoComErro.getCodigoPedido());
            pedidoNaoImportado.setErro(pedidoComErro.getErro());
            pedidoNaoImportado.setCliente(pedidoComErro.getCliente());
            pedidoNaoImportado.setPlataforma(pedidoComErro.getPlataforma());
            return repository.save(pedidoNaoImportado);
        }

        else {

            return pedido.get();
        }
    }

    public List<Pedido> retornarPedidosPendentes() {

        return repository.findByStatus(0);
    }

    public Optional<Pedido> registraComoIntegrado(String codigoPedido, String cliente, int status, String erro) {
        return repository.findByCodigoPedidoAndCliente(codigoPedido, cliente)
                .map(pedido -> {
                    pedido.setStatus(status);
                    pedido.setErro(erro);
                    return repository.save(pedido);
                });
    }

}