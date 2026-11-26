package com.citel.monitoramento_n8n.DTO;

import com.citel.monitoramento_n8n.model.TipoErro;

import java.time.LocalDateTime;

/** Item do GET /tipos-erro e resposta do POST e do PATCH. */
public record TipoErroDTO(
        Integer codigo,
        String plataforma,
        String codigoErro,
        Integer status,
        String validado,
        String exemplo,
        String idExemplo,
        LocalDateTime dataCadastro,
        String usuarioValidacao,
        LocalDateTime dataValidacao
) {

    public static TipoErroDTO de(TipoErro tipo) {
        return new TipoErroDTO(tipo.getCodigo(), tipo.getPlataforma(), tipo.getCodigoErro(), tipo.getStatus(),
                tipo.getValidado(), tipo.getExemplo(), tipo.getIdExemplo(),
                tipo.getDataCadastro(), tipo.getUsuarioValidacao(), tipo.getDataValidacao());
    }
}
