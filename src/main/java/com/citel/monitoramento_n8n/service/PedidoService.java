package com.citel.monitoramento_n8n.service;

import com.citel.monitoramento_n8n.DTO.PedidoDTO;
import com.citel.monitoramento_n8n.DTO.PedidoLoteDTO;
import com.citel.monitoramento_n8n.exception.ConflictException;
import com.citel.monitoramento_n8n.exception.NotFoundException;
import com.citel.monitoramento_n8n.model.Pedido;
import com.citel.monitoramento_n8n.model.Usuario;
import com.citel.monitoramento_n8n.repository.PedidosRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PedidoService {

    /** PEN_STATUS: 0 aguardando o n8n, 1 integrado, 2 erro, 3 finalizado. */
    static final int STATUS_AGUARDANDO = 0;
    static final int STATUS_ERRO = 2;

    private final PedidosRepository repository;

    public PedidoService(PedidosRepository repository) {
        this.repository = repository;
    }

    public Pedido registrarPedido(PedidoDTO pedidoComErro) {

        Pedido existente = repository.findByCodigoPedidoAndCliente(
                        pedidoComErro.getCodigoPedido(),
                        pedidoComErro.getCliente())
                .stream()
                .findFirst()
                .orElse(null);
        boolean novo = existente == null;
        Pedido pedido = novo ? new Pedido() : existente;

        // Atualiza ou seta os valores
        pedido.setStatus(pedidoComErro.getStatus());
        pedido.setCodigoPedido(pedidoComErro.getCodigoPedido());
        pedido.setErro(pedidoComErro.getErro());
        pedido.setCliente(pedidoComErro.getCliente());
        pedido.setPlataforma(pedidoComErro.getPlataforma());
        // Gravado como veio, sem conferir na CADINT: o n8n autentica com o login de serviço, que
        // não é o dono das integrações. Omitido, fica nulo.
        pedido.setIdIntegracao(normalizarIdIntegracao(pedidoComErro.getIdIntegracao()));
        pedido.setRotina(pedidoComErro.getRotina());
        // Cada nova tentativa de gravar o pedido com erro conta como um processamento. Só este
        // endpoint incrementa: o lote (registrarPedidosList) não mexe no sequencial. Pedido
        // novo já nasce com 1 (default da entidade).
        if (!novo) {
            pedido.setSequencialProcessamento(pedido.getSequencialProcessamento() + 1);
        }

        return repository.save(pedido);
    }

    public List<Pedido> registrarPedidosList(List<PedidoLoteDTO> listaPedidos) {
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
            // Gravado como veio, sem conferir na CADINT: o n8n autentica todas as capturas com o
            // login de serviço, que não é o dono das integrações. Omitido, fica nulo.
            ped.setIdIntegracao(normalizarIdIntegracao(dto.getIdIntegracao()));

            log.info(novo ? "Pedido criado - {}" : "Pedido atualizado - {}", dto.getCodigoPedido());
            listaPed.add(ped);
        }

        return repository.saveAll(listaPed);
    }

    /**
     * Devolve um pedido com erro para a fila: o status volta a 0, e é o que o n8n busca para
     * processar de novo. Só o status 2 (erro) pode ser reprocessado: um pedido já integrado ou
     * finalizado, reprocessado, poderia ser gravado em duplicidade no ERP.
     *
     * O sequencialProcessamento não é tocado — quem o incrementa é o reenvio do pedido pelo n8n.
     */
    @Transactional
    public Pedido reprocessar(String id, Usuario usuario) {
        Pedido pedido = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Pedido não encontrado: " + id));

        if (pedido.getStatus() != STATUS_ERRO) {
            throw new ConflictException("Só pedidos com erro (status " + STATUS_ERRO
                    + ") podem ser reprocessados. O status atual deste pedido é " + pedido.getStatus() + ".");
        }

        pedido.setStatus(STATUS_AGUARDANDO);
        pedido.setUltimaAlteracao(LocalDateTime.now());
        Pedido salvo = repository.save(pedido);

        log.info("Pedido reprocessado - pedido: {}, lojista: {}, plataforma: {}, por: {}",
                salvo.getCodigoPedido(), salvo.getCliente(), salvo.getPlataforma(),
                StringUtils.hasText(usuario.getNome()) ? usuario.getNome() : usuario.getEmail());
        return salvo;
    }

    public List<Pedido> retornarPedidosPendentes(String cliente, String codigoPedido, String status, String idIntegracao, LocalDate data) {
        log.info(" Buscando pedidos - Cliente: {}, Código: {}, Status: {}, IdIntegração: {}, Data {}",
                cliente, codigoPedido, status, idIntegracao, data);

        return repository.buscarPendentes(cliente, codigoPedido, parseStatus(status), idIntegracao, data);
    }

    /**
     * Busca por uma lista de status (ex.: ?status=1,2,3), opcionalmente combinada com os demais
     * filtros. Lista omitida ou vazia = sem filtro de status.
     */
    public List<Pedido> retornarPedidos(List<Integer> status, String cliente, String codigoPedido,
                                        String idIntegracao, LocalDate data) {
        List<Integer> statusFiltro = status == null
                ? List.of()
                : status.stream().filter(Objects::nonNull).distinct().toList();

        log.info("Buscando pedidos - Status: {}, Cliente: {}, Código: {}, IdIntegração: {}, Data: {}",
                statusFiltro, cliente, codigoPedido, idIntegracao, data);

        boolean filtrarStatus = !statusFiltro.isEmpty();

        return repository.buscarPorStatus(
                filtrarStatus,
                // Ignorada quando filtrarStatus = false, mas precisa ir preenchida: IN com
                // coleção vazia quebra o bind do parâmetro.
                filtrarStatus ? statusFiltro : List.of(Integer.MIN_VALUE),
                normalizar(cliente),
                normalizar(codigoPedido),
                normalizar(idIntegracao),
                data);
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

    /** Query param em branco (?cliente=) vale como ausente, e não como "cliente igual a vazio". */
    private static String normalizar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }

    private static String chave(String cliente, String codigoPedido) {
        return cliente + "|" + codigoPedido;
    }

    /** PEN_ID_INT como veio no payload, sem espaços; vazio ou omitido vira nulo. */
    private static String normalizarIdIntegracao(String idIntegracao) {
        return StringUtils.hasText(idIntegracao) ? idIntegracao.trim() : null;
    }
}
