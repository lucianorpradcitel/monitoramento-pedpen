package com.citel.monitoramento_n8n.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * Status de responsabilidade do erro (STAERR): 2 = erro da integração (culpa nossa),
 * 3 = não é culpa nossa (cliente ou plataforma). Só leitura: os valores entram por script.
 *
 * Não confundir com PEN_STATUS (0 PENDENTE, 1 INTEGRA, 2 ERRO, 3 FINALIZADO) nem com PRO_STATUS.
 */
@Entity
@Table(name = "STAERR")
@Getter
public class StatusErro {

    @Id
    @Column(name = "STA_CODIGO")
    private Integer codigo;

    @Column(name = "STA_DESCRI", nullable = false, length = 60)
    private String descricao;
}
