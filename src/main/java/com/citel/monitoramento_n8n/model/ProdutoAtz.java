package com.citel.monitoramento_n8n.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.Date;
import java.util.UUID;
@Data
@Entity
@Table(name="LOGPRO")
public class ProdutoAtz {
    @Column(name="AUTOINCREM")
    @Id
    private String id;
    @Column(name="PRO_NOMEMP")
    private String nomeEmpresa;
    @Column(name="PRO_NOMPLA")
    private  String nomePlataforma;
    @Column(name="PRO_CODITE")
    private String codite;
    @Column(name="PRO_IDPLAT")
    private String idPlataforma;
    @Column(name = "PRO_ACRESC")
    private String acrescimoPreco;
    @Column(name="PRO_VAREST")
    private String variacaoEstoque;
    @Column(name = "PRO_VALREC")
    private String valorRecebido;
    @Column(name="PRO_VALENV")
    private String valorEnviado;
    @Column(name="PRO_RSPNSE")
    private String codigoResposta;
    @Column(name = "PRO_TIPENV")
    private String tipoEnvio;


    public ProdutoAtz() {
        this.id = UUID.randomUUID().toString();

    }

}


