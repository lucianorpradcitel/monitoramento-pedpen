package com.citel.monitoramento_n8n.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.LocalDateTime;

/**
 * Histórico de alterações de uma integração (LOGINT): uma linha por campo alterado em cada
 * edição, com o valor de antes e o de depois e quem fez.
 *
 * Os valores são gravados exatamente como estão no CADINT, inclusive a chave privada — o acesso à
 * tela de edição e à própria tabela é restrito ao time.
 */
@Entity
@Table(name = "LOGINT")
@Getter
@Setter
public class LogIntegracao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AUTOINCREM")
    private Long id;

    /** Preenchida pelo DEFAULT CURRENT_TIMESTAMP do banco; o Hibernate a relê após o INSERT. */
    @Generated(event = EventType.INSERT)
    @Column(name = "LOG_DHUALT", insertable = false, updatable = false)
    private LocalDateTime dataAlteracao;

    @Column(name = "LOG_NOMPLA")
    private String nomePlataforma;

    @Column(name = "LOG_NOMCLI")
    private String nomeCliente;

    /** Código de autorização da integração (CADINT.INT_CODAUT), ex.: 0000004. */
    @Column(name = "LOG_CODAUT", length = 7)
    private String codigoIntegracao;

    /** Coluna do CADINT que mudou (INT_URLWBS, INT_URLAPI ou INT_PRVKEY). */
    @Column(name = "LOG_CMPALT")
    private String campoAlterado;

    @Column(name = "LOG_VALANT", columnDefinition = "text")
    private String valorAnterior;

    @Column(name = "LOG_VALATU", columnDefinition = "text")
    private String valorAtual;

    /** Nome do usuário interno (CADUSR) que fez a alteração. */
    @Column(name = "LOG_NOMUSU")
    private String nomeUsuario;
}
