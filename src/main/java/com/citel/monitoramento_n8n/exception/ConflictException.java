package com.citel.monitoramento_n8n.exception;

/**
 * Recurso já existe e a operação não pode sobrescrevê-lo. Resulta em 409.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String mensagem) {
        super(mensagem);
    }
}
