package com.citel.monitoramento_n8n.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
public class PedidoDTO {

    @Setter
    private String codigoPedido;
    @Setter
    private String cliente;
    @Setter
    private String erro;
    @Setter
    private String plataforma;
    // status permanece sem setter de propósito: mantém o comportamento atual
    // (o valor vindo do JSON não é populado; o service decide o status).
    private int status;
    @Setter
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dataPedido;
    @Setter
    @Schema(hidden = true)
    private LocalDateTime ultimaAlteracao;
    @Setter
    private int sequencialProcessamento;
    @Setter
    private String rotina;
}
