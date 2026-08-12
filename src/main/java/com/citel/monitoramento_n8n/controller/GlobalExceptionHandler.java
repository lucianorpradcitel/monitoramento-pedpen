package com.citel.monitoramento_n8n.controller;

import com.citel.monitoramento_n8n.exception.BusinessException;
import com.citel.monitoramento_n8n.exception.ConflictException;
import com.citel.monitoramento_n8n.exception.NotFoundException;
import com.citel.monitoramento_n8n.exception.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tratamento centralizado de exceções não capturadas nos controllers.
 * Substitui os try/catch repetidos que apenas relançavam RuntimeException (e resultavam em 500).
 * Mantém o mesmo status HTTP (500) para erros inesperados.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleNaoAutorizado(UnauthorizedException e) {
        log.warn("Requisição não autorizada: {}", e.getMessage());
        return corpo(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNaoEncontrado(NotFoundException e) {
        log.info("Recurso não encontrado: {}", e.getMessage());
        return corpo(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflito(ConflictException e) {
        log.info("Conflito: {}", e.getMessage());
        return corpo(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleRegraDeNegocio(BusinessException e) {
        log.info("Regra de negócio violada: {}", e.getMessage());
        return corpo(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidacao(MethodArgumentNotValidException e) {
        String mensagem = e.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("; "));

        if (mensagem.isEmpty()) {
            mensagem = e.getBindingResult().getAllErrors().stream()
                    .map(erro -> erro instanceof FieldError campo
                            ? campo.getField() + ": " + campo.getDefaultMessage()
                            : erro.getDefaultMessage())
                    .collect(Collectors.joining("; "));
        }

        log.info("Payload inválido: {}", mensagem);
        return corpo(HttpStatus.BAD_REQUEST, mensagem);
    }

    /**
     * Query param obrigatório ausente e JSON malformado são erro do chamador, não do servidor.
     * Sem estes dois handlers eles caem no catch-all abaixo e viram 500, o que faz o n8n tratar
     * um request errado como falha transitória e ficar retentando.
     */
    @ExceptionHandler({MissingServletRequestParameterException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<Map<String, Object>> handleRequisicaoMalFormada(Exception e) {
        log.info("Requisição mal formada: {}", e.getMessage());

        String mensagem = e instanceof MissingServletRequestParameterException faltando
                ? "Parâmetro obrigatório ausente: " + faltando.getParameterName()
                : "Corpo da requisição inválido ou mal formado";

        return corpo(HttpStatus.BAD_REQUEST, mensagem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleErroInesperado(Exception e) {
        log.error("Erro inesperado ao processar a requisição", e);
        return corpo(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno no servidor");
    }

    private ResponseEntity<Map<String, Object>> corpo(HttpStatus status, String mensagem) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", mensagem);

        return ResponseEntity.status(status).body(body);
    }
}
