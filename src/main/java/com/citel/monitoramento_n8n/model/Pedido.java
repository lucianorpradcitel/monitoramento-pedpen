package com.citel.monitoramento_n8n.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Formula;
import java.time.LocalDateTime;
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
    private LocalDateTime ultimaAlteracao;
    @Column(name="PEN_DTAINS", insertable = false, updatable = false)
    private LocalDateTime dataInsercao;
    @Column(name="PEN_SEQPRC")
    // Espelha o DEFAULT 1 da PEN_SEQPRC: o Hibernate grava o campo explicitamente no INSERT,
    // então sem isso um pedido novo entraria com 0.
    private int sequencialProcessamento = 1;
    @Column(name="PEN_ID_INT")
    private String idIntegracao;
    @Column(name="PEN_ROTINA")
    private String rotina;
    /**
     * Tipo do erro (CADMSG.MSG_CODIGO), que define a responsabilidade (veja {@link #responsabilidade}).
     * Nulo = sem tipo. Acompanha o erro: cada POST /pendentes grava o código que veio.
     */
    @Column(name="PEN_CODMSG")
    private Integer codigoMensagem;
    /**
     * Responsabilidade do erro, lida do tipo (CADMSG.MSG_CODSTA): 0 = não informado, 1 = culpa nossa,
     * 2 = culpa do cliente, 3 = instabilidade, 4 = culpa da plataforma externa. Pedido sem tipo vale 0.
     * Campo calculado pelo banco a cada leitura e só de leitura: não confundir com o status (PEN_STATUS),
     * que é a situação na fila. Em um pedido recém-gravado vem nulo até a próxima leitura.
     */
    @Formula("COALESCE((SELECT m.MSG_CODSTA FROM CADMSG m WHERE m.MSG_CODIGO = PEN_CODMSG), 0)")
    private Integer responsabilidade;

    public Pedido() {
        this.id = UUID.randomUUID().toString();

    }


    /** PEN_IDPED_: identifica a linha. O codigoPedido sozinho se repete entre lojistas. */
    public String getId() {
        return id;
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

    public String getIdIntegracao() {return idIntegracao;}

    public void setIdIntegracao(String idIntegracao) {this.idIntegracao = idIntegracao;}

    public String getRotina() {return rotina;}

    public void setRotina(String rotina) {this.rotina = rotina;}

    public Integer getCodigoMensagem() {return codigoMensagem;}

    public void setCodigoMensagem(Integer codigoMensagem) {this.codigoMensagem = codigoMensagem;}

    /** Só leitura: vem do banco (CADMSG.MSG_CODSTA do tipo). Sem setter de propósito. */
    public Integer getResponsabilidade() {return responsabilidade;}
}
