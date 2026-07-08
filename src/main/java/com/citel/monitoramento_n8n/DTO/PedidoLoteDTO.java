package com.citel.monitoramento_n8n.DTO;

import com.citel.monitoramento_n8n.model.Pedido;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public class PedidoLoteDTO {
    private String codigoPedido;
    private String cliente;
    private String plataforma;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dataPedido;
    @Schema(hidden = true)
    private LocalDateTime ultimaAlteracao;
    private int sequencialProcessamento;


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

    public String getPlataforma()
    {
        return plataforma;
    }

    public LocalDateTime getDataPedido() {return dataPedido;}

    public void setDataPedido(LocalDateTime dataPedido) { this.dataPedido = dataPedido;}

    public void setUltimaAlteracao(LocalDateTime ultimaAlteracao) { this.ultimaAlteracao = ultimaAlteracao;}

    public LocalDateTime getUltimaAlteracao() {return ultimaAlteracao;}

    public int getSequencialProcessamento() {return sequencialProcessamento;}

    public void setSequencialProcessamento(int sequencialProcessamento) {this.sequencialProcessamento = sequencialProcessamento;}

    public void setPlataforma(String plataforma)
    {
        this.plataforma = plataforma;
    }




    public static Pedido converterDTO(PedidoLoteDTO dto, Pedido pd)
    {
        pd.setCliente(dto.getCliente());
        pd.setCodigoPedido(dto.getCodigoPedido());
        pd.setPlataforma(dto.getPlataforma());
        pd.setDataPedido(dto.getDataPedido());
        pd.setUltimaAlteracao(LocalDateTime.now());
        pd.setSequencialProcessamento(pd.getSequencialProcessamento()+1);

        return pd;

    }
}
