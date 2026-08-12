package com.citel.monitoramento_n8n.exception;

/**
 * Credencial de integração ausente ou divergente (header x-ct-token). Resulta em 401.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String mensagem) {
        super(mensagem);
    }
}
