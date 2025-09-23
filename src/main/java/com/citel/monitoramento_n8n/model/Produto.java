package com.citel.monitoramento_n8n.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Date;
import java.util.UUID;

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
    private int status; //0 com erro, 1 resolvido


    public Produto() {
        this.id = UUID.randomUUID().toString();

    }


    public String getId() {return id;}
    public String getCodigoProduto() {
        return codigoProduto;
    }
    public void setCodigoProduto(String codigoProduto) {
        this.codigoProduto = codigoProduto;
    }

    public String getErro()
    {
        return mensagemErro;
    }

    public void setErro(String mensagemErro)
    {
        this.mensagemErro = mensagemErro;
    }

    public String getPlataforma()
    {
        return plataforma;
    }

    public void setPlataforma(String plataforma)
    {
        this.plataforma = plataforma;
    }

    public int getStatus()
    {
        return status;
    }
    public void setStatus(int errStatus)
    {
        this.status = errStatus;
    }

    public String getCliente() {return cliente;}

    public void setCliente(String cliente) { this.cliente = cliente;}

    public void setDataErro (Date dataErro) {this.dataErro = dataErro;}

    public Date getDataErro (){return dataErro;}
}
