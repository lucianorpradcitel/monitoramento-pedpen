package com.citel.monitoramento_n8n.service;

import com.citel.monitoramento_n8n.exception.BusinessException;
import com.citel.monitoramento_n8n.exception.ForbiddenException;
import com.citel.monitoramento_n8n.model.Usuario;

import java.util.List;

/**
 * Quem pode ver erros de qual responsabilidade (STAERR). O usuário comum (perfil USR) só vê o que é
 * dele para agir: 2 (culpa do cliente) e 4 (culpa da plataforma externa). Os erros 0 (a classificar),
 * 1 (culpa nossa) e 3 (instabilidade) são do time, e ficam com DEV e ADM.
 *
 * O n8n (token de lojista, sem {@link Usuario} no principal) lê a fila para processar e por isso não
 * é restringido aqui.
 */
public final class ResponsabilidadeErro {

    /** Responsabilidades que o perfil USR enxerga: 2 cliente e 4 plataforma externa. */
    public static final List<Integer> DO_USUARIO = List.of(2, 4);

    /** Responsabilidades válidas (STAERR): 0 não informado, 1 nossa, 2 cliente, 3 instabilidade, 4 plataforma. */
    public static final List<Integer> TODAS = List.of(0, 1, 2, 3, 4);

    private ResponsabilidadeErro() {
    }

    /**
     * A lista que a consulta deve usar: nula ou vazia vale todas (quem já usava a API não percebe mudança); valor
     * fora de 0 a 4 é recusado (422), em vez de devolver uma lista vazia que se confunde com "não há erros".
     */
    public static List<Integer> validar(List<Integer> pedidas) {
        List<Integer> escolhidas = (pedidas == null || pedidas.isEmpty()) ? TODAS : pedidas;
        if (!TODAS.containsAll(escolhidas)) {
            throw new BusinessException("responsabilidade deve ser de 0 a 4, mas veio " + escolhidas);
        }
        return escolhidas;
    }

    /**
     * A lista de responsabilidades que a consulta deve usar. DEV, ADM e lojista (usuario nulo) recebem o
     * que pediram. O USR sem pedido recebe 2 e 4; pedindo qualquer outra, leva 403, em vez de uma lista
     * vazia que o service leria como "todas".
     */
    public static List<Integer> permitidas(Usuario usuario, List<Integer> pedidas) {
        if (usuario == null || usuario.isDevOuAdmin()) {
            return pedidas;
        }
        if (pedidas == null || pedidas.isEmpty()) {
            return DO_USUARIO;
        }
        if (!DO_USUARIO.containsAll(pedidas)) {
            throw new ForbiddenException("Seu perfil só consulta erros de responsabilidade 2 (cliente) e 4 (plataforma externa)");
        }
        return pedidas;
    }
}
