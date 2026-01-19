package com.citel.monitoramento_n8n.sync.DTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MercosNfeResponseDTO {

    private Boolean success;
    private Integer statusCode;
    private Object data;
    private String error;
}