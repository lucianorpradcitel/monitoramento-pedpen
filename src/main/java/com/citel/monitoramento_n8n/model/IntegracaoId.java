package com.citel.monitoramento_n8n.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Chave composta da CADINT: (INT_CODAUT, INT_CODCLI).
 *
 * É composta porque o código de autorização pode se repetir entre lojistas diferentes — só o par
 * código + lojista identifica a integração. E é o que permite 1 lojista → N integrações: as duas
 * integrações de quem tem Tray e Mercos entram com códigos distintos sob o mesmo INT_CODCLI.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class IntegracaoId implements Serializable {

    private String codigoIntegracao;

    private Long codigoCliente;
}
