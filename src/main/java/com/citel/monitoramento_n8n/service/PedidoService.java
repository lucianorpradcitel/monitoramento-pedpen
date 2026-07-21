package com.citel.monitoramento_n8n.service;

import com.citel.monitoramento_n8n.DTO.PedidoDTO;
import com.citel.monitoramento_n8n.DTO.PedidoLoteDTO;
import com.citel.monitoramento_n8n.model.Pedido;
import com.citel.monitoramento_n8n.repository.PedidosRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PedidoService {

    private final PedidosRepository repository;

    public PedidoService(PedidosRepository repository) {
        this.repository = repository;
    }

    public Pedido registrarPedido(PedidoDTO pedidoComErro, String idInt) {

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
        pedido.setIdIntegracao(idInt);// vem do cliente autenticado (CADCLI.CLI_CODAUT)
        pedido.setRotina(pedidoComErro.getRotina());

        return repository.save(pedido);
    }

    public List<Pedido> registrarPedidosList(List<PedidoLoteDTO> listaPedidos, String idInt) {
        List<String> clientes = listaPedidos.stream()
                .map(PedidoLoteDTO::getCliente).distinct().toList();
        List<String> codigos = listaPedidos.stream()
                .map(PedidoLoteDTO::getCodigoPedido).distinct().toList();

        // Busca todos os existentes numa única query (evita N+1) e indexa por cliente|codigoPedido
        Map<String, Pedido> existentesPorChave = repository
                .findByClienteInAndCodigoPedidoIn(clientes, codigos)
                .stream()
                .collect(Collectors.toMap(
                        p -> chave(p.getCliente(), p.getCodigoPedido()),
                        p -> p,
                        (a, b) -> a));

        List<Pedido> listaPed = new ArrayList<>();
        for (PedidoLoteDTO dto : listaPedidos) {
            Pedido existente = existentesPorChave.get(chave(dto.getCliente(), dto.getCodigoPedido()));
            boolean novo = existente == null;

            Pedido ped = PedidoLoteDTO.converterDTO(dto, novo ? new Pedido() : existente);
            if (novo) {
                ped.setStatus(0);   // só define status quando é novo
            }
            ped.setIdIntegracao(idInt);   // vem do cliente autenticado (CADCLI.CLI_CODAUT)

            log.info(novo ? "Pedido criado - {}" : "Pedido atualizado - {}", dto.getCodigoPedido());
            listaPed.add(ped);
        }

        return repository.saveAll(listaPed);
    }

    public List<Pedido> retornarPedidosPendentes(String cliente, String codigoPedido, String status, String idIntegracao, LocalDate data) {
        log.info(" Buscando pedidos - Cliente: {}, Código: {}, Status: {}, IdIntegração: {}, Data {}",
                cliente, codigoPedido, status, idIntegracao, data);

        return repository.buscarPendentes(cliente, codigoPedido, parseStatus(status), idIntegracao, data);
    }

    private Integer parseStatus(String status) {
        if (status == null || status.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(status.replace(",", "."));
        } catch (NumberFormatException e) {
            log.warn("Status inválido: {}", status);
            return null;
        }
    }

    private static String chave(String cliente, String codigoPedido) {
        return cliente + "|" + codigoPedido;
    }
}
