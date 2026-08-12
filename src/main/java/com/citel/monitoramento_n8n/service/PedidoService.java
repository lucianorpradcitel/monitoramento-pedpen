package com.citel.monitoramento_n8n.service;

import com.citel.monitoramento_n8n.DTO.PedidoDTO;
import com.citel.monitoramento_n8n.DTO.PedidoLoteDTO;
import com.citel.monitoramento_n8n.model.Pedido;
import com.citel.monitoramento_n8n.repository.PedidosRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PedidoService {

    private final PedidosRepository repository;
    private final IntegracaoService integracaoService;

    public PedidoService(PedidosRepository repository, IntegracaoService integracaoService) {
        this.repository = repository;
        this.integracaoService = integracaoService;
    }

    public Pedido registrarPedido(PedidoDTO pedidoComErro, Long codigoCliente) {

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
        // Vem do payload e é conferido contra a CADINT do lojista autenticado. Omitido, fica nulo
        // — o CADCLI.CLI_CODAUT não serve mais como origem: é um valor por lojista e não distingue
        // as N integrações que um lojista pode ter.
        pedido.setIdIntegracao(
                integracaoService.resolverCodigoIntegracao(pedidoComErro.getIdIntegracao(), codigoCliente));
        pedido.setRotina(pedidoComErro.getRotina());

        return repository.save(pedido);
    }

    public List<Pedido> registrarPedidosList(List<PedidoLoteDTO> listaPedidos, Long codigoCliente) {
        List<String> clientes = listaPedidos.stream()
                .map(PedidoLoteDTO::getCliente).distinct().toList();
        List<String> codigos = listaPedidos.stream()
                .map(PedidoLoteDTO::getCodigoPedido).distinct().toList();

        // Valida todos os códigos de integração do lote numa query só, antes de gravar qualquer
        // coisa: se um deles não for do lojista, o lote inteiro é recusado.
        integracaoService.resolverCodigosIntegracao(
                listaPedidos.stream().map(PedidoLoteDTO::getIdIntegracao).toList(), codigoCliente);

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
            // Já validado acima; omitido, fica nulo.
            ped.setIdIntegracao(StringUtils.hasText(dto.getIdIntegracao()) ? dto.getIdIntegracao().trim() : null);

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
