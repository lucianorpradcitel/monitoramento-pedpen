package com.citel.monitoramento_n8n.exception;

/**
 * O usuário está autenticado, mas o recurso não é dele (ex.: lojista tentando mexer em pedido de outro lojista).
 * Resulta em 403.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String mensagem) {
        super(mensagem);
    }
}
