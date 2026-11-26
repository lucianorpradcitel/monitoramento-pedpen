package com.citel.monitoramento_n8n.service;

import com.citel.monitoramento_n8n.DTO.ContagemErrosDTO;
import com.citel.monitoramento_n8n.exception.BusinessException;
import com.citel.monitoramento_n8n.model.Usuario;
import com.citel.monitoramento_n8n.repository.PedidosRepository;
import com.citel.monitoramento_n8n.repository.ProdutoRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Conta os erros de produto e de pedido por responsabilidade, para a tela mostrar o total de cada visão sem baixar
 * as listas. O perfil USR só recebe as responsabilidades 2 e 4; DEV, ADM e o n8n recebem as cinco.
 */
@Service
public class ContagemErrosService {

    private final ProdutoRepository produtos;
    private final PedidosRepository pedidos;

    public ContagemErrosService(ProdutoRepository produtos, PedidosRepository pedidos) {
        this.produtos = produtos;
        this.pedidos = pedidos;
    }

    /**
     * @param statusProduto PRO_STATUS a contar (0 ou 1; o portal usa 0, os ainda não resolvidos)
     * @param statusPedido  PEN_STATUS a contar (0 a 3; o portal usa 2, os com erro)
     */
    public ContagemErrosDTO contar(Usuario usuario, int statusProduto, int statusPedido) {
        if (statusProduto != 0 && statusProduto != 1) {
            throw new BusinessException("statusProduto deve ser 0 ou 1, mas veio " + statusProduto);
        }
        if (statusPedido < 0 || statusPedido > 3) {
            throw new BusinessException("statusPedido deve ser de 0 a 3, mas veio " + statusPedido);
        }
        // Nulo (n8n), DEV e ADM: todas. USR: só as dele.
        List<Integer> visiveis = ResponsabilidadeErro.validar(ResponsabilidadeErro.permitidas(usuario, null));
        return new ContagemErrosDTO(
                montar(produtos.contarPorResponsabilidade(statusProduto), visiveis),
                montar(pedidos.contarPorResponsabilidade(statusPedido), visiveis));
    }

    /** Uma entrada para cada responsabilidade visível, com 0 onde não há erro (a tela não precisa adivinhar). */
    private static Map<Integer, Long> montar(List<Object[]> linhas, List<Integer> visiveis) {
        Map<Integer, Long> contagem = new LinkedHashMap<>();
        visiveis.forEach(r -> contagem.put(r, 0L));
        for (Object[] linha : linhas) {
            int responsabilidade = linha[0] == null ? 0 : ((Number) linha[0]).intValue();
            if (contagem.containsKey(responsabilidade)) {
                contagem.put(responsabilidade, ((Number) linha[1]).longValue());
            }
        }
        return contagem;
    }
}
