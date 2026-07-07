package com.citel.monitoramento_n8n.model;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import org.springframework.cglib.core.Local;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;


@Entity
@Table(name="PEDPEN")
public class Pedido {
    @Column(name="PEN_IDPED_")
    @Id
    private String id;
    @Column(name="PEN_NUMPOK")
    private String codigoPedido;
    @Column(name="PEN_NOMCLI")
    private String cliente;
    @Column(name="PEN_ERRPED")
    private String erro;
    @Column(name="PEN_NOMPLA")
    private String plataforma;
    @Column(name="PEN_STATUS")
    private int status;
    @Column(name="PEN_DTAPED", updatable = false)
    private LocalDateTime dataPedido;
    @Column(name="PEN_DHUALT")
    @Schema(hidden = true)
    private LocalDateTime ultimaAlteracao;
    @Column(name="PEN_DTAINS", insertable = false, updatable = false)
    private LocalDateTime dataInsercao;
    @Column(name="PEN_SEQPRC")
    private int sequencialProcessamento;




    public Pedido() {
        this.id = UUID.randomUUID().toString();

    }


    public void definePedidoComErro (String pedido, String cliente, String erro, String plataforma, int status)
    {
        this.codigoPedido = pedido;
        this.cliente = cliente;
        this.erro = erro;
        this.plataforma = plataforma;
        this.status = status;

    }

    public String getCodigoPedido() {
        return codigoPedido;
    }
    public void setCodigoPedido(String codigoPedido) {
        this.codigoPedido = codigoPedido;
    }

    public String getCliente() {
        return cliente;
    }
    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public String getErro()
    {
        return erro;
    }

    public void setErro(String erro)
    {
        this.erro = erro;
    }

    public String getPlataforma()
    {
        return plataforma;
    }

    public void setPlataforma(String plataforma)
    {
        this.plataforma = plataforma;
    }

    public void setDataPedido(LocalDateTime dataPedido) {this.dataPedido = dataPedido;}

    public void setUltimaAlteracao(LocalDateTime ultimaAlteracao) { this.ultimaAlteracao = ultimaAlteracao;}

    public LocalDateTime getUltimaAlteracao() {return ultimaAlteracao;}
    public int getSequencialProcessamento() {return sequencialProcessamento;}

    public void setSequencialProcessamento(int sequencialProcessamento) {this.sequencialProcessamento = sequencialProcessamento;}

    public int getStatus()
    {
        return status;
    }

    public void setStatus(int status)
    {
        this.status = status;
    }
}
