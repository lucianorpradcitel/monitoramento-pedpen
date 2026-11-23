package com.citel.monitoramento_n8n.model;


import io.swagger.v3.oas.annotations.media.Schema;
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
     * Tipo do erro (CADMSG.MSG_CODIGO), que diz a responsabilidade: 2 = culpa nossa, 3 = não é culpa
     * nossa. Nulo = erro ainda sem tipo. Acompanha a mensagem: mensagem nova sem código limpa o tipo.
     */
    @Column(name="PRO_CODMSG")
    private Integer codigoMensagem;

    public Produto() {
        this.id = UUID.randomUUID().toString();
    }

    /** Atalho de setMensagemErro. Escondido do Swagger, que senão expõe um campo "erro" que o JSON não tem. */
    @Schema(hidden = true)
    public void setErro(String erro) {
        this.mensagemErro = erro;
    }

    /** Soma +1 nas tentativas. Usado quando o produto já existe e o erro se repete. */
    public void incrementarTentativa() {
        this.tentativa = this.tentativa + 1;
    }
}
