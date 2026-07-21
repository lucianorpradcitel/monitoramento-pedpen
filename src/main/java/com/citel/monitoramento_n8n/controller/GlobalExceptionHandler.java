package com.citel.monitoramento_n8n.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tratamento centralizado de exceções não capturadas nos controllers.
 * Substitui os try/catch repetidos que apenas relançavam RuntimeException (e resultavam em 500).
 * Mantém o mesmo status HTTP (500) para erros inesperados.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleErroInesperado(Exception e) {
        log.error("Erro inesperado ao processar a requisição", e);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 500);
        body.put("error", "Erro interno no servidor");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
