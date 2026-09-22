package com.citel.monitoramento_n8n.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;


@Getter
@Setter
@Entity
@Table(name="PROERR")
public class Produto {
    @Column(name="PRO_IDERRO")
    @Id
    private String id;
    @Column(name="PRO_CODITE")
    private String codigoProduto;
    @Column(name="PRO_LOGERR")
    private String mensagemErro;
    @Column(name="PRO_DTAERR", insertable = false, updatable = false)
    private Date dataErro;
    @Column(name="PRO_CLIENT")
    private String cliente;
    @Column(name="PRO_INTEGR")
    private String plataforma;
    @Column(name="PRO_STATUS")
    private int status;
    @Column(name="PRO_ID_INT")
    private String idIntegracao;
    @Column(name="PRO_ROTINA")
    private String rotina;
    /** Quantas vezes o mesmo produto (codigoProduto + cliente + rotina) foi reportado com erro. */
    @Column(name="PRO_TENTAT")
    private int tentativa;
    /**
     * Liberação do produto. 'N' marca o que foi represado e não deve sair na listagem padrão;
     * nulo (o default da coluna) é o registro comum, sem restrição.
     */
    @Column(name="PRO_LIBERA", length = 1)
    private String libera;

    public Produto() {
        this.id = UUID.randomUUID().toString();
    }

    public void setErro(String erro) {
        this.mensagemErro = erro;
    }

    /** Soma +1 nas tentativas. Usado quando o produto já existe e o erro se repete. */
    public void incrementarTentativa() {
        this.tentativa = this.tentativa + 1;
    }
}
