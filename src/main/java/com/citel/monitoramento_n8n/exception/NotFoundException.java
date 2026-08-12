package com.citel.monitoramento_n8n.exception;

/**
 * Recurso não encontrado. Resulta em 404.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String mensagem) {
        super(mensagem);
    }
}
