package com.citel.monitoramento_n8n.exception;

/**
 * Requisição bem formada, mas que viola uma regra de negócio. Resulta em 422.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String mensagem) {
        super(mensagem);
    }
}
